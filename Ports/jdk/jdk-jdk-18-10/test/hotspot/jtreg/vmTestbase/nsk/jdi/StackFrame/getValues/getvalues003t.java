/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.StackFrame.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a debuggee class.
 */
public class getvalues003t {
    private Log log;
    private IOPipe pipe;
    private OtherThr auxThr;

    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new getvalues003t().runIt(args);
    }

    private int runIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);

        log = argHandler.createDebugeeLog();
        pipe = argHandler.createDebugeeIOPipe();

        Thread.currentThread().setName(getvalues003.DEBUGGEE_THRDNAMES[0]);
        startThread();

        // dummy local vars used by debugger for testing
        byte getvalues003tFindMe = 127;
        short shortVar = -32768;
        int intVar = 2147483647;
        long longVar = 9223372036854775807L;
        float floatVar = 5.1F;
        double doubleVar = 6.2D;
        char charVar = 'a';
        boolean booleanVar = true;
        String strVar =  "string var";

        // Now the debuggee is ready
        pipe.println(getvalues003.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(getvalues003.COMMAND_QUIT)) {
            killThread(argHandler.getWaitTime()*60000);
            log.complain("Debuggee: exiting due to the command "
                    + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // getvalues003.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        killThread(argHandler.getWaitTime()*60000);
        if (!cmd.equals(getvalues003.COMMAND_QUIT)) {
            log.complain("TEST BUG: unknown debugger command: "
                + cmd);
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }

    private void startThread() {
        Object readyObj = new Object();

        auxThr = new OtherThr(readyObj,
            getvalues003.DEBUGGEE_THRDNAMES[1]);
        auxThr.setDaemon(true);

        log.display("Debuggee: starting thread \""
            + auxThr.getName() + "\" ...");
        synchronized(readyObj) {
            auxThr.start();
            try {
                readyObj.wait(); // wait for the thread's readiness
            } catch (InterruptedException e) {
                log.complain("TEST FAILURE: Debuggee: waiting for the thread "
                    + auxThr + " start: caught " + e);
                pipe.println("failed");
                System.exit(Consts.JCK_STATUS_BASE +
                    Consts.TEST_FAILED);
            }
        }
        log.display("Debuggee: the thread \""
            + auxThr.getName() + "\" started");
    }

    private void killThread(int waitTime) {
        auxThr.doExit = true;
        try {
            auxThr.join(waitTime);
            log.display("Debuggee: thread \""
                + auxThr.getName() + "\" done");
        } catch (InterruptedException e) {
            log.complain("TEST FAILURE: Debuggee: joining the thread \""
                + auxThr.getName() + "\": caught " + e);
        }
    }

   /**
    * This is an auxiliary thread class used to check
    * an IllegalArgumentException in debugger.
    */
    class OtherThr extends Thread {
        volatile boolean doExit = false;
        private Object readyObj;

        OtherThr(Object readyObj, String name) {
            super(name);
            this.readyObj = readyObj;
        }

        public void run() {
            // dummy local vars used by debugger for testing
            byte getvalues003tFindMe = 127;
            short shortVar = -32768;
            int intVar = 2147483647;
            long longVar = 9223372036854775807L;
            float floatVar = 5.1F;
            double doubleVar = 6.2D;
            char charVar = 'a';
            boolean booleanVar = true;
            String strVar =  "string var";

            Thread thr = Thread.currentThread();

            synchronized(readyObj) {
                readyObj.notify(); // notify the main thread
            }
            log.display("Debuggee thread \""
                    + thr.getName() + "\": going to loop");
            while(!doExit) {
                int i = 0;
                i++; i--;

                // reliable analogue of Thread.yield()
                synchronized(this) {
                    try {
                        this.wait(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace(log.getOutStream());
                        log.complain("TEST FAILURE: Debuggee thread \""
                            + thr.getName()
                            + "\" interrupted while sleeping:\n\t" + e);
                        break;
                    }
                }
            }
            log.display("Debuggee thread \""
                + thr.getName() + "\" exiting ...");
        }
    }
/////////////////////////////////////////////////////////////////////////////

}
