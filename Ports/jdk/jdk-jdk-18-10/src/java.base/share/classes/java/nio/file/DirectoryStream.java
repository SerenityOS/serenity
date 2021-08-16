/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.util.Iterator;
import java.io.Closeable;
import java.io.IOException;

/**
 * An object to iterate over the entries in a directory. A directory stream
 * allows for the convenient use of the for-each construct to iterate over a
 * directory.
 *
 * <p> <b> While {@code DirectoryStream} extends {@code Iterable}, it is not a
 * general-purpose {@code Iterable} as it supports only a single {@code
 * Iterator}; invoking the {@link #iterator iterator} method to obtain a second
 * or subsequent iterator throws {@code IllegalStateException}. </b>
 *
 * <p> An important property of the directory stream's {@code Iterator} is that
 * its {@link Iterator#hasNext() hasNext} method is guaranteed to read-ahead by
 * at least one element. If {@code hasNext} method returns {@code true}, and is
 * followed by a call to the {@code next} method, it is guaranteed that the
 * {@code next} method will not throw an exception due to an I/O error, or
 * because the stream has been {@link #close closed}. The {@code Iterator} does
 * not support the {@link Iterator#remove remove} operation.
 *
 * <p> A {@code DirectoryStream} is opened upon creation and is closed by
 * invoking the {@code close} method. Closing a directory stream releases any
 * resources associated with the stream. Failure to close the stream may result
 * in a resource leak. The try-with-resources statement provides a useful
 * construct to ensure that the stream is closed:
 * <pre>
 *   Path dir = ...
 *   try (DirectoryStream&lt;Path&gt; stream = Files.newDirectoryStream(dir)) {
 *       for (Path entry: stream) {
 *           ...
 *       }
 *   }
 * </pre>
 *
 * <p> Once a directory stream is closed, then further access to the directory,
 * using the {@code Iterator}, behaves as if the end of stream has been reached.
 * Due to read-ahead, the {@code Iterator} may return one or more elements
 * after the directory stream has been closed. Once these buffered elements
 * have been read, then subsequent calls to the {@code hasNext} method returns
 * {@code false}, and subsequent calls to the {@code next} method will throw
 * {@code NoSuchElementException}.
 *
 * <p> A directory stream is not required to be <i>asynchronously closeable</i>.
 * If a thread is blocked on the directory stream's iterator reading from the
 * directory, and another thread invokes the {@code close} method, then the
 * second thread may block until the read operation is complete.
 *
 * <p> If an I/O error is encountered when accessing the directory then it
 * causes the {@code Iterator}'s {@code hasNext} or {@code next} methods to
 * throw {@link DirectoryIteratorException} with the {@link IOException} as the
 * cause. As stated above, the {@code hasNext} method is guaranteed to
 * read-ahead by at least one element. This means that if {@code hasNext} method
 * returns {@code true}, and is followed by a call to the {@code next} method,
 * then it is guaranteed that the {@code next} method will not fail with a
 * {@code DirectoryIteratorException}.
 *
 * <p> The elements returned by the iterator are in no specific order. Some file
 * systems maintain special links to the directory itself and the directory's
 * parent directory. Entries representing these links are not returned by the
 * iterator.
 *
 * <p> The iterator is <i>weakly consistent</i>. It is thread safe but does not
 * freeze the directory while iterating, so it may (or may not) reflect updates
 * to the directory that occur after the {@code DirectoryStream} is created.
 *
 * <p> <b>Usage Examples:</b>
 * Suppose we want a list of the source files in a directory. This example uses
 * both the for-each and try-with-resources constructs.
 * <pre>
 *   List&lt;Path&gt; listSourceFiles(Path dir) throws IOException {
 *       List&lt;Path&gt; result = new ArrayList&lt;&gt;();
 *       try (DirectoryStream&lt;Path&gt; stream = Files.newDirectoryStream(dir, "*.{c,h,cpp,hpp,java}")) {
 *           for (Path entry: stream) {
 *               result.add(entry);
 *           }
 *       } catch (DirectoryIteratorException ex) {
 *           // I/O error encountered during the iteration, the cause is an IOException
 *           throw ex.getCause();
 *       }
 *       return result;
 *   }
 * </pre>
 * @param   <T>     The type of element returned by the iterator
 *
 * @since 1.7
 *
 * @see Files#newDirectoryStream(Path)
 */

public interface DirectoryStream<T>
    extends Closeable, Iterable<T> {
    /**
     * An interface that is implemented by objects that decide if a directory
     * entry should be accepted or filtered. A {@code Filter} is passed as the
     * parameter to the {@link Files#newDirectoryStream(Path,DirectoryStream.Filter)}
     * method when opening a directory to iterate over the entries in the
     * directory.
     *
     * @param   <T>     the type of the directory entry
     *
     * @since 1.7
     */
    @FunctionalInterface
    public static interface Filter<T> {
        /**
         * Decides if the given directory entry should be accepted or filtered.
         *
         * @param   entry
         *          the directory entry to be tested
         *
         * @return  {@code true} if the directory entry should be accepted
         *
         * @throws  IOException
         *          If an I/O error occurs
         */
        boolean accept(T entry) throws IOException;
    }

    /**
     * Returns the iterator associated with this {@code DirectoryStream}.
     *
     * @return  the iterator associated with this {@code DirectoryStream}
     *
     * @throws  IllegalStateException
     *          if this directory stream is closed or the iterator has already
     *          been returned
     */
    @Override
    Iterator<T> iterator();
}
