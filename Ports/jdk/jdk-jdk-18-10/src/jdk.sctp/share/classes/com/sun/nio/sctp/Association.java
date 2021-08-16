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

/**
 * A class that represents an SCTP association.
 *
 * <P> An association exists between two SCTP endpoints. Each endpoint is
 * represented by a list of transport addresses through which that endpoint can
 * be reached and from which it will originate SCTP messages. The association
 * spans over all of the possible source/destination combinations which may be
 * generated from each endpoint's lists of addresses.
 *
 * <P> Associations are identified by their Association ID.
 * Association ID's are guaranteed to be unique for the lifetime of the
 * association. An association ID may be reused after the association has been
 * shutdown. An association ID is not unique across multiple SCTP channels.
 * An Association's local and remote addresses may change if the SCTP
 * implementation supports <I>Dynamic Address Reconfiguration</I> as defined by
 * <A HREF="http://tools.ietf.org/html/rfc5061">RFC5061</A>, see the
 * {@code bindAddress} and {@code unbindAddress} methods of {@link SctpChannel},
 * {@link SctpServerChannel}, and {@link SctpMultiChannel}.
 *
 * <P> An {@code Association} is returned from an {@link
 * SctpChannel#association SctpChannel} or an {@link
 * SctpMultiChannel#associations SctpMultiChannel}, as well
 * as being given as a parameter to {@link NotificationHandler
 * NotificationHandler} methods.
 *
 * @since 1.7
 */
public class Association {
    private final int associationID;
    private final int maxInStreams;
    private final int maxOutStreams;

    /**
     * Initializes a new instance of this class.
     *
     * @param  associationID
     *         The association ID
     * @param  maxInStreams
     *         The maximum number of inbound streams
     * @param  maxOutStreams
     *         The maximum number of outbound streams
     */
    protected Association(int associationID,
                          int maxInStreams,
                          int maxOutStreams) {
        this.associationID = associationID;
        this.maxInStreams = maxInStreams;
        this.maxOutStreams = maxOutStreams;
    }

    /**
     * Returns the associationID.
     *
     * @return  The association ID
     */
    public final int associationID() {
        return associationID;
    };

    /**
     * Returns the maximum number of inbound streams that this association
     * supports.
     *
     * <P> Data received on this association will be on stream number
     * {@code s}, where {@code 0 <= s < maxInboundStreams()}.
     *
     * @return  The maximum number of inbound streams
     */
    public final int maxInboundStreams() {
        return maxInStreams;
    };

    /**
     * Returns the maximum number of outbound streams that this association
     * supports.
     *
     * <P> Data sent on this association must be on stream number
     * {@code s}, where {@code 0 <= s < maxOutboundStreams()}.
     *
     * @return  The maximum number of outbound streams
     */
    public final int maxOutboundStreams() {
        return maxOutStreams;
    };
}
