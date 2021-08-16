/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8056174 8130181
 * @summary test the functions of JarSigner API
 * @modules java.base/sun.security.tools.keytool
 *          jdk.jartool
 */

import jdk.security.jarsigner.JarSigner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Function {
    public static void main(String[] args) throws Exception {

        try (FileOutputStream fout =new FileOutputStream("src.zip");
                ZipOutputStream zout = new ZipOutputStream(fout)) {
            zout.putNextEntry(new ZipEntry("x"));
            zout.write(new byte[10]);
            zout.closeEntry();
        }

        sun.security.tools.keytool.Main.main(
                ("-storetype jks -keystore ks -storepass changeit" +
                        " -keypass changeit -dname" +
                        " CN=RSA -alias r -genkeypair -keyalg rsa").split(" "));

        JarSigner.Builder jsb;

        try (FileInputStream fis = new FileInputStream("ks")) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(fis, "changeit".toCharArray());
            PrivateKey key = (PrivateKey)ks.getKey("r", "changeit".toCharArray());
            Certificate cert = ks.getCertificate("r");
            jsb = new JarSigner.Builder(key,
                    CertificateFactory.getInstance("X.509").generateCertPath(
                            Collections.singletonList(cert)));
        }

        jsb.digestAlgorithm("SHA1");
        jsb.signatureAlgorithm("SHA1withRSA");

        AtomicInteger counter = new AtomicInteger(0);
        StringBuilder sb = new StringBuilder();
        jsb.eventHandler(
                (a, f)->{
                    counter.incrementAndGet();
                    sb.append(a).append(' ').append(f).append('\n');
                });

        OutputStream blackHole = new OutputStream() {
            @Override
            public void write(int b) throws IOException { }
        };

        try (ZipFile src = new ZipFile("src.zip")) {
            jsb.build().sign(src, blackHole);
        }

        if (counter.get() != 4) {
            throw new Exception("Event number is " + counter.get()
                    + ":\n" + sb.toString());
        }

        // Provider test.
        Provider p = new MyProvider();
        jsb.digestAlgorithm("Five", p);
        jsb.signatureAlgorithm("SHA1WithRSA", p);
        try (ZipFile src = new ZipFile("src.zip");
                FileOutputStream out = new FileOutputStream("out.jar")) {
            jsb.build().sign(src, out);
        }

        try (JarFile signed = new JarFile("out.jar")) {
            Manifest man = signed.getManifest();
            assertTrue(man.getAttributes("x").getValue("Five-Digest").equals("FAKE"));

            Manifest sf = new Manifest(signed.getInputStream(
                    signed.getJarEntry("META-INF/SIGNER.SF")));
            assertTrue(sf.getMainAttributes().getValue("Five-Digest-Manifest")
                    .equals("FAKE"));
            assertTrue(sf.getAttributes("x").getValue("Five-Digest").equals("FAKE"));

            try (InputStream sig = signed.getInputStream(
                    signed.getJarEntry("META-INF/SIGNER.RSA"))) {
                byte[] data = sig.readAllBytes();
                assertTrue(Arrays.equals(
                        Arrays.copyOfRange(data, data.length-8, data.length),
                        "FAKEFAKE".getBytes()));
            }
        }
    }

    private static void assertTrue(boolean v) {
        if (!v) {
            throw new AssertionError();
        }
    }

    public static class MyProvider extends Provider {
        MyProvider() {
            super("MY", "1.0", null);
            put("MessageDigest.Five", Five.class.getName());
            put("Signature.SHA1WithRSA", SHA1WithRSA.class.getName());
        }
    }

    // "Five" is a MessageDigest always returns the same value
    public static class Five extends MessageDigest {
        static final byte[] dig = {0x14, 0x02, (byte)0x84}; //base64 -> FAKE
        public Five() { super("Five"); }
        protected void engineUpdate(byte input) { }
        protected void engineUpdate(byte[] input, int offset, int len) { }
        protected byte[] engineDigest() { return dig; }
        protected void engineReset() { }
    }

    // This fake "SHA1withRSA" is a Signature always returns the same value.
    // An existing name must be used otherwise PKCS7 does not which OID to use.
    public static class SHA1WithRSA extends Signature {
        static final byte[] sig = "FAKEFAKE".getBytes();
        public SHA1WithRSA() { super("SHA1WithRSA"); }
        protected void engineInitVerify(PublicKey publicKey)
                throws InvalidKeyException { }
        protected void engineInitSign(PrivateKey privateKey)
                throws InvalidKeyException { }
        protected void engineUpdate(byte b) throws SignatureException { }
        protected void engineUpdate(byte[] b, int off, int len)
                throws SignatureException { }
        protected byte[] engineSign() throws SignatureException { return sig; }
        protected boolean engineVerify(byte[] sigBytes)
                throws SignatureException {
            return Arrays.equals(sigBytes, sig);
        }
        protected void engineSetParameter(String param, Object value)
                throws InvalidParameterException { }
        protected Object engineGetParameter(String param)
                throws InvalidParameterException { return null; }
    }
}
