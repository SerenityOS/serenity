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

import java.nio.ByteBuffer;
import java.net.SocketAddress;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.SendFailedNotification;

/**
 * An implementation of SendFailedNotification
 */
public class SendFailed extends SendFailedNotification
    implements SctpNotification
{
    private Association association;
    /* assocId is used to lookup the association before the notification is
     * returned to user code */
    private int assocId;
    private SocketAddress address;
    private ByteBuffer buffer;
    private int errorCode;
    private int streamNumber;

    /* Invoked from native */
    private SendFailed(int assocId,
                       SocketAddress address,
                       ByteBuffer buffer,
                       int errorCode,
                       int streamNumber) {
        this.assocId = assocId;
        this.errorCode = errorCode;
        this.streamNumber = streamNumber;
        this.address = address;
        this.buffer = buffer;
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
        /* may be null */
        return association;
    }

    @Override
    public SocketAddress address() {
        assert address != null;
        return address;
    }

    @Override
    public ByteBuffer buffer() {
        assert buffer != null;
        return buffer;
    }

    @Override
    public int errorCode() {
        return errorCode;
    }

    @Override
    public int streamNumber() {
        return streamNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" [");
        sb.append("Association:").append(association);
        sb.append(", Address: ").append(address);
        sb.append(", buffer: ").append(buffer);
        sb.append(", errorCode: ").append(errorCode);
        sb.append(", streamNumber: ").append(streamNumber);
        sb.append("]");
        return sb.toString();
    }
}
