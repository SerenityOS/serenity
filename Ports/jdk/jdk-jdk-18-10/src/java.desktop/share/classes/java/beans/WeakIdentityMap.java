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

package java.beans;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Hash table based mapping, which uses weak references to store keys
 * and reference-equality in place of object-equality to compare them.
 * An entry will automatically be removed when its key is no longer
 * in ordinary use.  Both null values and the null key are supported.
 * This class does not require additional synchronization.
 * A thread-safety is provided by a fragile combination
 * of synchronized blocks and volatile fields.
 * Be very careful during editing!
 *
 * @see java.util.IdentityHashMap
 * @see java.util.WeakHashMap
 */
abstract class WeakIdentityMap<T> {

    private static final int MAXIMUM_CAPACITY = 1 << 30; // it MUST be a power of two
    private static final Object NULL = new Object(); // special object for null key

    private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    private volatile Entry<T>[] table = newTable(1<<3); // table's length MUST be a power of two
    private int threshold = 6; // the next size value at which to resize
    private int size = 0; // the number of key-value mappings

    public T get(Object key) {
        removeStaleEntries();
        if (key == null) {
            key = NULL;
        }
        int hash = key.hashCode();
        Entry<T>[] table = this.table;
        // unsynchronized search improves performance
        // the null value does not mean that there are no needed entry
        int index = getIndex(table, hash);
        for (Entry<T> entry = table[index]; entry != null; entry = entry.next) {
            if (entry.isMatched(key, hash)) {
                return entry.value;
            }
        }
        synchronized (NULL) {
            // synchronized search improves stability
            // we must create and add new value if there are no needed entry
            index = getIndex(this.table, hash);
            for (Entry<T> entry = this.table[index]; entry != null; entry = entry.next) {
                if (entry.isMatched(key, hash)) {
                    return entry.value;
                }
            }
            T value = create(key);
            this.table[index] = new Entry<T>(key, hash, value, this.queue, this.table[index]);
            if (++this.size >= this.threshold) {
                if (this.table.length == MAXIMUM_CAPACITY) {
                    this.threshold = Integer.MAX_VALUE;
                }
                else {
                    removeStaleEntries();
                    table = newTable(this.table.length * 2);
                    transfer(this.table, table);
                    // If ignoring null elements and processing ref queue caused massive
                    // shrinkage, then restore old table.  This should be rare, but avoids
                    // unbounded expansion of garbage-filled tables.
                    if (this.size >= this.threshold / 2) {
                        this.table = table;
                        this.threshold *= 2;
                    }
                    else {
                        transfer(table, this.table);
                    }
                }
            }
            return value;
        }
    }

    protected abstract T create(Object key);

    private void removeStaleEntries() {
        Object ref = this.queue.poll();
        if (ref != null) {
            synchronized (NULL) {
                do {
                    @SuppressWarnings("unchecked")
                    Entry<T> entry = (Entry<T>) ref;
                    int index = getIndex(this.table, entry.hash);

                    Entry<T> prev = this.table[index];
                    Entry<T> current = prev;
                    while (current != null) {
                        Entry<T> next = current.next;
                        if (current == entry) {
                            if (prev == entry) {
                                this.table[index] = next;
                            }
                            else {
                                prev.next = next;
                            }
                            entry.value = null; // Help GC
                            entry.next = null; // Help GC
                            this.size--;
                            break;
                        }
                        prev = current;
                        current = next;
                    }
                    ref = this.queue.poll();
                }
                while (ref != null);
            }
        }
    }

    private void transfer(Entry<T>[] oldTable, Entry<T>[] newTable) {
        for (int i = 0; i < oldTable.length; i++) {
            Entry<T> entry = oldTable[i];
            oldTable[i] = null;
            while (entry != null) {
                Entry<T> next = entry.next;
                Object key = entry.get();
                if (key == null) {
                    entry.value = null; // Help GC
                    entry.next = null; // Help GC
                    this.size--;
                }
                else {
                    int index = getIndex(newTable, entry.hash);
                    entry.next = newTable[index];
                    newTable[index] = entry;
                }
                entry = next;
            }
        }
    }


    @SuppressWarnings("unchecked")
    private Entry<T>[] newTable(int length) {
        return (Entry<T>[]) new Entry<?>[length];
    }

    private static int getIndex(Entry<?>[] table, int hash) {
        return hash & (table.length - 1);
    }

    private static class Entry<T> extends WeakReference<Object> {
        private final int hash;
        private volatile T value;
        private volatile Entry<T> next;

        Entry(Object key, int hash, T value, ReferenceQueue<Object> queue, Entry<T> next) {
            super(key, queue);
            this.hash = hash;
            this.value = value;
            this.next  = next;
        }

        boolean isMatched(Object key, int hash) {
            return (this.hash == hash) && (key == get());
        }
    }
}
