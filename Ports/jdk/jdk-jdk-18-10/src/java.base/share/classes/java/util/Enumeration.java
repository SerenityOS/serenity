/*
 * Copyright (c) 1994, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * An object that implements the Enumeration interface generates a
 * series of elements, one at a time. Successive calls to the
 * {@code nextElement} method return successive elements of the
 * series.
 * <p>
 * For example, to print all elements of a {@code Vector<E>} <i>v</i>:
 * <pre>
 *   for (Enumeration&lt;E&gt; e = v.elements(); e.hasMoreElements();)
 *       System.out.println(e.nextElement());</pre>
 * <p>
 * Methods are provided to enumerate through the elements of a
 * vector, the keys of a hashtable, and the values in a hashtable.
 * Enumerations are also used to specify the input streams to a
 * {@code SequenceInputStream}.
 *
 * @apiNote
 * The functionality of this interface is duplicated by the {@link Iterator}
 * interface.  In addition, {@code Iterator} adds an optional remove operation,
 * and has shorter method names.  New implementations should consider using
 * {@code Iterator} in preference to {@code Enumeration}. It is possible to
 * adapt an {@code Enumeration} to an {@code Iterator} by using the
 * {@link #asIterator} method.
 *
 * @see     java.util.Iterator
 * @see     java.io.SequenceInputStream
 * @see     java.util.Enumeration#nextElement()
 * @see     java.util.Hashtable
 * @see     java.util.Hashtable#elements()
 * @see     java.util.Hashtable#keys()
 * @see     java.util.Vector
 * @see     java.util.Vector#elements()
 *
 * @author  Lee Boynton
 * @since   1.0
 */
public interface Enumeration<E> {
    /**
     * Tests if this enumeration contains more elements.
     *
     * @return  {@code true} if and only if this enumeration object
     *           contains at least one more element to provide;
     *          {@code false} otherwise.
     */
    boolean hasMoreElements();

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide.
     *
     * @return     the next element of this enumeration.
     * @throws     NoSuchElementException  if no more elements exist.
     */
    E nextElement();

    /**
     * Returns an {@link Iterator} that traverses the remaining elements
     * covered by this enumeration. Traversal is undefined if any methods
     * are called on this enumeration after the call to {@code asIterator}.
     *
     * @apiNote
     * This method is intended to help adapt code that produces
     * {@code Enumeration} instances to code that consumes {@code Iterator}
     * instances. For example, the {@link java.util.jar.JarFile#entries()
     * JarFile.entries()} method returns an {@code Enumeration<JarEntry>}.
     * This can be turned into an {@code Iterator}, and then the
     * {@code forEachRemaining()} method can be used:
     *
     * <pre>{@code
     *     JarFile jarFile = ... ;
     *     jarFile.entries().asIterator().forEachRemaining(entry -> { ... });
     * }</pre>
     *
     * (Note that there is also a {@link java.util.jar.JarFile#stream()
     * JarFile.stream()} method that returns a {@code Stream} of entries,
     * which may be more convenient in some cases.)
     *
     * @implSpec
     * The default implementation returns an {@code Iterator} whose
     * {@link Iterator#hasNext hasNext} method calls this Enumeration's
     * {@code hasMoreElements} method, whose {@link Iterator#next next}
     * method calls this Enumeration's {@code nextElement} method, and
     * whose {@link Iterator#remove remove} method throws
     * {@code UnsupportedOperationException}.
     *
     * @return an Iterator representing the remaining elements of this Enumeration
     *
     * @since 9
     */
    default Iterator<E> asIterator() {
        return new Iterator<>() {
            @Override public boolean hasNext() {
                return hasMoreElements();
            }
            @Override public E next() {
                return nextElement();
            }
        };
    }
}
