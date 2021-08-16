/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8024833
 * @key intermittent
 * @summary Tests interruption of threads mapping sections of a file channel in
 *   an attempt to deadlock due to nesting of begin calls.
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.*;
import java.util.concurrent.Semaphore;
import static java.nio.file.StandardOpenOption.*;

public class InterruptMapDeadlock {

    static class Mapper extends Thread {
        final FileChannel fc;
        final Semaphore gate;
        volatile Exception exception;

        Mapper(FileChannel fc, Semaphore gate) {
            this.fc = fc;
            this.gate = gate;
        }

        @Override
        public void run() {
            try {
                gate.acquireUninterruptibly();
                fc.map(MapMode.READ_ONLY, 0, 1);
                throw new Exception("Map succeeded");
            } catch (IOException x) {
                System.out.println(x.getClass() + " (expected)");
            } catch (Exception unexpected) {
                this.exception = unexpected;
            }
        }

        Exception exception() {
            return exception;
        }

        static Mapper startMapper(FileChannel fc, Semaphore gate) {
            Mapper r = new Mapper(fc, gate);
            r.setDaemon(true);
            r.start();
            return r;
        }
    }

    static class Interruptor extends Thread {

        final Mapper[] mappers;
        final Semaphore gate;

        Interruptor(Mapper[] mappers, Semaphore gate) {
            this.mappers = mappers;
            this.gate = gate;
        }

        public void run() {
            gate.release(mappers.length);
            for (Mapper m : mappers) {
                m.interrupt();
            }
        }
    }
    // the number of mapper threads to start
    private static final int MAPPER_COUNT = 4;

    public static void main(String[] args) throws Exception {
        Path file = Paths.get("data.txt");
        FileChannel.open(file, CREATE, TRUNCATE_EXISTING, WRITE).close();

        Mapper[] mappers = new Mapper[MAPPER_COUNT];

        for (int i=1; i<=20; i++) {
            System.out.format("Iteration: %s%n", i);

            FileChannel fc = FileChannel.open(file);
            boolean failed = false;

            Semaphore gate = new Semaphore(0);
            // start mapper threads
            for (int j=0; j<MAPPER_COUNT; j++) {
                mappers[j] = Mapper.startMapper(fc, gate);
            }

            // interrupt and wait for the mappers to terminate
            Interruptor interruptor = new Interruptor(mappers, gate);
            interruptor.start();
            try {
                interruptor.join(10000);
                if (interruptor.isAlive()) {
                    System.err.println("Interruptor thread did not terminate:");
                    Throwable t = new Exception("Stack trace");
                    t.setStackTrace(interruptor.getStackTrace());
                    t.printStackTrace();
                    failed = true;
                }
            } catch (InterruptedException x) {
                System.err.println("Main thread was interrupted");
                failed = true;
            }

            for (Mapper m: mappers) {
                try {
                    m.join(10000);
                    Exception e = m.exception();
                    if (e != null) {
                        System.err.println("Mapper thread failed with: " + e);
                        failed = true;
                    } else if (m.isAlive()) {
                        System.err.println("Mapper thread did not terminate:");
                        Throwable t = new Exception("Stack trace");
                        t.setStackTrace(m.getStackTrace());
                        t.printStackTrace();
                        failed = true;
                    }
                } catch (InterruptedException x) {
                    System.err.println("Main thread was interrupted");
                    failed = true;
                }
            }

            if (failed)
                throw new RuntimeException("Test failed - see log for details");
            else
                fc.close();
        }
    }
}
