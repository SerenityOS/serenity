/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6879902
 * @summary CTW failure jdk6_18/hotspot/src/cpu/sparc/vm/assembler_sparc.hpp:845
 *
 * @run main compiler.codegen.Test6879902
 */

package compiler.codegen;

import java.util.Arrays;

public class Test6879902 {
    public static void main(String[] args) {
        Object[] oa = new Object[250];
        for (int i = 0; i < 250; i++) {
            oa[i] = Integer.valueOf(i);
        }
        Object[] oa2 = createArray(oa[0], oa[1], oa[2], oa[3], oa[4], oa[5], oa[6], oa[7], oa[8], oa[9], oa[10], oa[11], oa[12], oa[13], oa[14], oa[15], oa[16], oa[17], oa[18], oa[19], oa[20], oa[21], oa[22], oa[23], oa[24], oa[25], oa[26], oa[27], oa[28], oa[29], oa[30], oa[31], oa[32], oa[33], oa[34], oa[35], oa[36], oa[37], oa[38], oa[39], oa[40], oa[41], oa[42], oa[43], oa[44], oa[45], oa[46], oa[47], oa[48], oa[49], oa[50], oa[51], oa[52], oa[53], oa[54], oa[55], oa[56], oa[57], oa[58], oa[59], oa[60], oa[61], oa[62], oa[63], oa[64], oa[65], oa[66], oa[67], oa[68], oa[69], oa[70], oa[71], oa[72], oa[73], oa[74], oa[75], oa[76], oa[77], oa[78], oa[79], oa[80], oa[81], oa[82], oa[83], oa[84], oa[85], oa[86], oa[87], oa[88], oa[89], oa[90], oa[91], oa[92], oa[93], oa[94], oa[95], oa[96], oa[97], oa[98], oa[99], oa[100], oa[101], oa[102], oa[103], oa[104], oa[105], oa[106], oa[107], oa[108], oa[109], oa[110], oa[111], oa[112], oa[113], oa[114], oa[115], oa[116], oa[117], oa[118], oa[119], oa[120], oa[121], oa[122], oa[123], oa[124], oa[125], oa[126], oa[127], oa[128], oa[129], oa[130], oa[131], oa[132], oa[133], oa[134], oa[135], oa[136], oa[137], oa[138], oa[139], oa[140], oa[141], oa[142], oa[143], oa[144], oa[145], oa[146], oa[147], oa[148], oa[149], oa[150], oa[151], oa[152], oa[153], oa[154], oa[155], oa[156], oa[157], oa[158], oa[159], oa[160], oa[161], oa[162], oa[163], oa[164], oa[165], oa[166], oa[167], oa[168], oa[169], oa[170], oa[171], oa[172], oa[173], oa[174], oa[175], oa[176], oa[177], oa[178], oa[179], oa[180], oa[181], oa[182], oa[183], oa[184], oa[185], oa[186], oa[187], oa[188], oa[189], oa[190], oa[191], oa[192], oa[193], oa[194], oa[195], oa[196], oa[197], oa[198], oa[199], oa[200], oa[201], oa[202], oa[203], oa[204], oa[205], oa[206], oa[207], oa[208], oa[209], oa[210], oa[211], oa[212], oa[213], oa[214], oa[215], oa[216], oa[217], oa[218], oa[219], oa[220], oa[221], oa[222], oa[223], oa[224], oa[225], oa[226], oa[227], oa[228], oa[229], oa[230], oa[231], oa[232], oa[233], oa[234], oa[235], oa[236], oa[237], oa[238], oa[239], oa[240], oa[241], oa[242], oa[243], oa[244], oa[245], oa[246], oa[247], oa[248], oa[249]);
        if (!Arrays.equals(oa, oa2))
            throw new InternalError("arrays not equal");
    }

    public static Object[] createArray(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14, Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20, Object arg21, Object arg22, Object arg23, Object arg24, Object arg25, Object arg26, Object arg27, Object arg28, Object arg29, Object arg30, Object arg31, Object arg32, Object arg33, Object arg34, Object arg35, Object arg36, Object arg37, Object arg38, Object arg39, Object arg40, Object arg41, Object arg42, Object arg43, Object arg44, Object arg45, Object arg46, Object arg47, Object arg48, Object arg49, Object arg50, Object arg51, Object arg52, Object arg53, Object arg54, Object arg55, Object arg56, Object arg57, Object arg58, Object arg59, Object arg60, Object arg61, Object arg62, Object arg63, Object arg64, Object arg65, Object arg66, Object arg67, Object arg68, Object arg69, Object arg70, Object arg71, Object arg72, Object arg73, Object arg74, Object arg75, Object arg76, Object arg77, Object arg78, Object arg79, Object arg80, Object arg81, Object arg82, Object arg83, Object arg84, Object arg85, Object arg86, Object arg87, Object arg88, Object arg89, Object arg90, Object arg91, Object arg92, Object arg93, Object arg94, Object arg95, Object arg96, Object arg97, Object arg98, Object arg99, Object arg100, Object arg101, Object arg102, Object arg103, Object arg104, Object arg105, Object arg106, Object arg107, Object arg108, Object arg109, Object arg110, Object arg111, Object arg112, Object arg113, Object arg114, Object arg115, Object arg116, Object arg117, Object arg118, Object arg119, Object arg120, Object arg121, Object arg122, Object arg123, Object arg124, Object arg125, Object arg126, Object arg127, Object arg128, Object arg129, Object arg130, Object arg131, Object arg132, Object arg133, Object arg134, Object arg135, Object arg136, Object arg137, Object arg138, Object arg139, Object arg140, Object arg141, Object arg142, Object arg143, Object arg144, Object arg145, Object arg146, Object arg147, Object arg148, Object arg149, Object arg150, Object arg151, Object arg152, Object arg153, Object arg154, Object arg155, Object arg156, Object arg157, Object arg158, Object arg159, Object arg160, Object arg161, Object arg162, Object arg163, Object arg164, Object arg165, Object arg166, Object arg167, Object arg168, Object arg169, Object arg170, Object arg171, Object arg172, Object arg173, Object arg174, Object arg175, Object arg176, Object arg177, Object arg178, Object arg179, Object arg180, Object arg181, Object arg182, Object arg183, Object arg184, Object arg185, Object arg186, Object arg187, Object arg188, Object arg189, Object arg190, Object arg191, Object arg192, Object arg193, Object arg194, Object arg195, Object arg196, Object arg197, Object arg198, Object arg199, Object arg200, Object arg201, Object arg202, Object arg203, Object arg204, Object arg205, Object arg206, Object arg207, Object arg208, Object arg209, Object arg210, Object arg211, Object arg212, Object arg213, Object arg214, Object arg215, Object arg216, Object arg217, Object arg218, Object arg219, Object arg220, Object arg221, Object arg222, Object arg223, Object arg224, Object arg225, Object arg226, Object arg227, Object arg228, Object arg229, Object arg230, Object arg231, Object arg232, Object arg233, Object arg234, Object arg235, Object arg236, Object arg237, Object arg238, Object arg239, Object arg240, Object arg241, Object arg242, Object arg243, Object arg244, Object arg245, Object arg246, Object arg247, Object arg248, Object arg249) {
        return new Object[]{
            arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30, arg31, arg32, arg33, arg34, arg35, arg36, arg37, arg38, arg39, arg40, arg41, arg42, arg43, arg44, arg45, arg46, arg47, arg48, arg49, arg50, arg51, arg52, arg53, arg54, arg55, arg56, arg57, arg58, arg59, arg60, arg61, arg62, arg63, arg64, arg65, arg66, arg67, arg68, arg69, arg70, arg71, arg72, arg73, arg74, arg75, arg76, arg77, arg78, arg79, arg80, arg81, arg82, arg83, arg84, arg85, arg86, arg87, arg88, arg89, arg90, arg91, arg92, arg93, arg94, arg95, arg96, arg97, arg98, arg99, arg100, arg101, arg102, arg103, arg104, arg105, arg106, arg107, arg108, arg109, arg110, arg111, arg112, arg113, arg114, arg115, arg116, arg117, arg118, arg119, arg120, arg121, arg122, arg123, arg124, arg125, arg126, arg127, arg128, arg129, arg130, arg131, arg132, arg133, arg134, arg135, arg136, arg137, arg138, arg139, arg140, arg141, arg142, arg143, arg144, arg145, arg146, arg147, arg148, arg149, arg150, arg151, arg152, arg153, arg154, arg155, arg156, arg157, arg158, arg159, arg160, arg161, arg162, arg163, arg164, arg165, arg166, arg167, arg168, arg169, arg170, arg171, arg172, arg173, arg174, arg175, arg176, arg177, arg178, arg179, arg180, arg181, arg182, arg183, arg184, arg185, arg186, arg187, arg188, arg189, arg190, arg191, arg192, arg193, arg194, arg195, arg196, arg197, arg198, arg199, arg200, arg201, arg202, arg203, arg204, arg205, arg206, arg207, arg208, arg209, arg210, arg211, arg212, arg213, arg214, arg215, arg216, arg217, arg218, arg219, arg220, arg221, arg222, arg223, arg224, arg225, arg226, arg227, arg228, arg229, arg230, arg231, arg232, arg233, arg234, arg235, arg236, arg237, arg238, arg239, arg240, arg241, arg242, arg243, arg244, arg245, arg246, arg247, arg248, arg249};
    }
}
