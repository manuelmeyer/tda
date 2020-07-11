package com.vodafone.dca.infra;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.convert.converter.Converter;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;

public class RingBufferMessageDispatcher<T, R> implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(RingBufferMessageDispatcher.class);
    
    private Disruptor<RingBufferMessage<T>> disruptor;
    private RingBufferMessageProducerWithTranslator producer;
    private RingBuffer<RingBufferMessage<T>> ringBuffer;

    private Executor executor;

    // default converter in case we use <S, S> same classes.
    @SuppressWarnings("unchecked")
    private Converter<R, T> converter = ((source) -> (T) source);

    // exposed properties
    private int bufferSize = 128;
    private int parallelConsumers = 2;
    private String prefix = "ring-buffer-consumer";

    private BaseFlowErrorHandlers<R, T> flowErrorHandlers;
    private Consumer<T> messageHandler;
    // exposed properties

    private BiConsumer<T, Throwable> errorDispatcher;
    private BiConsumer<R, Throwable> errorConverterDispatcher;

    private boolean isRunning = false;
    
    static class RingBufferMessage<T> {

        T request;

        // release data entry so that they don't stay in the ring buffer
        // waiting for a new entry to overwrite them.
        void clear() {
            request = null;
        }
    }

    class RingBufferMessageFactory implements EventFactory<RingBufferMessage<T>> {

        @Override
        public RingBufferMessage<T> newInstance() {
            return new RingBufferMessage<>();
        }
    }

    class RingBufferMessageEventHandler implements WorkHandler<RingBufferMessage<T>> {

        @Override
        public void onEvent(RingBufferMessage<T> event) {
            try {
                if (isRunning) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Sending {} request", event.request);
                    }
                    messageHandler.accept(event.request);
                }
            } catch (Throwable e) {
                LOG.error("Uncaught exception occurred while processing an event.", e);
                try {
                    errorDispatcher.accept(event.request, e);
                } catch (Exception sent) {
                    LOG.error("Exception while sending the error", sent);
                }
            } finally {
                event.clear();
            }
        }
    }

    class RingBufferMessageProducerWithTranslator {
        private final RingBuffer<RingBufferMessage<T>> ringBuffer;

        RingBufferMessageProducerWithTranslator(RingBuffer<RingBufferMessage<T>> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        private final EventTranslatorOneArg<RingBufferMessage<T>, T> translator = (e, s, r) -> e.request = r;

        void onData(R request) {
            if (isRunning) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Getting a msg {}", request);
                }
                try {
                    ringBuffer.publishEvent(translator, converter.convert(request));
                } catch (Exception e) {
                    LOG.error("Exception while pushing a message in the ringbuffer - pushing it in the error channel", e);
                    try {
                        errorConverterDispatcher.accept(request, e);
                    } catch (Exception dispatched) {
                        LOG.error("Error while calling converter dispatcher", dispatched);
                    }
                }
            }
        }
    }

    public long remainingCapacity() {
        return ringBuffer == null ? 0 : ringBuffer.remainingCapacity();
    }

    public boolean isEmpty() {
        return ringBuffer != null && ringBuffer.remainingCapacity() == bufferSize;
    }

    public RingBufferMessageDispatcher<T, R> withMessageHandler(Consumer<T> messageHandler) {
        this.messageHandler = messageHandler;
        return this;
    }

    public RingBufferMessageDispatcher<T, R> withBufferSizeAndParallelConsumers(int bufferSize, int parallelConsumers) {
        this.bufferSize = bufferSize;
        this.parallelConsumers = parallelConsumers;
        return this;
    }

    public RingBufferMessageDispatcher<T, R> withThreadNamePrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public RingBufferMessageDispatcher<T, R> withConverter(Converter<R, T> converter) {
        this.converter = converter;
        return this;
    }

    public RingBufferMessageDispatcher<T, R> withErrorHandlers(BaseFlowErrorHandlers<R, T> flowErrorHandlers) {
        this.flowErrorHandlers = flowErrorHandlers;
        return this;
    }

    public RingBufferMessageDispatcher<T, R> initialise() {
        if (!isRunning) {
            checkProperties();
            createExecutorAndErrorHandlers();
            createRingBuffer();
        }
        isRunning = true;
        return this;
    }

    private void createExecutorAndErrorHandlers() {
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(prefix + "-%d").build());
        errorDispatcher = flowErrorHandlers::onFlowError;
        errorConverterDispatcher = flowErrorHandlers::onConverterFlowError;
    }

    private void checkProperties() {
        notNull(messageHandler, "messageHandler cannot be null.");
        notNull(converter, "converter must not be null");
        notNull(flowErrorHandlers, "errorHandlers(FlowErrorHandler) cannot be null");
        isTrue(parallelConsumers >= 1, "parallelConsumers must be >= 1.");
        isTrue((bufferSize & (bufferSize - 1)) == 0, "bufferSize must be a power of 2.");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createRingBuffer() {
        disruptor = new Disruptor<>(new RingBufferMessageFactory(), bufferSize, executor);
        WorkHandler[] eventHandlers = new WorkHandler[parallelConsumers];
        LOG.trace("Creating {} ring buffer consumer(s)", parallelConsumers);
        for (int index = 0; index < parallelConsumers; index++) {
            eventHandlers[index] = new RingBufferMessageEventHandler();
        }
        disruptor.handleEventsWithWorkerPool(eventHandlers);
        ringBuffer = disruptor.getRingBuffer();

        producer = new RingBufferMessageProducerWithTranslator(ringBuffer);
        disruptor.start();
    }

    public T receive(R request) {
        producer.onData(request);
        return null;
    }

    public void shutdown() {
        if (isRunning) {
            LOG.trace("Shutting down disruptor");
            isRunning = false;
            try {
                disruptor.shutdown(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOG.error("Error while stopping disruptor");
            }
            LOG.trace("Disruptor stopped");
        }
        isRunning = false;
    }

	@Override
	public void start() {
		LOG.info("Starting ring buffer");
		isRunning = true;
	}

	@Override
	public void stop() {
		if(isRunning) {
			LOG.info("Stopping ring buffer");
			shutdown();
		}
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public int getPhase() {
		return 0;
	}
}
