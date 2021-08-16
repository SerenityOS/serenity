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

#ifndef AWT_LIST_H
#define AWT_LIST_H

#include "awt_Component.h"

#include "sun_awt_windows_WListPeer.h"


/************************************************************************
 * AwtList class
 */

class AwtList : public AwtComponent {
public:
    AwtList();
    virtual ~AwtList();

    virtual LPCTSTR GetClassName();

    static AwtList* Create(jobject peer, jobject parent);

    virtual BOOL NeedDblClick() { return TRUE; }

    INLINE void Select(int pos) {
        if (isMultiSelect) {
            SendListMessage(LB_SETSEL, TRUE, pos);
        }
        else {
            SendListMessage(LB_SETCURSEL, pos);
        }
    }
    INLINE void Deselect(int pos) {
        if (isMultiSelect) {
            SendListMessage(LB_SETCARETINDEX, pos, FALSE);
            SendListMessage(LB_SETSEL, FALSE, pos);
        }
        else {
            SendListMessage(LB_SETCURSEL, (WPARAM)-1);
        }
    }
    INLINE UINT GetCount() {
        LRESULT index = SendListMessage(LB_GETCOUNT);
        DASSERT(index != LB_ERR);
        return static_cast<UINT>(index);
    }

    INLINE void InsertString(WPARAM index, LPTSTR str) {
        VERIFY(SendListMessage(LB_INSERTSTRING, index, (LPARAM)str) != LB_ERR);
    }
    INLINE BOOL IsItemSelected(UINT index) {
        LRESULT ret = SendListMessage(LB_GETSEL, index);
        DASSERT(ret != LB_ERR);
        return (ret > 0);
    }
    INLINE BOOL InvalidateList(CONST RECT* lpRect, BOOL bErase) {
        DASSERT(GetListHandle());
        return InvalidateRect(GetListHandle(), lpRect, bErase);
    }

    // Adjust the horizontal scrollbar as necessary
    void AdjustHorizontalScrollbar();
    void UpdateMaxItemWidth();

    INLINE long GetMaxWidth() {
        return m_nMaxWidth;
    }

    INLINE void CheckMaxWidth(long nWidth) {
        if (nWidth > m_nMaxWidth) {
            m_nMaxWidth = nWidth;
            AdjustHorizontalScrollbar();
        }
    }

    // Netscape : Change the font on the list and redraw the
    // items nicely.
    virtual void SetFont(AwtFont *pFont);

    /* Set whether a list accepts single or multiple selections. */
    void SetMultiSelect(BOOL ms);

    /*for multifont list */
    jobject PreferredItemSize(JNIEnv *envx);

    /*
     * Windows message handler functions
     */
    MsgRouting WmNcHitTest(UINT x, UINT y, LRESULT& retVal);
    MsgRouting WmMouseDown(UINT flags, int x, int y, int button);
    MsgRouting WmMouseUp(UINT flags, int x, int y, int button);
    MsgRouting WmNotify(UINT notifyCode);

    /* for multifont list */
    MsgRouting OwnerDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo);
    MsgRouting OwnerMeasureItem(UINT ctrlId, MEASUREITEMSTRUCT& measureInfo);

    //for horizontal scrollbar
    MsgRouting WmSize(UINT type, int w, int h);

    MsgRouting WmCtlColor(HDC hDC, HWND hCtrl, UINT ctlColor,
                          HBRUSH& retBrush);

    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    MsgRouting WmPrint(HDC hDC, LPARAM flags);

    virtual void SetDragCapture(UINT flags);
    virtual void ReleaseDragCapture(UINT flags);
    void Reshape(int x, int y, int w, int h);

    INLINE LRESULT SendListMessage(UINT msg, WPARAM wParam=0, LPARAM lParam=0)
    {
        DASSERT(GetListHandle() != NULL);
        return ::SendMessage(GetListHandle(), msg, wParam, lParam);
    }
    INLINE virtual LONG GetStyle() {
        DASSERT(GetListHandle());
        return ::GetWindowLong(GetListHandle(), GWL_STYLE);
    }
    INLINE virtual void SetStyle(LONG style) {
        DASSERT(GetListHandle());
        // SetWindowLong() error handling as recommended by Win32 API doc.
        ::SetLastError(0);
        LONG ret = ::SetWindowLong(GetListHandle(), GWL_STYLE, style);
        DASSERT(ret != 0 || ::GetLastError() == 0);
    }
    INLINE virtual LONG GetStyleEx() {
        DASSERT(GetListHandle());
        return ::GetWindowLong(GetListHandle(), GWL_EXSTYLE);
    }
    INLINE virtual void SetStyleEx(LONG style) {
        DASSERT(GetListHandle());
        // SetWindowLong() error handling as recommended by Win32 API doc.
        ::SetLastError(0);
        LONG ret = ::SetWindowLong(GetListHandle(), GWL_EXSTYLE, style);
        DASSERT(ret != 0 || ::GetLastError() == 0);
    }

    INLINE HWND GetDBCSEditHandle() { return GetListHandle(); }

    virtual BOOL InheritsNativeMouseWheelBehavior();

    virtual BOOL IsFocusingMouseMessage(MSG *pMsg);

    // some methods called on Toolkit thread
    static jint _GetMaxWidth(void *param);
    static void _UpdateMaxItemWidth(void *param);
    static void _AddItems(void *param);
    static void _DelItems(void *param);
    static void _Select(void *param);
    static void _Deselect(void *param);
    static void _MakeVisible(void *param);
    static void _SetMultipleSelections(void *param);
    static jboolean _IsSelected(void *param);

protected:
    INLINE HWND GetListHandle() { return GetHWnd(); }

    static BOOL IsListOwnerMessage(UINT message) {
        switch (message) {
        case WM_DRAWITEM:
        case WM_MEASUREITEM:
        case WM_COMMAND:
#if defined(WIN32)
        case WM_CTLCOLORLISTBOX:
#else
        case WM_CTLCOLOR:
#endif
            return TRUE;
        }
        return FALSE;
    }

    static BOOL IsAwtMessage(UINT message) {
        return (message >= WM_APP);
    }

private:
    BOOL isMultiSelect;
    BOOL isWrapperPrint;

    // The width of the longest item in the listbox
    long m_nMaxWidth;
};

#endif /* AWT_LIST_H */
