/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.nio.file.FileTreeWalker.Event;

/**
 * An {@code Iterator} to iterate over the nodes of a file tree.
 *
 * <pre>{@code
 *     try (FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options)) {
 *         while (iterator.hasNext()) {
 *             Event ev = iterator.next();
 *             Path path = ev.file();
 *             BasicFileAttributes attrs = ev.attributes();
 *         }
 *     }
 * }</pre>
 */

class FileTreeIterator implements Iterator<Event>, Closeable {
    private final FileTreeWalker walker;
    private Event next;

    /**
     * Creates a new iterator to walk the file tree starting at the given file.
     *
     * @throws  IllegalArgumentException
     *          if {@code maxDepth} is negative
     * @throws  IOException
     *          if an I/O errors occurs opening the starting file
     * @throws  SecurityException
     *          if the security manager denies access to the starting file
     * @throws  NullPointerException
     *          if {@code start} or {@code options} is {@code null} or
     *          the options array contains a {@code null} element
     */
    FileTreeIterator(Path start, int maxDepth, FileVisitOption... options)
        throws IOException
    {
        this.walker = new FileTreeWalker(Arrays.asList(options), maxDepth);
        this.next = walker.walk(start);
        assert next.type() == FileTreeWalker.EventType.ENTRY ||
               next.type() == FileTreeWalker.EventType.START_DIRECTORY;

        // IOException if there a problem accessing the starting file
        IOException ioe = next.ioeException();
        if (ioe != null)
            throw ioe;
    }

    private void fetchNextIfNeeded() {
        if (next == null) {
            FileTreeWalker.Event ev = walker.next();
            while (ev != null) {
                IOException ioe = ev.ioeException();
                if (ioe != null)
                    throw new UncheckedIOException(ioe);

                // END_DIRECTORY events are ignored
                if (ev.type() != FileTreeWalker.EventType.END_DIRECTORY) {
                    next = ev;
                    return;
                }
                ev = walker.next();
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        return next != null;
    }

    @Override
    public Event next() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        if (next == null)
            throw new NoSuchElementException();
        Event result = next;
        next = null;
        return result;
    }

    @Override
    public void close() {
        walker.close();
    }
}
