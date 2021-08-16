/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_CHOICE_H
#define AWT_CHOICE_H

#include "awt_Component.h"

#include "java_awt_Choice.h"
#include "sun_awt_windows_WChoicePeer.h"


/************************************************************************
 * Component class for system provided buttons
 */

class AwtChoice : public AwtComponent {
public:
    AwtChoice();

    virtual LPCTSTR GetClassName();
    static AwtChoice* Create(jobject peer, jobject hParent);

    virtual void Dispose();

    virtual void Reshape(int x, int y, int w, int h);
    void ResetDropDownHeight();
    int GetDropDownHeight();

#ifdef DEBUG
    void VerifyState(); /* verify component and peer are in sync. */
#endif

    /*for multifont list */
    jobject PreferredItemSize(JNIEnv *env);

    /*
     * Windows message handler functions
     */
    MsgRouting WmNotify(UINT notifyCode);

    /* for multifont choice */
    MsgRouting OwnerDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo);
    MsgRouting OwnerMeasureItem(UINT ctrlId, MEASUREITEMSTRUCT& measureInfo);

    /* Workaround for bug #4338368 */
    MsgRouting WmKillFocus(HWND hWndGotFocus);
    MsgRouting WmMouseUp(UINT flags, int x, int y, int button);

    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    INLINE HWND GetDBCSEditHandle() { return GetHWnd(); }
    virtual void SetFont(AwtFont *pFont);
    virtual BOOL InheritsNativeMouseWheelBehavior();
    virtual void SetDragCapture(UINT flags);
    virtual void ReleaseDragCapture(UINT flags);

    static BOOL mouseCapture;
    static BOOL skipNextMouseUp;

    // called on Toolkit thread from JNI
    static void _Reshape(void *param);
    static void _Select(void *param);
    static void _AddItems(void *param);
    static void _Remove(void *param);
    static void _RemoveAll(void *param);
    static void _CloseList(void *param);

private:
    int GetFieldHeight();
    int GetTotalHeight();
    static BOOL sm_isMouseMoveInList;
    HWND m_hList;
    WNDPROC m_listDefWindowProc;
    static LRESULT CALLBACK ListWindowProc(HWND hwnd, UINT message,
                                           WPARAM wParam, LPARAM lParam);
};

#endif /* AWT_CHOICE_H */
