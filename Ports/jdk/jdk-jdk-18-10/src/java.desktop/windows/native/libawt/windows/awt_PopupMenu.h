/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_POPUPMENU_H
#define AWT_POPUPMENU_H

#include "awt_Menu.h"

#include <java_awt_MenuItem.h>
#include <sun_awt_windows_WMenuItemPeer.h>
#include <sun_awt_windows_WPopupMenuPeer.h>


/************************************************************************
 * AwtPopupMenu class
 */

class AwtPopupMenu : public AwtMenu {
public:
    AwtPopupMenu();
    virtual ~AwtPopupMenu();

    virtual void Dispose();

    virtual LPCTSTR GetClassName();

    /* Create a new AwtPopupMenu.  This must be run on the main thread. */
    static AwtPopupMenu* Create(jobject self, jobject parent);

    /* Display the popup modally. */
    void Show(JNIEnv *env, jobject event, BOOL isTrayIconPopup);

    static void _Show(void *param);

    virtual AwtMenuBar* GetMenuBar() { return NULL; }
    INLINE void SetParent(AwtComponent* parent) { m_parent = parent; }
    virtual HWND GetOwnerHWnd() {
        return (m_parent == NULL) ? NULL : m_parent->GetHWnd();
    }
    virtual void Enable(BOOL isEnabled);
    virtual BOOL IsDisabledAndPopup();
    virtual void AddItem(AwtMenuItem *item);

private:
    AwtComponent* m_parent;
};

#endif /* AWT_POPUPMENU_H */
