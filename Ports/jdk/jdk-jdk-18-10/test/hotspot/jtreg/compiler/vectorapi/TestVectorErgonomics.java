/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package compiler.vectorapi;

/*
 * @test TestVectorErgonomics
 * @bug 8262508
 * @requires vm.compiler2.enabled
 * @summary Check ergonomics for Vector API
 * @library /test/lib
 * @run driver compiler.vectorapi.TestVectorErgonomics
 */

import jdk.test.lib.process.ProcessTools;

public class TestVectorErgonomics {

    public static void main(String[] args) throws Throwable {
        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:+EnableVectorReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorReboxing=true");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:+EnableVectorAggressiveReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorAggressiveReboxing=true");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:-EnableVectorReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorReboxing=false")
                    .shouldContain("EnableVectorAggressiveReboxing=false");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:-EnableVectorAggressiveReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorAggressiveReboxing=false");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:-EnableVectorSupport", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorSupport=false")
                    .shouldContain("EnableVectorReboxing=false")
                    .shouldContain("EnableVectorAggressiveReboxing=false");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:-EnableVectorSupport", "-XX:+EnableVectorReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorSupport=false")
                    .shouldContain("EnableVectorReboxing=false")
                    .shouldContain("EnableVectorAggressiveReboxing=false");

        ProcessTools.executeTestJvm("--add-modules=jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions",
                                    "-XX:-EnableVectorSupport", "-XX:+EnableVectorAggressiveReboxing", "-Xlog:compilation", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("EnableVectorSupport=false")
                    .shouldContain("EnableVectorReboxing=false")
                    .shouldContain("EnableVectorAggressiveReboxing=false");
    }
}
