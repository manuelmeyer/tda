/*
 * =========================================================================
 * Copyright (c) 2013-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 * =========================================================================
 */
package io.external.codec.impl;

import io.pivotal.rti.protocols.AbstractProtocolAdapter;
import io.pivotal.rti.protocols.ProtocolDetail;
import io.pivotal.rti.protocols.ProtocolDetails;
import io.pivotal.rti.protocols.ProtocolEvent;

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
 * This implementation is using a contrived binary protocol as an example.
 * 12 bytes - 4 bytes for length + 8 bytes for data.
 *
 * Modify the method {@link #bytesToProtocolEvent(byte[])} to generate
 * {@link io.pivotal.rti.io.protocols.ProtocolEvent} that takes into
 * consideration the specific implementation details such as the usage
 * of 3rd party codec or other libraries.
 *
 */
@ProtocolDetails(details={
        @ProtocolDetail(name="foo", type=String.class, description = "this is foo"),
        @ProtocolDetail(name="bar", type=int.class, description = "this is bar")
})
public class CustomBinaryProtocolAdapter extends AbstractProtocolAdapter {

    private static final String PROTOCOL_NAME = "binary-protocol";

    public CustomBinaryProtocolAdapter() {
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
        //skip length
        readUnsignedInt(buffer, 0);
        long primaryId = readUnsignedInt(buffer, 4);
        long fooValue = readUnsignedInt(buffer, 8);

        protocolEvent.setPrimaryId(String.valueOf(primaryId));
        protocolEvent.getProtocolDetails().put("foo", fooValue);

        List<ProtocolEvent> protocolEvents = new ArrayList<>();
        protocolEvents.add(protocolEvent);

        return protocolEvents;
    }

    public final long readUnsignedInt(byte[] buff, int pos) throws IOException {

        long res = ((buff[pos] & 0xffL) << 24) |
                ((buff[pos + 1] & 0xffL) << 16) |
                ((buff[pos + 2] & 0xffL) << 8) |
                (buff[pos + 3] & 0xffL);
        return res;
    }
}
