/*
 * =========================================================================
 * Copyright (c) 2013-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 * =========================================================================
 */
package io.external.codec.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.external.codec.impl.autoconfig.CustomBinaryProtocolAdapterConfig;
import io.pivotal.rti.protocols.ProtocolDetailMap;
import io.pivotal.rti.protocols.ProtocolEvent;
import io.pivotal.rti.prtocols.adapter.test.harness.ProtocolAdapterTestSupportConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Illustrating{@link CustomTextProtocolAdapter}
 * integration test using a contrived binary protocol as an example.
 *
 * Use this as a template to verify your {@link io.pivotal.rti.io.protocols.ProtocolAdapter} implementation.
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ContextConfiguration(classes = {CustomBinaryProtocolAdapterConfig.class, ProtocolAdapterTestSupportConfig.class})
@TestPropertySource("classpath:binary-protocol.properties")
public class CustomBinaryProtocolAdapterIntegTest {

    private static final long TEST_TIMEOUT = 5L;

    //Allocate 12 bytes - 4 bytes for length + 8 bytes for data
    private static byte[] DATA = ByteBuffer.allocate(12)
            .putInt(8)
            .putInt(51).putInt(52).
            array();

    @Autowired
    @Qualifier("output")
    SubscribableChannel output;

    @Value("${binary-protocol.tcp.host:127.0.0.1}")
    String host;

    @Value("${binary-protocol.tcp.port:49006}")
    int port;

    private CountDownLatch latch;

    @Test
    public void testProtocolAdapter() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        output.subscribe(new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {

                /**
                 * Following assertions are shown as an example. Please change accordingly.
                 */
                Assert.assertEquals(ProtocolEvent.class, message.getPayload().getClass());
                ProtocolEvent e = (ProtocolEvent) message.getPayload();
                assertTrue(e.getPrimaryId().equals("51"));
                ProtocolDetailMap ed = e.getProtocolDetails();
                assertTrue(ed.get("foo").equals(52L));

                latch.countDown();
            }
        });

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);

            /**
             * Make sure to change the contents of the socket outputstream
             * write below to reflect the appropriate data
             */
            socket.getOutputStream().write(DATA);
            socket.getOutputStream().flush();

        } catch (IOException e) {
            fail(e.getMessage());
        }

        boolean success = latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertTrue(success);
    }
}


