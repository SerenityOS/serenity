/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * To exercise swap, run with filter=LITTLE_ENDIAN on sparc,
 * filter=BIG_ENDIAN on x86.
 *
 * I have run this as follows:
 *
 * for f in -client -server; do mergeBench dolphin . jr -dsa -da $f SwapMicroBenchmark.java filter=LITTLE_ENDIAN; done
 *
 * @author Martin Buchholz
 */

import java.util.*;
import java.nio.*;
import java.util.regex.Pattern;

public class SwapMicroBenchmark {
    abstract static class Job {
        private final String name;
        public Job(String name) { this.name = name; }
        public String name() { return name; }
        public abstract void work() throws Throwable;
    }

    private static void collectAllGarbage() {
        final java.util.concurrent.CountDownLatch drained
            = new java.util.concurrent.CountDownLatch(1);
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
        if (filter == null) return jobs;
        Job[] newJobs = new Job[jobs.length];
        int n = 0;
        for (Job job : jobs)
            if (filter.matcher(job.name()).find())
                newJobs[n++] = job;
        // Arrays.copyOf not available in JDK 5
        Job[] ret = new Job[n];
        System.arraycopy(newJobs, 0, ret, 0, n);
        return ret;
    }

    private static void deoptimize(int sum) {
        if (sum == 42)
            System.out.println("the answer");
    }

    /**
     * Usage: [iterations=N] [size=N] [filter=REGEXP]
     */
    public static void main(String[] args) throws Throwable {
        final int iterations = intArg(args, "iterations", 10000);
        final int size       = intArg(args, "size", 1024);
        final Pattern filter = patternArg(args, "filter");

        final Random rnd = new Random();

        final ByteBuffer b = ByteBuffer.allocateDirect(8*size);
        for (int i = 0; i < b.limit(); i++)
            b.put(i, (byte) rnd.nextInt());

        Job[] jobs = {
            new Job("swap char BIG_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.BIG_ENDIAN);
                    CharBuffer x = b.asCharBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap char LITTLE_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    CharBuffer x = b.asCharBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap short BIG_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.BIG_ENDIAN);
                    ShortBuffer x = b.asShortBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap short LITTLE_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    ShortBuffer x = b.asShortBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap int BIG_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.BIG_ENDIAN);
                    IntBuffer x = b.asIntBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap int LITTLE_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    IntBuffer x = b.asIntBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap long BIG_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.BIG_ENDIAN);
                    LongBuffer x = b.asLongBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}},
            new Job("swap long LITTLE_ENDIAN") {
                public void work() throws Throwable {
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    LongBuffer x = b.asLongBuffer();
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (int j = 0, end = x.limit(); j < end; j++)
                            sum += x.get(j);
                        deoptimize(sum);}}}
        };

        time(filter(filter, jobs));
    }
}
