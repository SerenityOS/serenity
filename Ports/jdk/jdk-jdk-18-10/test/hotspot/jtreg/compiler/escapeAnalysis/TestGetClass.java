/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218201
 * @summary BCEscapeAnalyzer assigns wrong escape state to getClass return value.
 * @run main/othervm -XX:-TieredCompilation -Xcomp -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_getClass
 *                   -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,compiler.escapeAnalysis.TestGetClass::test
 *                   -XX:+PrintCompilation compiler.escapeAnalysis.TestGetClass
 * @run main/othervm -XX:-TieredCompilation -Xcomp -XX:+UnlockDiagnosticVMOptions -XX:ControlIntrinsic=-_getClass
 *                   -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,compiler.escapeAnalysis.TestGetClass::test
 *                   -XX:+PrintCompilation compiler.escapeAnalysis.TestGetClass
 */

package compiler.escapeAnalysis;

public class TestGetClass {
    static Object obj = new Object();

    public static boolean test() {
        if (obj.getClass() == Object.class) {
            synchronized (obj) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        if (!test()) {
            throw new RuntimeException("Test failed");
        }
    }
}
