/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851638
 * @summary Tests for StrictMath.log1p
 * @author Joseph D. Darcy
 */

/**
 * The tests in ../Math/Log1pTests.java test properties that should
 * hold for any log1p implementation, including the FDLIBM-based one
 * required for StrictMath.log1p.  Therefore, the test cases in
 * ../Math/Log1pTests.java are run against both the Math and
 * StrictMath versions of log1p.  The role of this test is to verify
 * that the FDLIBM log1p algorithm is being used by running golden
 * file tests on values that may vary from one conforming log1p
 * implementation to another.
 */

public class Log1pTests {
    private Log1pTests(){}

    static int testLog1pCase(double input, double expected) {
        return Tests.test("StrictMath.log1p(double)", input,
                          StrictMath.log1p(input), expected);
    }

    static int testLog1p() {
        int failures = 0;

        double [][] testCases = {
            {0x1.fffffffffffffp-54,     0x1.fffffffffffffp-54},
            {0x1.fffffffffcc48p-15,     0x1.fffc000aa74f3p-15},
            {0x1.ffffffffff224p-14,     0x1.fff8002aa8ccfp-14},
            {0x1.ffffffffff90cp-13,     0x1.fff000aaa23bdp-13},
            {0x1.fffffffffffcep-4,      0x1.e27076e2af2bap-4},
            {0x1.fffffffffffffp-2,      0x1.9f323ecbf984bp-2},
            {0x1.ffffffffffffdp-1,      0x1.62e42fefa39eep-1},
            {0x1.0p1,                   0x1.193ea7aad030ap0},
            {0x1.ffffffffffffbp1,       0x1.9c041f7ed8d31p0},
            {0x1.fffffffffffffp2,       0x1.193ea7aad030ap1},
            {0x1.fffffffffffe1p3,       0x1.6aa6bc1fa7f73p1},
            {0x1.fffffffffffe1p4,       0x1.bf8d8f4d5b8cap1},
            {0x1.ffffffffffff1p5,       0x1.0b29293942974p2},
            {0x1.fffffffffff41p6,       0x1.37072a9b5b6b4p2},
            {0x1.ffffffffffe65p7,       0x1.63241004e8fdep2},
            {0x1.ffffffffffca1p8,       0x1.8f60adf041b73p2},
            {0x1.fffffffffffffp9,       0x1.bbad39ebe1ccp2},
            {0x1.fffffffffffffp10,      0x1.e801c1698ba43p2},
            {0x1.ffffffffff2dep11,      0x1.0a2d23e3bb54bp3},
            {0x1.ffffffffff18dp12,      0x1.205a66eeb4f81p3},
            {0x1.ffffffffffff9p13,      0x1.368829f0af2dcp3},
            {0x1.fffffffffbc1ep14,      0x1.4cb62cf069217p3},
            {0x1.ffffffffffff5p16,      0x1.791282ee99d8ep3},
            {0x1.fffffffffba46p17,      0x1.8f40bded96cd1p3},
            {0x1.ffffffffffff7p18,      0x1.a56efcec920cbp3},
            {0x1.ffffffffffff7p19,      0x1.bb9d3deb8c76ap3},
            {0x1.ffffffffffff9p20,      0x1.d1cb7fea86bcap3},
            {0x1.ffffffffffff7p24,      0x1.1542457b37d42p4},
            {0x1.fffffffffffe7p29,      0x1.4cb5ecf0e964fp4},
            {0x1.ffffffffffff9p30,      0x1.57cd0e704682p4},
            {0x1.ffffffffffffbp34,      0x1.8429946e1cf5dp4},
            {0x1.fffffffffffedp35,      0x1.8f40b5ed9912dp4},
            {0x1.fffffffffffefp39,      0x1.bb9d3beb8c96ap4},
            {0x1.fffffffffffe1p40,      0x1.c6b45d6b09abap4},
            {0x1.fffffffffffe3p44,      0x1.f310e368fe17fp4},
            {0x1.ffffffffffff5p45,      0x1.fe2804e87b34cp4},
            {0x1.fffffffffffc5p66,      0x1.7386e22edf4a5p5},
            {0x1.fffffffffff98p90,      0x1.f89c7428bca5fp5},
            {0x1.a36e2eb1c317dp-14,     0x1.a368d0657ee51p-14},
            {0x1.0624dd2f18d5cp-10,     0x1.060354f8c2226p-10},
            {0x1.ffffffffffffdp-1,      0x1.62e42fefa39eep-1},
            {0x1.8ffffffffffccp6,       0x1.275e2271bba28p2},
            {0x1.f3fffffffff1p9,        0x1.ba2909ce4f846p2},
            {0x1.387ffffffffa8p13,      0x1.26bbed6fbd838p3},
            {0x1.869ffffffffe4p16,      0x1.7069f7a2d94f4p3},
            {0x1.e847fffffff3ep19,      0x1.ba18abb1dedbcp3},
            {0x1.312cfffffff23p23,      0x1.01e3b85ec299p4},
            {0x1.7d783ffffff17p26,      0x1.26bb1bbe0482ap4},
            {0x1.dcd64ffffffcep29,      0x1.4b927f3304b3ap4},
            {0x1.2a05f1ffffa3p33,       0x1.7069e2aa317fep4},
            {0x1.74876e7ffffbep36,      0x1.9541462195ffap4},
            {0x1.d1a94a1fffddp39,       0x1.ba18a999000a6p4},
            {0x1.2309ce53ffed2p43,      0x1.def00d106aa4ep4},
            {0x1.6bcc41e8ffe73p46,      0x1.01e3b843eaa6cp5},
            {0x1.c6bf52633fe7dp49,      0x1.144f69ff9ffbep5},
            {0x1.1c37937e07fffp53,      0x1.26bb1bbb55515p5},
            {0x1.6345785d89f12p56,      0x1.3926cd770aa62p5},
            {0x1.bc16d674ec76ap59,      0x1.4b927f32bffb6p5},
            {0x1.158e460913c51p63,      0x1.5dfe30ee75504p5},
            {0x1.5af1d78b58badp66,      0x1.7069e2aa2aa58p5},
            {0x1.b1ae4d6e2ecd4p69,      0x1.82d59465dffap5},
            {0x1.0f0cf064dd066p73,      0x1.95414621954d6p5},
            {0x1.52d02c7e14a9p76,       0x1.a7acf7dd4aa4cp5},
            {0x1.a784379d99c19p79,      0x1.ba18a998fff98p5},
            {0x1.08b2a2c27fb5p83,       0x1.cc845b54b54bap5},
            {0x1.4adf4b7320322p86,      0x1.def00d106aa42p5},
            {0x1.9d971e4fe7b91p89,      0x1.f15bbecc1ff6ap5},
            {0x1.027e72f1f0ea3p93,      0x1.01e3b843eaa63p6},
            {0x1.431e0fae6d44bp96,      0x1.0b199121c5512p6},
            {0x1.93e5939a086bcp99,      0x1.144f69ff9ffb4p6},
            {0x1.f8def8808ac86p102,     0x1.1d8542dd7aa65p6},
            {0x1.3b8b5b5056dc7p106,     0x1.26bb1bbb55514p6},
            {0x1.8a6e32246c76cp109,     0x1.2ff0f4992ffb8p6},
            {0x1.ed09bead86a07p112,     0x1.3926cd770aa41p6},
            {0x1.3426172c74d33p116,     0x1.425ca654e550ep6},
            {0x1.812f9cf791f1ep119,     0x1.4b927f32bffb4p6},
            {0x1.e17b8435758f2p122,     0x1.54c858109aa3ep6},
            {0x1.2ced32a169cfap126,     0x1.5dfe30ee754fap6},
            {0x1.78287f49c497cp129,     0x1.673409cc4ffbp6},
            {0x1.d6329f1c3492ep132,     0x1.7069e2aa2aa3p6},
            {0x1.25dfa371a14b8p136,     0x1.799fbb88054f2p6},
            {0x1.6f578c4e09f0ap139,     0x1.82d59465dffa8p6},
            {0x1.cb2d6f618c4b4p142,     0x1.8c0b6d43baa4cp6},
            {0x1.1efc659cf77abp146,     0x1.95414621954eap6},
            {0x1.66bb7f0435c5bp149,     0x1.9e771eff6ffa6p6},
            {0x1.c06a5ec5428a4p152,     0x1.a7acf7dd4aa36p6},
            {0x1.18427b3b49fc9p156,     0x1.b0e2d0bb254f6p6},
            {0x1.5e531a0a1c729p159,     0x1.ba18a998fff9cp6},
            {0x1.b5e7e08ca3686p162,     0x1.c34e8276daa4p6},
            {0x1.11b0ec57e6492p166,     0x1.cc845b54b54f2p6},
            {0x1.561d276ddfd7dp169,     0x1.d5ba34328ff9ap6},
            {0x1.aba471495757bp172,     0x1.def00d106aa3p6},
            {0x1.0b46c6cdd6a8ep176,     0x1.e825e5ee454ddp6},
            {0x1.4e1878814c5f4p179,     0x1.f15bbecc1ff88p6},
            {0x1.a19e96a19f65ap182,     0x1.fa9197a9faa2ep6},
            {0x1.05031e2503cfcp186,     0x1.01e3b843eaa71p7},
            {0x1.4643e5ae441d2p189,     0x1.067ea4b2d7fb6p7},
            {0x1.97d4df19d5c5dp192,     0x1.0b199121c5516p7},
            {0x1.fdca16e04ae24p195,     0x1.0fb47d90b2a65p7},
            {0x1.3e9e4e4c2f2dap199,     0x1.144f69ff9ffc4p7},
            {0x1.8e45e1df3ac31p202,     0x1.18ea566e8d514p7},
            {0x1.f1d75a5709306p205,     0x1.1d8542dd7aa63p7},
            {0x1.372698766608cp209,     0x1.22202f4c67fcp7},
            {0x1.84f03e93fef5p212,      0x1.26bb1bbb55508p7},
            {0x1.e62c4e38fdba1p215,     0x1.2b56082a42a4bp7},
            {0x1.2fdbb0e39f6b8p219,     0x1.2ff0f4992ffb6p7},
            {0x1.7bd29d1c875a2p222,     0x1.348be1081d50cp7},
            {0x1.dac74463a76e9p225,     0x1.3926cd770aa42p7},
            {0x1.28bc8abe48f57p229,     0x1.3dc1b9e5f7fap7},
            {0x1.72ebad6ddc67ep232,     0x1.425ca654e550ep7},
            {0x1.cfa698c952a3ap235,     0x1.46f792c3d2a53p7},
            {0x1.21c81f7dd42b1p239,     0x1.4b927f32bffb6p7},
            {0x1.6a3a275d4926bp242,     0x1.502d6ba1ad50ap7},
            {0x1.c4c8b134970ddp245,     0x1.54c858109aa0ep7},
            {0x1.61bcca711985dp252,     0x1.5dfe30ee75508p7},
            {0x1.ba2bfd0d5fe2ap255,     0x1.62991d5d62a5cp7},
            {0x1.59725db2728b7p262,     0x1.6bcef63b3d4fcp7},
            {0x1.afcef51f0fa33p265,     0x1.7069e2aa2aa5ap7},
            {0x1.0de1593368f8cp269,     0x1.7504cf1917f95p7},
            {0x1.5159af804425ep272,     0x1.799fbb88055p7},
            {0x1.a5b01b605409p275,      0x1.7e3aa7f6f2a3ep7},
            {0x1.078e111c34e5bp279,     0x1.82d59465dff9fp7},
            {0x1.497195634225fp282,     0x1.877080d4cd4f4p7},
            {0x1.9bcdfabc13053p285,     0x1.8c0b6d43baa4ep7},
            {0x1.0160bcb58c08cp289,     0x1.90a659b2a7fa7p7},
            {0x1.41b8ebe2eec13p292,     0x1.95414621954f4p7},
            {0x1.922726dbaa542p295,     0x1.99dc329082a46p7},
            {0x1.f6b0f09295714p298,     0x1.9e771eff6ffa3p7},
            {0x1.3a2e965b9d0b2p302,     0x1.a3120b6e5d4eep7},
            {0x1.88ba3bf284dd1p305,     0x1.a7acf7dd4aa4ep7},
            {0x1.32d17ed576f35p312,     0x1.b0e2d0bb254ep7},
            {0x1.7f85de8ad56bep315,     0x1.b57dbd2a12a44p7},
            {0x1.df67562d87c5cp318,     0x1.ba18a998fff65p7},
            {0x1.2ba095dc76db7p322,     0x1.beb39607ed4fp7},
            {0x1.7688bb5394bd3p325,     0x1.c34e8276daa48p7},
            {0x1.d42aea2878b45p328,     0x1.c7e96ee5c7f87p7},
            {0x1.249ad2594989p332,      0x1.cc845b54b54a6p7},
        };

        for (double[] testCase: testCases)
            failures+=testLog1pCase(testCase[0], testCase[1]);

        return failures;
    }

    public static void main(String [] argv) {
        int failures = 0;

        failures += testLog1p();

        if (failures > 0) {
            System.err.println("Testing log1p incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }
}
