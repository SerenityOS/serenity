/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216039
 * @summary Ensure the BC provider-reselection workaround in Signature class
 *     functions correctly
 * @modules java.base/sun.security.util
 * @run main/othervm SignatureGetInstance
 */
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import sun.security.util.SignatureUtil;

public class SignatureGetInstance {

    private static final String SIGALG = "RSASSA-PSS";

    public static void main(String[] args) throws Exception {
        Provider testProvider = new TestProvider();
        // put test provider before SunRsaSign provider
        Security.insertProviderAt(testProvider, 1);
        //Security.addProvider(testProvider);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair kp = kpg.generateKeyPair();

        MyPrivKey testPriv = new MyPrivKey();
        MyPubKey testPub = new MyPubKey();

        testDblInit(testPriv, testPub, true, "TestProvider");
        testDblInit(kp.getPrivate(), kp.getPublic(), true, "SunRsaSign");
        testDblInit(testPriv, kp.getPublic(), false, null);
        testDblInit(kp.getPrivate(), testPub, false, null);

        testSetAndInit(null, testPriv, true);
        testSetAndInit(null, testPub, true);
        testSetAndInit(null, kp.getPrivate(), true);
        testSetAndInit(null, kp.getPublic(), true);

        String provName = "SunRsaSign";
        testSetAndInit(provName, testPriv, false);
        testSetAndInit(provName, testPub, false);
        testSetAndInit(provName, kp.getPrivate(), true);
        testSetAndInit(provName, kp.getPublic(), true);

        provName = "TestProvider";
        testSetAndInit(provName, testPriv, true);
        testSetAndInit(provName, testPub, true);
        testSetAndInit(provName, kp.getPrivate(), false);
        testSetAndInit(provName, kp.getPublic(), false);

        System.out.println("Test Passed");
    }

    private static void checkName(Signature s, String name) {
        if (name != null &&
            !(name.equals(s.getProvider().getName()))) {
            throw new RuntimeException("Fail: provider name mismatch");
        }
    }

    private static void testDblInit(PrivateKey key1, PublicKey key2,
            boolean shouldPass, String expectedProvName) throws Exception {
        Signature sig = Signature.getInstance(SIGALG);
        SignatureUtil.initSignWithParam(sig, key1, PSSParameterSpec.DEFAULT, null);
        try {
            sig.initVerify(key2);
            if (!shouldPass) {
                throw new RuntimeException("Fail: should throw InvalidKeyException");
            }
            checkName(sig, expectedProvName);
        } catch (InvalidKeyException ike) {
            if (shouldPass) {
                System.out.println("Fail: Unexpected InvalidKeyException");
                throw ike;
            }
        }
    }

    private static void testSetAndInit(String provName, Key key,
            boolean shouldPass) throws Exception {
        Signature sig;
        if (provName == null) {
            sig = Signature.getInstance(SIGALG);
        } else {
            sig = Signature.getInstance(SIGALG, provName);
        }
        AlgorithmParameterSpec params = PSSParameterSpec.DEFAULT;
        boolean doSign = (key instanceof PrivateKey);
        try {
            if (doSign) {
                SignatureUtil.initSignWithParam(sig, (PrivateKey)key, params, null);
            } else {
                SignatureUtil.initVerifyWithParam(sig, (PublicKey)key, params);
            }
            if (!shouldPass) {
                throw new RuntimeException("Fail: should throw InvalidKeyException");
            }
            checkName(sig, provName);
            // check that the earlier parameter is still there
            if (sig.getParameters() == null) {
                throw new RuntimeException("Fail: parameters not preserved");
            }
        } catch (InvalidKeyException ike) {
            if (shouldPass) {
                System.out.println("Fail: Unexpected InvalidKeyException");
                throw ike;
            }
        }
    }

    // Test provider which only accepts its own Key objects
    // Registered to be more preferred than SunRsaSign provider
    // for testing deferred provider selection
    public static class TestProvider extends Provider {
        TestProvider() {
            super("TestProvider", "1.0", "provider for SignatureGetInstance");
            put("Signature.RSASSA-PSS",
                "SignatureGetInstance$MySigImpl");
        }
    }

    public static class MyPrivKey implements PrivateKey {
        public String getAlgorithm() { return "RSASSA-PSS"; }
        public String getFormat() { return "MyOwn"; }
        public byte[] getEncoded() { return null; }
    }

    public static class MyPubKey implements PublicKey {
        public String getAlgorithm() { return "RSASSA-PSS"; }
        public String getFormat() { return "MyOwn"; }
        public byte[] getEncoded() { return null; }
    }

    public static class MySigImpl extends SignatureSpi {
        // simulate BC behavior of only using params set before init calls
        AlgorithmParameterSpec initParamSpec = null;
        AlgorithmParameterSpec paramSpec = null;

        public MySigImpl() {
            super();
        }

        @Override
        protected void engineInitVerify(PublicKey publicKey)
                throws InvalidKeyException {
            if (!(publicKey instanceof MyPubKey)) {
                throw new InvalidKeyException("Must be MyPubKey");
            }
            initParamSpec = paramSpec;
        }

        @Override
        protected void engineInitSign(PrivateKey privateKey)
                throws InvalidKeyException {
            if (!(privateKey instanceof MyPrivKey)) {
                throw new InvalidKeyException("Must be MyPrivKey");
            }
            initParamSpec = paramSpec;
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
        protected void engineSetParameter(AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            paramSpec = params;
        }

        @Override
        @Deprecated
        protected AlgorithmParameters engineGetParameter(String param)
                throws InvalidParameterException {
            return null;
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            if (initParamSpec != null) {
                try {
                    AlgorithmParameters ap =
                        AlgorithmParameters.getInstance("RSASSA-PSS");
                    ap.init(initParamSpec);
                    return ap;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
    }
}
