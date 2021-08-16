/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

/**
 * Enum for SSL/(D)TLS content types.
 */
enum ContentType {
    INVALID             ((byte)0,   "invalid",
                            ProtocolVersion.PROTOCOLS_OF_13),
    CHANGE_CIPHER_SPEC  ((byte)20,  "change_cipher_spec",
                            ProtocolVersion.PROTOCOLS_TO_12),
    ALERT               ((byte)21,  "alert",
                            ProtocolVersion.PROTOCOLS_TO_13),
    HANDSHAKE           ((byte)22,  "handshake",
                            ProtocolVersion.PROTOCOLS_TO_13),
    APPLICATION_DATA    ((byte)23,  "application_data",
                            ProtocolVersion.PROTOCOLS_TO_13);

    final byte id;
    final String name;
    final ProtocolVersion[] supportedProtocols;

    private ContentType(byte id, String name,
            ProtocolVersion[] supportedProtocols) {
        this.id = id;
        this.name = name;
        this.supportedProtocols = supportedProtocols;
    }

    static ContentType valueOf(byte id) {
        for (ContentType ct : ContentType.values()) {
            if (ct.id == id) {
                return ct;
            }
        }

        return null;
    }

    static String nameOf(byte id) {
        for (ContentType ct : ContentType.values()) {
            if (ct.id == id) {
                return ct.name;
            }
        }

        return "<UNKNOWN CONTENT TYPE: " + (id & 0x0FF) + ">";
    }
}
