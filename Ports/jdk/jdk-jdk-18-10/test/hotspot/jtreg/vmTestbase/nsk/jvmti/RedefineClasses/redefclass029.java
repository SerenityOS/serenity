/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.RedefineClasses;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * The test exercises that the JVMTI function RedefineClasses()
 * enable to redefine a static inner (nested) class properly. Redefiniton
 * is performed in asynchronous manner from a separate
 * thread when the VM is provoked to switch into compiled mode.<br>
 * The test works as follows. Two threads are started: java thread
 * executing a nested class to be redefined, and native one executing
 * an agent code. Then the nested class method <code>redefclass029HotMethod()</code>
 * is provoked to be compiled (optimized), and thus the JVMTI event
 * <code>CompiledMethodLoad</code> should be sent. After that, the
 * agent redefines the nested class. Different kinds of outer fields
 * are accessed from executing methods in both versions of the
 * nested class. Upon the redefinition, the main test thread verifies
 * via the outer fields values that the nested method <code>run()</code>
 * having an active stack frame stays obsolete but the other nested
 * methods have been redefined. It also verifies that the outer class
 * is still can access the nested class fields after the redefinition.
 */
public class redefclass029 extends DebugeeClass {
    final static String REDEF_CLS_SIGNATURE =
        "Lnsk/jvmti/RedefineClasses/redefclass029$RedefClass;";

    static int status = Consts.TEST_PASSED;

    static Log log = null;

    // path to the directory with redefining class files
    static String clfBasePath = null;

    // dummy outer fields to be changed by the nested class
    private static int prStOuterFl[] = {0,0,0};
    static int packStOuterFl[] = {0,0,0};
    public static int pubStOuterFl[] = {0,0,0};

    /* a dummy outer private field to be accessed or not in
       the nested class and thus provoking compiler to add or not
       a synthetic access method into the outer class. */
    private static char prOuterFieldToBeAccessed = 'a';

    native static void storeClassBytes(byte[] classBytes);
    native static void notifyNativeAgent(); /** notify native agent that "hot" method is entered */
    native static boolean isRedefinitionOccurred(); /** check whether class redefinition was already occurred */

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new redefclass029().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        int iter;

        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);

        try {
            // number of iterations for a method to become 'hotspot' one
            iter = Integer.parseInt(args[0]);

            // directory to search a redefining class
            clfBasePath = args[1];
        } catch(Exception e) {
            throw new Failure("TEST BUG: Wrong test parameters, caught: "
                + e);
        }

        storeClassBytes(loadFromClassFile(REDEF_CLS_SIGNATURE));

        // testing sync
        log.display("waiting for the agent start ...\n");
        status = checkStatus(status);

        log.display("starting an auxiliary thread ...\n");
        RedefClass redefCls = new RedefClass(iter);
        redefCls.setDaemon(true);
        synchronized(redefCls) {
            redefCls.start();
            log.display("waiting for auxiliary thread readiness...\n");
            try {
                redefCls.wait(); // wait for the thread's readiness
            } catch (InterruptedException e) {
                redefCls.interrupt();
                throw new Failure("TEST FAILURE: waiting for auxiliary thread start, caught: "
                    + e);
            }
        }

        // testing sync
        log.display("auxiliary thread started\n"
            + "waiting for the agent finish ...\n");
        status = checkStatus(status);

        boolean isRedefinitionStarted = waitForRedefinitionStarted();
        boolean isRedefinitionCompleted = false;
        if (isRedefinitionStarted) {
            isRedefinitionCompleted = waitForRedefinitionCompleted(redefCls);
        }

        log.display("waiting for auxiliary thread ...\n");
        redefCls.stopMe = true;
        try {
            redefCls.join();
        } catch (InterruptedException e) {
            redefCls.interrupt();
            throw new Failure("TEST FAILURE: waiting for auxiliary thread death, caught: "
                + e);
        }

        // CR 6604375: check whether class redefinition occurred
        if (isRedefinitionCompleted) {
            // verify results
            checkOuterFields(0, 1);
            checkOuterFields(1, 2);
            checkOuterFields(2, 2);
            checkInnerFields(redefCls, 1);
        }

        return status;
    }

    private boolean waitForRedefinitionStarted() {
        final int SLEEP_MS = 20;
        int iterationsLeft = 2000 / SLEEP_MS;
        while (iterationsLeft >= 0) {
            if (isRedefinitionOccurred()) {
                log.display("Redefinition started.");
                return true;
            }
            --iterationsLeft;
            safeSleep(SLEEP_MS);
        }
        log.complain("Redefinition not started. May need more time for -Xcomp.");
        status = Consts.TEST_FAILED;
        return false;
    }

    private boolean waitForRedefinitionCompleted(RedefClass redefCls) {
        final int SLEEP_MS = 20;
        int iterationsLeft = 10000 / SLEEP_MS;
        while (iterationsLeft >= 0) {
            // Check if new code has changed fields.
            if (prStOuterFl[1] == 2 && prStOuterFl[2] == 2 && redefCls.prInnerFl == 1) {
                log.display("Redefinition completed.");
                return true;
            }
            --iterationsLeft;
            safeSleep(SLEEP_MS);
        }
        log.complain("Redefinition not completed. May need more time for -Xcomp.");
        status = Consts.TEST_FAILED;
        return false;
    }


    private void checkOuterFields(int index, int expValue) {
        if (prStOuterFl[index] != expValue
                || packStOuterFl[index] != expValue
                || pubStOuterFl[index] != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of outer fields:"
                + "\n\tprStOuterFl["+ index +"]: got: " + prStOuterFl[index]
                + ", expected: " + expValue
                + "\n\tpackStOuterFl["+ index +"]: got: " + packStOuterFl[index]
                + ", expected: " + expValue
                + "\n\tpubStOuterFl["+ index +"]: got: " + pubStOuterFl[index]
                + ", expected: " + expValue);
        }
    }

    private void checkInnerFields(RedefClass redefCls, int expValue) {
        if (redefCls.prInnerFl != expValue
                || redefCls.packInnerFl != expValue
                || redefCls.pubInnerFl != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of inner fields:"
                + "\n\tprInnerFl: got: " + redefCls.prInnerFl
                + ", expected: " + expValue
                + "\n\tpackInnerFl: got: " + redefCls.packInnerFl
                + ", expected: " + expValue
                + "\n\tpubInnerFl: got: " + redefCls.pubInnerFl
                + ", expected: " + expValue);
        }
    }

    /**
     * Load bytes of a redefining class.
     */
    private static byte[] loadFromClassFile(String signature) {
        String testPath = clfBasePath + File.separator
            + "newclass" + File.separator
            + signature.substring(1, signature.length()-1).replace('/', File.separatorChar)
            + ".class";
        File classFile = null;

        log.display("looking for class file at\n\t"
            + testPath + " ...\n");

        try {
            classFile = new File(testPath);
        } catch (NullPointerException e) {
            throw new Failure("FAILURE: failed to open class file, caught: "
                + e);
        }

        log.display("loading " + classFile.length()
            + " bytes from class file "+ testPath + " ...\n");
        byte[] buf = new byte[(int) classFile.length()];
        try {
            InputStream in = new FileInputStream(classFile);
            in.read(buf);
            in.close();
        } catch (Exception e) {
            throw new Failure("FAILURE: failed to load bytes from class file, caught: "
                + e);
        }

        log.display(classFile.length()
            + " bytes of a redefining class loaded\n");

        return buf;
    }

    /**
     * Static inner (nested) class to be redefined in the agent.
     */
    static class RedefClass extends Thread {
        boolean stopMe = false;
        int iter;

        // dummy inner fields to be accessed by the outer class
        private int prInnerFl = 1;
        int packInnerFl = 1;
        public int pubInnerFl = 1;

        RedefClass(int iter) {
            super("RedefClass");
            this.iter = iter;
        }

        /**
         * This method will have an active stack frame during
         * nested class redinition, so this version should continue
         * execution and thus outer fields should have values equal 1.
         */
        public void run() {
            log.display(this.getName() + ": started");
            synchronized(this) {
                this.notify(); // notify the main thread

                prStOuterFl[0] = 1;
                packStOuterFl[0] = 1;
                pubStOuterFl[0] = 1;

                while(!stopMe) {
                    warmUpMethod();

                    // get the main thread chance to obtain CPU
                    try {
                        this.wait(100);
                    } catch(Exception e) {}
                }
                log.display(this.getName() + ": exiting");
            }
        }

        void warmUpMethod() {
            prStOuterFl[1] = 1;
            packStOuterFl[1] = 1;
            pubStOuterFl[1] = 1;

            for (int i=0; i<iter; i++)
                redefclass029HotMethod(i);

            redefclass029HotMethod(10);
        }

        /**
         * Hotspot method to be compiled.
         */
        void redefclass029HotMethod(int i) {
            prStOuterFl[2] = 1;
            packStOuterFl[2] = 1;
            pubStOuterFl[2] = 1;

            int j=0;

            j +=i;
            j--;
            if (j >10)
                j = 0;

            notifyNativeAgent();
        }

        /**
         * A dummy method accessing a private field of the outer class.
         * It provokes compiler to add a synthetic access method
         * into the outer class.
         */
         void methAccessingOuterPrivateField() {
            prOuterFieldToBeAccessed = 'b';
         }
    }

}
