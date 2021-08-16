/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This test checks that <code>if any redefined methods have
 * active stack frames, those active frames continue to run
 * the bytecodes of the original method</code>.<br>
 * It creates an instance of tested class <code>redefclass002r</code>
 * and invokes <code>redefclass002r.activeMethod()</code>.
 * The <code>activeMethod()</code> method is an active frame
 * mentioned above.<br>
 * Then the test invokes native function <code>makeRedefinition()</code>
 * which makes class file redifinition of the loaded class
 * <code>redefclass002r</code>.<br>
 * Bytes of new version of the class <code>redefclass002r</code>
 * are taken from the <i>./newclass</i> directory.<br>
 * Finally, the test checks that the class <code>redefclass002r</code>
 * has been really redefined, and at the same time
 * the <code>activeMethod()</code> method stays original.
 */
public class redefclass002 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    static String fileDir = ".";

    private PrintStream out;
    private PipedInputStream pipeIn;
    private PipedOutputStream pipeOut;
    private redefclass002r redefClsObj;
    private runRedefCls runRedefClsThr;
    private volatile Object readyObj = new Object();

    static {
        try {
            System.loadLibrary("redefclass002");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass002 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int vrb, Class redefClass,
        byte[] classBytes);
    native static int suspThread(int vrb, runRedefCls runRedefClsThr);
    native static int resThread(int vrb, runRedefCls runRedefClsThr);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass002().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        File newRedefClassFile = null;
        byte[] redefClassBytes;
        int retValue = 0;

        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            if (token.equals("-v")) // verbose mode
                DEBUG_MODE = true;
            else
                fileDir = token;
        }

        pipeIn = new PipedInputStream();
        pipeOut = new PipedOutputStream();
        redefClsObj = new redefclass002r();
        runRedefClsThr = new runRedefCls(pipeIn, pipeOut,
            out, DEBUG_MODE, redefClsObj);
        synchronized(readyObj) {
            runRedefClsThr.start(); // start a separate thread
            try {
                if (DEBUG_MODE)
                    out.println("Waiting until a child thread really starts ...");
                readyObj.wait(); // wait for the thread's readiness
            } catch (InterruptedException e) {
                out.println("TEST FAILED: interrupted while waiting for readiness\n"
                    + "\tof the child thread " + runRedefClsThr.toString()
                    + ": caught " + e);
                shutdown(true);
                return FAILED;
            }
        }
        if (DEBUG_MODE)
            out.println("The child thread successfully started");

        if ((retValue=redefClsObj.checkIt(out, DEBUG_MODE)) == 19) {
            if (DEBUG_MODE)
                out.println("The method checkIt() of OLD redefclass002r was successfully invoked");
        } else {
            out.println("TEST: failed to invoke method redefclass002r.checkIt()");
            shutdown(true);
            return FAILED;
        }

// try to redefine class redefclass002r
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass002r.class.getName().replace('.', File.separatorChar) +
            ".class";
        if (DEBUG_MODE)
            out.println("Trying to redefine class from the file: " + fileName);
        try {
            FileInputStream in = new FileInputStream(fileName);
            redefClassBytes = new byte[in.available()];
            in.read(redefClassBytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

        if (DEBUG_MODE)
            retValue=suspThread(1, runRedefClsThr);
        else
            retValue=suspThread(0, runRedefClsThr);
        if (retValue != PASSED) {
            out.println("TEST: failed to suspend a child thread");
            shutdown(true);
            return FAILED;
        }

// make real redefinition
        if (DEBUG_MODE)
            retValue=makeRedefinition(1, redefClsObj.getClass(),
                redefClassBytes);
        else
            retValue=makeRedefinition(0, redefClsObj.getClass(),
                redefClassBytes);
        if (retValue != PASSED) {
            out.println("TEST: failed to redefine the class");
            resThread(0, runRedefClsThr);
            shutdown(true);
            return FAILED;
        }

        if (DEBUG_MODE)
            retValue=resThread(1, runRedefClsThr);
        else
            retValue=resThread(0, runRedefClsThr);
        if (retValue != PASSED) {
            out.println("TEST: failed to resume a child thread");
            shutdown(true);
            return FAILED;
        }

        if ((retValue=redefClsObj.checkIt(out, DEBUG_MODE)) == 73) {
            if (DEBUG_MODE)
                out.println("The method checkIt() of NEW redefclass002r was successfully invoked");
        } else {
            if (retValue == 19)
                out.println("TEST FAILED: the method redefclass002r.checkIt() is still old");
            else
                out.println("TEST: failed to call method redefclass002r.checkIt()");
            shutdown(true);
            return FAILED;
        }

        int _byte = -1;
        try {
            pipeOut.write(123);
            pipeOut.flush();
            if (DEBUG_MODE)
                out.println("Checking the redefined class ...");
            _byte = pipeIn.read();
            shutdown(true);
        } catch (IOException e) {
            out.println("TEST: failed to work with the pipe: " + e);
            return FAILED;
        }
        if (_byte == 234) {
            if (DEBUG_MODE)
                out.println("The answer \"" + _byte
                   + "\" from the executing method activeMethod()\n\tof the OLD redefclass002r was read as expected");
            return PASSED;
        } else {
            out.println("The answer \"" + _byte
                + "\" from the executing method activeMethod()\n\tof the NEW redefined redefclass002r was read\n"
                + "TEST FAILED: an active frame was replaced");
            return FAILED;
        }
    }

    private void shutdown(boolean full) {
        try {
           pipeIn.close();
           pipeOut.close();
           if (full)
               runRedefClsThr.join();
        } catch (Exception e) {
            out.println("TEST: failed to shutdown: caught " + e);
        }
    }

    class runRedefCls extends Thread {
        private PipedInputStream pipeIn;
        private PipedOutputStream pipeOut;
        private PrintStream out;
        private boolean DEBUG_MODE;
        private redefclass002r redefClassObj;

        runRedefCls(PipedInputStream pipeIn, PipedOutputStream pipeOut,
            PrintStream out, boolean verbose, redefclass002r redefClassObj) {
            this.pipeIn = pipeIn;
            this.pipeOut = pipeOut;
            this.out = out;
            DEBUG_MODE = verbose;
            this.redefClassObj = redefClassObj;
        }

        public void run() {
            if (DEBUG_MODE) {
                out.println("runRedefCls: started");
                out.println("runRedefCls: invoking activeMethod() ...");
            }
            redefClassObj.activeMethod(pipeIn, pipeOut, out,
                readyObj, DEBUG_MODE);
        }
    }
}
