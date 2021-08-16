/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.InputStreamReader;

import static jdk.test.lib.Asserts.assertGreaterThan;
import jdk.test.lib.process.ProcessTools;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import sun.tools.attach.HotSpotVirtualMachine;

/*
 * @test
 * @bug 6942989
 * @summary Check for WeakReference leak in Logger and anonymous Logger objects
 * @library /test/lib
 * @modules jdk.attach/sun.tools.attach
 *          java.logging
 * @build jdk.test.lib.process.ProcessTools
 * @run main/othervm -Djdk.attach.allowAttachSelf TestLoggerWeakRefLeak Logger
 * @run main/othervm -Djdk.attach.allowAttachSelf TestLoggerWeakRefLeak AnonymousLogger
 */
public class TestLoggerWeakRefLeak {

    private static final String TARGET_CLASS = "java.lang.ref.WeakReference";
    private static final int INSTANCE_COUNT = 100;
    private static int loggerCount = 0;

    public static void main(String[] args) throws Exception {
        if (args[0].equals("AnonymousLogger")) {
            System.out.println("Test for WeakReference leak in AnonymousLogger object");
            testIfLeaking(TestLoggerWeakRefLeak::callAnonymousLogger);
        } else {
            System.out.println("Test for WeakReference leak in Logger object");
            testIfLeaking(TestLoggerWeakRefLeak::callLogger);
        }
    }

    /**
     * For these particular WeakReference leaks, the count was always observed
     * to be increasing so if decreasing or the same count is observed,
     * then there is no leak.
     */
    private static void testIfLeaking(Runnable callLogger) throws Exception {
        long count = 0;
        int instanceCount = 0;
        int previousInstanceCount = 0;
        int increasingCount = 0;
        int decreasingCount = 0;

        while (true) {
            callLogger.run();
            count += INSTANCE_COUNT;

            if ((count % 1000) == 0) {
                System.out.println("call count = " + count);

                instanceCount = getInstanceCountFromHeapHisto();
                if (instanceCount > previousInstanceCount) {
                    increasingCount++;
                } else {
                    decreasingCount++;
                    System.out.println("increasing count: " + increasingCount);
                    System.out.println("decreasing or the same count: " + decreasingCount);
                    System.out.println("Test passed: decreasing or the same instance count is observed");
                    break;
                }
                previousInstanceCount = instanceCount;
            }

            delayExecution();
        }
    }

    /**
     * This Logger call is leaking two different WeakReferences:
     * - one in LogManager.LogNode
     * - one in Logger.kids
     */
    private static void callLogger() {
        for (int i = 0; i < INSTANCE_COUNT; i++) {
            java.util.logging.Logger.getLogger("logger-" + loggerCount);
            if (++loggerCount >= 25000) {
                // Limit the Logger namespace used by the test so the weak refs
                // in LogManager.loggers that are being properly managed
                // don't skew the counts by too much.
                loggerCount = 0;
            }
        }
    }

    /**
     * This Logger call is leaking a WeakReference in Logger.kids
     */
    private static void callAnonymousLogger() {
        for (int i = 0; i < INSTANCE_COUNT; i++) {
            java.util.logging.Logger.getAnonymousLogger();
        }
    }

    /**
     * 'vm.heapHisto("-live")' will request a full GC
     */
    private static int getInstanceCountFromHeapHisto() throws AttachNotSupportedException, Exception {
        int instanceCount = 0;

        HotSpotVirtualMachine vm = (HotSpotVirtualMachine) VirtualMachine
                .attach(Long.toString(ProcessTools.getProcessId()));
        try {
            try (InputStream heapHistoStream = vm.heapHisto("-live");
                    BufferedReader in = new BufferedReader(new InputStreamReader(heapHistoStream))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains(TARGET_CLASS)) {
                        instanceCount = Integer.parseInt(inputLine
                                .split("[ ]+")[2]);
                        System.out.println("instance count: " + instanceCount);
                        break;
                    }
                }
            }
        } finally {
            vm.detach();
        }

        assertGreaterThan(instanceCount, 0, "No instances of " + TARGET_CLASS + " are found");

        return instanceCount;
    }

    /**
     * Delay for 1/10 of a second to avoid CPU saturation
     */
    private static void delayExecution() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            // Ignore any exceptions
        }
    }

}
