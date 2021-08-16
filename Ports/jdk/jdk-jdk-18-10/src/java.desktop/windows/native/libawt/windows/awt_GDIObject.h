/*
 * Copyright (c) 1996, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_GDIOBJECT_H
#define AWT_GDIOBJECT_H

#include "awt.h"
#include "Hashtable.h"
#include "GDIHashtable.h"

#define MEMORY_OVER_SPEED 1

typedef struct {
    HDC hDC;
    BOOL gdiLimitReached;
} GetDCReturnStruct;

/*
 * An AwtGDIObject is a cached, color-based GDI object, such as a pen or
 * brush.  This class also includes static methods for tracking the
 * total number of active GDI Objects (Pen, Brush, and HDC).
 */
class AwtGDIObject {
public:
    INLINE COLORREF GetColor() { return m_color; }
    INLINE void SetColor(COLORREF color) { m_color = color; }

    INLINE HGDIOBJ GetHandle() { return m_handle; }
    INLINE void SetHandle(HGDIOBJ handle) { m_handle = handle; }

    /*
     * NOTE: we don't syncronize access to the reference counter.
     * Currently it is changed only when we are already synchronized
     * on the global BatchDestructionManager lock.
     */
    INLINE int GetRefCount() { return m_refCount; }
    INLINE int IncrRefCount() { return ++m_refCount; }
    INLINE int DecrRefCount() { return --m_refCount; }

    /*
     * Decrement the reference count of a cached GDI object.  When it hits
     * zero, notify the cache that the object can be safely removed.
     * The cache will eventually delete the GDI object and this wrapper.
     */
    INLINE void Release() {
#if MEMORY_OVER_SPEED
        ReleaseInCache();
#endif
    }

    // Workaround for Windows bug: do not let process have more than
    // a set number of active (unreleased) GDI objects at any given time.
    static BOOL IncrementIfAvailable();
    static void Decrement();
    static BOOL EnsureGDIObjectAvailability();

protected:
    /*
     * Get a GDI object from its respective cache.  If it doesn't exist
     * it gets created, otherwise its reference count gets bumped.
     */
    static AwtGDIObject* Get(COLORREF color);

    virtual void ReleaseInCache() = 0;

    INLINE AwtGDIObject() {
        m_handle = NULL;
        m_refCount = 0;
    }

    virtual ~AwtGDIObject() {
        if (m_handle != NULL) {
            ::DeleteObject(m_handle);
            Decrement();
        }
    }

private:
    static int GetMaxGDILimit();

    COLORREF m_color;
    HGDIOBJ  m_handle;
    int      m_refCount;
    static CriticalSection* objectCounterLock;
    static int numCurrentObjects;
    static int maxGDIObjects;
};

#endif // AWT_GDIOBJECT_H
