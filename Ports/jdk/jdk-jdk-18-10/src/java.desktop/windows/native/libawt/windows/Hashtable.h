/*
 * Copyright (c) 1996, 2002, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HASHTABLE_H
#define HASHTABLE_H

#include "awt.h"
#include "awt_Toolkit.h"

struct HashtableEntry {
    INT_PTR hash;
    void* key;
    void* value;
    HashtableEntry* next;
};

class HashtableEnumerator {
private:
    BOOL keys;
    int index;
    HashtableEntry** table;
    HashtableEntry* entry;

public:
    HashtableEnumerator(HashtableEntry* table[], int size, BOOL keys);
    BOOL hasMoreElements();
    void* nextElement();
};

/**
 * Hashtable class. Maps keys to values. Any object can be used as
 * a key and/or value.  As you might guess, this was brazenly stolen
 * from java.util.Hashtable.
 */
class Hashtable {
protected:
    /*
     * The hash table data.
     */
    HashtableEntry** table;

    /*
     * The size of table
     */
    int capacity;

    /*
     * The total number of entries in the hash table.
     */
    int count;

    /**
     * Rehashes the table when count exceeds this threshold.
     */
    int threshold;

    /**
     * The load factor for the hashtable.
     */
    float loadFactor;

    /**
     * Our C++ synchronizer.
     */
    CriticalSection lock;

    /**
     * Element deletion routine, if any.
     */
    void (*m_deleteProc)(void*);

#ifdef DEBUG
    char* m_name;
    int m_max;
    int m_collisions;
#endif

public:
    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity and the specified load factor.
     */
    Hashtable(const char* name, void (*deleteProc)(void*) = NULL,
              int initialCapacity = 29, float loadFactor = 0.75);

    virtual ~Hashtable();

    /**
     * Returns the number of elements contained in the hashtable.
     */
    INLINE int size() {
        return count;
    }

    /**
     * Returns true if the hashtable contains no elements.
     */
    INLINE BOOL isEmpty() {
        return count == 0;
    }

    /**
     * Returns an enumeration of the hashtable's keys.
     */
    INLINE HashtableEnumerator* keys() {
        CriticalSection::Lock l(lock);
        return new HashtableEnumerator(table, capacity, TRUE);
    }

    /**
     * Returns an enumeration of the elements. Use the Enumeration methods
     * on the returned object to fetch the elements sequentially.
     */
    INLINE HashtableEnumerator* elements() {
        CriticalSection::Lock l(lock);
        return new HashtableEnumerator(table, capacity, FALSE);
    }

    /**
     * Returns true if the specified object is an element of the hashtable.
     * This operation is more expensive than the containsKey() method.
     */
    BOOL contains(void* value);

    /**
     * Returns true if the collection contains an element for the key.
     */
    BOOL containsKey(void* key);

    /**
     * Gets the object associated with the specified key in the
     * hashtable.
     */
    void* get(void* key);

    /**
     * Puts the specified element into the hashtable, using the specified
     * key.  The element may be retrieved by doing a get() with the same key.
     * The key and the element cannot be null.
     */
    virtual void* put(void* key, void* value);

    /**
     * Removes the element corresponding to the key. Does nothing if the
     * key is not present.
     */
    void* remove(void* key);

    /**
     * Clears the hash table so that it has no more elements in it.
     */
    void clear();

protected:
    /**
     * Rehashes the content of the table into a bigger table.
     * This method is called automatically when the hashtable's
     * size exceeds the threshold.
     */
    void rehash();
};

#endif // HASHTABLE_H
