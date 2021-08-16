/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GDIHASHTABLE_H
#define GDIHASHTABLE_H

#include "Hashtable.h"

/*
 * This class has been created to fix bug #4191297.
 */

/**
 * GDIHashtable class. Subclasses Hashtable to provide
 * capability of batch destruction of freed GDI resources.
 * Assumes that values are only of AwtGDIObject type.
 */
class GDIHashtable : public Hashtable {
    struct ListEntry {
        GDIHashtable* table;
        ListEntry*      next;
    };

    /**
     * GDIHashtable::List class. Designed to store pointers
     * to all existing GDIHashtables. This is required
     * to flush all GDIHashtables at once.
     */
    class List {
    public:
        List() : m_pHead(NULL) {}
        ~List() { clear(); }

        void add(GDIHashtable*);
        void remove(GDIHashtable*);
        void flushAll();

    private:
        void clear();

        ListEntry* m_pHead;

        CriticalSection m_listLock;
    };

    friend class List;

    /**
     * GDIHashtable::BatchDestructionManager class.
     * Tracks the amount of remaining space in the GDI
     * and flushes GDIHashtables when needed.
     */
    class BatchDestructionManager {
    private:
        int               m_nCounter;
        UINT              m_nFirstThreshold;
        UINT              m_nSecondThreshold;
        UINT              m_nDestroyPeriod;
        BOOL              m_bBatchingEnabled;

        List              m_list;

        CriticalSection   m_managerLock;

    public:
        /**
         * Constructs a new BatchDestructionManager with the specified parameters.
         * The care should be taken when non-default values are used, since it
         * affects performance. They always should satisfy the inequality
         * 10 < nSecondThreshold < nFirstThreshold.
         *
         * @param nFirstThreshold if less than <code>nFirstThreshold</code> percents
         *        of space in GDI heaps is free all existing GDIHashtables will be
         *        flushed on the next call of <code>update</code>.
         * @param nSecondThreshold if less than <code>nSecondThreshold</code>
         *        percents of space in GDI heaps is free after the flush
         *        <code>update</code> will return <code>TRUE</code>.
         * @param nDestroyPeriod specifies how often free space in GDI heaps
         *        will be rechecked in low-resource situation.
         *        In detailss: after <code>update</code> prohibit batching by
         *        setting <code>m_bBatchingEnabled</code> to <code>FALSE</code>
         *        it won't recheck free GDI space for the next
         *        <code>nDestroyPeriod<code> calls. So during this time
         *        <code>shouldDestroy</code> will return <code>TRUE</code>.
         *        This is done to reduce performance impact
         *        caused by calls to <code>GetFreeSystemResourses</code>.
         */
        BatchDestructionManager(UINT nFirstThreshold = 50,
                                UINT nSecondThreshold = 15,
                                UINT nDestroyPeriod = 200);

        /**
         * Adds the specified GDIHashtable to the internal list.
         * <code>flushAll</code> flushes all GDIHashtables from this list.
         * @param table pointer to the GDIHashtable to be added.
         */
        INLINE void add(GDIHashtable* table) { m_list.add(table); }

        /**
         * Removes the specified GDIHashtable to the internal list.
         * Does nothing if the specified table doesn't exist.
         * @param table pointer to the GDIHashtable to be removed.
         */
        INLINE void remove(GDIHashtable* table) { m_list.remove(table); }

        /**
         * @return <code>TRUE</code> if unreferenced AwtGDIObjects shouldn't
         *         be destroyed immediatelly. They will be deleted in
         *         a batch when needed.
         *         <code>FALSE</code> if unreferenced AwtGDIObjects should
         *         be destroyed as soon as freed.
         */
        INLINE BOOL isBatchingEnabled() { return m_bBatchingEnabled; }

        /**
         * Flushes all the GDIHashtables from the internal list.
         */
        INLINE void flushAll() { m_list.flushAll(); }

        /**
         * Decrements the internal counter. The initial value
         * is assigned by <code>update</code> according to
         * the BatchDestructionManager parameters. When the
         * counter hits zero the BatchDestructionManager will
         * recheck the amount of free space in GDI heaps.
         * This is done to reduce the performance impact caused
         * by calls to GetFreeSystemResources. Currently this
         * method is called when a new GDI resource is created.
         */
        INLINE void decrementCounter() { m_nCounter--; }

        INLINE CriticalSection& getLock() { return m_managerLock; }
    };

 public:
    /**
     * Constructs a new, empty GDIHashtable with the specified initial
     * capacity and the specified load factor.
     */
    GDIHashtable(const char* name, void (*deleteProc)(void*) = NULL,
                   int initialCapacity = 29, float loadFactor = 0.75) :
        Hashtable(name, deleteProc, initialCapacity, loadFactor) {
        manager.add(this);
    }

    ~GDIHashtable() {
        manager.remove(this);
    }

    /**
     * Puts the specified element into the hashtable, using the specified
     * key.  The element may be retrieved by doing a get() with the same key.
     * The key and the element cannot be null.
     */
    void* put(void* key, void* value);

    /**
     * Depending on the amount of free space in GDI heads destroys
     * as unreferenced the element corresponding to the key or keeps
     * it for destruction in batch.
     * Does nothing if the key is not present.
     */
    void release(void* key);

    /**
     * Removes all unreferenced elements from the hastable.
     */
    void flush();

    /**
     * Flushes all existing GDIHashtable instances.
     */
    INLINE static void flushAll() { manager.flushAll(); }

    INLINE CriticalSection& getManagerLock() { return manager.getLock(); }

 private:

    static BatchDestructionManager manager;

};

#endif // GDIHASHTABLE_H
