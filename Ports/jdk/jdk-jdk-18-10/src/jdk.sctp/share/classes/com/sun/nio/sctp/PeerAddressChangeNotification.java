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
package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * Notification emitted when a destination address on a multi-homed peer
 * encounters a change.
 *
 * @since 1.7
 */
public abstract class PeerAddressChangeNotification
    implements Notification
{
    /**
     * Defines the type of address change event that occurred to the destination
     * address on a multi-homed peer when it encounters a change of interface
     * details.
     *
     * <P> Some of these events types are only generated when the association
     * supports dynamic address reconfiguration, e.g. {@code SCTP_ADDR_ADDED},
     * {@code SCTP_ADDR_REMOVED}, etc.
     *
     * @since 1.7
     */
    public enum AddressChangeEvent {
        /**
         * This address is now reachable.
         */
       ADDR_AVAILABLE,

       /**
        * The address specified can no longer be reached. Any data sent to this
        * address is rerouted to an alternate until this address becomes reachable.
        */
       ADDR_UNREACHABLE,

       /**
        * The address is no longer part of the association.
        */
       ADDR_REMOVED,

       /**
        * The address is now part of the association.
        */
       ADDR_ADDED,

       /**
        * This address has now been made to be the primary destination address.
        */
       ADDR_MADE_PRIMARY,

       /**
        * This address has now been confirmed as a valid address.
        */
       ADDR_CONFIRMED;
    }

    /**
     * Initializes a new instance of this class.
     */
    protected PeerAddressChangeNotification() {}

    /**
     * Returns the peer address.
     *
     * @return  The peer address
     */
    public abstract SocketAddress address();

    /**
     * Returns the association that this notification is applicable to.
     *
     * @return  The association whose peer address changed
     */
    public abstract Association association();

    /**
     * Returns the type of change event.
     *
     * @return  The event
     */
    public abstract AddressChangeEvent event();
}
