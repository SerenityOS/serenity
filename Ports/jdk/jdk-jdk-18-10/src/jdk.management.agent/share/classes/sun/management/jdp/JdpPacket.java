/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.management.jdp;

import java.io.IOException;

/**
 * Packet to broadcast
 *
 * <p>Each packet have to contain MAGIC and PROTOCOL_VERSION in order to be
 * recognized as a valid JDP packet.</p>
 *
 * <p>Default implementation build packet as a set of UTF-8 encoded Key/Value pairs
 * are stored as an ordered list of values, and are sent to the server
 * in that order.</p>
 *
 * <p>
 * Packet structure:
 *
 * 4 bytes JDP magic (0xC0FFE42)
 * 2 bytes JDP protocol version (01)
 *
 * 2 bytes size of key
 * x bytes key (UTF-8 encoded)
 * 2 bytes size of value
 * x bytes value (UTF-8 encoded)
 *
 * repeat as many times as necessary ...
 * </p>
  */
public interface JdpPacket {

    /**
     * This method responsible to assemble packet and return a byte array
     * ready to be sent across a Net.
     *
     * @return assembled packet as an array of bytes
     * @throws IOException
     */
    public byte[] getPacketData() throws IOException;

}
