/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

public enum Protocol {

    SSLV2HELLO(0x0002, "SSLv2Hello"),
    SSLV3     (0x0300, "SSLv3"),
    TLSV1     (0x0301, "TLSv1"),
    TLSV1_1   (0x0302, "TLSv1.1"),
    TLSV1_2   (0x0303, "TLSv1.2"),
    TLSV1_3   (0x0304, "TLSv1.3"),

    DTLS1_3   (0xFEFC, "DTLSv1.3"),
    DTLS1_2   (0xFEFD, "DTLSv1.2"),
    DTLS1_0   (0xFEFF, "DTLSv1.0");

    public final int id;
    public final String name;

    private Protocol(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static Protocol protocol(String name) {
        for (Protocol protocol : values()) {
            if (protocol.name.equals(name)) {
                return protocol;
            }
        }

        return null;
    }
}
