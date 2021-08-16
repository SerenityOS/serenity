/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.util;


/**
 * This class is an unsynchronized hash table primary used for String
 * to Object mapping.
 * <p>
 * The hash code uses the same algorithm as SymbolTable class.
 *
 * @author Elena Litani
 * @LastModified: Nov 2017
 */
public class SymbolHash {

    //
    // Constants
    //

    /** Default table size. */
    protected static final int TABLE_SIZE = 101;

    /** Maximum hash collisions per bucket. */
    protected static final int MAX_HASH_COLLISIONS = 40;

    protected static final int MULTIPLIERS_SIZE = 1 << 5;
    protected static final int MULTIPLIERS_MASK = MULTIPLIERS_SIZE - 1;

    //
    // Data
    //

    /** Actual table size **/
    protected int fTableSize;

    /** Buckets. */
    protected Entry[] fBuckets;

    /** Number of elements. */
    protected int fNum = 0;

    /**
     * Array of randomly selected hash function multipliers or <code>null</code>
     * if the default String.hashCode() function should be used.
     */
    protected int[] fHashMultipliers;

    //
    // Constructors
    //

    /** Constructs a key table with the default size. */
    public SymbolHash() {
        this(TABLE_SIZE);
    }

    /**
     * Constructs a key table with a given size.
     *
     * @param size  the size of the key table.
     */
    public SymbolHash(int size) {
        fTableSize = size;
        fBuckets = new Entry[fTableSize];
    }

    //
    // Public methods
    //

    /**
     * Adds the key/value mapping to the key table. If the key already exists,
     * the previous value associated with this key is overwritten by the new
     * value.
     *
     * @param key
     * @param value
     */
    public void put(Object key, Object value) {

        // search for identical key
        int collisionCount = 0;
        final int hash = hash(key);
        int bucket = hash % fTableSize;
        for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (key.equals(entry.key)) {
                // replace old value
                entry.value = value;
                return;
            }
            ++collisionCount;
        }

        if (fNum >= fTableSize) {
            // Rehash the table if the number of entries
            // would exceed the number of buckets.
            rehash();
            bucket = hash % fTableSize;
        }
        else if (collisionCount >= MAX_HASH_COLLISIONS && key instanceof String) {
            // Select a new hash function and rehash the table if
            // MAX_HASH_COLLISIONS is exceeded.
            rebalance();
            bucket = hash(key) % fTableSize;
        }

        // create new entry
        Entry entry = new Entry(key, value, fBuckets[bucket]);
        fBuckets[bucket] = entry;
        ++fNum;
    }

    /**
     * Get the value associated with the given key.
     *
     * @param key
     * @return the value associated with the given key.
     */
    public Object get(Object key) {
        int bucket = hash(key) % fTableSize;
        Entry entry = search(key, bucket);
        if (entry != null) {
            return entry.value;
        }
        return null;
    }

    /**
     * Get the number of key/value pairs stored in this table.
     *
     * @return the number of key/value pairs stored in this table.
     */
    public int getLength() {
        return fNum;
    }

    /**
     * Add all values to the given array. The array must have enough entry.
     *
     * @param elements  the array to store the elements
     * @param from      where to start store element in the array
     * @return          number of elements copied to the array
     */
    public int getValues(Object[] elements, int from) {
        for (int i=0, j=0; i<fTableSize && j<fNum; i++) {
            for (Entry entry = fBuckets[i]; entry != null; entry = entry.next) {
                elements[from+j] = entry.value;
                j++;
            }
        }
        return fNum;
    }

    /**
     * Return key/value pairs of all entries in the map
     */
    public Object[] getEntries() {
        Object[] entries = new Object[fNum << 1];
        for (int i=0, j=0; i<fTableSize && j<fNum << 1; i++) {
            for (Entry entry = fBuckets[i]; entry != null; entry = entry.next) {
                entries[j] = entry.key;
                entries[++j] = entry.value;
                j++;
            }
        }
        return entries;
    }

    /**
     * Make a clone of this object.
     */
    public SymbolHash makeClone() {
        SymbolHash newTable = new SymbolHash(fTableSize);
        newTable.fNum = fNum;
        newTable.fHashMultipliers = fHashMultipliers != null ? fHashMultipliers.clone() : null;
        for (int i = 0; i < fTableSize; i++) {
            if (fBuckets[i] != null) {
                newTable.fBuckets[i] = fBuckets[i].makeClone();
            }
        }
        return newTable;
    }

    /**
     * Remove all key/value association. This tries to save a bit of GC'ing
     * by at least keeping the fBuckets array around.
     */
    public void clear() {
        for (int i=0; i<fTableSize; i++) {
            fBuckets[i] = null;
        }
        fNum = 0;
        fHashMultipliers = null;
    } // clear():  void

    protected Entry search(Object key, int bucket) {
        // search for identical key
        for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (key.equals(entry.key))
                return entry;
        }
        return null;
    }

    /**
     * Returns a hashcode value for the specified key.
     *
     * @param key The key to hash.
     */
    protected int hash(Object key) {
        if (fHashMultipliers == null || !(key instanceof String)) {
            return key.hashCode() & 0x7FFFFFFF;
        }
        return hash0((String) key);
    } // hash(Object):int

    private int hash0(String symbol) {
        int code = 0;
        final int length = symbol.length();
        final int[] multipliers = fHashMultipliers;
        for (int i = 0; i < length; ++i) {
            code = code * multipliers[i & MULTIPLIERS_MASK] + symbol.charAt(i);
        }
        return code & 0x7FFFFFFF;
    } // hash0(String):int

    /**
     * Increases the capacity of and internally reorganizes this
     * SymbolHash, in order to accommodate and access its entries more
     * efficiently.  This method is called automatically when the
     * number of keys in the SymbolHash exceeds its number of buckets.
     */
    protected void rehash() {
        rehashCommon((fBuckets.length << 1) + 1);
    }

    /**
     * Randomly selects a new hash function and reorganizes this SymbolHash
     * in order to more evenly distribute its entries across the table. This
     * method is called automatically when the number keys in one of the
     * SymbolHash's buckets exceeds MAX_HASH_COLLISIONS.
     */
    protected void rebalance() {
        if (fHashMultipliers == null) {
            fHashMultipliers = new int[MULTIPLIERS_SIZE];
        }
        PrimeNumberSequenceGenerator.generateSequence(fHashMultipliers);
        rehashCommon(fBuckets.length);
    }

    private void rehashCommon(final int newCapacity) {

        final int oldCapacity = fBuckets.length;
        final Entry[] oldTable = fBuckets;

        final Entry[] newTable = new Entry[newCapacity];

        fBuckets = newTable;
        fTableSize = fBuckets.length;

        for (int i = oldCapacity; i-- > 0;) {
            for (Entry old = oldTable[i]; old != null; ) {
                Entry e = old;
                old = old.next;

                int index = hash(e.key) % newCapacity;
                e.next = newTable[index];
                newTable[index] = e;
            }
        }
    }

    //
    // Classes
    //

    /**
     * This class is a key table entry. Each entry acts as a node
     * in a linked list.
     */
    protected static final class Entry {
        // key/value
        public Object key;
        public Object value;
        /** The next entry. */
        public Entry next;

        public Entry() {
            key = null;
            value = null;
            next = null;
        }

        public Entry(Object key, Object value, Entry next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public Entry makeClone() {
            Entry entry = new Entry();
            entry.key = key;
            entry.value = value;
            if (next != null)
                entry.next = next.makeClone();
            return entry;
        }
    } // entry

} // class SymbolHash
