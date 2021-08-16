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
 * @test
 * @summary micro-benchmark correctness mode
 * @run main IteratorMicroBenchmark iterations=1 size=8 warmup=0
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Usage: [iterations=N] [size=N] [filter=REGEXP] [warmup=SECONDS]
 *
 * To run this in micro-benchmark mode, simply run as a normal java program.
 * Be patient; this program runs for a very long time.
 * For faster runs, restrict execution using command line args.
 *
 * @author Martin Buchholz
 */
public class IteratorMicroBenchmark {
    abstract static class Job {
        private final String name;
        public Job(String name) { this.name = name; }
        public String name() { return name; }
        public abstract void work() throws Throwable;
    }

    static double warmupSeconds;
    static long warmupNanos;

    // --------------- GC finalization infrastructure ---------------

    /** No guarantees, but effective in practice. */
    static void forceFullGc() {
        long timeoutMillis = 1000L;
        CountDownLatch finalized = new CountDownLatch(1);
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        WeakReference<Object> ref = new WeakReference<>(
            new Object() { protected void finalize() { finalized.countDown(); }},
            queue);
        try {
            for (int tries = 3; tries--> 0; ) {
                System.gc();
                if (finalized.await(timeoutMillis, MILLISECONDS)
                    && queue.remove(timeoutMillis) != null
                    && ref.get() == null) {
                    System.runFinalization(); // try to pick up stragglers
                    return;
                }
                timeoutMillis *= 4;
            }
        } catch (InterruptedException unexpected) {
            throw new AssertionError("unexpected InterruptedException");
        }
        throw new AssertionError("failed to do a \"full\" gc");
    }

    /**
     * Runs each job for long enough that all the runtime compilers
     * have had plenty of time to warm up, i.e. get around to
     * compiling everything worth compiling.
     * Returns array of average times per job per run.
     */
    private static long[] time0(Job ... jobs) throws Throwable {
        long[] nanoss = new long[jobs.length];
        for (int i = 0; i < jobs.length; i++) {
            if (warmupNanos > 0) forceFullGc();
            Job job = jobs[i];
            long totalTime;
            int runs = 0;
            long startTime = System.nanoTime();
            do { job.work(); runs++; }
            while ((totalTime = System.nanoTime() - startTime) < warmupNanos);
            nanoss[i] = totalTime/runs;
        }
        return nanoss;
    }

    private static void time(Job ... jobs) throws Throwable {
        if (warmupSeconds > 0.0) time0(jobs); // Warm up run
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
        return (val == null) ? defaultValue : Integer.parseInt(val);
    }

    private static double doubleArg(String[] args, String keyword, double defaultValue) {
        String val = keywordValue(args, keyword);
        return (val == null) ? defaultValue : Double.parseDouble(val);
    }

    private static Pattern patternArg(String[] args, String keyword) {
        String val = keywordValue(args, keyword);
        return (val == null) ? null : Pattern.compile(val);
    }

    private static Job[] filter(Pattern filter, Job[] jobs) {
        return (filter == null) ? jobs
            : Arrays.stream(jobs)
            .filter(job -> filter.matcher(job.name()).find())
            .collect(toList())
            .toArray(new Job[0]);
    }

    private static void deoptimize(int sum) {
        if (sum == 42)
            System.out.println("the answer");
    }

    private static <T> List<T> asSubList(List<T> list) {
        return list.subList(0, list.size());
    }

    private static <T> Iterable<T> backwards(final List<T> list) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final ListIterator<T> it = list.listIterator(list.size());
                    public boolean hasNext() { return it.hasPrevious(); }
                    public T next()          { return it.previous(); }
                    public void remove()     {        it.remove(); }};}};
    }

    public static void main(String[] args) throws Throwable {
        final int iterations = intArg(args, "iterations", 100_000);
        final int size       = intArg(args, "size", 1000);
        warmupSeconds        = doubleArg(args, "warmup", 7.0);
        final Pattern filter = patternArg(args, "filter");

        warmupNanos = (long) (warmupSeconds * (1000L * 1000L * 1000L));

//         System.out.printf(
//             "iterations=%d size=%d, warmup=%1g, filter=\"%s\"%n",
//             iterations, size, warmupSeconds, filter);

        final ConcurrentSkipListMap<Integer,Integer> m
            = new ConcurrentSkipListMap<>();
        final ArrayList<Integer> al = new ArrayList<>(size);

        // Populate collections with random data
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < size; i++) {
            m.put(rnd.nextInt(size), rnd.nextInt(size));
            al.add(rnd.nextInt(size));
        }
        final Vector<Integer> v = new Vector<>(al);
        final ArrayDeque<Integer> ad = new ArrayDeque<>(al);
        // shuffle ArrayDeque elements so they wrap
        for (int i = 0, n = rnd.nextInt(size); i < n; i++)
            ad.addLast(ad.removeFirst());

        // Also test "short" collections
        final int shortSize = 5;
        final Vector<Integer> sv = new Vector<>(v.subList(0, shortSize));
        final ArrayList<Integer> sal = new ArrayList<>(sv);

        // Checks for correctness *and* prevents loop optimizations
        class Check {
            private int sum;
            public void sum(int sum) {
                if (this.sum == 0)
                    this.sum = sum;
                if (this.sum != sum)
                    throw new AssertionError("Sum mismatch");
            }
        }
        final Check check      = new Check();
        final Check shortCheck = new Check();

        Job[] jobs = {
//          new Job("Vector iterate desugared") {
//              public void work() throws Throwable {
//                  for (int i = 0; i < iterations; i++) {
//                      int sum = 0;
//                      for (Iterator<Integer> it = v.iterator(); it.hasNext();)
//                          sum += it.next();
//                      check.sum(sum);}}},
            new Job("array loop") {
                public void work() throws Throwable {
                    Integer[] a = al.toArray(new Integer[0]);
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        int size = a.length;
                        for (int j = 0; j < size; ++j)
                            sum += a[j];
                        check.sum(sum);}}},
            new Job("descending array loop") {
                public void work() throws Throwable {
                    Integer[] a = al.toArray(new Integer[0]);
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        int size = a.length;
                        for (int j = size - 1; j >= 0; j--)
                            sum += a[j];
                        check.sum(sum);}}},
            new Job("Vector get loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        int size = v.size();
                        for (int j = 0; j < size; ++j)
                            sum += v.get(j);
                        check.sum(sum);}}},
            new Job("Vector iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : v)
                            sum += n;
                        check.sum(sum);}}},
            new Job("Vector descending listIterator loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        ListIterator<Integer> it = v.listIterator(al.size());
                        while (it.hasPrevious())
                            sum += it.previous();
                        check.sum(sum);}}},
            new Job("Vector Enumeration loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        Enumeration<Integer> it = v.elements();
                        while (it.hasMoreElements())
                            sum += it.nextElement();
                        check.sum(sum);}}},
            new Job("Vector subList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : asSubList(v))
                            sum += n;
                        check.sum(sum);}}},
            new Job("Vector subList subList subList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : asSubList(asSubList(asSubList(v))))
                            sum += n;
                        check.sum(sum);}}},
            new Job("Vector backwards wrapper ListIterator for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : backwards(v))
                            sum += n;
                        check.sum(sum);}}},
            new Job("Vector backwards wrapper subList ListIterator for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : backwards(asSubList(v)))
                            sum += n;
                        check.sum(sum);}}},
//          new Job("Vector iterate for loop invokeinterface") {
//              public void work() throws Throwable {
//                  final List<Integer> l = v;
//                  for (int i = 0; i < iterations; i++) {
//                      int sum = 0;
//                      for (Integer n : l)
//                          sum += n;
//                      check.sum(sum);}}},
//          new Job("Vector subList iterate for loop invokeinterface") {
//              public void work() throws Throwable {
//                  final List<Integer> l = v;
//                  for (int i = 0; i < iterations; i++) {
//                      int sum = 0;
//                      for (Integer n : asSubList(l))
//                          sum += n;
//                      check.sum(sum);}}},
            new Job("Short Vector get loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        int size = sv.size();
                        for (int j = 0; j < size; ++j)
                            sum += sv.get(j);
                        shortCheck.sum(sum);}}},
            new Job("Short Vector iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        for (Integer n : sv)
                            sum += n;
                        shortCheck.sum(sum);}}},
            new Job("Short Vector sublist iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        for (Integer n : asSubList(sv))
                            sum += n;
                        shortCheck.sum(sum);}}},
            new Job("ArrayList get loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        int size = al.size();
                        for (int j = 0; j < size; ++j)
                            sum += al.get(j);
                        check.sum(sum);}}},
            new Job("ArrayList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : al)
                            sum += n;
                        check.sum(sum);}}},
            new Job("ArrayDeque iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : ad)
                            sum += n;
                        check.sum(sum);}}},
            new Job("ArrayList descending listIterator loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        ListIterator<Integer> it = al.listIterator(al.size());
                        while (it.hasPrevious())
                            sum += it.previous();
                        check.sum(sum);}}},
            new Job("ArrayList listIterator loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        ListIterator<Integer> it = al.listIterator();
                        while (it.hasNext())
                            sum += it.next();
                        check.sum(sum);}}},
            new Job("ArrayDeque.descendingIterator() loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        Iterator<Integer> it = ad.descendingIterator();
                        while (it.hasNext())
                            sum += it.next();
                        check.sum(sum);}}},
            new Job("ArrayList.forEach") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        al.forEach(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.forEach") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.forEach(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("Vector.forEach") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        v.forEach(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayList.iterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        al.iterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.descendingIterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.descendingIterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.iterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.iterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("Vector.iterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        v.iterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayList.spliterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        al.spliterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.spliterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.spliterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("Vector.spliterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        v.spliterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job("ArrayList.spliterator().tryAdvance()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        Spliterator<Integer> spliterator = al.spliterator();
                        do {} while (spliterator.tryAdvance(n -> sum[0] += n));
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.spliterator().tryAdvance()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        Spliterator<Integer> spliterator = ad.spliterator();
                        do {} while (spliterator.tryAdvance(n -> sum[0] += n));
                        check.sum(sum[0]);}}},
            new Job("Vector.spliterator().tryAdvance()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        Spliterator<Integer> spliterator = v.spliterator();
                        do {} while (spliterator.tryAdvance(n -> sum[0] += n));
                        check.sum(sum[0]);}}},
            new Job("ArrayList.removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        al.removeIf(n -> { sum[0] += n; return false; });
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.removeIf(n -> { sum[0] += n; return false; });
                        check.sum(sum[0]);}}},
            new Job("Vector.removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        v.removeIf(n -> { sum[0] += n; return false; });
                        check.sum(sum[0]);}}},
            new Job("ArrayList subList .removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    List<Integer> sl = asSubList(al);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        sl.removeIf(n -> { sum[0] += n; return false; });
                        check.sum(sum[0]);}}},
            new Job("ArrayList subList get loop") {
                public void work() throws Throwable {
                    List<Integer> sl = asSubList(al);
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        int size = sl.size();
                        for (int j = 0; j < size; ++j)
                            sum += sl.get(j);
                        check.sum(sum);}}},
            new Job("ArrayList subList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : asSubList(al))
                            sum += n;
                        check.sum(sum);}}},
            new Job("ArrayList subList subList subList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : asSubList(asSubList(asSubList(al))))
                            sum += n;
                        check.sum(sum);}}},
            new Job("ArrayList backwards wrapper ListIterator for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : backwards(al))
                            sum += n;
                        check.sum(sum);}}},
            new Job("ArrayList backwards wrapper subList ListIterator for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : backwards(asSubList(al)))
                            sum += n;
                        check.sum(sum);}}},
//          new Job("ArrayList iterate desugared") {
//              public void work() throws Throwable {
//                  for (int i = 0; i < iterations; i++) {
//                      int sum = 0;
//                      for (Iterator<Integer> it = al.iterator(); it.hasNext();)
//                          sum += it.next();
//                      check.sum(sum);}}},
            new Job("Short ArrayList get loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        int size = sal.size();
                        for (int j = 0; j < size; ++j)
                            sum += sal.get(j);
                        shortCheck.sum(sum);}}},
            new Job("Short ArrayList iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        for (Integer n : sal)
                            sum += n;
                        shortCheck.sum(sum);}}},
            new Job("Short ArrayList sublist iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < (iterations * size / shortSize); i++) {
                        int sum = 0;
                        for (Integer n : asSubList(sal))
                            sum += n;
                        shortCheck.sum(sum);}}},
            new Job("Vector ArrayList alternating iteration") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        Iterator<Integer> it1 = v.iterator();
                        Iterator<Integer> it2 = al.iterator();
                        while (it1.hasNext())
                            sum += it1.next() + it2.next();
                        check.sum(sum/2);}}},
            new Job("Vector ArrayList alternating invokeVirtual iteration") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        List<Iterator<Integer>> its = new ArrayList<>(2);
                        its.add(v.iterator());
                        its.add(al.iterator());
                        for (int k = 0; its.get(k).hasNext(); k = (k == 0) ? 1 : 0)
                            sum += its.get(k).next();
                        check.sum(sum/2);}}},
            new Job("ConcurrentSkipListMap entrySet iterate") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Map.Entry<Integer,Integer> e : m.entrySet())
                            sum += e.getKey();
                        deoptimize(sum);}}},
            new Job("ArrayList.toArray()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Object o : al.toArray())
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("ArrayList.toArray(a)") {
                public void work() throws Throwable {
                    Integer[] a = new Integer[size];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        al.toArray(a);
                        for (Object o : a)
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("ArrayList subList .toArray()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Object o : asSubList(al).toArray())
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("ArrayList subList .toArray(a)") {
                public void work() throws Throwable {
                    Integer[] a = new Integer[size];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        asSubList(al).toArray(a);
                        for (Object o : a)
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.toArray()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Object o : ad.toArray())
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("ArrayDeque.toArray(a)") {
                public void work() throws Throwable {
                    Integer[] a = new Integer[size];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        ad.toArray(a);
                        for (Object o : a)
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("Vector.toArray()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Object o : v.toArray())
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job("Vector.toArray(a)") {
                public void work() throws Throwable {
                    Integer[] a = new Integer[size];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        v.toArray(a);
                        for (Object o : a)
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
        };

        time(filter(filter, jobs));
    }
}
