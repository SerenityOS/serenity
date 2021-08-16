/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print;

import java.io.IOException;

/**
 * Interface {@code MultiDoc} specifies the interface for an object that
 * supplies more than one piece of print data for a Print Job. "Doc" is a short,
 * easy-to-pronounce term that means "a piece of print data," and a "multidoc"
 * is a group of several docs. The client passes to the Print Job an object that
 * implements interface {@code MultiDoc}, and the Print Job calls methods on
 * that object to obtain the print data.
 * <p>
 * Interface {@code MultiDoc} provides an abstraction similar to a "linked list"
 * of docs. A multidoc object is like a node in the linked list, containing the
 * current doc in the list and a pointer to the next node (multidoc) in the
 * list. The Print Job can call the multidoc's {@link #getDoc() getDoc()} method
 * to get the current doc. When it's ready to go on to the next doc, the Print
 * Job can call the multidoc's {@link #next() next()} method to get the next
 * multidoc, which contains the next doc. So Print Job code for accessing a
 * multidoc might look like this:
 *
 * <pre>
 *      void processMultiDoc(MultiDoc theMultiDoc) {
 *
 *          MultiDoc current = theMultiDoc;
 *
 *          while (current != null) {
 *              processDoc (current.getDoc());
 *              current = current.next();
 *          }
 *      }
 * </pre>
 * Of course, interface {@code MultiDoc} can be implemented in any way that
 * fulfills the contract; it doesn't have to use a linked list in the
 * implementation.
 * <p>
 * To get all the print data for a multidoc print job, a Print Service proxy
 * could use either of two patterns:
 * <ol type=1>
 *   <li>The <b>interleaved</b> pattern: Get the doc from the current multidoc.
 *   Get the print data representation object from the current doc. Get all the
 *   print data from the print data representation object. Get the next multidoc
 *   from the current multidoc, and repeat until there are no more. (The code
 *   example above uses the interleaved pattern.)
 *   <li>The <b>all-at-once</b> pattern: Get the doc from the current multidoc,
 *   and save the doc in a list. Get the next multidoc from the current
 *   multidoc, and repeat until there are no more. Then iterate over the list of
 *   saved docs. Get the print data representation object from the current doc.
 *   Get all the print data from the print data representation object. Go to the
 *   next doc in the list, and repeat until there are no more.
 * </ol>
 * Now, consider a printing client that is generating print data on the fly and
 * does not have the resources to store more than one piece of print data at a
 * time. If the print service proxy used the all-at-once pattern to get the
 * print data, it would pose a problem for such a client; the client would have
 * to keep all the docs' print data around until the print service proxy comes
 * back and asks for them, which the client is not able to do. To work with such
 * a client, the print service proxy must use the interleaved pattern.
 * <p>
 * To address this problem, and to simplify the design of clients providing
 * multiple docs to a Print Job, every Print Service proxy that supports
 * multidoc print jobs is required to access a {@code MultiDoc} object using the
 * interleaved pattern. That is, given a {@code MultiDoc} object, the print
 * service proxy will call {@link #getDoc() getDoc()} one or more times until it
 * successfully obtains the current {@code Doc} object. The print service proxy
 * will then obtain the current doc's print data, not proceeding until all the
 * print data is obtained or an unrecoverable error occurs. If it is able to
 * continue, the print service proxy will then call {@link #next() next()} one
 * or more times until it successfully obtains either the next {@code MultiDoc}
 * object or an indication that there are no more. An implementation of
 * interface {@code MultiDoc} can assume the print service proxy will follow
 * this interleaved pattern; for any other pattern of usage, the
 * {@code MultiDoc} implementation's behavior is unspecified.
 * <p>
 * There is no restriction on the number of client threads that may be
 * simultaneously accessing the same multidoc. Therefore, all implementations of
 * interface MultiDoc must be designed to be multiple thread safe. In fact, a
 * client thread could be adding docs to the end of the (conceptual) list while
 * a Print Job thread is simultaneously obtaining docs from the beginning of the
 * list; provided the multidoc object synchronizes the threads properly, the two
 * threads will not interfere with each other.
 */
public interface MultiDoc {

    /**
     * Obtain the current doc object.
     *
     * @return current doc object
     * @throws IOException if an error occurred when reading the document
     */
    public Doc getDoc() throws IOException;

    /**
     * Go to the multidoc object that contains the next doc object in the
     * sequence of doc objects.
     *
     * @return multidoc object containing the next doc object, or {@code null}
     *         if there are no further doc objects
     * @throws IOException if an error occurred locating the next document
     */
    public MultiDoc next() throws IOException;
}
