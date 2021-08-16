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

/**
 * @test
 * @summary Test implicit String concatenations with lots of arguments (two-slot version)
 *
 * @compile ImplicitStringConcatManyLongs.java
 * @run main/othervm -Xverify:all ImplicitStringConcatManyLongs
 *
 * @compile -XDstringConcat=inline ImplicitStringConcatManyLongs.java
 * @run main/othervm -Xverify:all ImplicitStringConcatManyLongs
 *
 * @compile -XDstringConcat=indy ImplicitStringConcatManyLongs.java
 * @run main/othervm -Xverify:all ImplicitStringConcatManyLongs
 *
 * @compile -XDstringConcat=indyWithConstants ImplicitStringConcatManyLongs.java
 * @run main/othervm -Xverify:all ImplicitStringConcatManyLongs
*/

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ImplicitStringConcatManyLongs {

    static long s000, s001, s002, s003, s004, s005, s006, s007, s008, s009;
    static long s010, s011, s012, s013, s014, s015, s016, s017, s018, s019;
    static long s020, s021, s022, s023, s024, s025, s026, s027, s028, s029;
    static long s030, s031, s032, s033, s034, s035, s036, s037, s038, s039;
    static long s040, s041, s042, s043, s044, s045, s046, s047, s048, s049;
    static long s050, s051, s052, s053, s054, s055, s056, s057, s058, s059;
    static long s060, s061, s062, s063, s064, s065, s066, s067, s068, s069;
    static long s070, s071, s072, s073, s074, s075, s076, s077, s078, s079;
    static long s080, s081, s082, s083, s084, s085, s086, s087, s088, s089;
    static long s090, s091, s092, s093, s094, s095, s096, s097, s098, s099;

    static long s100, s101, s102, s103, s104, s105, s106, s107, s108, s109;
    static long s110, s111, s112, s113, s114, s115, s116, s117, s118, s119;
    static long s120, s121, s122, s123, s124, s125, s126, s127, s128, s129;
    static long s130, s131, s132, s133, s134, s135, s136, s137, s138, s139;
    static long s140, s141, s142, s143, s144, s145, s146, s147, s148, s149;
    static long s150, s151, s152, s153, s154, s155, s156, s157, s158, s159;
    static long s160, s161, s162, s163, s164, s165, s166, s167, s168, s169;
    static long s170, s171, s172, s173, s174, s175, s176, s177, s178, s179;
    static long s180, s181, s182, s183, s184, s185, s186, s187, s188, s189;
    static long s190, s191, s192, s193, s194, s195, s196, s197, s198, s199;

    static long s200, s201, s202, s203, s204, s205, s206, s207, s208, s209;
    static long s210, s211, s212, s213, s214, s215, s216, s217, s218, s219;
    static long s220, s221, s222, s223, s224, s225, s226, s227, s228, s229;
    static long s230, s231, s232, s233, s234, s235, s236, s237, s238, s239;
    static long s240, s241, s242, s243, s244, s245, s246, s247, s248, s249;
    static long s250, s251, s252, s253, s254, s255, s256, s257, s258, s259;
    static long s260, s261, s262, s263, s264, s265, s266, s267, s268, s269;
    static long s270, s271, s272, s273, s274, s275, s276, s277, s278, s279;
    static long s280, s281, s282, s283, s284, s285, s286, s287, s288, s289;
    static long s290, s291, s292, s293, s294, s295, s296, s297, s298, s299;

    static {
        for (Field f : ImplicitStringConcatManyLongs.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                String name = f.getName();
                try {
                    f.set(null, Long.valueOf(name.substring(1)));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String res = "" +
            s000 + s001 + s002 + s003 + s004 + s005 + s006 + s007 + s008 + s009 +
            s010 + s011 + s012 + s013 + s014 + s015 + s016 + s017 + s018 + s019 +
            s020 + s021 + s022 + s023 + s024 + s025 + s026 + s027 + s028 + s029 +
            s030 + s031 + s032 + s033 + s034 + s035 + s036 + s037 + s038 + s039 +
            s040 + s041 + s042 + s043 + s044 + s045 + s046 + s047 + s048 + s049 +
            s050 + s051 + s052 + s053 + s054 + s055 + s056 + s057 + s058 + s059 +
            s060 + s061 + s062 + s063 + s064 + s065 + s066 + s067 + s068 + s069 +
            s070 + s071 + s072 + s073 + s074 + s075 + s076 + s077 + s078 + s079 +
            s080 + s081 + s082 + s083 + s084 + s085 + s086 + s087 + s088 + s089 +
            s090 + s091 + s092 + s093 + s094 + s095 + s096 + s097 + s098 + s099 +

            s100 + s101 + s102 + s103 + s104 + s105 + s106 + s107 + s108 + s109 +
            s110 + s111 + s112 + s113 + s114 + s115 + s116 + s117 + s118 + s119 +
            s120 + s121 + s122 + s123 + s124 + s125 + s126 + s127 + s128 + s129 +
            s130 + s131 + s132 + s133 + s134 + s135 + s136 + s137 + s138 + s139 +
            s140 + s141 + s142 + s143 + s144 + s145 + s146 + s147 + s148 + s149 +
            s150 + s151 + s152 + s153 + s154 + s155 + s156 + s157 + s158 + s159 +
            s160 + s161 + s162 + s163 + s164 + s165 + s166 + s167 + s168 + s169 +
            s170 + s171 + s172 + s173 + s174 + s175 + s176 + s177 + s178 + s179 +
            s180 + s181 + s182 + s183 + s184 + s185 + s186 + s187 + s188 + s189 +
            s190 + s191 + s192 + s193 + s194 + s195 + s196 + s197 + s198 + s199 +

            s200 + s201 + s202 + s203 + s204 + s205 + s206 + s207 + s208 + s209 +
            s210 + s211 + s212 + s213 + s214 + s215 + s216 + s217 + s218 + s219 +
            s220 + s221 + s222 + s223 + s224 + s225 + s226 + s227 + s228 + s229 +
            s230 + s231 + s232 + s233 + s234 + s235 + s236 + s237 + s238 + s239 +
            s240 + s241 + s242 + s243 + s244 + s245 + s246 + s247 + s248 + s249 +
            s250 + s251 + s252 + s253 + s254 + s255 + s256 + s257 + s258 + s259 +
            s260 + s261 + s262 + s263 + s264 + s265 + s266 + s267 + s268 + s269 +
            s270 + s271 + s272 + s273 + s274 + s275 + s276 + s277 + s278 + s279 +
            s280 + s281 + s282 + s283 + s284 + s285 + s286 + s287 + s288 + s289 +
            s290 + s291 + s292 + s293 + s294 + s295 + s296 + s297 + s298 + s299;

       StringBuilder sb = new StringBuilder();
       for (int c = 0; c < 300; c++) {
            sb.append(c);
       }
       test(sb.toString(), res);
    }

    public static void test(String expected, String actual) {
       // Fingers crossed: String concat should work.
       if (!expected.equals(actual)) {
          throw new IllegalStateException("Expected = " + expected + ", actual = " + actual);
       }
    }
}

