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

#ifndef AWT_TEXTCOMPONENT_H
#define AWT_TEXTCOMPONENT_H

#include "awt_Component.h"

#include "sun_awt_windows_WTextComponentPeer.h"

#include <ole2.h>
#include <richedit.h>
#include <richole.h>


/************************************************************************
 * AwtTextComponent class
 */

class AwtTextComponent : public AwtComponent {
public:
    static jmethodID canAccessClipboardMID;

    AwtTextComponent();

    static AwtTextComponent* Create(jobject self, jobject parent, BOOL isMultiline);

    virtual void Dispose();

    virtual LPCTSTR GetClassName();
    LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);

    int RemoveCR(WCHAR *pStr);

    virtual LONG getJavaSelPos(LONG orgPos);
    virtual LONG getWin32SelPos(LONG orgPos);

    void CheckLineSeparator(WCHAR *pStr);

    virtual void SetSelRange(LONG start, LONG end);

    INLINE void SetText(LPCTSTR text) {
        ::SetWindowText(GetHWnd(), text);
    }

    INLINE virtual int GetText(LPTSTR buffer, int size) {
        return ::GetWindowText(GetHWnd(), buffer, size);
    }

    // called on Toolkit thread from JNI
    static jstring _GetText(void *param);

    void SetFont(AwtFont* font);

    virtual void Enable(BOOL bEnable);
    virtual void SetColor(COLORREF c);
    virtual void SetBackgroundColor(COLORREF c);

    /*
     * Windows message handler functions
     */
    MsgRouting WmNotify(UINT notifyCode);
    MsgRouting HandleEvent(MSG *msg, BOOL synthetic);
    MsgRouting WmPaste();

    INLINE void SetIgnoreEnChange(BOOL b) { m_bIgnoreEnChange = b; }

    virtual BOOL IsFocusingMouseMessage(MSG *pMsg);

/*  To be fully implemented in a future release

    MsgRouting WmKeyDown(UINT wkey, UINT repCnt,
                         UINT flags, BOOL system);  // accessibility support
*/


    //im --- for over the spot composition
    void SetCompositionWindow(RECT& rect);

    INLINE HWND GetDBCSEditHandle() { return GetHWnd(); }

    BOOL m_isLFonly;
    BOOL m_EOLchecked;

    // some methods invoked on Toolkit thread
    static void _SetText(void *param);
    static jint _GetSelectionStart(void *param);
    static jint _GetSelectionEnd(void *param);
    static void _Select(void *param);
    static void _EnableEditing(void *param);

  protected:
    INLINE LONG GetStartSelectionPos() { return m_lStartPos; }
    INLINE LONG GetEndSelectionPos() { return m_lEndPos; }
    INLINE LONG GetLastSelectionPos() { return m_lLastPos; }
    INLINE VOID SetStartSelectionPos(LONG lPos) { m_lStartPos = lPos; }
    INLINE VOID SetEndSelectionPos(LONG lPos) { m_lEndPos = lPos; }
    INLINE VOID SetLastSelectionPos(LONG lPos) { m_lLastPos = lPos; }

    void EditGetSel(CHARRANGE &cr);

    // Used to prevent untrusted code from synthesizing a WM_PASTE message
    // by posting a <CTRL>-V KeyEvent
    BOOL    m_synthetic;
    LONG EditGetCharFromPos(POINT& pt);

    // RichEdit 1.0 control generates EN_CHANGE notifications not only
    // on text changes, but also on any character formatting change.
    // This flag is true when the latter case is detected.
    BOOL    m_bIgnoreEnChange;

    // RichEdit 1.0 control undoes a character formatting change
    // if it is the latest. We don't create our own undo buffer,
    // but just prohibit undo in case if the latest operation
    // is a formatting change.
    BOOL    m_bCanUndo;

    /*****************************************************************
     * Inner class OleCallback declaration.
     */
    class OleCallback : public IRichEditOleCallback {
    public:
        OleCallback();

        STDMETHODIMP QueryInterface(REFIID riid, LPVOID * ppvObj);
        STDMETHODIMP_(ULONG) AddRef();
        STDMETHODIMP_(ULONG) Release();
        STDMETHODIMP GetNewStorage(LPSTORAGE FAR * ppstg);
        STDMETHODIMP GetInPlaceContext(LPOLEINPLACEFRAME FAR * ppipframe,
                                       LPOLEINPLACEUIWINDOW FAR* ppipuiDoc,
                                       LPOLEINPLACEFRAMEINFO pipfinfo);
        STDMETHODIMP ShowContainerUI(BOOL fShow);
        STDMETHODIMP QueryInsertObject(LPCLSID pclsid, LPSTORAGE pstg, LONG cp);
        STDMETHODIMP DeleteObject(LPOLEOBJECT poleobj);
        STDMETHODIMP QueryAcceptData(LPDATAOBJECT pdataobj, CLIPFORMAT *pcfFormat,
                                     DWORD reco, BOOL fReally, HGLOBAL hMetaPict);
        STDMETHODIMP ContextSensitiveHelp(BOOL fEnterMode);
        STDMETHODIMP GetClipboardData(CHARRANGE *pchrg, DWORD reco,
                                      LPDATAOBJECT *ppdataobj);
        STDMETHODIMP GetDragDropEffect(BOOL fDrag, DWORD grfKeyState,
                                       LPDWORD pdwEffect);
        STDMETHODIMP GetContextMenu(WORD seltype, LPOLEOBJECT poleobj,
                                    CHARRANGE FAR * pchrg, HMENU FAR * phmenu);
    private:
        ULONG             m_refs; // Reference count
    };//OleCallback class

    INLINE static OleCallback& GetOleCallback() { return sm_oleCallback; }


private:

    // Fields to track the selection state while the left mouse button is
    // pressed. They are used to simulate autoscrolling.
    LONG    m_lStartPos;
    LONG    m_lEndPos;
    LONG    m_lLastPos;

    HFONT m_hFont;
    //im --- end

    static OleCallback sm_oleCallback;

    static WNDPROC sm_pDefWindowProc;
    HWND    m_hEditCtrl;

    static LRESULT CALLBACK EditProc(HWND hWnd, UINT message,
                                     WPARAM wParam, LPARAM lParam);
    MsgRouting WmContextMenu(HWND hCtrl, UINT xPos, UINT yPos);

    //
    // Accessibility support
    //
//public:
//    jlong javaEventsMask;
};

#endif /* AWT_TEXTCOMPONENT_H */
