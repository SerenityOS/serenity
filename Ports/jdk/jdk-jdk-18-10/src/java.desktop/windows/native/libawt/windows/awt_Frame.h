/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_FRAME_H
#define AWT_FRAME_H

#include "awt_Window.h"
#include "awt_MenuBar.h" //add for multifont
#include "awt_Toolkit.h"
#include "Hashtable.h"

#include "java_awt_Frame.h"
#include "sun_awt_windows_WFramePeer.h"


/************************************************************************
 * AwtFrame class
 */

#define AWT_FRAME_WINDOW_CLASS_NAME TEXT("SunAwtFrame")


class AwtFrame : public AwtWindow {
public:
    enum FrameExecIds {
        FRAME_SETMENUBAR
    };

    /* java.awt.Frame fields and method IDs */
    static jfieldID undecoratedID;

    /* sun.awt.windows.WEmbeddedFrame fields and method IDs */
    static jfieldID handleID;

    static jmethodID getExtendedStateMID;

    /* method id for WEmbeddedFrame.requestActivate() method */
    static jmethodID activateEmbeddingTopLevelMID;

    /* field id for WEmbeddedFrame.isEmbeddedInIE */
    static jfieldID isEmbeddedInIEID;

    AwtFrame();
    virtual ~AwtFrame();

    virtual void Dispose();

    virtual LPCTSTR GetClassName();

    /* Create a new AwtFrame.  This must be run on the main thread. */
    static AwtFrame* Create(jobject self, jobject parent);

    /* Returns whether this frame is embedded in an external native frame. */
    INLINE BOOL IsEmbeddedFrame() { return m_isEmbedded; }
    /* Returns whether this frame is lightweight. */
    INLINE virtual BOOL IsLightweightFrame() { return m_isLightweight; }

    INLINE BOOL IsSimpleWindow() { return FALSE; }

    /* Returns whether this window is in iconified state. */
    INLINE BOOL isIconic() { return m_iconic; }
    INLINE void setIconic(BOOL b) { m_iconic = b; }

    /* Returns whether this window is in zoomed state. */
    INLINE BOOL isZoomed() { return m_zoomed; }
    INLINE void setZoomed(BOOL b) { m_zoomed = b; }

    void Show();

    INLINE void DrawMenuBar() { VERIFY(::DrawMenuBar(GetHWnd())); }

    virtual void DoUpdateIcon();
    virtual HICON GetEffectiveIcon(int iconType);

    /*for WmDrawItem and WmMeasureItem method */
    AwtMenuBar* GetMenuBar();
    void SetMenuBar(AwtMenuBar*);

    virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);

    MsgRouting WmGetMinMaxInfo(LPMINMAXINFO lpmmi);
    MsgRouting WmSize(UINT type, int w, int h);
    MsgRouting WmActivate(UINT nState, BOOL fMinimized, HWND opposite);
    MsgRouting WmDrawItem(UINT ctrlId, DRAWITEMSTRUCT& drawInfo);
    MsgRouting WmMeasureItem(UINT ctrlId, MEASUREITEMSTRUCT& measureInfo);
    MsgRouting WmEnterMenuLoop(BOOL isTrackPopupMenu);
    MsgRouting WmExitMenuLoop(BOOL isTrackPopupMenu);
    MsgRouting WmMouseUp(UINT flags, int x, int y, int button);
    MsgRouting WmMouseMove(UINT flags, int x, int y);
    MsgRouting WmNcMouseDown(WPARAM hitTest, int x, int y, int button);
    MsgRouting WmNcMouseUp(WPARAM hitTest, int x, int y, int button);
    MsgRouting WmGetIcon(WPARAM iconType, LRESULT& retVal);
    MsgRouting WmShowWindow(BOOL show, UINT status);
    MsgRouting WmWindowPosChanging(LPARAM windowPos);

    virtual MsgRouting WmSysCommand(UINT uCmdType, int xPos, int yPos);

    LRESULT WinThreadExecProc(ExecuteArgs * args);

    INLINE BOOL IsUndecorated() { return m_isUndecorated; }

    INLINE HWND GetProxyFocusOwner() {
        return GetHWnd();
    }

    void SetMaximizedBounds(int x, int y, int w, int h);
    void ClearMaximizedBounds();

    // returns true if the frame is inputmethod window
    INLINE BOOL isInputMethodWindow() { return m_isInputMethodWindow; }
    // adjusts the IME candidate window position if needed
    void AdjustCandidateWindowPos();

    // invoked on Toolkit thread
    static jobject _GetBoundsPrivate(void *param);

    // some methods called on Toolkit thread
    static void _SetState(void *param);
    static jint _GetState(void *param);
    static void _SetMaximizedBounds(void *param);
    static void _ClearMaximizedBounds(void *param);
    static void _SetMenuBar(void *param);
    static void _SetIMMOption(void *param);
    static void _SynthesizeWmActivate(void *param);
    static void _NotifyModalBlocked(void *param);

    virtual void Reshape(int x, int y, int width, int height);

    virtual BOOL AwtSetActiveWindow(BOOL isMouseEventCause = FALSE, UINT hittest = HTCLIENT);

    void CheckRetainActualFocusedWindow(HWND activatedOpositeHWnd);
    BOOL CheckActivateActualFocusedWindow(HWND deactivatedOpositeHWnd);

    INLINE HWND GetImeTargetComponent() { return m_imeTargetComponent; }
    INLINE void SetImeTargetComponent(HWND hwnd) { m_imeTargetComponent = hwnd; }

protected:
    /* The frame is undecorated. */
    BOOL m_isUndecorated;

private:
    LRESULT ProxyWindowProc(UINT message, WPARAM wParam, LPARAM lParam, MsgRouting &mr);

    /* The frame's embedding parent (if any) */
    HWND m_parentWnd;

    /* The frame's menubar. */
    AwtMenuBar* menuBar;

    /* The frame is an EmbeddedFrame. */
    BOOL m_isEmbedded;

    /* Fix for JDK-8056915:
       The embedded frame must gain focus by setting focus to its parent. */
    BOOL m_isEmbeddedInIE;

    /* Checks whether the frame is embedded in IE */
    static BOOL IsEmbeddedInIE(HWND hwndParent);

    /* The frame is a LightweightFrame */
    BOOL m_isLightweight;

    /* used so that calls to ::MoveWindow in SetMenuBar don't propogate
       because they are immediately followed by calls to Component.resize */
    BOOL m_ignoreWmSize;

    /* tracks whether or not menu on this frame is dropped down */
    BOOL m_isMenuDropped;

    /* The frame is an InputMethodWindow */
    BOOL m_isInputMethodWindow;

    // retains the target component for the IME messages
    HWND m_imeTargetComponent;

    /*
     * Fix for 4823903.
     * Retains a focus proxied window to set the focus correctly
     * when its owner get activated.
     */
    AwtWindow *m_actualFocusedWindow;

    /* The original, default WndProc for m_proxyFocusOwner. */
    WNDPROC m_proxyDefWindowProc;

    BOOL m_iconic;          /* are we in an iconic state */
    BOOL m_zoomed;          /* are we in a zoomed state */

    /* whether WmSize() must unconditionally reset zoomed state */
    BOOL m_forceResetZoomed;

    BOOL  m_maxBoundsSet;
    POINT m_maxPos;
    POINT m_maxSize;

    BOOL isInManualMoveOrSize;
    WPARAM grabbedHitTest;
    POINT savedMousePos;

    /*
     * Hashtable<Thread, BlockedThreadStruct> - a table that contains all the
     * information about non-toolkit threads with modal blocked embedded
     * frames. This information includes: number of blocked embedded frames
     * created on the thread, and mouse and modal hooks installed for
     * that thread. For every thread each hook is installed only once
     */
    static Hashtable sm_BlockedThreads;
};

#endif /* AWT_FRAME_H */
