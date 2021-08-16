/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017 SAP SE. All rights reserved.
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
 * @bug 8173743
 * @requires vm.compMode != "Xcomp"
 * @requires vm.jvmti
 * @summary Failures during class definition can lead to memory leaks in metaspace
 * @requires vm.opt.final.ClassUnloading
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI test.DefineClass defineClass
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI test.DefineClass defineSystemClass
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+AllowParallelDefineClass
 *                   test.DefineClass defineClassParallel
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-AllowParallelDefineClass
 *                   test.DefineClass defineClassParallel
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Djdk.attach.allowAttachSelf test.DefineClass redefineClass
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Djdk.attach.allowAttachSelf test.DefineClass redefineClassWithError
 * @author volker.simonis@gmail.com
 */

package test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.sun.tools.attach.VirtualMachine;

import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class DefineClass {

    private static Instrumentation instrumentation;

    public void getID(CountDownLatch start, CountDownLatch stop) {
        String id = "AAAAAAAA";
        System.out.println(id);
        try {
            // Signal that we've entered the activation..
            start.countDown();
            //..and wait until we can leave it.
            stop.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(id);
        return;
    }

    private static class MyThread extends Thread {
        private DefineClass dc;
        private CountDownLatch start, stop;

        public MyThread(DefineClass dc, CountDownLatch start, CountDownLatch stop) {
            this.dc = dc;
            this.start = start;
            this.stop = stop;
        }

        public void run() {
            dc.getID(start, stop);
        }
    }

    private static class ParallelLoadingThread extends Thread {
        private MyParallelClassLoader pcl;
        private CountDownLatch stop;
        private byte[] buf;

        public ParallelLoadingThread(MyParallelClassLoader pcl, byte[] buf, CountDownLatch stop) {
            this.pcl = pcl;
            this.stop = stop;
            this.buf = buf;
        }

        public void run() {
            try {
                stop.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                @SuppressWarnings("unchecked")
                Class<DefineClass> dc = (Class<DefineClass>) pcl.myDefineClass(DefineClass.class.getName(), buf, 0, buf.length);
            }
            catch (LinkageError jle) {
                // Expected with a parallel capable class loader and
                // -XX:+AllowParallelDefineClass
                pcl.incrementLinkageErrors();
            }

        }
    }

    static private class MyClassLoader extends ClassLoader {
        public Class<?> myDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
            return defineClass(name, b, off, len, null);
        }
    }

    static private class MyParallelClassLoader extends ClassLoader {
        static {
            System.out.println("parallelCapable : " + registerAsParallelCapable());
        }
        public Class<?> myDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
            return defineClass(name, b, off, len, null);
        }
        public synchronized void incrementLinkageErrors() {
            linkageErrors++;
        }
        public int getLinkageErrors() {
            return linkageErrors;
        }
        private volatile int linkageErrors;
    }

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("Loading Java Agent.");
        instrumentation = inst;
    }


    private static void loadInstrumentationAgent(String myName, byte[] buf) throws Exception {
        // Create agent jar file on the fly
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().put(new Attributes.Name("Agent-Class"), myName);
        m.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
        File jarFile = File.createTempFile("agent", ".jar");
        jarFile.deleteOnExit();
        JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile), m);
        jar.putNextEntry(new JarEntry(myName.replace('.', '/') + ".class"));
        jar.write(buf);
        jar.close();
        String pid = Long.toString(ProcessTools.getProcessId());
        System.out.println("Our pid is = " + pid);
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(jarFile.getAbsolutePath());
    }

    private static byte[] getBytecodes(String myName) throws Exception {
        InputStream is = DefineClass.class.getResourceAsStream(myName + ".class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
        buf = baos.toByteArray();
        System.out.println("sizeof(" + myName + ".class) == " + buf.length);
        return buf;
    }

    private static int getStringIndex(String needle, byte[] buf) {
        return getStringIndex(needle, buf, 0);
    }

    private static int getStringIndex(String needle, byte[] buf, int offset) {
        outer:
        for (int i = offset; i < buf.length - offset - needle.length(); i++) {
            for (int j = 0; j < needle.length(); j++) {
                if (buf[i + j] != (byte)needle.charAt(j)) continue outer;
            }
            return i;
        }
        return 0;
    }

    private static void replaceString(byte[] buf, String name, int index) {
        for (int i = index; i < index + name.length(); i++) {
            buf[i] = (byte)name.charAt(i - index);
        }
    }

    public static WhiteBox wb = WhiteBox.getWhiteBox();

    private static void checkClasses(int expectedCount, boolean reportError) {
        int count = wb.countAliveClasses("test.DefineClass");
        String res = "Should have " + expectedCount +
                     " DefineClass instances and we have: " + count;
        System.out.println(res);
        if (reportError && count != expectedCount) {
            throw new RuntimeException(res);
        }
    }

    public static final int ITERATIONS = 10;

    private static void checkClassesAfterGC(int expectedCount) {
        // The first System.gc() doesn't clean metaspaces but triggers cleaning
        // for the next safepoint.
        // In the future the ServiceThread may clean metaspaces, but this loop
        // should give it enough time to run, when that is changed.
        // We might need to revisit this test though.
        for (int i = 0; i < ITERATIONS; i++) {
            System.gc();
            System.out.println("System.gc()");
            // Break if the GC has cleaned metaspace before iterations.
            if (wb.countAliveClasses("test.DefineClass") == expectedCount) break;
        }
        checkClasses(expectedCount, true);
    }

    public static void main(String[] args) throws Exception {
        String myName = DefineClass.class.getName();
        byte[] buf = getBytecodes(myName.substring(myName.lastIndexOf(".") + 1));
        int iterations = (args.length > 1 ? Integer.parseInt(args[1]) : ITERATIONS);

        if (args.length == 0 || "defineClass".equals(args[0])) {
            MyClassLoader cl = new MyClassLoader();
            for (int i = 0; i < iterations; i++) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<DefineClass> dc = (Class<DefineClass>) cl.myDefineClass(myName, buf, 0, buf.length);
                    System.out.println(dc);
                }
                catch (LinkageError jle) {
                    // Can only define once!
                    if (i == 0) throw new Exception("Should succeed the first time.");
                }
            }
            // We expect to have two instances of DefineClass here: the initial version in which we are
            // executing and another version which was loaded into our own classloader 'MyClassLoader'.
            // All the subsequent attempts to reload DefineClass into our 'MyClassLoader' should have failed.
            // The ClassLoaderDataGraph has the failed instances recorded at least until the next GC.
            checkClasses(2, false);
            // At least after some System.gc() the failed loading attempts should leave no instances around!
            checkClassesAfterGC(2);
        }
        else if ("defineSystemClass".equals(args[0])) {
            MyClassLoader cl = new MyClassLoader();
            int index = getStringIndex("test/DefineClass", buf);
            replaceString(buf, "java/DefineClass", index);
            while ((index = getStringIndex("Ltest/DefineClass;", buf, index + 1)) != 0) {
                replaceString(buf, "Ljava/DefineClass;", index);
            }
            index = getStringIndex("test.DefineClass", buf);
            replaceString(buf, "java.DefineClass", index);

            for (int i = 0; i < iterations; i++) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<DefineClass> dc = (Class<DefineClass>) cl.myDefineClass(null, buf, 0, buf.length);
                    throw new RuntimeException("Defining a class in the 'java' package should fail!");
                }
                catch (java.lang.SecurityException jlse) {
                    // Expected, because we're not allowed to define a class in the 'java' package
                }
            }
            // We expect to stay with one (the initial) instances of DefineClass.
            // All the subsequent attempts to reload DefineClass into the 'java' package should have failed.
            // The ClassLoaderDataGraph has the failed instances recorded at least until the next GC.
            checkClasses(1, false);
            checkClassesAfterGC(1);
        }
        else if ("defineClassParallel".equals(args[0])) {
            MyParallelClassLoader pcl = new MyParallelClassLoader();
            CountDownLatch stop = new CountDownLatch(1);

            Thread[] threads = new Thread[iterations];
            for (int i = 0; i < iterations; i++) {
                (threads[i] = new ParallelLoadingThread(pcl, buf, stop)).start();
            }
            stop.countDown(); // start parallel class loading..
            // ..and wait until all threads loaded the class
            for (int i = 0; i < iterations; i++) {
                threads[i].join();
            }
            System.out.print("Counted " + pcl.getLinkageErrors() + " LinkageErrors ");
            System.out.println(pcl.getLinkageErrors() == 0 ?
                    "" : "(use -XX:+AllowParallelDefineClass to avoid this)");
            // After System.gc() we expect to remain with two instances: one is the initial version which is
            // kept alive by this main method and another one in the parallel class loader.
            checkClassesAfterGC(2);
        }
        else if ("redefineClass".equals(args[0])) {
            loadInstrumentationAgent(myName, buf);
            int index = getStringIndex("AAAAAAAA", buf);
            CountDownLatch stop = new CountDownLatch(1);

            Thread[] threads = new Thread[iterations];
            for (int i = 0; i < iterations; i++) {
                buf[index] = (byte) ('A' + i + 1); // Change string constant in getID() which is legal in redefinition
                instrumentation.redefineClasses(new ClassDefinition(DefineClass.class, buf));
                DefineClass dc = DefineClass.class.newInstance();
                CountDownLatch start = new CountDownLatch(1);
                (threads[i] = new MyThread(dc, start, stop)).start();
                start.await(); // Wait until the new thread entered the getID() method
            }
            // We expect to have one instance for each redefinition because they are all kept alive by an activation
            // plus the initial version which is kept active by this main method.
            checkClasses(iterations + 1, true);
            stop.countDown(); // Let all threads leave the DefineClass.getID() activation..
            // ..and wait until really all of them returned from DefineClass.getID()
            for (int i = 0; i < iterations; i++) {
                threads[i].join();
            }
            // After System.gc() we expect to remain with two instances: one is the initial version which is
            // kept alive by this main method and another one which is the latest redefined version.
            checkClassesAfterGC(2);
        }
        else if ("redefineClassWithError".equals(args[0])) {
            loadInstrumentationAgent(myName, buf);
            int index = getStringIndex("getID", buf);

            for (int i = 0; i < iterations; i++) {
                buf[index] = (byte) 'X'; // Change getID() to XetID() which is illegal in redefinition
                try {
                    instrumentation.redefineClasses(new ClassDefinition(DefineClass.class, buf));
                    throw new RuntimeException("Class redefinition isn't allowed to change method names!");
                }
                catch (UnsupportedOperationException uoe) {
                    // Expected because redefinition can't change the name of methods
                }
            }
            // We expect just a single DefineClass instance because failed redefinitions should
            // leave no garbage around.
            // The ClassLoaderDataGraph has the failed instances recorded at least until the next GC.
            checkClasses(1, false);
            // At least after a System.gc() we should definitely stay with a single instance!
            checkClassesAfterGC(1);
        }
    }
}
