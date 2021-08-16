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

package gc.arguments;

/*
 * @test TestSoftMaxHeapSizeFlag
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestSoftMaxHeapSizeFlag
 */

import jdk.test.lib.process.ProcessTools;

public class TestSoftMaxHeapSizeFlag {
    // Note: Xms and Xmx values get aligned up by HeapAlignment which is 32M with 64k pages.
    private static final long Xms              = 224 * 1024 * 1024;
    private static final long Xmx              = 320 * 1024 * 1024;
    private static final long greaterThanXmx   = Xmx + 1;
    private static final long betweenXmsAndXmx = (Xms + Xmx) / 2;

    public static void main(String args[]) throws Exception {
        // Test default value
        ProcessTools.executeTestJvm("-Xms" + Xms, "-Xmx" + Xmx,
                                    "-XX:+PrintFlagsFinal", "-version")
                    .shouldMatch("SoftMaxHeapSize[ ]+=[ ]+" + Xmx)
                    .shouldHaveExitValue(0);

        // Test setting small value
        ProcessTools.executeTestJvm("-Xms" + Xms, "-Xmx" + Xmx,
                                    "-XX:SoftMaxHeapSize=" + Xms,
                                    "-XX:+PrintFlagsFinal", "-version")
                    .shouldMatch("SoftMaxHeapSize[ ]+=[ ]+" + Xms)
                    .shouldHaveExitValue(0);

        // Test setting middle value
        ProcessTools.executeTestJvm("-Xms" + Xms, "-Xmx" + Xmx,
                                    "-XX:SoftMaxHeapSize=" + betweenXmsAndXmx,
                                    "-XX:+PrintFlagsFinal", "-version")
                    .shouldMatch("SoftMaxHeapSize[ ]+=[ ]+" + betweenXmsAndXmx)
                    .shouldHaveExitValue(0);

        // Test setting largest value
        ProcessTools.executeTestJvm("-Xms" + Xms, "-Xmx" + Xmx,
                                    "-XX:SoftMaxHeapSize=" + Xmx,
                                    "-XX:+PrintFlagsFinal", "-version")
                    .shouldMatch("SoftMaxHeapSize[ ]+=[ ]+" + Xmx)
                    .shouldHaveExitValue(0);

        // Test setting a too large value
        ProcessTools.executeTestJvm("-Xms" + Xms, "-Xmx" + Xmx,
                                    "-XX:SoftMaxHeapSize=" + greaterThanXmx,
                                    "-XX:+PrintFlagsFinal", "-version")
                    .shouldContain("SoftMaxHeapSize must be less than or equal to the maximum heap size")
                    .shouldHaveExitValue(1);
    }
}
