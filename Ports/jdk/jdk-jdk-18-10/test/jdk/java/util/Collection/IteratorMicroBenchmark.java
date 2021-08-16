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
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toCollection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
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
import java.util.Spliterator;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.UnaryOperator;
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
public class IteratorMicroBenchmark {
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

    IteratorMicroBenchmark(String[] args) {
        iterations    = intArg(args, "iterations", 10_000);
        size          = intArg(args, "size", 1000);
        warmupSeconds = doubleArg(args, "warmup", 7.0);
        nameFilter    = patternArg(args, "filter");
        reverse       = booleanArg(args, "reverse");
        shuffle       = booleanArg(args, "shuffle");

        warmupNanos = (long) (warmupSeconds * (1000L * 1000L * 1000L));
    }

    // --------------- GC finalization infrastructure ---------------

    /** No guarantees, but effective in practice. */
    static void forceFullGc() {
        long timeoutMillis = 1000L;
        CountDownLatch finalized = new CountDownLatch(1);
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        WeakReference<Object> ref = new WeakReference<>(
            new Object() {
                @SuppressWarnings("deprecation")
                protected void finalize() { finalized.countDown(); }},
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
        new IteratorMicroBenchmark(args).run();
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

    static List<Integer> makeSubList(
        List<Integer> elements,
        UnaryOperator<List<Integer>> copyConstructor) {
        final ArrayList<Integer> padded = new ArrayList<>();
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final int frontPorch = rnd.nextInt(3);
        final int backPorch = rnd.nextInt(3);
        for (int n = frontPorch; n--> 0; ) padded.add(rnd.nextInt());
        padded.addAll(elements);
        for (int n = backPorch; n--> 0; ) padded.add(rnd.nextInt());
        return copyConstructor.apply(padded)
            .subList(frontPorch, frontPorch + elements.size());
    }

    void run() throws Throwable {
        final ArrayList<Integer> al = new ArrayList<>(size);

        // Populate collections with random data
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < size; i++)
            al.add(rnd.nextInt(size));

        final ArrayDeque<Integer> ad = new ArrayDeque<>(al);
        final ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<>(al.size());
        abq.addAll(al);

        // shuffle circular array elements so they wrap
        for (int i = 0, n = rnd.nextInt(size); i < n; i++) {
            ad.addLast(ad.removeFirst());
            abq.add(abq.remove());
        }

        final Integer[] array = al.toArray(new Integer[0]);
        final List<Integer> immutableSubList
            = makeSubList(al, x -> List.of(x.toArray(new Integer[0])));

        Stream<Collection<Integer>> collections = concatStreams(
            Stream.of(
                // Lists and their subLists
                al,
                makeSubList(al, ArrayList::new),
                new Vector<>(al),
                makeSubList(al, Vector::new),
                new LinkedList<>(al),
                makeSubList(al, LinkedList::new),
                new CopyOnWriteArrayList<>(al),
                makeSubList(al, CopyOnWriteArrayList::new),

                ad,
                new PriorityQueue<>(al),
                new ConcurrentLinkedQueue<>(al),
                new ConcurrentLinkedDeque<>(al),

                // Blocking Queues
                abq,
                new LinkedBlockingQueue<>(al),
                new LinkedBlockingDeque<>(al),
                new LinkedTransferQueue<>(al),
                new PriorityBlockingQueue<>(al),

                List.of(al.toArray(new Integer[0]))),

            // avoid UnsupportedOperationException in jdk9 and jdk10
            (goodClassName(immutableSubList).equals("RandomAccessSubList"))
            ? Stream.empty()
            : Stream.of(immutableSubList));

        ArrayList<Job> jobs = collections
            .flatMap(x -> jobs(x))
            .filter(job ->
                nameFilter == null || nameFilter.matcher(job.name()).find())
            .collect(toCollection(ArrayList::new));

        if (reverse) Collections.reverse(jobs);
        if (shuffle) Collections.shuffle(jobs);

        time(jobs);
    }

    @SafeVarargs @SuppressWarnings("varargs")
    private static <T> Stream<T> concatStreams(Stream<T> ... streams) {
        return Stream.of(streams).flatMap(s -> s);
    }

    boolean isMutable(Collection<Integer> x) {
        return !(x.getClass().getName().contains("ImmutableCollections$"));
    }

    Stream<Job> jobs(Collection<Integer> x) {
        return concatStreams(
            collectionJobs(x),

            (isMutable(x))
            ? mutableCollectionJobs(x)
            : Stream.empty(),

            (x instanceof Deque)
            ? dequeJobs((Deque<Integer>)x)
            : Stream.empty(),

            (x instanceof List)
            ? listJobs((List<Integer>)x)
            : Stream.empty(),

            (x instanceof List && isMutable(x))
            ? mutableListJobs((List<Integer>)x)
            : Stream.empty());
    }

    Object sneakyAdder(int[] sneakySum) {
        return new Object() {
            public int hashCode() { throw new AssertionError(); }
            public boolean equals(Object z) {
                sneakySum[0] += (int) z; return false; }};
    }

    Stream<Job> collectionJobs(Collection<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " iterate for loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        for (Integer n : x)
                            sum += n;
                        check.sum(sum);}}},
            new Job(klazz + " iterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.iterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job(klazz + " spliterator().tryAdvance()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        Spliterator<Integer> spliterator = x.spliterator();
                        do {} while (spliterator.tryAdvance(n -> sum[0] += n));
                        check.sum(sum[0]);}}},
            new Job(klazz + " spliterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.spliterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job(klazz + " contains") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Object sneakyAdder = sneakyAdder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.contains(sneakyAdder)) throw new AssertionError();
                        check.sum(sum[0]);}}},
            new Job(klazz + " containsAll") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Collection<Object> sneakyAdderCollection =
                        Collections.singleton(sneakyAdder(sum));
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.containsAll(sneakyAdderCollection))
                            throw new AssertionError();
                        check.sum(sum[0]);}}},
            new Job(klazz + " forEach") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.forEach(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job(klazz + " toArray()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Object o : x.toArray())
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job(klazz + " toArray(a)") {
                public void work() throws Throwable {
                    Integer[] a = new Integer[x.size()];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.toArray(a);
                        for (Object o : a)
                            sum[0] += (Integer) o;
                        check.sum(sum[0]);}}},
            new Job(klazz + " toArray(empty)") {
                public void work() throws Throwable {
                    Integer[] empty = new Integer[0];
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Integer o : x.toArray(empty))
                            sum[0] += o;
                        check.sum(sum[0]);}}},
            new Job(klazz + " stream().forEach") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.stream().forEach(n -> sum[0] += n);
                        check.sum(sum[0]);}}},
            new Job(klazz + " stream().mapToInt") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        check.sum(x.stream().mapToInt(e -> e).sum());}}},
            new Job(klazz + " stream().collect") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        check.sum(x.stream()
                                  .collect(summingInt(e -> e)));}}},
            new Job(klazz + " stream()::iterator") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Integer o : (Iterable<Integer>) x.stream()::iterator)
                            sum[0] += o;
                        check.sum(sum[0]);}}},
            new Job(klazz + " parallelStream().forEach") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        LongAdder sum = new LongAdder();
                        x.parallelStream().forEach(n -> sum.add(n));
                        check.sum((int) sum.sum());}}},
            new Job(klazz + " parallelStream().mapToInt") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        check.sum(x.parallelStream().mapToInt(e -> e).sum());}}},
            new Job(klazz + " parallelStream().collect") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        check.sum(x.parallelStream()
                                  .collect(summingInt(e -> e)));}}},
            new Job(klazz + " parallelStream()::iterator") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        for (Integer o : (Iterable<Integer>) x.parallelStream()::iterator)
                            sum[0] += o;
                        check.sum(sum[0]);}}});
    }

    Stream<Job> mutableCollectionJobs(Collection<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " removeIf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.removeIf(n -> { sum[0] += n; return false; }))
                            throw new AssertionError();
                        check.sum(sum[0]);}}},
            new Job(klazz + " remove(Object)") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Object sneakyAdder = sneakyAdder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.remove(sneakyAdder)) throw new AssertionError();
                        check.sum(sum[0]);}}});
    }

    Stream<Job> dequeJobs(Deque<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " descendingIterator() loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        Iterator<Integer> it = x.descendingIterator();
                        while (it.hasNext())
                            sum += it.next();
                        check.sum(sum);}}},
            new Job(klazz + " descendingIterator().forEachRemaining()") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.descendingIterator().forEachRemaining(n -> sum[0] += n);
                        check.sum(sum[0]);}}});
    }

    Stream<Job> listJobs(List<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " listIterator forward loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        ListIterator<Integer> it = x.listIterator();
                        while (it.hasNext())
                            sum += it.next();
                        check.sum(sum);}}},
            new Job(klazz + " listIterator backward loop") {
                public void work() throws Throwable {
                    for (int i = 0; i < iterations; i++) {
                        int sum = 0;
                        ListIterator<Integer> it = x.listIterator(x.size());
                        while (it.hasPrevious())
                            sum += it.previous();
                        check.sum(sum);}}},
            new Job(klazz + " indexOf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Object sneakyAdder = sneakyAdder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.indexOf(sneakyAdder) != -1)
                            throw new AssertionError();
                        check.sum(sum[0]);}}},
            new Job(klazz + " lastIndexOf") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    Object sneakyAdder = sneakyAdder(sum);
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        if (x.lastIndexOf(sneakyAdder) != -1)
                            throw new AssertionError();
                        check.sum(sum[0]);}}},
            new Job(klazz + " equals") {
                public void work() throws Throwable {
                    ArrayList<Integer> copy = new ArrayList<>(x);
                    for (int i = 0; i < iterations; i++) {
                        if (!x.equals(copy))
                            throw new AssertionError();}}},
            new Job(klazz + " hashCode") {
                public void work() throws Throwable {
                    int hashCode = Arrays.hashCode(x.toArray());
                    for (int i = 0; i < iterations; i++) {
                        if (x.hashCode() != hashCode)
                            throw new AssertionError();}}});
    }

    Stream<Job> mutableListJobs(List<Integer> x) {
        final String klazz = goodClassName(x);
        return Stream.of(
            new Job(klazz + " replaceAll") {
                public void work() throws Throwable {
                    int[] sum = new int[1];
                    UnaryOperator<Integer> sneakyAdder =
                        x -> { sum[0] += x; return x; };
                    for (int i = 0; i < iterations; i++) {
                        sum[0] = 0;
                        x.replaceAll(sneakyAdder);
                        check.sum(sum[0]);}}});
    }
}
