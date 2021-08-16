/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214096 8216039
 * @summary Make sure SignatureUtil works with null algorithm parameters
 * @modules java.base/sun.security.util
 */
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import sun.security.util.SignatureUtil;

public class SetNullSigParams {

    public static void main(String[] args) throws Exception {
        Signature sig = new SpecialSigImpl();
        SignatureUtil.initVerifyWithParam(sig, (PublicKey) null, null);
        SignatureUtil.initSignWithParam(sig, null, null, null);
    }

    // Sample Signature impl class which simulates 3rd party provider behavior
    // and throws NPE when given null algorithm parameters
    // For max backward-compatibility, sun.security.util.SignatureUtil class
    // now calls setParameter() only when algorithm parameters is non-null
    private static class SpecialSigImpl extends Signature {
        SpecialSigImpl() {
            super("ANY");
        }
        @Override
        protected void engineInitVerify(PublicKey publicKey)
                throws InvalidKeyException {}
        @Override
        protected void engineInitSign(PrivateKey privateKey)
                throws InvalidKeyException {}
        @Override
        protected void engineUpdate(byte b) throws SignatureException {}
        @Override
        protected void engineUpdate(byte[] b, int off, int len)
                throws SignatureException {}
        @Override
        protected byte[] engineSign() throws SignatureException { return null; }
        @Override
        protected boolean engineVerify(byte[] sigBytes)
                throws SignatureException { return false; }
        @Override
        protected void engineSetParameter(String param, Object value)
                throws InvalidParameterException {}
        @Override
        protected void engineSetParameter(AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            if (params == null) throw new NullPointerException("Test Failed");
        }
        @Override
        protected Object engineGetParameter(String param)
                throws InvalidParameterException { return null; }
    }
}
