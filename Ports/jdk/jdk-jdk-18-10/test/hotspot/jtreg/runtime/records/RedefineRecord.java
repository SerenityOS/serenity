/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @library /test/lib
 * @summary Test that a class that is a record can be redefined.
 * @modules java.base/jdk.internal.misc
 * @modules java.instrument
 * @requires vm.jvmti
 * @requires vm.flagless
 * @run driver RedefineRecord buildagent
 * @run driver/timeout=6000 RedefineRecord runtest
 */

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.RuntimeException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.lang.instrument.IllegalClassFormatException;
import java.util.spi.ToolProvider;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefineRecord {

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    record Tester(int x, String y, long z) { }

    static class LoggingTransformer implements ClassFileTransformer {

        public LoggingTransformer() {}

        public byte[] transform(ClassLoader loader, String className,
                                Class classBeingRedefined, ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            return null;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        LoggingTransformer t = new LoggingTransformer();
        inst.addTransformer(t, true);
        {
            Class demoClass = Class.forName("RedefineRecord$Tester");
            inst.retransformClasses(demoClass);
        }
    }

    private static void buildAgent() {
        try {
            ClassFileInstaller.main("RedefineRecord");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try {
            PrintWriter pw = new PrintWriter("MANIFEST.MF");
            pw.println("Premain-Class: RedefineRecord");
            pw.println("Agent-Class: RedefineRecord");
            pw.println("Can-Redefine-Classes: true");
            pw.println("Can-Retransform-Classes: true");
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        if (JAR.run(System.out, System.err, "-cmf", "MANIFEST.MF", "redefineagent.jar", "RedefineRecord.class") != 0) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length == 1 && argv[0].equals("buildagent")) {
            buildAgent();
            return;
        }
        if (argv.length == 1 && argv[0].equals("runtest")) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:MetaspaceSize=12m",
                "-XX:MaxMetaspaceSize=12m",
                "-javaagent:redefineagent.jar",
                "RedefineRecord");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldNotContain("processing of -javaagent failed");
            output.shouldHaveExitValue(0);
        }
    }
}
