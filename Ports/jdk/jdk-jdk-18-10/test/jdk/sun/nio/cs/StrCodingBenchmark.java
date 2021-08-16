/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Usage: java StringCodingBenchmark
 * [-Diterations=N] [-Dsize=N] [-Dsubsize=N] [-Dmaxchar=N]
 * [-Dfilter=REGEXP] [-DSecurityManager=true]
 */
public class StrCodingBenchmark {
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
    public static long[] time0(Job ... jobs) throws Throwable {
        //final long warmupNanos = 10L * 1000L * 1000L * 1000L;
        final long warmupNanos = 100L * 100L;
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

    public static long[] time(Job ... jobs) throws Throwable {

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
        String format = String.format("%%-%ds %%%dd %n",
                                      nameWidth, millisWidth);
        String headerFormat = String.format("%%-%ds %%%ds%n",
                                            nameWidth, millisWidth);
        System.out.printf(headerFormat, "Method", "Millis");

        // Print out absolute and relative times, calibrated against first job
        for (int i = 0; i < jobs.length; i++)
            System.out.printf(format, jobs[i].name(), milliss[i], ratios[i]);
        return milliss;
    }

    public static Job[] filter(Pattern filter, Job[] jobs) {
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

    static class PermissiveSecurityManger extends SecurityManager {
        @Override public void checkPermission(java.security.Permission p) {
        }
    }

    public static void main(String[] args) throws Throwable {
        final int itrs = Integer.getInteger("iterations", 100000);
        final int size       = Integer.getInteger("size", 2048);
        final int subsize    = Integer.getInteger("subsize", 128);
        final int maxchar    = Integer.getInteger("maxchar", 128);
        final String regex = System.getProperty("filter");
        final Pattern filter = (regex == null) ? null : Pattern.compile(regex);
        final boolean useSecurityManager = Boolean.getBoolean("SecurityManager");
        if (useSecurityManager)
            System.setSecurityManager(new PermissiveSecurityManger());
        final Random rnd = new Random();

        for (Charset charset:  Charset.availableCharsets().values()) {
            if (!("ISO-8859-1".equals(charset.name()) ||
                  "US-ASCII".equals(charset.name()) ||
                  charset.newDecoder() instanceof sun.nio.cs.SingleByte.Decoder))
                continue;
            final String csn = charset.name();
            final Charset cs = charset;
            final StringBuilder sb = new StringBuilder();
            {
                final CharsetEncoder enc = cs.newEncoder();
                for (int i = 0; i < size; ) {
                    char c = (char) rnd.nextInt(maxchar);
                    if (enc.canEncode(c)) {
                        sb.append(c);
                        i++;
                    }
                }
            }
            final String string = sb.toString();
            final byte[] bytes  = string.getBytes(cs);

            System.out.printf("%n--------%s---------%n", csn);
            for (int sz = 4; sz <= 2048; sz *= 2) {
                System.out.printf("   [len=%d]%n", sz);
                final byte[] bs  = Arrays.copyOf(bytes, sz);
                final String str = new String(bs, csn);
                Job[] jobs = {
                    new Job("String decode: csn") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                            new String(bs, csn);
                    }},

                    new Job("String decode: cs") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                            new String(bs, cs);
                    }},

                    new Job("String encode: csn") {
                    public void work() throws Throwable {
                        for (int i = 0; i < itrs; i++)
                                str.getBytes(csn);
                    }},

                    new Job("String encode: cs") {
                    public void work() throws Throwable {
                         for (int i = 0; i < itrs; i++)
                          str.getBytes(cs);
                    }},
                };
                time(filter(filter, jobs));
            }
        }
    }
}
