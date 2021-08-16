/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Brush.h"

GDIHashtable AwtBrush::cache("Brush cache", DeleteAwtBrush);

AwtBrush::AwtBrush(COLORREF color) {
    if (!EnsureGDIObjectAvailability()) {
        // If we've run out of GDI objects, don't try to create
        // a new one
        return;
    }
    SetColor(color);
    HBRUSH brush = ::CreateSolidBrush(color);
    /*
     * Fix for BugTraq ID 4191297.
     * If GDI resource creation failed flush all GDIHashtables
     * to destroy unreferenced GDI resources.
     */
    if (brush == NULL) {
        cache.flushAll();
        brush = ::CreateSolidBrush(color);
    }
    DASSERT(brush != NULL);
    SetHandle(brush);
    if (brush == NULL) {
        // We've already incremented the counter: decrement if
        // creation failed
        Decrement();
    }
}

AwtBrush* AwtBrush::Get(COLORREF color) {

    CriticalSection::Lock l(cache.getManagerLock());

    AwtBrush* obj = static_cast<AwtBrush*>(cache.get(
        reinterpret_cast<void*>(static_cast<INT_PTR>(color))));
    if (obj == NULL) {
        obj = new AwtBrush(color);
        VERIFY(cache.put(reinterpret_cast<void*>(
            static_cast<INT_PTR>(color)), obj) == NULL);
    }
    obj->IncrRefCount();
    return obj;
}

void AwtBrush::ReleaseInCache() {

    CriticalSection::Lock l(cache.getManagerLock());

    if (DecrRefCount() == 0) {
        cache.release(reinterpret_cast<void*>(
            static_cast<INT_PTR>(GetColor())));
    }
}

void AwtBrush::DeleteAwtBrush(void* pBrush) {
    delete (AwtBrush*)pBrush;
}
