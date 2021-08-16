/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7087021 8013069
 * @summary Clone tests for all MAC algorithms.
 * @author Jan Luehe
 */
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.*;

public class MacClone {

    public static void main(String[] args) throws Exception {

        String[] algos = { "HmacMD5", "HmacSHA1", "HmacSHA224", "HmacSHA256",
                           "HmacSHA384", "HmacSHA512" };
        KeyGenerator kgen = KeyGenerator.getInstance("DES");
        SecretKey skey = kgen.generateKey();
        for (String algo : algos) {
            doTest(algo, skey, null);
        }

        String[] algos2 = { "HmacPBESHA1", "PBEWithHmacSHA1",
                            "PBEWithHmacSHA224", "PBEWithHmacSHA256",
                            "PBEWithHmacSHA384", "PBEWithHmacSHA512" };
        skey = new SecretKeySpec("whatever".getBytes(), "PBE");
        PBEParameterSpec params =
            new PBEParameterSpec("1234567890".getBytes(), 500);
        for (String algo : algos2) {
            doTest(algo, skey, params);
        }
        System.out.println("Test Passed");
    }

    private static void doTest(String algo, SecretKey skey,
        AlgorithmParameterSpec params) throws Exception {
        //
        // Clone an uninitialized Mac object
        //
        Mac mac = Mac.getInstance(algo, "SunJCE");
        Mac macClone = (Mac)mac.clone();
        System.out.println(macClone.getProvider().toString());
        System.out.println(macClone.getAlgorithm());
        boolean thrown = false;
        try {
            macClone.update((byte)0x12);
        } catch (IllegalStateException ise) {
            thrown = true;
        }
        if (!thrown) {
            throw new Exception("Expected IllegalStateException not thrown");
        }

        //
        // Clone an initialized Mac object
        //
        mac = Mac.getInstance(algo, "SunJCE");
        mac.init(skey, params);
        macClone = (Mac)mac.clone();
        System.out.println(macClone.getProvider().toString());
        System.out.println(macClone.getAlgorithm());
        mac.update((byte)0x12);
        macClone.update((byte)0x12);
        byte[] macFinal = mac.doFinal();
        byte[] macCloneFinal = macClone.doFinal();
        if (!java.util.Arrays.equals(macFinal, macCloneFinal)) {
            throw new Exception("ERROR: MAC result of init clone is different");
        } else System.out.println("MAC check#1 passed");

        //
        // Clone an updated Mac object
        //
        mac.update((byte)0x12);
        macClone = (Mac)mac.clone();
        mac.update((byte)0x34);
        macClone.update((byte)0x34);
        macFinal = mac.doFinal();
        macCloneFinal = macClone.doFinal();
        if (!java.util.Arrays.equals(macFinal, macCloneFinal)) {
            throw new Exception("ERROR: MAC result of updated clone is different");
        } else System.out.println("MAC check#2 passed");
    }
}
