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

public class ap04t001 extends DebugeeClass {

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap04t001().runThis(argv, out);
    }

    private static int OBJ_MAX_COUNT = 100000;
    private static ap04t001[] root;
    public static volatile boolean iterationCompleted = true;
    public static volatile int cleanedCount = 0;
    private static Wicket secondaryLock = null;

    private static native void setTag(Object target, long tag);

    public static native void runIterateOverHeap();
    public static native void runIterateOverInstancesOfClass();
    public static native void runIterateOverReachableObjects();
    public static native void runIterateOverObjectsReachableFromObject();

    public static native void forceGC();

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
            runCase("1", "thread1", new ap04t001HeapIterator(), false);
            runCase("2", "thread2", new ap04t001AllReachachableObjectsIterator(), false);
            runCase("3", "thread3", new ap04t001SomeReachachableObjectsIterator(), true);
            runCase("4", "thread4", new ap04t001ClassIterator(), false);

        } catch (OutOfMemoryError e) {
            log.display("Warning: OutOfMemoryError was thrown. Test exited");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        status = checkStatus(status);
        return status;
    }

    private static void unlockSecondary() {
        if (secondaryLock != null)
            secondaryLock.unlock();
    }

    private static void runCase ( String caseName,
                           String threadName,
                           ap04t001Iterator iterator,
                           boolean useSecondaryLock) {

        log.display("CASE #" + caseName + ":");
        log.display("Allocating objects...");
        root = new ap04t001[OBJ_MAX_COUNT];
        for (int i = 0; i < OBJ_MAX_COUNT; i++) {
            root[i] = new ap04t001();
            setTag(root[i], i + 1 );
        }

        log.display("Start thread and making garbage collection");

        if (useSecondaryLock) secondaryLock = new Wicket();
        ap04t001Thread thread = startThread( threadName, iterator);
        if (useSecondaryLock) secondaryLock.waitFor();

        root = null;
        forceGC();
        log.display("All objects collected");

        log.display("Wait for thread to finish");
        joinThread(thread);

        log.display("CASE #" + caseName + " finished.\n");
    }


    private static ap04t001Thread startThread(String name, ap04t001Iterator iterator) {
        ap04t001Thread thread = new ap04t001Thread(name, new Wicket(), iterator);
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
interface ap04t001Iterator {
    public void runIteration();
}

class ap04t001HeapIterator implements ap04t001Iterator {
    public void runIteration() {
        ap04t001.runIterateOverHeap();
        ap04t001.iterationCompleted = true;
    }
}

class ap04t001ClassIterator implements ap04t001Iterator {
    public void runIteration() {
        ap04t001.runIterateOverInstancesOfClass();
        ap04t001.iterationCompleted = true;
    }
}

class ap04t001AllReachachableObjectsIterator implements ap04t001Iterator {
    public void runIteration() {
        ap04t001.runIterateOverReachableObjects();
        ap04t001.iterationCompleted = true;
    }
}

class ap04t001SomeReachachableObjectsIterator implements ap04t001Iterator {
    public void runIteration() {
        ap04t001.runIterateOverObjectsReachableFromObject();
        ap04t001.iterationCompleted = true;
    }
}

/**************************************************************************/
class ap04t001Thread extends Thread {
    String name;
    ap04t001Iterator iterator;
    Wicket startLock;
    Wicket endLock;

    public ap04t001Thread( String name,
                           Wicket startLock,
                           ap04t001Iterator iterator ) {

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
        ap04t001.log.display(name + " started.");
        ap04t001.iterationCompleted = false;
        startLock.unlock();

        iterator.runIteration();
        ap04t001.log.display(name + " finished.");
    }
}
