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
 * enable to redefine a local inner class properly. Redefiniton
 * is performed in asynchronous manner from a separate thread when
 * the VM is provoked to switch into compiled mode.<br>
 * The test works as follows. Two threads are started. One is a java
 * thread creating and executing a local class in an instance
 * invoker method of the outer class. Second is a native thread
 * executing an agent code. Then the local class method
 * <code>redefclass030HotMethod()</code> is provoked to be compiled
 * (optimized), and thus the JVMTI event <code>CompiledMethodLoad</code>
 * should be sent. After that, the agent redefines the local class.
 * Different kinds of outer fields and a local variable of the outer
 * invoker method are accessed from executing methods in both versions
 * of the local class. Upon the redefinition, the main test thread verifies
 * via the outer fields/local variable values that the inner methods
 * have been redefined. It also verifies that the outer class
 * is still can access the local class fields after the redefinition.
 */
public class redefclass030 extends DebugeeClass {
    final static String REDEF_CLS_SIGNATURE =
        "Lnsk/jvmti/RedefineClasses/redefclass030$RedefClassWrapper$1RedefClass;";

    static int status = Consts.TEST_PASSED;

    static Log log = null;

    // path to the directory with redefining class files
    static String clfBasePath = null;

    // dummy "outer-outer" fields to be changed by the local class
    private int prOuterOuterFl[] = {0,0};
    int packOuterOuterFl[] = {0,0};
    public int pubOuterOuterFl[] = {0,0};
    private static int prStOuterOuterFl[] = {0,0};
    static int packStOuterOuterFl[] = {0,0};
    public static int pubStOuterOuterFl[] = {0,0};

    native static void storeClassBytes(byte[] classBytes);
    native static void notifyNativeAgent(); /** notify native agent that "hot" method is entered */
    native static boolean isRedefinitionOccurred(); /** check whether class redefinition was already occurred */

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new redefclass030().runIt(args, out);
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
        RedefClassWrapper redefClsWrapper = new RedefClassWrapper(iter);
        redefClsWrapper.setDaemon(true);
        synchronized(redefClsWrapper) {
            redefClsWrapper.start();
            log.display("waiting for auxiliary thread readiness...\n");
            try {
                redefClsWrapper.wait(); // wait for the thread's readiness
            } catch (InterruptedException e) {
                redefClsWrapper.interrupt();
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
            isRedefinitionCompleted = waitForRedefinitionCompleted(redefClsWrapper);
        }

        log.display("waiting for auxiliary thread ...\n");
        redefClsWrapper.stopMe = true;
        try {
            redefClsWrapper.join();
        } catch (InterruptedException e) {
            redefClsWrapper.interrupt();
            throw new Failure("TEST FAILURE: waiting for auxiliary thread death, caught: "
                + e);
        }

        // CR 6604375: check whether class redefinition occurred
        if (isRedefinitionCompleted) {
            // verify results
            checkOuterOuterFields(0, 2);
            checkOuterOuterFields(1, 2);
            checkOuterFields(redefClsWrapper, 0, 2);
            checkOuterFields(redefClsWrapper, 1, 2);
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

    private boolean waitForRedefinitionCompleted(RedefClassWrapper redefClsWrapper) {
        final int SLEEP_MS = 20;
        int iterationsLeft = 10000 / SLEEP_MS;
        while (iterationsLeft >= 0) {
            // Check if new code has changed fields.
            if (prStOuterOuterFl[0] == 2 && prStOuterOuterFl[1] == 2 && redefClsWrapper.prOuterFl[1] == 2) {
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

    private void checkOuterOuterFields(int index, int expValue) {
        if (prOuterOuterFl[index] != expValue
                || packOuterOuterFl[index] != expValue
                || pubOuterOuterFl[index] != expValue
                || prStOuterOuterFl[index] != expValue
                || packStOuterOuterFl[index] != expValue
                || pubStOuterOuterFl[index] != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of outer fields of the class"
                + "\n\t\"" + this.toString() + "\":"
                + "\n\t\tprOuterOuterFl["+ index +"]: got: " + prOuterOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpackOuterOuterFl["+ index +"]: got: " + packOuterOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpubOuterOuterFl["+ index +"]: got: " + pubOuterOuterFl[index]
                + ", expected: " + expValue

                + "\n\t\tprStOuterOuterFl["+ index +"]: got: " + prStOuterOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpackStOuterOuterFl["+ index +"]: got: " + packStOuterOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpubStOuterOuterFl["+ index +"]: got: " + pubStOuterOuterFl[index]
                + ", expected: " + expValue);
        }
    }

    private void checkOuterFields(RedefClassWrapper redefClsWrapper, int index, int expValue) {
        if (redefClsWrapper.prOuterFl[index] != expValue
                || redefClsWrapper.packOuterFl[index] != expValue
                || redefClsWrapper.pubOuterFl[index] != expValue) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: unexpected values of outer fields of the class"
                + "\n\t\"" + redefClsWrapper.toString() + "\":"
                + "\n\t\tprOuterFl["+ index +"]: got: " + redefClsWrapper.prOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpackOuterFl["+ index +"]: got: " + redefClsWrapper.packOuterFl[index]
                + ", expected: " + expValue
                + "\n\t\tpubOuterFl["+ index +"]: got: " + redefClsWrapper.pubOuterFl[index]
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
     * Class executing the local inner class to be redefined.
     */
    class RedefClassWrapper extends Thread {
        boolean stopMe = false;
        int iter;

        // dummy outer fields to be changed by the local class
        private int prOuterFl[] = {0,0};
        int packOuterFl[] = {0,0};
        public int pubOuterFl[] = {0,0};

        RedefClassWrapper(int iter) {
            super("RedefClassWrapper");
            this.iter = iter;
        }

        public void run() {
            // dummy local vars to be changed by the local class
            final int outerLocalVar[] = {0,0};

            /**
             * Local inner class to be redefined in an agent.
             */
            class RedefClass {
                int iter;

                // dummy inner fields to be accessed by the outer class
                private int prInnerFl = 1;
                int packInnerFl = 1;
                public int pubInnerFl = 1;

                RedefClass(int iter) {
                    this.iter = iter;
                }

                void warmUpMethod() {
                    prOuterOuterFl[0] = 1;
                    packOuterOuterFl[0] = 1;
                    pubOuterOuterFl[0] = 1;
                    prStOuterOuterFl[0] = 1;
                    packStOuterOuterFl[0] = 1;
                    pubStOuterOuterFl[0] = 1;

                    prOuterFl[0] = 1;
                    packOuterFl[0] = 1;
                    pubOuterFl[0] = 1;

                    outerLocalVar[0] = 1;

                    for (int i=0; i<iter; i++)
                        redefclass030HotMethod(i);

                    redefclass030HotMethod(10);
                }

                /**
                 * Hotspot method to be compiled.
                 */
                void redefclass030HotMethod(int i) {
                    prOuterOuterFl[1] = 1;
                    packOuterOuterFl[1] = 1;
                    pubOuterOuterFl[1] = 1;
                    prStOuterOuterFl[1] = 1;
                    packStOuterOuterFl[1] = 1;
                    pubStOuterOuterFl[1] = 1;

                    prOuterFl[1] = 1;
                    packOuterFl[1] = 1;
                    pubOuterFl[1] = 1;

                    outerLocalVar[1] = 1;

                    int j=0;

                    j +=i;
                    j--;
                    if (j >10)
                        j = 0;

                    notifyNativeAgent();
                }
            }
            /*************************************************************/

            log.display(this.getName() + ": started");

            RedefClass redefCls = new RedefClass(iter);

            synchronized(this) {
                this.notify(); // notify the main thread

                while(!stopMe) {
                    redefCls.warmUpMethod();

                    // get the main thread chance to obtain CPU
                    try {
                        this.wait(100);
                    } catch(Exception e) {}
                }

                if (redefCls.prInnerFl != 1
                        || redefCls.packInnerFl != 1
                        || redefCls.pubInnerFl != 1) {
                    status = Consts.TEST_FAILED;
                    log.complain("TEST FAILED: unexpected values of inner fields of the local class"
                        + "\n\t\"" + this.toString() + "\":"
                        + "\n\t\tprInnerFl: got: " + redefCls.prInnerFl
                        + ", expected: 1"
                        + "\n\t\tpackInnerFl: got: " + redefCls.packInnerFl
                        + ", expected: 1"
                        + "\n\t\tpubInnerFl: got: " + redefCls.pubInnerFl
                        + ", expected: 1");
                }

                log.display(this.getName() + ": exiting");
            }
        }
    }

}
