/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8183591
 * @summary Test decoding of DER length fields containing Integer.MAX_VALUE
 * @run main TestMaxLengthDER
 */

import java.io.*;
import java.math.*;
import java.security.*;
import java.security.spec.*;

public class TestMaxLengthDER {

    public static void main(String[] args) throws Exception {

        String message = "Message";
        Signature sig = Signature.getInstance("SHA256withDSA");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        SecureRandom rnd = new SecureRandom();
        rnd.setSeed(1);
        kpg.initialize(2048, rnd);
        KeyPair kp = kpg.generateKeyPair();
        sig.initSign(kp.getPrivate());
        sig.update(message.getBytes());
        byte[] sigData = sig.sign();

        // Set the length of the second integer to Integer.MAX_VALUE
        // First copy all the signature data to the correct location
        int lengthPos = sigData[3] + 5;
        byte[] modifiedSigData = new byte[sigData.length + 4];
        System.arraycopy(sigData, 0, modifiedSigData, 0, lengthPos);
        System.arraycopy(sigData, lengthPos + 1, modifiedSigData,
            lengthPos + 5, sigData.length - (lengthPos + 1));

        // Increase the length (in bytes) of the sequence to account for
        // the larger length field
        modifiedSigData[1] += 4;

        // Modify the length field
        modifiedSigData[lengthPos] = (byte) 0x84;
        modifiedSigData[lengthPos + 1] = (byte) 0x7F;
        modifiedSigData[lengthPos + 2] = (byte) 0xFF;
        modifiedSigData[lengthPos + 3] = (byte) 0xFF;
        modifiedSigData[lengthPos + 4] = (byte) 0xFF;

        sig.initVerify(kp.getPublic());
        sig.update(message.getBytes());

        try {
            sig.verify(modifiedSigData);
            throw new RuntimeException("No exception on misencoded signature");
        } catch (SignatureException ex) {
            if (ex.getCause() instanceof EOFException) {
                // this is expected
            } else {
                throw ex;
            }
        }
    }
}
