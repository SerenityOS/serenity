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
package jdk.internal.util;

import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility methods to check if state or arguments are correct.
 *
 */
public class Preconditions {

    /**
     * Utility exception formatters which can be used in {@code Preconditions}
     * check functions below.
     *
     * These anonymous inner classes can be syntactically replaced by lambda
     * expression or method reference, but it's not feasible in practices,
     * because {@code Preconditions} is used in many fundamental classes such
     * as {@code java.lang.String}, lambda expressions or method references
     * exercise many other code at VM startup, this could lead a recursive
     * calls when fundamental classes is used in lambda expressions or method
     * references.
     */
    public static final BiFunction<String, List<Number>, StringIndexOutOfBoundsException>
            SIOOBE_FORMATTER = Preconditions.outOfBoundsExceptionFormatter(new Function<>() {
        @Override
        public StringIndexOutOfBoundsException apply(String s) {
            return new StringIndexOutOfBoundsException(s);
        }
    });

    public static final BiFunction<String, List<Number>, ArrayIndexOutOfBoundsException>
            AIOOBE_FORMATTER = Preconditions.outOfBoundsExceptionFormatter(new Function<>() {
        @Override
        public ArrayIndexOutOfBoundsException apply(String s) {
            return new ArrayIndexOutOfBoundsException(s);
        }
    });

    public static final BiFunction<String,List<Number>, IndexOutOfBoundsException>
            IOOBE_FORMATTER = Preconditions.outOfBoundsExceptionFormatter(new Function<>() {
        @Override
        public IndexOutOfBoundsException apply(String s) {
            return new IndexOutOfBoundsException(s);
        }
    });

    /**
     * Maps out-of-bounds values to a runtime exception.
     *
     * @param checkKind the kind of bounds check, whose name may correspond
     *        to the name of one of the range check methods, checkIndex,
     *        checkFromToIndex, checkFromIndexSize
     * @param args the out-of-bounds arguments that failed the range check.
     *        If the checkKind corresponds a the name of a range check method
     *        then the bounds arguments are those that can be passed in order
     *        to the method.
     * @param oobef the exception formatter that when applied with a checkKind
     *        and a list out-of-bounds arguments returns a runtime exception.
     *        If {@code null} then, it is as if an exception formatter was
     *        supplied that returns {@link IndexOutOfBoundsException} for any
     *        given arguments.
     * @return the runtime exception
     */
    private static RuntimeException outOfBounds(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobef,
            String checkKind,
            Number... args) {
        List<Number> largs = List.of(args);
        RuntimeException e = oobef == null
                             ? null : oobef.apply(checkKind, largs);
        return e == null
               ? new IndexOutOfBoundsException(outOfBoundsMessage(checkKind, largs)) : e;
    }

    private static RuntimeException outOfBoundsCheckIndex(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            int index, int length) {
        return outOfBounds(oobe, "checkIndex", index, length);
    }

    private static RuntimeException outOfBoundsCheckFromToIndex(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            int fromIndex, int toIndex, int length) {
        return outOfBounds(oobe, "checkFromToIndex", fromIndex, toIndex, length);
    }

    private static RuntimeException outOfBoundsCheckFromIndexSize(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            int fromIndex, int size, int length) {
        return outOfBounds(oobe, "checkFromIndexSize", fromIndex, size, length);
    }

    private static RuntimeException outOfBoundsCheckIndex(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            long index, long length) {
        return outOfBounds(oobe, "checkIndex", index, length);
    }

    private static RuntimeException outOfBoundsCheckFromToIndex(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            long fromIndex, long toIndex, long length) {
        return outOfBounds(oobe, "checkFromToIndex", fromIndex, toIndex, length);
    }

    private static RuntimeException outOfBoundsCheckFromIndexSize(
            BiFunction<String, List<Number>, ? extends RuntimeException> oobe,
            long fromIndex, long size, long length) {
        return outOfBounds(oobe, "checkFromIndexSize", fromIndex, size, length);
    }

    /**
     * Returns an out-of-bounds exception formatter from an given exception
     * factory.  The exception formatter is a function that formats an
     * out-of-bounds message from its arguments and applies that message to the
     * given exception factory to produce and relay an exception.
     *
     * <p>The exception formatter accepts two arguments: a {@code String}
     * describing the out-of-bounds range check that failed, referred to as the
     * <em>check kind</em>; and a {@code List<Number>} containing the
     * out-of-bound integral values that failed the check.  The list of
     * out-of-bound values is not modified.
     *
     * <p>Three check kinds are supported {@code checkIndex},
     * {@code checkFromToIndex} and {@code checkFromIndexSize} corresponding
     * respectively to the specified application of an exception formatter as an
     * argument to the out-of-bounds range check methods
     * {@link #checkIndex(int, int, BiFunction) checkIndex},
     * {@link #checkFromToIndex(int, int, int, BiFunction) checkFromToIndex}, and
     * {@link #checkFromIndexSize(int, int, int, BiFunction) checkFromIndexSize}.
     * Thus a supported check kind corresponds to a method name and the
     * out-of-bound integral values correspond to method argument values, in
     * order, preceding the exception formatter argument (similar in many
     * respects to the form of arguments required for a reflective invocation of
     * such a range check method).
     *
     * <p>Formatter arguments conforming to such supported check kinds will
     * produce specific exception messages describing failed out-of-bounds
     * checks.  Otherwise, more generic exception messages will be produced in
     * any of the following cases: the check kind is supported but fewer
     * or more out-of-bounds values are supplied, the check kind is not
     * supported, the check kind is {@code null}, or the list of out-of-bound
     * values is {@code null}.
     *
     * @apiNote
     * This method produces an out-of-bounds exception formatter that can be
     * passed as an argument to any of the supported out-of-bounds range check
     * methods declared by {@code Objects}.  For example, a formatter producing
     * an {@code ArrayIndexOutOfBoundsException} may be produced and stored on a
     * {@code static final} field as follows:
     * <pre>{@code
     * static final
     * BiFunction<String, List<Number>, ArrayIndexOutOfBoundsException> AIOOBEF =
     *     outOfBoundsExceptionFormatter(ArrayIndexOutOfBoundsException::new);
     * }</pre>
     * The formatter instance {@code AIOOBEF} may be passed as an argument to an
     * out-of-bounds range check method, such as checking if an {@code index}
     * is within the bounds of a {@code limit}:
     * <pre>{@code
     * checkIndex(index, limit, AIOOBEF);
     * }</pre>
     * If the bounds check fails then the range check method will throw an
     * {@code ArrayIndexOutOfBoundsException} with an appropriate exception
     * message that is a produced from {@code AIOOBEF} as follows:
     * <pre>{@code
     * AIOOBEF.apply("checkIndex", List.of(index, limit));
     * }</pre>
     *
     * @param f the exception factory, that produces an exception from a message
     *        where the message is produced and formatted by the returned
     *        exception formatter.  If this factory is stateless and side-effect
     *        free then so is the returned formatter.
     *        Exceptions thrown by the factory are relayed to the caller
     *        of the returned formatter.
     * @param <X> the type of runtime exception to be returned by the given
     *        exception factory and relayed by the exception formatter
     * @return the out-of-bounds exception formatter
     */
    public static <X extends RuntimeException>
    BiFunction<String, List<Number>, X> outOfBoundsExceptionFormatter(Function<String, X> f) {
        // Use anonymous class to avoid bootstrap issues if this method is
        // used early in startup
        return new BiFunction<String, List<Number>, X>() {
            @Override
            public X apply(String checkKind, List<Number> args) {
                return f.apply(outOfBoundsMessage(checkKind, args));
            }
        };
    }

    private static String outOfBoundsMessage(String checkKind, List<? extends Number> args) {
        if (checkKind == null && args == null) {
            return String.format("Range check failed");
        } else if (checkKind == null) {
            return String.format("Range check failed: %s", args);
        } else if (args == null) {
            return String.format("Range check failed: %s", checkKind);
        }

        int argSize = 0;
        switch (checkKind) {
            case "checkIndex":
                argSize = 2;
                break;
            case "checkFromToIndex":
            case "checkFromIndexSize":
                argSize = 3;
                break;
            default:
        }

        // Switch to default if fewer or more arguments than required are supplied
        switch ((args.size() != argSize) ? "" : checkKind) {
            case "checkIndex":
                return String.format("Index %s out of bounds for length %s",
                                     args.get(0), args.get(1));
            case "checkFromToIndex":
                return String.format("Range [%s, %s) out of bounds for length %s",
                                     args.get(0), args.get(1), args.get(2));
            case "checkFromIndexSize":
                return String.format("Range [%s, %<s + %s) out of bounds for length %s",
                                     args.get(0), args.get(1), args.get(2));
            default:
                return String.format("Range check failed: %s %s", checkKind, args);
        }
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The {@code index} is defined to be out of bounds if any of the
     * following inequalities is true:
     * <ul>
     *  <li>{@code index < 0}</li>
     *  <li>{@code index >= length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the {@code index} is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkIndex};
     * and an unmodifiable list of integers whose values are, in order, the
     * out-of-bounds arguments {@code index} and {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code index} if it is within bounds of the range
     * @throws X if the {@code index} is out of bounds and the exception
     *         formatter is non-{@code null}
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     *         and the exception formatter is {@code null}
     * @since 9
     *
     * @implNote
     * This method is made intrinsic in optimizing compilers to guide them to
     * perform unsigned comparisons of the index and length when it is known the
     * length is a non-negative value (such as that of an array length or from
     * the upper bound of a loop)
     */
    @IntrinsicCandidate
    public static <X extends RuntimeException>
    int checkIndex(int index, int length,
                   BiFunction<String, List<Number>, X> oobef) {
        if (index < 0 || index >= length)
            throw outOfBoundsCheckIndex(oobef, index, length);
        return index;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code fromIndex > toIndex}</li>
     *  <li>{@code toIndex > length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the sub-range is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkFromToIndex};
     * and an unmodifiable list of integers whose values are, in order, the
     * out-of-bounds arguments {@code fromIndex}, {@code toIndex}, and {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws X if the sub-range is out of bounds and the exception factory
     *         function is non-{@code null}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds and
     *         the exception factory function is {@code null}
     * @since 9
     */
    public static <X extends RuntimeException>
    int checkFromToIndex(int fromIndex, int toIndex, int length,
                         BiFunction<String, List<Number>, X> oobef) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw outOfBoundsCheckFromToIndex(oobef, fromIndex, toIndex, length);
        return fromIndex;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code size < 0}</li>
     *  <li>{@code fromIndex + size > length}, taking into account integer overflow</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the sub-range is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkFromIndexSize};
     * and an unmodifiable list of integers whose values are, in order, the
     * out-of-bounds arguments {@code fromIndex}, {@code size}, and
     * {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws X if the sub-range is out of bounds and the exception factory
     *         function is non-{@code null}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds and
     *         the exception factory function is {@code null}
     * @since 9
     */
    public static <X extends RuntimeException>
    int checkFromIndexSize(int fromIndex, int size, int length,
                           BiFunction<String, List<Number>, X> oobef) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw outOfBoundsCheckFromIndexSize(oobef, fromIndex, size, length);
        return fromIndex;
    }

    /**
     * Checks if the {@code index} is within the bounds of the range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The {@code index} is defined to be out of bounds if any of the
     * following inequalities is true:
     * <ul>
     *  <li>{@code index < 0}</li>
     *  <li>{@code index >= length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the {@code index} is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkIndex};
     * and an unmodifiable list of longs whose values are, in order, the
     * out-of-bounds arguments {@code index} and {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param index the index
     * @param length the upper-bound (exclusive) of the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code index} if it is within bounds of the range
     * @throws X if the {@code index} is out of bounds and the exception
     *         formatter is non-{@code null}
     * @throws IndexOutOfBoundsException if the {@code index} is out of bounds
     *         and the exception formatter is {@code null}
     * @since 16
     *
     * @implNote
     * This method is made intrinsic in optimizing compilers to guide them to
     * perform unsigned comparisons of the index and length when it is known the
     * length is a non-negative value (such as that of an array length or from
     * the upper bound of a loop)
     */
    @IntrinsicCandidate
    public static <X extends RuntimeException>
    long checkIndex(long index, long length,
                    BiFunction<String, List<Number>, X> oobef) {
        if (index < 0 || index >= length)
            throw outOfBoundsCheckIndex(oobef, index, length);
        return index;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) is within the bounds of range from {@code 0}
     * (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code fromIndex > toIndex}</li>
     *  <li>{@code toIndex > length}</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the sub-range is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkFromToIndex};
     * and an unmodifiable list of longs whose values are, in order, the
     * out-of-bounds arguments {@code fromIndex}, {@code toIndex}, and {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param fromIndex the lower-bound (inclusive) of the sub-range
     * @param toIndex the upper-bound (exclusive) of the sub-range
     * @param length the upper-bound (exclusive) the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws X if the sub-range is out of bounds and the exception factory
     *         function is non-{@code null}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds and
     *         the exception factory function is {@code null}
     * @since 16
     */
    public static <X extends RuntimeException>
    long checkFromToIndex(long fromIndex, long toIndex, long length,
                          BiFunction<String, List<Number>, X> oobef) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw outOfBoundsCheckFromToIndex(oobef, fromIndex, toIndex, length);
        return fromIndex;
    }

    /**
     * Checks if the sub-range from {@code fromIndex} (inclusive) to
     * {@code fromIndex + size} (exclusive) is within the bounds of range from
     * {@code 0} (inclusive) to {@code length} (exclusive).
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     *  <li>{@code fromIndex < 0}</li>
     *  <li>{@code size < 0}</li>
     *  <li>{@code fromIndex + size > length}, taking into account integer overflow</li>
     *  <li>{@code length < 0}, which is implied from the former inequalities</li>
     * </ul>
     *
     * <p>If the sub-range is out of bounds, then a runtime exception is
     * thrown that is the result of applying the following arguments to the
     * exception formatter: the name of this method, {@code checkFromIndexSize};
     * and an unmodifiable list of longs whose values are, in order, the
     * out-of-bounds arguments {@code fromIndex}, {@code size}, and
     * {@code length}.
     *
     * @param <X> the type of runtime exception to throw if the arguments are
     *        out of bounds
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @param oobef the exception formatter that when applied with this
     *        method name and out-of-bounds arguments returns a runtime
     *        exception.  If {@code null} or returns {@code null} then, it is as
     *        if an exception formatter produced from an invocation of
     *        {@code outOfBoundsExceptionFormatter(IndexOutOfBounds::new)} is used
     *        instead (though it may be more efficient).
     *        Exceptions thrown by the formatter are relayed to the caller.
     * @return {@code fromIndex} if the sub-range within bounds of the range
     * @throws X if the sub-range is out of bounds and the exception factory
     *         function is non-{@code null}
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds and
     *         the exception factory function is {@code null}
     * @since 16
     */
    public static <X extends RuntimeException>
    long checkFromIndexSize(long fromIndex, long size, long length,
                            BiFunction<String, List<Number>, X> oobef) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw outOfBoundsCheckFromIndexSize(oobef, fromIndex, size, length);
        return fromIndex;
    }
}
