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

/**
 * @test
 * @bug 8246203
 * @requires vm.debug == true & vm.flavor == "server"
 * @summary Test which causes a stack overflow segmentation fault with -XX:+VerifyIterativeGVN due to a too deep recursion in Node::verify_recur().
 *
 * @run main/othervm/timeout=600 -Xcomp -XX:+VerifyIterativeGVN -XX:CompileCommand=compileonly,compiler.loopopts.TestDeepGraphVerifyIterativeGVN::*
 *                               compiler.loopopts.TestDeepGraphVerifyIterativeGVN
 */

package compiler.loopopts;

public class TestDeepGraphVerifyIterativeGVN
{
    static volatile int[] iArr;
    static volatile int x;

    public static void main(String[] arr) {
        /*
         * Just enough statements to create a deep enough graph (i.e. many nodes in one chain). The current recursive verification in Node::verify_recur() will follow this chain
         * and call itself again for each newly discovered input node. The current implementation can only handle up to around 10000 recursive calls and will then crash with a
         * stack overflow segementation fault. The iterative fix needs much less memory and does not result in a segementation fault anymore.
         */
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
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
        iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 }; iArr = new int[] { x % 2 };
    }
}
