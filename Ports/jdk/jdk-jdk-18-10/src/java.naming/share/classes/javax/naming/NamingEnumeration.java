/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming;

import java.util.Enumeration;

/**
  * This interface is for enumerating lists returned by
  * methods in the javax.naming and javax.naming.directory packages.
  * It extends Enumeration to allow as exceptions to be thrown during
  * the enumeration.
  *<p>
  * When a method such as list(), listBindings(), or search() returns
  * a NamingEnumeration, any exceptions encountered are reserved until
  * all results have been returned. At the end of the enumeration, the
  * exception is thrown (by hasMore());
  * <p>
  * For example, if the list() is
  * returning only a partial answer, the corresponding exception would
  * be PartialResultException. list() would first return a NamingEnumeration.
  * When the last of the results has been returned by the NamingEnumeration's
  * next(), invoking hasMore() would result in PartialResultException being thrown.
  *<p>
  * In another example, if a search() method was invoked with a specified
  * size limit of 'n'. If the answer consists of more than 'n' results,
  * search() would first return a NamingEnumeration.
  * When the n'th result has been returned by invoking next() on the
  * NamingEnumeration, a SizeLimitExceedException would then thrown when
  * hasMore() is invoked.
  *<p>
  * Note that if the program uses hasMoreElements() and nextElement() instead
  * to iterate through the NamingEnumeration, because these methods
  * cannot throw exceptions, no exception will be thrown. Instead,
  * in the previous example, after the n'th result has been returned by
  * nextElement(), invoking hasMoreElements() would return false.
  *<p>
  * Note also that NoSuchElementException is thrown if the program invokes
  * next() or nextElement() when there are no elements left in the enumeration.
  * The program can always avoid this exception by using hasMore() and
  * hasMoreElements() to check whether the end of the enumeration has been reached.
  *<p>
  * If an exception is thrown during an enumeration,
  * the enumeration becomes invalid.
  * Subsequent invocation of any method on that enumeration
  * will yield undefined results.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see Context#list
  * @see Context#listBindings
  * @see javax.naming.directory.DirContext#search
  * @see javax.naming.directory.Attributes#getAll
  * @see javax.naming.directory.Attributes#getIDs
  * @see javax.naming.directory.Attribute#getAll
  * @since 1.3
  */
public interface NamingEnumeration<T> extends Enumeration<T> {
    /**
      * Retrieves the next element in the enumeration.
      * This method allows naming exceptions encountered while
      * retrieving the next element to be caught and handled
      * by the application.
      * <p>
      * Note that {@code next()} can also throw the runtime exception
      * NoSuchElementException to indicate that the caller is
      * attempting to enumerate beyond the end of the enumeration.
      * This is different from a NamingException, which indicates
      * that there was a problem in obtaining the next element,
      * for example, due to a referral or server unavailability, etc.
      *
      * @return         The possibly null element in the enumeration.
      *     null is only valid for enumerations that can return
      *     null (e.g. Attribute.getAll() returns an enumeration of
      *     attribute values, and an attribute value can be null).
      * @throws NamingException If a naming exception is encountered while attempting
      *                 to retrieve the next element. See NamingException
      *                 and its subclasses for the possible naming exceptions.
      * @throws java.util.NoSuchElementException If attempting to get the next element when none is available.
      * @see java.util.Enumeration#nextElement
      */
    public T next() throws NamingException;

    /**
      * Determines whether there are any more elements in the enumeration.
      * This method allows naming exceptions encountered while
      * determining whether there are more elements to be caught and handled
      * by the application.
      *
      * @return         true if there is more in the enumeration ; false otherwise.
      * @throws NamingException
      *                 If a naming exception is encountered while attempting
      *                 to determine whether there is another element
      *                 in the enumeration. See NamingException
      *                 and its subclasses for the possible naming exceptions.
      * @see java.util.Enumeration#hasMoreElements
      */
    public boolean hasMore() throws NamingException;

    /**
     * Closes this enumeration.
     *
     * After this method has been invoked on this enumeration, the
     * enumeration becomes invalid and subsequent invocation of any of
     * its methods will yield undefined results.
     * This method is intended for aborting an enumeration to free up resources.
     * If an enumeration proceeds to the end--that is, until
     * {@code hasMoreElements()} or {@code hasMore()} returns {@code false}--
     * resources will be freed up automatically and there is no need to
     * explicitly call {@code close()}.
     *<p>
     * This method indicates to the service provider that it is free
     * to release resources associated with the enumeration, and can
     * notify servers to cancel any outstanding requests. The {@code close()}
     * method is a hint to implementations for managing their resources.
     * Implementations are encouraged to use appropriate algorithms to
     * manage their resources when client omits the {@code close()} calls.
     *
     * @throws NamingException If a naming exception is encountered
     * while closing the enumeration.
     * @since 1.3
     */
    public void close() throws NamingException;
}
