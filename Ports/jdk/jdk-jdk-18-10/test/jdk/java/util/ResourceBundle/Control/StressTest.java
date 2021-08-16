/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5102289
 * @summary Stress test for ResourceBundle.getBundle with ResourceBundle.Control.
 * @run main/othervm -esa StressTest 2 15
 * @key randomness
 */

import java.util.*;
import java.util.concurrent.atomic.*;

// Usage: java StressTest [threadsFactor [duration]]
public class StressTest {
    static final Locale ROOT_LOCALE = new Locale("");
    static final Random rand = new Random();
    static final Locale[] locales = {
        Locale.US,
        Locale.CHINA,
        ROOT_LOCALE,
        Locale.JAPAN,
        Locale.CANADA,
        Locale.KOREA
    };
    static final String[] expected = {
        "U.S.A.",
        "China",
        "U.S.A.",
        "Japan",
        "U.S.A.", // StressOut_en_CA.properties is empty.
        "Korea"
    };
    static final long startTime = System.currentTimeMillis();

    // increment each element when one getBundle call is done.
    static AtomicIntegerArray counters;
    static int[] prevCounters;
    static int intervalForCounterCheck;
    static AtomicInteger clearCounter = new AtomicInteger();

    static volatile boolean runrun = true;

    public static void main(String[] args) {
        int threadsFactor = 2;
        if (args.length > 0) {
            threadsFactor = Math.max(2, Integer.parseInt(args[0]));
        }
        int duration = 180;
        if (args.length > 1) {
            duration = Math.max(5, Integer.parseInt(args[1]));
        }

        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            Thread[] tasks = new Thread[locales.length * threadsFactor];
            counters = new AtomicIntegerArray(tasks.length);

            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new Thread(new Worker(i));
            }
            for (int i = 0; i < tasks.length; i++) {
                tasks[i].start();
            }

            int nProcessors = Runtime.getRuntime().availableProcessors();
            intervalForCounterCheck = Math.max(tasks.length / nProcessors, 1);
            System.out.printf(
                "%d processors, intervalForCounterCheck = %d [sec]%n",
                          nProcessors, intervalForCounterCheck);
            try {
                for (int i = 0; runrun && i < duration; i++) {
                    Thread.sleep(1000); // 1 second
                    if ((i % intervalForCounterCheck) == 0) {
                        checkCounters();
                    }
                }
                runrun = false;
                for (int i = 0; i < tasks.length; i++) {
                    tasks[i].join();
                }
            } catch (InterruptedException e) {
            }

            printCounters();
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    static void checkCounters() {
        int length = counters.length();
        int[] snapshot = new int[length];
        for (int i = 0; i < length; i++) {
            snapshot[i] = counters.get(i);
        }

        if (prevCounters == null) {
            prevCounters = snapshot;
            return;
        }

        for (int i = 0; i < length; i++) {
            if (snapshot[i] > prevCounters[i]) {
                continue;
            }
            System.out.printf(
                "Warning: Thread #%d hasn't updated its counter for the last %d second(s).%n",
                i, intervalForCounterCheck);
        }
        prevCounters = snapshot;
    }

    static void printCounters() {
        long total = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < counters.length(); i++) {
            int counter = counters.get(i);
            total += counter;
            min = Math.min(min, counter);
            max = Math.max(max, counter);
        }
        System.out.printf("Total: %d calls, min=%d, max=%d, cache cleared %d times%n",
                          total, min, max, clearCounter.get());
    }

    static class Worker implements Runnable {
        final int id;
        final int index;
        final Locale locale;
        final String str;
        final int max;
        final boolean cleaner;
        ResourceBundle.Control control;

        Worker(int i) {
            id = i;
            index = i % locales.length;
            locale = locales[index];
            cleaner = locale.equals(ROOT_LOCALE);
            str = expected[index];
            max = rand.nextInt((index + 1) * 500) + 1000;
            control = new TestControl(max);
            System.out.println("Worker" + i + ": locale="+locale+", expected="+str+
                               ", max="+max);
        }

        public void run() {
            while (runrun) {
                ResourceBundle rb = ResourceBundle.getBundle("StressOut", locale, control);
                counters.incrementAndGet(id);
                String s = rb.getString("data");
                if (!s.equals(str)) {
                    runrun = false;
                    throw new RuntimeException(locale + ": rb.locale=" + rb.getLocale() +
                                               ", got " + s + ", expected " + str);
                }
                try {
                    Thread.sleep(rand.nextInt(max/500));
                } catch (InterruptedException e) {
                }
                if (cleaner && (rand.nextInt(10000) == 0)) {
                    //System.out.println("Clearing cache!");
                    ResourceBundle.clearCache();
                    clearCounter.incrementAndGet();
                }
            }
        }

        static class TestControl extends ResourceBundle.Control {
            int max;

            public List<Locale> getCandidateLocales(String baseName, Locale locale) {
                List<Locale> list = super.getCandidateLocales(baseName, locale);
                //System.out.println("Candidate locales=" + list);
                return list;
            }
            public TestControl(int max) {
                this.max = max;
            }
            public long getTimeToLive(String baseName, Locale locale) {
                // This will set TTL to a random value for each bundle.
                long ttl = rand.nextInt(max);
                //System.out.println("TTL: " + baseName + "_" + locale + " for " + ttl);
                return ttl;
            }
            public boolean needsReload(String baseName, Locale locale,
                                       String format, ClassLoader loader,
                                       ResourceBundle bundle, long loadTime) {
                //System.out.println("Expired: " + baseName + "_" + locale + "." + format +
                //                 " at " + (loadTime-startTime) + ", bundle=" + bundle);
                return rand.nextBoolean();
            }
        }
    }
}
