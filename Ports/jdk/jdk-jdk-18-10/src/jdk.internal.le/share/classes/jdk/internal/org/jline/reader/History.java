/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Console history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public interface History extends Iterable<History.Entry>
{

    /**
     * Initialize the history for the given reader.
     * @param reader the reader to attach to
     */
    void attach(LineReader reader);

    /**
     * Load history.
     * @throws IOException if a problem occurs
     */
    void load() throws IOException;

    /**
     * Save history.
     * @throws IOException if a problem occurs
     */
    void save() throws IOException;

    /**
     * Write history to the file. If incremental only the events that are new since the last incremental operation to
     * the file are added.
     * @param  file        History file
     * @param  incremental If true incremental write operation is performed.
     * @throws IOException if a problem occurs
     */
    void write(Path file, boolean incremental) throws IOException;

    /**
     * Append history to the file. If incremental only the events that are new since the last incremental operation to
     * the file are added.
     * @param  file        History file
     * @param  incremental If true incremental append operation is performed.
     * @throws IOException if a problem occurs
     */
    void append(Path file, boolean incremental) throws IOException;

    /**
     * Read history from the file. If incremental only the events that are not contained within the internal list are added.
     * @param  file        History file
     * @param  incremental If true incremental read operation is performed.
     * @throws IOException if a problem occurs
     */
    void read(Path file, boolean incremental) throws IOException;

    /**
     * Purge history.
     * @throws IOException if a problem occurs
     */
    void purge() throws IOException;


    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    int index();

    int first();

    int last();

    String get(int index);

    default void add(String line) {
        add(Instant.now(), line);
    }

    void add(Instant time, String line);

    /**
     * Check if an entry should be persisted or not.
     *
     * @param entry the entry to check
     * @return <code>true</code> if the given entry should be persisted, <code>false</code> otherwise
     */
    default boolean isPersistable(Entry entry) {
        return true;
    }

    //
    // Entries
    //

    interface Entry
    {
        int index();

        Instant time();

        String line();
    }

    ListIterator<Entry> iterator(int index);

    default ListIterator<Entry> iterator() {
        return iterator(first());
    }

    default Iterator<Entry> reverseIterator() {
        return reverseIterator(last());
    }

    default Iterator<Entry> reverseIterator(int index) {
        return new Iterator<Entry>() {
            private final ListIterator<Entry> it = iterator(index + 1);
            @Override
            public boolean hasNext() {
                return it.hasPrevious();
            }
            @Override
            public Entry next() {
                return it.previous();
            }
            @Override
            public void remove() {
                it.remove();
                resetIndex();
            }
        };
    }

    //
    // Navigation
    //

    /**
     * Return the content of the current buffer.
     *
     * @return the content of the current buffer
     */
    String current();

    /**
     * Move the pointer to the previous element in the buffer.
     *
     * @return true if we successfully went to the previous element
     */
    boolean previous();

    /**
     * Move the pointer to the next element in the buffer.
     *
     * @return true if we successfully went to the next element
     */
    boolean next();

    /**
     * Moves the history index to the first entry.
     *
     * @return Return false if there are no iterator in the history or if the
     * history is already at the beginning.
     */
    boolean moveToFirst();

    /**
     * This moves the history to the last entry. This entry is one position
     * before the moveToEnd() position.
     *
     * @return Returns false if there were no history iterator or the history
     * index was already at the last entry.
     */
    boolean moveToLast();

    /**
     * Move to the specified index in the history
     *
     * @param index The index to move to.
     * @return      Returns true if the index was moved.
     */
    boolean moveTo(int index);

    /**
     * Move to the end of the history buffer. This will be a blank entry, after
     * all of the other iterator.
     */
    void moveToEnd();

    /**
     * Reset index after remove
     */
    void resetIndex();
}
