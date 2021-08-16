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
 * @bug 4856968 7054918 8130181
 * @library ../testlibrary
 * @summary make sure getInstance() works correctly, including failover
 *   and delayed provider selection for Signatures
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;
import java.security.cert.*;

public class GetInstance {

    private static void same(Provider p1, Provider p2) throws Exception {
        if (p1 != p2) {
            throw new Exception("Wrong provider");
        }
    }

    public static void main(String[] args) throws Exception {
        ProvidersSnapshot snapshot = ProvidersSnapshot.create();
        try {
            main0(args);
        } finally {
            snapshot.restore();
        }
    }

    public static void main0(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        Provider foo = new FooProvider();
        Provider bar = new BarProvider();
        Provider baz = new BazProvider();

        Security.addProvider(foo);
        Security.addProvider(bar);
        Security.addProvider(baz);

        System.out.println("Testing MessageDigest.getInstance()...");
        MessageDigest m;
        m = MessageDigest.getInstance("foo");
        m = MessageDigest.getInstance("foo", "foo");
        m = MessageDigest.getInstance("foo", foo);

        System.out.println("Testing Signature.getInstance() for SPI...");
        Signature sig;
        PrivateKey privateKey = new FooPrivateKey();
        sig = Signature.getInstance("foo");
        same(foo, sig.getProvider());
        sig = Signature.getInstance("foo");
        sig.initSign(privateKey);
        same(foo, sig.getProvider());
        sig = Signature.getInstance("foo", "foo");
        sig.initSign(privateKey);
        same(foo, sig.getProvider());
        sig = Signature.getInstance("foo", foo);
        sig.initSign(privateKey);
        same(foo, sig.getProvider());

        System.out.println("Testing Signature.getInstance() for Signature...");
        sig = Signature.getInstance("fuu");
        same(foo, sig.getProvider());
        sig = Signature.getInstance("fuu");
        sig.initSign(privateKey);
        same(foo, sig.getProvider());
        sig = Signature.getInstance("fuu", "foo");
        sig.initSign(privateKey);
        same(foo, sig.getProvider());
        sig = Signature.getInstance("fuu", foo);
        sig.initSign(privateKey);
        same(foo, sig.getProvider());

        System.out.println("Testing CertStore.getInstance()...");
        CertStoreParameters params = new CollectionCertStoreParameters(Collections.EMPTY_LIST);
        CertStore cs;
        cs = CertStore.getInstance("foo", params);
        cs = CertStore.getInstance("foo", params, "foo");
        cs = CertStore.getInstance("foo", params, foo);

        System.out.println("Testing failover...");
        m = MessageDigest.getInstance("bar");
        same(m.getProvider(), baz);
        sig = Signature.getInstance("bar");
        same(sig.getProvider(), baz);
        cs = CertStore.getInstance("bar", params);
        same(cs.getProvider(), baz);

        System.out.println("Testing Signature delayed provider selection...");
        sig = Signature.getInstance("baz");
        sig.initVerify(new FooPublicKey());
        same(sig.getProvider(), baz);

        Provider.Service s = foo.getService("CertStore", "foo");
        s.newInstance(null);
        s.newInstance(params);
        try {
            s.newInstance(0);
            throw new Exception("call should not succeed");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause instanceof InvalidParameterException == false) {
                throw new Exception("incorrect exception");
            }
        }

        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    public static class FooProvider extends Provider {
        FooProvider() {
            super("foo", "1.0", "none");
            put("MessageDigest.foo", "GetInstance$FooDigest");
            put("CertStore.foo",     "GetInstance$FooStore");
            put("Signature.foo",     "GetInstance$FooSignatureSpi");

            put("Signature.fuu",     "GetInstance$BazSignature");

            // throws InvalidKeyException, skipped in delayed provider selection
            put("Signature.baz",     "GetInstance$BazSignatureSpi");
        }
    }

    public static class BarProvider extends Provider {
        BarProvider() {
            super("bar", "1.0", "none");
            // all entries invalid for failover
            put("MessageDigest.bar", "GetInstance$FooKey");
            put("Signature.bar",     "GetInstance$FooKey");
            put("Certstore.bar",     "GetInstance$FooKey");

            // not an SPI, skipped in delayed provider selection
            put("Signature.baz",     "GetInstance$BazSignature");
        }
    }

    public static class BazProvider extends Provider {
        BazProvider() {
            super("baz", "1.0", "none");
            put("MessageDigest.bar", "GetInstance$FooDigest");
            put("CertStore.bar",     "GetInstance$FooStore");
            put("Signature.bar",     "GetInstance$FooSignatureSpi");

            put("Signature.baz",     "GetInstance$FooSignatureSpi");
        }
    }

    public static class FooDigest extends MessageDigestSpi {
        public byte[] engineDigest() { return new byte[0]; }
        public void engineReset() {}
        public void engineUpdate(byte input) {}
        public void engineUpdate(byte[] b, int ofs, int len) {}
    }

    public static class FooStore extends CertStoreSpi {
        public FooStore(CertStoreParameters params) throws InvalidAlgorithmParameterException { super(params); }
        public Collection engineGetCertificates(CertSelector sel) { return Collections.EMPTY_LIST; }
        public Collection engineGetCRLs(CRLSelector sel) { return Collections.EMPTY_LIST; }
    }

    public static class BaseSignatureSpi extends SignatureSpi {
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        }
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        }
        protected void engineUpdate(byte b) throws SignatureException { }
        protected void engineUpdate(byte[] b, int off, int len) throws SignatureException { }
        protected byte[] engineSign() throws SignatureException {
            return new byte[0];
        }
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            return false;
        }
        protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        }
        protected Object engineGetParameter(String param) throws InvalidParameterException {
            return null;
        }
    }

    public static class BaseSignature extends Signature {
        BaseSignature(String s) {
            super(s);
        }
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
            //
        }
        protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException { }
        protected void engineUpdate(byte b) throws SignatureException { }
        protected void engineUpdate(byte[] b, int off, int len) throws SignatureException { }
        protected byte[] engineSign() throws SignatureException {
            return new byte[0];
        }
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            return false;
        }
        protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        }
        protected Object engineGetParameter(String param) throws InvalidParameterException {
            return null;
        }
    }

    public static abstract class FooKey implements Key {
        public String getFormat() { return null; }
        public byte[] getEncoded() { return null; }
        public String getAlgorithm() { return "foo"; }
    }

    public static class FooPrivateKey extends FooKey implements PrivateKey { }

    public static class FooPublicKey extends FooKey implements PublicKey { }

    public static class FooSignatureSpi extends BaseSignatureSpi {
        public FooSignatureSpi() {
            super();
            System.out.println("FooSignatureSpi constructor");
        }
    }

    public static class BazSignatureSpi extends BaseSignatureSpi {
        public BazSignatureSpi() {
            super();
            System.out.println("BazSignatureSpi constructor");
        }
        protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
            throw new InvalidKeyException("verify not supported");
        }
    }

    public static class BazSignature extends BaseSignature {
        public BazSignature() {
            super("baz");
            System.out.println("BazSignature constructor");
        }
    }

}
