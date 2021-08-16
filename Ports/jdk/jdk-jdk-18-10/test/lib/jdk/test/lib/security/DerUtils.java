/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.security;

import jdk.test.lib.Asserts;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.KnownOIDs;
import sun.security.util.ObjectIdentifier;

import java.io.IOException;

public class DerUtils {
    /**
     * Returns a DerValue (deep) inside another DerValue.
     * <p>
     * The location of the inner DerValue is expressed as a string, in which
     * each character is a step from the outer DerValue into the inner one.
     * If it's a number n, the n'th element (starting from 0) of a sequence
     * is the next step. If it's 'c', the content of an OctetString parsed
     * as a DerValue is the next step. Note that n cannot be bigger than 9.
     * <p>
     * Attention: do not reuse the return value. DerValue is mutable and
     * reading it advances a pointer inside.
     * <p>
     * For example, here is a PKCS #12 file:
     * <pre>
     * 0000:0845  [] SEQUENCE
     * 0004:0003  [0]     INTEGER 3
     * 0007:07FE  [1]     SEQUENCE
     * 000B:000B  [10]         OID 1.2.840.113549.1.7.1 (data)
     * 0016:07EF  [11]         cont [0]
     * 001A:07EB  [110]             OCTET STRING
     * ...
     * </pre>
     * and the content of OCTET string at offset 001A can be parsed as another
     * DerValue which is:
     * <pre>
     * 0000:07E7  [] SEQUENCE
     * 0004:0303  [0]     SEQUENCE
     * 0008:000B  [00]         OID 1.2.840.113549.1.7.1 (data)
     * ....
     * </pre>
     * Then the OID is {@code innerDerValue(data, "110c00").getOID()}.
     *
     * @param data the outer DerValue. We choose byte[] instead of DerValue
     *             because DerValue is mutable and cannot be reused.
     * @param location the location of the inner DerValue
     * @return the inner DerValue, or null if no DerValue is at the location
     * @throws IOException if an I/O error happens
     */
    public static DerValue innerDerValue(byte[] data, String location)
            throws IOException {

        DerValue v  = new DerValue(data);
        for (char step : location.toCharArray()) {
            if (step == 'c') {
                v = new DerValue(v.getOctetString());
            } else {
                DerInputStream ins = v.getData();
                // skip n DerValue in the sequence
                for (int i = 0; i < step - '0'; i++) {
                    ins.getDerValue();
                }
                if (ins.available() > 0) {
                    v = ins.getDerValue();
                } else {
                    return null;
                }
            }
        }
        return v;
    }

    /**
     * Ensures that the inner DerValue is the expected ObjectIdentifier.
     */
    public static void checkAlg(byte[] der, String location,
            ObjectIdentifier expected) throws Exception {
        Asserts.assertEQ(innerDerValue(der, location).getOID(), expected);
    }

    /**
     * Ensures that the inner DerValue is the expected ObjectIdentifier.
     */
    public static void checkAlg(byte[] der, String location,
            KnownOIDs expected) throws Exception {
        Asserts.assertEQ(innerDerValue(der, location).getOID(),
                ObjectIdentifier.of(expected));
    }

    /**
     * Ensures that the inner DerValue is the expected integer.
     */
    public static void checkInt(byte[] der, String location, int expected)
            throws Exception {
        Asserts.assertEQ(innerDerValue(der, location).getInteger(), expected);
    }

    /**
     * Ensures that there is no inner DerValue at the specified location.
     */
    public static void shouldNotExist(byte[] der, String location)
            throws Exception {
        Asserts.assertTrue(innerDerValue(der, location) == null);
    }
}
