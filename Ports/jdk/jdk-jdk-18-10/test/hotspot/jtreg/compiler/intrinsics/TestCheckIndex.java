/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8255150
 * @summary Add utility methods to check long indexes and ranges
 * @requires vm.compiler2.enabled
 * @requires vm.compMode != "Xcomp"
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -ea -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-BackgroundCompilation TestCheckIndex
 *
 */

import java.util.Objects;
import sun.hotspot.WhiteBox;
import java.lang.reflect.Method;
import compiler.whitebox.CompilerWhiteBoxTest;

public class TestCheckIndex {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public static void main(String[] args) throws NoSuchMethodException {
        Objects.checkIndex(0, 10); // Load class
        Method m1 = TestCheckIndex.class.getDeclaredMethod("test1", int.class, int.class);
        Method m2 = TestCheckIndex.class.getDeclaredMethod("test2", long.class, long.class);
        Method m3 = TestCheckIndex.class.getDeclaredMethod("test3", int.class, int.class);
        Method m4 = TestCheckIndex.class.getDeclaredMethod("test4", long.class, long.class);
        assert m1 != null && m2 != null && m3 != null && m4 != null;
        WHITE_BOX.enqueueMethodForCompilation(m1, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(m1)) {
            throw new RuntimeException("should be compiled");
        }
        WHITE_BOX.enqueueMethodForCompilation(m2, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(m2)) {
            throw new RuntimeException("should be compiled");
        }
        WHITE_BOX.enqueueMethodForCompilation(m3, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(m3)) {
            throw new RuntimeException("should be compiled");
        }
        WHITE_BOX.enqueueMethodForCompilation(m4, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(m4)) {
            throw new RuntimeException("should be compiled");
        }

        if (test1(0, 10) != 0) {
            throw new RuntimeException("incorrect result");
        }
        if (!WHITE_BOX.isMethodCompiled(m1)) {
            throw new RuntimeException("should still be compiled");
        }
        if (test2(0, 10) != 0) {
            throw new RuntimeException("incorrect result");
        }
        if (!WHITE_BOX.isMethodCompiled(m2)) {
            throw new RuntimeException("should still be compiled");
        }

        try {
            test1(0, -10);
            throw new RuntimeException("exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
        }
        if (WHITE_BOX.isMethodCompiled(m1)) {
            throw new RuntimeException("should have deoptimized");
        }
        try {
            test2(0, -10);
            throw new RuntimeException("exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
        }
        if (WHITE_BOX.isMethodCompiled(m2)) {
            throw new RuntimeException("should have deoptimized");
        }

        try {
            test3(42, 10);
            throw new RuntimeException("exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
        }
        if (WHITE_BOX.isMethodCompiled(m3)) {
            throw new RuntimeException("should have deoptimized");
        }
        try {
            test4(42, 10);
            throw new RuntimeException("exception not thrown");
        } catch (IndexOutOfBoundsException ioobe) {
        }
        if (WHITE_BOX.isMethodCompiled(m4)) {
            throw new RuntimeException("should have deoptimized");
        }
    }

    static int test1(int index, int length) {
        return Objects.checkIndex(index, length);
    }

    static long test2(long index, long length) {
        return Objects.checkIndex(index, length);
    }

    static int test3(int index, int length) {
        return Objects.checkIndex(index, length);
    }

    static long test4(long index, long length) {
        return Objects.checkIndex(index, length);
    }
}
