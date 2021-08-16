/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test probing content type simultaneously from multiple threads.
 * @requires os.family == "linux"
 * @build ParallelProbes SimpleFileTypeDetector
 * @run main/othervm ParallelProbes 10
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ParallelProbes {

    private static final int REPEATS = 1000;

    private int numThreads = 0;
    private ArrayList<Thread> threads;

    public ParallelProbes(int numThreads) {
        System.out.println("Using <" + numThreads + "> threads.");
        this.numThreads = numThreads;
        this.threads = new ArrayList<Thread>(numThreads);
    }

    private Path createTmpFile() throws IOException {
        Path dir = Paths.get(System.getProperty("test.dir", "."));
        final Path p = Files.createTempFile(dir, "prefix", ".json");
        Files.write(p, "{\"test\"}".getBytes());
        System.out.println("Write test file <" + p + ">");
        return p;
    }

    private Runnable createRunnable(final Path p) {
        Runnable r = new Runnable() {
            public void run() {
                for (int i = 0; i < REPEATS; i++) {
                    try {
                        System.out.println(Thread.currentThread().getName()
                            + " -> " + Files.probeContentType(p));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        };
        return r;
    }

    public void start() throws IOException {
        for (int i = 0; i < numThreads; i++) {
            final Path p = createTmpFile();
            Runnable r = createRunnable(p);
            Thread thread = new Thread(r, "thread-" + i);
            thread.start();
            threads.add(thread);
        }
    }

    public void join() {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // ignore it and proceed to the next one
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ParallelProbes probes =
            new ParallelProbes(args.length < 1 ? 1 : Integer.parseInt(args[0]));
        probes.start();
        probes.join();
    }
}
