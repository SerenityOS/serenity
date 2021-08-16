/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6921885
 * @run main/othervm SiblingIOEHandle
 * @summary inherit IOE handles and MS CreateProcess limitations (kb315939)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class SiblingIOEHandle {
    private static enum APP {
        A, B, C;
    }

    private static File stopC = new File(".\\StopCs.txt");
    private static String SIGNAL = "B child reported.";
    private static String JAVA_EXE = System.getProperty("java.home")
            + File.separator + "bin"
            + File.separator + "java";

    private static String[] getCommandArray(String processName) {
        String[] cmdArray = {
                JAVA_EXE,
                "-cp",
                System.getProperty("java.class.path"),
                SiblingIOEHandle.class.getName(),
                processName
        };
        return cmdArray;
    }

    public static void main(String[] args) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        APP app = (args.length > 0) ? APP.valueOf(args[0]) : APP.A;
        switch (app) {
            case A:
                performA(true);
                performA(false);
                break;
            case B:
                performB();
                break;
            case C:
                performC();
                break;
        }
    }

    static boolean procClaunched = false;

    private static void waitAbit() {
        try {
            Thread.sleep(0);
        } catch (InterruptedException ex) {
            // that was long enough
        }
    }

    private static boolean waitBarrier(CyclicBarrier barrier) {
        while (true) try {
            barrier.await();
            return true;
        } catch (InterruptedException ex) {
            continue;
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static class ProcessC implements Runnable {
        private CyclicBarrier barrier;
        private Process processC;

        public ProcessC(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                if (waitBarrier(barrier)) {
                    waitAbit();
                    // Run process C next to B ASAP to make an attempt
                    // to capture the B-process IOE handles in C process.
                    ProcessBuilder builderC = new ProcessBuilder(
                            getCommandArray(APP.C.name()));
                    processC = builderC.start();
                    procClaunched = true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void waitFor() throws InterruptedException {
            processC.waitFor();
        }
    }

    private static void performA(boolean fileOut) {
        try {
            stopC.delete();
            ProcessBuilder builderB = new ProcessBuilder(
                    getCommandArray(APP.B.name()));

            File outB = null;
            if (fileOut) {
                outB = new File("outB.txt");
                builderB.redirectOutput(outB);
            }
            builderB.redirectErrorStream(true);

            final CyclicBarrier barrier = new CyclicBarrier(2);
            //Create process C in a new thread
            ProcessC processC = new ProcessC(barrier);
            Thread procCRunner = new Thread(processC);
            procCRunner.start();

            if (!waitBarrier(barrier)) {
                throw new RuntimeException("Catastrophe in process A! Synchronization failed.");
            }
            // Run process B first.
            Process processB = builderB.start();

            while (true) try {
                procCRunner.join();
                break;
            } catch (InterruptedException ex) {
                continue;
            }

            if (!procClaunched) {
                throw new RuntimeException("Catastrophe in process A! C was not launched.");
            }

            processB.getOutputStream().close();
            processB.getErrorStream().close();

            if (fileOut) {
                try {
                    processB.waitFor();
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Catastrophe in process B! B hung up.");
                }
                System.err.println("Trying to delete [outB.txt].");
                if (!outB.delete()) {
                    throw new RuntimeException("Greedy brother C deadlock! File share.");
                }
                System.err.println("Succeeded in delete [outB.txt].");
            } else {
                System.err.println("Read stream start.");
                boolean isSignalReceived = false;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                        processB.getInputStream(), "utf-8"))) {
                    String result;
                    while ((result = in.readLine()) != null) {
                        if (SIGNAL.equals(result)) {
                            isSignalReceived = true;
                        } else {
                            throw new RuntimeException("Catastrophe in process B! Bad output.");
                        }
                    }
                }
                if (!isSignalReceived) {
                    throw new RuntimeException("Signal from B was not received");
                }
                System.err.println("Read stream finished.");
            }
            // If JDK-6921885 is not fixed that point is unreachable.
            // Test timeout exception.

            // write signal file to stop C process.
            stopC.createNewFile();
            processC.waitFor();
        } catch (IOException ex) {
            throw new RuntimeException("Catastrophe in process A!", ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException("Process A was interrupted while waiting for C", ex);
        }
    }

    private static void performB() {
        System.out.println(SIGNAL);
    }

    private static void performC() {
        // If JDK-7147084 is not fixed the loop is 5min long.
        for (int i = 0; i < 5 * 60; ++i) {
            try {
                Thread.sleep(1000);
                // check for sucess
                if (stopC.exists())
                    break;
            } catch (InterruptedException ex) {
                // that is ok. Longer sleep - better effect.
            }
        }
    }
}
