/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessHandle;

import jdk.test.lib.util.FileUtils;

/*
 * @test
 * @bug 8239893
 * @summary Verify that handles for processes that terminate do not accumulate
 * @requires ((os.family == "windows") & (vm.compMode != "Xcomp"))
 * @library /test/lib
 * @run main/othervm/native -Xint CheckHandles
 */
public class CheckHandles {

    public static void main(String[] args) throws Exception {
        System.out.println("mypid: " + ProcessHandle.current().pid());

        // Warmup the process launch mechanism and vm to stabilize the number of handles in use
        int MAX_WARMUP = 20;
        long prevCount = FileUtils.getProcessHandleCount();
        for (int i = 0; i < MAX_WARMUP; i++) {
            oneProcess();
            System.gc();        // an opportunity to close unreferenced handles
            sleep(10);

            long count = FileUtils.getProcessHandleCount();
            if (count < 0)
                throw new AssertionError("getProcessHandleCount failed");
            System.out.println("warmup handle delta: " + (count - prevCount));
            prevCount = count;
        }
        System.out.println("Warmup done");
        System.out.println();

        prevCount = FileUtils.getProcessHandleCount();
        long startHandles = prevCount;
        long maxHandles = startHandles;
        int MAX_SPAWN = 50;
        for (int i = 0; i < MAX_SPAWN; i++) {
            oneProcess();
            System.gc();        // an opportunity to close unreferenced handles
            sleep(10);

            long count = FileUtils.getProcessHandleCount();
            if (count < 0)
                throw new AssertionError("getProcessHandleCount failed");
            System.out.println("handle delta: " + (count - prevCount));
            prevCount = count;
            maxHandles = Math.max(maxHandles, count);
        }

        System.out.println("Processes started: " + MAX_SPAWN);
        System.out.println("startHandles: " + startHandles);
        System.out.println("maxHandles:   " + maxHandles);

        final float ERROR_PERCENT = 10.0f;   // allowable extra handles
        final long ERROR_THRESHOLD = startHandles + Math.round(startHandles * ERROR_PERCENT / 100.0f);
        if (maxHandles >= ERROR_THRESHOLD) {
            throw new AssertionError("Handle use increased by more than " + ERROR_PERCENT + " percent.");
        }
    }

    /**
     * Start a single process and consume its output.
     */
    private static void oneProcess() {
        try {

            Process testProcess = new ProcessBuilder("cmd", "/c", "dir").start();

            Thread outputConsumer = new Thread(() -> consumeStream(testProcess.getInputStream()));
            outputConsumer.setDaemon(true);
            outputConsumer.start();
            Thread errorConsumer = new Thread(() -> consumeStream(testProcess.getErrorStream()));
            errorConsumer.setDaemon(true);
            errorConsumer.start();

            testProcess.waitFor();
            outputConsumer.join();
            errorConsumer.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception", e);
        }
    }

    private static void consumeStream(InputStream inputStream) {
        BufferedReader reader = null;
        try {
            int lines = 0;
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            // ignore
        }
    }
}
