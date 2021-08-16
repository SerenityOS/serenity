/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * This class is a symbol table implementation that guarantees that
 * strings used as identifiers are unique references. Multiple calls
 * to <code>addSymbol</code> will always return the same string
 * reference.
 * <p>
 * The symbol table performs the same task as <code>String.intern()</code>
 * with the following differences:
 * <ul>
 *  <li>
 *   A new string object does not need to be created in order to
 *   retrieve a unique reference. Symbols can be added by using
 *   a series of characters in a character array.
 *  </li>
 *  <li>
 *   Users of the symbol table can provide their own symbol hashing
 *   implementation. For example, a simple string hashing algorithm
 *   may fail to produce a balanced set of hashcodes for symbols
 *   that are <em>mostly</em> unique. Strings with similar leading
 *   characters are especially prone to this poor hashing behavior.
 *  </li>
 * </ul>
 *
 * @see SymbolHash
 *
 * @author Andy Clark
 *
 */
public class SymbolTable {

    //
    // Constants
    //

    /** Default table size. */
    protected static final int TABLE_SIZE = 101;

    /** Maximum hash collisions per bucket for a table with load factor == 1. */
    protected static final int MAX_HASH_COLLISIONS = 40;

    protected static final int MULTIPLIERS_SIZE = 1 << 5;
    protected static final int MULTIPLIERS_MASK = MULTIPLIERS_SIZE - 1;

    //
    // Data
    //

    /** Buckets. */
    protected Entry[] fBuckets = null;

    /** actual table size **/
    protected int fTableSize;

    /** The total number of entries in the hash table. */
    protected transient int fCount;

    /** The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).) */
    protected int fThreshold;

    /** The load factor for the SymbolTable. */
    protected float fLoadFactor;

    /**
     * A new hash function is selected and the table is rehashed when
     * the number of keys in the bucket exceeds this threshold.
     */
    protected final int fCollisionThreshold;

    /**
     * Array of randomly selected hash function multipliers or <code>null</code>
     * if the default String.hashCode() function should be used.
     */
    protected int[] fHashMultipliers;

    //
    // Constructors
    //

    /**
     * Constructs a new, empty SymbolTable with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the SymbolTable.
     * @param      loadFactor        the load factor of the SymbolTable.
     * @throws     IllegalArgumentException  if the initial capacity is less
     *             than zero, or if the load factor is nonpositive.
     */
    public SymbolTable(int initialCapacity, float loadFactor) {

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor);
        }

        if (initialCapacity == 0) {
            initialCapacity = 1;
        }

        fLoadFactor = loadFactor;
        fTableSize = initialCapacity;
        fBuckets = new Entry[fTableSize];
        fThreshold = (int)(fTableSize * loadFactor);
        fCollisionThreshold = (int)(MAX_HASH_COLLISIONS * loadFactor);
        fCount = 0;
    }

    /**
     * Constructs a new, empty SymbolTable with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param     initialCapacity   the initial capacity of the hashtable.
     * @throws    IllegalArgumentException if the initial capacity is less
     *            than zero.
     */
    public SymbolTable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty SymbolTable with a default initial capacity (101)
     * and load factor, which is <tt>0.75</tt>.
     */
    public SymbolTable() {
        this(TABLE_SIZE, 0.75f);
    }

    //
    // Public methods
    //

    /**
     * Adds the specified symbol to the symbol table and returns a
     * reference to the unique symbol. If the symbol already exists,
     * the previous symbol reference is returned instead, in order
     * guarantee that symbol references remain unique.
     *
     * @param symbol The new symbol.
     */
    public String addSymbol(String symbol) {

        // search for identical symbol
        int collisionCount = 0;
        int bucket = hash(symbol) % fTableSize;
        for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (entry.symbol.equals(symbol)) {
                return entry.symbol;
            }
            ++collisionCount;
        }
        return addSymbol0(symbol, bucket, collisionCount);

    } // addSymbol(String):String

    private String addSymbol0(String symbol, int bucket, int collisionCount) {

        if (fCount >= fThreshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            bucket = hash(symbol) % fTableSize;
        }
        else if (collisionCount >= fCollisionThreshold) {
            // Select a new hash function and rehash the table if
            // the collision threshold is exceeded.
            rebalance();
            bucket = hash(symbol) % fTableSize;
        }

        // create new entry
        Entry entry = new Entry(symbol, fBuckets[bucket]);
        fBuckets[bucket] = entry;
        ++fCount;
        return entry.symbol;

    } // addSymbol0(String,int,int):String

    /**
     * Adds the specified symbol to the symbol table and returns a
     * reference to the unique symbol. If the symbol already exists,
     * the previous symbol reference is returned instead, in order
     * guarantee that symbol references remain unique.
     *
     * @param buffer The buffer containing the new symbol.
     * @param offset The offset into the buffer of the new symbol.
     * @param length The length of the new symbol in the buffer.
     */
    public String addSymbol(char[] buffer, int offset, int length) {

        // search for identical symbol
        int collisionCount = 0;
        int bucket = hash(buffer, offset, length) % fTableSize;
        OUTER: for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != entry.characters[i]) {
                        ++collisionCount;
                        continue OUTER;
                    }
                }
                return entry.symbol;
            }
            ++collisionCount;
        }
        return addSymbol0(buffer, offset, length, bucket, collisionCount);

    } // addSymbol(char[],int,int):String

    private String addSymbol0(char[] buffer, int offset, int length, int bucket, int collisionCount) {

        if (fCount >= fThreshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            bucket = hash(buffer, offset, length) % fTableSize;
        }
        else if (collisionCount >= fCollisionThreshold) {
            // Select a new hash function and rehash the table if
            // the collision threshold is exceeded.
            rebalance();
            bucket = hash(buffer, offset, length) % fTableSize;
        }

        // add new entry
        Entry entry = new Entry(buffer, offset, length, fBuckets[bucket]);
        fBuckets[bucket] = entry;
        ++fCount;
        return entry.symbol;

    } // addSymbol0(char[],int,int,int,int):String

    /**
     * Returns a hashcode value for the specified symbol. The value
     * returned by this method must be identical to the value returned
     * by the <code>hash(char[],int,int)</code> method when called
     * with the character array that comprises the symbol string.
     *
     * @param symbol The symbol to hash.
     */
    public int hash(String symbol) {
        if (fHashMultipliers == null) {
            return symbol.hashCode() & 0x7FFFFFFF;
        }
        return hash0(symbol);
    } // hash(String):int

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
     * Returns a hashcode value for the specified symbol information.
     * The value returned by this method must be identical to the value
     * returned by the <code>hash(String)</code> method when called
     * with the string object created from the symbol information.
     *
     * @param buffer The character buffer containing the symbol.
     * @param offset The offset into the character buffer of the start
     *               of the symbol.
     * @param length The length of the symbol.
     */
    public int hash(char[] buffer, int offset, int length) {
        if (fHashMultipliers == null) {
            int code = 0;
            for (int i = 0; i < length; ++i) {
                code = code * 31 + buffer[offset + i];
            }
            return code & 0x7FFFFFFF;
        }
        return hash0(buffer, offset, length);

    } // hash(char[],int,int):int

    private int hash0(char[] buffer, int offset, int length) {
        int code = 0;
        final int[] multipliers = fHashMultipliers;
        for (int i = 0; i < length; ++i) {
            code = code * multipliers[i & MULTIPLIERS_MASK] + buffer[offset + i];
        }
        return code & 0x7FFFFFFF;
    } // hash0(char[],int,int):int

    /**
     * Increases the capacity of and internally reorganizes this
     * SymbolTable, in order to accommodate and access its entries more
     * efficiently.  This method is called automatically when the
     * number of keys in the SymbolTable exceeds this hashtable's capacity
     * and load factor.
     */
    protected void rehash() {
        rehashCommon(fBuckets.length * 2 + 1);
    }

    /**
     * Randomly selects a new hash function and reorganizes this SymbolTable
     * in order to more evenly distribute its entries across the table. This
     * method is called automatically when the number keys in one of the
     * SymbolTable's buckets exceeds the given collision threshold.
     */
    protected void rebalance() {
        if (fHashMultipliers == null) {
            fHashMultipliers = new int[MULTIPLIERS_SIZE];
        }
        PrimeNumberSequenceGenerator.generateSequence(fHashMultipliers);
        rehashCommon(fBuckets.length);
    }

    private void rehashCommon(final int newCapacity) {

        int oldCapacity = fBuckets.length;
        Entry[] oldTable = fBuckets;

        Entry[] newTable = new Entry[newCapacity];

        fThreshold = (int)(newCapacity * fLoadFactor);
        fBuckets = newTable;
        fTableSize = fBuckets.length;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry old = oldTable[i] ; old != null ; ) {
                Entry e = old;
                old = old.next;

                int index = hash(e.symbol) % newCapacity;
                e.next = newTable[index];
                newTable[index] = e;
            }
        }
    }

    /**
     * Returns true if the symbol table already contains the specified
     * symbol.
     *
     * @param symbol The symbol to look for.
     */
    public boolean containsSymbol(String symbol) {

        // search for identical symbol
        int bucket = hash(symbol) % fTableSize;
        int length = symbol.length();
        OUTER: for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (symbol.charAt(i) != entry.characters[i]) {
                        continue OUTER;
                    }
                }
                return true;
            }
        }

        return false;

    } // containsSymbol(String):boolean

    /**
     * Returns true if the symbol table already contains the specified
     * symbol.
     *
     * @param buffer The buffer containing the symbol to look for.
     * @param offset The offset into the buffer.
     * @param length The length of the symbol in the buffer.
     */
    public boolean containsSymbol(char[] buffer, int offset, int length) {

        // search for identical symbol
        int bucket = hash(buffer, offset, length) % fTableSize;
        OUTER: for (Entry entry = fBuckets[bucket]; entry != null; entry = entry.next) {
            if (length == entry.characters.length) {
                for (int i = 0; i < length; i++) {
                    if (buffer[offset + i] != entry.characters[i]) {
                        continue OUTER;
                    }
                }
                return true;
            }
        }

        return false;

    } // containsSymbol(char[],int,int):boolean

    //
    // Classes
    //

    /**
     * This class is a symbol table entry. Each entry acts as a node
     * in a linked list.
     */
    protected static final class Entry {

        //
        // Data
        //

        /** Symbol. */
        public final String symbol;

        /**
         * Symbol characters. This information is duplicated here for
         * comparison performance.
         */
        public final char[] characters;

        /** The next entry. */
        public Entry next;

        //
        // Constructors
        //

        /**
         * Constructs a new entry from the specified symbol and next entry
         * reference.
         */
        public Entry(String symbol, Entry next) {
            this.symbol = symbol.intern();
            characters = new char[symbol.length()];
            symbol.getChars(0, characters.length, characters, 0);
            this.next = next;
        }

        /**
         * Constructs a new entry from the specified symbol information and
         * next entry reference.
         */
        public Entry(char[] ch, int offset, int length, Entry next) {
            characters = new char[length];
            System.arraycopy(ch, offset, characters, 0, length);
            symbol = new String(characters).intern();
            this.next = next;
        }

    } // class Entry

} // class SymbolTable
