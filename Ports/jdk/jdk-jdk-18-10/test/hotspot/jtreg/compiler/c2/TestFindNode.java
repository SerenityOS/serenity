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
 * @bug 8247743
 * @requires vm.debug == true & vm.flavor == "server"
 * @summary Test which uses some special flags in order to test Node::find() in debug builds which could result in an endless loop or a stack overflow crash.
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=option,*::*,bool,Vectorize,true -XX:+PrintOpto -XX:+TraceLoopOpts compiler.c2.TestFindNode
 */
package compiler.c2;

public class TestFindNode {
    static volatile int[] iArr;
    static volatile int x;
    static int[] iArr2 = new int[100];
    static int[] iArr3 = new int[100];

    public static void main(String[] arr) {
        for (int i = 0; i < 2000; i++) {
            test();
        }
    }

    public static void test() {
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };

        for (int i = 0; i < 50; i++) {
            iArr2[i] = iArr3[i];
        }
    }
}
