/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2013 IBM Corporation
 */

/*
 * @test
 * @bug 8014620 8130181
 * @summary Signature.getAlgorithm return null in special case
 * @run main/othervm SignatureGetAlgorithm
 * @author youdwei
 */
import java.security.*;

public class SignatureGetAlgorithm {

    public static void main(String[] args) throws Exception {
        Provider testProvider = new TestProvider();
        Security.addProvider(testProvider);
        Signature sig = Signature.getInstance("MySignatureAlg");
        String algorithm = sig.getAlgorithm();
        System.out.println("Algorithm Name: " + algorithm);
        if (algorithm == null) {
            throw new Exception("algorithm name should be 'MySignatureAlg'");
        }
    }

    public static class TestProvider extends Provider {
        TestProvider() {
            super("testSignatureGetAlgorithm", "1.0", "test Signatures");
            put("Signature.MySignatureAlg",
                "SignatureGetAlgorithm$MySignatureAlg");
        }
    }

    public static class MySignatureAlg extends Signature {

        public MySignatureAlg() {
            super(null);
        }

        MySignatureAlg(String s) {
            super(s);
        }

        @Override
        protected void engineInitVerify(PublicKey publicKey)
                throws InvalidKeyException {
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey)
                throws InvalidKeyException {
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
        }

        @Override
        protected void engineUpdate(byte[] b, int off, int len)
                throws SignatureException {
        }

        @Override
        protected byte[] engineSign()
                throws SignatureException {
            return new byte[0];
        }

        @Override
        protected boolean engineVerify(byte[] sigBytes)
                throws SignatureException {
            return false;
        }

        @Override
        @Deprecated
        protected void engineSetParameter(String param, Object value)
                throws InvalidParameterException {
        }

        @Override
        @Deprecated
        protected Object engineGetParameter(String param)
                throws InvalidParameterException {
            return null;
        }
    }
}
