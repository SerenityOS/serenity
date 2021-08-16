/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4227137
 * @summary Basic functional test for virtual-machine shutdown hooks
 * @modules java.desktop
 * @library /test/lib
 * @build jdk.test.lib.process.*
 * @run testng Basic
 */

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import jdk.test.lib.process.ProcessTools;

public class Basic {

    static Runtime rt = Runtime.getRuntime();
    static PrintStream out = System.out;

    // Expect that no finalizer is invoked at exit
    @DataProvider(name = "testcase")
    public Object[][] getTestCase() {
        return new Object[][] {
            { "fallThrough", 0, "h1", "" },
            { "exit0",       0, "h1", "" },
            { "exit0NoHook", 0, "",   "" },
            { "exit1",       1, "h1", "" },
            { "exit1NoHook", 1, "",   "" },
            { "halt",        0, "",   "" },
            { "haltNoHook",  0, "",   "" },
            { "haltInHook",  0, "h1", "" },
            { "addLate",     0, "h1",
                "Caught as expected: java.lang.IllegalStateException: Shutdown in progress" },
            { "removeLate",  0, "h2",
                "Caught as expected: java.lang.IllegalStateException: Shutdown in progress" }
        };
    }

    @Test(dataProvider = "testcase")
    public void test(String testcase, int exitValue, String hook, String finalizer)
            throws Exception {
        System.out.println("Test " + testcase);
        ProcessTools.executeTestJava("Basic", testcase)
                .shouldHaveExitValue(exitValue)
                .stdoutShouldMatch(
                    hook + (hook.isEmpty() ? "" : System.lineSeparator()) + finalizer);
        System.out.println("Passed");
    }

    public static class Fin {
        String name;

        public Fin(String name) {
            this.name = name;
        }

        public void go() { }

        protected void finalize() {
            out.println(name);
            go();
        }
    }


    public static class Hook extends Thread {
        String name;

        public Hook(String name) {
            this.name = name;
        }

        public void go() { }

        public void run() {
            out.println(name);
            go();
        }
    }

    public static void fallThrough() throws Exception {
        rt.addShutdownHook(new Hook("h1"));
        Fin f = new Fin("f1");
    }

    public static void exit0() throws Exception {
        rt.addShutdownHook(new Hook("h1"));
        Fin f = new Fin("f1");
        rt.exit(0);
    }

    public static void exit0NoHook() throws Exception {
        Fin f = new Fin("f1");
        rt.exit(0);
    }

    /* Finalizer should not run */
    public static void exit1() throws Exception {
        rt.addShutdownHook(new Hook("h1"));
        Fin f = new Fin("f1");
        rt.exit(1);
    }

    public static void exit1NoHook() throws Exception {
        Fin f = new Fin("f1");
        rt.exit(1);
    }

    public static void halt() throws Exception {
        rt.addShutdownHook(new Hook("h1") {
            public void go() { rt.halt(1); }});
        Fin f = new Fin("f1") { public void go() { rt.halt(1); }};
        rt.halt(0);
    }

    public static void haltNoHook() throws Exception {
        Fin f = new Fin("f1") { public void go() { rt.halt(1); }};
        rt.halt(0);
    }

    public static void haltInHook() throws Exception {
        rt.addShutdownHook(new Hook("h1") {
            public void go() { rt.halt(0); }});
        Fin f = new Fin("f1");
        rt.exit(1);
    }

    public static void addLate() throws Exception {
        rt.addShutdownHook(new Hook("h1") {
            public void go() {
                try {
                    rt.addShutdownHook(new Hook("h2"));
                } catch (IllegalStateException x) {
                    out.println("Caught as expected: " + x);
                    rt.halt(0);
                }
            }});
        rt.exit(1);
    }

    public static void removeLate() throws Exception {
        final Hook h1 = new Hook("h1");
        rt.addShutdownHook(new Hook("h2") {
            public void go() {
                try {
                    rt.removeShutdownHook(h1);
                } catch (IllegalStateException x) {
                    out.println("Caught as expected: " + x);
                    rt.halt(0);
                }
            }});
        rt.exit(1);
    }

    /* For INT, HUP, TERM */
    public static void stall() throws Exception {
        Fin f = new Fin("f1");
        rt.addShutdownHook(new Hook("h1"));
        out.print("Type ^C now: ");
        out.flush();
        Thread.sleep(100000);
    }


    public static void main(String[] args) throws Throwable {
        Method m = Basic.class.getMethod(args[0], new Class[] { });
        String log = null;
        try {
            log = System.getProperty("log");
        } catch (SecurityException x) { }
        if (log != null) {
            out = new PrintStream(new FileOutputStream(log), true);
        }
        try {
            m.invoke(null, new Object[] { });
        } catch (InvocationTargetException x) {
            throw x.getTargetException();
        }
    }

}
