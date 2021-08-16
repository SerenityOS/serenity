/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * This is not a regression test, but a micro-benchmark.
 *
 * I have run this as follows:
 *
 * repeat 5 for f in -client -server; do mergeBench dolphin . jr -dsa -da $f RangeCheckMicroBenchmark.java; done
 *
 *
 * @author Martin Buchholz
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class RangeCheckMicroBenchmark {
    abstract static class Job {
        private final String name;
        Job(String name) { this.name = name; }
        String name() { return name; }
        abstract void work() throws Throwable;
    }

    private static void collectAllGarbage() {
        final CountDownLatch drained = new CountDownLatch(1);
        try {
            System.gc();        // enqueue finalizable objects
            new Object() { protected void finalize() {
                drained.countDown(); }};
            System.gc();        // enqueue detector
            drained.await();    // wait for finalizer queue to drain
            System.gc();        // cleanup finalized objects
        } catch (InterruptedException e) { throw new Error(e); }
    }

    /**
     * Runs each job for long enough that all the runtime compilers
     * have had plenty of time to warm up, i.e. get around to
     * compiling everything worth compiling.
     * Returns array of average times per job per run.
     */
    private static long[] time0(Job ... jobs) throws Throwable {
        final long warmupNanos = 10L * 1000L * 1000L * 1000L;
        long[] nanoss = new long[jobs.length];
        for (int i = 0; i < jobs.length; i++) {
            collectAllGarbage();
            long t0 = System.nanoTime();
            long t;
            int j = 0;
            do { jobs[i].work(); j++; }
            while ((t = System.nanoTime() - t0) < warmupNanos);
            nanoss[i] = t/j;
        }
        return nanoss;
    }

    private static void time(Job ... jobs) throws Throwable {

        long[] warmup = time0(jobs); // Warm up run
        long[] nanoss = time0(jobs); // Real timing run
        long[] milliss = new long[jobs.length];
        double[] ratios = new double[jobs.length];

        final String nameHeader   = "Method";
        final String millisHeader = "Millis";
        final String ratioHeader  = "Ratio";

        int nameWidth   = nameHeader.length();
        int millisWidth = millisHeader.length();
        int ratioWidth  = ratioHeader.length();

        for (int i = 0; i < jobs.length; i++) {
            nameWidth = Math.max(nameWidth, jobs[i].name().length());

            milliss[i] = nanoss[i]/(1000L * 1000L);
            millisWidth = Math.max(millisWidth,
                                   String.format("%d", milliss[i]).length());

            ratios[i] = (double) nanoss[i] / (double) nanoss[0];
            ratioWidth = Math.max(ratioWidth,
                                  String.format("%.3f", ratios[i]).length());
        }

        String format = String.format("%%-%ds %%%dd %%%d.3f%%n",
                                      nameWidth, millisWidth, ratioWidth);
        String headerFormat = String.format("%%-%ds %%%ds %%%ds%%n",
                                            nameWidth, millisWidth, ratioWidth);
        System.out.printf(headerFormat, "Method", "Millis", "Ratio");

        // Print out absolute and relative times, calibrated against first job
        for (int i = 0; i < jobs.length; i++)
            System.out.printf(format, jobs[i].name(), milliss[i], ratios[i]);
    }

    private static String keywordValue(String[] args, String keyword) {
        for (String arg : args)
            if (arg.startsWith(keyword))
                return arg.substring(keyword.length() + 1);
        return null;
    }

    private static int intArg(String[] args, String keyword, int defaultValue) {
        String val = keywordValue(args, keyword);
        return val == null ? defaultValue : Integer.parseInt(val);
    }

    private static Pattern patternArg(String[] args, String keyword) {
        String val = keywordValue(args, keyword);
        return val == null ? null : Pattern.compile(val);
    }

    private static Job[] filter(Pattern filter, Job[] jobs) {
        return (filter == null) ? jobs
            : Arrays.stream(jobs)
            .filter(job -> filter.matcher(job.name()).find())
            .collect(toList())
            .toArray(new Job[0]);
    }

    private static void deoptimize(ArrayList<Integer> list) {
        for (Integer x : list)
            if (x == null)
                throw new Error();
    }

    /**
     * Usage: [iterations=N] [size=N] [filter=REGEXP]
     */
    public static void main(String[] args) throws Throwable {
        final int iterations = intArg(args, "iterations", 30000);
        final int size       = intArg(args, "size", 1000);
        final Pattern filter = patternArg(args, "filter");

        final ArrayList<Integer> list = new ArrayList<>();
        final Random rnd = new Random();
        for (int i = 0; i < size; i++)
            list.add(rnd.nextInt());

        final Job[] jobs = {
            new Job("get") { void work() {
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        if (list.get(k) == 42)
                            throw new Error();
                }
                deoptimize(list);}},
            new Job("set") { void work() {
                Integer[] xs = list.toArray(new Integer[size]);
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        list.set(k, xs[k]);
                }
                deoptimize(list);}},
            new Job("get/set") { void work() {
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        list.set(k, list.get(size - k - 1));
                }
                deoptimize(list);}},
            new Job("add/remove at end") { void work() {
                Integer x = rnd.nextInt();
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size - 1; k++) {
                        list.add(size, x);
                        list.remove(size);
                    }
                }
                deoptimize(list);}},
            new Job("subList get") { void work() {
                List<Integer> sublist = list.subList(0, list.size());
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        if (sublist.get(k) == 42)
                            throw new Error();
                }
                deoptimize(list);}},
            new Job("subList set") { void work() {
                List<Integer> sublist = list.subList(0, list.size());
                Integer[] xs = sublist.toArray(new Integer[size]);
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        sublist.set(k, xs[k]);
                }
                deoptimize(list);}},
            new Job("subList get/set") { void work() {
                List<Integer> sublist = list.subList(0, list.size());
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size; k++)
                        sublist.set(k, sublist.get(size - k - 1));
                }
                deoptimize(list);}},
            new Job("subList add/remove at end") { void work() {
                List<Integer> sublist = list.subList(0, list.size());
                Integer x = rnd.nextInt();
                for (int i = 0; i < iterations; i++) {
                    for (int k = 0; k < size - 1; k++) {
                        sublist.add(size, x);
                        sublist.remove(size);
                    }
                }
                deoptimize(list);}}
        };

        time(filter(filter, jobs));
    }
}
