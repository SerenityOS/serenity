/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8129786
 * @summary Verify that JVM correctly processes very long unrecognized VM option
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @run driver TestLongUnrecognizedVMOption
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestLongUnrecognizedVMOption {

    /* Create option with very long length(greater than 500 characters) */
    private static final String VERY_LONG_OPTION = String.format("%500s=10", "unrecognizedoption").replace(" ", "a");

    public static void main(String[] args) throws Exception {
        OutputAnalyzer output;

        output = new OutputAnalyzer(ProcessTools.createJavaProcessBuilder("-XX:" + VERY_LONG_OPTION, "-version").start());
        output.shouldHaveExitValue(1);
        output.shouldContain(String.format("Unrecognized VM option '%s'", VERY_LONG_OPTION));
    }
}
