/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171277 8206915
 * @summary Test XDH key agreement
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main TestXDH
 */

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import java.util.Arrays;
import java.util.HexFormat;

import jdk.test.lib.Convert;
import jdk.test.lib.hexdump.ASN1Formatter;
import jdk.test.lib.hexdump.HexPrinter;

public class TestXDH {

    public static void main(String[] args) throws Exception {

        runBasicTests();
        runKAT();
        runSmallOrderTest();
        runNonCanonicalTest();
        runCurveMixTest();
    }

    private static void runBasicTests() throws Exception {
        runBasicTest("XDH", null);
        runBasicTest("XDH", 255);
        runBasicTest("XDH", 448);
        runBasicTest("XDH", "X25519");
        runBasicTest("XDH", "X448");
        runBasicTest("X25519", null);
        runBasicTest("X448", null);
        runBasicTest("1.3.101.110", null);
        runBasicTest("1.3.101.111", null);
        runBasicTest("OID.1.3.101.110", null);
        runBasicTest("OID.1.3.101.111", null);
    }

    private static void runBasicTest(String name, Object param)
        throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name);
        AlgorithmParameterSpec paramSpec = null;
        if (param instanceof Integer) {
            kpg.initialize((Integer) param);
        } else if (param instanceof String) {
            paramSpec = new NamedParameterSpec((String) param);
            kpg.initialize(paramSpec);
        }
        KeyPair kp = kpg.generateKeyPair();

        KeyAgreement ka = KeyAgreement.getInstance(name);
        ka.init(kp.getPrivate(), paramSpec);
        ka.doPhase(kp.getPublic(), true);

        byte[] secret = ka.generateSecret();

        KeyFactory kf = KeyFactory.getInstance(name);
        // Test with X509 and PKCS8 key specs
        X509EncodedKeySpec pubSpec =
            kf.getKeySpec(kp.getPublic(), X509EncodedKeySpec.class);
        PKCS8EncodedKeySpec priSpec =
            kf.getKeySpec(kp.getPrivate(), PKCS8EncodedKeySpec.class);

        PublicKey pubKey = kf.generatePublic(pubSpec);
        PrivateKey priKey = kf.generatePrivate(priSpec);

        ka.init(priKey);
        ka.doPhase(pubKey, true);
        byte[] secret2 = ka.generateSecret();
        if (!Arrays.equals(secret, secret2)) {
            throw new RuntimeException("Arrays not equal");
        }

        // make sure generateSecret() resets the state to after init()
        try {
            ka.generateSecret();
            throw new RuntimeException("generateSecret does not reset state");
        } catch (IllegalStateException ex) {
            // do nothing---this is expected
        }
        ka.doPhase(pubKey, true);
        ka.generateSecret();

        // test with XDH key specs
        XECPublicKeySpec xdhPublic =
            kf.getKeySpec(kp.getPublic(), XECPublicKeySpec.class);
        XECPrivateKeySpec xdhPrivate =
            kf.getKeySpec(kp.getPrivate(), XECPrivateKeySpec.class);
        PublicKey pubKey2 = kf.generatePublic(xdhPublic);
        PrivateKey priKey2 = kf.generatePrivate(xdhPrivate);
        ka.init(priKey2);
        ka.doPhase(pubKey2, true);
        byte[] secret3 = ka.generateSecret();
        if (!Arrays.equals(secret, secret3)) {
            throw new RuntimeException("Arrays not equal");
        }
    }

    private static void runSmallOrderTest() throws Exception {
        // Ensure that small-order points are rejected

        // X25519
        // 0
        testSmallOrder(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "0000000000000000000000000000000000000000000000000000000000000000",
            "0000000000000000000000000000000000000000000000000000000000000000");
        // 1 and -1
        testSmallOrder(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "0100000000000000000000000000000000000000000000000000000000000000",
            "0000000000000000000000000000000000000000000000000000000000000000");
        testSmallOrder(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
            "0000000000000000000000000000000000000000000000000000000000000000");

        // order 8 points
        testSmallOrder(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "5f9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f1157",
            "0000000000000000000000000000000000000000000000000000000000000000");
        testSmallOrder(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "e0eb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b800",
            "0000000000000000000000000000000000000000000000000000000000000000");

        // X448
        // 0
        testSmallOrder(
            "X448",
            "9A8F4925D1519F5775CF46B04B5800D4EE9EE8BAE8BC5565D498C28DD9C9BA" +
            "F574A9419744897391006382A6F127AB1D9AC2D8C0A598726B",
            "00000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000",
            "00000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000");
        // 1 and -1
        testSmallOrder(
            "X448",
            "9A8F4925D1519F5775CF46B04B5800D4EE9EE8BAE8BC5565D498C28DD9C9BA" +
            "F574A9419744897391006382A6F127AB1D9AC2D8C0A598726B",
            "01000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000",
            "00000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000");
        testSmallOrder(
            "X448",
            "9A8F4925D1519F5775CF46B04B5800D4EE9EE8BAE8BC5565D498C28DD9C9BAF" +
            "574A9419744897391006382A6F127AB1D9AC2D8C0A598726B",
            "fefffffffffffffffffffffffffffffffffffffffffffffffffffffffefffff" +
            "fffffffffffffffffffffffffffffffffffffffffffffffff",
            "000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000");
    }

    private static void testSmallOrder(String name, String a_pri,
            String b_pub, String result) throws Exception {

        try {
            runDiffieHellmanTest(name, a_pri, b_pub, result);
        } catch (InvalidKeyException ex) {
            return;
        }

        throw new RuntimeException("No exception on small-order point");
    }

    private static void runNonCanonicalTest() throws Exception {
        // Test non-canonical values

        // high bit of public key set
        // X25519
        runDiffieHellmanTest(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "DE9EDB7D7B7DC1B4D35B61C2ECE435373F8343C85B78674DADFC7E146F882B8F",
            "954e472439316f118ae158b65619eecff9e6bcf51ab29add66f3fd088681e233");

        runDiffieHellmanTest(
            "3030020100300706032b656e05000422042077076d0a7318a57d3c16c1725" +
            "1b26645df4c2f87ebc0992ab177fba51db92c2a",
            "302c300706032b656e0500032100de9edb7d7b7dc1b4d35b61c2ece435373f" +
            "8343c85b78674dadfc7e146f882b8f",
            "954e472439316f118ae158b65619eecff9e6bcf51ab29add66f3fd088681e233");

        // large public key

        // X25519
        // public key value is 2^255-2
        runDiffieHellmanTest(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "FEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7F",
            "81a02a45014594332261085128959869fc0540c6b12380f51db4b41380de2c2c");

        runDiffieHellmanTest(
            "3030020100300706032b656e05000422042077076d0a7318a57d3c16c17251" +
            "b26645df4c2f87ebc0992ab177fba51db92c2a",
            "302c300706032b656e0500032100FEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFF7F",
            "81a02a45014594332261085128959869fc0540c6b12380f51db4b41380de2c2c");

        // X448
        // public key value is 2^448-2
        runDiffieHellmanTest(
            "X448",
            "9A8F4925D1519F5775CF46B04B5800D4EE9EE8BAE8BC5565D498C28DD9C9BA" +
            "F574A9419744897391006382A6F127AB1D9AC2D8C0A598726B",
            "FEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "66e2e682b1f8e68c809f1bb3e406bd826921d9c1a5bfbfcbab7ae72feecee6" +
            "3660eabd54934f3382061d17607f581a90bdac917a064959fb");

        runDiffieHellmanTest(
            "3048020100300706032B656F0500043A04389A8F4925D1519F5775CF46B04B" +
            "5800D4EE9EE8BAE8BC5565D498C28DD9C9BAF574A9419744897391006382A6" +
            "F127AB1D9AC2D8C0A598726B",
            "3044300706032B656F0500033900FEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
            "FFFFFFFFFFFFFFFF",
            "66e2e682b1f8e68c809f1bb3e406bd826921d9c1a5bfbfcbab7ae72feecee6" +
            "3660eabd54934f3382061d17607f581a90bdac917a064959fb");

    }

    private static void runKAT() throws Exception {
        // Test both sides of the key exchange using vectors in RFC 7748

        // X25519
        // raw
        runDiffieHellmanTest(
            "X25519",
            "77076D0A7318A57D3C16C17251B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "DE9EDB7D7B7DC1B4D35B61C2ECE435373F8343C85B78674DADFC7E146F882B4F",
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");

        runDiffieHellmanTest(
            "X25519",
            "5DAB087E624A8A4B79E17F8B83800EE66F3BB1292618B6FD1C2F8B27FF88E0EB",
            "8520F0098930A754748B7DDCB43EF75A0DBF3A0D26381AF4EBA4A98EAA9B4E6A",
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");

        // encoded
        runDiffieHellmanTest(
            "3030020100300706032B656E05000422042077076D0A7318A57D3C16C17251" +
            "B26645DF4C2F87EBC0992AB177FBA51DB92C2A",
            "302C300706032B656E0500032100DE9EDB7D7B7DC1B4D35B61C2ECE435373F" +
            "8343C85B78674DADFC7E146F882B4F",
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");

        runDiffieHellmanTest(
            "3030020100300706032B656E0500042204205DAB087E624A8A4B79E17F8B83" +
            "800EE66F3BB1292618B6FD1C2F8B27FF88E0EB",
            "302C300706032B656E05000321008520F0098930A754748B7DDCB43EF75A0D" +
            "BF3A0D26381AF4EBA4A98EAA9B4E6A",
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");

        // X448
        //raw
        runDiffieHellmanTest(
            "X448",
            "9A8F4925D1519F5775CF46B04B5800D4EE9EE8BAE8BC5565D498C28DD9C9BA" +
            "F574A9419744897391006382A6F127AB1D9AC2D8C0A598726B",
            "3EB7A829B0CD20F5BCFC0B599B6FECCF6DA4627107BDB0D4F345B43027D8B9" +
            "72FC3E34FB4232A13CA706DCB57AEC3DAE07BDC1C67BF33609",
            "07fff4181ac6cc95ec1c16a94a0f74d12da232ce40a77552281d282bb60c0b" +
            "56fd2464c335543936521c24403085d59a449a5037514a879d");

        runDiffieHellmanTest(
            "X448",
            "1C306A7AC2A0E2E0990B294470CBA339E6453772B075811D8FAD0D1D6927C1" +
            "20BB5EE8972B0D3E21374C9C921B09D1B0366F10B65173992D",
            "9B08F7CC31B7E3E67D22D5AEA121074A273BD2B83DE09C63FAA73D2C22C5D9" +
            "BBC836647241D953D40C5B12DA88120D53177F80E532C41FA0",
            "07fff4181ac6cc95ec1c16a94a0f74d12da232ce40a77552281d282bb60c0b" +
            "56fd2464c335543936521c24403085d59a449a5037514a879d");

        //encoded
        runDiffieHellmanTest(
            "3048020100300706032B656F0500043A04389A8F4925D1519F5775CF46B04B" +
            "5800D4EE9EE8BAE8BC5565D498C28DD9C9BAF574A9419744897391006382A6" +
            "F127AB1D9AC2D8C0A598726B",
            "3044300706032B656F05000339003EB7A829B0CD20F5BCFC0B599B6FECCF6D" +
            "A4627107BDB0D4F345B43027D8B972FC3E34FB4232A13CA706DCB57AEC3DAE" +
            "07BDC1C67BF33609",
            "07fff4181ac6cc95ec1c16a94a0f74d12da232ce40a77552281d282bb60c0b" +
            "56fd2464c335543936521c24403085d59a449a5037514a879d");

        runDiffieHellmanTest(
            "3048020100300706032B656F0500043A04381C306A7AC2A0E2E0990B294470" +
            "CBA339E6453772B075811D8FAD0D1D6927C120BB5EE8972B0D3E21374C9C92" +
            "1B09D1B0366F10B65173992D",
            "3044300706032B656F05000339009B08F7CC31B7E3E67D22D5AEA121074A27" +
            "3BD2B83DE09C63FAA73D2C22C5D9BBC836647241D953D40C5B12DA88120D53" +
            "177F80E532C41FA0",
            "07fff4181ac6cc95ec1c16a94a0f74d12da232ce40a77552281d282bb60c0b" +
            "56fd2464c335543936521c24403085d59a449a5037514a879d");
    }

    private static void runDiffieHellmanTest(String a_pri,
        String b_pub, String result) throws Exception {

        KeyFactory kf = KeyFactory.getInstance("XDH");
        byte[] a_pri_ba = HexFormat.of().parseHex(a_pri);
        KeySpec privateSpec = new PKCS8EncodedKeySpec(a_pri_ba);
        PrivateKey privateKey = kf.generatePrivate(privateSpec);
        byte[] b_pub_ba = HexFormat.of().parseHex(b_pub);
        KeySpec publicSpec = new X509EncodedKeySpec(b_pub_ba);
        PublicKey publicKey = kf.generatePublic(publicSpec);

        KeyAgreement ka = KeyAgreement.getInstance("XDH");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);

        byte[] sharedSecret = ka.generateSecret();
        byte[] expectedResult = HexFormat.of().parseHex(result);
        if (!Arrays.equals(sharedSecret, expectedResult)) {
            throw new RuntimeException("fail: expected=" + result + ", actual="
                + HexFormat.of().withUpperCase().formatHex(sharedSecret));
        }

    }

    private static void runDiffieHellmanTest(String curveName, String a_pri,
        String b_pub, String result) throws Exception {

        NamedParameterSpec paramSpec = new NamedParameterSpec(curveName);
        KeyFactory kf = KeyFactory.getInstance("XDH");
        KeySpec privateSpec = new XECPrivateKeySpec(paramSpec,
            HexFormat.of().parseHex(a_pri));
        PrivateKey privateKey = kf.generatePrivate(privateSpec);
        boolean clearHighBit = curveName.equals("X25519");
        KeySpec publicSpec = new XECPublicKeySpec(paramSpec,
            Convert.hexStringToBigInteger(clearHighBit, b_pub));
        PublicKey publicKey = kf.generatePublic(publicSpec);

        byte[] encodedPrivateKey = privateKey.getEncoded();
        System.out.println("Encoded private: " +
            HexFormat.of().withUpperCase().formatHex(encodedPrivateKey));
        System.out.println(HexPrinter.simple()
                .formatter(ASN1Formatter.formatter())
                .toString(encodedPrivateKey));
        byte[] encodedPublicKey = publicKey.getEncoded();
        System.out.println("Encoded public: " +
            HexFormat.of().withUpperCase().formatHex(encodedPublicKey));
        System.out.println(HexPrinter.simple()
                .formatter(ASN1Formatter.formatter())
                .toString(encodedPublicKey));
        KeyAgreement ka = KeyAgreement.getInstance("XDH");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);

        byte[] sharedSecret = ka.generateSecret();
        byte[] expectedResult = HexFormat.of().parseHex(result);
        if (!Arrays.equals(sharedSecret, expectedResult)) {
            throw new RuntimeException("fail: expected=" + result + ", actual="
                + HexFormat.of().withUpperCase().formatHex(sharedSecret));
        }
    }

    /*
     * Ensure that SunEC rejects parameters/points for the wrong curve
     * when the algorithm ID for a specific curve is specified.
     */
    private static void runCurveMixTest() throws Exception {
        runCurveMixTest("SunEC", "X25519", 448);
        runCurveMixTest("SunEC", "X25519", "X448");
        runCurveMixTest("SunEC", "X448", 255);
        runCurveMixTest("SunEC", "X448", "X25519");
    }

    private static void runCurveMixTest(String providerName, String name,
                                        Object param) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name,
            providerName);

        try {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param));
            }
            throw new RuntimeException(name + " KeyPairGenerator accepted "
                + param.toString() + " parameters");
        } catch (InvalidParameterException ex) {
            // expected
        }

        // the rest of the test uses the parameter as an algorithm name to
        // produce keys
        if (param instanceof Integer) {
            return;
        }
        String otherName = (String) param;
        KeyPairGenerator otherKpg = KeyPairGenerator.getInstance(otherName,
            providerName);
        KeyPair otherKp = otherKpg.generateKeyPair();

        // ensure the KeyFactory rejects incorrect keys
        KeyFactory kf = KeyFactory.getInstance(name, providerName);
        try {
            kf.getKeySpec(otherKp.getPublic(), XECPublicKeySpec.class);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }
        try {
            kf.getKeySpec(otherKp.getPrivate(), XECPrivateKeySpec.class);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }

        try {
            kf.translateKey(otherKp.getPublic());
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeyException ex) {
            // expected
        }
        try {
            kf.translateKey(otherKp.getPrivate());
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeyException ex) {
            // expected
        }

        KeyFactory otherKf = KeyFactory.getInstance(otherName, providerName);
        XECPublicKeySpec otherPubSpec = otherKf.getKeySpec(otherKp.getPublic(),
            XECPublicKeySpec.class);
        try {
            kf.generatePublic(otherPubSpec);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }
        XECPrivateKeySpec otherPriSpec =
            otherKf.getKeySpec(otherKp.getPrivate(), XECPrivateKeySpec.class);
        try {
            kf.generatePrivate(otherPriSpec);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }

        // ensure the KeyAgreement rejects incorrect keys
        KeyAgreement ka = KeyAgreement.getInstance(name, providerName);
        try {
            ka.init(otherKp.getPrivate());
            throw new RuntimeException(name + " KeyAgreement accepted "
                + param.toString() + " key");
        } catch (InvalidKeyException ex) {
            // expected
        }
        KeyPair kp = kpg.generateKeyPair();
        ka.init(kp.getPrivate());
        try {
            // This should always be rejected because it doesn't match the key
            // passed to init, but it is tested here for good measure.
            ka.doPhase(otherKp.getPublic(), true);
            throw new RuntimeException(name + " KeyAgreement accepted "
                + param.toString() + " key");
        } catch (InvalidKeyException ex) {
            // expected
        }
    }
}

