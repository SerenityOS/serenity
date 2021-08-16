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
 * @bug 8260009
 * @library /test/lib
 * @summary Test that a permitted subclass, that is resolved by its super
            class, can be redefined.
 * @modules java.base/jdk.internal.misc
 * @modules java.instrument
 * @requires vm.jvmti
 * @requires vm.flagless
 * @compile RedefinePermittedSubclass.java
 * @run driver RedefinePermittedSubclass buildagent
 * @run driver/timeout=6000 RedefinePermittedSubclass runtest
 */

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.RuntimeException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.spi.ToolProvider;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class RedefinePermittedSubclass {

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    non-sealed class A extends Tester {
       public void printIt() { System.out.println("In A"); }
    }

    sealed static class Tester permits A {
        // Make sure A is resolved in Tester's constant pool.
        public void resolveA(A a) {
            a.printIt();
        }
    }

    static class LoggingTransformer implements ClassFileTransformer {

        public LoggingTransformer() {}

        public byte[] transform(ClassLoader loader, String className,
                                Class classBeingRedefined, ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            return null;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        RedefinePermittedSubclass r = new RedefinePermittedSubclass();
        r.play(); // Resolve A before redefining it.
        LoggingTransformer t = new LoggingTransformer();
        inst.addTransformer(t, true);
        {
            Class demoClassA = Class.forName("RedefinePermittedSubclass$A");
            inst.retransformClasses(demoClassA);
        }
    }

    private static void buildAgent() {
        try {
            ClassFileInstaller.main("RedefinePermittedSubclass");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try {
            PrintWriter pw = new PrintWriter("MANIFEST.MF");
            pw.println("Premain-Class: RedefinePermittedSubclass");
            pw.println("Agent-Class: RedefinePermittedSubclass");
            pw.println("Can-Redefine-Classes: true");
            pw.println("Can-Retransform-Classes: true");
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        if (JAR.run(System.out, System.err, "-cmf",  "MANIFEST.MF", "redefineagent.jar",
                    "RedefinePermittedSubclass.class") != 0) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    public void play () {
        A a = new A();
        Tester t = new Tester();
        t.resolveA(a);
    }

    public static void main(String argv[]) throws Exception {
        RedefinePermittedSubclass r = new RedefinePermittedSubclass();
        r.play();
        if (argv.length == 1 && argv[0].equals("buildagent")) {
            buildAgent();
            return;
        }
        if (argv.length == 1 && argv[0].equals("runtest")) {
            String[] javaArgs1 = { "-XX:MetaspaceSize=12m", "-XX:MaxMetaspaceSize=12m",
                                   "-javaagent:redefineagent.jar", "RedefinePermittedSubclass"};
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(javaArgs1);
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldNotContain("processing of -javaagent failed");
            output.shouldHaveExitValue(0);
        }
    }
}
