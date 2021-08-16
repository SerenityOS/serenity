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
 * @bug 8040237
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.instrument
 * @requires vm.jvmti
 * @build compiler.profiling.spectrapredefineclass_classloaders.Agent
 *        compiler.profiling.spectrapredefineclass_classloaders.Test
 *        compiler.profiling.spectrapredefineclass_classloaders.A
 *        compiler.profiling.spectrapredefineclass_classloaders.B
 * @run driver jdk.test.lib.helpers.ClassFileInstaller compiler.profiling.spectrapredefineclass_classloaders.Agent
 * @run driver compiler.profiling.spectrapredefineclass_classloaders.Launcher
 * @run main/othervm -XX:CompilationMode=high-only -XX:-BackgroundCompilation -XX:CompileThreshold=10000
 *                   -XX:-UseOnStackReplacement -XX:TypeProfileLevel=222
 *                   -XX:ReservedCodeCacheSize=3M -Djdk.attach.allowAttachSelf
 *                   compiler.profiling.spectrapredefineclass_classloaders.Agent
 */

package compiler.profiling.spectrapredefineclass_classloaders;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Launcher {
    private static final String MANIFEST = "MANIFEST.MF";
    public static void main(String[] args) throws Exception  {
        try (PrintWriter pw = new PrintWriter(MANIFEST)) {
            pw.println("Agent-Class: " + Agent.class.getName());
            pw.println("Can-Retransform-Classes: true");
        }

        JDKToolLauncher jar = JDKToolLauncher.create("jar")
                .addToolArg("cmf")
                .addToolArg(MANIFEST)
                .addToolArg(Agent.AGENT_JAR)
                .addToolArg(Agent.class.getName().replace('.', File.separatorChar) + ".class");

        ProcessBuilder pb = new ProcessBuilder(jar.getCommand());
        try {
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        } catch (IOException ex) {
            throw new Error("TESTBUG: jar failed.", ex);
        }
    }
}
