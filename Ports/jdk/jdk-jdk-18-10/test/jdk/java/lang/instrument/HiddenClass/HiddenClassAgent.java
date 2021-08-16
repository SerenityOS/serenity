/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.instrument
 *          jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils *
 * @run shell ../MakeJAR3.sh HiddenClassAgent 'Can-Retransform-Classes: true'
 * @run main/othervm/native -javaagent:HiddenClassAgent.jar HiddenClassApp
 */

import java.lang.instrument.*;

/* Test that Instrumentation getAllLoadedClasses includes
 * hidden classes but getInitiatedClasses does not.
 * Check that all hidden classes are non-retransformable.
 */
public class HiddenClassAgent extends Thread {
    private static volatile boolean completed = false;
    private static volatile boolean failed = false;
    private static volatile boolean hiddenClassLoaded = false;

    private static Instrumentation instr = null;
    private static Object monitor = new Object();

    static void log(String str) { System.err.println(str); }
    public static boolean failed() { return failed; }

    public static
    boolean checkWaitForCompleteness() {
        try {
            synchronized (monitor) {
                while (!completed) {
                    monitor.wait(100);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            log("HiddenClassAgent: waitCheckForCompletness: Caught InterruptedException: " + ex);
        }
        return completed;
    }

    public static
    void setHiddenClassLoaded() {
        synchronized (monitor) {
            hiddenClassLoaded = true;
            monitor.notifyAll();
        }
    }

    private static
    void waitForHiddenClassLoad() {
        try {
            synchronized (monitor) {
                while (!hiddenClassLoaded) {
                    monitor.wait(100);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            log("HiddenClassAgent: waitForHiddenClassLoad: Caught InterruptedException: " + ex);
        }
    }

    private static
    ClassLoader testGetAllLoadedClasses() {
        boolean hiddenClassFound = false;
        ClassLoader loader = null;
        Class<?>[] classes = instr.getAllLoadedClasses();

        for (int i = 0; i < classes.length; i++) {
            Class klass = classes[i];

            if (!klass.isHidden() || !klass.getName().contains("HiddenClass/")) {
                continue;
            }
            log("HiddenClassAgent: getAllLoadedClasses returned hidden class: " + klass);
            hiddenClassFound = true;
            loader = klass.getClassLoader();
            log("HiddenClassAgent: class loader of hidden class: " + loader);

            try {
                instr.retransformClasses(klass);
                log("HiddenClassAgent: FAIL: hidden class is retransformable: " + klass);
                failed = true;
            } catch (UnmodifiableClassException e) {
                log("HiddenClassAgent: Got expected UnmodifiableClassException for class: " + klass);
            } catch (Throwable e) {
                log("HiddenClassAgent: FAIL: unexpected throwable in hidden class retransform: " + klass);
                log("HiddenClassAgent: got Throwable" + e);
                failed = true;
            }
        }
        if (!hiddenClassFound) {
            log("HiddenClassAgent: FAIL: a hidden class is not found in getAllLoadedClasses list");
            failed = true;
        }
        return loader;
    }

    private static
    void testGetInitiatedClasses(ClassLoader loader) {
        Class<?>[] classes = instr.getInitiatedClasses(loader);
        for (int i = 0; i < classes.length; i++) {
            Class klass = classes[i];

            if (klass.isHidden()) {
                log("HiddenClassAgent: FAIL: getInitiatedClasses returned hidden class: " + klass);
                failed = true;
                return;
            }
        }
        log("HiddenClassAgent: getInitiatedClasses returned no hidden classes as expected");
    }

    public static void
    premain(String agentArgs, Instrumentation instrumentation) {
        instr = instrumentation;
        Thread agentThread = new HiddenClassAgent();
        agentThread.start();
    }

    public void run () {
        log("HiddenClassAgent: started");
        waitForHiddenClassLoad();

        // Test getAllLoadedClasses
        ClassLoader loader = testGetAllLoadedClasses();

        // Test getInitiatedClasses
        testGetInitiatedClasses(null);
        if (loader != null) {
            testGetInitiatedClasses(loader);
        }

        synchronized (monitor) {
            completed = true;
            monitor.notifyAll();
        }
        log("HiddenClassAgent: finished");
    }
}
