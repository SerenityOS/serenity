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

/*
 * @test
 * @bug 8165751
 * @summary Verify that that a subclass of Signature that does not contain a
 *     provider can be used to verify.
 * @run main/othervm -Djava.security.debug=provider NoProvider
 */

import java.security.*;

public class NoProvider {

    private static class NoProviderPublicKey implements PublicKey {

        public String getAlgorithm() {
            return "NoProvider";
        }
        public String getFormat() {
            return "none";
        }
        public byte[] getEncoded() {
            return new byte[1];
        }
    }

    private static class NoProviderSignature extends Signature {

        public NoProviderSignature() {
            super("NoProvider");
        }

        protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException {
            // do nothing
        }

        protected void engineInitSign(PrivateKey privateKey)
            throws InvalidKeyException {
            // do nothing
        }

        protected void engineUpdate(byte b) throws SignatureException {
            // do nothing
        }

        protected void engineUpdate(byte[] b, int off, int len)
            throws SignatureException {
            // do nothing
        }

        protected byte[] engineSign() throws SignatureException {
            return new byte[1];
        }

        protected boolean engineVerify(byte[] sigBytes)
            throws SignatureException {
            return false;
        }

        @Deprecated
        protected void engineSetParameter(String param, Object value)
            throws InvalidParameterException {
            // do nothing
        }

        @Deprecated
        protected Object engineGetParameter(String param)
            throws InvalidParameterException {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        new NoProviderSignature().initVerify(new NoProviderPublicKey());
    }
}
