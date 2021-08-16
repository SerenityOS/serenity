/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7040151
 * @summary SPNEGO GSS code does not parse tokens in accordance to RFC 2478
 * @modules java.security.jgss/sun.security.jgss.spnego
 * @compile -XDignore.symbol.file NegTokenTargFields.java
 * @run main NegTokenTargFields nomech
 * @run main/fail NegTokenTargFields badorder
 */

import sun.security.jgss.spnego.NegTokenTarg;

public class NegTokenTargFields {

    // A hand-crafted NegTokenTarg with negResult and responseToken only
    public static byte[] nomech = {
        (byte)0xA1, (byte)0x0F, (byte)0x30, (byte)0x0D,
        (byte)0xA0, (byte)0x03, (byte)0x0A, (byte)0x01,
        (byte)0x02, (byte)0xA2, (byte)0x02, (byte)0x04,
        (byte)0x00, (byte)0xA3, (byte)0x02, (byte)0x04,
        (byte)0x00,
    };

    // A hand-crafted NegTokenTarg with negResult and supportedMech in wrong order
    public static byte[] badorder = {
        (byte)0xA1, (byte)0x1E, (byte)0x30, (byte)0x1C,
        (byte)0xA1, (byte)0x0B, (byte)0x06, (byte)0x09,
        (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86,
        (byte)0xF7, (byte)0x12, (byte)0x01, (byte)0x02,
        (byte)0x02, (byte)0xA0, (byte)0x03, (byte)0x0A,
        (byte)0x01, (byte)0x00, (byte)0xA2, (byte)0x03,
        (byte)0x04, (byte)0x01, (byte)0x00, (byte)0xA3,
        (byte)0x03, (byte)0x04, (byte)0x01, (byte)0x00,
    };

    public static void main(String[] args) throws Exception {
        byte[] buf = (byte[])NegTokenTargFields.class.getField(args[0]).get(null);
        new NegTokenTarg(buf);
    }
}
