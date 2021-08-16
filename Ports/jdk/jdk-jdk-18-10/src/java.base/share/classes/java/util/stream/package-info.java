/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/**
 * Classes to support functional-style operations on streams of elements, such
 * as map-reduce transformations on collections.  For example:
 *
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(b -> b.getColor() == RED)
 *                      .mapToInt(b -> b.getWeight())
 *                      .sum();
 * }</pre>
 *
 * <p>Here we use {@code widgets}, a {@code Collection<Widget>},
 * as a source for a stream, and then perform a filter-map-reduce on the stream
 * to obtain the sum of the weights of the red widgets.  (Summation is an
 * example of a <a href="package-summary.html#Reduction">reduction</a>
 * operation.)
 *
 * <p>The key abstraction introduced in this package is <em>stream</em>.  The
 * classes {@link java.util.stream.Stream}, {@link java.util.stream.IntStream},
 * {@link java.util.stream.LongStream}, and {@link java.util.stream.DoubleStream}
 * are streams over objects and the primitive {@code int}, {@code long}, and
 * {@code double} types.  Streams differ from collections in several ways:
 *
 * <ul>
 *     <li>No storage.  A stream is not a data structure that stores elements;
 *     instead, it conveys elements from a source such as a data structure,
 *     an array, a generator function, or an I/O channel, through a pipeline of
 *     computational operations.</li>
 *     <li>Functional in nature.  An operation on a stream produces a result,
 *     but does not modify its source.  For example, filtering a {@code Stream}
 *     obtained from a collection produces a new {@code Stream} without the
 *     filtered elements, rather than removing elements from the source
 *     collection.</li>
 *     <li>Laziness-seeking.  Many stream operations, such as filtering, mapping,
 *     or duplicate removal, can be implemented lazily, exposing opportunities
 *     for optimization.  For example, "find the first {@code String} with
 *     three consecutive vowels" need not examine all the input strings.
 *     Stream operations are divided into intermediate ({@code Stream}-producing)
 *     operations and terminal (value- or side-effect-producing) operations.
 *     Intermediate operations are always lazy.</li>
 *     <li>Possibly unbounded.  While collections have a finite size, streams
 *     need not.  Short-circuiting operations such as {@code limit(n)} or
 *     {@code findFirst()} can allow computations on infinite streams to
 *     complete in finite time.</li>
 *     <li>Consumable. The elements of a stream are only visited once during
 *     the life of a stream. Like an {@link java.util.Iterator}, a new stream
 *     must be generated to revisit the same elements of the source.
 *     </li>
 * </ul>
 *
 * Streams can be obtained in a number of ways. Some examples include:
 * <ul>
 *     <li>From a {@link java.util.Collection} via the {@code stream()} and
 *     {@code parallelStream()} methods;</li>
 *     <li>From an array via {@link java.util.Arrays#stream(Object[])};</li>
 *     <li>From static factory methods on the stream classes, such as
 *     {@link java.util.stream.Stream#of(Object[])},
 *     {@link java.util.stream.IntStream#range(int, int)}
 *     or {@link java.util.stream.Stream#iterate(Object, UnaryOperator)};</li>
 *     <li>The lines of a file can be obtained from {@link java.io.BufferedReader#lines()};</li>
 *     <li>Streams of file paths can be obtained from methods in {@link java.nio.file.Files};</li>
 *     <li>Streams of random numbers can be obtained from {@link java.util.Random#ints()};</li>
 *     <li>Numerous other stream-bearing methods in the JDK, including
 *     {@link java.util.BitSet#stream()},
 *     {@link java.util.regex.Pattern#splitAsStream(java.lang.CharSequence)},
 *     and {@link java.util.jar.JarFile#stream()}.</li>
 * </ul>
 *
 * <p>Additional stream sources can be provided by third-party libraries using
 * <a href="package-summary.html#StreamSources">these techniques</a>.
 *
 * <h2><a id="StreamOps">Stream operations and pipelines</a></h2>
 *
 * <p>Stream operations are divided into <em>intermediate</em> and
 * <em>terminal</em> operations, and are combined to form <em>stream
 * pipelines</em>.  A stream pipeline consists of a source (such as a
 * {@code Collection}, an array, a generator function, or an I/O channel);
 * followed by zero or more intermediate operations such as
 * {@code Stream.filter} or {@code Stream.map}; and a terminal operation such
 * as {@code Stream.forEach} or {@code Stream.reduce}.
 *
 * <p>Intermediate operations return a new stream.  They are always
 * <em>lazy</em>; executing an intermediate operation such as
 * {@code filter()} does not actually perform any filtering, but instead
 * creates a new stream that, when traversed, contains the elements of
 * the initial stream that match the given predicate.  Traversal
 * of the pipeline source does not begin until the terminal operation of the
 * pipeline is executed.
 *
 * <p>Terminal operations, such as {@code Stream.forEach} or
 * {@code IntStream.sum}, may traverse the stream to produce a result or a
 * side-effect. After the terminal operation is performed, the stream pipeline
 * is considered consumed, and can no longer be used; if you need to traverse
 * the same data source again, you must return to the data source to get a new
 * stream.  In almost all cases, terminal operations are <em>eager</em>,
 * completing their traversal of the data source and processing of the pipeline
 * before returning.  Only the terminal operations {@code iterator()} and
 * {@code spliterator()} are not; these are provided as an "escape hatch" to enable
 * arbitrary client-controlled pipeline traversals in the event that the
 * existing operations are not sufficient to the task.
 *
 * <p> Processing streams lazily allows for significant efficiencies; in a
 * pipeline such as the filter-map-sum example above, filtering, mapping, and
 * summing can be fused into a single pass on the data, with minimal
 * intermediate state. Laziness also allows avoiding examining all the data
 * when it is not necessary; for operations such as "find the first string
 * longer than 1000 characters", it is only necessary to examine just enough
 * strings to find one that has the desired characteristics without examining
 * all of the strings available from the source. (This behavior becomes even
 * more important when the input stream is infinite and not merely large.)
 *
 * <p>Intermediate operations are further divided into <em>stateless</em>
 * and <em>stateful</em> operations. Stateless operations, such as {@code filter}
 * and {@code map}, retain no state from previously seen element when processing
 * a new element -- each element can be processed
 * independently of operations on other elements.  Stateful operations, such as
 * {@code distinct} and {@code sorted}, may incorporate state from previously
 * seen elements when processing new elements.
 *
 * <p>Stateful operations may need to process the entire input
 * before producing a result.  For example, one cannot produce any results from
 * sorting a stream until one has seen all elements of the stream.  As a result,
 * under parallel computation, some pipelines containing stateful intermediate
 * operations may require multiple passes on the data or may need to buffer
 * significant data.  Pipelines containing exclusively stateless intermediate
 * operations can be processed in a single pass, whether sequential or parallel,
 * with minimal data buffering.
 *
 * <p>Further, some operations are deemed <em>short-circuiting</em> operations.
 * An intermediate operation is short-circuiting if, when presented with
 * infinite input, it may produce a finite stream as a result.  A terminal
 * operation is short-circuiting if, when presented with infinite input, it may
 * terminate in finite time.  Having a short-circuiting operation in the pipeline
 * is a necessary, but not sufficient, condition for the processing of an infinite
 * stream to terminate normally in finite time.
 *
 * <h3><a id="Parallelism">Parallelism</a></h3>
 *
 * <p>Processing elements with an explicit {@code for-}loop is inherently serial.
 * Streams facilitate parallel execution by reframing the computation as a pipeline of
 * aggregate operations, rather than as imperative operations on each individual
 * element.  All streams operations can execute either in serial or in parallel.
 * The stream implementations in the JDK create serial streams unless parallelism is
 * explicitly requested.  For example, {@code Collection} has methods
 * {@link java.util.Collection#stream} and {@link java.util.Collection#parallelStream},
 * which produce sequential and parallel streams respectively; other
 * stream-bearing methods such as {@link java.util.stream.IntStream#range(int, int)}
 * produce sequential streams but these streams can be efficiently parallelized by
 * invoking their {@link java.util.stream.BaseStream#parallel()} method.
 * To execute the prior "sum of weights of widgets" query in parallel, we would
 * do:
 *
 * <pre>{@code
 *     int sumOfWeights = widgets.parallelStream()
 *                               .filter(b -> b.getColor() == RED)
 *                               .mapToInt(b -> b.getWeight())
 *                               .sum();
 * }</pre>
 *
 * <p>The only difference between the serial and parallel versions of this
 * example is the creation of the initial stream, using "{@code parallelStream()}"
 * instead of "{@code stream()}". The stream pipeline is executed sequentially or
 * in parallel depending on the mode of the stream on which the terminal operation
 * is invoked. The sequential or parallel mode of a stream can be determined with the
 * {@link java.util.stream.BaseStream#isParallel()} method, and the
 * stream's mode can be modified with the
 * {@link java.util.stream.BaseStream#sequential()} and
 * {@link java.util.stream.BaseStream#parallel()} operations.
 * The most recent sequential or parallel mode setting applies to the
 * execution of the entire stream pipeline.
 *
 * <p>Except for operations identified as explicitly nondeterministic, such
 * as {@code findAny()}, whether a stream executes sequentially or in parallel
 * should not change the result of the computation.
 *
 * <p>Most stream operations accept parameters that describe user-specified
 * behavior, which are often lambda expressions.  To preserve correct behavior,
 * these <em>behavioral parameters</em> must be <em>non-interfering</em>, and in
 * most cases must be <em>stateless</em>.  Such parameters are always instances
 * of a <a href="../function/package-summary.html">functional interface</a> such
 * as {@link java.util.function.Function}, and are often lambda expressions or
 * method references.
 *
 * <h3><a id="NonInterference">Non-interference</a></h3>
 *
 * Streams enable you to execute possibly-parallel aggregate operations over a
 * variety of data sources, including even non-thread-safe collections such as
 * {@code ArrayList}. This is possible only if we can prevent
 * <em>interference</em> with the data source during the execution of a stream
 * pipeline.  Except for the escape-hatch operations {@code iterator()} and
 * {@code spliterator()}, execution begins when the terminal operation is
 * invoked, and ends when the terminal operation completes.  For most data
 * sources, preventing interference means ensuring that the data source is
 * <em>not modified at all</em> during the execution of the stream pipeline.
 * The notable exception to this are streams whose sources are concurrent
 * collections, which are specifically designed to handle concurrent modification.
 * Concurrent stream sources are those whose {@code Spliterator} reports the
 * {@code CONCURRENT} characteristic.
 *
 * <p>Accordingly, behavioral parameters in stream pipelines whose source might
 * not be concurrent should never modify the stream's data source.
 * A behavioral parameter is said to <em>interfere</em> with a non-concurrent
 * data source if it modifies, or causes to be
 * modified, the stream's data source.  The need for non-interference applies
 * to all pipelines, not just parallel ones.  Unless the stream source is
 * concurrent, modifying a stream's data source during execution of a stream
 * pipeline can cause exceptions, incorrect answers, or nonconformant behavior.
 *
 * For well-behaved stream sources, the source can be modified before the
 * terminal operation commences and those modifications will be reflected in
 * the covered elements.  For example, consider the following code:
 *
 * <pre>{@code
 *     List<String> l = new ArrayList(Arrays.asList("one", "two"));
 *     Stream<String> sl = l.stream();
 *     l.add("three");
 *     String s = sl.collect(joining(" "));
 * }</pre>
 *
 * First a list is created consisting of two strings: "one" and "two". Then a
 * stream is created from that list. Next the list is modified by adding a third
 * string: "three". Finally the elements of the stream are collected and joined
 * together. Since the list was modified before the terminal {@code collect}
 * operation commenced the result will be a string of "one two three". All the
 * streams returned from JDK collections, and most other JDK classes,
 * are well-behaved in this manner; for streams generated by other libraries, see
 * <a href="package-summary.html#StreamSources">Low-level stream
 * construction</a> for requirements for building well-behaved streams.
 *
 * <h3><a id="Statelessness">Stateless behaviors</a></h3>
 *
 * Stream pipeline results may be nondeterministic or incorrect if the behavioral
 * parameters to the stream operations are <em>stateful</em>.  A stateful lambda
 * (or other object implementing the appropriate functional interface) is one
 * whose result depends on any state which might change during the execution
 * of the stream pipeline.  An example of a stateful lambda is the parameter
 * to {@code map()} in:
 *
 * <pre>{@code
 *     Set<Integer> seen = Collections.synchronizedSet(new HashSet<>());
 *     stream.parallel().map(e -> { if (seen.add(e)) return 0; else return e; })...
 * }</pre>
 *
 * Here, if the mapping operation is performed in parallel, the results for the
 * same input could vary from run to run, due to thread scheduling differences,
 * whereas, with a stateless lambda expression the results would always be the
 * same.
 *
 * <p>Note also that attempting to access mutable state from behavioral parameters
 * presents you with a bad choice with respect to safety and performance; if
 * you do not synchronize access to that state, you have a data race and
 * therefore your code is broken, but if you do synchronize access to that
 * state, you risk having contention undermine the parallelism you are seeking
 * to benefit from.  The best approach is to avoid stateful behavioral
 * parameters to stream operations entirely; there is usually a way to
 * restructure the stream pipeline to avoid statefulness.
 *
 * <h3><a id="SideEffects">Side-effects</a></h3>
 *
 * Side-effects in behavioral parameters to stream operations are, in general,
 * discouraged, as they can often lead to unwitting violations of the
 * statelessness requirement, as well as other thread-safety hazards.
 *
 * <p>If the behavioral parameters do have side-effects, unless explicitly
 * stated, there are no guarantees as to:
 * <ul>
 *    <li>the <a href="../concurrent/package-summary.html#MemoryVisibility">
 *    <i>visibility</i></a> of those side-effects to other threads;</li>
 *    <li>that different operations on the "same" element within the same stream
 *    pipeline are executed in the same thread; and</li>
 *    <li>that behavioral parameters are always invoked, since a stream
 *    implementation is free to elide operations (or entire stages) from a
 *    stream pipeline if it can prove that it would not affect the result of the
 *    computation.
 *    </li>
 * </ul>
 * <p>The ordering of side-effects may be surprising.  Even when a pipeline is
 * constrained to produce a <em>result</em> that is consistent with the
 * encounter order of the stream source (for example,
 * {@code IntStream.range(0,5).parallel().map(x -> x*2).toArray()}
 * must produce {@code [0, 2, 4, 6, 8]}), no guarantees are made as to the order
 * in which the mapper function is applied to individual elements, or in what
 * thread any behavioral parameter is executed for a given element.
 *
 * <p>The eliding of side-effects may also be surprising.  With the exception of
 * terminal operations {@link java.util.stream.Stream#forEach forEach} and
 * {@link java.util.stream.Stream#forEachOrdered forEachOrdered}, side-effects
 * of behavioral parameters may not always be executed when the stream
 * implementation can optimize away the execution of behavioral parameters
 * without affecting the result of the computation.  (For a specific example
 * see the API note documented on the {@link java.util.stream.Stream#count count}
 * operation.)
 *
 * <p>Many computations where one might be tempted to use side-effects can be more
 * safely and efficiently expressed without side-effects, such as using
 * <a href="package-summary.html#Reduction">reduction</a> instead of mutable
 * accumulators. However, side-effects such as using {@code println()} for debugging
 * purposes are usually harmless.  A small number of stream operations, such as
 * {@code forEach()} and {@code peek()}, can operate only via side-effects;
 * these should be used with care.
 *
 * <p>As an example of how to transform a stream pipeline that inappropriately
 * uses side-effects to one that does not, the following code searches a stream
 * of strings for those matching a given regular expression, and puts the
 * matches in a list.
 *
 * <pre>{@code
 *     ArrayList<String> results = new ArrayList<>();
 *     stream.filter(s -> pattern.matcher(s).matches())
 *           .forEach(s -> results.add(s));  // Unnecessary use of side-effects!
 * }</pre>
 *
 * This code unnecessarily uses side-effects.  If executed in parallel, the
 * non-thread-safety of {@code ArrayList} would cause incorrect results, and
 * adding needed synchronization would cause contention, undermining the
 * benefit of parallelism.  Furthermore, using side-effects here is completely
 * unnecessary; the {@code forEach()} can simply be replaced with a reduction
 * operation that is safer, more efficient, and more amenable to
 * parallelization:
 *
 * <pre>{@code
 *     List<String> results =
 *         stream.filter(s -> pattern.matcher(s).matches())
 *               .toList();  // No side-effects!
 * }</pre>
 *
 * <h3><a id="Ordering">Ordering</a></h3>
 *
 * <p>Streams may or may not have a defined <em>encounter order</em>.  Whether
 * or not a stream has an encounter order depends on the source and the
 * intermediate operations.  Certain stream sources (such as {@code List} or
 * arrays) are intrinsically ordered, whereas others (such as {@code HashSet})
 * are not.  Some intermediate operations, such as {@code sorted()}, may impose
 * an encounter order on an otherwise unordered stream, and others may render an
 * ordered stream unordered, such as {@link java.util.stream.BaseStream#unordered()}.
 * Further, some terminal operations may ignore encounter order, such as
 * {@code forEach()}.
 *
 * <p>If a stream is ordered, most operations are constrained to operate on the
 * elements in their encounter order; if the source of a stream is a {@code List}
 * containing {@code [1, 2, 3]}, then the result of executing {@code map(x -> x*2)}
 * must be {@code [2, 4, 6]}.  However, if the source has no defined encounter
 * order, then any permutation of the values {@code [2, 4, 6]} would be a valid
 * result.
 *
 * <p>For sequential streams, the presence or absence of an encounter order does
 * not affect performance, only determinism.  If a stream is ordered, repeated
 * execution of identical stream pipelines on an identical source will produce
 * an identical result; if it is not ordered, repeated execution might produce
 * different results.
 *
 * <p>For parallel streams, relaxing the ordering constraint can sometimes enable
 * more efficient execution.  Certain aggregate operations,
 * such as filtering duplicates ({@code distinct()}) or grouped reductions
 * ({@code Collectors.groupingBy()}) can be implemented more efficiently if ordering of elements
 * is not relevant.  Similarly, operations that are intrinsically tied to encounter order,
 * such as {@code limit()}, may require
 * buffering to ensure proper ordering, undermining the benefit of parallelism.
 * In cases where the stream has an encounter order, but the user does not
 * particularly <em>care</em> about that encounter order, explicitly de-ordering
 * the stream with {@link java.util.stream.BaseStream#unordered() unordered()} may
 * improve parallel performance for some stateful or terminal operations.
 * However, most stream pipelines, such as the "sum of weight of blocks" example
 * above, still parallelize efficiently even under ordering constraints.
 *
 * <h2><a id="Reduction">Reduction operations</a></h2>
 *
 * A <em>reduction</em> operation (also called a <em>fold</em>) takes a sequence
 * of input elements and combines them into a single summary result by repeated
 * application of a combining operation, such as finding the sum or maximum of
 * a set of numbers, or accumulating elements into a list.  The streams classes have
 * multiple forms of general reduction operations, called
 * {@link java.util.stream.Stream#reduce(java.util.function.BinaryOperator) reduce()}
 * and {@link java.util.stream.Stream#collect(java.util.stream.Collector) collect()},
 * as well as multiple specialized reduction forms such as
 * {@link java.util.stream.IntStream#sum() sum()}, {@link java.util.stream.IntStream#max() max()},
 * or {@link java.util.stream.IntStream#count() count()}.
 *
 * <p>Of course, such operations can be readily implemented as simple sequential
 * loops, as in:
 * <pre>{@code
 *    int sum = 0;
 *    for (int x : numbers) {
 *       sum += x;
 *    }
 * }</pre>
 * However, there are good reasons to prefer a reduce operation
 * over a mutative accumulation such as the above.  Not only is a reduction
 * "more abstract" -- it operates on the stream as a whole rather than individual
 * elements -- but a properly constructed reduce operation is inherently
 * parallelizable, so long as the function(s) used to process the elements
 * are <a href="package-summary.html#Associativity">associative</a> and
 * <a href="package-summary.html#Statelessness">stateless</a>.
 * For example, given a stream of numbers for which we want to find the sum, we
 * can write:
 * <pre>{@code
 *    int sum = numbers.stream().reduce(0, (x,y) -> x+y);
 * }</pre>
 * or:
 * <pre>{@code
 *    int sum = numbers.stream().reduce(0, Integer::sum);
 * }</pre>
 *
 * <p>These reduction operations can run safely in parallel with almost no
 * modification:
 * <pre>{@code
 *    int sum = numbers.parallelStream().reduce(0, Integer::sum);
 * }</pre>
 *
 * <p>Reduction parallellizes well because the implementation
 * can operate on subsets of the data in parallel, and then combine the
 * intermediate results to get the final correct answer.  (Even if the language
 * had a "parallel for-each" construct, the mutative accumulation approach would
 * still require the developer to provide
 * thread-safe updates to the shared accumulating variable {@code sum}, and
 * the required synchronization would then likely eliminate any performance gain from
 * parallelism.)  Using {@code reduce()} instead removes all of the
 * burden of parallelizing the reduction operation, and the library can provide
 * an efficient parallel implementation with no additional synchronization
 * required.
 *
 * <p>The "widgets" examples shown earlier shows how reduction combines with
 * other operations to replace for-loops with bulk operations.  If {@code widgets}
 * is a collection of {@code Widget} objects, which have a {@code getWeight} method,
 * we can find the heaviest widget with:
 * <pre>{@code
 *     OptionalInt heaviest = widgets.parallelStream()
 *                                   .mapToInt(Widget::getWeight)
 *                                   .max();
 * }</pre>
 *
 * <p>In its more general form, a {@code reduce} operation on elements of type
 * {@code <T>} yielding a result of type {@code <U>} requires three parameters:
 * <pre>{@code
 * <U> U reduce(U identity,
 *              BiFunction<U, ? super T, U> accumulator,
 *              BinaryOperator<U> combiner);
 * }</pre>
 * Here, the <em>identity</em> element is both an initial seed value for the reduction
 * and a default result if there are no input elements. The <em>accumulator</em>
 * function takes a partial result and the next element, and produces a new
 * partial result. The <em>combiner</em> function combines two partial results
 * to produce a new partial result.  (The combiner is necessary in parallel
 * reductions, where the input is partitioned, a partial accumulation computed
 * for each partition, and then the partial results are combined to produce a
 * final result.)
 *
 * <p>More formally, the {@code identity} value must be an <em>identity</em> for
 * the combiner function. This means that for all {@code u},
 * {@code combiner.apply(identity, u)} is equal to {@code u}. Additionally, the
 * {@code combiner} function must be <a href="package-summary.html#Associativity">associative</a> and
 * must be compatible with the {@code accumulator} function: for all {@code u}
 * and {@code t}, {@code combiner.apply(u, accumulator.apply(identity, t))} must
 * be {@code equals()} to {@code accumulator.apply(u, t)}.
 *
 * <p>The three-argument form is a generalization of the two-argument form,
 * incorporating a mapping step into the accumulation step.  We could
 * re-cast the simple sum-of-weights example using the more general form as
 * follows:
 * <pre>{@code
 *     int sumOfWeights = widgets.stream()
 *                               .reduce(0,
 *                                       (sum, b) -> sum + b.getWeight(),
 *                                       Integer::sum);
 * }</pre>
 * though the explicit map-reduce form is more readable and therefore should
 * usually be preferred. The generalized form is provided for cases where
 * significant work can be optimized away by combining mapping and reducing
 * into a single function.
 *
 * <h3><a id="MutableReduction">Mutable reduction</a></h3>
 *
 * A <em>mutable reduction operation</em> accumulates input elements into a
 * mutable result container, such as a {@code Collection} or {@code StringBuilder},
 * as it processes the elements in the stream.
 *
 * <p>If we wanted to take a stream of strings and concatenate them into a
 * single long string, we <em>could</em> achieve this with ordinary reduction:
 * <pre>{@code
 *     String concatenated = strings.reduce("", String::concat)
 * }</pre>
 *
 * <p>We would get the desired result, and it would even work in parallel.  However,
 * we might not be happy about the performance!  Such an implementation would do
 * a great deal of string copying, and the run time would be <em>O(n^2)</em> in
 * the number of characters.  A more performant approach would be to accumulate
 * the results into a {@link java.lang.StringBuilder}, which is a mutable
 * container for accumulating strings.  We can use the same technique to
 * parallelize mutable reduction as we do with ordinary reduction.
 *
 * <p>The mutable reduction operation is called
 * {@link java.util.stream.Stream#collect(Collector) collect()},
 * as it collects together the desired results into a result container such
 * as a {@code Collection}.
 * A {@code collect} operation requires three functions:
 * a supplier function to construct new instances of the result container, an
 * accumulator function to incorporate an input element into a result
 * container, and a combining function to merge the contents of one result
 * container into another.  The form of this is very similar to the general
 * form of ordinary reduction:
 * <pre>{@code
 * <R> R collect(Supplier<R> supplier,
 *               BiConsumer<R, ? super T> accumulator,
 *               BiConsumer<R, R> combiner);
 * }</pre>
 * <p>As with {@code reduce()}, a benefit of expressing {@code collect} in this
 * abstract way is that it is directly amenable to parallelization: we can
 * accumulate partial results in parallel and then combine them, so long as the
 * accumulation and combining functions satisfy the appropriate requirements.
 * For example, to collect the String representations of the elements in a
 * stream into an {@code ArrayList}, we could write the obvious sequential
 * for-each form:
 * <pre>{@code
 *     ArrayList<String> strings = new ArrayList<>();
 *     for (T element : stream) {
 *         strings.add(element.toString());
 *     }
 * }</pre>
 * Or we could use a parallelizable collect form:
 * <pre>{@code
 *     ArrayList<String> strings = stream.collect(() -> new ArrayList<>(),
 *                                                (c, e) -> c.add(e.toString()),
 *                                                (c1, c2) -> c1.addAll(c2));
 * }</pre>
 * or, pulling the mapping operation out of the accumulator function, we could
 * express it more succinctly as:
 * <pre>{@code
 *     List<String> strings = stream.map(Object::toString)
 *                                  .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
 * }</pre>
 * Here, our supplier is just the {@link java.util.ArrayList#ArrayList()
 * ArrayList constructor}, the accumulator adds the stringified element to an
 * {@code ArrayList}, and the combiner simply uses {@link java.util.ArrayList#addAll addAll}
 * to copy the strings from one container into the other.
 *
 * <p>The three aspects of {@code collect} -- supplier, accumulator, and
 * combiner -- are tightly coupled.  We can use the abstraction of a
 * {@link java.util.stream.Collector} to capture all three aspects.  The
 * above example for collecting strings into a {@code List} can be rewritten
 * using a standard {@code Collector} as:
 * <pre>{@code
 *     List<String> strings = stream.map(Object::toString)
 *                                  .collect(Collectors.toList());
 * }</pre>
 *
 * <p>Packaging mutable reductions into a Collector has another advantage:
 * composability.  The class {@link java.util.stream.Collectors} contains a
 * number of predefined factories for collectors, including combinators
 * that transform one collector into another.  For example, suppose we have a
 * collector that computes the sum of the salaries of a stream of
 * employees, as follows:
 *
 * <pre>{@code
 *     Collector<Employee, ?, Integer> summingSalaries
 *         = Collectors.summingInt(Employee::getSalary);
 * }</pre>
 *
 * (The {@code ?} for the second type parameter merely indicates that we don't
 * care about the intermediate representation used by this collector.)
 * If we wanted to create a collector to tabulate the sum of salaries by
 * department, we could reuse {@code summingSalaries} using
 * {@link java.util.stream.Collectors#groupingBy(java.util.function.Function, java.util.stream.Collector) groupingBy}:
 *
 * <pre>{@code
 *     Map<Department, Integer> salariesByDept
 *         = employees.stream().collect(Collectors.groupingBy(Employee::getDepartment,
 *                                                            summingSalaries));
 * }</pre>
 *
 * <p>As with the regular reduction operation, {@code collect()} operations can
 * only be parallelized if appropriate conditions are met.  For any partially
 * accumulated result, combining it with an empty result container must
 * produce an equivalent result.  That is, for a partially accumulated result
 * {@code p} that is the result of any series of accumulator and combiner
 * invocations, {@code p} must be equivalent to
 * {@code combiner.apply(p, supplier.get())}.
 *
 * <p>Further, however the computation is split, it must produce an equivalent
 * result.  For any input elements {@code t1} and {@code t2}, the results
 * {@code r1} and {@code r2} in the computation below must be equivalent:
 * <pre>{@code
 *     A a1 = supplier.get();
 *     accumulator.accept(a1, t1);
 *     accumulator.accept(a1, t2);
 *     R r1 = finisher.apply(a1);  // result without splitting
 *
 *     A a2 = supplier.get();
 *     accumulator.accept(a2, t1);
 *     A a3 = supplier.get();
 *     accumulator.accept(a3, t2);
 *     R r2 = finisher.apply(combiner.apply(a2, a3));  // result with splitting
 * }</pre>
 *
 * <p>Here, equivalence generally means according to {@link java.lang.Object#equals(Object)}.
 * but in some cases equivalence may be relaxed to account for differences in
 * order.
 *
 * <h3><a id="ConcurrentReduction">Reduction, concurrency, and ordering</a></h3>
 *
 * With some complex reduction operations, for example a {@code collect()} that
 * produces a {@code Map}, such as:
 * <pre>{@code
 *     Map<Buyer, List<Transaction>> salesByBuyer
 *         = txns.parallelStream()
 *               .collect(Collectors.groupingBy(Transaction::getBuyer));
 * }</pre>
 * it may actually be counterproductive to perform the operation in parallel.
 * This is because the combining step (merging one {@code Map} into another by
 * key) can be expensive for some {@code Map} implementations.
 *
 * <p>Suppose, however, that the result container used in this reduction
 * was a concurrently modifiable collection -- such as a
 * {@link java.util.concurrent.ConcurrentHashMap}. In that case, the parallel
 * invocations of the accumulator could actually deposit their results
 * concurrently into the same shared result container, eliminating the need for
 * the combiner to merge distinct result containers. This potentially provides
 * a boost to the parallel execution performance. We call this a
 * <em>concurrent</em> reduction.
 *
 * <p>A {@link java.util.stream.Collector} that supports concurrent reduction is
 * marked with the {@link java.util.stream.Collector.Characteristics#CONCURRENT}
 * characteristic.  However, a concurrent collection also has a downside.  If
 * multiple threads are depositing results concurrently into a shared container,
 * the order in which results are deposited is non-deterministic. Consequently,
 * a concurrent reduction is only possible if ordering is not important for the
 * stream being processed. The {@link java.util.stream.Stream#collect(Collector)}
 * implementation will only perform a concurrent reduction if
 * <ul>
 * <li>The stream is parallel;</li>
 * <li>The collector has the
 * {@link java.util.stream.Collector.Characteristics#CONCURRENT} characteristic,
 * and;</li>
 * <li>Either the stream is unordered, or the collector has the
 * {@link java.util.stream.Collector.Characteristics#UNORDERED} characteristic.
 * </ul>
 * You can ensure the stream is unordered by using the
 * {@link java.util.stream.BaseStream#unordered()} method.  For example:
 * <pre>{@code
 *     Map<Buyer, List<Transaction>> salesByBuyer
 *         = txns.parallelStream()
 *               .unordered()
 *               .collect(groupingByConcurrent(Transaction::getBuyer));
 * }</pre>
 * (where {@link java.util.stream.Collectors#groupingByConcurrent} is the
 * concurrent equivalent of {@code groupingBy}).
 *
 * <p>Note that if it is important that the elements for a given key appear in
 * the order they appear in the source, then we cannot use a concurrent
 * reduction, as ordering is one of the casualties of concurrent insertion.
 * We would then be constrained to implement either a sequential reduction or
 * a merge-based parallel reduction.
 *
 * <h3><a id="Associativity">Associativity</a></h3>
 *
 * An operator or function {@code op} is <em>associative</em> if the following
 * holds:
 * <pre>{@code
 *     (a op b) op c == a op (b op c)
 * }</pre>
 * The importance of this to parallel evaluation can be seen if we expand this
 * to four terms:
 * <pre>{@code
 *     a op b op c op d == (a op b) op (c op d)
 * }</pre>
 * So we can evaluate {@code (a op b)} in parallel with {@code (c op d)}, and
 * then invoke {@code op} on the results.
 *
 * <p>Examples of associative operations include numeric addition, min, and
 * max, and string concatenation.
 *
 * <h2><a id="StreamSources">Low-level stream construction</a></h2>
 *
 * So far, all the stream examples have used methods like
 * {@link java.util.Collection#stream()} or {@link java.util.Arrays#stream(Object[])}
 * to obtain a stream.  How are those stream-bearing methods implemented?
 *
 * <p>The class {@link java.util.stream.StreamSupport} has a number of
 * low-level methods for creating a stream, all using some form of a
 * {@link java.util.Spliterator}. A spliterator is the parallel analogue of an
 * {@link java.util.Iterator}; it describes a (possibly infinite) collection of
 * elements, with support for sequentially advancing, bulk traversal, and
 * splitting off some portion of the input into another spliterator which can
 * be processed in parallel.  At the lowest level, all streams are driven by a
 * spliterator.
 *
 * <p>There are a number of implementation choices in implementing a
 * spliterator, nearly all of which are tradeoffs between simplicity of
 * implementation and runtime performance of streams using that spliterator.
 * The simplest, but least performant, way to create a spliterator is to
 * create one from an iterator using
 * {@link java.util.Spliterators#spliteratorUnknownSize(java.util.Iterator, int)}.
 * While such a spliterator will work, it will likely offer poor parallel
 * performance, since we have lost sizing information (how big is the
 * underlying data set), as well as being constrained to a simplistic
 * splitting algorithm.
 *
 * <p>A higher-quality spliterator will provide balanced and known-size
 * splits, accurate sizing information, and a number of other
 * {@link java.util.Spliterator#characteristics() characteristics} of the
 * spliterator or data that can be used by implementations to optimize
 * execution.
 *
 * <p>Spliterators for mutable data sources have an additional challenge;
 * timing of binding to the data, since the data could change between the time
 * the spliterator is created and the time the stream pipeline is executed.
 * Ideally, a spliterator for a stream would report a characteristic of
 * {@code IMMUTABLE} or {@code CONCURRENT}; if not it should be
 * <a href="../Spliterator.html#binding"><em>late-binding</em></a>. If a source
 * cannot directly supply a recommended spliterator, it may indirectly supply
 * a spliterator using a {@code Supplier}, and construct a stream via the
 * {@code Supplier}-accepting versions of
 * {@link java.util.stream.StreamSupport#stream(Supplier, int, boolean) stream()}.
 * The spliterator is obtained from the supplier only after the terminal
 * operation of the stream pipeline commences.
 *
 * <p>These requirements significantly reduce the scope of potential
 * interference between mutations of the stream source and execution of stream
 * pipelines. Streams based on spliterators with the desired characteristics,
 * or those using the Supplier-based factory forms, are immune to
 * modifications of the data source prior to commencement of the terminal
 * operation (provided the behavioral parameters to the stream operations meet
 * the required criteria for non-interference and statelessness).  See
 * <a href="package-summary.html#NonInterference">Non-Interference</a>
 * for more details.
 *
 * @since 1.8
 */
package java.util.stream;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
