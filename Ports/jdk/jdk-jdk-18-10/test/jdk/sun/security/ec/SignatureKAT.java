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

import jdk.test.lib.Convert;

import java.security.*;
import java.security.spec.*;
import java.math.*;
import java.util.*;

/*
 * @test
 * @bug 8172366
 * @summary Known Answer Test for ECDSA signature
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main/othervm SignatureKAT
 */
public class SignatureKAT {

    private static String checkHex(String hex) {
        // if hex length is odd, need to prepend 0
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        return hex;
    }

    private static class CurveData {
        private String name;
        private byte[] msgBytes;
        private BigInteger priv;
        private BigInteger pubX;
        private BigInteger pubY;

        private static BigInteger toBigInteger(String hex) {
            byte[] bytes = HexFormat.of().parseHex(checkHex(hex));
            return new BigInteger(1, bytes);
        }
        CurveData(String name, String msg, String priv, String pubX,
                String pubY) {
            this.name = name;
            this.msgBytes = msg.getBytes();
            this.priv = toBigInteger(priv);
            this.pubX = toBigInteger(pubX);
            this.pubY = toBigInteger(pubY);
        }
    }

    private static class TestData {
        private String sigName;
        private CurveData cd;
        private byte[] expSig;

        TestData(String sigName, CurveData cd, String r, String s) {
            this.sigName = sigName;
            this.cd = cd;
            if (r.length() != s.length() || r != checkHex(r) ||
                s != checkHex(s)) {
                throw new RuntimeException("Error: invalid r, s");
            }
            this.expSig = HexFormat.of().parseHex(r + s);
        }
    }

    // These test values are from the examples shown in the page below:
    // https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines/example-values
    private static final CurveData P_256 = new CurveData(
        "secp256r1", "Example of ECDSA with P-256",
        "C477F9F65C22CCE20657FAA5B2D1D8122336F851A508A1ED04E479C34985BF96",
        "B7E08AFDFE94BAD3F1DC8C734798BA1C62B3A0AD1E9EA2A38201CD0889BC7A19",
        "3603F747959DBF7A4BB226E41928729063ADC7AE43529E61B563BBC606CC5E09"
    );

    private static final CurveData P_384 = new CurveData(
        "secp384r1", "Example of ECDSA with P-384",
        "F92C02ED629E4B48C0584B1C6CE3A3E3B4FAAE4AFC6ACB0455E73DFC392E6A0AE393A8565E6B9714D1224B57D83F8A08",
        "3BF701BC9E9D36B4D5F1455343F09126F2564390F2B487365071243C61E6471FB9D2AB74657B82F9086489D9EF0F5CB5",
        "D1A358EAFBF952E68D533855CCBDAA6FF75B137A5101443199325583552A6295FFE5382D00CFCDA30344A9B5B68DB855"
    );

    private static final CurveData P_521 = new CurveData(
        "secp521r1", "Example of ECDSA with P-521",
        "100085F47B8E1B8B11B7EB33028C0B2888E304BFC98501955B45BBA1478DC184EEEDF09B86A5F7C21994406072787205E69A63709FE35AA93BA333514B24F961722",
        "98E91EEF9A68452822309C52FAB453F5F117C1DA8ED796B255E9AB8F6410CCA16E59DF403A6BDC6CA467A37056B1E54B3005D8AC030DECFEB68DF18B171885D5C4",
        "164350C321AECFC1CCA1BA4364C9B15656150B4B78D6A48D7D28E7F31985EF17BE8554376B72900712C4B83AD668327231526E313F5F092999A4632FD50D946BC2E"
    );

    private static TestData[] TEST_DATUM = {
        // secp256r1, secp384r1, and secp521r1 remain enabled
        new TestData("SHA256withECDSAinP1363Format", P_256,
            "2B42F576D07F4165FF65D1F3B1500F81E44C316F1F0B3EF57325B69ACA46104F",
            "DC42C2122D6392CD3E3A993A89502A8198C1886FE69D262C4B329BDB6B63FAF1"),
        new TestData("SHA3-256withECDSAinP1363Format", P_256,
            "2B42F576D07F4165FF65D1F3B1500F81E44C316F1F0B3EF57325B69ACA46104F",
            "0A861C2526900245C73BACB9ADAEC1A5ACB3BA1F7114A3C334FDCD5B7690DADD"),
        new TestData("SHA384withECDSAinP1363Format", P_384,
            "30EA514FC0D38D8208756F068113C7CADA9F66A3B40EA3B313D040D9B57DD41A332795D02CC7D507FCEF9FAF01A27088",
            "CC808E504BE414F46C9027BCBF78ADF067A43922D6FCAA66C4476875FBB7B94EFD1F7D5DBE620BFB821C46D549683AD8"),
        new TestData("SHA3-384withECDSAinP1363Format", P_384,
            "30EA514FC0D38D8208756F068113C7CADA9F66A3B40EA3B313D040D9B57DD41A332795D02CC7D507FCEF9FAF01A27088",
            "691B9D4969451A98036D53AA725458602125DE74881BBC333012CA4FA55BDE39D1BF16A6AAE3FE4992C567C6E7892337"),
        new TestData("SHA512withECDSAinP1363Format", P_521,
            "0140C8EDCA57108CE3F7E7A240DDD3AD74D81E2DE62451FC1D558FDC79269ADACD1C2526EEEEF32F8C0432A9D56E2B4A8A732891C37C9B96641A9254CCFE5DC3E2BA",
            "00D72F15229D0096376DA6651D9985BFD7C07F8D49583B545DB3EAB20E0A2C1E8615BD9E298455BDEB6B61378E77AF1C54EEE2CE37B2C61F5C9A8232951CB988B5B1"),
        new TestData("SHA3-512withECDSAinP1363Format", P_521,
            "0140C8EDCA57108CE3F7E7A240DDD3AD74D81E2DE62451FC1D558FDC79269ADACD1C2526EEEEF32F8C0432A9D56E2B4A8A732891C37C9B96641A9254CCFE5DC3E2BA",
            "00B25188492D58E808EDEBD7BF440ED20DB771CA7C618595D5398E1B1C0098E300D8C803EC69EC5F46C84FC61967A302D366C627FCFA56F87F241EF921B6E627ADBF"),
    };

    private static void runTest(TestData td) throws Exception {
        System.out.println("Testing " + td.sigName + " with " + td.cd.name);

        AlgorithmParameters params =
            AlgorithmParameters.getInstance("EC", "SunEC");
        params.init(new ECGenParameterSpec(td.cd.name));
        ECParameterSpec ecParams =
            params.getParameterSpec(ECParameterSpec.class);

        KeyFactory kf = KeyFactory.getInstance("EC", "SunEC");
        PrivateKey privKey = kf.generatePrivate
                (new ECPrivateKeySpec(td.cd.priv, ecParams));

        Signature sig = Signature.getInstance(td.sigName, "SunEC");
        sig.initSign(privKey);
        sig.update(td.cd.msgBytes);
        // NOTE: there is no way to set the nonce value into current SunEC
        // ECDSA signature, thus the output signature bytes likely won't
        // match the expected signature bytes
        byte[] ov = sig.sign();

        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec
                (new ECPoint(td.cd.pubX, td.cd.pubY), ecParams);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);

        sig.initVerify(pubKey);
        sig.update(td.cd.msgBytes);
        if (!sig.verify(ov)) {
            throw new RuntimeException("Error verifying actual sig bytes");
        }

        sig.update(td.cd.msgBytes);
        if (!sig.verify(td.expSig)) {
            throw new RuntimeException("Error verifying expected sig bytes");
        }
    }

    public static void main(String[] args) throws Exception {
        for (TestData td : TEST_DATUM) {
            runTest(td);
        }
    }
}
