/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc;

import jdk.internal.vm.annotation.Contended;

/*
 * @test TestHumongousReferenceObjectParallel
 * @summary Test that verifies that iteration over large, plain Java objects, that potentially cross region boundaries, with references in them works.
 * @requires vm.gc.Parallel
 * @bug 8151499 8153734
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xmx128m -XX:+UseParallelGC -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 */

/*
 * @test TestHumongousReferenceObjectG1
 * @summary Test that verifies that iteration over large, plain Java objects, that potentially cross region boundaries on G1, with references in them works.
 * @requires vm.gc.G1
 * @bug 8151499 8153734
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xmx128m -XX:+UseG1GC -XX:G1HeapRegionSize=1M -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xmx128m -XX:+UseG1GC -XX:G1HeapRegionSize=2M -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xmx128m -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xmx128m -XX:+UseG1GC -XX:G1HeapRegionSize=8M -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 */

/*
 * @test TestHumongousReferenceObjectShenandoah
 * @summary Test that verifies that iteration over large, plain Java objects, that potentially cross region boundaries, with references in them works.
 * @requires vm.gc.Shenandoah
 * @bug 8151499 8153734
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xms128m -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahRegionSize=8M -XX:ContendedPaddingWidth=8192 gc.TestHumongousReferenceObject
 * @run main/othervm -XX:+EnableContended -XX:-RestrictContended -Xms128m -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahRegionSize=8M -XX:ContendedPaddingWidth=8192 -XX:+UnlockDiagnosticVMOptions -XX:+ShenandoahVerify gc.TestHumongousReferenceObject
 */
public class TestHumongousReferenceObject {

    /*
      Due to 300 fields with 8K @Contended padding around each field, it takes 2.4M bytes per instance.
      With small G1 regions, it is bound to cross regions. G1 should properly (card) mark the object nevertheless.
      With 128M heap, it is enough to allocate ~100 of these objects to provoke at least one GC.
     */

    static volatile Object instance;

    public static void main(String[] args) {
        for (int c = 0; c < 100; c++) {
            instance = new TestHumongousReferenceObject();
        }
    }

    @Contended Integer int_1 = new Integer(1);
    @Contended Integer int_2 = new Integer(2);
    @Contended Integer int_3 = new Integer(3);
    @Contended Integer int_4 = new Integer(4);
    @Contended Integer int_5 = new Integer(5);
    @Contended Integer int_6 = new Integer(6);
    @Contended Integer int_7 = new Integer(7);
    @Contended Integer int_8 = new Integer(8);
    @Contended Integer int_9 = new Integer(9);
    @Contended Integer int_10 = new Integer(10);
    @Contended Integer int_11 = new Integer(11);
    @Contended Integer int_12 = new Integer(12);
    @Contended Integer int_13 = new Integer(13);
    @Contended Integer int_14 = new Integer(14);
    @Contended Integer int_15 = new Integer(15);
    @Contended Integer int_16 = new Integer(16);
    @Contended Integer int_17 = new Integer(17);
    @Contended Integer int_18 = new Integer(18);
    @Contended Integer int_19 = new Integer(19);
    @Contended Integer int_20 = new Integer(20);
    @Contended Integer int_21 = new Integer(21);
    @Contended Integer int_22 = new Integer(22);
    @Contended Integer int_23 = new Integer(23);
    @Contended Integer int_24 = new Integer(24);
    @Contended Integer int_25 = new Integer(25);
    @Contended Integer int_26 = new Integer(26);
    @Contended Integer int_27 = new Integer(27);
    @Contended Integer int_28 = new Integer(28);
    @Contended Integer int_29 = new Integer(29);
    @Contended Integer int_30 = new Integer(30);
    @Contended Integer int_31 = new Integer(31);
    @Contended Integer int_32 = new Integer(32);
    @Contended Integer int_33 = new Integer(33);
    @Contended Integer int_34 = new Integer(34);
    @Contended Integer int_35 = new Integer(35);
    @Contended Integer int_36 = new Integer(36);
    @Contended Integer int_37 = new Integer(37);
    @Contended Integer int_38 = new Integer(38);
    @Contended Integer int_39 = new Integer(39);
    @Contended Integer int_40 = new Integer(40);
    @Contended Integer int_41 = new Integer(41);
    @Contended Integer int_42 = new Integer(42);
    @Contended Integer int_43 = new Integer(43);
    @Contended Integer int_44 = new Integer(44);
    @Contended Integer int_45 = new Integer(45);
    @Contended Integer int_46 = new Integer(46);
    @Contended Integer int_47 = new Integer(47);
    @Contended Integer int_48 = new Integer(48);
    @Contended Integer int_49 = new Integer(49);
    @Contended Integer int_50 = new Integer(50);
    @Contended Integer int_51 = new Integer(51);
    @Contended Integer int_52 = new Integer(52);
    @Contended Integer int_53 = new Integer(53);
    @Contended Integer int_54 = new Integer(54);
    @Contended Integer int_55 = new Integer(55);
    @Contended Integer int_56 = new Integer(56);
    @Contended Integer int_57 = new Integer(57);
    @Contended Integer int_58 = new Integer(58);
    @Contended Integer int_59 = new Integer(59);
    @Contended Integer int_60 = new Integer(60);
    @Contended Integer int_61 = new Integer(61);
    @Contended Integer int_62 = new Integer(62);
    @Contended Integer int_63 = new Integer(63);
    @Contended Integer int_64 = new Integer(64);
    @Contended Integer int_65 = new Integer(65);
    @Contended Integer int_66 = new Integer(66);
    @Contended Integer int_67 = new Integer(67);
    @Contended Integer int_68 = new Integer(68);
    @Contended Integer int_69 = new Integer(69);
    @Contended Integer int_70 = new Integer(70);
    @Contended Integer int_71 = new Integer(71);
    @Contended Integer int_72 = new Integer(72);
    @Contended Integer int_73 = new Integer(73);
    @Contended Integer int_74 = new Integer(74);
    @Contended Integer int_75 = new Integer(75);
    @Contended Integer int_76 = new Integer(76);
    @Contended Integer int_77 = new Integer(77);
    @Contended Integer int_78 = new Integer(78);
    @Contended Integer int_79 = new Integer(79);
    @Contended Integer int_80 = new Integer(80);
    @Contended Integer int_81 = new Integer(81);
    @Contended Integer int_82 = new Integer(82);
    @Contended Integer int_83 = new Integer(83);
    @Contended Integer int_84 = new Integer(84);
    @Contended Integer int_85 = new Integer(85);
    @Contended Integer int_86 = new Integer(86);
    @Contended Integer int_87 = new Integer(87);
    @Contended Integer int_88 = new Integer(88);
    @Contended Integer int_89 = new Integer(89);
    @Contended Integer int_90 = new Integer(90);
    @Contended Integer int_91 = new Integer(91);
    @Contended Integer int_92 = new Integer(92);
    @Contended Integer int_93 = new Integer(93);
    @Contended Integer int_94 = new Integer(94);
    @Contended Integer int_95 = new Integer(95);
    @Contended Integer int_96 = new Integer(96);
    @Contended Integer int_97 = new Integer(97);
    @Contended Integer int_98 = new Integer(98);
    @Contended Integer int_99 = new Integer(99);
    @Contended Integer int_100 = new Integer(100);
    @Contended Integer int_101 = new Integer(101);
    @Contended Integer int_102 = new Integer(102);
    @Contended Integer int_103 = new Integer(103);
    @Contended Integer int_104 = new Integer(104);
    @Contended Integer int_105 = new Integer(105);
    @Contended Integer int_106 = new Integer(106);
    @Contended Integer int_107 = new Integer(107);
    @Contended Integer int_108 = new Integer(108);
    @Contended Integer int_109 = new Integer(109);
    @Contended Integer int_110 = new Integer(110);
    @Contended Integer int_111 = new Integer(111);
    @Contended Integer int_112 = new Integer(112);
    @Contended Integer int_113 = new Integer(113);
    @Contended Integer int_114 = new Integer(114);
    @Contended Integer int_115 = new Integer(115);
    @Contended Integer int_116 = new Integer(116);
    @Contended Integer int_117 = new Integer(117);
    @Contended Integer int_118 = new Integer(118);
    @Contended Integer int_119 = new Integer(119);
    @Contended Integer int_120 = new Integer(120);
    @Contended Integer int_121 = new Integer(121);
    @Contended Integer int_122 = new Integer(122);
    @Contended Integer int_123 = new Integer(123);
    @Contended Integer int_124 = new Integer(124);
    @Contended Integer int_125 = new Integer(125);
    @Contended Integer int_126 = new Integer(126);
    @Contended Integer int_127 = new Integer(127);
    @Contended Integer int_128 = new Integer(128);
    @Contended Integer int_129 = new Integer(129);
    @Contended Integer int_130 = new Integer(130);
    @Contended Integer int_131 = new Integer(131);
    @Contended Integer int_132 = new Integer(132);
    @Contended Integer int_133 = new Integer(133);
    @Contended Integer int_134 = new Integer(134);
    @Contended Integer int_135 = new Integer(135);
    @Contended Integer int_136 = new Integer(136);
    @Contended Integer int_137 = new Integer(137);
    @Contended Integer int_138 = new Integer(138);
    @Contended Integer int_139 = new Integer(139);
    @Contended Integer int_140 = new Integer(140);
    @Contended Integer int_141 = new Integer(141);
    @Contended Integer int_142 = new Integer(142);
    @Contended Integer int_143 = new Integer(143);
    @Contended Integer int_144 = new Integer(144);
    @Contended Integer int_145 = new Integer(145);
    @Contended Integer int_146 = new Integer(146);
    @Contended Integer int_147 = new Integer(147);
    @Contended Integer int_148 = new Integer(148);
    @Contended Integer int_149 = new Integer(149);
    @Contended Integer int_150 = new Integer(150);
    @Contended Integer int_151 = new Integer(151);
    @Contended Integer int_152 = new Integer(152);
    @Contended Integer int_153 = new Integer(153);
    @Contended Integer int_154 = new Integer(154);
    @Contended Integer int_155 = new Integer(155);
    @Contended Integer int_156 = new Integer(156);
    @Contended Integer int_157 = new Integer(157);
    @Contended Integer int_158 = new Integer(158);
    @Contended Integer int_159 = new Integer(159);
    @Contended Integer int_160 = new Integer(160);
    @Contended Integer int_161 = new Integer(161);
    @Contended Integer int_162 = new Integer(162);
    @Contended Integer int_163 = new Integer(163);
    @Contended Integer int_164 = new Integer(164);
    @Contended Integer int_165 = new Integer(165);
    @Contended Integer int_166 = new Integer(166);
    @Contended Integer int_167 = new Integer(167);
    @Contended Integer int_168 = new Integer(168);
    @Contended Integer int_169 = new Integer(169);
    @Contended Integer int_170 = new Integer(170);
    @Contended Integer int_171 = new Integer(171);
    @Contended Integer int_172 = new Integer(172);
    @Contended Integer int_173 = new Integer(173);
    @Contended Integer int_174 = new Integer(174);
    @Contended Integer int_175 = new Integer(175);
    @Contended Integer int_176 = new Integer(176);
    @Contended Integer int_177 = new Integer(177);
    @Contended Integer int_178 = new Integer(178);
    @Contended Integer int_179 = new Integer(179);
    @Contended Integer int_180 = new Integer(180);
    @Contended Integer int_181 = new Integer(181);
    @Contended Integer int_182 = new Integer(182);
    @Contended Integer int_183 = new Integer(183);
    @Contended Integer int_184 = new Integer(184);
    @Contended Integer int_185 = new Integer(185);
    @Contended Integer int_186 = new Integer(186);
    @Contended Integer int_187 = new Integer(187);
    @Contended Integer int_188 = new Integer(188);
    @Contended Integer int_189 = new Integer(189);
    @Contended Integer int_190 = new Integer(190);
    @Contended Integer int_191 = new Integer(191);
    @Contended Integer int_192 = new Integer(192);
    @Contended Integer int_193 = new Integer(193);
    @Contended Integer int_194 = new Integer(194);
    @Contended Integer int_195 = new Integer(195);
    @Contended Integer int_196 = new Integer(196);
    @Contended Integer int_197 = new Integer(197);
    @Contended Integer int_198 = new Integer(198);
    @Contended Integer int_199 = new Integer(199);
    @Contended Integer int_200 = new Integer(200);
    @Contended Integer int_201 = new Integer(201);
    @Contended Integer int_202 = new Integer(202);
    @Contended Integer int_203 = new Integer(203);
    @Contended Integer int_204 = new Integer(204);
    @Contended Integer int_205 = new Integer(205);
    @Contended Integer int_206 = new Integer(206);
    @Contended Integer int_207 = new Integer(207);
    @Contended Integer int_208 = new Integer(208);
    @Contended Integer int_209 = new Integer(209);
    @Contended Integer int_210 = new Integer(210);
    @Contended Integer int_211 = new Integer(211);
    @Contended Integer int_212 = new Integer(212);
    @Contended Integer int_213 = new Integer(213);
    @Contended Integer int_214 = new Integer(214);
    @Contended Integer int_215 = new Integer(215);
    @Contended Integer int_216 = new Integer(216);
    @Contended Integer int_217 = new Integer(217);
    @Contended Integer int_218 = new Integer(218);
    @Contended Integer int_219 = new Integer(219);
    @Contended Integer int_220 = new Integer(220);
    @Contended Integer int_221 = new Integer(221);
    @Contended Integer int_222 = new Integer(222);
    @Contended Integer int_223 = new Integer(223);
    @Contended Integer int_224 = new Integer(224);
    @Contended Integer int_225 = new Integer(225);
    @Contended Integer int_226 = new Integer(226);
    @Contended Integer int_227 = new Integer(227);
    @Contended Integer int_228 = new Integer(228);
    @Contended Integer int_229 = new Integer(229);
    @Contended Integer int_230 = new Integer(230);
    @Contended Integer int_231 = new Integer(231);
    @Contended Integer int_232 = new Integer(232);
    @Contended Integer int_233 = new Integer(233);
    @Contended Integer int_234 = new Integer(234);
    @Contended Integer int_235 = new Integer(235);
    @Contended Integer int_236 = new Integer(236);
    @Contended Integer int_237 = new Integer(237);
    @Contended Integer int_238 = new Integer(238);
    @Contended Integer int_239 = new Integer(239);
    @Contended Integer int_240 = new Integer(240);
    @Contended Integer int_241 = new Integer(241);
    @Contended Integer int_242 = new Integer(242);
    @Contended Integer int_243 = new Integer(243);
    @Contended Integer int_244 = new Integer(244);
    @Contended Integer int_245 = new Integer(245);
    @Contended Integer int_246 = new Integer(246);
    @Contended Integer int_247 = new Integer(247);
    @Contended Integer int_248 = new Integer(248);
    @Contended Integer int_249 = new Integer(249);
    @Contended Integer int_250 = new Integer(250);
    @Contended Integer int_251 = new Integer(251);
    @Contended Integer int_252 = new Integer(252);
    @Contended Integer int_253 = new Integer(253);
    @Contended Integer int_254 = new Integer(254);
    @Contended Integer int_255 = new Integer(255);
    @Contended Integer int_256 = new Integer(256);
    @Contended Integer int_257 = new Integer(257);
    @Contended Integer int_258 = new Integer(258);
    @Contended Integer int_259 = new Integer(259);
    @Contended Integer int_260 = new Integer(260);
    @Contended Integer int_261 = new Integer(261);
    @Contended Integer int_262 = new Integer(262);
    @Contended Integer int_263 = new Integer(263);
    @Contended Integer int_264 = new Integer(264);
    @Contended Integer int_265 = new Integer(265);
    @Contended Integer int_266 = new Integer(266);
    @Contended Integer int_267 = new Integer(267);
    @Contended Integer int_268 = new Integer(268);
    @Contended Integer int_269 = new Integer(269);
    @Contended Integer int_270 = new Integer(270);
    @Contended Integer int_271 = new Integer(271);
    @Contended Integer int_272 = new Integer(272);
    @Contended Integer int_273 = new Integer(273);
    @Contended Integer int_274 = new Integer(274);
    @Contended Integer int_275 = new Integer(275);
    @Contended Integer int_276 = new Integer(276);
    @Contended Integer int_277 = new Integer(277);
    @Contended Integer int_278 = new Integer(278);
    @Contended Integer int_279 = new Integer(279);
    @Contended Integer int_280 = new Integer(280);
    @Contended Integer int_281 = new Integer(281);
    @Contended Integer int_282 = new Integer(282);
    @Contended Integer int_283 = new Integer(283);
    @Contended Integer int_284 = new Integer(284);
    @Contended Integer int_285 = new Integer(285);
    @Contended Integer int_286 = new Integer(286);
    @Contended Integer int_287 = new Integer(287);
    @Contended Integer int_288 = new Integer(288);
    @Contended Integer int_289 = new Integer(289);
    @Contended Integer int_290 = new Integer(290);
    @Contended Integer int_291 = new Integer(291);
    @Contended Integer int_292 = new Integer(292);
    @Contended Integer int_293 = new Integer(293);
    @Contended Integer int_294 = new Integer(294);
    @Contended Integer int_295 = new Integer(295);
    @Contended Integer int_296 = new Integer(296);
    @Contended Integer int_297 = new Integer(297);
    @Contended Integer int_298 = new Integer(298);
    @Contended Integer int_299 = new Integer(299);
    @Contended Integer int_300 = new Integer(300);
}
