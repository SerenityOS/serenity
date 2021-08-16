/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests WB::isMethodCompilable(m) in combination with compiler directives that prevent a compilation of m.
 * @bug 8263582
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:CompileCommand=compileonly,compiler.whitebox.TestMethodCompilableCompilerDirectives::doesNotExist
 *      compiler.whitebox.TestMethodCompilableCompilerDirectives
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:CompileCommand=exclude,compiler.whitebox.TestMethodCompilableCompilerDirectives::*
 *      compiler.whitebox.TestMethodCompilableCompilerDirectives
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;
import java.lang.reflect.Method;

public class TestMethodCompilableCompilerDirectives {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    // Method too simple for C2 and only C1 compiled.
    public static int c1Compiled() {
        return 3;
    }


    // Method first C1 and then C2 compiled.
    public static int c2Compiled() {
        for (int i = 0; i < 100; i++);
        return 3;
    }

    // WB::isMethodCompilable(m) uses Method::is_not_compilable() to decide if m is compilable. Method::is_not_compilable(), however,
    // returns false regardless of any compiler directives if m was not yet tried to be compiled. The compiler directive ExcludeOption
    // to prevent a compilation is evaluated lazily and is only applied when a compilation for m is attempted.
    // Another problem is that Method::is_not_compilable() only returns true for CompLevel_any if C1 AND C2 cannot compile it.
    // This means that a compilation of m must have been attempted for C1 and C2 before WB::isMethodCompilable(m, CompLevel_any) will
    // ever return false. This disregards any compiler directives (e.g. compileonly, exclude) that prohibit a compilation of m completely.
    // WB::isMethodCompilable() should be aware of the ExcludeOption compiler directives at any point in time.
    public static void main(String[] args) throws NoSuchMethodException {
        Method c1CompiledMethod = TestMethodCompilableCompilerDirectives.class.getDeclaredMethod("c1Compiled");
        Method c2CompiledMethod = TestMethodCompilableCompilerDirectives.class.getDeclaredMethod("c2Compiled");

        boolean compilable = WhiteBox.getWhiteBox().isMethodCompilable(c1CompiledMethod);
        Asserts.assertFalse(compilable);
        for (int i = 0; i < 3000; i++) {
            c1Compiled();
        }
        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c1CompiledMethod);
        Asserts.assertFalse(compilable);


        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c2CompiledMethod);
        Asserts.assertFalse(compilable);
        for (int i = 0; i < 3000; i++) {
            c2Compiled();
        }
        compilable = WhiteBox.getWhiteBox().isMethodCompilable(c2CompiledMethod);
        Asserts.assertFalse(compilable);
    }
}
