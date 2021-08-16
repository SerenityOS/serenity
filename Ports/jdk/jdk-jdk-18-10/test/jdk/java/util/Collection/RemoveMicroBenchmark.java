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
 * @run main RemoveMicroBenchmark iterations=1 size=8 warmup=0
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toCollection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Usage: [iterations=N] [size=N] [filter=REGEXP] [warmup=SECONDS]
 *
 * To run this in micro-benchmark mode, simply run as a normal java program.
 * Be patient; this program runs for a very long time.
 * For faster runs, restrict execution using command line args.
 *
 * @author Martin Buchholz
 */
public class RemoveMicroBenchmark {
    abstract static class Job {
        private final String name;
        public Job(String name) { this.name = name; }
        public String name() { return name; }
        public abstract void work() throws Throwable;
        public void run() {
            try { work(); }
            catch (Throwable ex) {
                // current job cannot always be deduced from stacktrace.
                throw new RuntimeException("Job failed: " + name(), ex);
            }
        }
    }

    final int iterations;
    final int size;             // number of elements in collections
    final double warmupSeconds;
    final long warmupNanos;
    final Pattern nameFilter;   // select subset of Jobs to run
    final boolean reverse;      // reverse order of Jobs
    final boolean shuffle;      // randomize order of Jobs

    final ArrayList<Integer> elements; // contains size random Integers

    RemoveMicroBenchmark(String[] args) {
        iterations    = intArg(args, "iterations", 10_000);
        size          = intArg(args, "size", 1000);
        warmupSeconds = doubleArg(args, "warmup", 7.0);
        nameFilter    = patternArg(args, "filter");
        reverse       = booleanArg(args, "reverse");
        shuffle       = booleanArg(args, "shuffle");

        warmupNanos = (long) (warmupSeconds * (1000L * 1000L * 1000L));

        elements = ThreadLocalRandom.current().ints(size)
            .mapToObj(x -> x)
            .collect(toCollection(ArrayList::new));
    }

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
    long[] time0(List<Job> jobs) {
        final int size = jobs.size();
        long[] nanoss = new long[size];
        for (int i = 0; i < size; i++) {
            if (warmupNanos > 0) forceFullGc();
            Job job = jobs.get(i);
            long totalTime;
            int runs = 0;
            long startTime = System.nanoTime();
            do { job.run(); runs++; }
            while ((totalTime = System.nanoTime() - startTime) < warmupNanos);
            nanoss[i] = totalTime/runs;
        }
        return nanoss;
    }

    void time(List<Job> jobs) throws Throwable {
        if (warmupNanos > 0) time0(jobs); // Warm up run
        final int size = jobs.size();
        final long[] nanoss = time0(jobs); // Real timing run
        final long[] milliss = new long[size];
        final double[] ratios = new double[size];

        final String nameHeader   = "Method";
        final String millisHeader = "Millis";
        final String ratioHeader  = "Ratio";

        int nameWidth   = nameHeader.length();
        int millisWidth = millisHeader.length();
        int ratioWidth  = ratioHeader.length();

        for (int i = 0; i < size; i++) {
            nameWidth = Math.max(nameWidth, jobs.get(i).name().length());

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
        for (int i = 0; i < size; i++)
            System.out.printf(format, jobs.get(i).name(), milliss[i], ratios[i]);
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

    private static boolean booleanArg(String[] args, String keyword) {
        String val = keywordValue(args, keyword);
        if (val == null || val.equals("false")) return false;
        if (val.equals("true")) return true;
        throw new IllegalArgumentException(val);
    }

    private static void deoptimize(int sum) {
        if (sum == 42)
            System.out.println("the answer");
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

    // Checks for correctness *and* prevents loop optimizations
    static class Check {
        private int sum;
        public void sum(int sum) {
            if (this.sum == 0)
                this.sum = sum;
            if (this.sum != sum)
                throw new AssertionError("Sum mismatch");
        }
    }
    volatile Check check = new Check();

    public static void main(String[] args) throws Throwable {
        new RemoveMicroBenchmark(args).run();
    }

    HashMap<Class<?>, String> goodClassName = new HashMap<>();

    String goodClassName(Class<?> klazz) {
        return goodClassName.computeIfAbsent(
            klazz,
            k -> {
                String simple = k.getSimpleName();
                return (simple.equals("SubList")) // too simple!
                    ? k.getName().replaceFirst(".*\\.", "")
                    : simple;
            });
    }

    String goodClassName(Object x) {
        return goodClassName(x.getClass());
    }

    static List<Integer> makeSubList(List<Integer> list) {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int size = rnd.nextInt(4);
        for (int i = size; i--> 0; )
            list.add(rnd.nextInt());
        int index = rnd.nextInt(size + 1);
        return list.subList(index, index);
    }

    private static <T> List<T> asSubList(List<T> list) {
        return list.subList(0, list.size());
    }

    @SafeVarargs @SuppressWarnings("varargs")
    private static <T> Stream<T> concatStreams(Stream<T> ... streams) {
        return Stream.of(streams).flatMap(s -> s);
    }

    Class<?> topLevelClass(Object x) {
        for (Class<?> k = x.getClass();; ) {
            Class<?> enclosing = k.getEnclosingClass();
            if (enclosing == null)
                return k;
            k = enclosing;
        }
    }

    void run() throws Throwable {
        ArrayList<Job> jobs = Stream.<Collection<Integer>>of(
                new ArrayList<>(),
                makeSubList(new ArrayList<>()),
                new LinkedList<>(),
                makeSubList(new LinkedList<>()),
                new Vector<>(),
                makeSubList(new Vector<>()),
                new CopyOnWriteArrayList<>(),
                makeSubList(new CopyOnWriteArrayList<>()),
                new ArrayDeque<>(),
                new PriorityQueue<>(),
                new ArrayBlockingQueue<>(elements.size()),
                new ConcurrentLinkedQueue<>(),
                new ConcurrentLinkedDeque<>(),
                new LinkedBlockingQueue<>(),
                new LinkedBlockingDeque<>(),
                new LinkedTransferQueue<>(),
                new PriorityBlockingQueue<>())
            .flatMap(x -> jobs(x))
            .filter(job ->
                nameFilter == null || nameFilter.matcher(job.name()).find())
            .collect(toCollection(ArrayList::new));

        if (reverse) Collections.reverse(jobs);
        if (shuffle) Collections.shuffle(jobs);

        time(jobs);
    }

    Stream<Job> jobs(Collection<Integer> x) {
        return concatStreams(
            collectionJobs(x),

            (CopyOnWriteArrayList.class.isAssignableFrom(topLevelClass(x)))
            ? Stream.empty()
            : iteratorRemoveJobs(x),

            (x instanceof Queue)
            ? queueJobs((Queue<Integer>)x)
            : Stream.empty(),

            (x instanceof Deque)
            ? dequeJobs((Deque<Integer>)x)
            : Stream.empty(),

            (x instanceof BlockingQueue)
            ? blockingQueueJobs((BlockingQueue<Integer>)x)
            : Stream.empty(),

            (x instanceof BlockingDeque)
            ? blockingDequeJobs((BlockingDeque<Integer>)x)
            : Stream.empty());
    }

    Collection<Integer> universeRecorder(int[] sum) {
        return new ArrayList<>() {
            public boolean contains(Object x) {
                sum[0] += (Integer) x;
                return true;
            }};
    }

    Collection<Integer> emptyRecorder(int[] sum) {
        return new ArrayList<>() {
            public boolean contains(Object x) {
                sum[0] += (Integer) x;
                return false;
            }};
    }

    Stream<Job> collectionJobs(Collection<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        x.removeIf(n -> { sum[0] += n; return true; });
                        check.sum(sum[0]);}}},
            new Job(klazz + " removeIf rnd-two-pass") {
                public void work() throws Throwable {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        x.removeIf(n -> {
                            boolean b = rnd.nextBoolean();
                            if (b) sum[0] += n;
                            return b; });
                        x.removeIf(n -> { sum[0] += n; return true; });
                        check.sum(sum[0]);}}},
            new Job(klazz + " removeAll") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Collection<Integer> universe = universeRecorder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        x.removeAll(universe);
                        check.sum(sum[0]);}}},
            new Job(klazz + " retainAll") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Collection<Integer> empty = emptyRecorder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        x.retainAll(empty);
                        check.sum(sum[0]);}}},
            new Job(klazz + " clear") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        x.forEach(e -> sum[0] += e);
                        x.clear();
                        check.sum(sum[0]);}}});
    }

    Stream<Job> iteratorRemoveJobs(Collection<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
             new Job(klazz + " Iterator.remove") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        Iterator<Integer> it = x.iterator();
                        while (it.hasNext()) {
                            sum[0] += it.next();
                            it.remove();
                        }
                        check.sum(sum[0]);}}},
            new Job(klazz + " Iterator.remove-rnd-two-pass") {
                public void work() throws Throwable {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Iterator<Integer> it = x.iterator();
                             it.hasNext(); ) {
                            Integer e = it.next();
                            if (rnd.nextBoolean()) {
                                sum[0] += e;
                                it.remove();
                            }
                        }
                        for (Iterator<Integer> it = x.iterator();
                             it.hasNext(); ) {
                            sum[0] += it.next();
                            it.remove();
                        }
                        check.sum(sum[0]);}}});
    }

    Stream<Job> queueJobs(Queue<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " poll()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.poll()) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}});
    }

    Stream<Job> dequeJobs(Deque<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " descendingIterator().remove") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        Iterator<Integer> it = x.descendingIterator();
                        while (it.hasNext()) {
                            sum[0] += it.next();
                            it.remove();
                        }
                        check.sum(sum[0]);}}},
            new Job(klazz + " pollFirst()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.pollFirst()) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}},
            new Job(klazz + " pollLast()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.pollLast()) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}});
    }

    Stream<Job> blockingQueueJobs(BlockingQueue<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " timed poll()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.poll(0L, TimeUnit.DAYS)) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}},
            new Job(klazz + " drainTo(sink)") {
                public void work() throws Throwable {
                    ArrayList<Integer> sink = new ArrayList<>();
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        sink.clear();
                        x.addAll(elements);
                        x.drainTo(sink);
                        sink.forEach(e -> sum[0] += e);
                        check.sum(sum[0]);}}},
            new Job(klazz + " drainTo(sink, n)") {
                public void work() throws Throwable {
                    ArrayList<Integer> sink = new ArrayList<>();
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        sink.clear();
                        x.addAll(elements);
                        x.drainTo(sink, elements.size());
                        sink.forEach(e -> sum[0] += e);
                        check.sum(sum[0]);}}});
    }

    Stream<Job> blockingDequeJobs(BlockingDeque<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " timed pollFirst()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.pollFirst(0L, TimeUnit.DAYS)) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}},
            new Job(klazz + " timed pollLast()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.addAll(elements);
                        for (Integer e; (e = x.pollLast(0L, TimeUnit.DAYS)) != null; )
                            sum[0] += e;
                        check.sum(sum[0]);}}});
    }
}
