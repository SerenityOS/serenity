/*
 * Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8257228
 * @library /test/lib
 * @requires vm.bits == 64
 * @build gc.g1.TestBuffersToCardsOverflow jdk.test.lib.process.*
 * @run main gc.g1.TestBuffersToCardsOverflow
 */

package gc.g1;

import jdk.test.lib.process.ProcessTools;

public class TestBuffersToCardsOverflow {
    public static void main(String... args) throws Exception {
        ProcessTools.executeTestJava("-XX:G1ConcRefinementThresholdStep=16G",
                                     "-XX:G1UpdateBufferSize=1G")
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldNotContain("SIGFPE")
                .stdoutShouldNotContain("hs_err");
    }
}
