/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8140450
 * @library /test/lib
 * @summary Test if the getCallerClass method returns empty optional
 * @run main CallerFromMain exec
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class CallerFromMain {

    private static final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            ProcessBuilder pb = ProcessTools.createTestJvm("CallerFromMain");
            OutputAnalyzer output = ProcessTools.executeProcess(pb);
            System.out.println(output.getOutput());
            output.shouldHaveExitValue(0);
            return;
        }

        // StackWalker::getCallerClass
        // CallerFromMain::main
        // no caller
        try {
            Class<?> c = sw.getCallerClass();
            throw new RuntimeException("UOE not thrown. Caller: " + c);
        } catch (IllegalCallerException e) {}

        // StackWalker::getCallerClass
        // Runnable::run
        // Thread::run
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                Class<?> c = sw.getCallerClass();
                System.out.println("Call from Thread.run: " + c);
                assertThreadClassAsCaller(c);
            }
        });
        t1.setDaemon(true);
        t1.start();

        // StackWalker::getCallerClass
        // CallerFromMain::doit
        // Thread::run
        Thread t2 = new Thread(CallerFromMain::doit);
        t2.setDaemon(true);
        t2.start();

        // StackWalker::getCallerClass
        // MyRunnable::run
        // Thread::run
        Thread t3 = new Thread(new MyRunnable());
        t3.setDaemon(true);
        t3.start();

        // StackWalker::getCallerClass
        // Runnable::run
        // MyThread::run
        Thread t4 = new MyThread(new Runnable() {
            @Override
            public void run() {
                Class<?> c = sw.getCallerClass();
                System.out.println("Call from MyThread.run: " + c);
                if (c != MyThread.class) {
                    throw new RuntimeException("Expected MyThread.class but got " + c);
                }
            }
        });
        t4.setDaemon(true);
        t4.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }

    static class MyThread extends Thread {
        final Runnable runnable;
        MyThread(Runnable runnable) {
            super("MyThread");
            this.runnable = runnable;
        }
        public void run() {
            runnable.run();
        }
    }

    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            Class<?> c = sw.getCallerClass();
            System.out.println("Call from Thread::run: " + c);
            assertThreadClassAsCaller(c);
        }
     }

    static void doit() {
        Class<?> c = sw.getCallerClass();
        System.out.println("Call from CallerFromMain.doit: " + c);
        assertThreadClassAsCaller(c);
    }

    static void assertThreadClassAsCaller(Class<?> caller) {
        if (caller != Thread.class) {
            throw new RuntimeException("Expected Thread.class but got " + caller);
        }
    }
}
