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

package nsk.jvmti.scenarios.allocation.AP04;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap04t003 extends DebugeeClass {

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap04t003().runThis(argv, out);
    }

    private static int OBJ_MAX_COUNT = 100000;
    private static ap04t003[] root;
    private static int modified;
    public  static volatile boolean iterationCompleted = true;

    private static native void setTag(Object target, long tag);

    public static native void runIterateOverHeap();
    public static native void runIterateOverInstancesOfClass();
    public static native void runIterateOverReachableObjects();
    public static native void runIterateOverObjectsReachableFromObject();

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    static long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        status = checkStatus(status);

        try {
            runCase("1", "thread1", new ap04t003HeapIterator());
            runCase("2", "thread2", new ap04t003AllReachachableObjectsIterator());
            runCase("3", "thread3", new ap04t003SomeReachachableObjectsIterator());
            runCase("4", "thread4", new ap04t003ClassIterator());

        } catch (OutOfMemoryError e) {
            log.display("Warning: OutOfMemoryError was thrown. Test exited");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        status = checkStatus(status);
        return status;
    }

    private void runCase ( String caseName,
                           String threadName,
                           ap04t003Iterator iterator ) {

        log.display("CASE #" + caseName + ":");
        log.display("Allocating objects...");
        root = new ap04t003[OBJ_MAX_COUNT];
        for (int i = 0; i < OBJ_MAX_COUNT; i++) {
            root[i] = new ap04t003();
            setTag(root[i], 1);
        }

        log.display("Start heap iteration thread");
        ap04t003Thread thread = startThread( threadName, iterator);

        log.display("Wait for thread to finish");
        joinThread(thread);
        log.display("Cleaning tags and references to objects...");
        for (int i = 0; i < OBJ_MAX_COUNT; i++) {
            if (root[i] != null) {
                setTag(root[i], 0);
            }
            root[i] = null;
        }
        System.gc();

        log.display("CASE #" + caseName + " finished.\n");
    }


    private static ap04t003Thread startThread(String name, ap04t003Iterator iterator) {
        ap04t003Thread thread = new ap04t003Thread(name, new Wicket(), iterator);
        thread.start();
        thread.getStartLock().waitFor();
        return thread;
    }

    private static void joinThread(Thread thread) {
        if (thread.isAlive()) {
            try {
                thread.join(timeout);
            } catch (InterruptedException e) {
                throw new Failure(e);
            }
        }
    }
}

/**************************************************************************/
interface ap04t003Iterator {
    public void runIteration();
}

class ap04t003HeapIterator implements ap04t003Iterator {
    public void runIteration() {
        ap04t003.runIterateOverHeap();
        ap04t003.iterationCompleted = true;
    }
}

class ap04t003ClassIterator implements ap04t003Iterator {
    public void runIteration() {
        ap04t003.runIterateOverInstancesOfClass();
        ap04t003.iterationCompleted = true;
    }
}

class ap04t003AllReachachableObjectsIterator implements ap04t003Iterator {
    public void runIteration() {
        ap04t003.runIterateOverReachableObjects();
        ap04t003.iterationCompleted = true;
    }
}

class ap04t003SomeReachachableObjectsIterator implements ap04t003Iterator {
    public void runIteration() {
        ap04t003.runIterateOverObjectsReachableFromObject();
        ap04t003.iterationCompleted = true;
    }
}

/**************************************************************************/
class ap04t003Thread extends Thread {
    String name;
    ap04t003Iterator iterator;
    Wicket startLock;
    Wicket endLock;

    public ap04t003Thread( String name,
                           Wicket startLock,
                           ap04t003Iterator iterator ) {

        if (name == null || name.length() == 0) {
            throw new Failure("Empty thread name.");
        }
        if (startLock == null) {
            throw new Failure("startLock is null.");
        }
        if (iterator == null) {
            throw new Failure("iterator is null.");
        }

        this.name       = name;
        this.startLock  = startLock;
        this.iterator   = iterator;
    }

    public Wicket getStartLock() {
        return startLock;
    }

    public void run() {
        ap04t003.log.display(name + " started.");
        ap04t003.iterationCompleted = false;
        startLock.unlock();

        iterator.runIteration();
        ap04t003.log.display(name + " finished.");
    }
}
