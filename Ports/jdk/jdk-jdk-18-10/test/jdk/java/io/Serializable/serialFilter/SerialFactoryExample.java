/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Status;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import static java.io.ObjectInputFilter.Status.ALLOWED;
import static java.io.ObjectInputFilter.Status.REJECTED;
import static java.io.ObjectInputFilter.Status.UNDECIDED;

/* @test
 * @run testng/othervm SerialFactoryExample
 * @run testng/othervm -Djdk.serialFilterFactory=SerialFactoryExample$FilterInThread SerialFactoryExample
 * @summary Test SerialFactoryExample
 */

/*
 * Context-specific Deserialization Filter Example
 *
 * To protect deserialization of a thread or a call to an untrusted library function,
 * a filter is set that applies to every deserialization within the thread.
 *
 * The `doWithSerialFilter` method arguments are a serial filter and
 * a lambda to invoke with the filter in force.  Its implementation creates a stack of filters
 * using a `ThreadLocal`. The stack of filters is composed with the static JVM-wide filter,
 * and an optional stream-specific filter.
 *
 * The FilterInThread filter factory is set as the JVM-wide filter factory.
 * When the filter factory is invoked during the construction of each `ObjectInputStream`,
 * it retrieves the filter(s) from the thread local and combines it with the static JVM-wide filter,
 * and the stream-specific filter.
 *
 * If more than one filter is to be applied to the stream, two filters can be composed
 * using `ObjectInputFilter.merge`.  When invoked, each of the filters is invoked and the results
 * are combined such that if either filter rejects a class, the result is rejected.
 * If either filter allows the class, then it is allowed, otherwise it is undecided.
 * Hierarchies and chains of filters can be built using `ObjectInputFilter.merge`.
 *
 * The `doWithSerialFilter` calls can be nested. When nested, the filters are concatenated.
 */
@Test
public class SerialFactoryExample {

    @DataProvider(name = "Examples")
    static Object[][] examples() {
        return new Object[][]{
                {new Point(1, 2), null,
                        ALLOWED},
                {new Point(1, 2), ObjectInputFilter.Config.createFilter("SerialFactoryExample$Point"),
                        ALLOWED},
                {Integer.valueOf(10), Filters.allowPlatformClasses(),
                        ALLOWED},          // Integer is a platform class
                {new int[10], ObjectInputFilter.Config.createFilter("SerialFactoryExample$Point"),
                        UNDECIDED},          // arrays of primitives are UNDECIDED -> allowed
                {int.class, ObjectInputFilter.Config.createFilter("SerialFactoryExample$Point"),
                        UNDECIDED},          // primitive classes are UNDECIDED -> allowed
                {new Point[] {new Point(1, 1)}, ObjectInputFilter.Config.createFilter("SerialFactoryExample$Point"),
                        ALLOWED},          // Arrays of allowed classes are allowed
                {new Integer[10], ObjectInputFilter.Config.createFilter("SerialFactoryExample$Point"),
                        REJECTED},   // Base component type is checked -> REJECTED
                {new Point(1, 2), ObjectInputFilter.Config.createFilter("!SerialFactoryExample$Point"),
                        REJECTED},   // Denied
                {new Point(1, 3), Filters.allowPlatformClasses(),
                        REJECTED},   // Not a platform class
                {new Point(1, 4), ObjectInputFilter.Config.createFilter("java.lang.Integer"),
                        REJECTED},   // Only Integer is ALLOWED
                {new Point(1, 5), ObjectInputFilter.allowFilter(cl -> cl.getClassLoader() == ClassLoader.getPlatformClassLoader(), UNDECIDED),
                        REJECTED},   // Not platform loader is UNDECIDED -> a class that should not be undecided -> rejected
        };
    }


    @Test(dataProvider = "Examples")
    void examples(Serializable obj, ObjectInputFilter filter, Status expected) {
        // Establish FilterInThread as the application-wide filter factory
        FilterInThread filterInThread;
        if (ObjectInputFilter.Config.getSerialFilterFactory() instanceof FilterInThread fit) {
            // Filter factory selected on the command line with -Djdk.serialFilterFactory=<classname>
            filterInThread = fit;
        } else {
            // Create a FilterInThread filter factory and set
            // An IllegalStateException will be thrown if the filter factory was already
            // initialized to an incompatible filter factory.
            filterInThread = new FilterInThread();
            ObjectInputFilter.Config.setSerialFilterFactory(filterInThread);
        }
        try {
            filterInThread.doWithSerialFilter(filter, () -> {
                byte[] bytes = writeObject(obj);
                Object o = deserializeObject(bytes);
            });
            if (expected.equals(REJECTED))
                Assert.fail("IllegalClassException should have occurred");
        } catch (UncheckedIOException uioe) {
            IOException ioe = uioe.getCause();
            Assert.assertEquals(ioe.getClass(), InvalidClassException.class, "Wrong exception");
            Assert.assertEquals(REJECTED, expected, "Exception should not have occurred");
        }
    }

    /**
     * Test various filters with various objects and the resulting status
     * @param obj an object
     * @param filter a filter
     * @param expected status
     */
    @Test(dataProvider = "Examples")
    void checkStatus(Serializable obj, ObjectInputFilter filter, Status expected) {
        // Establish FilterInThread as the application-wide filter factory
        FilterInThread filterInThread;
        if (ObjectInputFilter.Config.getSerialFilterFactory() instanceof FilterInThread fit) {
            // Filter factory selected on the command line with -Djdk.serialFilterFactory=<classname>
            filterInThread = fit;
        } else {
            // Create a FilterInThread filter factory and set
            // An IllegalStateException will be thrown if the filter factory was already
            // initialized to an incompatible filter factory.
            filterInThread = new FilterInThread();
            ObjectInputFilter.Config.setSerialFilterFactory(filterInThread);
        }

        try {
            filterInThread.doWithSerialFilter(filter, () -> {
                // Classes are serialized as themselves, otherwise pass the object's class
                Class<?> clazz = (obj instanceof Class<?>) ? (Class<?>)obj : obj.getClass();
                ObjectInputFilter.FilterInfo info = new SerialInfo(clazz);
                var compositeFilter = filterInThread.apply(null, ObjectInputFilter.Config.getSerialFilter());
                System.out.println("    filter in effect: " + filterInThread.currFilter);
                if (compositeFilter != null) {
                    Status actualStatus = compositeFilter.checkInput(info);
                    Assert.assertEquals(actualStatus, expected, "Wrong Status");
                }
            });

        } catch (Exception ex) {
            Assert.fail("unexpected exception", ex);
        }
    }

    /**
     * A Context-specific Deserialization Filter Factory to create filters that apply
     * a serial filter to all of the deserializations performed in a thread.
     * The purpose is to establish a deserialization filter that will reject all classes
     * that are not explicitly included.
     * <p>
     * The filter factory creates a composite filter of the stream-specific filter,
     * the thread-specific filter, the static JVM-wide filter, and a filter to reject all UNDECIDED cases.
     * The static JVM-wide filter is always included, if it is configured;
     * see ObjectInputFilter.Config.getSerialFilter().
     * <p>
     * To enable these protections the FilterInThread instance should be set as the
     * JVM-wide filter factory in ObjectInputFilter.Config.setSerialFilterFactory.
     *
     * The {@code doWithSerialFilter} is invoked with a serial filter and a lambda
     * to be invoked after the filter is applied.
     */
    public static final class FilterInThread
            implements BinaryOperator<ObjectInputFilter> {

        // ThreadLocal holding the Deque of serial filters to be applied, not null
        private final ThreadLocal<ArrayDeque<ObjectInputFilter>> filterThreadLocal =
                ThreadLocal.withInitial(() -> new ArrayDeque<>());

        private ObjectInputFilter currFilter;

        /**
         * Construct a FilterInThread deserialization filter factory.
         * The constructor is public so FilterInThread can be set on the command line
         * with {@code -Djdk.serialFilterFactory=SerialFactoryExample$FilterInThread}.
         */
        public FilterInThread() {
        }

        /**
         * Applies the filter to the thread and invokes the runnable.
         * The filter is pushed to a ThreadLocal, saving the old value.
         * If there was a previous thread filter, the new filter is appended
         * and made the active filter.
         * The runnable is invoked.
         * The previous filter is restored to the ThreadLocal.
         *
         * @param filter the serial filter to apply
         * @param runnable a runnable to invoke
         */
        public void doWithSerialFilter(ObjectInputFilter filter, Runnable runnable) {
            var prevFilters = filterThreadLocal.get();
            try {
                if (filter != null)
                    prevFilters.addLast(filter);
                runnable.run();
            } finally {
                if (filter != null) {
                    var lastFilter = prevFilters.removeLast();
                    assert lastFilter == filter : "Filter removed out of order";
                }
            }
        }

        /**
         * Returns a composite filter of the stream-specific filter, the thread-specific filter,
         * the static JVM-wide filter, and a filter to reject all UNDECIDED cases.
         * The purpose is to establish a deserialization filter that will reject all classes
         * that are not explicitly included.
         * The static JVM-wide filter is always checked, if it is configured;
         * see ObjectInputFilter.Config.getSerialFilter().
         * Any or all of the filters are optional and if not supplied or configured are null.
         * <p>
         * This method is first called from the constructor with current == null and
         * next == static JVM-wide filter.
         * The filter returned is the static JVM-wide filter merged with the thread-specific filter
         * and followed by a filter to map all UNDECIDED status values to REJECTED.
         * This last step ensures that the collective group of filters covers every possible case,
         * any classes that are not ALLOWED will be REJECTED.
         * <p>
         * The method may be called a second time from {@code ObjectInputStream.setObjectInputFilter(next)}
         * to add a stream-specific filter.  The stream-specific filter is prepended to the
         * composite filter created above when called from the constructor.
         * <p>
         *
         * @param curr the current filter, may be null
         * @param next the next filter, may be null
         * @return a deserialization filter to use for the stream, may be null
         */
        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            if (curr == null) {
                // Called from the OIS constructor or perhaps OIS.setObjectInputFilter with no current filter
                // no current filter, prepend next to threadFilter, both may be null or non-null

                // Assemble the filters in sequence, most recently added first
                var filters = filterThreadLocal.get();
                ObjectInputFilter filter = null;
                for (ObjectInputFilter f : filters) {
                    filter = ObjectInputFilter.merge(f, filter);
                }
                if (next != null) {
                    // Prepend a filter to reject all UNDECIDED results
                    if (filter != null) {
                        filter = ObjectInputFilter.rejectUndecidedClass(filter);
                    }

                    // Prepend the next filter to the thread filter, if any
                    // Initially this would be the static JVM-wide filter passed from the OIS constructor
                    // The static JVM-wide filter allow, reject, or leave classes undecided
                    filter = ObjectInputFilter.merge(next, filter);
                }
                // Check that the static JVM-wide filter did not leave any classes undecided
                if (filter != null) {
                    // Append the filter to reject all UNDECIDED results
                    filter = ObjectInputFilter.rejectUndecidedClass(filter);
                }
                // Return the filter, unless a stream-specific filter is set later
                // The filter may be null if no filters are configured
                currFilter = filter;
                return currFilter;
            } else {
                // Called from OIS.setObjectInputFilter with a previously set filter.
                // The curr filter already incorporates the thread filter and rejection of undecided status
                // Prepend the stream-specific filter or the current filter if no stream-specific filter
                currFilter = (next == null) ? curr : ObjectInputFilter.rejectUndecidedClass(ObjectInputFilter.merge(next, curr));
                return currFilter;
            }
        }

        public String toString() {
            return Objects.toString(currFilter, "none");
        }
    }


    /**
     * Simple example code from the ObjectInputFilter Class javadoc.
     */
    public static final class SimpleFilterInThread implements BinaryOperator<ObjectInputFilter> {

        // ThreadLocal to hold the serial filter to be applied
        private final ThreadLocal<ObjectInputFilter> filterThreadLocal = new ThreadLocal<>();

        // Construct a FilterInThread deserialization filter factory.
        public SimpleFilterInThread() {}

        /**
         * The filter factory, which is invoked every time a new ObjectInputStream
         * is created.  If a per-stream filter is already set then it returns a
         * filter that combines the results of invoking each filter.
         *
         * @param curr the current filter on the stream
         * @param next a per stream filter
         * @return the selected filter
         */
        public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
            if (curr == null) {
                // Called from the OIS constructor or perhaps OIS.setObjectInputFilter with no current filter
                var filter = filterThreadLocal.get();
                if (filter != null) {
                    // Prepend a filter to reject all UNDECIDED results
                    filter = ObjectInputFilter.rejectUndecidedClass(filter);
                }
                if (next != null) {
                    // Prepend the next filter to the thread filter, if any
                    // Initially this is the static JVM-wide filter passed from the OIS constructor
                    // Append the filter to reject all UNDECIDED results
                    filter = ObjectInputFilter.merge(next, filter);
                    filter = ObjectInputFilter.rejectUndecidedClass(filter);
                }
                return filter;
            } else {
                // Called from OIS.setObjectInputFilter with a current filter and a stream-specific filter.
                // The curr filter already incorporates the thread filter and static JVM-wide filter
                // and rejection of undecided classes
                // If there is a stream-specific filter prepend it and a filter to recheck for undecided
                if (next != null) {
                    next = ObjectInputFilter.merge(next, curr);
                    next = ObjectInputFilter.rejectUndecidedClass(next);
                    return next;
                }
                return curr;
            }
        }

        /**
         * Applies the filter to the thread and invokes the runnable.
         *
         * @param filter the serial filter to apply to every deserialization in the thread
         * @param runnable a Runnable to invoke
         */
        public void doWithSerialFilter(ObjectInputFilter filter, Runnable runnable) {
            var prevFilter = filterThreadLocal.get();
            try {
                filterThreadLocal.set(filter);
                runnable.run();
            } finally {
                filterThreadLocal.set(prevFilter);
            }
        }
    }

    /**
     * Write an object and return a byte array with the bytes.
     *
     * @param object object to serialize
     * @return the byte array of the serialized object
     * @throws UncheckedIOException if an exception occurs
     */
    private static byte[] writeObject(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Deserialize an object.
     *
     * @param bytes an object.
     * @throws UncheckedIOException for I/O exceptions and ClassNotFoundException
     */
    private static Object deserializeObject(byte[] bytes) {
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);
            System.out.println("  filter in effect: " + ois.getObjectInputFilter());
            return ois.readObject();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new UncheckedIOException(new InvalidClassException(cnfe.getMessage()));
        }
    }


    /**
     * ObjectInputFilter utilities to create filters that combine the results of other filters.
     */
    public static final class Filters {
        /**
         * Returns a filter that returns {@code Status.ALLOWED} if the predicate
         * on the class is {@code true}.
         * The filter returns {@code ALLOWED} or the {@code otherStatus} based on the predicate
         * of the {@code non-null} class and {@code UNDECIDED} if the class is {@code null}.
         *
         * <p>When the filter's {@link ObjectInputFilter#checkInput checkInput(info)} method is invoked,
         * the predicate is applied to the {@link ObjectInputFilter.FilterInfo#serialClass() info.serialClass()},
         * the return Status is:
         * <ul>
         *     <li>{@link Status#UNDECIDED UNDECIDED}, if the {@code serialClass} is {@code null},</li>
         *     <li>{@link Status#ALLOWED ALLOWED}, if the predicate on the class returns {@code true},</li>
         *     <li>Otherwise, return {@code otherStatus}.</li>
         * </ul>
         * <p>
         * Example, to create a filter that will allow any class loaded from the platform classloader.
         * <pre><code>
         *     ObjectInputFilter f = allowFilter(cl -> cl.getClassLoader() == ClassLoader.getPlatformClassLoader()
         *                                          || cl.getClassLoader() == null, Status.UNDECIDED);
         * </code></pre>
         *
         * @param predicate a predicate to test a non-null Class, non-null
         * @param otherStatus a Status to use if the predicate is {@code false}
         * @return a filter than returns {@code ALLOWED} if the predicate on the class returns {@code true},
         *          otherwise the {@code otherStatus}
         * @since 17
         */
        public static ObjectInputFilter allowFilter(Predicate<Class<?>> predicate, Status otherStatus) {
            Objects.requireNonNull(predicate, "predicate");
            Objects.requireNonNull(otherStatus, "otherStatus");
            return new PredicateFilter(predicate, ALLOWED, otherStatus);
        }

        /**
         * Returns a filter that returns {@code Status.REJECTED} if the predicate
         * on the class is {@code true}.
         * The filter returns {@code ALLOWED} or the {@code otherStatus} based on the predicate
         * of the {@code non-null} class and {@code UNDECIDED} if the class is {@code null}.
         *
         * When the filter's {@link ObjectInputFilter#checkInput checkInput(info)} method is invoked,
         * the predicate is applied to the {@link ObjectInputFilter.FilterInfo#serialClass() serialClass()},
         * the return Status is:
         * <ul>
         *     <li>{@link Status#UNDECIDED UNDECIDED}, if the {@code serialClass} is {@code null},</li>
         *     <li>{@link Status#REJECTED REJECTED}, if the predicate on the class returns {@code true},</li>
         *     <li>Otherwise, return {@code otherStatus}.</li>
         * </ul>
         * <p>
         * Example, to create a filter that will reject any class loaded from the application classloader.
         * <pre><code>
         *     ObjectInputFilter f = rejectFilter(cl ->
         *          cl.getClassLoader() == ClassLoader.ClassLoader.getSystemClassLoader(), Status.UNDECIDED);
         * </code></pre>
         *
         * @param predicate a predicate to test a non-null Class, non-null
         * @param otherStatus a Status to use if the predicate is {@code false}
         * @return returns a filter that returns {@link Status#REJECTED REJECTED} if the predicate on the class
         *          returns {@code true}, otherwise {@link Status#UNDECIDED UNDECIDED}
         * @since 17
         */
        public static ObjectInputFilter rejectFilter(Predicate<Class<?>> predicate, Status otherStatus) {
            Objects.requireNonNull(predicate, "predicate");
            Objects.requireNonNull(otherStatus, "otherStatus");
            return new PredicateFilter(predicate, REJECTED, otherStatus);
        }

        /**
         * Returns a filter that returns {@code Status.ALLOWED} if the check is for limits
         * and not checking a class; otherwise {@code Status.UNDECIDED}.
         * If the {@link ObjectInputFilter.FilterInfo#serialClass() serialClass()} is {@code null}, the filter returns
         * {@code Status.ALLOWED}, otherwise return {@code Status.UNDECIDED}.
         * The limit values of {@link ObjectInputFilter.FilterInfo#arrayLength() arrayLength()},
         * {@link ObjectInputFilter.FilterInfo#depth() depth()}, {@link ObjectInputFilter.FilterInfo#references() references()},
         * and {@link ObjectInputFilter.FilterInfo#streamBytes() streamBytes()} are not checked.
         * To place a limit, create a separate filter with limits such as:
         * <pre>{@code
         * Config.createFilter("maxarray=10000,maxdepth=40");
         * }</pre>
         *
         * When the filter's {@link ObjectInputFilter#checkInput} method is invoked,
         * the Status returned is:
         * <ul>
         *     <li>{@link Status#ALLOWED ALLOWED}, if the {@code serialClass} is {@code null},</li>
         *     <li>Otherwise, return {@link Status#UNDECIDED UNDECIDED}</li>
         * </ul>
         *
         * @return a filter that returns {@code Status.ALLOWED} if the check is for limits
         *          and not checking a class; otherwise {@code Status.UNDECIDED}
         * @since 17
         */
        public static ObjectInputFilter allowMaxLimits() {
            return new AllowMaxLimitsFilter(ALLOWED, UNDECIDED);
        }

        /**
         * Returns a filter that merges the status of a filter and another filter.
         * If the other filter is {@code null}, the filter is returned.
         * Otherwise, a filter is returned to merge the pair of {@code non-null} filters.
         *
         * The filter returned implements the {@link ObjectInputFilter#checkInput(ObjectInputFilter.FilterInfo)} method
         * as follows:
         * <ul>
         *     <li>Invoke {@code filter} on the {@code FilterInfo} to get its {@code status};
         *     <li>Return {@code REJECTED} if the {@code status} is {@code REJECTED};
         *     <li>Invoke the {@code otherFilter} to get the {@code otherStatus};
         *     <li>Return {@code REJECTED} if the {@code otherStatus} is {@code REJECTED};
         *     <li>Return {@code ALLOWED}, if either {@code status} or {@code otherStatus}
         *          is {@code ALLOWED}, </li>
         *     <li>Otherwise, return {@code UNDECIDED}</li>
         * </ul>
         *
         * @param filter a filter, non-null
         * @param anotherFilter a filter to be merged with the filter, may be {@code null}
         * @return an {@link ObjectInputFilter} that merges the status of the filter and another filter
         * @since 17
         */
        public static ObjectInputFilter merge(ObjectInputFilter filter, ObjectInputFilter anotherFilter) {
            Objects.requireNonNull(filter, "filter");
            return (anotherFilter == null) ? filter : new MergeFilter(filter, anotherFilter);
        }

        /**
         * Returns a filter that invokes a filter and maps {@code UNDECIDED} to {@code REJECTED}
         * for classes, with some exceptions, and otherwise returns the status.
         * The filter returned checks that classes not {@code ALLOWED} and not {@code REJECTED} by the filter
         * are {@code REJECTED}, if the class is an array and the base component type is not allowed,
         * otherwise the result is {@code UNDECIDED}.
         *
         * <p>
         * Object deserialization accepts a class if the filter returns {@code UNDECIDED}.
         * Adding a filter to reject undecided results for classes that have not been
         * either allowed or rejected can prevent classes from slipping through the filter.
         *
         * @implSpec
         * The filter returned implements the {@link ObjectInputFilter#checkInput(ObjectInputFilter.FilterInfo)} method
         * as follows:
         * <ul>
         *     <li>Invoke the filter on the {@code FilterInfo} to get its {@code status};
         *     <li>Return the {@code status} if the status is {@code REJECTED} or {@code ALLOWED};
         *     <li>Return {@code UNDECIDED} if the {@code filterInfo.getSerialClass() serialClass}
         *          is {@code null};
         *     <li>Determine the base component type if the {@code serialClass} is
         *          an {@linkplain Class#isArray() array};
         *     <li>Return {@code UNDECIDED} if the base component type is
         *          a {@linkplain Class#isPrimitive() primitive class};
         *     <li>Invoke the filter on the {@code base component type} to get its
         *          {@code component status};</li>
         *     <li>Return {@code ALLOWED} if the component status is {@code ALLOWED};
         *     <li>Otherwise, return {@code REJECTED}.</li>
         * </ul>
         *
         * @param filter a filter, non-null
         * @return an {@link ObjectInputFilter} that maps an {@link Status#UNDECIDED}
         *      status to {@link Status#REJECTED} for classes, otherwise returns the
         *      filter status
         * @since 17
         */
        public static ObjectInputFilter rejectUndecidedClass(ObjectInputFilter filter) {
            Objects.requireNonNull(filter, "filter");
            return new RejectUndecidedFilter(filter);
        }

        /**
         * Returns a filter that allows a class only if the class was loaded by the platform class loader.
         * Otherwise, it returns UNDECIDED; leaving the choice to another filter.
         * @return a filter that allows a class only if the class was loaded by the platform class loader
         */
        public static ObjectInputFilter allowPlatformClasses() {
            return new AllowPlatformClassFilter();
        }

        /**
         * An ObjectInputFilter to evaluate a predicate mapping a class to a boolean.
         */
        private static class PredicateFilter implements ObjectInputFilter {
            private final Predicate<Class<?>> predicate;
            private final Status ifTrueStatus;
            private final Status ifFalseStatus;

            PredicateFilter(Predicate<Class<?>> predicate, Status ifTrueStatus, Status ifFalseStatus) {
                this.predicate = predicate;
                this.ifTrueStatus = ifTrueStatus;
                this.ifFalseStatus = ifFalseStatus;
            }

            /**
             * Returns a filter that returns {@code ifTrueStatus} or the {@code ifFalseStatus}
             * based on the predicate of the {@code non-null} class and {@code UNDECIDED}
             * if the class is {@code null}.
             *
             * @param info the FilterInfo
             * @return a filter that returns {@code ifTrueStatus} or the {@code ifFalseStatus}
             *          based on the predicate of the {@code non-null} class and {@code UNDECIDED}
             *          if the class is {@code null}
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
                Class<?> clazz = info.serialClass();
                Status status = (clazz == null) ? UNDECIDED
                        : (predicate.test(clazz)) ? ifTrueStatus : ifFalseStatus;
                return status;
            }

            /**
             * Return a String describing the filter, its predicate, and true and false status values.
             * @return a String describing the filter, its predicate, and true and false status values.
             */
            public String toString() {
                return "predicate(" + predicate + ", ifTrue: " + ifTrueStatus + ", ifFalse:" + ifFalseStatus+ ")";
            }
        }

        /**
         * An ObjectInputFilter to evaluate if a FilterInfo is checking only limits,
         * and not classes.
         */
        private static class AllowMaxLimitsFilter implements ObjectInputFilter {
            private final Status limitCheck;
            private final Status classCheck;

            AllowMaxLimitsFilter(Status limitCheck, Status classCheck) {
                this.limitCheck = limitCheck;
                this.classCheck = classCheck;
            }

            /**
             * If the FilterInfo is only checking a limit, return the requested
             * status, otherwise the other status.
             *
             * @param info the FilterInfo
             * @return the status of corresponding to serialClass == null or not
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
                return (info.serialClass() == null) ? limitCheck : classCheck;
            }

            public String toString() {
                return "allowMaxLimits()";
            }
        }

        /**
         * An ObjectInputFilter that merges the status of two filters.
         */
        private static class MergeFilter implements ObjectInputFilter {
            private final ObjectInputFilter first;
            private final ObjectInputFilter second;

            MergeFilter(ObjectInputFilter first, ObjectInputFilter second) {
                this.first = first;
                this.second = second;
            }

            /**
             * Returns REJECTED if either of the filters returns REJECTED,
             * and ALLOWED if either of the filters returns ALLOWED.
             * Returns {@code UNDECIDED} if either filter returns {@code UNDECIDED}.
             *
             * @param info the FilterInfo
             * @return Status.REJECTED if either of the filters returns REJECTED,
             * and ALLOWED if either filter returns ALLOWED; otherwise returns
             * {@code UNDECIDED} if both filters returned {@code UNDECIDED}
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
                Status firstStatus = Objects.requireNonNull(first.checkInput(info), "status");
                if (REJECTED.equals(firstStatus)) {
                    return REJECTED;
                }
                Status secondStatus = Objects.requireNonNull(second.checkInput(info), "other status");
                if (REJECTED.equals(secondStatus)) {
                    return REJECTED;
                }
                if (ALLOWED.equals(firstStatus) || ALLOWED.equals(secondStatus)) {
                    return ALLOWED;
                }
                return UNDECIDED;
            }

            @Override
            public String toString() {
                return "merge(" + first + ", " + second + ")";
            }
        }

        /**
         * A filter that maps the status {@code UNDECIDED} to {@code REJECTED} when checking a class.
         */
        private static class RejectUndecidedFilter implements ObjectInputFilter {
            private final ObjectInputFilter filter;

            private RejectUndecidedFilter(ObjectInputFilter filter) {
                this.filter = Objects.requireNonNull(filter, "filter");
            }

            /**
             * Apply the filter and return the status if not UNDECIDED and checking a class.
             * For array classes, re-check the final component type against the filter.
             * Make an exception for Primitive classes that are implicitly allowed by the pattern based filter.
             * @param info the FilterInfo
             * @return the status of applying the filter and checking the class
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
                Status status = Objects.requireNonNull(filter.checkInput(info), "status");
                Class<?> clazz = info.serialClass();
                if (clazz == null || !UNDECIDED.equals(status))
                    return status;
                status = REJECTED;
                // Find the base component type
                while (clazz.isArray()) {
                    clazz = clazz.getComponentType();
                }
                if (clazz.isPrimitive()) {
                    status = UNDECIDED;
                } else {
                    // for non-primitive types;  re-filter the base component type
                    FilterInfo clazzInfo = new SerialInfo(info, clazz);
                    Status clazzStatus = filter.checkInput(clazzInfo);
                    status = (ALLOWED.equals(clazzStatus)) ? ALLOWED : REJECTED;
                }
                return status;
            }

            public String toString() {
                return "rejectUndecidedClass(" + filter + ")";
            }

            /**
             * FilterInfo instance with a specific class and delegating to an existing FilterInfo.
             * Nested in the rejectUndecided class.
             */
            static class SerialInfo implements ObjectInputFilter.FilterInfo {
                private final FilterInfo base;
                private final Class<?> clazz;

                SerialInfo(FilterInfo base, Class<?> clazz) {
                    this.base = base;
                    this.clazz = clazz;
                }

                @Override
                public Class<?> serialClass() {
                    return clazz;
                }

                @Override
                public long arrayLength() {
                    return base.arrayLength();
                }

                @Override
                public long depth() {
                    return base.depth();
                }

                @Override
                public long references() {
                    return base.references();
                }

                @Override
                public long streamBytes() {
                    return base.streamBytes();
                }
            }

        }

        /**
         * An ObjectInputFilter that merges the results of two filters.
         */
        private static class MergeManyFilter implements ObjectInputFilter {
            private final List<ObjectInputFilter> filters;
            private final Status otherStatus;

            MergeManyFilter(List<ObjectInputFilter> first, Status otherStatus) {
                this.filters = Objects.requireNonNull(first, "filters");
                this.otherStatus = Objects.requireNonNull(otherStatus, "otherStatus");
            }

            /**
             * Returns REJECTED if any of the filters returns REJECTED,
             * and ALLOWED if any of the filters returns ALLOWED.
             * Returns UNDECIDED if there is no class to be checked or all filters return UNDECIDED.
             *
             * @param info the FilterInfo
             * @return Status.UNDECIDED if there is no class to check,
             *      Status.REJECTED if any of the filters returns REJECTED,
             *      Status.ALLOWED if any filter returns ALLOWED;
             *      otherwise returns {@code otherStatus}
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
                if (info.serialClass() == null)
                    return UNDECIDED;
                Status status = otherStatus;
                for (ObjectInputFilter filter : filters) {
                    Status aStatus = filter.checkInput(info);
                    if (REJECTED.equals(aStatus)) {
                        return REJECTED;
                    }
                    if (ALLOWED.equals(aStatus)) {
                        status = ALLOWED;
                    }
                }
                return status;
            }

            @Override
            public String toString() {
                return "mergeManyFilter(" + filters + ")";
            }
        }

        /**
         * An ObjectInputFilter that allows a class only if the class was loaded by the platform class loader.
         * Otherwise, it returns undecided; leaving the choice to another filter.
         */
        private static class AllowPlatformClassFilter implements ObjectInputFilter {

            /**
             * Returns ALLOWED only if the class, if non-null, was loaded by the platformClassLoader.
             *
             * @param filter the FilterInfo
             * @return Status.ALLOWED only if the class loader of the class was the PlatformClassLoader;
             * otherwise Status.UNDECIDED
             */
            public ObjectInputFilter.Status checkInput(FilterInfo filter) {
                final Class<?> serialClass = filter.serialClass();
                return (serialClass != null &&
                        (serialClass.getClassLoader() == null ||
                        ClassLoader.getPlatformClassLoader().equals(serialClass.getClassLoader())))
                        ? ObjectInputFilter.Status.ALLOWED
                        : ObjectInputFilter.Status.UNDECIDED;
            }

            public String toString() {
                return "allowPlatformClasses";
            }
        }
    }

    /**
     * FilterInfo instance with a specific class.
     */
    static class SerialInfo implements ObjectInputFilter.FilterInfo {
        private final Class<?> clazz;

        SerialInfo(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> serialClass() {
            return clazz;
        }

        @Override
        public long arrayLength() {
            return 0;
        }

        @Override
        public long depth() {
            return 0;
        }

        @Override
        public long references() {
            return 0;
        }

        @Override
        public long streamBytes() {
            return 0;
        }
    }

    /**
     * A test class.
     */
    static record Point(int x, int y) implements Serializable {
    }
}
