/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244724
 * @summary Test which exceeds the live node limit in Compile::Code_Gen(). The previouslz failing assert is now only checked during parsing and during Compile::Optimize().
 *
 * @compile -XDstringConcat=inline TestLiveNodeLimit.java
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.c2.TestLiveNodeLimit::test* compiler.c2.TestLiveNodeLimit
 */
package compiler.c2;

import java.util.ArrayList;

public class TestLiveNodeLimit {

    public static ArrayList<String> arrList = new ArrayList<String>();

    public static void main(String[] args) {
        for (int i = 0; i < 10000; i++) {
            testNodeLimitInBuildCfg();
            testNodeLimitInGlobalCodeMotion();
        }
    }

    // Hits live node limit in PhaseCFG::build_cfg(). There are around 70000 nodes right before build_cfg()
    // which creates another 15000 nodes which exceeds the default max node limit of 80000.
    public static int testNodeLimitInBuildCfg() {
        // Just some string operations that create a lot of nodes when compiled with javac with -XDstringConcat=inline
        // which generate a lot of StringBuilder objects and calls
        String someString = "as@df#as@df" + "fdsa";
        String[] split = someString.split("#");
        String[] tmp1 = split[0].split("@");
        String[] tmp2 = split[0].split("@");
        String concat1 = tmp1[0] + tmp2[0] + split[0];
        String concat2 = tmp1[0] + tmp2[0] + split[0];
        String concat3 = tmp1[0] + tmp2[0];
        String concat4 = tmp1[0] + tmp2[0];
        String string1 = "string1";
        String[] stringArr1 = arrList.toArray(new String[4]);
        String[] stringArr2 = new String[3];
        String[] stringArr3 = new String[3];
        if (stringArr1.length > 1) {
            stringArr2 = new String[3 * stringArr1.length];
            stringArr3 = new String[3 * stringArr1.length];
            for (int i = 0; i < stringArr1.length; i++) {
                stringArr2[3 * i    ] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr2[3 * i + 1] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr2[3 * i + 2] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr3[3 * i    ] = string1 + concat2 + concat3 + concat4 + stringArr1[i];
                stringArr3[3 * i + 1] = string1 + concat2 + concat3 + concat4 + stringArr1[i];
                stringArr3[3 * i + 2] = string1 + concat2 + concat3 + concat4 + stringArr1[i];
            }
        }
        return stringArr1.length + stringArr2.length + stringArr3.length;
    }

    // Hits live node limit in PhaseCFG::global_code_motion(). There are around 79000 nodes right before global_code_motion()
    // which creates another 2000 nodes which exceeds the default max node limit of 80000.
    public static int testNodeLimitInGlobalCodeMotion() {
        // Just some string operations that create a lot of nodes when compiled with javac with -XDstringConcat=inline
        // which generate a lot of StringBuilder objects and calls
        String someString = "as@df#as@df" + "fdsa";
        String[] split = someString.split("#");
        String[] tmp1 = split[0].split("@");
        String[] tmp2 = split[0].split("@");
        String concat1 = tmp1[0] + tmp2[0] + split[0];
        String concat2 = tmp1[0] + tmp2[0] + split[0];
        String concat3 = tmp1[0] + tmp2[0] + split[0];
        String concat4 = tmp1[0] + tmp2[0] + split[0];
        String string1 = "string1";
        String[] stringArr1 = arrList.toArray(new String[4]);
        String[] stringArr2 = new String[3];
        String[] stringArr3 = new String[3];
        if (stringArr1.length > 1) {
            stringArr2 = new String[3 * stringArr1.length];
            stringArr3 = new String[3 * stringArr1.length];
            for (int i = 0; i < stringArr1.length; i++) {
                stringArr2[3 * i    ] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr2[3 * i + 1] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr2[3 * i + 2] = string1 + concat1 + concat3 + stringArr1[i];
                stringArr3[3 * i    ] = string1 + concat2 + concat4 + stringArr1[i];
                stringArr3[3 * i + 1] = string1 + concat2 + concat4 + stringArr1[i];
                stringArr3[3 * i + 2] = string1 + concat2 + stringArr1[i];
            }
        }
        return stringArr1.length + stringArr2.length + stringArr3.length;
    }
}

