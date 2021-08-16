/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

/**
 * Tests various algorithm settings for PKCS12 keystores.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@Fork(jvmArgsAppend = {"-Xms1024m", "-Xmx1024m", "-Xmn768m", "-XX:+UseParallelGC"}, value = 5)
public class PKCS12KeyStores {

    private static final char[] PASS = "changeit".toCharArray();

    private Key pk;
    private Certificate[] certs;

    // Several pkcs12 keystores in byte arrays
    private byte[] bw2048;
    private byte[] bw50000;     // Default old
    private byte[] bs50000;
    private byte[] bs10000;     // Default new
    private byte[] bs2048;

    // Decodes HEX string to byte array
    private static byte[] xeh(String in) {
        return new BigInteger(in, 16).toByteArray();
    }

    @Setup
    public void setup() throws Exception {
        // Just generate a keypair and dump getEncoded() of key and cert.
        byte[] x1 = xeh("3041020100301306072A8648CE3D020106082A8648CE3D03" +
                "0107042730250201010420B561D1FBE150488508BBE8FF4540F09057" +
                "58712F5D2D3CC80F9A15BA5D481117");
        byte[] x2 = xeh("3082012D3081D5A00302010202084EE6ECC5585640A7300A" +
                "06082A8648CE3D040302300C310A30080603550403130161301E170D" +
                "3230313131373230343730355A170D3233303831343230343730355A" +
                "300C310A300806035504031301613059301306072A8648CE3D020106" +
                "082A8648CE3D030107034200041E761F511841602E272B40A021995D" +
                "1BD828DDC7F71412D6A66CC0CB858C856D32C58273E494676D1D2B05" +
                "B8E9B08207A122265C2AA5FCBDCE19E5E88CA7A1B6A321301F301D06" +
                "03551D0E04160414173F278D77096E5C8EA182D12F147694587B5D9A" +
                "300A06082A8648CE3D04030203470030440220760CEAF1FA7041CB8C" +
                "1CA80AF60E4F9C9D5136D96B2AF0AAA9440F79561C44E502205D5C72" +
                "886C92B95A681C4393C67AAEC8DA9FD7910FF9BF2BCB721AE71D1B6F88");
        KeyFactory kf = KeyFactory.getInstance("EC");
        pk = kf.generatePrivate(new PKCS8EncodedKeySpec(x1));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        certs = new Certificate[]{cf.generateCertificate(new ByteArrayInputStream(x2))};

        bw2048 = outweak2048();
        bw50000 = outweak50000_Old();
        bs50000 = outstrong50000();
        bs10000 = outstrong10000_New();
        bs2048 = outstrong2048();
    }

    // Reads in a pkcs12 keystore
    private KeyStore in(byte[] b) throws Exception {
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new ByteArrayInputStream(b), PASS);
        if (!ks.getCertificate("a").getPublicKey().getAlgorithm().equals(
                ks.getKey("a", PASS).getAlgorithm())) {
            throw new RuntimeException("Not same alg");
        }
        return ks;
    }

    // Generates a pkcs12 keystore with the specified algorithm/ic
    private byte[] out(String cAlg, String cIc, String kAlg, String kIc,
                      String mAlg, String mIc) throws Exception {
        System.setProperty("keystore.pkcs12.certProtectionAlgorithm", cAlg);
        System.setProperty("keystore.pkcs12.certPbeIterationCount", cIc);
        System.setProperty("keystore.pkcs12.keyProtectionAlgorithm", kAlg);
        System.setProperty("keystore.pkcs12.keyPbeIterationCount", kIc);
        System.setProperty("keystore.pkcs12.macAlgorithm", mAlg);
        System.setProperty("keystore.pkcs12.macIterationCount", mIc);
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(null, null);
        ks.setKeyEntry("a", pk, PASS, certs);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ks.store(bout, PASS);
        return bout.toByteArray();
    }

    // Benchmark methods start here:

    // Reading a keystore
    @Benchmark
    public KeyStore inweak2048() throws Exception {
        return in(bw2048);
    }

    @Benchmark
    public KeyStore inweak50000_Old() throws Exception {
        return in(bw50000);
    }

    @Benchmark
    public KeyStore instrong50000() throws Exception {
        return in(bs50000);
    }

    @Benchmark
    public KeyStore instrong10000_New() throws Exception {
        return in(bs10000);
    }

    @Benchmark
    public KeyStore instrong2048() throws Exception {
        return in(bs2048);
    }

    // Writing a keystore
    @Benchmark
    public byte[] outweak2048() throws Exception {
        return out("PBEWithSHA1AndRC2_40", "2048",
                "PBEWithSHA1AndDESede", "2048",
                "HmacPBESHA1", "2048");
    }

    @Benchmark
    public byte[] outweak50000_Old() throws Exception {
        return out("PBEWithSHA1AndRC2_40", "50000",
                "PBEWithSHA1AndDESede", "50000",
                "HmacPBESHA1", "100000");
                // Attention: 100000 is old default Mac ic
    }

    @Benchmark
    public byte[] outstrong50000() throws Exception {
        return out("PBEWithHmacSHA256AndAES_256", "50000",
                "PBEWithHmacSHA256AndAES_256", "50000",
                "HmacPBESHA256", "100000");
                // Attention: 100000 is old default Mac ic
    }

    @Benchmark
    public byte[] outstrong10000_New() throws Exception {
        return out("PBEWithHmacSHA256AndAES_256", "10000",
                "PBEWithHmacSHA256AndAES_256", "10000",
                "HmacPBESHA256", "10000");
    }

    @Benchmark
    public byte[] outstrong2048() throws Exception {
        return out("PBEWithHmacSHA256AndAES_256", "2048",
                "PBEWithHmacSHA256AndAES_256", "2048",
                "HmacPBESHA256", "2048");
    }
}
