/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.lock;

import nsk.share.runner.*;
import nsk.share.gc.*;
import nsk.share.gc.gp.*;
import nsk.share.gc.lock.*;
import nsk.share.test.ExecutionController;

/**
 * Test how GC is affected by locking.
 *
 * A number of threads is started. Each one locks and eats memory.
 */
public class LockerTest extends ThreadedGCTest implements GarbageProducerAware, GarbageProducer1Aware, LockersAware {

    private GarbageProducer garbageProducer;
    private GarbageProducer garbageProducer1;
    private Lockers lockers;
    private long objectSize = 1000;

    private class Worker implements Runnable {

        byte[] rezerve = new byte[1024 * 1024];
        private Locker locker = lockers.createLocker(garbageProducer1.create(objectSize));

        public Worker() {
            locker.enable();
        }

        public void run() {
            locker.lock();
            GarbageUtils.eatMemory(getExecutionController(), garbageProducer);
            locker.unlock();
        }
    }

    protected Runnable createRunnable(int i) {
        return new Worker();
    }

    public void setGarbageProducer(GarbageProducer garbageProducer) {
        this.garbageProducer = garbageProducer;
    }

    public void setGarbageProducer1(GarbageProducer garbageProducer1) {
        this.garbageProducer1 = garbageProducer1;
    }

    public void setLockers(Lockers lockers) {
        this.lockers = lockers;
    }

    public static void main(String[] args) {
        RunParams.getInstance().setRunMemDiagThread(false);
        GC.runTest(new LockerTest(), args);
    }
}
