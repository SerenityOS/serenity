/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that retransforming and redefining anonymous classes gets UnmodifiableClassException
 * @requires vm.jvmti
 * @modules java.base/jdk.internal.misc
 * @modules java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main ModifyAnonymous buildagent
 * @run main/othervm -javaagent:redefineagent.jar ModifyAnonymous
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.RuntimeException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ModifyAnonymous {

    public static class LambdaTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException {
            return null;
        }
    }

    static Instrumentation inst = null;
    static volatile boolean done = false;

    public static void premain(String args, Instrumentation instrumentation) {

        inst = instrumentation;
        System.out.println("javaagent in da house!");
        instrumentation.addTransformer(new LambdaTransformer());
    }

    private static void buildAgent() {
        try {
            ClassFileInstaller.main("ModifyAnonymous");
        } catch (Exception e) {
            throw new RuntimeException("Could not write agent classfile", e);
        }

        try {
            PrintWriter pw = new PrintWriter("MANIFEST.MF");
            pw.println("Premain-Class: ModifyAnonymous");
            pw.println("Agent-Class: ModifyAnonymous");
            pw.println("Can-Retransform-Classes: true");
            pw.println("Can-Redefine-Classes: true");
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write manifest file for the agent", e);
        }

        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!jarTool.run(new String[] { "-cmf", "MANIFEST.MF", "redefineagent.jar", "ModifyAnonymous.class" })) {
            throw new RuntimeException("Could not write the agent jar file");
        }
    }

    public static class InstanceMethodCallSiteApp {

        public static void test() throws InterruptedException {
            for (int i = 0; i < 2; i++) {
                InstanceMethodCallSiteApp app = new InstanceMethodCallSiteApp();
                Runnable r = app::doWork;   // this creates an anonymous class
                while (!done) {
                    r.run();
                    Thread.sleep(10);
                }
            }
        }

        public void doWork() {
            System.out.print(".");
        }
    }

    static void runTest() {
        PrintWriter pw;
        String logName = System.getProperty("test.classes") +
            File.separator + "loadedClasses.log";
        // Create a log file to capture the names of the classes in the
        // allLoadedClasses array. The log file is for assisting in debugging
        // in case a null class is encountered in the allLoadedClasses array.
        try {
            pw = new PrintWriter(new FileOutputStream(
                new File(logName), true));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write loaded classes to log", e);
        }
        while (!done) {
            Class[] allLoadedClasses = inst.getAllLoadedClasses();
            int len = allLoadedClasses.length;
            pw.println("    allLoadedClasses length: " + len);
            for (int idx = 0; idx < len; idx++) {
                Class cls = allLoadedClasses[idx];
                pw.println("    " + idx + " " +
                    ((cls != null) ? cls.getName() : "null"));
            }
            for (int idx = 0; idx < len; idx++) {
                Class clazz = allLoadedClasses[idx];
                if (clazz == null) {
                    pw.flush();
                    pw.close();
                    throw new RuntimeException("null class encountered");
                }
                final String name = clazz.getName();
                if (name.contains("$$Lambda$") && name.contains("App")) {
                    if (inst.isModifiableClass(clazz)) {
                        pw.flush();
                        pw.close();
                        throw new RuntimeException ("Class should not be modifiable");
                    }
                    // Try to modify them anyway.
                    try {
                        System.out.println("retransform called for " + name);
                        inst.retransformClasses(clazz);
                    } catch(java.lang.instrument.UnmodifiableClassException t) {
                        System.out.println("PASSED: expecting UnmodifiableClassException");
                        t.printStackTrace();
                    }
                    try {
                        System.out.println("redefine called for " + name);
                        String newclass = "class Dummy {}";
                        byte[] bytecode = InMemoryJavaCompiler.compile("Dummy", newclass);
                        ClassDefinition cld = new ClassDefinition(clazz, bytecode);
                        inst.redefineClasses(new ClassDefinition[] { cld });
                    } catch(java.lang.instrument.UnmodifiableClassException t) {
                        System.out.println("PASSED: expecting UnmodifiableClassException");
                        t.printStackTrace();
                    } catch(java.lang.ClassNotFoundException e) {
                        pw.flush();
                        pw.close();
                        throw new RuntimeException ("ClassNotFoundException thrown");
                    }
                    done = true;
                }
            }
        }
        pw.flush();
        pw.close();
    }

    public static void main(String argv[]) throws InterruptedException, RuntimeException {
        if (argv.length == 1 && argv[0].equals("buildagent")) {
            buildAgent();
            return;
        }

        if (inst == null) {
            throw new RuntimeException("Instrumentation object was null");
        }

        new Thread() {
            public void run() {
                runTest();
            }
        }.start();

        // Test that NCDFE is not thrown for anonymous class:
        // ModifyAnonymous$InstanceMethodCallSiteApp$$Lambda$18
        try {
            ModifyAnonymous test = new ModifyAnonymous();
            InstanceMethodCallSiteApp.test();
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("FAILED: NoClassDefFoundError thrown for " + e.getMessage());
        }
        System.out.println("PASSED: NoClassDefFound error not thrown");
    }
}
