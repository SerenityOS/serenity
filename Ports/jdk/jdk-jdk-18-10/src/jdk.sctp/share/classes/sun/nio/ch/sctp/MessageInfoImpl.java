/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.nio.ch.sctp;

import java.net.SocketAddress;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Association;

/**
 * An implementation of a MessageInfo.
 */
public class MessageInfoImpl extends MessageInfo {
    private final SocketAddress address;
    private final int bytes;          /* 0 */

    private Association association;
    private int assocId;
    private int streamNumber;
    private boolean complete = true;
    private boolean unordered;  /* false */
    private long timeToLive;    /* 0L */
    private int ppid;           /* 0 */

    public MessageInfoImpl(Association association,
                           SocketAddress address,
                           int streamNumber) {
        this.association = association;
        this.address = address;
        this.streamNumber = streamNumber;
        bytes = 0;
    }

    /* Invoked from native */
    private MessageInfoImpl(int assocId,
                            SocketAddress address,
                            int bytes,
                            int streamNumber,
                            boolean complete,
                            boolean unordered,
                            int ppid) {
        this.assocId = assocId;
        this.address = address;
        this.bytes = bytes;
        this.streamNumber = streamNumber;
        this.complete = complete;
        this.unordered = unordered;
        this.ppid = ppid;
    }

    @Override
    public Association association() {
        return association;
    }

    /**
     * MessageInfoImpl instances created from native will need to have their
     * association set from the channel.
     */
    void setAssociation(Association association) {
        this.association = association;
    }

    int associationID() {
        return assocId;
    }

    @Override
    public SocketAddress address() {
        return address;
    }

    @Override
    public int bytes() {
        return bytes;
    }

    @Override
    public int streamNumber() {
        return streamNumber;
    }

    @Override
    public MessageInfo streamNumber(int streamNumber) {
        if (streamNumber < 0 || streamNumber > 65536)
            throw new IllegalArgumentException("Invalid stream number");

        this.streamNumber = streamNumber;
        return this;
    }

    @Override
    public int payloadProtocolID() {
        return ppid;
    }

    @Override
    public MessageInfo payloadProtocolID(int ppid) {
        this.ppid = ppid;
        return this;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public MessageInfo complete(boolean complete) {
        this.complete = complete;
        return this;
    }

    @Override
    public boolean isUnordered() {
        return unordered;
    }

    @Override
    public MessageInfo unordered(boolean unordered) {
        this.unordered = unordered;
        return this;
    }

    @Override
    public long timeToLive() {
        return timeToLive;
    }

    @Override
    public MessageInfo timeToLive(long millis) {
        timeToLive = millis;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append( "[Address: ").append(address)
          .append(", Association: ").append(association)
          .append(", Assoc ID: ").append(assocId)
          .append(", Bytes: ").append(bytes)
          .append(", Stream Number: ").append(streamNumber)
          .append(", Complete: ").append(complete)
          .append(", isUnordered: ").append(unordered)
          .append("]");
        return sb.toString();
    }
}
