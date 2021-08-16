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
 * @bug 8171277
 * @summary Test XEC curve operations
 * @modules jdk.crypto.ec/sun.security.ec
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main TestXECOps
 */

import sun.security.ec.*;

import java.security.spec.NamedParameterSpec;
import java.util.*;
import jdk.test.lib.Convert;

// Test vectors are from RFC 7748

public class TestXECOps {

    public static void main(String[] args) {
        TestXECOps m = new TestXECOps();

        m.runTest("X25519",
            "a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4",
            "e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c",
            "c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552");

        m.runTest("X25519",
            "4b66e9d4d1b4673c5ad22691957d6af5c11b6421e0ea01d42ca4169e7918ba0d",
            "e5210f12786811d3f4b7959d0538ae2c31dbe7106fc03c3efc4cd549c715a493",
            "95cbde9476e8907d7aade45cb4b873f88b595a68799fa152e6f8f7647aac7957");

        m.runDiffieHellmanTest("X25519",
            "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a",
            "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb",
            "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");


        m.runTest("X448",
            "3d262fddf9ec8e88495266fea19a34d28882acef045104d0d1aae121700a77" +
            "9c984c24f8cdd78fbff44943eba368f54b29259a4f1c600ad3",
            "06fce640fa3487bfda5f6cf2d5263f8aad88334cbd07437f020f08f9814dc0" +
            "31ddbdc38c19c6da2583fa5429db94ada18aa7a7fb4ef8a086",
            "ce3e4ff95a60dc6697da1db1d85e6afbdf79b50a2412d7546d5f239fe14fba" +
            "adeb445fc66a01b0779d98223961111e21766282f73dd96b6f");

        m.runTest("X448",
            "203d494428b8399352665ddca42f9de8fef600908e0d461cb021f8c538345d" +
            "d77c3e4806e25f46d3315c44e0a5b4371282dd2c8d5be3095f",
            "0fbcc2f993cd56d3305b0b7d9e55d4c1a8fb5dbb52f8e9a1e9b6201b165d01" +
            "5894e56c4d3570bee52fe205e28a78b91cdfbde71ce8d157db",
            "884a02576239ff7a2f2f63b2db6a9ff37047ac13568e1e30fe63c4a7ad1b3e" +
            "e3a5700df34321d62077e63633c575c1c954514e99da7c179d");

        m.runDiffieHellmanTest("X448",
            "9a8f4925d1519f5775cf46b04b5800d4ee9ee8bae8bc5565d498c28dd9c9ba" +
            "f574a9419744897391006382a6f127ab1d9ac2d8c0a598726b",
            "1c306a7ac2a0e2e0990b294470cba339e6453772b075811d8fad0d1d6927c1" +
            "20bb5ee8972b0d3e21374c9c921b09d1b0366f10b65173992d",
            "07fff4181ac6cc95ec1c16a94a0f74d12da232ce40a77552281d282bb60c0b" +
            "56fd2464c335543936521c24403085d59a449a5037514a879d");
    }

    private void runDiffieHellmanTest(String opName, String a_str,
        String b_str, String result_str) {

        NamedParameterSpec paramSpec = new NamedParameterSpec(opName);
        XECParameters settings =
            XECParameters.get(RuntimeException::new, paramSpec);
        XECOperations ops = new XECOperations(settings);

        byte[] basePoint = Convert.byteToByteArray(settings.getBasePoint(),
            settings.getBytes());
        byte[] a = HexFormat.of().parseHex(a_str);
        byte[] b = HexFormat.of().parseHex(b_str);
        byte[] expectedResult = HexFormat.of().parseHex(result_str);

        byte[] a_copy = Arrays.copyOf(a, a.length);
        byte[] b_copy = Arrays.copyOf(b, b.length);
        byte[] basePoint_copy = Arrays.copyOf(basePoint, basePoint.length);

        byte[] resultA = ops.encodedPointMultiply(b,
            ops.encodedPointMultiply(a, basePoint));
        byte[] resultB = ops.encodedPointMultiply(a_copy,
            ops.encodedPointMultiply(b_copy, basePoint_copy));
        if (!Arrays.equals(resultA, expectedResult)) {
            throw new RuntimeException("fail");
        }
        if (!Arrays.equals(resultB, expectedResult)) {
            throw new RuntimeException("fail");
        }
    }

    private void runTest(String opName, String k_in_str,
        String u_in_str, String u_out_str) {

        byte[] k_in = HexFormat.of().parseHex(k_in_str);
        byte[] u_in = HexFormat.of().parseHex(u_in_str);
        byte[] u_out_expected = HexFormat.of().parseHex(u_out_str);

        NamedParameterSpec paramSpec = new NamedParameterSpec(opName);
        XECParameters settings =
            XECParameters.get(RuntimeException::new, paramSpec);
        XECOperations ops = new XECOperations(settings);
        byte[] u_out = ops.encodedPointMultiply(k_in, u_in);

        if (!Arrays.equals(u_out, u_out_expected)) {
            throw new RuntimeException("fail");
        }
    }
}

