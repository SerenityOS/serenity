/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182999
 * @summary Ensure that SunEC behaves correctly for unsupported curves.
 * @run main/othervm -Djdk.sunec.disableNative=false InvalidCurve
 */

import java.security.*;
import java.security.spec.*;
import java.math.*;

public class InvalidCurve {

    public static void main(String[] args) {

        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance("EC", "SunEC");
            ECGenParameterSpec brainpoolSpec =
                new ECGenParameterSpec("brainpoolP160r1");
            keyGen.initialize(brainpoolSpec);
        } catch (InvalidAlgorithmParameterException ex) {
            System.out.println(ex.getMessage());
            // this is expected
            return;
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new RuntimeException(ex);
        }

        keyGen.generateKeyPair();

        // If we make it to here, then the test is not working correctly.
        throw new RuntimeException("The expected exception was not thrown.");

    }

}

