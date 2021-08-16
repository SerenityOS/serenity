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

package gc.stress.gclocker;

/*
 * @test TestExcessGCLockerCollections
 * @bug 8048556
 * @summary Check for GC Locker initiated GCs that immediately follow another
 * GC and so have very little needing to be collected.
 * @requires vm.gc != "Z"
 * @requires vm.gc != "Epsilon"
 * @requires vm.gc != "Shenandoah"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver/timeout=1000 gc.stress.gclocker.TestExcessGCLockerCollections 300 4 2
 */

import java.util.HashMap;
import java.util.Map;

import java.util.zip.Deflater;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

class TestExcessGCLockerCollectionsAux {
    static private final int LARGE_MAP_SIZE = 64 * 1024;

    static private final int MAP_ARRAY_LENGTH = 4;
    static private final int MAP_SIZE = 1024;

    static private final int BYTE_ARRAY_LENGTH = 128 * 1024;

    static private void println(String str) { System.out.println(str); }

    static private volatile boolean keepRunning = true;

    static Map<Integer,String> populateMap(int size) {
        Map<Integer,String> map = new HashMap<Integer,String>();
        for (int i = 0; i < size; i += 1) {
            Integer keyInt = Integer.valueOf(i);
            String valStr = "value is [" + i + "]";
            map.put(keyInt,valStr);
        }
        return map;
    }

    static private class AllocatingWorker implements Runnable {
        private final Object[] array = new Object[MAP_ARRAY_LENGTH];
        private int arrayIndex = 0;

        private void doStep() {
            Map<Integer,String> map = populateMap(MAP_SIZE);
            array[arrayIndex] = map;
            arrayIndex = (arrayIndex + 1) % MAP_ARRAY_LENGTH;
        }

        public void run() {
            while (keepRunning) {
                doStep();
            }
        }
    }

    static private class JNICriticalWorker implements Runnable {
        private int count;

        private void doStep() {
            byte[] inputArray = new byte[BYTE_ARRAY_LENGTH];
            for (int i = 0; i < inputArray.length; i += 1) {
                inputArray[i] = (byte) (count + i);
            }

            Deflater deflater = new Deflater();
            deflater.setInput(inputArray);
            deflater.finish();

            byte[] outputArray = new byte[2 * inputArray.length];
            deflater.deflate(outputArray);

            count += 1;
        }

        public void run() {
            while (keepRunning) {
                doStep();
            }
        }
    }

    static public Map<Integer,String> largeMap;

    static public void main(String args[]) {
        long durationSec = Long.parseLong(args[0]);
        int allocThreadNum = Integer.parseInt(args[1]);
        int jniCriticalThreadNum = Integer.parseInt(args[2]);

        println("Running for " + durationSec + " secs");

        largeMap = populateMap(LARGE_MAP_SIZE);

        println("Starting " + allocThreadNum + " allocating threads");
        for (int i = 0; i < allocThreadNum; i += 1) {
            new Thread(new AllocatingWorker()).start();
        }

        println("Starting " + jniCriticalThreadNum + " jni critical threads");
        for (int i = 0; i < jniCriticalThreadNum; i += 1) {
            new Thread(new JNICriticalWorker()).start();
        }

        long durationMS = (long) (1000 * durationSec);
        long start = System.currentTimeMillis();
        long now = start;
        long soFar = now - start;
        while (soFar < durationMS) {
            try {
                Thread.sleep(durationMS - soFar);
            } catch (Exception e) {
            }
            now = System.currentTimeMillis();
            soFar = now - start;
        }
        println("Done.");
        keepRunning = false;
    }
}

public class TestExcessGCLockerCollections {
    private static final String locker =
        "\\[gc\\s*\\] .* \\(GCLocker Initiated GC\\)";
    private static final String ANY_LOCKER = locker + " [1-9][0-9]*M";
    private static final String BAD_LOCKER = locker + " [1-9][0-9]?M";

    private static final String[] COMMON_OPTIONS = new String[] {
        "-Xmx1G", "-Xms1G", "-Xmn256M", "-Xlog:gc" };

    public static void main(String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: TestExcessGCLockerCollectionsAux" +
                               " <duration sec> <alloc threads>" +
                               " <jni critical threads>");
            throw new RuntimeException("Invalid arguments");
        }

        ArrayList<String> finalArgs = new ArrayList<String>();
        finalArgs.addAll(Arrays.asList(COMMON_OPTIONS));
        finalArgs.add(TestExcessGCLockerCollectionsAux.class.getName());
        finalArgs.addAll(Arrays.asList(args));

        // GC and other options obtained from test framework.
        OutputAnalyzer output = ProcessTools.executeTestJvm(finalArgs);
        output.shouldHaveExitValue(0);
        //System.out.println("------------- begin stdout ----------------");
        //System.out.println(output.getStdout());
        //System.out.println("------------- end stdout ----------------");
        output.stdoutShouldMatch(ANY_LOCKER);
        output.stdoutShouldNotMatch(BAD_LOCKER);
    }
}
