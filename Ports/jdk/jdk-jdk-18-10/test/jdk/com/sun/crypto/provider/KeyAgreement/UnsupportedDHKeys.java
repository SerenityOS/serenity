/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8072452
 * @summary Support DHE sizes up to 8192-bits and DSA sizes up to 3072-bits
 */

import java.math.BigInteger;

import java.security.*;
import javax.crypto.*;
import javax.crypto.interfaces.*;
import javax.crypto.spec.*;

public class UnsupportedDHKeys {

    /*
     * Sizes and values for various lengths.
     */
    private enum UnsupportedKeySize {
        // not multiple of 64
        dhp513(513),    dhp769(769),    dhp895(895),
        dhp1023(1023),  dhp1535(1535),  dhp2047(2047),

        // unsupported
        dhp2176(2176),  dhp3008(3008),  dhp4032(4032),
        dhp5120(5120),  dhp6400(6400),  dhp7680(7680),
        dhp8191(8191),  dhp8128(8128),  dhp8260(8260);

        final int primeSize;

        UnsupportedKeySize(int primeSize) {
            this.primeSize = primeSize;
        }
    }

    public static void main(String[] args) throws Exception {
        for (UnsupportedKeySize keySize : UnsupportedKeySize.values()) {
            try {
                System.out.println("Checking " + keySize.primeSize + " ...");
                KeyPairGenerator kpg =
                        KeyPairGenerator.getInstance("DH", "SunJCE");
                kpg.initialize(keySize.primeSize);

                throw new Exception("Should not support " + keySize.primeSize);
            } catch (InvalidParameterException ipe) {
                System.out.println("\tOk, unsupported");
            }
        }
    }
}
