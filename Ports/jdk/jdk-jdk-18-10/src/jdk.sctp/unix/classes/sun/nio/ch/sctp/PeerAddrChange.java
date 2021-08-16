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

import java.net.SocketAddress;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import java.lang.annotation.Native;

/**
 * An implementation of PeerAddressChangeNotification
 */
public class PeerAddrChange extends PeerAddressChangeNotification
    implements SctpNotification
{
    /* static final ints so that they can be referenced from native */
    @Native private final static int SCTP_ADDR_AVAILABLE = 1;
    @Native private final static int SCTP_ADDR_UNREACHABLE = 2;
    @Native private final static int SCTP_ADDR_REMOVED = 3;
    @Native private final static int SCTP_ADDR_ADDED = 4;
    @Native private final static int SCTP_ADDR_MADE_PRIM = 5;
    @Native private final static int SCTP_ADDR_CONFIRMED =6;

    private Association association;

    /* assocId is used to lookup the association before the notification is
     * returned to user code */
    private int assocId;
    private SocketAddress address;
    private AddressChangeEvent event;

    /* Invoked from native */
    private PeerAddrChange(int assocId, SocketAddress address, int intEvent) {
        switch (intEvent) {
            case SCTP_ADDR_AVAILABLE :
                this.event = AddressChangeEvent.ADDR_AVAILABLE;
                break;
            case SCTP_ADDR_UNREACHABLE :
                this.event = AddressChangeEvent.ADDR_UNREACHABLE;
                break;
            case SCTP_ADDR_REMOVED :
                this.event = AddressChangeEvent.ADDR_REMOVED;
                break;
            case SCTP_ADDR_ADDED :
                this.event = AddressChangeEvent.ADDR_ADDED;
                break;
            case SCTP_ADDR_MADE_PRIM :
                this.event = AddressChangeEvent.ADDR_MADE_PRIMARY;
                break;
            case SCTP_ADDR_CONFIRMED :
                this.event = AddressChangeEvent.ADDR_CONFIRMED;
                break;
            default:
                throw new AssertionError("Unknown event type");
        }
        this.assocId = assocId;
        this.address = address;
    }

    @Override
    public int assocId() {
        return assocId;
    }

    @Override
    public void setAssociation(Association association) {
        this.association = association;
    }

    @Override
    public SocketAddress address() {
        assert address != null;
        return address;
    }

    @Override
    public Association association() {
        assert association != null;
        return association;
    }

    @Override
    public AddressChangeEvent event() {
        assert event != null;
        return event;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" [");
        sb.append("Address: ").append(address);
        sb.append(", Association:").append(association);
        sb.append(", Event: ").append(event).append("]");
        return sb.toString();
    }
}

