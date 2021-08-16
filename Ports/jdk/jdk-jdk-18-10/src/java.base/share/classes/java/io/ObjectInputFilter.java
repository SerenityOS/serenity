/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import jdk.internal.access.SharedSecrets;
import jdk.internal.util.StaticProperty;
import sun.security.action.GetBooleanAction;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.io.ObjectInputFilter.Status.*;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

/**
 * Filter classes, array lengths, and graph metrics during deserialization.
 *
 * <p><strong>Warning: Deserialization of untrusted data is inherently dangerous
 * and should be avoided. Untrusted data should be carefully validated according to the
 * "Serialization and Deserialization" section of the
 * {@extLink secure_coding_guidelines_javase Secure Coding Guidelines for Java SE}.
 * {@extLink serialization_filter_guide Serialization Filtering} describes best
 * practices for defensive use of serial filters.
 * </strong></p>
 *
 * <p>To protect against deserialization vulnerabilities, application developers
 * need a clear description of the objects that can be deserialized
 * by each component or library. For each context and use case, developers should
 * construct and apply an appropriate filter.
 *
 * <h2>Deserialization Filtering Factory and Filters</h2>
 * The parts of deserialization filtering are the filters, composite filters, and filter factory.
 * Each filter performs checks on classes and resource limits to determine the status as
 * rejected, allowed, or undecided.
 * Filters can be composed of other filters and merge or combine their results.
 * The filter factory is responsible for establishing and updating the filter
 * for each {@link ObjectInputStream}.
 *
 * <p>For simple cases, a static JVM-wide filter can be set for the entire application,
 * without setting a filter factory.
 * The JVM-wide filter can be set either with a system property on the command line or by
 * calling {@linkplain Config#setSerialFilter(ObjectInputFilter) Config.setSerialFilter}.
 * No custom filter factory needs to be specified, defaulting to the builtin filter factory.
 * The builtin filter factory provides the {@linkplain Config#getSerialFilter static JVM-wide filter}
 * for each {@linkplain ObjectInputStream ObjectInputStream}.
 *
 * <p>For example, a filter that allows example classes, allows classes in the
 * {@code java.base} module, and rejects all other classes can be set:
 *
 * <pre>{@code As a command line property:
 *     % java -Djdk.serialFilter="example.*;java.base/*;!*" ...}</pre>
 *
 * <pre>{@code Or programmatically:
 *     var filter = ObjectInputFilter.Config.createFilter("example.*;java.base/*;!*")
 *     ObjectInputFilter.Config.setSerialFilter(filter);}</pre>
 *
 * <p>In an application with multiple execution contexts, the application can provide a
 * {@linkplain Config#setSerialFilterFactory(BinaryOperator) filter factory} to
 * protect individual contexts by providing a custom filter for each. When the stream
 * is constructed, the filter factory is called to identify the execution context from the available
 * information, including the current thread-local state, hierarchy of callers, library, module,
 * and class loader. At that point, the filter factory policy for creating or selecting filters
 * can choose a specific filter or composition of filters based on the context.
 * The JVM-wide deserialization filter factory ensures that a context-specific deserialization
 * filter can be set on every {@link ObjectInputStream} and every object read from the
 * stream can be checked.
 *
 * <h2>Invoking the Filter Factory</h2>
 * <p>The JVM-wide filter factory is a function invoked when each {@link ObjectInputStream} is
 * {@linkplain ObjectInputStream#ObjectInputStream() constructed} and when the
 * {@linkplain ObjectInputStream#setObjectInputFilter(ObjectInputFilter) stream-specific filter is set}.
 * The parameters are the current filter and a requested filter and it
 * returns the filter to be used for the stream. When invoked from the
 * {@linkplain ObjectInputStream#ObjectInputStream(InputStream) ObjectInputStream constructors},
 * the first parameter is {@code null} and the second parameter is the
 * {@linkplain ObjectInputFilter.Config#getSerialFilter() static JVM-wide filter}.
 * When invoked from {@link ObjectInputStream#setObjectInputFilter ObjectInputStream.setObjectInputFilter},
 * the first parameter is the filter currently set on the stream (which was set in the constructor),
 * and the second parameter is the filter given to {@code ObjectInputStream.setObjectInputFilter}.
 * The current and new filter may each be {@code null} and the factory may return {@code null}.
 * Note that the filter factory implementation can also use any contextual information
 * at its disposal, for example, extracted from the application thread context, or its call stack,
 * to compose and combine a new filter. It is not restricted to only use its two parameters.
 *
 * <p>The active deserialization filter factory is either:
 * <ul>
 * <li>The application specific filter factory set via {@link Config#setSerialFilterFactory(BinaryOperator)}
 *     or the system property {@code jdk.serialFilterFactory} or
 *     the security property {@code jdk.serialFilterFactory}.
 * <li>Otherwise, a builtin deserialization filter factory
 *     provides the {@linkplain Config#getSerialFilter static JVM-wide filter} when invoked from the
 *     {@linkplain ObjectInputStream#ObjectInputStream(InputStream) ObjectInputStream constructors}
 *     and replaces the static filter when invoked from
 *     {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter)}.
 *     See {@linkplain Config#getSerialFilterFactory() getSerialFilterFactory}.
 * </ul>
 *
 * <h2>Filters</h2>
 * Filters can be created from a {@linkplain Config#createFilter(String) pattern string},
 * or based on a {@linkplain Predicate predicate of a class} to
 * {@linkplain #allowFilter(Predicate, Status) allow} or
 * {@linkplain #rejectFilter(Predicate, Status) reject} classes.
 *
 * <p>The filter's {@link #checkInput checkInput(FilterInfo)} method is invoked
 * zero or more times while {@linkplain ObjectInputStream#readObject() reading objects}.
 * The method is called to validate classes, the length of each array,
 * the number of objects being read from the stream, the depth of the graph,
 * and the total number of bytes read from the stream.
 *
 * <p>Composite filters combine or check the results of other filters.
 * The {@link #merge(ObjectInputFilter, ObjectInputFilter) merge(filter, anotherFilter)}
 * filter combines the status value of two filters.
 * The {@link #rejectUndecidedClass(ObjectInputFilter) rejectUndecidedClass(filter)}
 * checks the result of a filter for classes when the status is {@code UNDECIDED}.
 * In many cases any class not {@code ALLOWED} by the filter should be {@code REJECTED}.
 * <p>
 * A deserialization filter determines whether the arguments are allowed or rejected and
 * should return the appropriate status: {@link Status#ALLOWED ALLOWED} or {@link Status#REJECTED REJECTED}.
 * If the filter cannot determine the status it should return {@link Status#UNDECIDED UNDECIDED}.
 * Filters should be designed for the specific use case and expected types.
 * A filter designed for a particular use may be passed a class outside
 * of the scope of the filter. If the purpose of the filter is to reject classes
 * then it can reject a candidate class that matches and report {@code UNDECIDED} for others.
 * A filter may be called with class equals {@code null}, {@code arrayLength} equal -1,
 * the depth, number of references, and stream size and return a status
 * that reflects only one or only some of the values.
 * This allows a filter to be specific about the choice it is reporting and
 * to use other filters without forcing either allowed or rejected status.
 *
 * <h2>Filter Model Examples</h2>
 * For simple applications, a single predefined filter listing allowed or rejected
 * classes may be sufficient to manage the risk of deserializing unexpected classes.
 * <p>For an application composed from multiple modules or libraries, the structure
 * of the application can be used to identify the classes to be allowed or rejected
 * by each {@link ObjectInputStream} in each context of the application.
 * The deserialization filter factory is invoked when each stream is constructed and
 * can examine the thread or program to determine a context-specific filter to be applied.
 * Some possible examples:
 * <ul>
 *     <li>Thread-local state can hold the filter to be applied or composed
 *         with a stream-specific filter.
 *         Filters could be pushed and popped from a virtual stack of filters
 *         maintained by the application or libraries.
 *     <li>The filter factory can identify the caller of the deserialization method
 *         and use module or library context to select a filter or compose an appropriate
 *         context-specific filter.
 *         A mechanism could identify a callee with restricted or unrestricted
 *         access to serialized classes and choose a filter accordingly.
 * </ul>
 *
 * <h2>Example to filter every deserialization in a thread</h2>
 *
 * This class shows how an application provided filter factory can combine filters
 * to check every deserialization operation that takes place in a thread.
 * It defines a thread-local variable to hold the thread-specific filter, and constructs a filter factory
 * that composes that filter with the static JVM-wide filter and the stream-specific filter.
 * The {@code doWithSerialFilter} method does the setup of the thread-specific filter
 * and invokes the application provided {@link Runnable Runnable}.
 *
 * <pre>{@code
 * public static final class FilterInThread implements BinaryOperator<ObjectInputFilter> {
 *
 *     private final ThreadLocal<ObjectInputFilter> filterThreadLocal = new ThreadLocal<>();
 *
 *     // Construct a FilterInThread deserialization filter factory.
 *     public FilterInThread() {}
 *
 *     // Returns a composite filter of the static JVM-wide filter, a thread-specific filter,
 *     // and the stream-specific filter.
 *     public ObjectInputFilter apply(ObjectInputFilter curr, ObjectInputFilter next) {
 *         if (curr == null) {
 *             // Called from the OIS constructor or perhaps OIS.setObjectInputFilter with no current filter
 *             var filter = filterThreadLocal.get();
 *             if (filter != null) {
 *                 // Wrap the filter to reject UNDECIDED results
 *                 filter = ObjectInputFilter.rejectUndecidedClass(filter);
 *             }
 *             if (next != null) {
 *                 // Merge the next filter with the thread filter, if any
 *                 // Initially this is the static JVM-wide filter passed from the OIS constructor
 *                 // Wrap the filter to reject UNDECIDED results
 *                 filter = ObjectInputFilter.merge(next, filter);
 *                 filter = ObjectInputFilter.rejectUndecidedClass(filter);
 *             }
 *             return filter;
 *         } else {
 *             // Called from OIS.setObjectInputFilter with a current filter and a stream-specific filter.
 *             // The curr filter already incorporates the thread filter and static JVM-wide filter
 *             // and rejection of undecided classes
 *             // If there is a stream-specific filter wrap it and a filter to recheck for undecided
 *             if (next != null) {
 *                 next = ObjectInputFilter.merge(next, curr);
 *                 next = ObjectInputFilter.rejectUndecidedClass(next);
 *                 return next;
 *             }
 *             return curr;
 *         }
 *     }
 *
 *     // Applies the filter to the thread and invokes the runnable.
 *     public void doWithSerialFilter(ObjectInputFilter filter, Runnable runnable) {
 *         var prevFilter = filterThreadLocal.get();
 *         try {
 *             filterThreadLocal.set(filter);
 *             runnable.run();
 *         } finally {
 *             filterThreadLocal.set(prevFilter);
 *         }
 *     }
 * }
 * }</pre>
 * <h3>Using the Filter Factory</h3>
 * To use {@code FilterInThread} utility create an instance and configure it as the
 * JVM-wide filter factory.  The {@code doWithSerialFilter} method is invoked with a
 * filter allowing the example application and core classes:
 * <pre>{@code
 *        // Create a FilterInThread filter factory and set
 *        var filterInThread = new FilterInThread();
 *        ObjectInputFilter.Config.setSerialFilterFactory(filterInThread);
 *
 *        // Create a filter to allow example.* classes and reject all others
 *        var filter = ObjectInputFilter.Config.createFilter("example.*;java.base/*;!*");
 *        filterInThread.doWithSerialFilter(filter, () -> {
 *              byte[] bytes = ...;
 *              var o = deserializeObject(bytes);
 *        });
 * }</pre>
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a
 * method in this interface and its nested classes will cause a
 * {@link NullPointerException} to be thrown.
 *
 * @see ObjectInputStream#setObjectInputFilter(ObjectInputFilter)
 * @since 9
 */
@FunctionalInterface
public interface ObjectInputFilter {

    /**
     * Check the class, array length, number of object references, depth,
     * stream size, and other available filtering information.
     * Implementations of this method check the contents of the object graph being created
     * during deserialization. The filter returns {@link Status#ALLOWED Status.ALLOWED},
     * {@link Status#REJECTED Status.REJECTED}, or {@link Status#UNDECIDED Status.UNDECIDED}.
     *
     * <p>If {@code filterInfo.serialClass()} is {@code non-null}, there is a class to be checked.
     * If {@code serialClass()} is {@code null}, there is no class and the info contains
     * only metrics related to the depth of the graph being deserialized, the number of
     * references, and the size of the stream read.
     *
     * @apiNote Each filter implementing {@code checkInput} should return one of the values of {@link Status}.
     * Returning {@code null} may result in a {@link NullPointerException} or other unpredictable behavior.
     *
     * @param filterInfo provides information about the current object being deserialized,
     *             if any, and the status of the {@link ObjectInputStream}
     * @return  {@link Status#ALLOWED Status.ALLOWED} if accepted,
     *          {@link Status#REJECTED Status.REJECTED} if rejected,
     *          {@link Status#UNDECIDED Status.UNDECIDED} if undecided.
     */
    Status checkInput(FilterInfo filterInfo);

    /**
     * Returns a filter that returns {@code Status.ALLOWED} if the predicate
     * on the class is {@code true}.
     * The filter returns {@code ALLOWED} or the {@code otherStatus} based on the predicate
     * of the {@code non-null} class and {@code UNDECIDED} if the class is {@code null}.
     *
     * <p>When the filter's {@link ObjectInputFilter#checkInput checkInput(info)} method is invoked,
     * the predicate is applied to the {@link FilterInfo#serialClass() info.serialClass()},
     * the return Status is:
     * <ul>
     *     <li>{@link Status#UNDECIDED UNDECIDED}, if the {@code serialClass} is {@code null},</li>
     *     <li>{@link Status#ALLOWED ALLOWED}, if the predicate on the class returns {@code true},</li>
     *     <li>Otherwise, return {@code otherStatus}.</li>
     * </ul>
     * <p>
     * Example, to create a filter that will allow any class loaded from the platform
     * or bootstrap classloaders.
     * <pre><code>
     *     ObjectInputFilter f
     *         = allowFilter(cl -> cl.getClassLoader() == ClassLoader.getPlatformClassLoader() ||
     *                       cl.getClassLoader() == null, Status.UNDECIDED);
     * </code></pre>
     *
     * @param predicate a predicate to test a non-null Class
     * @param otherStatus a Status to use if the predicate is {@code false}
     * @return a filter that returns {@code ALLOWED} if the predicate
     *          on the class is {@code true}
     * @since 17
     */
    static ObjectInputFilter allowFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(otherStatus, "otherStatus");
        return new Config.PredicateFilter(predicate, ALLOWED, otherStatus);
    }

    /**
     * Returns a filter that returns {@code Status.REJECTED} if the predicate
     * on the class is {@code true}.
     * The filter returns {@code REJECTED} or the {@code otherStatus} based on the predicate
     * of the {@code non-null} class and {@code UNDECIDED} if the class is {@code null}.
     *
     * When the filter's {@link ObjectInputFilter#checkInput checkInput(info)} method is invoked,
     * the predicate is applied to the {@link FilterInfo#serialClass() serialClass()},
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
     * @param predicate a predicate to test a non-null Class
     * @param otherStatus a Status to use if the predicate is {@code false}
     * @return returns a filter that returns {@code REJECTED} if the predicate
     *          on the class is {@code true}
     * @since 17
     */
    static ObjectInputFilter rejectFilter(Predicate<Class<?>> predicate, Status otherStatus) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(otherStatus, "otherStatus");
        return new Config.PredicateFilter(predicate, REJECTED, otherStatus);
    }

    /**
     * Returns a filter that merges the status of a filter and another filter.
     * If {@code another} filter is {@code null}, the {@code filter} is returned.
     * Otherwise, a {@code filter} is returned to merge the pair of {@code non-null} filters.
     *
     * The filter returned implements the {@link ObjectInputFilter#checkInput(FilterInfo)} method
     * as follows:
     * <ul>
     *     <li>Invoke {@code filter} on the {@code FilterInfo} to get its {@code status};
     *     <li>Return {@code REJECTED} if the {@code status} is {@code REJECTED};
     *     <li>Invoke {@code anotherFilter} to get the {@code otherStatus};
     *     <li>Return {@code REJECTED} if the {@code otherStatus} is {@code REJECTED};
     *     <li>Return {@code ALLOWED}, if either {@code status} or {@code otherStatus}
     *          is {@code ALLOWED}, </li>
     *     <li>Otherwise, return {@code UNDECIDED}</li>
     * </ul>
     *
     * @param filter a filter
     * @param anotherFilter a filter to be merged with the filter, may be {@code null}
     * @return an {@link ObjectInputFilter} that merges the status of the filter and another filter
     * @since 17
     */
    static ObjectInputFilter merge(ObjectInputFilter filter, ObjectInputFilter anotherFilter) {
        Objects.requireNonNull(filter, "filter");
        return (anotherFilter == null) ? filter : new Config.MergeFilter(filter, anotherFilter);
    }

    /**
     * Returns a filter that invokes a given filter and maps {@code UNDECIDED} to {@code REJECTED}
     * for classes, with some special cases, and otherwise returns the status.
     * If the class is not a primitive class and not an array, the status returned is {@code REJECTED}.
     * If the class is a primitive class or an array class additional checks are performed;
     * see the list below for details.
     *
     * <p>Object deserialization accepts a class if the filter returns {@code UNDECIDED}.
     * Adding a filter to reject undecided results for classes that have not been
     * either allowed or rejected can prevent classes from slipping through the filter.
     *
     * @implSpec
     * The filter returned implements the {@link ObjectInputFilter#checkInput(FilterInfo)} method
     * as follows:
     * <ul>
     *     <li>Invoke the filter on the {@code FilterInfo} to get its {@code status};
     *     <li>Return the {@code status} if the status is {@code REJECTED} or {@code ALLOWED};
     *     <li>Return {@code UNDECIDED} if the {@code filterInfo.getSerialClass() serialClass}
     *          is {@code null};
     *     <li>Return {@code REJECTED} if the class is not an {@linkplain Class#isArray() array};
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
     * @param filter a filter
     * @return an {@link ObjectInputFilter} that maps an {@link Status#UNDECIDED}
     *      status to {@link Status#REJECTED} for classes, otherwise returns the
     *      filter status
     * @since 17
     */
    static ObjectInputFilter rejectUndecidedClass(ObjectInputFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return new Config.RejectUndecidedFilter(filter);
    }

    /**
     * FilterInfo provides access to information about the current object
     * being deserialized and the status of the {@link ObjectInputStream}.
     * @since 9
     */
    interface FilterInfo {
        /**
         * The class of an object being deserialized.
         * For arrays, it is the array type.
         * For example, the array class name of a 2 dimensional array of strings is
         * "{@code [[Ljava.lang.String;}".
         * To check the array's element type, iteratively use
         * {@link Class#getComponentType() Class.getComponentType} while the result
         * is an array and then check the class.
         * The {@code serialClass is null} in the case where a new object is not being
         * created and to give the filter a chance to check the depth, number of
         * references to existing objects, and the stream size.
         *
         * @return class of an object being deserialized; may be null
         */
        Class<?> serialClass();

        /**
         * The number of array elements when deserializing an array of the class.
         *
         * @return the non-negative number of array elements when deserializing
         * an array of the class, otherwise -1
         */
        long arrayLength();

        /**
         * The current depth.
         * The depth starts at {@code 1} and increases for each nested object and
         * decrements when each nested object returns.
         *
         * @return the current depth
         */
        long depth();

        /**
         * The current number of object references.
         *
         * @return the non-negative current number of object references
         */
        long references();

        /**
         * The current number of bytes consumed.
         * @implSpec  {@code streamBytes} is implementation specific
         * and may not be directly related to the object in the stream
         * that caused the callback.
         *
         * @return the non-negative current number of bytes consumed
         */
        long streamBytes();
    }

    /**
     * The status of a check on the class, array length, number of references,
     * depth, and stream size.
     *
     * @since 9
     */
    enum Status {
        /**
         * The status is undecided, not allowed and not rejected.
         */
        UNDECIDED,
        /**
         * The status is allowed.
         */
        ALLOWED,
        /**
         * The status is rejected.
         */
        REJECTED;
    }

    /**
     * A utility class to set and get the JVM-wide deserialization filter factory,
     * the static JVM-wide filter, or to create a filter from a pattern string.
     * The static filter factory and the static filter apply to the whole Java runtime,
     * or "JVM-wide", there is only one of each. For a complete description of
     * the function and use refer to {@link ObjectInputFilter}.
     *
     * <p>The JVM-wide deserialization filter factory and the static JVM-wide filter
     * can be configured from system properties during the initialization of the
     * {@code ObjectInputFilter.Config} class.
     *
     * <p>If the Java virtual machine is started with the system property
     * {@systemProperty jdk.serialFilter}, its value is used to configure the filter.
     * If the system property is not defined, and the {@link java.security.Security} property
     * {@code jdk.serialFilter} is defined then it is used to configure the filter.
     * Otherwise, the filter is not configured during initialization and
     * can be set with {@link #setSerialFilter(ObjectInputFilter) Config.setSerialFilter}.
     * Setting the {@code jdk.serialFilter} with {@link System#setProperty(String, String)
     * System.setProperty} <em>does not set the filter</em>.
     * The syntax for the property value is the same as for the
     * {@link #createFilter(String) createFilter} method.
     *
     * <p>
     * If the Java virtual machine is started with the system property
     * {@systemProperty jdk.serialFilterFactory} or the {@link java.security.Security} property
     * of the same name, its value names the class to configure the JVM-wide deserialization
     * filter factory.
     * If the system property is not defined, and the {@link java.security.Security} property
     * {@code jdk.serialFilterFactory} is defined then it is used to configure the filter factory.
     * If it remains unset, the filter factory is a builtin filter factory compatible
     * with previous versions.
     *
     * <p>The class must be public, must have a public zero-argument constructor, implement the
     * {@link BinaryOperator {@literal BinaryOperator<ObjectInputFilter>}} interface, provide its implementation and
     * be accessible via the {@linkplain ClassLoader#getSystemClassLoader() application class loader}.
     * If the filter factory constructor is not invoked successfully, an {@link ExceptionInInitializerError}
     * is thrown and subsequent use of the filter factory for deserialization fails with
     * {@link IllegalStateException}.
     * The filter factory configured using the system or security property during initialization
     * can NOT be replaced with {@link #setSerialFilterFactory(BinaryOperator) Config.setSerialFilterFactory}.
     * This ensures that a filter factory set on the command line is not overridden accidentally
     * or intentionally by the application.
     *
     * <p>Setting the {@code jdk.serialFilterFactory} with {@link System#setProperty(String, String)
     * System.setProperty} <em>does not set the filter factory</em>.
     * The syntax for the system property value and security property value is the
     * fully qualified class name of the deserialization filter factory.
     * @since 9
     */
    final class Config {
        /**
         * Lock object for filter and filter factory.
         */
        private final static Object serialFilterLock = new Object();

        /**
         * The property name for the filter.
         * Used as a system property and a java.security.Security property.
         */
        private static final String SERIAL_FILTER_PROPNAME = "jdk.serialFilter";

        /**
         * The property name for the filter factory.
         * Used as a system property and a java.security.Security property.
         */
        private static final String SERIAL_FILTER_FACTORY_PROPNAME = "jdk.serialFilterFactory";

        /**
         * Current static filter.
         */
        private static volatile ObjectInputFilter serialFilter;

        /**
         * Current serial filter factory.
         * @see Config#setSerialFilterFactory(BinaryOperator)
         */
        private static volatile BinaryOperator<ObjectInputFilter> serialFilterFactory;

        /**
         * Boolean to indicate that the filter factory can not be set or replaced.
         * - an ObjectInputStream has already been created using the current filter factory
         * - has been set on the command line
         * @see Config#setSerialFilterFactory(BinaryOperator)
         */
        private static final AtomicBoolean filterFactoryNoReplace = new AtomicBoolean();

        /**
         * Debug and Trace Logger
         */
        private static final System.Logger configLog;

        static {
            /*
             * Initialize the configuration containing the filter factory, static filter, and logger.
             * <ul>
             * <li>The logger is created.
             * <li>The property 'jdk.serialFilter" is read, either as a system property or a security property,
             *     and if set, defines the configured static JVM-wide filter and is logged.
             * <li>The property jdk.serialFilterFactory is read, either as a system property or a security property,
             *     and if set, defines the initial filter factory and is logged.
             * </ul>
             */

            // Initialize the logger.
            configLog = System.getLogger("java.io.serialization");

            // Get the values of the system properties, if they are defined
            @SuppressWarnings("removal")
            String factoryClassName = StaticProperty.jdkSerialFilterFactory() != null
                    ? StaticProperty.jdkSerialFilterFactory()
                    : AccessController.doPrivileged((PrivilegedAction<String>) () ->
                        Security.getProperty(SERIAL_FILTER_FACTORY_PROPNAME));

            @SuppressWarnings("removal")
            String filterString = StaticProperty.jdkSerialFilter() != null
                    ? StaticProperty.jdkSerialFilter()
                    : AccessController.doPrivileged((PrivilegedAction<String>) () ->
                        Security.getProperty(SERIAL_FILTER_PROPNAME));

            // Initialize the static filter if the jdk.serialFilter is present
            ObjectInputFilter filter = null;
            if (filterString != null) {
                configLog.log(DEBUG,
                        "Creating deserialization filter from {0}", filterString);
                try {
                    filter = createFilter(filterString);
                } catch (RuntimeException re) {
                    configLog.log(ERROR,
                            "Error configuring filter: {0}", re);
                }
            }
            serialFilter = filter;

            // Initialize the filter factory if the jdk.serialFilterFactory is defined
            // otherwise use the builtin filter factory.
            if (factoryClassName == null) {
                serialFilterFactory = new BuiltinFilterFactory();
            } else {
                try {
                    // Load using the system class loader, the named class may be an application class.
                    // Cause Config.setSerialFilterFactory to throw {@link IllegalStateException}
                    // if Config.setSerialFilterFactory is called as a side effect of the
                    // static initialization of the class or constructor.
                    filterFactoryNoReplace.set(true);

                    Class<?> factoryClass = Class.forName(factoryClassName, true,
                            ClassLoader.getSystemClassLoader());
                    @SuppressWarnings("unchecked")
                    BinaryOperator<ObjectInputFilter> factory =
                            (BinaryOperator<ObjectInputFilter>)
                            factoryClass.getConstructor().newInstance(new Object[0]);
                    configLog.log(DEBUG,
                            "Creating deserialization filter factory for {0}", factoryClassName);
                    serialFilterFactory = factory;
                } catch (RuntimeException | ClassNotFoundException | NoSuchMethodException |
                        IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                    Throwable th = (ex instanceof InvocationTargetException ite) ? ite.getCause() : ex;
                    configLog.log(ERROR,
                            "Error configuring filter factory: {0}", (Object)th);
                    // Do not continue if configuration not initialized
                    throw new ExceptionInInitializerError(th);
                }
            }
            // Setup shared secrets for RegistryImpl to use.
            SharedSecrets.setJavaObjectInputFilterAccess(Config::createFilter2);
        }

        /**
         * Config has no instances.
         */
        private Config() {
        }

        /**
         * Logger for filter actions.
         */
        private static void traceFilter(String msg, Object... args) {
            configLog.log(TRACE, msg, args);
        }

        /**
         * Returns the static JVM-wide deserialization filter or {@code null} if not configured.
         *
         * @return the static JVM-wide deserialization filter or {@code null} if not configured
         */
        public static ObjectInputFilter getSerialFilter() {
            return serialFilter;
        }

        /**
         * Set the static JVM-wide filter if it has not already been configured or set.
         *
         * @param filter the deserialization filter to set as the JVM-wide filter; not null
         * @throws SecurityException if there is security manager and the
         *       {@code SerializablePermission("serialFilter")} is not granted
         * @throws IllegalStateException if the filter has already been set
         */
        public static void setSerialFilter(ObjectInputFilter filter) {
            Objects.requireNonNull(filter, "filter");
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(ObjectStreamConstants.SERIAL_FILTER_PERMISSION);
            }
            synchronized (serialFilterLock) {
                if (serialFilter != null) {
                    throw new IllegalStateException("Serial filter can only be set once");
                }
                serialFilter = filter;
            }
        }

        /**
         * Returns the JVM-wide deserialization filter factory.
         * If the filter factory has been {@linkplain #setSerialFilterFactory(BinaryOperator) set} it is returned,
         * otherwise, a builtin deserialization filter factory is returned.
         * The filter factory provides a filter for every ObjectInputStream when invoked from
         * {@linkplain ObjectInputStream#ObjectInputStream(InputStream) ObjectInputStream constructors}
         * and when a stream-specific filter is set with
         * {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter) setObjectInputFilter}.
         *
         * @implSpec
         * The builtin deserialization filter factory provides the
         * {@linkplain #getSerialFilter static JVM-wide filter} when invoked from
         * {@linkplain ObjectInputStream#ObjectInputStream(InputStream) ObjectInputStream constructors}.
         * When invoked {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter)
         * to set the stream-specific filter} the requested filter replaces the static JVM-wide filter,
         * unless it has already been set.
         * The builtin deserialization filter factory implements the behavior of earlier versions of
         * setting the initial filter in the {@link ObjectInputStream} constructor and
         * {@link ObjectInputStream#setObjectInputFilter}.
         *
         * @return the JVM-wide deserialization filter factory; non-null
         * @throws IllegalStateException if the filter factory initialization is incomplete
         * @since 17
         */
        public static BinaryOperator<ObjectInputFilter> getSerialFilterFactory() {
            if (serialFilterFactory == null)
                throw new IllegalStateException("Serial filter factory initialization incomplete");
            return serialFilterFactory;
        }

        /**
         * Returns the serial filter factory singleton and prevents it from changing
         * thereafter.
         * This package private method is *only* called by {@link ObjectInputStream#ObjectInputStream()}
         * and  {@link ObjectInputStream#ObjectInputStream(InputStream)}.
         * {@link ObjectInputFilter.Config#setSerialFilterFactory(BinaryOperator)} enforces
         * the requirement that the filter factory can not be changed after an ObjectInputStream
         * is created.
         *
         * @return the serial filter factory
         * @throws IllegalStateException if the filter factory initialization is incomplete
         */
        /* package-private */
        static BinaryOperator<ObjectInputFilter> getSerialFilterFactorySingleton() {
            filterFactoryNoReplace.set(true);
            return getSerialFilterFactory();
        }

        /**
         * Set the {@linkplain #getSerialFilterFactory() JVM-wide deserialization filter factory}.
         * The filter factory can be configured exactly once with one of:
         * setting the {@code jdk.serialFilterFactory} property on the command line,
         * setting the {@code jdk.serialFilterFactory} property in the {@link java.security.Security}
         * file, or using this {@code setSerialFilterFactory} method.
         * The filter factory can be set only before any {@link ObjectInputStream} has been
         * created to avoid any inconsistency in which filter factory is being used.
         *
         * <p>The JVM-wide filter factory is invoked when an ObjectInputStream
         * {@linkplain ObjectInputStream#ObjectInputStream() is constructed} and when the
         * {@linkplain ObjectInputStream#setObjectInputFilter(ObjectInputFilter) stream-specific filter is set}.
         * The parameters are the current filter and a requested filter and it
         * returns the filter to be used for the stream.
         * If the current filter is {@code non-null}, the filter factory must return a
         * {@code non-null} filter; this is to prevent unintentional disabling of filtering
         * after it has been enabled.
         * The factory determines the filter to be used for {@code ObjectInputStream} streams based
         * on its inputs, any other filters, context, or state that is available.
         * The factory may throw runtime exceptions to signal incorrect use or invalid parameters.
         * See the {@linkplain ObjectInputFilter filter models} for examples of composition and delegation.
         *
         * @param filterFactory the deserialization filter factory to set as the
         *         JVM-wide filter factory; not null
         * @throws IllegalStateException if the builtin deserialization filter factory
         *         has already been replaced or any instance of {@link ObjectInputStream}
         *         has been created.
         * @throws SecurityException if there is security manager and the
         *       {@code SerializablePermission("serialFilter")} is not granted
         * @since 17
         */
        public static void setSerialFilterFactory(BinaryOperator<ObjectInputFilter> filterFactory) {
            Objects.requireNonNull(filterFactory, "filterFactory");
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(ObjectStreamConstants.SERIAL_FILTER_PERMISSION);
            }
            if (filterFactoryNoReplace.getAndSet(true)) {
                final String msg = serialFilterFactory != null
                        ? serialFilterFactory.getClass().getName()
                        : "initialization incomplete";
                throw new IllegalStateException("Cannot replace filter factory: " + msg);
            }
            configLog.log(DEBUG,
                    "Setting deserialization filter factory to {0}", filterFactory.getClass().getName());
            serialFilterFactory = filterFactory;
        }

        /**
         * Returns an ObjectInputFilter from a string of patterns.
         * <p>
         * Patterns are separated by ";" (semicolon). Whitespace is significant and
         * is considered part of the pattern.
         * If a pattern includes an equals assignment, "{@code =}" it sets a limit.
         * If a limit appears more than once the last value is used.
         * <ul>
         *     <li>maxdepth={@code value} - the maximum depth of a graph</li>
         *     <li>maxrefs={@code value}  - the maximum number of internal references</li>
         *     <li>maxbytes={@code value} - the maximum number of bytes in the input stream</li>
         *     <li>maxarray={@code value} - the maximum array length allowed</li>
         * </ul>
         * <p>
         * Other patterns match or reject class or package name
         * as returned from {@link Class#getName() Class.getName()} and
         * if an optional module name is present
         * {@link Module#getName() class.getModule().getName()}.
         * Note that for arrays the element type is used in the pattern,
         * not the array type.
         * <ul>
         * <li>If the pattern starts with "!", the class is rejected if the remaining pattern is matched;
         *     otherwise the class is allowed if the pattern matches.
         * <li>If the pattern contains "/", the non-empty prefix up to the "/" is the module name;
         *     if the module name matches the module name of the class then
         *     the remaining pattern is matched with the class name.
         *     If there is no "/", the module name is not compared.
         * <li>If the pattern ends with ".**" it matches any class in the package and all subpackages.
         * <li>If the pattern ends with ".*" it matches any class in the package.
         * <li>If the pattern ends with "*", it matches any class with the pattern as a prefix.
         * <li>If the pattern is equal to the class name, it matches.
         * <li>Otherwise, the pattern is not matched.
         * </ul>
         * <p>
         * The resulting filter performs the limit checks and then
         * tries to match the class, if any. If any of the limits are exceeded,
         * the filter returns {@link Status#REJECTED Status.REJECTED}.
         * If the class is an array type, the class to be matched is the element type.
         * Arrays of any number of dimensions are treated the same as the element type.
         * For example, a pattern of "{@code !example.Foo}",
         * rejects creation of any instance or array of {@code example.Foo}.
         * The first pattern that matches, working from left to right, determines
         * the {@link Status#ALLOWED Status.ALLOWED}
         * or {@link Status#REJECTED Status.REJECTED} result.
         * If the limits are not exceeded and no pattern matches the class,
         * the result is {@link Status#UNDECIDED Status.UNDECIDED}.
         *
         * @param pattern the pattern string to parse; not null
         * @return a filter to check a class being deserialized;
         *          {@code null} if no patterns
         * @throws IllegalArgumentException if the pattern string is illegal or
         *         malformed and cannot be parsed.
         *         In particular, if any of the following is true:
         * <ul>
         * <li>   if a limit is missing the name or the name is not one of
         *        "maxdepth", "maxrefs", "maxbytes", or "maxarray"
         * <li>   if the value of the limit can not be parsed by
         *        {@link Long#parseLong Long.parseLong} or is negative
         * <li>   if the pattern contains "/" and the module name is missing
         *        or the remaining pattern is empty
         * <li>   if the package is missing for ".*" and ".**"
         * </ul>
         */
        public static ObjectInputFilter createFilter(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            return Global.createFilter(pattern, true);
        }

        /**
         * Returns an ObjectInputFilter from a string of patterns that
         * checks only the length for arrays, not the component type.
         *
         * @param pattern the pattern string to parse; not null
         * @return a filter to check a class being deserialized;
         *          {@code null} if no patterns
         */
        static ObjectInputFilter createFilter2(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            return Global.createFilter(pattern, false);
        }

        /**
         * Implementation of ObjectInputFilter that performs the checks of
         * the JVM-wide deserialization filter. If configured, it will be
         * used for all ObjectInputStreams that do not set their own filters.
         *
         */
        final static class Global implements ObjectInputFilter {
            /**
             * The pattern used to create the filter.
             */
            private final String pattern;
            /**
             * The list of class filters.
             */
            private final List<Function<Class<?>, Status>> filters;
            /**
             * Maximum allowed bytes in the stream.
             */
            private long maxStreamBytes;
            /**
             * Maximum depth of the graph allowed.
             */
            private long maxDepth;
            /**
             * Maximum number of references in a graph.
             */
            private long maxReferences;
            /**
             * Maximum length of any array.
             */
            private long maxArrayLength;
            /**
             * True to check the component type for arrays.
             */
            private final boolean checkComponentType;

            /**
             * Returns an ObjectInputFilter from a string of patterns.
             *
             * @param pattern the pattern string to parse
             * @param checkComponentType true if the filter should check
             *                           the component type of arrays
             * @return a filter to check a class being deserialized;
             *          {@code null} if no patterns
             * @throws IllegalArgumentException if the parameter is malformed
             *                if the pattern is missing the name, the long value
             *                is not a number or is negative.
             */
            static ObjectInputFilter createFilter(String pattern, boolean checkComponentType) {
                try {
                    return new Global(pattern, checkComponentType);
                } catch (UnsupportedOperationException uoe) {
                    // no non-empty patterns
                    return null;
                }
            }

            /**
             * Construct a new filter from the pattern String.
             *
             * @param pattern a pattern string of filters
             * @param checkComponentType true if the filter should check
             *                           the component type of arrays
             * @throws IllegalArgumentException if the pattern is malformed
             * @throws UnsupportedOperationException if there are no non-empty patterns
             */
            private Global(String pattern, boolean checkComponentType) {
                boolean hasLimits = false;
                this.pattern = pattern;
                this.checkComponentType = checkComponentType;

                maxArrayLength = Long.MAX_VALUE; // Default values are unlimited
                maxDepth = Long.MAX_VALUE;
                maxReferences = Long.MAX_VALUE;
                maxStreamBytes = Long.MAX_VALUE;

                String[] patterns = pattern.split(";");
                filters = new ArrayList<>(patterns.length);
                for (int i = 0; i < patterns.length; i++) {
                    String p = patterns[i];
                    int nameLen = p.length();
                    if (nameLen == 0) {
                        continue;
                    }
                    if (parseLimit(p)) {
                        // If the pattern contained a limit setting, i.e. type=value
                        hasLimits = true;
                        continue;
                    }
                    boolean negate = p.charAt(0) == '!';
                    int poffset = negate ? 1 : 0;

                    // isolate module name, if any
                    int slash = p.indexOf('/', poffset);
                    if (slash == poffset) {
                        throw new IllegalArgumentException("module name is missing in: \"" + pattern + "\"");
                    }
                    final String moduleName = (slash >= 0) ? p.substring(poffset, slash) : null;
                    poffset = (slash >= 0) ? slash + 1 : poffset;

                    final Function<Class<?>, Status> patternFilter;
                    if (p.endsWith("*")) {
                        // Wildcard cases
                        if (p.endsWith(".*")) {
                            // Pattern is a package name with a wildcard
                            final String pkg = p.substring(poffset, nameLen - 2);
                            if (pkg.isEmpty()) {
                                throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                            }
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> matchesPackage(c, pkg) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> matchesPackage(c, pkg) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        } else if (p.endsWith(".**")) {
                            // Pattern is a package prefix with a double wildcard
                            final String pkgs = p.substring(poffset, nameLen - 2);
                            if (pkgs.length() < 2) {
                                throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                            }
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(pkgs) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(pkgs) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        } else {
                            // Pattern is a classname (possibly empty) with a trailing wildcard
                            final String className = p.substring(poffset, nameLen - 1);
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(className) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(className) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        }
                    } else {
                        final String name = p.substring(poffset);
                        if (name.isEmpty()) {
                            throw new IllegalArgumentException("class or package missing in: \"" + pattern + "\"");
                        }
                        // Pattern is a class name
                        if (negate) {
                            // A Function that fails if the class equals the pattern, otherwise don't care
                            patternFilter = c -> c.getName().equals(name) ? Status.REJECTED : Status.UNDECIDED;
                        } else {
                            // A Function that succeeds if the class equals the pattern, otherwise don't care
                            patternFilter = c -> c.getName().equals(name) ? Status.ALLOWED : Status.UNDECIDED;
                        }
                    }
                    // If there is a moduleName, combine the module name check with the package/class check
                    if (moduleName == null) {
                        filters.add(patternFilter);
                    } else {
                        filters.add(c -> moduleName.equals(c.getModule().getName()) ? patternFilter.apply(c) : Status.UNDECIDED);
                    }
                }
                if (filters.isEmpty() && !hasLimits) {
                    throw new UnsupportedOperationException("no non-empty patterns");
                }
            }

            /**
             * Parse out a limit for one of maxarray, maxdepth, maxbytes, maxreferences.
             *
             * @param pattern a string with a type name, '=' and a value
             * @return {@code true} if a limit was parsed, else {@code false}
             * @throws IllegalArgumentException if the pattern is missing
             *                the name, the Long value is not a number or is negative.
             */
            private boolean parseLimit(String pattern) {
                int eqNdx = pattern.indexOf('=');
                if (eqNdx < 0) {
                    // not a limit pattern
                    return false;
                }
                String valueString = pattern.substring(eqNdx + 1);
                if (pattern.startsWith("maxdepth=")) {
                    maxDepth = parseValue(valueString);
                } else if (pattern.startsWith("maxarray=")) {
                    maxArrayLength = parseValue(valueString);
                } else if (pattern.startsWith("maxrefs=")) {
                    maxReferences = parseValue(valueString);
                } else if (pattern.startsWith("maxbytes=")) {
                    maxStreamBytes = parseValue(valueString);
                } else {
                    throw new IllegalArgumentException("unknown limit: " + pattern.substring(0, eqNdx));
                }
                return true;
            }

            /**
             * Parse the value of a limit and check that it is non-negative.
             * @param string inputstring
             * @return the parsed value
             * @throws IllegalArgumentException if parsing the value fails or the value is negative
             */
            private static long parseValue(String string) throws IllegalArgumentException {
                // Parse a Long from after the '=' to the end
                long value = Long.parseLong(string);
                if (value < 0) {
                    throw new IllegalArgumentException("negative limit: " + string);
                }
                return value;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Status checkInput(FilterInfo filterInfo) {
                if (filterInfo.references() < 0
                        || filterInfo.depth() < 0
                        || filterInfo.streamBytes() < 0
                        || filterInfo.references() > maxReferences
                        || filterInfo.depth() > maxDepth
                        || filterInfo.streamBytes() > maxStreamBytes) {
                    return Status.REJECTED;
                }

                Class<?> clazz = filterInfo.serialClass();
                if (clazz != null) {
                    if (clazz.isArray()) {
                        if (filterInfo.arrayLength() >= 0 && filterInfo.arrayLength() > maxArrayLength) {
                            // array length is too big
                            return Status.REJECTED;
                        }
                        if (!checkComponentType) {
                            // As revised; do not check the component type for arrays
                            traceFilter("Pattern filter array class: {0}, filter: {1}", clazz, this);
                            return Status.UNDECIDED;
                        }
                        do {
                            // Arrays are decided based on the component type
                            clazz = clazz.getComponentType();
                        } while (clazz.isArray());
                    }

                    if (clazz.isPrimitive())  {
                        // Primitive types are undecided; let someone else decide
                        traceFilter("Pattern filter UNDECIDED, primitive class: {0}, filter: {1}", clazz, this);
                        return UNDECIDED;
                    } else {
                        // Find any filter that allowed or rejected the class
                        final Class<?> cl = clazz;
                        Optional<Status> status = filters.stream()
                                .map(f -> f.apply(cl))
                                .filter(p -> p != Status.UNDECIDED)
                                .findFirst();
                        Status s = status.orElse(Status.UNDECIDED);
                        traceFilter("Pattern filter {0}, class: {1}, filter: {2}", s, cl, this);
                        return s;
                    }
                }
                // There are no classes to check and none of the limits have been exceeded.
                return UNDECIDED;
            }

            /**
             * Returns {@code true} if the class is in the package.
             *
             * @param c   a class
             * @param pkg a package name
             * @return {@code true} if the class is in the package,
             * otherwise {@code false}
             */
            private static boolean matchesPackage(Class<?> c, String pkg) {
                return pkg.equals(c.getPackageName());
            }

            /**
             * Returns the pattern used to create this filter.
             * @return the pattern used to create this filter
             */
            @Override
            public String toString() {
                return pattern;
            }
        }

        /**
         * An ObjectInputFilter to evaluate a predicate mapping a class to a boolean.
         * @see ObjectInputFilter#allowFilter(Predicate, Status)
         * @see ObjectInputFilter#rejectFilter(Predicate, Status)
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
                traceFilter("PredicateFilter {0}, filter: {1}", status, this);
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
         * An ObjectInputFilter that merges the status of two filters.
         * @see ObjectInputFilter#merge(ObjectInputFilter, ObjectInputFilter)
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
             * otherwise, ALLOWED if either of the filters returns ALLOWED,
             * otherwise, returns {@code UNDECIDED}.
             *
             * @param info the FilterInfo
             * @return REJECTED if either of the filters returns REJECTED,
             *          otherwise, ALLOWED if either of the filters returns ALLOWED,
             *          otherwise, returns {@code UNDECIDED}.
             */
            public ObjectInputFilter.Status checkInput(FilterInfo info) {
               Status firstStatus = Objects.requireNonNull(first.checkInput(info), "status");
                if (REJECTED.equals(firstStatus)) {
                    traceFilter("MergeFilter REJECTED first: {0}, filter: {1}",
                            firstStatus, this);
                    return REJECTED;
                }
                Status secondStatus = Objects.requireNonNull(second.checkInput(info), "other status");
                if (REJECTED.equals(secondStatus)) {
                    traceFilter("MergeFilter REJECTED {0}, {1}, filter: {2}",
                            firstStatus, secondStatus, this);
                    return REJECTED;
                }
                if (ALLOWED.equals(firstStatus) || ALLOWED.equals(secondStatus)) {
                    traceFilter("MergeFilter ALLOWED either: {0}, {1}, filter: {2}",
                            firstStatus, secondStatus, this);
                    return ALLOWED;
                }
                traceFilter("MergeFilter UNDECIDED {0}, {1}, filter: {2}",
                        firstStatus, secondStatus, this);
                return UNDECIDED;
            }

            @Override
            public String toString() {
                return "merge(" + first + ", " + second + ")";
            }
        }

        /**
         * A filter that maps the status {@code UNDECIDED} to {@code REJECTED} when checking a class.
         * @see ObjectInputFilter#rejectUndecidedClass(ObjectInputFilter)
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
                    traceFilter("RejectUndecidedFilter Array Component type {0} class: {1}, filter: {2}",
                            clazzStatus, clazz, this);
                    status = (ALLOWED.equals(clazzStatus)) ? ALLOWED : REJECTED;
                }
                traceFilter("RejectUndecidedFilter {0} class: {1}, filter: {2}",
                        status, info.serialClass(), this);
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
         * Builtin Deserialization filter factory.
         * The builtin deserialization filter factory implements the behavior of earlier versions of
         * setting the static serial filter in the {@link ObjectInputStream} constructor and
         * {@link ObjectInputStream#setObjectInputFilter} in cooperation with {@code ObjectInputStream}.
         * Checking that the stream-specific filter can only be set once and throwing
         * {@link IllegalStateException} is handled by
         * {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter)}.
         *
         * @see Config#getSerialFilterFactory()
         */
        private static final class BuiltinFilterFactory implements BinaryOperator<ObjectInputFilter> {
            /**
             * Returns the {@code ObjectInputFilter} to be used for an ObjectInputStream.
             *
             * <p>When invoked from the
             * {@linkplain ObjectInputStream#ObjectInputStream(InputStream) ObjectInputStream constructors},
             * the first parameter is {@code null} and the second parameter is the
             * {@linkplain ObjectInputFilter.Config#getSerialFilter() static JVM-wide filter};
             * the value returned is {@code newFilter}, the static JVM-wide filter.
             * <p>
             * When invoked from
             * {@link ObjectInputStream#setObjectInputFilter(ObjectInputFilter) setObjectInputFilter}
             * to set the stream-specific filter, the value is {@code newFilter} to replace the
             * previous filter.
             *
             * @param oldFilter the current filter, may be null
             * @param newFilter a new filter, may be null
             * @return an ObjectInputFilter, the new Filter, may be null
             */
            @Override
            public ObjectInputFilter apply(ObjectInputFilter oldFilter, ObjectInputFilter newFilter) {
                traceFilter("Builtin factory: {0} -> new: {1}", oldFilter, newFilter);
                return newFilter;
            }

            /**
             * Returns the class name of this builtin deserialization filter factory.
             * @return returns the class name of this builtin deserialization filter factory
             */
            public String toString() {
                return this.getClass().getName();
            }
        }
    }
}
