/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ObjectReference.referringObjects.referringObjects003;

import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.jdi.HeapwalkingDebuggee;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 *  Test class handle request for start/stop test threads(threads are included in special thread group)
 */
public class referringObjects003a extends HeapwalkingDebuggee {

    static AtomicBoolean shouldStop = new AtomicBoolean(false);

    class TestThread implements Runnable {
        public void run() {
            while(!shouldStop.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // just wait for shouldStop
                }
            }
        }
    }

    private List<Thread> threads = new ArrayList<Thread>();

    private List<ReferringObject> referrers;

    public static final String COMMAND_START_THREADS = "startThreads";

    public static final String COMMAND_STOP_THREADS = "stopThreads";

    public static void main(String args[]) {
        new referringObjects003a().doTest(args);
    }

    private void addAllTypeReferernces(Object object) {
        for (String referenceType : ObjectInstancesManager.allReferenceTypes)
            referrers.add(new ReferringObject(object, referenceType));
    }

    public void startThreads(int threadCount) {
        referrers = new ArrayList<ReferringObject>();

        ThreadGroup threadGroup = new ThreadGroup("Test thread group");
        addAllTypeReferernces(threadGroup);

        for (int i = 0; i < threadCount; i++) {
            Thread testThread = new Thread(threadGroup, new TestThread(), "Test thread " + i);
            threads.add(testThread);
            addAllTypeReferernces(testThread);
            testThread.start();
        }
    }

    public void stopThreads() {
        shouldStop.set(true);

        for (Thread testThread : threads) {
            try {
                testThread.join();
            } catch (InterruptedException e) {
                log.display("Main thread was unexpected interrupted");
                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
            }
        }

        for (ReferringObject referringObject : referrers)
            referringObject.delete();

        threads = null;
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
            tokenizer.whitespaceChars(':', ':');

            if (command.startsWith(COMMAND_START_THREADS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new IllegalArgumentException("Invalid command format");

                int threadCount = (int) tokenizer.nval;

                startThreads(threadCount);

                return true;
            } else if (command.equals(COMMAND_STOP_THREADS)) {
                stopThreads();

                return true;
            }

        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }
}
