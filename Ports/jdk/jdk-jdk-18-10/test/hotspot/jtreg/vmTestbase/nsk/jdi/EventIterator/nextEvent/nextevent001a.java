/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.EventIterator.nextEvent;

import nsk.share.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the nextevent001 JDI test.
 */

public class nextevent001a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static ArgumentHandler argHandler;
    static Log log;

    //--------------------------------------------------   log procedures

    private static void log1(String message) {
        log.display("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        log.complain("**> debuggee: " + message);
    }

    //====================================================== test program

    static TestClass tcObject = new TestClass();

    static String threadNames[] = {
               "awThread",  "mwThread",  "bpThread", "excThread",
               "menThread", "mexThread", "stThread"
               };

    static int threadsN = threadNames.length;

    static Thread threads[] = new Thread[threadsN];

    static Thread thread2 = null;

    //------------------------------------------------------ common section

    static int exitCode = PASSED;

    static int instruction = 1;
    static int end         = 0;

    static int maxInstr    = 1;    // 2;

    static int lineForComm = 2;

    private static void methodForCommunication() {
        int i1 = instruction;
        int i2 = i1;
        int i3 = i2;
    }
    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();

        log1("debuggee started!");

        label0:
            for (int i = 0; ; i++) {

                if (instruction > maxInstr) {
                    logErr("ERROR: unexpected instruction: " + instruction);
                    exitCode = FAILED;
                    break ;
                }

                switch (i) {

    //------------------------------------------------------  section tested

                    case 0:
                          thread2 = new Thread3nextevent001a("thread2");
                          threadStart(thread2);
                          try {
                              // we should wait here for thread2 completion (see 6671428)
                              thread2.join();
                          } catch (InterruptedException e) {
                              logErr("ERROR: unexpected exception: " + e);
                              exitCode = FAILED;
                              break ;
                          }

                          for (int n1 = 0; n1 < threadsN; n1++) {
                              if (n1 < threadsN-1)
                                  threads[n1] =  new Thread1nextevent001a(threadNames[n1]);
                              else
                                  threads[n1] =  new Thread2nextevent001a(threadNames[n1]);
                          }
                          log1("       threads has been created");

                          synchronized (lockingObject2) {
                              log1("      loop: threadStart(threads[n2])");
                              for (int n2 = 0; n2 < threadsN; n2++)
                                  if ( threadStart(threads[n2]) != PASSED )
                                      break label0;

                              log1("      methodForCommunication();");
                              methodForCommunication();
                          }

                          for (int n2 = 0; n2 < threadsN; n2++) {
                              synchronized (locks[n2]) {
                                  log1("      synchronized (locks[n2]) : n2 == " + n2);
                              }
                          }

                          methodForCommunication();
                                break ;

    //-------------------------------------------------    standard end section

                    default:
                                instruction = end;
                                methodForCommunication();
                                break label0;
                }
            }

        System.exit(exitCode + PASS_BASE);
    }

    static Object waitnotifyObj = new Object();
    static Object lockingObject = new Object();

    static int threadStart(Thread t) {
        synchronized (waitnotifyObj) {
            t.start();
            try {
                waitnotifyObj.wait();
            } catch ( Exception e) {
                exitCode = FAILED;
                logErr("       Exception : " + e );
                return FAILED;
            }
        }
        return PASSED;
    }


    public static void nullMethod() {
        throw new NullPointerException("test");
    }

    static Object lockingObject2 = new Object();
    static Object locks[]        = new Object[threadsN];

    static volatile int n = 0;

    static class Thread1nextevent001a extends Thread {

        int threadIndex;

        public Thread1nextevent001a(String threadName) {
            super(threadName);
            threadIndex = n;
            locks[threadIndex] = new Object();
            n++;
        }

        public void run() {
            log3("  'run': enter  :: threadIndex == " + threadIndex);

            synchronized (locks[threadIndex]) {
                log3("enter synchronized (locks[threadIndex]) ::  threadIndex == " + threadIndex);
                synchronized (waitnotifyObj) {
                    waitnotifyObj.notify();
                }
                log3("  'run': exit  synchronized (waitnotifyObj)");

                synchronized (lockingObject2) {
                    TestClass.method();
                }
                log3("exit  synchronized (locks[threadIndex]) ::  threadIndex == " + threadIndex);
            }
            return;
        }

    }

    static class Thread2nextevent001a extends Thread {

        int threadIndex;

        public Thread2nextevent001a(String threadName) {
            super(threadName);
            threadIndex = n;
            locks[threadIndex] = new Object();
            n++;
        }

        public void run() {
            log3("  'run': enter  :: threadIndex == " + threadIndex);

            synchronized (locks[threadIndex]) {
                log3("enter synchronized (locks[threadIndex]) ::  threadIndex == " + threadIndex);
                synchronized (waitnotifyObj) {
                    waitnotifyObj.notify();
                }
                m1();
                log3("exit  synchronized (locks[threadIndex]) ::  threadIndex == " + threadIndex);
            }
            return;
        }

        private void m1() {
            synchronized (lockingObject2) {
                log3("  'm1': enter");

                log3("  'm1': exit");
            }
        }

    }

    static class Thread3nextevent001a extends Thread {

        String tName = null;

        public Thread3nextevent001a(String threadName) {
            super(threadName);
            tName = threadName;
        }

        public void run() {
            log3("  'run': enter  :: threadName == " + tName);
            synchronized (waitnotifyObj) {
                    waitnotifyObj.notify();
            }
            log3("  'run': exit   :: threadName == " + tName);
            return;
        }
    }


    public static void log3(String str) {
        log1(Thread.currentThread().getName() + " : " + str);
    }

}

class TestClass {

    static int breakpointLine = 3;
    static String awFieldName = "var1";
    static String mwFieldName = "var2";

    static int var1 = 0;
    static int var2 = 0;
    static int var3 = 0;

    static void method () {
        var1 += 1;
        var3 += 1;
        var2  = var3;
        try {
            nextevent001a.nullMethod();
        } catch ( NullPointerException e ) {
            nextevent001a.log3("  NullPointerException : " + e);
        }
    }
}
