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

/**
 * JdpGenericPacket responsible to provide fields
 * common for all Jdp packets
 */
public abstract class JdpGenericPacket implements JdpPacket {

    /**
     * JDP protocol magic. Magic allows a reader to quickly select
     * JDP packets from a bunch of broadcast packets addressed to the same port
     * and broadcast group. Any packet intended to be parsed by JDP client
     * has to start from this  magic.
     */
    private static final int MAGIC = 0xC0FFEE42;

    /**
     * Current version of protocol. Any implementation of this protocol has to
     * conform with the packet structure and the flow described in JEP-168
     */
    private static final short PROTOCOL_VERSION = 1;

    /**
     * Default do-nothing constructor
     */
    protected  JdpGenericPacket(){
        // do nothing
    }


    /**
     * Validate protocol header magic field
     *
     * @param magic - value to validate
     * @throws JdpException
     */
    public static void checkMagic(int magic)
            throws JdpException {
        if (magic != MAGIC) {
            throw new JdpException("Invalid JDP magic header: " + magic);
        }
    }

    /**
     * Validate protocol header version field
     *
     * @param version - value to validate
     * @throws JdpException
     */
    public static void checkVersion(short version)
            throws JdpException {

        if (version > PROTOCOL_VERSION) {
            throw new JdpException("Unsupported protocol version: " + version);
        }
    }

    /**
     *
     * @return protocol magic
     */
    public static int getMagic() {
        return MAGIC;
    }

    /**
     *
     * @return current protocol version
     */
    public static short getVersion() {
        return PROTOCOL_VERSION;
    }
}
