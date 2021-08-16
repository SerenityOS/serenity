/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5033578
 * @summary Tests for {Math, StrictMath}.tan
 * @author Joseph D. Darcy
 */

public class TanTests {
    private TanTests(){}

    static int testTanCase(double input, double expected, double ulps) {
        int failures = 0;
        failures += Tests.testUlpDiff("StrictMath.tan(double, double)", input,
                               StrictMath.tan(input), expected, ulps);
        failures += Tests.testUlpDiff("Math.tan(double, double)", input,
                               Math.tan(input), expected, ulps);
        return failures;
    }

    static int testTan() {
        int failures = 0;

        double [][] testCases = {
            // 1.1 ulp case from Gonnet
            {0x1.31b97c4000001p24,      -0x1.d08538b656222p34,  1.9},
            // Remaining test cases adapted from work by Alex Liu
            {0x1.be1b2d17ba207p6, -0x1.cf489c89f8066p49, 1.100000},
            {0x1.e0a9e6ab97de7p7, 0x1.d31ce95f57459p50, 1.100000},
            {0x1.23f8c5bcf003ep11, 0x1.f022585dbb50ap50, 1.100000},
            {0x1.44bdb557e1dc1p20, 0x1.b67eaf362701fp49, 1.100000},
            {0x1.604759040fb6fp68, 0x1.d574bc1f9e903p50, 1.100000},
            {0x1.3d33fa4e5ba47p70, 0x1.ce1dd6e33fef8p49, 1.100000},
            {0x1.f19e5d71b26bap85, 0x1.c2536a9119dd2p55, 1.100000},
            {0x1.43ed062d2d62cp88, -0x1.c94b0c5b7b05p49, 1.100000},
            {0x1.b7b895b030bep88, -0x1.cba9ebb0f20b9p51, 1.100000},
            {0x1.a86090fe7c144p95, 0x1.d5ad72ca48bbfp48, 1.100000},
            {0x1.d199df0700a61p95, -0x1.b8dd636f8dba7p49, 1.100000},
            {0x1.d713037d1d222p106, -0x1.d57f035fd0146p48, 1.100000},
            {0x1.ed1f6b066569bp115, 0x1.840af46cc9bep48, 1.100000},
            {0x1.16800a51eff75p118, 0x1.c9f91caf08a6ap49, 1.100000},
            {0x1.c1169c1040ecdp134, 0x1.e44a7eb56cb7p48, 1.100000},
            {0x1.19b0fb40dddd5p145, -0x1.f1b1c235774b2p48, 1.100000},
            {0x1.4d6b47f2480f8p162, 0x1.da1c2010795a5p51, 1.100000},
            {0x1.682ff8e5429ddp163, -0x1.95a7aee1e93bep55, 1.100000},
            {0x1.d0569fad9657dp204, -0x1.8f2ca17123aa5p49, 1.100000},
            {0x1.55505de5bbc14p206, -0x1.e8d28e39ddf9p50, 1.100000},
            {0x1.cf497083e6c77p206, -0x1.fd3fbaa40de18p49, 1.100000},
            {0x1.c5b30c8686203p214, 0x1.f4d14469638a9p48, 1.100000},
            {0x1.60d15b12ff0b7p217, 0x1.bc150932bd3d7p48, 1.100000},
            {0x1.07cc6858d980bp218, -0x1.f3f7355c983a5p51, 1.100000},
            {0x1.e06a67cd86969p218, 0x1.b0873124d98afp51, 1.100000},
            {0x1.49704174c38e3p229, 0x1.e0301142ccbc2p49, 1.100000},
            {0x1.ea19ceab3b06ap230, -0x1.fc22e687f0482p48, 1.100000},
            {0x1.0c905503fea72p236, -0x1.7d4e9a45014d5p49, 1.100000},
            {0x1.28eb1f8ddd7c3p257, -0x1.a951893680c71p49, 1.100000},
            {0x1.310b11af2bfbep260, 0x1.84d458039c2e6p48, 1.100000},
            {0x1.f3c172bb7afc2p265, -0x1.fb3139d3ba04fp49, 1.100000},
            {0x1.54a28326cfedep267, 0x1.f416de8fb60bap53, 1.100000},
            {0x1.5a5154d9d609dp269, -0x1.83d74cea8141p51, 1.100000},
            {0x1.3ee75fd803b21p275, 0x1.b9ab67b61bf65p50, 1.100000},
            {0x1.f4a4c781834d9p277, -0x1.d639ec63bf3b6p49, 1.100000},
            {0x1.2053d5c14cf78p279, 0x1.fc31413372cdcp50, 1.100000},
            {0x1.896d0a9acee4cp298, 0x1.f9136d6e27a5cp48, 1.100000},
            {0x1.f010da08a862p302, -0x1.fd812c5e13483p49, 1.100000},
            {0x1.65f2e272f729fp308, -0x1.f9f642ddaa32dp49, 1.100000},
            {0x1.a8afbc4edb07dp309, 0x1.fa0d458320902p52, 1.100000},
            {0x1.4d311a5447cdep329, -0x1.f7e98fe193e81p49, 1.100000},
            {0x1.808f66338b21bp345, -0x1.bceaf45f61155p49, 1.100000},
            {0x1.5a34aacf5ded1p350, 0x1.d41f0f13fadd4p49, 1.100000},
            {0x1.3e8b85532bad1p354, -0x1.f0b21179d663ep49, 1.100000},
            {0x1.1c2ecf01570acp394, -0x1.c215c9e2b7b24p49, 1.100000},
            {0x1.666eba99d2837p402, 0x1.fbd5c4b527506p48, 1.100000},
            {0x1.6cc39f07fafbbp460, -0x1.f087548a00e7cp49, 1.100000},
            {0x1.9481228fea3ffp463, -0x1.c585e64ff44c8p48, 1.100000},
            {0x1.79c3af0b4d0d4p466, 0x1.c9ed3716691f2p51, 1.100000},
            {0x1.993ea84c3e23bp468, 0x1.a6b3954fc37f3p49, 1.100000},
            {0x1.cfd6b13f64408p470, -0x1.f4db7cc2c09bp47, 1.100000},
            {0x1.b820ccdd52299p473, 0x1.77a1ff863b0f3p52, 1.100000},
            {0x1.157ef3a1528a5p475, -0x1.f4e14ddc45e49p51, 1.100000},
            {0x1.b492a8997bc36p478, -0x1.e0db26b7f03e8p48, 1.100000},
            {0x1.e0ea5674b831bp480, 0x1.e0ad6b3cdccdfp48, 1.100000},
            {0x1.c62ac8b32cb9ep497, 0x1.c95d00a36f677p48, 1.100000},
            {0x1.467f1daf12b43p498, 0x1.c6d3fdc096f0bp50, 1.100000},
            {0x1.336e5a83e390cp502, 0x1.fc873dae28572p48, 1.100000},
            {0x1.aaab1de0d6727p506, -0x1.e0482967d0354p49, 1.100000},
            {0x1.e5ce06a12139cp507, 0x1.cea42e29735bdp49, 1.100000},
            {0x1.87dad74d0dda8p516, -0x1.b2cde6c0a8b9fp48, 1.100000},
            {0x1.e4feb94ee0989p524, -0x1.b227d0d0ffaa8p49, 1.100000},
            {0x1.31c082b1361ebp525, 0x1.a7ed49158d736p49, 1.100000},
            {0x1.56913865b3e16p531, 0x1.eeb7a32591c3bp52, 1.100000},
            {0x1.36ade1fa883cap544, -0x1.fa087aadc0cbp48, 1.100000},
            {0x1.de57314df4af8p559, 0x1.c686aa5a41075p49, 1.100000},
            {0x1.0bb29bf7960ddp586, -0x1.d29ae1a3023cep50, 1.100000},
            {0x1.049a584685941p588, -0x1.eebfb159dba67p51, 1.100000},
            {0x1.33c1d4257b294p589, 0x1.ea1eedabea109p48, 1.100000},
            {0x1.3587e511bf47bp590, 0x1.c897858ce0ca9p48, 1.100000},
            {0x1.d12ee010c0facp590, 0x1.ab5b4b5065aa3p48, 1.100000},
            {0x1.87bbed5af48d9p605, 0x1.f512c3b2be7cap50, 1.100000},
            {0x1.a0b1131240cebp605, -0x1.fa373983fd571p48, 1.100000},
            {0x1.116fdda1a04c9p616, -0x1.d76fdbc8552f3p51, 1.100000},
            {0x1.67ebae833a034p620, 0x1.e1313af0a4075p50, 1.100000},
            {0x1.9a50fbc5b0fecp627, 0x1.d89150884fbf7p50, 1.100000},
            {0x1.6d625e0757e9cp631, -0x1.d0a5ecf002555p49, 1.100000},
            {0x1.e880344cc9913p636, -0x1.fafd04caaf58bp48, 1.100000},
            {0x1.e0a180b843cc5p650, 0x1.ea2aea3b8c953p49, 1.100000},
            {0x1.fa91ce15157b2p652, 0x1.e6f5f4d47d83fp48, 1.100000},
            {0x1.7696347caf8dfp654, 0x1.e0d36f2aef7dap51, 1.100000},
            {0x1.886484b536161p666, -0x1.e3c96481e335bp51, 1.100000},
            {0x1.0aa3ff2b41abdp675, -0x1.b3300ee04b4c8p50, 1.100000},
            {0x1.d695ac08fe897p675, -0x1.c27fd21ecb13p51, 1.100000},
            {0x1.4c1e532d7a99ap680, 0x1.e2ec695260c39p49, 1.100000},
            {0x1.44a9f3e395802p685, -0x1.e7273ab9ce8e2p52, 1.100000},
            {0x1.3a25ec2b43d45p697, -0x1.d23187ba6321ep49, 1.100000},
            {0x1.96f5c2420c3fdp716, -0x1.ea06ab71ad719p49, 1.100000},
            {0x1.926c063a9406bp741, 0x1.e3d3d9262fd66p48, 1.100000},
            {0x1.1a57713d6fd93p754, -0x1.c10074d49490dp48, 1.100000},
            {0x1.739387922e672p772, 0x1.bda527e215a3cp49, 1.100000},
            {0x1.d286eff17f4d4p793, 0x1.d01c678ebfa1p49, 1.100000},
            {0x1.f3d777206a062p794, -0x1.d8604b6d18385p49, 1.100000},
            {0x1.ae91e6574da91p826, -0x1.fd1b26ab656c2p49, 1.100000},
            {0x1.4422b3c871c9p836, 0x1.9d2cab1f3aebcp48, 1.100000},
            {0x1.7ff8537071e1p840, 0x1.badde451c6ed7p48, 1.100000},
            {0x1.c6fe9202e219dp845, -0x1.b2aa20745de3p51, 1.100000},
            {0x1.a95a0b4015d88p846, 0x1.cdf5dfd045657p50, 1.100000},
            {0x1.f823b9cff0daep867, 0x1.fd72fce3d5505p48, 1.100000},
            {0x1.a6bee2afcd2fp886, 0x1.fe06265cd3aebp49, 1.100000},
            {0x1.7b034b3412d17p892, 0x1.e48055812d391p50, 1.100000},
            {0x1.58588f8cda276p894, 0x1.f806fddf0dd05p53, 1.100000},
            {0x1.ce750a7963463p896, 0x1.e94f1f4018402p48, 1.100000},
            {0x1.3d50a91fe82cfp897, 0x1.cd518fda10e95p48, 1.100000},
            {0x1.f82dea1c0b809p897, -0x1.d6a0ef08179c5p48, 1.100000},
            {0x1.38673e8c6a4afp903, 0x1.f4113a036478p48, 1.100000},
            {0x1.dfb75e4a7432p911, 0x1.eb7bc6cb4d7f3p48, 1.100000},
            {0x1.1230b975a72b3p916, -0x1.e1042be0759f9p48, 1.100000},
            {0x1.302c2f5a4e6e5p916, 0x1.f66a9874cd60ap48, 1.100000},
            {0x1.04e07a1d67b93p921, 0x1.87735139f6a0bp53, 1.100000},
            {0x1.5a3eb79cd06fap931, -0x1.e00930c219ef3p51, 1.100000},
            {0x1.8fb45679936fp937, 0x1.9a427588645c4p50, 1.100000},
            {0x1.c4abb225260c6p964, -0x1.d1e64e91ac6ap50, 1.100000},
            {0x1.b43e449b25382p982, -0x1.f1848cc5ac4fep50, 1.100000},
            {0x1.504d9d7179b1ap983, 0x1.a4e51ea807786p48, 1.100000},
            {0x1.83a5af80fb39bp987, 0x1.a6dde6c2220ebp48, 1.100000},
            {0x1.5d978d9ad84c8p1011, 0x1.ec96900bfd1ddp51, 1.100000},
        };

        for(double[] testCase: testCases) {
            failures += testTanCase(testCase[0], testCase[1], testCase[2]);
        }

        return failures;
    }

    public static void main(String [] argv) {
        int failures = 0;

        failures += testTan();

        if (failures > 0) {
            System.err.println("Testing tan incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }
}
