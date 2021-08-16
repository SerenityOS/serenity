/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8225690
 * @requires os.family != "windows"
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @run main ConcAttachTest
 */

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachNotSupportedException;

import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;

public class ConcAttachTest implements Runnable {

    private static final int NUM_CONC_REQUESTS = 100;

    private static final int THREAD_POOL_TIMEOUT_IN_SEC = 30;

    private static CountDownLatch latch;

    private static String strPID;

    // Attach to LingeredApp concurrently.
    public void run() {
        VirtualMachine vm = null;

        try {
            latch.countDown();
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            vm = VirtualMachine.attach(strPID);
        } catch (AttachNotSupportedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                vm.detach();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void checkAttachListenerThread() throws InterruptedException, IOException {
        JDKToolLauncher jcmd = JDKToolLauncher.createUsingTestJDK("jcmd");
        jcmd.addVMArgs(Utils.getTestJavaOpts());
        jcmd.addToolArg(strPID);
        jcmd.addToolArg("Thread.print");

        ProcessBuilder pb = new ProcessBuilder(jcmd.getCommand());
        Process jcmdProc = pb.start();

        OutputAnalyzer out = new OutputAnalyzer(jcmdProc);

        jcmdProc.waitFor();

        System.out.println(out.getStdout());
        System.err.println(out.getStderr());

        long numOfAttachListener = out.asLines()
                                      .stream()
                                      .filter(l -> l.contains("Attach Listener"))
                                      .count();

        Asserts.assertEquals(1L, numOfAttachListener, "AttachListener should exist only 1 thread.");
    }

    public static void main(String... args) throws Exception {
        LingeredApp app = null;
        latch = new CountDownLatch(NUM_CONC_REQUESTS);
        ExecutorService pool = Executors.newFixedThreadPool(NUM_CONC_REQUESTS);

        try {
            app = LingeredApp.startApp();
            strPID = Long.toString(app.getPid());

            for (int i = 0; i < NUM_CONC_REQUESTS; i++) {
                pool.submit(new ConcAttachTest());
            }

            pool.shutdown();
            pool.awaitTermination(THREAD_POOL_TIMEOUT_IN_SEC, TimeUnit.SECONDS);

            checkAttachListenerThread();
        } finally {
            LingeredApp.stopApp(app);
        }
    }

}
