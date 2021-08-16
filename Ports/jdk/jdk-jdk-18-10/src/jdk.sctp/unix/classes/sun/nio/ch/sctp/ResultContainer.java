/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;

/**
 * Wraps the actual message or notification so that it can be
 * set and returned from the native receive implementation.
 */
public class ResultContainer {
    /* static final ints so that they can be referenced from native */
    @Native static final int NOTHING = 0;
    @Native static final int MESSAGE = 1;
    @Native static final int SEND_FAILED = 2;
    @Native static final int ASSOCIATION_CHANGED = 3;
    @Native static final int PEER_ADDRESS_CHANGED = 4;
    @Native static final int SHUTDOWN = 5;

    private Object value;
    private int type;

    int type() {
        return type;
    }

    boolean hasSomething() {
        return type() != NOTHING;
    }

    boolean isNotification() {
        return type() != MESSAGE && type() != NOTHING ? true : false;
    }

    void clear() {
        type = NOTHING;
        value = null;
    }

    SctpNotification notification() {
        assert type() != MESSAGE && type() != NOTHING;

        return (SctpNotification) value;
    }

    MessageInfoImpl getMessageInfo() {
        assert type() == MESSAGE;

        if (value instanceof MessageInfoImpl)
            return (MessageInfoImpl) value;

        return null;
    }

    SendFailed getSendFailed() {
        assert type() == SEND_FAILED;

        if (value instanceof SendFailed)
            return (SendFailed) value;

        return null;
    }

    AssociationChange getAssociationChanged() {
        assert type() == ASSOCIATION_CHANGED;

        if (value instanceof AssociationChange)
            return (AssociationChange) value;

        return null;
    }

    PeerAddrChange getPeerAddressChanged() {
        assert type() == PEER_ADDRESS_CHANGED;

        if (value instanceof PeerAddrChange)
            return (PeerAddrChange) value;

        return null;
    }

    Shutdown getShutdown() {
        assert type() == SHUTDOWN;

        if (value instanceof Shutdown)
            return (Shutdown) value;

        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ");
        switch (type) {
            case NOTHING:              sb.append("NOTHING");             break;
            case MESSAGE:              sb.append("MESSAGE");             break;
            case SEND_FAILED:          sb.append("SEND FAILED");         break;
            case ASSOCIATION_CHANGED:  sb.append("ASSOCIATION CHANGE");  break;
            case PEER_ADDRESS_CHANGED: sb.append("PEER ADDRESS CHANGE"); break;
            case SHUTDOWN:             sb.append("SHUTDOWN");            break;
            default :                  sb.append("Unknown result type");
        }
        sb.append(", Value: ");
        sb.append((value == null) ? "null" : value.toString());
        return sb.toString();
    }
}
