/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import java.lang.annotation.Native;

/**
 * An implementation of AssociationChangeNotification
 */
public class AssociationChange extends AssociationChangeNotification
    implements SctpNotification
{
    /* static final ints so that they can be referenced from native */
    @Native private final static int SCTP_COMM_UP = 1;
    @Native private final static int SCTP_COMM_LOST = 2;
    @Native private final static int SCTP_RESTART = 3;
    @Native private final static int SCTP_SHUTDOWN = 4;
    @Native private final static int SCTP_CANT_START = 5;

    private Association association;

    /* assocId is used to lookup the association before the notification is
     * returned to user code */
    private int assocId;
    private AssocChangeEvent event;
    private int maxOutStreams;
    private int maxInStreams;

    /* Invoked from native */
    private AssociationChange(int assocId,
                              int intEvent,
                              int maxOutStreams,
                              int maxInStreams) {
        switch (intEvent) {
            case SCTP_COMM_UP :
                this.event = AssocChangeEvent.COMM_UP;
                break;
            case SCTP_COMM_LOST :
                this.event = AssocChangeEvent.COMM_LOST;
                break;
            case SCTP_RESTART :
                this.event = AssocChangeEvent.RESTART;
                break;
            case SCTP_SHUTDOWN :
                this.event = AssocChangeEvent.SHUTDOWN;
                break;
            case SCTP_CANT_START :
                this.event = AssocChangeEvent.CANT_START;
                break;
            default :
                throw new AssertionError(
                      "Unknown Association Change Event type: " + intEvent);
        }

        this.assocId = assocId;
        this.maxOutStreams = maxOutStreams;
        this.maxInStreams = maxInStreams;
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
    public Association association() {
        assert event == AssocChangeEvent.CANT_START ? true : association != null;
        return association;
    }

    @Override
    public AssocChangeEvent event() {
        return event;
    }

    int maxOutStreams() {
        return maxOutStreams;
    }

    int maxInStreams() {
        return maxInStreams;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" [");
        sb.append("Association:").append(association);
        sb.append(", Event: ").append(event).append("]");
        return sb.toString();
    }
}
