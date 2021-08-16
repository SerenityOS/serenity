/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include "ObjectList.h"
#include "awtmsg.h"

///////////////////////////////////////////////////////////////////////////
// AwtObject list -- track all created widgets for cleanup and debugging

AwtObjectList theAwtObjectList;

AwtObjectList::AwtObjectList()
{
    m_head = NULL;
}

void AwtObjectList::Add(AwtObject* obj)
{
    CriticalSection::Lock l(m_lock);

    /* Verify that the object is not already in the list. */
    DASSERT(LookUp(obj) == NULL);

    AwtObjectListItem* item = new AwtObjectListItem(obj);
    item->next = m_head;
    m_head = item;
}

BOOL AwtObjectList::Remove(AwtObject* obj)
{
    CriticalSection::Lock l(m_lock);

    AwtObjectListItem* item = m_head;
    AwtObjectListItem* lastItem = NULL;

    while (item != NULL) {
        if (item->obj == obj) {
            if (lastItem == NULL) {
                m_head = item->next;
            } else {
                lastItem->next = item->next;
            }
            DASSERT(item != NULL);
            delete item;
            return TRUE;
        }
        lastItem = item;
        item = item->next;
    }

    return FALSE;

//    DASSERT(FALSE);  // should never get here...
                      // even if it does it shouldn't be fatal.
}

#ifdef DEBUG
AwtObject* AwtObjectList::LookUp(AwtObject* obj)
{
    CriticalSection::Lock l(m_lock);

    AwtObjectListItem* item = m_head;

    while (item != NULL) {
        if (item->obj == obj) {
            return obj;
        }
        item = item->next;
    }
    return NULL;
}
#endif /* DEBUG */

void AwtObjectList::Cleanup()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    CHECK_IS_TOOLKIT_THREAD()

    CriticalSection::Lock l(theAwtObjectList.m_lock);

    CriticalSection &syncCS = AwtToolkit::GetInstance().GetSyncCS();
    BOOL entered = syncCS.TryEnter();
    if (entered) {
        AwtObjectListItem* item = theAwtObjectList.m_head;
        while (item != NULL) {
            // AwtObject::Dispose() method will call AwtObjectList::Remove(),
            // which will delete the item structure.
            AwtObjectListItem* next = item->next;
            // destructor for item->obj will be called from item->obj->Dispose() method
            item->obj->Dispose();
            item = next;
        }
        theAwtObjectList.m_head = NULL;
        syncCS.Leave();
    } else {
        AwtToolkit::GetInstance().PostMessage(WM_AWT_OBJECTLISTCLEANUP, NULL, NULL);
    }
}
