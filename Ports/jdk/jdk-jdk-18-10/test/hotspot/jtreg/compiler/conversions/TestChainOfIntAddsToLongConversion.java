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

package compiler.conversions;

/*
 * @test
 * @bug 8253404
 * @requires vm.compiler2.enabled
 * @summary Tests that the optimization of a chain of integer additions followed
 *          by a long conversion does not lead to an explosion of live nodes.
 * @library /test/lib /
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *      -XX:CompileOnly=compiler.conversions.TestChainOfIntAddsToLongConversion::main
 *      -XX:MaxNodeLimit=1000 -XX:NodeLimitFudgeFactor=25
 *      compiler.conversions.TestChainOfIntAddsToLongConversion
 */

public class TestChainOfIntAddsToLongConversion {
    public static void main(String[] args) {
        long out = 0;
        for (int i = 0; i < 2; i++) {
            int foo = i;
            for (int j = 0; j < 17; j++) {
                // Int addition to be turned into a chain by loop unrolling.
                foo = foo + foo;
            }
            // Int to long conversion.
            out = foo;
        }
        System.out.println(out);
    }
}
