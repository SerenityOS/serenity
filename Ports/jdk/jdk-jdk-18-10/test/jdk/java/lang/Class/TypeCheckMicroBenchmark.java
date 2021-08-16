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
 * repeat 5 for f in -client -server; do mergeBench dolphin . jr -dsa -da $f TypeCheckMicroBenchmark.java; done
 *
 *
 * @author Martin Buchholz
 */

import java.util.*;

public class TypeCheckMicroBenchmark {
    abstract static class Job {
        private final String name;
        Job(String name) { this.name = name; }
        String name() { return name; }
        abstract void work() throws Throwable;
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

    private static java.util.regex.Pattern patternArg(String[] args,
                                                      String keyword) {
        String val = keywordValue(args, keyword);
        return val == null ? null : java.util.regex.Pattern.compile(val);
    }

    private static Job[] filter(java.util.regex.Pattern filter,
                                Job[] jobs) {
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

    /**
     * Usage: [iterations=N] [size=N] [filter=REGEXP]
     */
    public static void main(String[] args) throws Throwable {
        final int iterations = intArg(args, "iterations", 30000);
        final int size       = intArg(args, "size", 1000);
        final java.util.regex.Pattern filter
            = patternArg(args, "filter");

        final List<Integer> list = new ArrayList<Integer>();
        final Random rnd = new Random();
        for (int i = 0; i < size; i++)
            list.add(rnd.nextInt());
        final Class klazz = Integer.class;

        final Job[] jobs = {
            new Job("toArray(T[])") { void work() {
                Object[] a = new Integer[0];
                for (int i = 0; i < iterations; i++) {
                    try { list.toArray(a); }
                    catch (ArrayStoreException ase) {
                        throw new ClassCastException(); }}}},
            new Job("isInstance") { void work() {
                for (int i = 0; i < iterations; i++) {
                    for (Object x : list.toArray())
                        if (! (x != null && klazz.isInstance(x)))
                            throw new ClassCastException(); }}},
            new Job("Class.cast") { void work() {
                for (int i = 0; i < iterations; i++) {
                    for (Object x : list.toArray())
                        klazz.cast(x); }}},
            new Job("write into array") { void work() {
                Object[] a = new Integer[1];
                for (int i = 0; i < iterations; i++) {
                    for (Object x : list.toArray()) {
                        try { a[0] = x; }
                        catch (ArrayStoreException unused) {
                            throw new ClassCastException(); }}}}},
            new Job("write into dynamic array") { void work() {
                for (int i = 0; i < iterations; i++) {
                    for (Object x : list.toArray()) {
                        Object[] a = (Object[])
                            java.lang.reflect.Array.newInstance(klazz, 1);
                        try { a[0] = x; }
                        catch (ArrayStoreException unused) {
                            throw new ClassCastException(); }}}}}
        };

        time(filter(filter, jobs));
    }
}
