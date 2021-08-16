/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.base/jdk.internal.misc:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                   -Xbootclasspath/a:. -XX:+WhiteBoxAPI
 *                   -Xbatch -XX:-TieredCompilation -XX:+AlwaysIncrementalInline
 *                   -XX:CompileCommand=compileonly,compiler.ciReplay.TestDumpReplay::*
 *                   compiler.ciReplay.TestDumpReplay
 */

package compiler.ciReplay;

import sun.hotspot.WhiteBox;

public class TestDumpReplay {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private static final String emptyString;

    static {
        emptyString = "";
    }

    public static void m1() {
        m2();
    }

    public static void m2() {
        m3();
    }

    public static void m3() {

    }

    public static void main(String[] args) {
        // Add compiler control directive to force generation of replay file
        String directive = "[{ match: \"*.*\", DumpReplay: true }]";
        if (WHITE_BOX.addCompilerDirective(directive) != 1) {
            throw new RuntimeException("Failed to add compiler directive");
        }

        // Trigger compilation of m1
        for (int i = 0; i < 10_000; ++i) {
            m1();
        }
    }
}
