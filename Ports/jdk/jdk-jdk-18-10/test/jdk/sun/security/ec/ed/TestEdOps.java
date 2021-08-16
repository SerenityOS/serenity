/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166597
 * @summary Test EdDSA basic operations
 * @library /test/lib
 * @modules java.base/sun.security.provider
 *          java.base/sun.security.util
 *          java.base/sun.security.util.math
 *          java.base/sun.security.util.math.intpoly
 *          jdk.crypto.ec/sun.security.ec.ed
 * @run main TestEdOps
 */

import sun.security.ec.ed.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.HexFormat;

import sun.security.provider.SHAKE256;

public class TestEdOps {

    public static void main(String[] args) throws Exception {

        testShake();
        testVectors();
        testRandom(1000);
        testInvalidPoints();
    }

    private static void testShake() {

        runShakeTest(32, "765db6ab3af389b8c775c8eb99fe72",
        "ccb6564a655c94d714f80b9f8de9e2610c4478778eac1b9256237dbf90e50581");

        runShakeTest(32,
            "0e3dcd346c68bc5b5cafe3342a7e0e29272e42fba12a51081251abca989c77a1" +
            "a501e2",
            "c934ab7f2148da5ca2ce948432fa72be49420f10e3dbc1906016773d9819cff4");

        runShakeTest(32,
            "7e4c74f480e60565fe39e483b5204e24753841dec9ef3ec0dadd4e3f91584373" +
            "fc424084f3267b5ffb8342ad6a683c05cc41f26086f18dceb921e1",
            "a6450836c02f8fdfe841fbcb4b4fc7dca9bd56019b92582095ee5d11eca45fa0");

        System.out.println("SHAKE256 tests passed");
    }

    private static void runShakeTest(int outputLen, String msg, String digest) {
        byte[] msgBytes = HexFormat.of().parseHex(msg);
        byte[] digestBytes = HexFormat.of().parseHex(digest);
        SHAKE256 md = new SHAKE256(outputLen);
        md.update(msgBytes, 0, msgBytes.length);
        byte[] computed = md.digest();
        if (!Arrays.equals(digestBytes, computed)) {
            throw new RuntimeException("hash is incorrect");
        }
    }

    private static void testVectors() throws Exception {
        EdDSAParameters ed25519Params = EdDSAParameters.get(
            RuntimeException::new, NamedParameterSpec.ED25519);
        EdDSAOperations ops = new EdDSAOperations(ed25519Params);

        runSignTest(ops,
            "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
            "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
            "",
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
            "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");

        runSignTest(ops,
            "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
            "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
            "72",
            "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da" +
            "085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00");

        runSignTest(ops,
            "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
            "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
            "af82",
            "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac" +
            "18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a");

        runSignTest(ops,
            "f5e5767cf153319517630f226876b86c8160cc583bc013744c6bf255f5cc0ee5",
            "278117fc144c72340f67d0f2316e8386ceffbf2b2428c9c51fef7c597f1d426e",
            "08b8b2b733424243760fe426a4b54908632110a66c2f6591eabd3345e3e4eb98" +
            "fa6e264bf09efe12ee50f8f54e9f77b1e355f6c50544e23fb1433ddf73be84d8" +
            "79de7c0046dc4996d9e773f4bc9efe5738829adb26c81b37c93a1b270b20329d" +
            "658675fc6ea534e0810a4432826bf58c941efb65d57a338bbd2e26640f89ffbc" +
            "1a858efcb8550ee3a5e1998bd177e93a7363c344fe6b199ee5d02e82d522c4fe" +
            "ba15452f80288a821a579116ec6dad2b3b310da903401aa62100ab5d1a36553e" +
            "06203b33890cc9b832f79ef80560ccb9a39ce767967ed628c6ad573cb116dbef" +
            "efd75499da96bd68a8a97b928a8bbc103b6621fcde2beca1231d206be6cd9ec7" +
            "aff6f6c94fcd7204ed3455c68c83f4a41da4af2b74ef5c53f1d8ac70bdcb7ed1" +
            "85ce81bd84359d44254d95629e9855a94a7c1958d1f8ada5d0532ed8a5aa3fb2" +
            "d17ba70eb6248e594e1a2297acbbb39d502f1a8c6eb6f1ce22b3de1a1f40cc24" +
            "554119a831a9aad6079cad88425de6bde1a9187ebb6092cf67bf2b13fd65f270" +
            "88d78b7e883c8759d2c4f5c65adb7553878ad575f9fad878e80a0c9ba63bcbcc" +
            "2732e69485bbc9c90bfbd62481d9089beccf80cfe2df16a2cf65bd92dd597b07" +
            "07e0917af48bbb75fed413d238f5555a7a569d80c3414a8d0859dc65a46128ba" +
            "b27af87a71314f318c782b23ebfe808b82b0ce26401d2e22f04d83d1255dc51a" +
            "ddd3b75a2b1ae0784504df543af8969be3ea7082ff7fc9888c144da2af58429e" +
            "c96031dbcad3dad9af0dcbaaaf268cb8fcffead94f3c7ca495e056a9b47acdb7" +
            "51fb73e666c6c655ade8297297d07ad1ba5e43f1bca32301651339e22904cc8c" +
            "42f58c30c04aafdb038dda0847dd988dcda6f3bfd15c4b4c4525004aa06eeff8" +
            "ca61783aacec57fb3d1f92b0fe2fd1a85f6724517b65e614ad6808d6f6ee34df" +
            "f7310fdc82aebfd904b01e1dc54b2927094b2db68d6f903b68401adebf5a7e08" +
            "d78ff4ef5d63653a65040cf9bfd4aca7984a74d37145986780fc0b16ac451649" +
            "de6188a7dbdf191f64b5fc5e2ab47b57f7f7276cd419c17a3ca8e1b939ae49e4" +
            "88acba6b965610b5480109c8b17b80e1b7b750dfc7598d5d5011fd2dcc5600a3" +
            "2ef5b52a1ecc820e308aa342721aac0943bf6686b64b2579376504ccc493d97e" +
            "6aed3fb0f9cd71a43dd497f01f17c0e2cb3797aa2a2f256656168e6c496afc5f" +
            "b93246f6b1116398a346f1a641f3b041e989f7914f90cc2c7fff357876e506b5" +
            "0d334ba77c225bc307ba537152f3f1610e4eafe595f6d9d90d11faa933a15ef1" +
            "369546868a7f3a45a96768d40fd9d03412c091c6315cf4fde7cb68606937380d" +
            "b2eaaa707b4c4185c32eddcdd306705e4dc1ffc872eeee475a64dfac86aba41c" +
            "0618983f8741c5ef68d3a101e8a3b8cac60c905c15fc910840b94c00a0b9d0",
            "0aab4c900501b3e24d7cdf4663326a3a87df5e4843b2cbdb67cbf6e460fec350" +
            "aa5371b1508f9f4528ecea23c436d94b5e8fcd4f681e30a6ac00a9704a188a03");

        runSignTest(ops,
            "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42",
            "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf",
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
            "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            "dc2a4459e7369633a52b1bf277839a00201009a3efbf3ecb69bea2186c26b589" +
            "09351fc9ac90b3ecfdfbc7c66431e0303dca179c138ac17ad9bef1177331a704");


        EdDSAParameters ed448Params = EdDSAParameters.get(RuntimeException::new,
            NamedParameterSpec.ED448);
        EdDSAOperations ed448Ops = new EdDSAOperations(ed448Params);

        runSignTest(ed448Ops,
            "c4eab05d357007c632f3dbb48489924d552b08fe0c353a0d4a1f00acda2c463a" +
            "fbea67c5e8d2877c5e3bc397a659949ef8021e954e0a12274e",
            "43ba28f430cdff456ae531545f7ecd0ac834a55d9358c0372bfa0c6c6798c086" +
            "6aea01eb00742802b8438ea4cb82169c235160627b4c3a9480",
            "03",
            "26b8f91727bd62897af15e41eb43c377efb9c610d48f2335cb0bd0087810f435" +
            "2541b143c4b981b7e18f62de8ccdf633fc1bf037ab7cd779805e0dbcc0aae1cb" +
            "cee1afb2e027df36bc04dcecbf154336c19f0af7e0a6472905e799f1953d2a0f" +
            "f3348ab21aa4adafd1d234441cf807c03a00");

        System.out.println("All test vectors passed");
    }

    private static void runSignTest(EdDSAOperations ops, String privateKey,
                                    String publicKey, String message,
                                    String signature) throws Exception {

        EdDSAParameterSpec sigParams = new EdDSAParameterSpec(false);

        HexFormat hex = HexFormat.of();
        byte[] privKeyBytes = hex.parseHex(privateKey);
        byte[] pubKeyBytes = hex.parseHex(publicKey);
        byte[] msgBytes = hex.parseHex(message);
        byte[] computedSig = ops.sign(sigParams, privKeyBytes, msgBytes);
        if (!Arrays.equals(computedSig, hex.parseHex(signature))) {
            throw new RuntimeException("Incorrect signature: " +
                HexFormat.of().withUpperCase().formatHex(computedSig) + " != " + signature);
        }
        // Test public key computation
        EdECPoint pubPoint = ops.computePublic(privKeyBytes);
        EdDSAPublicKeyImpl pubKey =
            new EdDSAPublicKeyImpl(ops.getParameters(), pubPoint);
        byte[] computedPubKey = pubKey.getEncodedPoint();
        if (!Arrays.equals(computedPubKey, pubKeyBytes)) {
            throw new RuntimeException("Incorrect public key");
        }

        // test verification
        ops.verify(sigParams, pubKeyBytes, msgBytes, computedSig);
    }

    private static void testRandom(int count) throws Exception {

        EdDSAParameterSpec sigParams = new EdDSAParameterSpec(false);
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(1);

        EdDSAParameters params = EdDSAParameters.get(RuntimeException::new,
            NamedParameterSpec.ED25519);
        EdDSAOperations ops = new EdDSAOperations(params);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            byte[] privKey = ops.generatePrivate(random);
            byte[] message = new byte[1024];
            random.nextBytes(message);
            byte[] sig = ops.sign(sigParams, privKey, message);

            EdECPoint pubKeyPoint = ops.computePublic(privKey);
            EdDSAPublicKeyImpl pubKey =
                new EdDSAPublicKeyImpl(params, pubKeyPoint);
            byte[] encodedPubKey = pubKey.getEncodedPoint();
            if (!ops.verify(sigParams, encodedPubKey, message, sig)) {
                throw new RuntimeException("signature did not verify");
            }
        }

        long endTime = System.currentTimeMillis();
        double millisPerIter = (double) (endTime - startTime) / count;
        System.out.println("Time per keygen+sign+verify: " +
            millisPerIter + " ms");
    }

    private static void testInvalidPoints() throws Exception {

        // Ed25519

        // incorrect length
        testInvalidPoint(NamedParameterSpec.ED25519, "");
        testInvalidPoint(NamedParameterSpec.ED25519, "ffffff");
        testInvalidPoint(NamedParameterSpec.ED25519,
            "abd75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f70751" +
            "1a");
        // y value too large
        testInvalidPoint(NamedParameterSpec.ED25519,
            "8ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe");
        // not square
        testInvalidPoint(NamedParameterSpec.ED25519,
            "d85a980182b10ac7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");
        // x = 0, but x mod 2 == 1
        testInvalidPoint(NamedParameterSpec.ED25519,
            "0100000000000000000000000000000000000000000000000000000000000080");

        // Ed448
        testInvalidPoint(NamedParameterSpec.ED448, "");
        testInvalidPoint(NamedParameterSpec.ED448, "ffffff");
        testInvalidPoint(NamedParameterSpec.ED448,
            "ab43ba28f430cdfe456ae531545f7ecd0ac834a55c9358c0372bfa0c6c6798c0" +
            "866aea01eb00742802b8438ea4cb82169c235160627b4c3a9480");
        // y value too large
        testInvalidPoint(NamedParameterSpec.ED448,
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
            "ffffffffffffffffffffffffffffffffffffffffffffffffffff00");
        // not square
        testInvalidPoint(NamedParameterSpec.ED448,
            "43ba28f430cdfe456ae531545f7ecd0ac834a55c9358c0372bfa0c6c6798c086" +
            "6aea01eb00742802b8438ea4cb82169c235160627b4c3a9480");
        // x = 0, but x mod 2 == 1
        testInvalidPoint(NamedParameterSpec.ED448,
            "0100000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000080");

    }

    private static void testInvalidPoint(NamedParameterSpec curve,
                                         String pointStr) throws Exception {

        byte[] encodedPoint = HexFormat.of().parseHex(pointStr);
        EdDSAParameters params =
            EdDSAParameters.get(RuntimeException::new, curve);
        EdDSAOperations ops = new EdDSAOperations(params);
        try {
            ops.decodeAffinePoint(InvalidKeyException::new, encodedPoint);
            throw new RuntimeException("No exception on invalid point");
        } catch (InvalidKeyException ex) {
            // this is expected
        }
    }
}
