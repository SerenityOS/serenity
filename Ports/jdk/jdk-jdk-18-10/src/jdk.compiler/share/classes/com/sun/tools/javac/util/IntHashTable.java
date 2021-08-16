/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

/**
 * A hash table that maps Object to int.
 *
 * This is a custom hash table optimised for the Object {@literal ->} int
 * maps. This is done to avoid unnecessary object allocation in the image set.
 *
 * @author Charles Turner
 * @author Per Bothner
 */
public class IntHashTable {
    private static final int DEFAULT_INITIAL_SIZE = 64;
    protected Object[] objs; // the domain set
    protected int[] ints; // the image set
    protected int mask; // used to clip int's into the domain
    protected int num_bindings; // the number of mappings (including DELETED)
    private static final Object DELETED = new Object();

    /**
     * Construct an Object {@literal ->} int hash table.
     *
     * The default size of the hash table is 64 mappings.
     */
    public IntHashTable() {
        objs = new Object[DEFAULT_INITIAL_SIZE];
        ints = new int[DEFAULT_INITIAL_SIZE];
        mask = DEFAULT_INITIAL_SIZE - 1;
    }

    /**
     * Construct an Object {@literal ->} int hash table with a specified amount of mappings.
     * @param capacity The number of default mappings in this hash table.
     */
    public IntHashTable(int capacity) {
        int log2Size = 4;
        while (capacity > (1 << log2Size)) {
            log2Size++;
        }
        capacity = 1 << log2Size;
        objs = new Object[capacity];
        ints = new int[capacity];
        mask = capacity - 1;
    }

    /**
     * Compute the hash code of a given object.
     *
     * @param key The object whose hash code is to be computed.
     * @return zero if the object is null, otherwise the identityHashCode
     */
    protected int hash(Object key) {
        return System.identityHashCode(key);
    }

    /**
     * Find either the index of a key's value, or the index of an available space.
     *
     * @param key The key to whose index you want to find.
     * @return Either the index of the key's value, or an index pointing to
     * unoccupied space.
     */
    protected int lookup(Object key) {
        Object node;
        int hash = hash(key);
        int hash1 = hash ^ (hash >>> 15);
        int hash2 = (hash ^ (hash << 6)) | 1; //ensure coprimeness
        int deleted = -1;
        for (int i = hash1 & mask;; i = (i + hash2) & mask) {
            node = objs[i];
            if (node == key)
                return i;
            if (node == null)
                return deleted >= 0 ? deleted : i;
            if (node == DELETED && deleted < 0)
                deleted = i;
        }
    }

    /**
     * Return the value to which the specified key is mapped.
     *
     * @param key The key to whose value you want to find.
     * @return A non-negative integer if the value is found.
     *         Otherwise, it is -1.
     */
    public int get(Object key) {
        int index = lookup(key);
        Object node = objs[index];
        return node == null || node == DELETED ? -1 : ints[index];
    }

    /**
     * Associates the specified key with the specified value in this map.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or -1 if there was
     * no mapping for key.
     */
    public int put(Object key, int value) {
        int index = lookup(key);
        Object old = objs[index];
        if (old == null || old == DELETED) {
            objs[index] = key;
            ints[index] = value;
            if (old != DELETED)
                num_bindings++;
            if (3 * num_bindings >= 2 * objs.length)
                rehash();
            return -1;
        } else { // update existing mapping
            int oldValue = ints[index];
            ints[index] = value;
            return oldValue;
        }
    }

    /**
     * Remove the mapping(key and value) of the specified key.
     *
     * @param key the key to whose value you want to remove.
     * @return the removed value associated with the specified key,
     *         or -1 if there was no mapping for the specified key.
     */
    public int remove(Object key) {
        int index = lookup(key);
        Object old = objs[index];
        if (old == null || old == DELETED)
            return -1;
        objs[index] = DELETED;
        return ints[index];
    }

    /**
     * Expand the hash table when it exceeds the load factor.
     *
     * Rehash the existing objects.
     */
    protected void rehash() {
        Object[] oldObjsTable = objs;
        int[] oldIntsTable = ints;
        int newCapacity = oldObjsTable.length << 1;
        objs = new Object[newCapacity];
        ints = new int[newCapacity];
        mask = newCapacity - 1;
        num_bindings = 0; // this is recomputed below
        Object key;
        for (int i = oldIntsTable.length; --i >= 0;) {
            key = oldObjsTable[i];
            if (key != null && key != DELETED)
                put(key, oldIntsTable[i]);
        }
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        for (int i = objs.length; --i >= 0;) {
            objs[i] = null;
        }
        num_bindings = 0;
    }
}
