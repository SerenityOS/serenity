/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/**
 * An ordered sequence of constants, some of which may not yet
 * be present.  This type is used by {@link BootstrapCallInfo}
 * to represent the sequence of bootstrap arguments associated
 * with a bootstrap method, without forcing their immediate
 * resolution.
 * <p>
 * If you use the
 * {@linkplain ConstantGroup#get(int) simple get method},
 * the constant will be resolved, if this has not already
 * happened.  An occasional side effect of resolution is a
 * {@code LinkageError}, which happens if the system
 * could not resolve the constant in question.
 * <p>
 * In order to peek at a constant without necessarily
 * resolving it, use the
 * {@linkplain ConstantGroup#get(int,Object)
 * non-throwing get method}.
 * This method will never throw a resolution error.
 * Instead, if the resolution would result in an error,
 * or if the implementation elects not to attempt
 * resolution at this point, then the method will
 * return the user-supplied sentinel value.
 * <p>
 * To iterate through the constants, resolving as you go,
 * use the iterator provided on the {@link List}-typed view.
 * If you supply a sentinel, resolution will be suppressed.
 * <p>
 * Typically the constant is drawn from a constant pool entry
 * in the virtual machine. Constant pool entries undergo a
 * one-time state transition from unresolved to resolved,
 * with a permanently recorded result.  Usually that result
 * is the desired constant value, but it may also be an error.
 * In any case, the results displayed by a {@code ConstantGroup}
 * are stable in the same way.  If a query to a particular
 * constant in a {@code ConstantGroup} throws an exception once,
 * it will throw the same kind of exception forever after.
 * If the query returns a constant value once, it will return
 * the same value forever after.
 * <p>
 * The only possible change in the status of a constant is
 * from the unresolved to the resolved state, and that
 * happens exactly once.  A constant will never revert to
 * an unlinked state.  However, from the point of view of
 * this interface, constants may appear to spontaneously
 * resolve.  This is so because constant pools are global
 * structures shared across threads, and because
 * prefetching of some constants may occur, there are no
 * strong guarantees when the virtual machine may resolve
 * constants.
 * <p>
 * When choosing sentinel values, be aware that a constant
 * pool which has {@code CONSTANT_Dynamic} entries
 * can contain potentially any representable value,
 * and arbitrary implementations of {@code ConstantGroup}
 * are also free to produce arbitrary values.
 * This means some obvious choices for sentinel values,
 * such as {@code null}, may sometimes fail to distinguish
 * a resolved from an unresolved constant in the group.
 * The most reliable sentinel is a privately created object,
 * or perhaps the {@code ConstantGroup} itself.
 * @since 1.10
 */
// public
interface ConstantGroup {
    /// Access

    /**
     * Returns the number of constants in this group.
     * This value never changes, for any particular group.
     * @return the number of constants in this group
     */
    int size();

    /**
     * Returns the selected constant, resolving it if necessary.
     * Throws a linkage error if resolution proves impossible.
     * @param index which constant to select
     * @return the selected constant
     * @throws LinkageError if the selected constant needs resolution and cannot be resolved
     */
    Object get(int index) throws LinkageError;

    /**
     * Returns the selected constant,
     * or the given sentinel value if there is none available.
     * If the constant cannot be resolved, the sentinel will be returned.
     * If the constant can (perhaps) be resolved, but has not yet been resolved,
     * then the sentinel <em>may</em> be returned, at the implementation's discretion.
     * To force resolution (and a possible exception), call {@link #get(int)}.
     * @param index the selected constant
     * @param ifNotPresent the sentinel value to return if the constant is not present
     * @return the selected constant, if available, else the sentinel value
     */
    Object get(int index, Object ifNotPresent);

    /**
     * Returns an indication of whether a constant may be available.
     * If it returns {@code true}, it will always return true in the future,
     * and a call to {@link #get(int)} will never throw an exception.
     * <p>
     * After a normal return from {@link #get(int)} or a present
     * value is reported from {@link #get(int,Object)}, this method
     * must always return true.
     * <p>
     * If this method returns {@code false}, nothing in particular
     * can be inferred, since the query only concerns the internal
     * logic of the {@code ConstantGroup} object which ensures that
     * a successful query to a constant will always remain successful.
     * The only way to force a permanent decision about whether
     * a constant is available is to call {@link #get(int)} and
     * be ready for an exception if the constant is unavailable.
     * @param index the selected constant
     * @return {@code true} if the selected constant is known by
     *     this object to be present, {@code false} if it is known
     *     not to be present or
     */
    boolean isPresent(int index);

    /// Views

    /**
     * Create a view on this group as a {@link List} view.
     * Any request for a constant through this view will
     * force resolution.
     * @return a {@code List} view on this group which will force resolution
     */
    default List<Object> asList() {
        return new AbstractConstantGroup.AsList(this, 0, size());
    }

    /**
     * Create a view on this group as a {@link List} view.
     * Any request for a constant through this view will
     * return the given sentinel value, if the corresponding
     * call to {@link #get(int,Object)} would do so.
     * @param ifNotPresent the sentinel value to return if a constant is not present
     * @return a {@code List} view on this group which will not force resolution
     */
    default List<Object> asList(Object ifNotPresent) {
        return new AbstractConstantGroup.AsList(this, 0, size(), ifNotPresent);
    }

    /**
     * Create a view on a sub-sequence of this group.
     * @param start the index to begin the view
     * @param end the index to end the view
     * @return a view on the selected sub-group
     */
    default ConstantGroup subGroup(int start, int end) {
        return new AbstractConstantGroup.SubGroup(this, start, end);
    }

    /// Bulk operations

    /**
     * Copy a sequence of constant values into a given buffer.
     * This is equivalent to {@code end-offset} separate calls to {@code get},
     * for each index in the range from {@code offset} up to but not including {@code end}.
     * For the first constant that cannot be resolved,
     * a {@code LinkageError} is thrown, but only after
     * preceding constant value have been stored.
     * @param start index of first constant to retrieve
     * @param end limiting index of constants to retrieve
     * @param buf array to receive the requested values
     * @param pos position in the array to offset storing the values
     * @return the limiting index, {@code end}
     * @throws LinkageError if a constant cannot be resolved
     */
    default int copyConstants(int start, int end,
                              Object[] buf, int pos)
            throws LinkageError
    {
        int bufBase = pos - start;  // buf[bufBase + i] = get(i)
        for (int i = start; i < end; i++) {
            buf[bufBase + i] = get(i);
        }
        return end;
    }

    /**
     * Copy a sequence of constant values into a given buffer.
     * This is equivalent to {@code end-offset} separate calls to {@code get},
     * for each index in the range from {@code offset} up to but not including {@code end}.
     * Any constants that cannot be resolved are replaced by the
     * given sentinel value.
     * @param start index of first constant to retrieve
     * @param end limiting index of constants to retrieve
     * @param buf array to receive the requested values
     * @param pos position in the array to offset storing the values
     * @param ifNotPresent sentinel value to store if a value is not available
     * @return the limiting index, {@code end}
     * @throws LinkageError if {@code resolve} is true and a constant cannot be resolved
     */
    default int copyConstants(int start, int end,
                              Object[] buf, int pos,
                              Object ifNotPresent) {
        int bufBase = pos - start;  // buf[bufBase + i] = get(i)
        for (int i = start; i < end; i++) {
            buf[bufBase + i] = get(i, ifNotPresent);
        }
        return end;
    }

    /**
     * Make a new constant group with the given constants.
     * The value of {@code ifNotPresent} may be any reference.
     * If this value is encountered as an element of the
     * {@code constants} list, the new constant group will
     * regard that element of the list as logically missing.
     * If the new constant group is called upon to resolve
     * a missing element of the group, it will refer to the
     * given {@code constantProvider}, by calling it on the
     * index of the missing element.
     * The {@code constantProvider} must be stable, in the sense
     * that the outcome of calling it on the same index twice
     * will produce equivalent results.
     * If {@code constantProvider} is the null reference, then
     * it will be treated as if it were a function which raises
     * {@link NoSuchElementException}.
     * @param constants the elements of this constant group
     * @param ifNotPresent sentinel value provided instead of a missing constant
     * @param constantProvider function to call when a missing constant is resolved
     * @return a new constant group with the given constants and resolution behavior
     */
    static ConstantGroup makeConstantGroup(List<Object> constants,
                                           Object ifNotPresent,
                                           IntFunction<Object> constantProvider) {
        class Impl extends AbstractConstantGroup.WithCache {
            Impl() {
                super(constants.size());
                initializeCache(constants, ifNotPresent);
            }
            @Override
            Object fillCache(int index) {
                if (constantProvider == null)  super.fillCache(index);
                return constantProvider.apply(index);
            }
        }
        return new Impl();
    }

    /**
     * Make a new constant group with the given constant values.
     * The constants will be copied from the given list into the
     * new constant group, forcing resolution if any are missing.
     * @param constants the constants of this constant group
     * @return a new constant group with the given constants
     */
    static ConstantGroup makeConstantGroup(List<Object> constants) {
        final Object NP = AbstractConstantGroup.WithCache.NOT_PRESENT;
        assert(!constants.contains(NP));  // secret value
        return makeConstantGroup(constants, NP, null);
    }

}
