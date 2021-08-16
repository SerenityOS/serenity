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
package com.sun.beans.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Hash table based implementation of the cache,
 * which allows to use weak or soft references for keys and values.
 * An entry in a {@code Cache} will automatically be removed
 * when its key or value is no longer in ordinary use.
 *
 * @author Sergey Malenkov
 * @since 1.8
 */
public abstract class Cache<K,V> {
    private static final int MAXIMUM_CAPACITY = 1 << 30; // maximum capacity MUST be a power of two <= 1<<30

    private final boolean identity; // defines whether the identity comparison is used
    private final Kind keyKind; // a reference kind for the cache keys
    private final Kind valueKind; // a reference kind for the cache values

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>(); // queue for references to remove

    private volatile CacheEntry<K,V>[] table = newTable(1 << 3); // table's length MUST be a power of two
    private int threshold = 6; // the next size value at which to resize
    private int size; // the number of key-value mappings contained in this map

    /**
     * Creates a corresponding value for the specified key.
     *
     * @param key a key that can be used to create a value
     * @return a corresponding value for the specified key
     */
    public abstract V create(K key);

    /**
     * Constructs an empty {@code Cache}.
     * The default initial capacity is 8.
     * The default load factor is 0.75.
     *
     * @param keyKind   a reference kind for keys
     * @param valueKind a reference kind for values
     *
     * @throws NullPointerException if {@code keyKind} or {@code valueKind} are {@code null}
     */
    public Cache(Kind keyKind, Kind valueKind) {
        this(keyKind, valueKind, false);
    }

    /**
     * Constructs an empty {@code Cache}
     * with the specified comparison method.
     * The default initial capacity is 8.
     * The default load factor is 0.75.
     *
     * @param keyKind   a reference kind for keys
     * @param valueKind a reference kind for values
     * @param identity  defines whether reference-equality
     *                  is used in place of object-equality
     *
     * @throws NullPointerException if {@code keyKind} or {@code valueKind} are {@code null}
     */
    public Cache(Kind keyKind, Kind valueKind, boolean identity) {
        Objects.requireNonNull(keyKind, "keyKind");
        Objects.requireNonNull(valueKind, "valueKind");
        this.keyKind = keyKind;
        this.valueKind = valueKind;
        this.identity = identity;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if there is no mapping for the key.
     *
     * @param key the key whose cached value is to be returned
     * @return a value to which the specified key is mapped,
     *         or {@code null} if there is no mapping for {@code key}
     *
     * @throws NullPointerException if {@code key} is {@code null}
     *                              or corresponding value is {@code null}
     */
    public final V get(K key) {
        Objects.requireNonNull(key, "key");
        removeStaleEntries();
        int hash = hash(key);
        // unsynchronized search improves performance
        // the null value does not mean that there are no needed entry
        CacheEntry<K,V>[] table = this.table; // unsynchronized access
        V current = getEntryValue(key, hash, table[index(hash, table)]);
        if (current != null) {
            return current;
        }
        synchronized (this.queue) {
            // synchronized search improves stability
            // we must create and add new value if there are no needed entry
            current = getEntryValue(key, hash, this.table[index(hash, this.table)]);
            if (current != null) {
                return current;
            }
            V value = create(key);
            Objects.requireNonNull(value, "value");
            int index = index(hash, this.table);
            this.table[index] = new CacheEntry<>(hash, key, value, this.table[index]);
            if (++this.size >= this.threshold) {
                if (this.table.length == MAXIMUM_CAPACITY) {
                    this.threshold = Integer.MAX_VALUE;
                } else {
                    removeStaleEntries();
                    table = newTable(this.table.length << 1);
                    transfer(this.table, table);
                    // If ignoring null elements and processing ref queue caused massive
                    // shrinkage, then restore old table.  This should be rare, but avoids
                    // unbounded expansion of garbage-filled tables.
                    if (this.size >= this.threshold / 2) {
                        this.table = table;
                        this.threshold <<= 1;
                    } else {
                        transfer(table, this.table);
                    }
                    removeStaleEntries();
                }
            }
            return value;
        }
    }

    /**
     * Removes the cached value that corresponds to the specified key.
     *
     * @param key the key whose mapping is to be removed from this cache
     */
    public final void remove(K key) {
        if (key != null) {
            synchronized (this.queue) {
                removeStaleEntries();
                int hash = hash(key);
                int index = index(hash, this.table);
                CacheEntry<K,V> prev = this.table[index];
                CacheEntry<K,V> entry = prev;
                while (entry != null) {
                    CacheEntry<K,V> next = entry.next;
                    if (entry.matches(hash, key)) {
                        if (entry == prev) {
                            this.table[index] = next;
                        } else {
                            prev.next = next;
                        }
                        entry.unlink();
                        break;
                    }
                    prev = entry;
                    entry = next;
                }
            }
        }
    }

    /**
     * Removes all of the mappings from this cache.
     * It will be empty after this call returns.
     */
    public final void clear() {
        synchronized (this.queue) {
            int index = this.table.length;
            while (0 < index--) {
                CacheEntry<K,V> entry = this.table[index];
                while (entry != null) {
                    CacheEntry<K,V> next = entry.next;
                    entry.unlink();
                    entry = next;
                }
                this.table[index] = null;
            }
            while (null != this.queue.poll()) {
                // Clear out the reference queue.
            }
        }
    }

    /**
     * Retrieves object hash code and applies a supplemental hash function
     * to the result hash, which defends against poor quality hash functions.
     * This is critical because {@code Cache} uses power-of-two length hash tables,
     * that otherwise encounter collisions for hashCodes that do not differ
     * in lower bits.
     *
     * @param key the object which hash code is to be calculated
     * @return a hash code value for the specified object
     */
    private int hash(Object key) {
        if (this.identity) {
            int hash = System.identityHashCode(key);
            return (hash << 1) - (hash << 8);
        }
        int hash = key.hashCode();
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        hash ^= (hash >>> 20) ^ (hash >>> 12);
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }

    /**
     * Returns index of the specified hash code in the given table.
     * Note that the table size must be a power of two.
     *
     * @param hash  the hash code
     * @param table the table
     * @return an index of the specified hash code in the given table
     */
    private static int index(int hash, Object[] table) {
        return hash & (table.length - 1);
    }

    /**
     * Creates a new array for the cache entries.
     *
     * @param size requested capacity MUST be a power of two
     * @return a new array for the cache entries
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private CacheEntry<K,V>[] newTable(int size) {
        return (CacheEntry<K,V>[]) new CacheEntry[size];
    }

    private V getEntryValue(K key, int hash, CacheEntry<K,V> entry) {
        while (entry != null) {
            if (entry.matches(hash, key)) {
                return entry.value.getReferent();
            }
            entry = entry.next;
        }
        return null;
    }

    private void removeStaleEntries() {
        Object reference = this.queue.poll();
        if (reference != null) {
            synchronized (this.queue) {
                do {
                    if (reference instanceof Ref) {
                        @SuppressWarnings("rawtypes")
                        Ref ref = (Ref) reference;
                        @SuppressWarnings("unchecked")
                        CacheEntry<K,V> owner = (CacheEntry<K,V>) ref.getOwner();
                        if (owner != null) {
                            int index = index(owner.hash, this.table);
                            CacheEntry<K,V> prev = this.table[index];
                            CacheEntry<K,V> entry = prev;
                            while (entry != null) {
                                CacheEntry<K,V> next = entry.next;
                                if (entry == owner) {
                                    if (entry == prev) {
                                        this.table[index] = next;
                                    } else {
                                        prev.next = next;
                                    }
                                    entry.unlink();
                                    break;
                                }
                                prev = entry;
                                entry = next;
                            }
                        }
                    }
                    reference = this.queue.poll();
                }
                while (reference != null);
            }
        }
    }

    private void transfer(CacheEntry<K,V>[] oldTable, CacheEntry<K,V>[] newTable) {
        int oldIndex = oldTable.length;
        while (0 < oldIndex--) {
            CacheEntry<K,V> entry = oldTable[oldIndex];
            oldTable[oldIndex] = null;
            while (entry != null) {
                CacheEntry<K,V> next = entry.next;
                if (entry.key.isStale() || entry.value.isStale()) {
                    entry.unlink();
                } else {
                    int newIndex = index(entry.hash, newTable);
                    entry.next = newTable[newIndex];
                    newTable[newIndex] = entry;
                }
                entry = next;
            }
        }
    }

    /**
     * Represents a cache entry (key-value pair).
     */
    private final class CacheEntry<K,V> {
        private final int hash;
        private final Ref<K> key;
        private final Ref<V> value;
        private volatile CacheEntry<K,V> next;

        /**
         * Constructs an entry for the cache.
         *
         * @param hash  the hash code calculated for the entry key
         * @param key   the entry key
         * @param value the initial value of the entry
         * @param next  the next entry in a chain
         */
        private CacheEntry(int hash, K key, V value, CacheEntry<K,V> next) {
            this.hash = hash;
            this.key = Cache.this.keyKind.create(this, key, Cache.this.queue);
            this.value = Cache.this.valueKind.create(this, value, Cache.this.queue);
            this.next = next;
        }

        /**
         * Determines whether the entry has the given key with the given hash code.
         *
         * @param hash   an expected hash code
         * @param object an object to be compared with the entry key
         * @return {@code true} if the entry has the given key with the given hash code;
         *         {@code false} otherwise
         */
        private boolean matches(int hash, Object object) {
            if (this.hash != hash) {
                return false;
            }
            Object key = this.key.getReferent();
            return (key == object) || !Cache.this.identity && (key != null) && key.equals(object);
        }

        /**
         * Marks the entry as actually removed from the cache.
         */
        private void unlink() {
            this.next = null;
            this.key.removeOwner();
            this.value.removeOwner();
            Cache.this.size--;
        }
    }

    /**
     * Basic interface for references.
     * It defines the operations common for the all kind of references.
     *
     * @param <T> the type of object to refer
     */
    private static interface Ref<T> {
        /**
         * Returns the object that possesses information about the reference.
         *
         * @return the owner of the reference or {@code null} if the owner is unknown
         */
        Object getOwner();

        /**
         * Returns the object to refer.
         *
         * @return the referred object or {@code null} if it was collected
         */
        T getReferent();

        /**
         * Determines whether the referred object was taken by the garbage collector or not.
         *
         * @return {@code true} if the referred object was collected
         */
        boolean isStale();

        /**
         * Marks this reference as removed from the cache.
         */
        void removeOwner();
    }

    /**
     * Represents a reference kind.
     */
    public static enum Kind {
        STRONG {
            <T> Ref<T> create(Object owner, T value, ReferenceQueue<? super T> queue) {
                return new Strong<>(owner, value);
            }
        },
        SOFT {
            <T> Ref<T> create(Object owner, T referent, ReferenceQueue<? super T> queue) {
                return (referent == null)
                        ? new Strong<>(owner, referent)
                        : new Soft<>(owner, referent, queue);
            }
        },
        WEAK {
            <T> Ref<T> create(Object owner, T referent, ReferenceQueue<? super T> queue) {
                return (referent == null)
                        ? new Strong<>(owner, referent)
                        : new Weak<>(owner, referent, queue);
            }
        };

        /**
         * Creates a reference to the specified object.
         *
         * @param <T>      the type of object to refer
         * @param owner    the owner of the reference, if needed
         * @param referent the object to refer
         * @param queue    the queue to register the reference with,
         *                 or {@code null} if registration is not required
         * @return the reference to the specified object
         */
        abstract <T> Ref<T> create(Object owner, T referent, ReferenceQueue<? super T> queue);

        /**
         * This is an implementation of the {@link Cache.Ref} interface
         * that uses the strong references that prevent their referents
         * from being made finalizable, finalized, and then reclaimed.
         *
         * @param <T> the type of object to refer
         */
        private static final class Strong<T> implements Ref<T> {
            private Object owner;
            private final T referent;

            /**
             * Creates a strong reference to the specified object.
             *
             * @param owner    the owner of the reference, if needed
             * @param referent the non-null object to refer
             */
            private Strong(Object owner, T referent) {
                this.owner = owner;
                this.referent = referent;
            }

            /**
             * Returns the object that possesses information about the reference.
             *
             * @return the owner of the reference or {@code null} if the owner is unknown
             */
            public Object getOwner() {
                return this.owner;
            }

            /**
             * Returns the object to refer.
             *
             * @return the referred object
             */
            public T getReferent() {
                return this.referent;
            }

            /**
             * Determines whether the referred object was taken by the garbage collector or not.
             *
             * @return {@code true} if the referred object was collected
             */
            public boolean isStale() {
                return false;
            }

            /**
             * Marks this reference as removed from the cache.
             */
            public void removeOwner() {
                this.owner = null;
            }
        }

        /**
         * This is an implementation of the {@link Cache.Ref} interface
         * that uses the soft references that are cleared at the discretion
         * of the garbage collector in response to a memory request.
         *
         * @param <T> the type of object to refer
         * @see java.lang.ref.SoftReference
         */
        private static final class Soft<T> extends SoftReference<T> implements Ref<T> {
            private Object owner;

            /**
             * Creates a soft reference to the specified object.
             *
             * @param owner    the owner of the reference, if needed
             * @param referent the non-null object to refer
             * @param queue    the queue to register the reference with,
             *                 or {@code null} if registration is not required
             */
            private Soft(Object owner, T referent, ReferenceQueue<? super T> queue) {
                super(referent, queue);
                this.owner = owner;
            }

            /**
             * Returns the object that possesses information about the reference.
             *
             * @return the owner of the reference or {@code null} if the owner is unknown
             */
            public Object getOwner() {
                return this.owner;
            }

            /**
             * Returns the object to refer.
             *
             * @return the referred object or {@code null} if it was collected
             */
            public T getReferent() {
                return get();
            }

            /**
             * Determines whether the referred object was taken by the garbage collector or not.
             *
             * @return {@code true} if the referred object was collected
             */
            public boolean isStale() {
                return null == get();
            }

            /**
             * Marks this reference as removed from the cache.
             */
            public void removeOwner() {
                this.owner = null;
            }
        }

        /**
         * This is an implementation of the {@link Cache.Ref} interface
         * that uses the weak references that do not prevent their referents
         * from being made finalizable, finalized, and then reclaimed.
         *
         * @param <T> the type of object to refer
         * @see java.lang.ref.WeakReference
         */
        private static final class Weak<T> extends WeakReference<T> implements Ref<T> {
            private Object owner;

            /**
             * Creates a weak reference to the specified object.
             *
             * @param owner    the owner of the reference, if needed
             * @param referent the non-null object to refer
             * @param queue    the queue to register the reference with,
             *                 or {@code null} if registration is not required
             */
            private Weak(Object owner, T referent, ReferenceQueue<? super T> queue) {
                super(referent, queue);
                this.owner = owner;
            }

            /**
             * Returns the object that possesses information about the reference.
             *
             * @return the owner of the reference or {@code null} if the owner is unknown
             */
            public Object getOwner() {
                return this.owner;
            }

            /**
             * Returns the object to refer.
             *
             * @return the referred object or {@code null} if it was collected
             */
            public T getReferent() {
                return get();
            }

            /**
             * Determines whether the referred object was taken by the garbage collector or not.
             *
             * @return {@code true} if the referred object was collected
             */
            public boolean isStale() {
                return null == get();
            }

            /**
             * Marks this reference as removed from the cache.
             */
            public void removeOwner() {
                this.owner = null;
            }
        }
    }
}
