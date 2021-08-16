/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4937853 8170876
 * @summary Make sure normal calls of NullCipher does not throw NPE.
 * @author Valerie Peng
 * @key randomness
 * @run main TestNPE
 * @run main/othervm -Djava.security.debug=provider TestNPE
 */
import java.util.Arrays;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.NullCipher;
import javax.crypto.spec.SecretKeySpec;

public class TestNPE {
    private static byte[] BYTES = new byte[16];
    static {
        new SecureRandom().nextBytes(BYTES);
    }

    public static void main(String[] args) throws Exception {
        NullCipher nc = new NullCipher();
        // testing init(...)
        nc.init(Cipher.ENCRYPT_MODE, (Certificate) null);
        nc.init(Cipher.ENCRYPT_MODE, (Certificate) null, (SecureRandom) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null, (AlgorithmParameters) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null, (AlgorithmParameterSpec) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null, (AlgorithmParameterSpec) null,
            (SecureRandom) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null, (AlgorithmParameters) null,
            (SecureRandom) null);
        nc.init(Cipher.ENCRYPT_MODE, (Key) null, (SecureRandom) null);
        // testing getBlockSize()
        if (nc.getBlockSize() != 1) {
            throw new Exception("Error with getBlockSize()");
        }
        // testing update(...)
        byte[] out = nc.update(BYTES);
        if (!Arrays.equals(out, BYTES)) {
            throw new Exception("Error with update(byte[])");
        }
        out = nc.update(BYTES, 0, BYTES.length);
        if (!Arrays.equals(out, BYTES)) {
            throw new Exception("Error with update(byte[], int, int)");
        }
        if (nc.update(BYTES, 0, BYTES.length, out) != BYTES.length) {
            throw new Exception("Error with update(byte[], int, int, byte[])");
        }
        if (nc.update(BYTES, 0, BYTES.length, out, 0) != BYTES.length) {
            throw new Exception(
                "Error with update(byte[], int, int, byte[], int)");
        }
        // testing doFinal(...)
        if (nc.doFinal() != null) {
            throw new Exception("Error with doFinal()");
        }
        if (nc.doFinal(out, 0) != 0) {
             throw new Exception("Error with doFinal(byte[], 0)");
        }
        out = nc.doFinal(BYTES);
        if (!Arrays.equals(out, BYTES)) {
            throw new Exception("Error with doFinal(byte[])");
        }
        out = nc.doFinal(BYTES, 0, BYTES.length);
        if (!Arrays.equals(out, BYTES)) {
            throw new Exception("Error with doFinal(byte[], int, int)");
        }
        if (nc.doFinal(BYTES, 0, BYTES.length, out) != BYTES.length) {
            throw new Exception(
                "Error with doFinal(byte[], int, int, byte[])");
        }
        if (nc.doFinal(BYTES, 0, BYTES.length, out, 0) != BYTES.length) {
            throw new Exception(
                "Error with doFinal(byte[], int, int, byte[], int)");
        }
        // testing getExemptionMechanism()
        if (nc.getExemptionMechanism() != null) {
            throw new Exception("Error with getExemptionMechanism()");
        }
        // testing getOutputSize(int)
        if (nc.getOutputSize(5) != 5) {
            throw new Exception("Error with getOutputSize()");
        }
        // testing getIV(), getParameters(), getAlgorithm(), etc.
        if (nc.getIV() == null) { // should've been null;
                                  // left it as is for backward-compatibility
            throw new Exception("Error with getIV()");
        }
        if (nc.getParameters() != null) {
            throw new Exception("Error with getParameters()");
        }
        if (nc.getAlgorithm() != null) {
            throw new Exception("Error with getAlgorithm()");
        }
        if (nc.getProvider() != null) { // not associated with any provider
            throw new Exception("Error with getProvider()");
        }
        System.out.println("Test Done");
    }
}
