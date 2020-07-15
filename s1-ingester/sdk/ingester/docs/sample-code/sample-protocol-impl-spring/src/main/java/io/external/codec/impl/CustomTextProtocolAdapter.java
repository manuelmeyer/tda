package io.external.codec.impl;

import io.pivotal.rti.protocols.AbstractProtocolAdapter;
import io.pivotal.rti.protocols.ProtocolDetail;
import io.pivotal.rti.protocols.ProtocolDetails;
import io.pivotal.rti.protocols.ProtocolEvent;
import io.pivotal.rti.protocols.TextProtocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
* Sample implementation for {@link io.pivotal.rti.io.protocols.ProtocolAdapter}
* that extends the base API implementation available at
* {@link io.pivotal.rti.io.protocols.AbstractProtocolAdapter}
*
* This implementation is using text string as an example.
*
* Modify the method {@link #bytesToProtocolEvent(byte[])} to generate
* {@link io.pivotal.rti.io.protocols.ProtocolEvent} that takes into
* consideration the specific implementation details such as the usage
* of 3rd party codec or other libraries.
*
*/
@TextProtocol
@ProtocolDetails(details={
        @ProtocolDetail(name="foo", type=String.class, description = "this is foo"),
        @ProtocolDetail(name="bar", type=int.class, description = "this is bar")
})
public class CustomTextProtocolAdapter extends AbstractProtocolAdapter {

    private static final String PROTOCOL_NAME = "text-protocol";

    public CustomTextProtocolAdapter() {
        super(PROTOCOL_NAME);
    }

    /**
     *
     * This method is implemented for illustration purposes.
     *
     * You have to modify it to accommodate your specific implementation.
     *
     * @param buffer byte array that is used for transforming to a list of
     * {@link io.pivotal.rti.io.protocols.ProtocolEvent}s
     *
     * @return
     * @throws IOException
     */
    @Override
    public Collection<ProtocolEvent> bytesToProtocolEvent(byte[] buffer) throws IOException {

        ProtocolEvent protocolEvent = new ProtocolEvent();
        protocolEvent.setKey(UUID.randomUUID().toString());
        protocolEvent.setPrimaryId(new String(buffer));
        protocolEvent.getProtocolDetails().put("foo", "bar");
        protocolEvent.getProtocolDetails().put("bar", (Integer)1);

        List<ProtocolEvent> protocolEvents = new ArrayList<>();
        protocolEvents.add(protocolEvent);

        return protocolEvents;
    }
}
