/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8011867 8242151
 * @summary Accept unknown PKCS #9 attributes
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.util
 */

import java.io.*;
import java.util.Arrays;

import sun.security.pkcs.PKCS9Attribute;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import jdk.test.lib.hexdump.HexPrinter;

public class UnknownAttribute {

    public static void main(String[] args) throws Exception {
        // Unknown attr
        PKCS9Attribute p1 = new PKCS9Attribute(
                PKCS9Attribute.CHALLENGE_PASSWORD_OID, "t0p5ecr3t");
        if (!p1.isKnown()) {
            throw new Exception();
        }
        // Unknown attr from DER
        byte[] data = {
                0x30, 0x08,                 // SEQUENCE OF
                0x06, 0x02, 0x2A, 0x03,     // OID 1.2.3 and
                0x31, 0x02, 0x05, 0x00      // an empty SET
        };
        PKCS9Attribute p2 = new PKCS9Attribute(new DerValue(data));
        if (p2.isKnown()) {
            throw new Exception();
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        p2.derEncode(bout);
        HexPrinter.simple().dest(System.err).format(bout.toByteArray());
        if (!Arrays.equals(data, bout.toByteArray())) {
            throw new Exception();
        }
        // Unknown attr from value
        try {
            new PKCS9Attribute(ObjectIdentifier.of("1.2.3"), "hello");
            throw new Exception();
        } catch (IllegalArgumentException iae) {
            // Good. Unknown attr must have byte[] value type
        }
        PKCS9Attribute p3 = new PKCS9Attribute(
                ObjectIdentifier.of("1.2.3"), new byte[]{0x31,0x02,0x05,0x00});
        if (p3.isKnown()) {
            throw new Exception();
        }
        bout = new ByteArrayOutputStream();
        p3.derEncode(bout);
        if (!Arrays.equals(data, bout.toByteArray())) {
            throw new Exception();
        }
    }
}
