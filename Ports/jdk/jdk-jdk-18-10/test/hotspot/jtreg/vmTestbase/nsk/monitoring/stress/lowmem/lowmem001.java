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
package nsk.monitoring.stress.lowmem;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryType;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import nsk.share.Log;
import nsk.share.TestFailure;
import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.array.ByteArrayProducer;
import nsk.share.gc.gp.classload.GeneratedClassProducer;
import nsk.monitoring.share.*;
import nsk.share.test.ExecutionController;

public class lowmem001 extends ThreadedGCTest {

    // The max heap usage after whih we free memory and restart
    static final int MAX_HEAP_USAGE = 70;
    // isOOM is used to stop allocation and free resources
    // immediately after first OOME
    // really it could be only if we didn't check usage in time
    static AtomicBoolean isOOM = new AtomicBoolean(false);
    static ArgumentHandler argHandler;
    MemoryMonitor monitor;

    public static void main(String[] args) {
        argHandler = new ArgumentHandler(args);
        GC.runTest(new lowmem001(), args);
    }

    @Override
    public void run() {
        Log log = new Log(System.out, true);
        // System.err is duplicated into buffer
        // it should be empty
        MyStream stream = new MyStream(System.err);
        System.setErr(new PrintStream(stream));

        monitor = Monitor.getMemoryMonitor(log, argHandler);
        try {
            monitor.enableMonitoring();
            monitor.updateThresholds();
            super.run();
            monitor.disableMonitoring();
        } catch (Exception e) {
            throw new TestFailure(e);
        }
        if (isOOM.get() == true) {
            log.display("The OOME happened during test");
            // We control memory at 70 %
            // each time when we want to eat 512 bytes
            // if we got OOME it is really ugly
            throw new TestFailure("OOME should not happened.");
        }
        if (!monitor.getPassedStatus()) {
            throw new TestFailure("MemoryMonitor fails. See log.");
        }
        if (!stream.isEmpty()) {
            String string = stream.getString();
            if (string.contains("java.lang.OutOfMemoryError")) {
                log.display("WARNING: The System.err contains OutOfMemory.");
                // the OOME is not error
                log.complain(string);
                return;
            }
            log.complain(string);
            throw new TestFailure("Error stream is not empty.");
        }

    }

    @Override
    protected Runnable createRunnable(int i) {
        String memory = argHandler.getTestedMemory();
        if (memory.equals(MemoryMonitor.HEAP_TYPE)) {
            return new HeapStresser();
        }
        if (memory.equals(MemoryMonitor.NONHEAP_TYPE)) {
            return new ClassStresser();
        }
        // mixed type
        return i % 2 == 0 ? new HeapStresser() : new ClassStresser();
    }

    /*
     * Simple ClassLoader is used for non-heap stressing
     * should be revised after permgen removal
     */
    class ClassStresser extends Thread {

        @Override
        public void run() {
            ExecutionController stresser = getExecutionController();
            GeneratedClassProducer gp = new GeneratedClassProducer();
            while (stresser.continueExecution()) {
                try {
                    gp.create(0);
                } catch (OutOfMemoryError e) {
                    // drop 'gc', reset Thresholds and start new iteration
                    monitor.resetThresholds(MemoryType.NON_HEAP);
                    return;
                }
            }
        }
    };

    class HeapStresser extends Thread {

        final long chunkSize = 512;
        List storage;
        GarbageProducer gp = new ByteArrayProducer();


        @Override
        public void run() {
            storage = new LinkedList();
            ExecutionController stresser = getExecutionController();
            MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

            while (stresser.continueExecution()) {
                try {
                   storage.add(gp.create(chunkSize + new Object().hashCode() % 31));
                   storage.add(gp.create(chunkSize));
                   storage.remove(0);
                    if (isOOM.get() == true || !stresser.continueExecution()) {
                        stresser.finish();
                        storage = null;
                        return;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    // If memory is low free resources and restart
                    if (bean.getHeapMemoryUsage().getUsed()
                            > bean.getHeapMemoryUsage().getMax() * MAX_HEAP_USAGE / 100) {
                        storage = new LinkedList();
                        monitor.resetThresholds(MemoryType.HEAP);
                    }
                } catch (OutOfMemoryError e) {
                    // Let finish, the managment/memorymonitor could be
                    // corrupted after OOME
                    storage = null;
                    isOOM.set(true);
                    stresser.finish();
                    return;
                }
            }
        }
    }

    static class MyStream extends OutputStream {

        PrintStream err;

        public MyStream(PrintStream err) {
            this.err = err;
        }
        private final static int SIZE = 100000;
        private char[] value = new char[SIZE];
        private int count = 0;

        // No additional memory allocation during write
        @Override
        public synchronized void write(int b) {
            if (count < SIZE) {
                value[count++] = (char) b;
            }
            try {
                err.write(b);
            } catch (OutOfMemoryError oome) {
                isOOM.set(true);
            }
        }

        public String getString() {
            return new String(value, 0, count);
        }

        public boolean isEmpty() {
            return count == 0;
        }
    }
}
