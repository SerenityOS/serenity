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

package nsk.jvmti.IsMethodObsolete;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class isobsolete001 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("isobsolete001");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new isobsolete001().runIt(argv, out);
    }

    /* =================================================================== */

    // names
    public static final String PACKAGE_NAME = "nsk.jvmti.IsMethodObsolete";
    public static final String REDEFINED_CLASS_NAME = PACKAGE_NAME + ".isobsolete001r";

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    public static isobsolete001r testedObject = null;
    public static byte classfileBytes[] = null;

    private static Object startingMonitor = new Object();
    private static volatile boolean shouldFinish = false;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        String args[] = argHandler.getArguments();
        if (args.length <= 0) {
            throw new Failure("Path for redefined class file not specified");
        }

        String location = args[0];
        String filename = location + File.separator + "newclass" + File.separator
                        + REDEFINED_CLASS_NAME.replace('.', File.separatorChar) + ".class";

        log.display("Reading bytes of redefined class from file: \n\t" + filename);
        classfileBytes = readBytes(filename);

        log.display("Creating tested object");
        testedObject = new isobsolete001r();

        log.display("Creating tested thread");
        isobsolete001Thread thread = new isobsolete001Thread("testedThread");

        log.display("Start tested thread and run tested methods");
        try {
            synchronized (startingMonitor) {
                thread.start();
                startingMonitor.wait();
            }

            log.display("Sync: tested methods are running on a thread stack");
            status = checkStatus(status);
        } catch (InterruptedException e) {
            throw new Failure("Interruption while waiting for thread to start: \n\t" + e);
        } finally {
            log.display("Let tested thread to finish");
            shouldFinish = true;
        }

        log.display("Waiting for thread to finish");
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new Failure("Interruption while waiting for thread to finish: \n\t" + e);
        }

        return status;
    }

    // read bytes of classfile to redefine
    private byte[] readBytes(String filename) {
        byte bytes[];

        try {
            FileInputStream in = new FileInputStream(filename);
            bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
        } catch (IOException e) {
            throw new Failure("Unexpected exception while reading class file: \n\t" + e);
        }

        return bytes;
    }

    // notify about starting and run until finished
    public static void running() {
        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // run continuously
        int n = 100;
        while (!shouldFinish) {
            int k = 0;
            for (int i = 0; i < n; i++) {
                k += n - i;
            }
            n--;
            if (n <= 0) n = 100;
        }
    }
}

/* =================================================================== */

// tested thread
class isobsolete001Thread extends Thread {

    public isobsolete001Thread(String name) {
        super(name);
    }

    public void run() {
        isobsolete001r.testedStaticMethod(100, isobsolete001.testedObject);
    }
}
