/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package sun.jvm.hotspot.debugger;

import java.util.*;

/**
 * This is a copy of java.util.HashMap which uses longs as keys
 * instead of Objects. It turns out that using this in the PageCache
 * implementation speeds up heap traversals by a factor of three.
 *
 * @author  Josh Bloch
 * @author Arthur van Hoff
 */

public class LongHashMap
{
    static class Entry {
        private int    hash;
        private long   key;
        private Object value;
        private Entry  next;

        Entry(int hash, long key, Object value, Entry next) {
            this.hash  = hash;
            this.key   = key;
            this.value = value;
            this.next  = next;
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry.
         */
        long getKey() { return key; }

        /**
         * Returns the value corresponding to this entry.  If the mapping
         * has been removed from the backing map (by the iterator's
         * <tt>remove</tt> operation), the results of this call are undefined.
         *
         * @return the value corresponding to this entry.
         */
        Object getValue() { return value; }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation).  (Writes through to the map.)  The
         * behavior of this call is undefined if the mapping has already been
         * removed from the map (by the iterator's <tt>remove</tt> operation).
         *
         * @param value new value to be stored in this entry.
         * @return old value corresponding to the entry.
         *
         * @throws UnsupportedOperationException if the <tt>put</tt> operation
         *            is not supported by the backing map.
         * @throws ClassCastException if the class of the specified value
         *            prevents it from being stored in the backing map.
         * @throws    IllegalArgumentException if some aspect of this value
         *            prevents it from being stored in the backing map.
         * @throws NullPointerException the backing map does not permit
         *            <tt>null</tt> values, and the specified value is
         *            <tt>null</tt>.
         */
        Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns <tt>true</tt> if the given object is also a map entry and
         * the two entries represent the same mapping.  More formally, two
         * entries <tt>e1</tt> and <tt>e2</tt> represent the same mapping
         * if<pre>
         *     (e1.getKey()==null ?
         *      e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &&
         *     (e1.getValue()==null ?
         *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
         * </pre>
         * This ensures that the <tt>equals</tt> method works properly across
         * different implementations of the <tt>Map.Entry</tt> interface.
         *
         * @param o object to be compared for equality with this map entry.
         * @return <tt>true</tt> if the specified object is equal to this map
         *         entry.
         */
        public boolean equals(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry e = (Entry)o;
            return (key == e.getKey()) && eq(value, e.getValue());
        }

        /**
         * Returns the hash code value for this map entry.  The hash code
         * of a map entry <tt>e</tt> is defined to be: <pre>
         *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
         *     (e.getValue()==null ? 0 : e.getValue().hashCode())
         * </pre>
         * This ensures that <tt>e1.equals(e2)</tt> implies that
         * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
         * <tt>e1</tt> and <tt>e2</tt>, as required by the general
         * contract of <tt>Object.hashCode</tt>.
         *
         * @return the hash code value for this map entry.
         * @see Object#hashCode()
         * @see Object#equals(Object)
         * @see #equals(Object)
         */
        public int hashCode() {
            return hash ^ (value==null ? 0 : value.hashCode());
        }
    }

    /**
     * The hash table data.
     */
    transient Entry table[];

    /**
     * The total number of mappings in the hash table.
     */
    transient int size;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount = 0;

    /**
     * Constructs a new, empty map with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the HashMap.
     * @param      loadFactor        the load factor of the HashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public LongHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        if (initialCapacity==0)
            initialCapacity = 1;
        this.loadFactor = loadFactor;
        table = new Entry[initialCapacity];
        threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the HashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public LongHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public LongHashMap() {
        this(11, 0.75f);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @return the value to which this map maps the specified key.
     * @param key key whose associated value is to be returned.
     */
    public Object get(long key) {
        Entry e = getEntry(key);
        return (e == null ? null : e.value);
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @param key key whose presence in this Map is to be tested.
     */
    public boolean containsKey(long key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in the
     * HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry getEntry(long key) {
        Entry tab[] = table;
        int hash = (int) key;
        int index = (hash & 0x7FFFFFFF) % tab.length;

        for (Entry e = tab[index]; e != null; e = e.next)
            if (e.hash == hash && e.key ==key)
                return e;

        return null;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
        Entry tab[] = table;

        if (value==null) {
            for (int i = tab.length ; i-- > 0 ;)
                for (Entry e = tab[i] ; e != null ; e = e.next)
                    if (e.value==null)
                        return true;
        } else {
            for (int i = tab.length ; i-- > 0 ;)
                for (Entry e = tab[i] ; e != null ; e = e.next)
                    if (value.equals(e.value))
                        return true;
        }

        return false;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the HashMap previously associated
     *         <tt>null</tt> with the specified key.
     */
    public Object put(long key, Object value) {
        Entry tab[] = table;
        int hash = (int) key;
        int index = (hash & 0x7FFFFFFF) % tab.length;

        // Look for entry in hash table
        for (Entry e = tab[index] ; e != null ; e = e.next) {
            if (e.hash == hash && e.key == key) {
                Object oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }

        // It's not there; grow the hash table if necessary...
        modCount++;
        if (size >= threshold) {
            rehash();
            tab = table;
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // ...and add the entry
        size++;
        tab[index] = newEntry(hash, key, value, tab[index]);
        return null;
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key.
     */
    public Object remove(long key) {
        Entry e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }

    /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry removeEntryForKey(long key) {
        Entry tab[] = table;
        int hash = (int) key;
        int index = (hash & 0x7FFFFFFF) % tab.length;

        for (Entry e = tab[index], prev = null; e != null;
             prev = e, e = e.next) {
            if (e.hash == hash && e.key == key) {
                modCount++;
                if (prev != null)
                    prev.next = e.next;
                else
                    tab[index] = e.next;

                size--;
                return e;
            }
        }

        return null;
    }

    /**
     * Removes the specified entry from this HashMap (and increments modCount).
     *
     * @throws ConcurrentModificationException if the entry is not in the Map
     */
    void removeEntry(Entry doomed) {
        Entry[] tab = table;
        int index = (doomed.hash & 0x7FFFFFFF) % tab.length;

        for (Entry e = tab[index], prev = null; e != null;
             prev = e, e = e.next) {
            if (e == doomed) {
                modCount++;
                if (prev == null)
                    tab[index] = e.next;
                else
                    prev.next = e.next;
                size--;
                return;
            }
        }
        throw new ConcurrentModificationException();
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        Entry tab[] = table;
        modCount++;
        for (int index = tab.length; --index >= 0; )
            tab[index] = null;
        size = 0;
    }

    /**
     * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     */
    void rehash() {
        Entry oldTable[] = table;
        int oldCapacity = oldTable.length;
        int newCapacity = oldCapacity * 2 + 1;
        Entry newTable[] = new Entry[newCapacity];

        modCount++;
        threshold = (int)(newCapacity * loadFactor);
        table = newTable;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (Entry old = oldTable[i] ; old != null ; ) {
                Entry e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newTable[index];
                newTable[index] = e;
            }
        }
    }

    static boolean eq(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    Entry newEntry(int hash, long key, Object value, Entry next) {
        return new Entry(hash, key, value, next);
    }

    int capacity() {
        return table.length;
    }

    float loadFactor() {
        return loadFactor;
    }
}
