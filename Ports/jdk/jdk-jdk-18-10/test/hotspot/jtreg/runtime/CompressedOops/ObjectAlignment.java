/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8022865
 * @summary Tests for the -XX:ObjectAlignmentInBytes command line option
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver ObjectAlignment
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ObjectAlignment {

    public static void main(String[] args) throws Exception {

        if (Platform.is64bit()) {
            // Minimum alignment should be 8
            testObjectAlignment(4)
                .shouldContain("outside the allowed range")
                .shouldHaveExitValue(1);

            // Alignment has to be a power of 2
            testObjectAlignment(9)
                .shouldContain("must be power of 2")
                .shouldHaveExitValue(1);

            testObjectAlignment(-1)
                .shouldContain("outside the allowed range")
                .shouldHaveExitValue(1);

            // Maximum alignment allowed is 256
            testObjectAlignment(512)
                .shouldContain("outside the allowed range")
                .shouldHaveExitValue(1);

            // Valid alignments should work
            testObjectAlignment(8).shouldHaveExitValue(0);
            testObjectAlignment(16).shouldHaveExitValue(0);
            testObjectAlignment(256).shouldHaveExitValue(0);

        } else {
            // For a 32bit JVM the option isn't there, make sure it's not silently ignored
            testObjectAlignment(8)
                .shouldContain("Unrecognized VM option 'ObjectAlignmentInBytes=8'")
                .shouldHaveExitValue(1);
        }
    }

    private static OutputAnalyzer testObjectAlignment(int alignment) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:ObjectAlignmentInBytes=" + alignment,
                                                                  "-version");
        return new OutputAnalyzer(pb.start());
    }
}
