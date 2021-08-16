/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress.systemgc;

// A test that stresses a full GC by allocating objects of different lifetimes
// and concurrently calling System.gc().

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

final class ThreadUtils {
    public static void sleep(long durationMS) {
        try {
            Thread.sleep(durationMS);
        } catch (Exception e) {
        }
    }
}

class Exitable {
    private volatile boolean shouldExit = false;

    protected boolean shouldExit() {
        return shouldExit;
    }

    public void exit() {
        shouldExit = true;
    }
}

class ShortLivedAllocationTask extends Exitable implements Runnable {
    private Map<String, String> map = new HashMap<>();

    @Override
    public void run() {
        map = new HashMap<>();
        while (!shouldExit()) {
            for (int i = 0; i < 200; i++) {
                String key = "short" + " key = " + i;
                String value = "the value is " + i;
                map.put(key, value);
            }
        }
    }
}

class LongLivedAllocationTask extends Exitable implements Runnable {
    private Map<String, String> map;

    LongLivedAllocationTask(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public void run() {
        while (!shouldExit()) {
            String prefix = "long" + System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                String key = prefix + " key = " + i;
                String value = "the value is " + i;
                map.put(key, value);
            }
        }
    }
}

class SystemGCTask extends Exitable implements Runnable {
    private long delayMS;

    SystemGCTask(long delayMS) {
        this.delayMS = delayMS;
    }

    @Override
    public void run() {
        while (!shouldExit()) {
            System.gc();
            ThreadUtils.sleep(delayMS);
        }
    }
}

public class TestSystemGC {
    private static long endTime;

    private static final int numGroups = 7;
    private static final int numGCsPerGroup = 4;

    private static Map<String, String> longLivedMap = new TreeMap<>();

    private static void populateLongLived() {
        for (int i = 0; i < 1000000; i++) {
            String key = "all" + " key = " + i;
            String value = "the value is " + i;
            longLivedMap.put(key, value);
        }
    }

    private static long getDelayMS(int group) {
        if (group == 0) {
            return 0;
        }

        int res = 16;
        for (int i = 0; i < group; i++) {
            res *= 2;
        }
        return res;
    }

    private static void doSystemGCs() {
        ThreadUtils.sleep(1000);

        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < numGCsPerGroup; j++) {
               System.gc();
               if (System.currentTimeMillis() >= endTime) {
                   return;
               }
               ThreadUtils.sleep(getDelayMS(i));
            }
        }
    }

    private static SystemGCTask createSystemGCTask(int group) {
        long delay0 = getDelayMS(group);
        long delay1 = getDelayMS(group + 1);
        long delay = delay0 + (delay1 - delay0) / 2;
        return new SystemGCTask(delay);
    }

    private static void startTask(Runnable task) {
        if (task != null) {
            new Thread(task).start();
        }
    }

    private static void exitTask(Exitable task) {
        if (task != null) {
            task.exit();
        }
    }

    private static void runAllPhases() {
        for (int i = 0; i < 4 && System.currentTimeMillis() < endTime; i++) {
            SystemGCTask gcTask =
                (i % 2 == 1) ? createSystemGCTask(numGroups / 3) : null;
            ShortLivedAllocationTask shortTask =
                (i == 1 || i == 3) ?  new ShortLivedAllocationTask() : null;
            LongLivedAllocationTask longTask =
                (i == 2 || i == 3) ? new LongLivedAllocationTask(longLivedMap) : null;

            startTask(gcTask);
            startTask(shortTask);
            startTask(longTask);

            doSystemGCs();

            exitTask(gcTask);
            exitTask(shortTask);
            exitTask(longTask);

            ThreadUtils.sleep(1000);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must specify timeout in seconds as first argument");
        }
        int timeout = Integer.parseInt(args[0]) * 1000;
        System.out.println("Running with timeout of " + timeout + "ms");
        endTime = System.currentTimeMillis() + timeout;
        // First allocate the long lived objects and then run all phases.
        populateLongLived();
        runAllPhases();
    }
}
