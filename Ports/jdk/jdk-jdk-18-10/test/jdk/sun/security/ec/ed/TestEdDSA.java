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
 * @summary EdDSA Signature Known Answer Tests (KAT) from RFC 8032
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main TestEdDSA
 */

import java.security.*;
import java.security.spec.*;
import java.util.*;
import jdk.test.lib.Convert;

public class TestEdDSA {

    private static SecureRandom random = new SecureRandom();

    public static void main(String[] args) throws Exception {
        runBasicTests();
        runKAT();
        runCurveMixTest();
    }

    private static void runKAT() throws Exception {

        // "pure" Ed25519
        runSignTest("Ed25519", null,
            "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
            "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
            "",
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
            "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");

        runSignTest("Ed25519", null,
            "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
            "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
            "72",
            "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da" +
            "085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00");

        runSignTest("Ed25519", null,
            "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
            "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
            "af82",
            "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac" +
            "18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a");

        runSignTest("Ed25519", null,
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

        runSignTest("Ed25519", null,
            "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42",
            "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf",
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
            "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            "dc2a4459e7369633a52b1bf277839a00201009a3efbf3ecb69bea2186c26b589" +
            "09351fc9ac90b3ecfdfbc7c66431e0303dca179c138ac17ad9bef1177331a704");

        // Ed25519ctx
        byte[] context = HexFormat.of().parseHex("666f6f");
        runSignTest("Ed25519", new EdDSAParameterSpec(false, context),
            "0305334e381af78f141cb666f6199f57bc3495335a256a95bd2a55bf546663f6",
            "dfc9425e4f968f7f0c29f0259cf5f9aed6851c2bb4ad8bfb860cfee0ab248292",
            "f726936d19c800494e3fdaff20b276a8",
            "55a4cc2f70a54e04288c5f4cd1e45a7bb520b36292911876cada7323198dd87a" +
            "8b36950b95130022907a7fb7c4e9b2d5f6cca685a587b4b21f4b888e4e7edb0d");

       context = HexFormat.of().parseHex("626172");
        runSignTest("Ed25519", new EdDSAParameterSpec(false, context),
            "0305334e381af78f141cb666f6199f57bc3495335a256a95bd2a55bf546663f6",
            "dfc9425e4f968f7f0c29f0259cf5f9aed6851c2bb4ad8bfb860cfee0ab248292",
            "f726936d19c800494e3fdaff20b276a8",
            "fc60d5872fc46b3aa69f8b5b4351d5808f92bcc044606db097abab6dbcb1aee3" +
            "216c48e8b3b66431b5b186d1d28f8ee15a5ca2df6668346291c2043d4eb3e90d");

        context = HexFormat.of().parseHex("666f6f");
        runSignTest("Ed25519", new EdDSAParameterSpec(false, context),
            "0305334e381af78f141cb666f6199f57bc3495335a256a95bd2a55bf546663f6",
            "dfc9425e4f968f7f0c29f0259cf5f9aed6851c2bb4ad8bfb860cfee0ab248292",
            "508e9e6882b979fea900f62adceaca35",
            "8b70c1cc8310e1de20ac53ce28ae6e7207f33c3295e03bb5c0732a1d20dc6490" +
            "8922a8b052cf99b7c4fe107a5abb5b2c4085ae75890d02df26269d8945f84b0b");

        context = HexFormat.of().parseHex("666f6f");
        runSignTest("Ed25519", new EdDSAParameterSpec(false, context),
            "ab9c2853ce297ddab85c993b3ae14bcad39b2c682beabc27d6d4eb20711d6560",
            "0f1d1274943b91415889152e893d80e93275a1fc0b65fd71b4b0dda10ad7d772",
            "f726936d19c800494e3fdaff20b276a8",
            "21655b5f1aa965996b3f97b3c849eafba922a0a62992f73b3d1b73106a84ad85" +
            "e9b86a7b6005ea868337ff2d20a7f5fbd4cd10b0be49a68da2b2e0dc0ad8960f");

        // Ed25519ph
        runSignTest("Ed25519", new EdDSAParameterSpec(true),
            "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42",
            "ec172b93ad5e563bf4932c70e1245034c35467ef2efd4d64ebf819683467e2bf",
            "616263",
            "98a70222f0b8121aa9d30f813d683f809e462b469c7ff87639499bb94e6dae41" +
                "31f85042463c2a355a2003d062adf5aaa10b8c61e636062aaad11c2a26083406");

        // Ed448
        runSignTest("Ed448", null,
            "6c82a562cb808d10d632be89c8513ebf6c929f34ddfa8c9f63c9960ef6e348a3" +
            "528c8a3fcc2f044e39a3fc5b94492f8f032e7549a20098f95b",
            "5fd7449b59b461fd2ce787ec616ad46a1da1342485a70e1f8a0ea75d80e96778" +
            "edf124769b46c7061bd6783df1e50f6cd1fa1abeafe8256180",
            "",
            "533a37f6bbe457251f023c0d88f976ae2dfb504a843e34d2074fd823d41a591f" +
            "2b233f034f628281f2fd7a22ddd47d7828c59bd0a21bfd3980ff0d2028d4b18a" +
            "9df63e006c5d1c2d345b925d8dc00b4104852db99ac5c7cdda8530a113a0f4db" +
            "b61149f05a7363268c71d95808ff2e652600");

        runSignTest("Ed448", null,
            "c4eab05d357007c632f3dbb48489924d552b08fe0c353a0d4a1f00acda2c463a" +
            "fbea67c5e8d2877c5e3bc397a659949ef8021e954e0a12274e",
            "43ba28f430cdff456ae531545f7ecd0ac834a55d9358c0372bfa0c6c6798c086" +
            "6aea01eb00742802b8438ea4cb82169c235160627b4c3a9480",
            "03",
            "26b8f91727bd62897af15e41eb43c377efb9c610d48f2335cb0bd0087810f435" +
            "2541b143c4b981b7e18f62de8ccdf633fc1bf037ab7cd779805e0dbcc0aae1cb" +
            "cee1afb2e027df36bc04dcecbf154336c19f0af7e0a6472905e799f1953d2a0f" +
            "f3348ab21aa4adafd1d234441cf807c03a00");

        context = HexFormat.of().parseHex("666f6f");
        runSignTest("Ed448", new EdDSAParameterSpec(false, context),
            "c4eab05d357007c632f3dbb48489924d552b08fe0c353a0d4a1f00acda2c463a" +
            "fbea67c5e8d2877c5e3bc397a659949ef8021e954e0a12274e",
            "43ba28f430cdff456ae531545f7ecd0ac834a55d9358c0372bfa0c6c6798c086" +
            "6aea01eb00742802b8438ea4cb82169c235160627b4c3a9480",
            "03",
            "d4f8f6131770dd46f40867d6fd5d5055de43541f8c5e35abbcd001b32a89f7d2" +
            "151f7647f11d8ca2ae279fb842d607217fce6e042f6815ea000c85741de5" +
            "c8da1144a6a1aba7f96de42505d7a7298524fda538fccbbb754f578c1cad" +
            "10d54d0d5428407e85dcbc98a49155c13764e66c3c00");

        runSignTest("Ed448", null,
            "cd23d24f714274e744343237b93290f511f6425f98e64459ff203e898508" +
            "3ffdf60500553abc0e05cd02184bdb89c4ccd67e187951267eb328",
            "dcea9e78f35a1bf3499a831b10b86c90aac01cd84b67a0109b55a36e9328" +
            "b1e365fce161d71ce7131a543ea4cb5f7e9f1d8b00696447001400",
            "0c3e544074ec63b0265e0c",
            "1f0a8888ce25e8d458a21130879b840a9089d999aaba039eaf3e3afa090a09d3" +
            "89dba82c4ff2ae8ac5cdfb7c55e94d5d961a29fe0109941e00b8dbdeea6d3b05" +
            "1068df7254c0cdc129cbe62db2dc957dbb47b51fd3f213fb8698f064774250a5" +
            "028961c9bf8ffd973fe5d5c206492b140e00");

        runSignTest("Ed448", null,
            "258cdd4ada32ed9c9ff54e63756ae582fb8fab2ac721f2c8e676a72768513d93" +
            "9f63dddb55609133f29adf86ec9929dccb52c1c5fd2ff7e21b",
            "3ba16da0c6f2cc1f30187740756f5e798d6bc5fc015d7c63cc9510ee3fd44adc" +
            "24d8e968b6e46e6f94d19b945361726bd75e149ef09817f580",
            "64a65f3cdedcdd66811e2915",
            "7eeeab7c4e50fb799b418ee5e3197ff6bf15d43a14c34389b59dd1a7b1b85b4ae904" +
            "38aca634bea45e3a2695f1270f07fdcdf7c62b8efeaf00b45c2c96ba457eb1a8" +
            "bf075a3db28e5c24f6b923ed4ad747c3c9e03c7079efb87cb110d3a99861e720" +
            "03cbae6d6b8b827e4e6c143064ff3c00");

        runSignTest("Ed448", null,
            "7ef4e84544236752fbb56b8f31a23a10e42814f5f55ca037cdcc11c64c9a3b29" +
            "49c1bb60700314611732a6c2fea98eebc0266a11a93970100e",
            "b3da079b0aa493a5772029f0467baebee5a8112d9d3a22532361da294f7bb381" +
            "5c5dc59e176b4d9f381ca0938e13c6c07b174be65dfa578e80",
            "64a65f3cdedcdd66811e2915e7",
            "6a12066f55331b6c22acd5d5bfc5d71228fbda80ae8dec26bdd306743c5027cb" +
            "4890810c162c027468675ecf645a83176c0d7323a2ccde2d80efe5a1268e" +
            "8aca1d6fbc194d3f77c44986eb4ab4177919ad8bec33eb47bbb5fc6e2819" +
            "6fd1caf56b4e7e0ba5519234d047155ac727a1053100");

        runSignTest("Ed448", null,
            "d65df341ad13e008567688baedda8e9dcdc17dc024974ea5b4227b6530e339bf" +
            "f21f99e68ca6968f3cca6dfe0fb9f4fab4fa135d5542ea3f01",
            "df9705f58edbab802c7f8363cfe5560ab1c6132c20a9f1dd163483a26f8ac53a" +
            "39d6808bf4a1dfbd261b099bb03b3fb50906cb28bd8a081f00",
            "bd0f6a3747cd561bdddf4640a332461a4a30a12a434cd0bf40d766d9c6d458e5" +
            "512204a30c17d1f50b5079631f64eb3112182da3005835461113718d1a5ef944",
            "554bc2480860b49eab8532d2a533b7d578ef473eeb58c98bb2d0e1ce488a98b1" +
            "8dfde9b9b90775e67f47d4a1c3482058efc9f40d2ca033a0801b63d45b3b722e" +
            "f552bad3b4ccb667da350192b61c508cf7b6b5adadc2c8d9a446ef003fb05cba" +
            "5f30e88e36ec2703b349ca229c2670833900");

        runSignTest("Ed448", new EdDSAParameterSpec(false),
            "2ec5fe3c17045abdb136a5e6a913e32ab75ae68b53d2fc149b77e504132d3756" +
            "9b7e766ba74a19bd6162343a21c8590aa9cebca9014c636df5",
            "79756f014dcfe2079f5dd9e718be4171e2ef2486a08f25186f6bff43a9936b9b" +
            "fe12402b08ae65798a3d81e22e9ec80e7690862ef3d4ed3a00",
            "15777532b0bdd0d1389f636c5f6b9ba734c90af572877e2d272dd078aa1e567c" +
            "fa80e12928bb542330e8409f3174504107ecd5efac61ae7504dabe2a602ede89" +
            "e5cca6257a7c77e27a702b3ae39fc769fc54f2395ae6a1178cab4738e543072f" +
            "c1c177fe71e92e25bf03e4ecb72f47b64d0465aaea4c7fad372536c8ba516a60" +
            "39c3c2a39f0e4d832be432dfa9a706a6e5c7e19f397964ca4258002f7c0541b5" +
            "90316dbc5622b6b2a6fe7a4abffd96105eca76ea7b98816af0748c10df048ce0" +
            "12d901015a51f189f3888145c03650aa23ce894c3bd889e030d565071c59f409" +
            "a9981b51878fd6fc110624dcbcde0bf7a69ccce38fabdf86f3bef6044819de11",
            "c650ddbb0601c19ca11439e1640dd931f43c518ea5bea70d3dcde5f4191fe53f" +
            "00cf966546b72bcc7d58be2b9badef28743954e3a44a23f880e8d4f1cfce2d7a" +
            "61452d26da05896f0a50da66a239a8a188b6d825b3305ad77b73fbac0836ecc6" +
            "0987fd08527c1a8e80d5823e65cafe2a3d00");

        runSignTest("Ed448", null,
            "872d093780f5d3730df7c212664b37b8a0f24f56810daa8382cd4f" +
            "a3f77634ec44dc54f1c2ed9bea86fafb7632d8be199ea165f5ad55dd9ce8",
            "a81b2e8a70a5ac94ffdbcc9badfc3feb0801f258578bb114ad44ece" +
            "1ec0e799da08effb81c5d685c0c56f64eecaef8cdf11cc38737838cf400",
            "6ddf802e1aae4986935f7f981ba3f0351d6273c0a0c22c9c0e8339168e675412" +
            "a3debfaf435ed651558007db4384b650fcc07e3b586a27a4f7a00ac8a6fec2cd" +
            "86ae4bf1570c41e6a40c931db27b2faa15a8cedd52cff7362c4e6e23daec0fbc" +
            "3a79b6806e316efcc7b68119bf46bc76a26067a53f296dafdbdc11c77f7777e9" +
            "72660cf4b6a9b369a6665f02e0cc9b6edfad136b4fabe723d2813db3136cfde9" +
            "b6d044322fee2947952e031b73ab5c603349b307bdc27bc6cb8b8bbd7bd32321" +
            "9b8033a581b59eadebb09b3c4f3d2277d4f0343624acc817804728b25ab79717" +
            "2b4c5c21a22f9c7839d64300232eb66e53f31c723fa37fe387c7d3e50bdf9813" +
            "a30e5bb12cf4cd930c40cfb4e1fc622592a49588794494d56d24ea4b40c89fc0" +
            "596cc9ebb961c8cb10adde976a5d602b1c3f85b9b9a001ed3c6a4d3b1437f520" +
            "96cd1956d042a597d561a596ecd3d1735a8d570ea0ec27225a2c4aaff26306d1" +
            "526c1af3ca6d9cf5a2c98f47e1c46db9a33234cfd4d81f2c98538a09ebe76998" +
            "d0d8fd25997c7d255c6d66ece6fa56f11144950f027795e653008f4bd7ca2dee" +
            "85d8e90f3dc315130ce2a00375a318c7c3d97be2c8ce5b6db41a6254ff264fa6" +
            "155baee3b0773c0f497c573f19bb4f4240281f0b1f4f7be857a4e59d416c06b4" +
            "c50fa09e1810ddc6b1467baeac5a3668d11b6ecaa901440016f389f80acc4db9" +
            "77025e7f5924388c7e340a732e554440e76570f8dd71b7d640b3450d1fd5f041" +
            "0a18f9a3494f707c717b79b4bf75c98400b096b21653b5d217cf3565c9597456" +
            "f70703497a078763829bc01bb1cbc8fa04eadc9a6e3f6699587a9e75c94e5bab" +
            "0036e0b2e711392cff0047d0d6b05bd2a588bc109718954259f1d86678a579a3" +
            "120f19cfb2963f177aeb70f2d4844826262e51b80271272068ef5b3856fa8535" +
            "aa2a88b2d41f2a0e2fda7624c2850272ac4a2f561f8f2f7a318bfd5caf969614" +
            "9e4ac824ad3460538fdc25421beec2cc6818162d06bbed0c40a387192349db67" +
            "a118bada6cd5ab0140ee273204f628aad1c135f770279a651e24d8c14d75a605" +
            "9d76b96a6fd857def5e0b354b27ab937a5815d16b5fae407ff18222c6d1ed263" +
            "be68c95f32d908bd895cd76207ae726487567f9a67dad79abec316f683b17f2d" +
            "02bf07e0ac8b5bc6162cf94697b3c27cd1fea49b27f23ba2901871962506520c" +
            "392da8b6ad0d99f7013fbc06c2c17a569500c8a7696481c1cd33e9b14e40b82e" +
            "79a5f5db82571ba97bae3ad3e0479515bb0e2b0f3bfcd1fd33034efc6245eddd" +
            "7ee2086ddae2600d8ca73e214e8c2b0bdb2b047c6a464a562ed77b73d2d841c4" +
            "b34973551257713b753632efba348169abc90a68f42611a40126d7cb21b58695" +
            "568186f7e569d2ff0f9e745d0487dd2eb997cafc5abf9dd102e62ff66cba87",
            "e301345a41a39a4d72fff8df69c98075a0cc082b802fc9b2b6bc503f926b65bd" +
            "df7f4c8f1cb49f6396afc8a70abe6d8aef0db478d4c6b2970076c6a0484fe76d" +
            "76b3a97625d79f1ce240e7c576750d295528286f719b413de9ada3e8eb78ed57" +
            "3603ce30d8bb761785dc30dbc320869e1a00");

        runSignTest("Ed448", new EdDSAParameterSpec(true),
            "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901" +
            "b96dca3d42ef7822e0d5104127dc05d6dbefde69e3ab2cec7c867c6e2c49",
            "259b71c19f83ef77a7abd26524cbdb3161b590a48f7d17de3ee0ba9" +
            "c52beb743c09428a131d6b1b57303d90d8132c276d5ed3d5d01c0f53880",
            "616263",
            "822f6901f7480f3d5f562c592994d9693602875614483256505600bbc281ae38" +
            "1f54d6bce2ea911574932f52a4e6cadd78769375ec3ffd1b801a0d9b3f4030cd" +
            "433964b6457ea39476511214f97469b57dd32dbc560a9a94d00bff07620464a3" +
            "ad203df7dc7ce360c3cd3696d9d9fab90f00");

        runSignTest("Ed448", new EdDSAParameterSpec(true, context),
            "833fe62409237b9d62ec77587520911e9a759cec1d19755b7da901b96dca3d42" +
            "ef7822e0d5104127dc05d6dbefde69e3ab2cec7c867c6e2c49",
            "259b71c19f83ef77a7abd26524cbdb3161b590a48f7d17de3ee0ba9c52beb743" +
            "c09428a131d6b1b57303d90d8132c276d5ed3d5d01c0f53880",
            "616263",
            "c32299d46ec8ff02b54540982814dce9a05812f81962b649d528095916a2aa48" +
            "1065b1580423ef927ecf0af5888f90da0f6a9a85ad5dc3f280d91224ba9911a3" +
            "653d00e484e2ce232521481c8658df304bb7745a73514cdb9bf3e15784ab7128" +
            "4f8d0704a608c54a6b62d97beb511d132100");

        System.out.println("All test vectors passed");
    }

    private static void runSignTest(String algorithm,
            AlgorithmParameterSpec params, String privateKey, String publicKey,
            String message, String signature) throws Exception {

        byte[] privKeyBytes = HexFormat.of().parseHex(privateKey);
        EdECPoint pubKeyPoint = Convert.hexStringToEdPoint(publicKey);
        byte[] msgBytes = HexFormat.of().parseHex(message);
        byte[] computedSig;

        NamedParameterSpec namedSpec = new NamedParameterSpec(algorithm);
        EdECPrivateKeySpec privKeySpec =
            new EdECPrivateKeySpec(namedSpec, privKeyBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        PrivateKey privKey = kf.generatePrivate(privKeySpec);
        Signature sig = Signature.getInstance(algorithm);
        if (params != null) {
            sig.setParameter(params);
        }
        sig.initSign(privKey);
        sig.update(msgBytes);
        computedSig = sig.sign();
        if (!Arrays.equals(computedSig, HexFormat.of().parseHex(signature))) {
            throw new RuntimeException("Incorrect signature");
        }

        // test verification
        sig = Signature.getInstance(algorithm);
        if (params != null) {
            sig.setParameter(params);
        }
        EdECPublicKeySpec pubKeySpec =
            new EdECPublicKeySpec(namedSpec, pubKeyPoint);
        PublicKey pubKey = kf.generatePublic(pubKeySpec);
        sig.initVerify(pubKey);
        sig.update(msgBytes);
        if (!sig.verify(computedSig)) {
            throw new RuntimeException("Signature did not verify");
        }
    }


    private static void runBasicTests() throws Exception {
        runBasicTest("EdDSA", null);

        runBasicTest("EdDSA", 255);
        runBasicTest("EdDSA", "Ed25519");
        runBasicTest("Ed25519", null);
        runBasicTest("1.3.101.112", null);
        runBasicTest("OID.1.3.101.112", null);

        runBasicTest("EdDSA", 448);
        runBasicTest("EdDSA", "Ed448");
        runBasicTest("Ed448", null);
        runBasicTest("1.3.101.113", null);
        runBasicTest("OID.1.3.101.113", null);
    }

    private static void runBasicTest(String name, Object param)
        throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name);
        if (param instanceof Integer) {
            kpg.initialize((Integer) param, random);
        } else if (param instanceof String) {
            kpg.initialize(new NamedParameterSpec((String) param), random);
        }
        KeyPair kp = kpg.generateKeyPair();

        Signature sig = Signature.getInstance(name);
        sig.initSign(kp.getPrivate());
        byte[] testMessage = new byte[1024];
        random.nextBytes(testMessage);
        sig.update(testMessage);
        byte[] msgSig = sig.sign();

        // sign again, should return false
        byte[] x = sig.sign();
        if (Arrays.compare(msgSig, x) == 0) {
            throw new RuntimeException("Second sign identical, didn't reset");
        }

        // verify the signature
        sig.initVerify(kp.getPublic());
        sig.update(testMessage);
        if (!sig.verify(msgSig)) {
            throw new RuntimeException("Valid signature did not verify");
        }

        // verify again, should return false
        if (sig.verify(msgSig)) {
            throw new RuntimeException("Second verify succeeded, didn't reset");
        }

        // try verifying an incorrect signature
        testMessage[0] ^= (byte) 0x01;
        sig.update(testMessage);
        if (sig.verify(msgSig)) {
            throw new RuntimeException("Invalid signature verified");
        }

        KeyFactory kf = KeyFactory.getInstance(name);
        // Test with X509 and PKCS8 key specs
        X509EncodedKeySpec pubSpec =
            kf.getKeySpec(kp.getPublic(), X509EncodedKeySpec.class);
        PKCS8EncodedKeySpec priSpec =
            kf.getKeySpec(kp.getPrivate(), PKCS8EncodedKeySpec.class);

        PublicKey pubKey = kf.generatePublic(pubSpec);
        PrivateKey priKey = kf.generatePrivate(priSpec);

        sig.initSign(priKey);
        sig.update(testMessage);
        msgSig = sig.sign();
        sig.initVerify(pubKey);
        sig.update(testMessage);
        if (!sig.verify(msgSig)) {
            throw new RuntimeException("Valid signature did not verify");
        }

        // test with EdEC key specs
        EdECPublicKeySpec edPublic =
            kf.getKeySpec(kp.getPublic(), EdECPublicKeySpec.class);
        EdECPrivateKeySpec edPrivate =
            kf.getKeySpec(kp.getPrivate(), EdECPrivateKeySpec.class);
        PublicKey pubKey2 = kf.generatePublic(edPublic);
        PrivateKey priKey2 = kf.generatePrivate(edPrivate);
        sig.initSign(priKey2);
        sig.update(testMessage);
        msgSig = sig.sign();
        sig.initVerify(pubKey2);
        sig.update(testMessage);
        if (!sig.verify(msgSig)) {
            throw new RuntimeException("Valid signature did not verify");
        }
    }

    /*
     * Ensure that SunEC rejects parameters/points for the wrong curve
     * when the algorithm ID for a specific curve is specified.
     */
    private static void runCurveMixTest() throws Exception {
        runCurveMixTest("SunEC", "Ed25519", 448);
        runCurveMixTest("SunEC", "Ed25519", "Ed448");
        runCurveMixTest("SunEC", "Ed448", 255);
        runCurveMixTest("SunEC", "Ed448", "Ed25519");
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
            if (param instanceof String) {
                throw new RuntimeException(
                        "InvalidAlgorithmParameterException expected", ex);
            }
            // expected

        } catch (InvalidAlgorithmParameterException ex) {
            if (param instanceof Integer) {
                throw new RuntimeException("InvalidParameterException expected",
                        ex);
            }
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
            kf.getKeySpec(otherKp.getPublic(), EdECPublicKeySpec.class);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }
        try {
            kf.getKeySpec(otherKp.getPrivate(), EdECPrivateKeySpec.class);
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
        EdECPublicKeySpec otherPubSpec = otherKf.getKeySpec(otherKp.getPublic(),
            EdECPublicKeySpec.class);
        try {
            kf.generatePublic(otherPubSpec);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }
        EdECPrivateKeySpec otherPriSpec =
            otherKf.getKeySpec(otherKp.getPrivate(), EdECPrivateKeySpec.class);
        try {
            kf.generatePrivate(otherPriSpec);
            throw new RuntimeException(name + " KeyFactory accepted "
                + param.toString() + " key");
        } catch (InvalidKeySpecException ex) {
            // expected
        }

        // ensure the Signature rejects incorrect keys
        Signature sig = Signature.getInstance(name, providerName);
        try {
            sig.initSign(otherKp.getPrivate());
            throw new RuntimeException(name + " Signature accepted "
                + param.toString() + " signing key");
        } catch (InvalidKeyException ex) {
            // expected
        }

        try {
            sig.initVerify(otherKp.getPublic());
            throw new RuntimeException(name + " Signature accepted "
                + param.toString() + " verification key");
        } catch (InvalidKeyException ex) {
            // expected
        }
    }
}
