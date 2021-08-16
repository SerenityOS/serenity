/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.*;
import java.util.stream.*;

/*
 * @test
 * @bug 8242263
 * @summary Exercise DiagnoseSyncOnValueBasedClasses diagnostic flag
 * @requires vm.flagless
 * @library /test/lib
 * @run driver/timeout=180000 SyncOnValueBasedClassTest
 */

public class SyncOnValueBasedClassTest {
    static final int LOOP_COUNT = 3000;
    static final int THREAD_COUNT = 2;
    static String[] fatalTests[];
    static String[] logTests[];
    static List<Object> testObjects = new ArrayList<Object>();

    private static final String[] specificFlags[] = {
        {"-Xint"},
        {"-Xcomp", "-XX:TieredStopAtLevel=1"},
        {"-Xcomp", "-XX:-TieredCompilation"},
    };

    private static void initTestObjects() {
        testObjects.add(Character.valueOf('H'));
        testObjects.add(Boolean.valueOf(true));
        testObjects.add(Byte.valueOf((byte)0x40));
        testObjects.add(Short.valueOf((short)0x4000));
        testObjects.add(Integer.valueOf(0x40000000));
        testObjects.add(Long.valueOf(0x4000000000000000L));
        testObjects.add(Float.valueOf(1.20f));
        testObjects.add(Double.valueOf(1.2345));
    }

    private static void generateTests() {
        initTestObjects();
        String[] commonFatalTestsFlags = {"-XX:+UnlockDiagnosticVMOptions", "-XX:-CreateCoredumpOnCrash", "-XX:DiagnoseSyncOnValueBasedClasses=1"};
        fatalTests = new String[specificFlags.length * testObjects.size()][];
        for (int i = 0; i < specificFlags.length; i++) {
            for (int j = 0; j < testObjects.size(); j++) {
                int index = i * testObjects.size() + j;
                fatalTests[index] = Stream.of(commonFatalTestsFlags, specificFlags[i], new String[] {"SyncOnValueBasedClassTest$FatalTest", Integer.toString(j)})
                                          .flatMap(Stream::of)
                                          .toArray(String[]::new);
            }
        }
        String[] commonLogTestsFlags = {"-XX:+UnlockDiagnosticVMOptions", "-XX:DiagnoseSyncOnValueBasedClasses=2"};
        logTests = new String[specificFlags.length][];
        for (int i = 0; i < specificFlags.length; i++) {
            logTests[i] = Stream.of(commonLogTestsFlags, specificFlags[i], new String[] {"SyncOnValueBasedClassTest$LogTest"})
                                .flatMap(Stream::of)
                                .toArray(String[]::new);
        }
    }

    public static void main(String[] args) throws Exception {
        generateTests();
        for (int i = 0; i < fatalTests.length; i++) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(fatalTests[i]);
            OutputAnalyzer output = ProcessTools.executeProcess(pb);
            output.shouldContain("fatal error: Synchronizing on object");
            output.shouldNotContain("synchronization on value based class did not fail");
            output.shouldNotHaveExitValue(0);
        }
        for (int i = 0; i < logTests.length; i++) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(logTests[i]);
            OutputAnalyzer output = ProcessTools.executeProcess(pb);
            output.shouldHaveExitValue(0);
            checkOutput(output);
        }
    }

    private static void checkOutput(OutputAnalyzer output) {
        String out = output.getOutput();
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Character.*"));
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Boolean.*"));
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Byte.*"));
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Short.*"));
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Integer.*"));
        assertTrue(out.matches("(?s).*Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Long.*"));
        String[] res = out.split("Synchronizing on object 0[xX][0-9a-fA-F]+ of klass java\\.lang\\.Float\\R");
        assertTrue(res.length - 1 == (LOOP_COUNT * THREAD_COUNT + 1), res.length - 1);
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new RuntimeException("No synchronization matches");
        }
    }

    private static void assertTrue(boolean condition, int count) {
        if (!condition) {
            throw new RuntimeException("Synchronization count was " + count);
        }
    }

    static class FatalTest {
        public static void main(String[] args) throws Exception {
            initTestObjects();
            synchronized (testObjects.get(Integer.valueOf(args[0]))) {
                throw new RuntimeException("synchronization on value based class did not fail");
            }
        }
    }

    static class LogTest implements Runnable {
        private static long sharedCounter = 0L;
        private static Float sharedLock1 = 0.0f;

        public static void main(String[] args) throws Exception {
            initTestObjects();
            for (Object obj : testObjects) {
                synchronized (obj) {
                    sharedCounter++;
                }
            }

            LogTest test = new LogTest();
            Thread[] threads = new Thread[THREAD_COUNT];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(test);
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < LOOP_COUNT; i++) {
                synchronized (sharedLock1) {
                    sharedCounter++;
                }
            }
        }
    }
}
