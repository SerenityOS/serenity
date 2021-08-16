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

package nsk.jvmti.scenarios.hotswap.HS201;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * The test implements the JVMTI <i>hotswap</i> scenario <code>HS201</code>:
 * <ol><li> Enable events Exception, MethodExit, FramePop.</li>
 *     <li> Call method a() which calls method b().</li>
 *     <li> Throw an uncaught exception in the method b().</li>
 *     <li> Redefine a class with the changed methods a() and b() within
 *         triggered callbacks Exception, MethodExit, FramePop caused by
 *         the uncaught exception. New version contains the changed methods
 *         a() and b(). Both methods should stay obsolete during their
 *         execution.</li>
 * </ol>
 * The classfile redefinition and all checks are performed during the
 * appropriate triggered callbacks. The methods are checked for obsolescence
 * by analyzing length of the byte code array that implement the redefined
 * method. The reason is that method ID from input parameters will be
 * reasigned to the new method during execution of the very first callback but
 * it will be already referred to the old method in the others subsequent
 * callbacks in accordance with the JVMTI spec:
 * <blockquote><i>An original method version which is not equivalent to
 * the new method version is called obsolete and is assigned a new method
 * ID; the original method ID now refers to the new method version</i></blockquote>
 */
public class hs201t003 extends DebugeeClass {
    final static String REDEF_CLS_SIGNATURE =
        "Lnsk/jvmti/scenarios/hotswap/HS201/hs201t003r;";

    static int status = Consts.TEST_PASSED;

    static Log log = null;

    // path to the directory with redefining class files
    static String clfBasePath = null;

    // refer to a tested class to be redefined
    hs201t003r redefCls;

    native static void storeClassBytes(byte[] classBytes);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new hs201t003().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        int iter;

        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);

        try {
            // directory to search a redefining class
            clfBasePath = args[0];
        } catch(Exception e) {
            throw new Failure("TEST BUG: Wrong test parameters, caught: "
                + e);
        }

        storeClassBytes(loadFromClassFile(REDEF_CLS_SIGNATURE));

        // testing sync
        log.display("waiting for the agent start ...\n");
        status = checkStatus(status);

        log.display("starting an auxiliary thread ...\n");
        RedefClassWrapper redefClsWrapper = new RedefClassWrapper();
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

        log.display("waiting for auxiliary thread ...\n");
        redefClsWrapper.stopMe = true;
        try {
            redefClsWrapper.join();
        } catch (InterruptedException e) {
            redefClsWrapper.interrupt();
            throw new Failure("TEST FAILURE: waiting for auxiliary thread death, caught: "
                + e);
        }

        // verify classfile redefinition
        try {
            redefCls.entryMethod();
        } catch(Exception e) {
            ; // ignoring
        }
        if (redefCls.entryMethodFld == 0 || redefCls.entryMethod2Fld == 0) {
            status = Consts.TEST_FAILED;
            log.complain("TEST FAILED: tested class has not been redefined:\n"
                + "\tthe class fields still have old values: "
                + redefCls.entryMethodFld + " " + redefCls.entryMethodFld
                + "\n\texpected values: 1 1");
        }
        else
            log.display("CHECK PASSED: tested class has been redefined");

        return status;
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
     * Class executing a class to be redefined.
     */
    class RedefClassWrapper extends Thread {
        boolean stopMe = false;

        RedefClassWrapper() {
            super("RedefClassWrapper");
        }

        public void run() {
            log.display(this.getName() + ": started");

            redefCls = new hs201t003r();

            synchronized(this) {
                this.notify(); // notify the main thread

                while(!stopMe) {
                    try {
                        redefCls.entryMethod();
                    } catch(Exception e) {
                        break; // ignoring
                    }

                    // get the main thread chance to obtain CPU
                    try {
                        this.wait(100);
                    } catch(Exception e) {}
                }

                log.display(this.getName() + ": exiting");
            }
        }
    }
    /*************************************************************/

}

/**
 * A class to be redefined in an agent.
 */
class hs201t003r {
    // dummy fields used only to verify class file redefinition
    public static int entryMethodFld = 0;
    public static int entryMethod2Fld = 0;

    void entryMethod() throws Exception {
        entryMethod2();
    }

    void entryMethod2() throws Exception {
        String str = "Tasya";
        int i = 10;
        i +=10;
        i--;
        String str2 = "Hi " + str;

        throw new Exception("Test exception");
    }
}
