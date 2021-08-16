/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import sun.security.util.HexDumpEncoder;

/**
  * Base class that defines common fields, constants, and debug method.
  *
  * @author Jagane Sundar
  */
public abstract class Ber {

    protected byte buf[];
    protected int offset;
    protected int bufsize;

    protected Ber() {
    }

    public static void dumpBER(OutputStream outStream, String tag, byte[] bytes,
        int from, int to) {

        try {
            outStream.write('\n');
            outStream.write(tag.getBytes("UTF8"));

            new HexDumpEncoder().encodeBuffer(
                new ByteArrayInputStream(bytes, from, to),
                outStream);

            outStream.write('\n');
        } catch (IOException e) {
            try {
                outStream.write(
                    "Ber.dumpBER(): error encountered\n".getBytes("UTF8"));
            } catch (IOException e2) {
                // ignore
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // some ASN defines
    //
    ////////////////////////////////////////////////////////////////////////////

    public static final int ASN_BOOLEAN         = 0x01;
    public static final int ASN_INTEGER         = 0x02;
    public static final int ASN_BIT_STRING      = 0x03;
    public static final int ASN_SIMPLE_STRING   = 0x04;
    public static final int ASN_OCTET_STR       = 0x04;
    public static final int ASN_NULL            = 0x05;
    public static final int ASN_OBJECT_ID       = 0x06;
    public static final int ASN_SEQUENCE        = 0x10;
    public static final int ASN_SET             = 0x11;


    public static final int ASN_PRIMITIVE       = 0x00;
    public static final int ASN_UNIVERSAL       = 0x00;
    public static final int ASN_CONSTRUCTOR     = 0x20;
    public static final int ASN_APPLICATION     = 0x40;
    public static final int ASN_CONTEXT         = 0x80;
    public static final int ASN_PRIVATE         = 0xC0;

    public static final int ASN_ENUMERATED      = 0x0a;

    static final class EncodeException extends IOException {
        private static final long serialVersionUID = -5247359637775781768L;
        EncodeException(String msg) {
            super(msg);
        }
    }

    static final class DecodeException extends IOException {
        private static final long serialVersionUID = 8735036969244425583L;
        DecodeException(String msg) {
            super(msg);
        }
    }
}
