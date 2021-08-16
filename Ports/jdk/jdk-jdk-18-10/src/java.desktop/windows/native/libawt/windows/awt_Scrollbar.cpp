/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Toolkit.h"
#include "awt_Scrollbar.h"
#include "awt_Canvas.h"
#include "awt_Window.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _SetValues() method
struct SetValuesStruct {
    jobject scrollbar;
    jint value;
    jint visible;
    jint min, max;
};
// struct for _SetLineIncrement()/_SetPageIncrement() methods
struct SetIncrementStruct {
    jobject scrollbar;
    jint increment;
};
/************************************************************************
 * AwtScrollbar fields
 */

jfieldID AwtScrollbar::lineIncrementID;
jfieldID AwtScrollbar::pageIncrementID;
jfieldID AwtScrollbar::orientationID;

BOOL     AwtScrollbar::ms_isInsideMouseFilter = FALSE;
int      AwtScrollbar::ms_instanceCounter = 0;
HHOOK    AwtScrollbar::ms_hMouseFilter;

/************************************************************************
 * AwtScrollbar methods
 */

AwtScrollbar::AwtScrollbar() {
    m_orientation = SB_HORZ;
    m_lineIncr = 0;
    m_pageIncr = 0;
    m_prevCallback = NULL;
    m_prevCallbackPos = 0;
    ms_instanceCounter++;

    /*
     * Fix for 4515085.
     * Use the hook to process WM_LBUTTONUP message.
     */
    if (AwtScrollbar::ms_instanceCounter == 1) {
        AwtScrollbar::ms_hMouseFilter =
            ::SetWindowsHookEx(WH_MOUSE, (HOOKPROC)AwtScrollbar::MouseFilter,
                               0, AwtToolkit::MainThread());
    }
}

AwtScrollbar::~AwtScrollbar()
{
}

void AwtScrollbar::Dispose()
{
    if (--ms_instanceCounter == 0) {
        ::UnhookWindowsHookEx(ms_hMouseFilter);
    }
    AwtComponent::Dispose();
}

LPCTSTR
AwtScrollbar::GetClassName() {
    return TEXT("SCROLLBAR");  /* System provided scrollbar class */
}

/* Create a new AwtScrollbar object and window.   */
AwtScrollbar *
AwtScrollbar::Create(jobject peer, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtScrollbar* c = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtCanvas* awtParent;

        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        target = env->GetObjectField(peer, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        c = new AwtScrollbar();

        {
            jint orientation =
                env->GetIntField(target, AwtScrollbar::orientationID);
            c->m_orientation = (orientation == java_awt_Scrollbar_VERTICAL) ?
                SB_VERT : SB_HORZ;
            c->m_lineIncr =
                env->GetIntField(target, AwtScrollbar::lineIncrementID);
            c->m_pageIncr =
                env->GetIntField(target, AwtScrollbar::pageIncrementID);

            DWORD style = WS_CHILD | WS_CLIPSIBLINGS |
                c->m_orientation;/* Note: SB_ and SBS_ are the same here */

            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);

            c->CreateHWnd(env, L"", style, 0,
                          x, y, width, height,
                          awtParent->GetHWnd(),
                          reinterpret_cast<HMENU>(static_cast<INT_PTR>(
                awtParent->CreateControlID())),
                          ::GetSysColor(COLOR_SCROLLBAR),
                          ::GetSysColor(COLOR_SCROLLBAR),
                          peer);
            c->m_backgroundColorSet = TRUE;
            /* suppress inheriting parent's color. */
            c->UpdateBackground(env, target);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    return c;
}

LRESULT CALLBACK
AwtScrollbar::MouseFilter(int nCode, WPARAM wParam, LPARAM lParam)
{
    if (((UINT)wParam == WM_LBUTTONUP || (UINT)wParam == WM_MOUSEMOVE) &&
        ms_isInsideMouseFilter != TRUE &&
        nCode >= 0)
    {
        HWND hwnd = ((PMOUSEHOOKSTRUCT)lParam)->hwnd;
        AwtComponent *comp = AwtComponent::GetComponent(hwnd);

        if (comp != NULL && comp->IsScrollbar()) {
            MSG msg;
            LPMSG lpMsg = (LPMSG)&msg;
            UINT msgID = (UINT)wParam;

            ms_isInsideMouseFilter = TRUE;

            // Peek the message to get wParam containing the message's flags.
            // <::PeekMessage> will call this hook again. To prevent recursive
            // processing the <ms_isInsideMouseFilter> flag is used.
            // Calling <::PeekMessage> is not so good desision but is the only one
            // found to get those flags (used further in Java event creation).
            // WARNING! If you are about to add new hook of WM_MOUSE type make
            // it ready for recursive call, otherwise modify this one.
            if (::PeekMessage(lpMsg, hwnd, msgID, msgID, PM_NOREMOVE)) {
                comp->WindowProc(msgID, lpMsg->wParam, lpMsg->lParam);
            }

            ms_isInsideMouseFilter = FALSE;
        }
    }
    return ::CallNextHookEx(AwtScrollbar::ms_hMouseFilter, nCode, wParam, lParam);
}


LRESULT
AwtScrollbar::WindowProc(UINT message, WPARAM wParam, LPARAM lParam)
{
    // Delegate real work to super
    LRESULT retValue = AwtComponent::WindowProc(message, wParam, lParam);

    // After-hooks for workarounds
    switch (message) {

      // Work around a windows bug described in KB article Q73839.
      // Need to update focus indicator on scrollbar if thumb
      // proportion or thumb position was changed.

      case WM_SIZE:
      case SBM_SETSCROLLINFO:
      case SBM_SETRANGE:
      case SBM_SETRANGEREDRAW:
          if (AwtComponent::sm_focusOwner == GetHWnd()) {
              UpdateFocusIndicator();
          }
          break;
    }

    return retValue;
}

MsgRouting
AwtScrollbar::WmNcHitTest(UINT x, UINT y, LRESULT& retVal)
{
    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd())))) {
        retVal = HTCLIENT;
        return mrConsume;
    }
    return AwtComponent::WmNcHitTest(x, y, retVal);
}

// Fix for a race condition when the WM_LBUTTONUP is picked by the AWT
// message loop before(!) the windows internal message loop for the
// scrollbar is started in response to WM_LBUTTONDOWN.  See KB article
// Q102552.
//
// Note that WM_LBUTTONUP is processed by the windows internal message
// loop.  May be we can synthesize a MOUSE_RELEASED event but that
// seems kludgey, so we'd better left this as is for now.

MsgRouting
AwtScrollbar::WmMouseDown(UINT flags, int x, int y, int button)
{
    // We pass the WM_LBUTTONDOWN up to Java, but we process it
    // immediately as well to avoid the race.  Later when this press
    // event returns to us wrapped into a WM_AWT_HANDLE_EVENT we
    // ignore it in the HandleEvent below.  This means that we can not
    // consume the mouse press in the Java world.

    MsgRouting usualRoute = AwtComponent::WmMouseDown(flags, x, y, button);

    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd())))) {
        return mrConsume;
    }

    if (button == LEFT_BUTTON)
        return mrDoDefault;    // Force immediate processing to avoid the race.
    else
        return usualRoute;
}

MsgRouting
AwtScrollbar::HandleEvent(MSG *msg, BOOL synthetic)
{
    // SCROLLBAR control doesn't cause activation on mouse/key events,
    // so we can safely (for synthetic focus) pass them to the system proc.

    if (IsFocusingMouseMessage(msg)) {
        // Left button press was already routed to default window
        // procedure in the WmMouseDown above.  Propagating synthetic
        // press seems like a bad idea as internal message loop
        // doesn't know how to unwrap synthetic release.
        delete msg;
        return mrConsume;
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

// Work around a windows bug descrbed in KB article Q73839.  Reset
// focus on scrollbars to update focus indicator.  The article advises
// to disable/enable the scrollbar.
void
AwtScrollbar::UpdateFocusIndicator()
{
    if (IsFocusable()) {
        // todo: doesn't work
        SendMessage((WPARAM)ESB_DISABLE_BOTH);
        SendMessage((WPARAM)ESB_ENABLE_BOTH);
    }
}

// In a windows app one would call SetScrollInfo from WM_[HV]SCROLL
// handler directly.  Since we call SetScrollInfo from Java world
// after scroll handler is over next WM_[HV]SCROLL event can be
// delivered before SetScrollInfo was called in response to the
// previous one and thus we would fire exactly the same event which
// will only contribute to the growth of the backlog of scroll events.

const char * const AwtScrollbar::SbNlineDown = "lineDown";
const char * const AwtScrollbar::SbNlineUp   = "lineUp";
const char * const AwtScrollbar::SbNpageDown = "pageDown";
const char * const AwtScrollbar::SbNpageUp   = "pageUp";
const char * const AwtScrollbar::SbNdrag     = "drag";
const char * const AwtScrollbar::SbNdragEnd  = "dragEnd";
const char * const AwtScrollbar::SbNwarp     = "warp";

inline void
AwtScrollbar::DoScrollCallbackCoalesce(const char* methodName, int newPos)
{
    if (methodName == m_prevCallback && newPos == m_prevCallbackPos) {
        DTRACE_PRINTLN2("AwtScrollbar: ignoring duplicate callback %s(%d)",
                        methodName, newPos);
    }
    else {
        DoCallback(methodName, "(I)V", newPos);
        m_prevCallback = methodName;
        m_prevCallbackPos = newPos;
    }
}


MsgRouting
AwtScrollbar::WmVScroll(UINT scrollCode, UINT pos, HWND hScrollbar)
{
    int minVal, maxVal;    // scrollbar range
    int minPos, maxPos;    // thumb positions (max depends on visible amount)
    int curPos, newPos;

    // For drags we have old (static) and new (dynamic) thumb positions
    int dragP = (scrollCode == SB_THUMBTRACK
              || scrollCode == SB_THUMBPOSITION);
    int thumbPos;

    SCROLLINFO si;
    si.cbSize = sizeof si;
    si.fMask = SIF_POS | SIF_PAGE | SIF_RANGE;

    // From, _Win32 Programming_, by Rector and Newcommer, p. 185:
    // "In some of the older documentation on Win32 scroll bars,
    // including that published by Microsoft, you may read that
    // you *cannot* obtain the scroll position while in a handler.
    // The SIF_TRACKPOS flag was added after this documentation
    // was published.  Beware of this older documentation; it may
    // have other obsolete features."
    if (dragP) {
        si.fMask |= SIF_TRACKPOS;
    }

    VERIFY(::GetScrollInfo(GetHWnd(), SB_CTL, &si));
    curPos = si.nPos;
    minPos = minVal = si.nMin;

    // Upper bound of the range.  Note that adding 1 here is safe
    // and won't cause a wrap, since we have substracted 1 in the
    // SetValues above.
    maxVal = si.nMax + 1;

    // Meaningful maximum position is maximum - visible.
    maxPos = maxVal - si.nPage;

    // XXX: Documentation for SBM_SETRANGE says that scrollbar
    // range is limited by MAXLONG, which is 2**31, but when a
    // scroll range is greater than that, thumbPos is reported
    // incorrectly due to integer arithmetic wrap(s).
    thumbPos = dragP ? si.nTrackPos : curPos;

    // NB: Beware arithmetic wrap when calculating newPos
    switch (scrollCode) {

      case SB_LINEUP:
          if ((__int64)curPos - m_lineIncr > minPos)
              newPos = curPos - m_lineIncr;
          else
              newPos = minPos;
          if (newPos != curPos)
              DoScrollCallbackCoalesce(SbNlineUp, newPos);
          break;

      case SB_LINEDOWN:
          if ((__int64)curPos + m_lineIncr < maxPos)
              newPos = curPos + m_lineIncr;
          else
              newPos = maxPos;
          if (newPos != curPos)
              DoScrollCallbackCoalesce(SbNlineDown, newPos);
          break;

      case SB_PAGEUP:
          if ((__int64)curPos - m_pageIncr > minPos)
              newPos = curPos - m_pageIncr;
          else
              newPos = minPos;
          if (newPos != curPos)
              DoScrollCallbackCoalesce(SbNpageUp, newPos);
          break;

      case SB_PAGEDOWN:
          if ((__int64)curPos + m_pageIncr < maxPos)
              newPos = curPos + m_pageIncr;
          else
              newPos = maxPos;
          if (newPos != curPos)
              DoScrollCallbackCoalesce(SbNpageDown, newPos);
          break;

      case SB_TOP:
          if (minPos != curPos)
              DoScrollCallbackCoalesce(SbNwarp, minPos);
          break;

      case SB_BOTTOM:
          if (maxPos != curPos)
              DoScrollCallbackCoalesce(SbNwarp, maxPos);
          break;

      case SB_THUMBTRACK:
          if (thumbPos != curPos)
              DoScrollCallbackCoalesce(SbNdrag, thumbPos);
          break;

      case SB_THUMBPOSITION:
          DoScrollCallbackCoalesce(SbNdragEnd, thumbPos);
          break;

      case SB_ENDSCROLL:
          // reset book-keeping info
          m_prevCallback = NULL;
          break;
    }
    return mrDoDefault;
}

MsgRouting
AwtScrollbar::WmHScroll(UINT scrollCode, UINT pos, HWND hScrollbar)
{
    return WmVScroll(scrollCode, pos, hScrollbar);
}

void AwtScrollbar::_SetValues(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetValuesStruct *svs = (SetValuesStruct *)param;
    jobject self = svs->scrollbar;

    SCROLLINFO si;
    si.cbSize = sizeof si;
    si.fMask  = SIF_POS | SIF_PAGE | SIF_RANGE;
    si.nMin   = svs->min;
    si.nMax   = svs->max - 1;
    si.nPage  = svs->visible;
    si.nPos   = svs->value;

    AwtScrollbar *sb = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    sb = (AwtScrollbar *)pData;
    if (::IsWindow(sb->GetHWnd()))
    {
        BOOL update_p = ::IsWindowEnabled(sb->GetHWnd()); // don't redraw if disabled
        DTRACE_PRINTLN5("AwtScrollbar::SetValues(val = %d, vis = %d,"//(ctd.)
                        " min = %d, max = %d)%s",
            svs->value, svs->visible, svs->min, svs->max,
            update_p ? "" : " - NOT redrawing");
        ::SetScrollInfo(sb->GetHWnd(), SB_CTL, &si, update_p);
    }
ret:
    env->DeleteGlobalRef(self);

    delete svs;
}

void AwtScrollbar::_SetLineIncrement(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetIncrementStruct *sis = (SetIncrementStruct *)param;
    jobject self = sis->scrollbar;
    jint increment = sis->increment;

    AwtScrollbar *sb = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    sb = (AwtScrollbar *)pData;
    if (::IsWindow(sb->GetHWnd()))
    {
        sb->SetLineIncrement(increment);
    }
ret:
    env->DeleteGlobalRef(self);

    delete sis;
}

void AwtScrollbar::_SetPageIncrement(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetIncrementStruct *sis = (SetIncrementStruct *)param;
    jobject self = sis->scrollbar;
    jint increment = sis->increment;

    AwtScrollbar *sb = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    sb = (AwtScrollbar *)pData;
    if (::IsWindow(sb->GetHWnd()))
    {
        sb->SetPageIncrement(increment);
    }
ret:
    env->DeleteGlobalRef(self);

    delete sis;
}

/************************************************************************
 * Scrollbar native methods
 */

extern "C" {

/*
 * Class:     java_awt_Scrollbar
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_Scrollbar_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtScrollbar::lineIncrementID = env->GetFieldID(cls, "lineIncrement", "I");
    DASSERT(AwtScrollbar::lineIncrementID != NULL);
    CHECK_NULL(AwtScrollbar::lineIncrementID);

    AwtScrollbar::pageIncrementID = env->GetFieldID(cls, "pageIncrement", "I");
    DASSERT(AwtScrollbar::pageIncrementID != NULL);
    CHECK_NULL(AwtScrollbar::pageIncrementID);

    AwtScrollbar::orientationID = env->GetFieldID(cls, "orientation", "I");
    DASSERT(AwtScrollbar::orientationID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WScrollbarPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WScrollbarPeer
 * Method:    setValues
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollbarPeer_setValues(JNIEnv *env, jobject self,
                                              jint value, jint visible,
                                              jint minimum, jint maximum)
{
    TRY;

    SetValuesStruct *svs = new SetValuesStruct;
    svs->scrollbar = env->NewGlobalRef(self);
    svs->value = value;
    svs->visible = visible;
    svs->min = minimum;
    svs->max = maximum;

    AwtToolkit::GetInstance().SyncCall(AwtScrollbar::_SetValues, svs);
    // global ref and svs are deleted in _SetValues

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollbarPeer
 * Method:    setLineIncrement
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollbarPeer_setLineIncrement(JNIEnv *env, jobject self,
                                                     jint increment)
{
    TRY;

    SetIncrementStruct *sis = new SetIncrementStruct;
    sis->scrollbar = env->NewGlobalRef(self);
    sis->increment = increment;

    AwtToolkit::GetInstance().SyncCall(AwtScrollbar::_SetLineIncrement, sis);
    // global ref and svs are deleted in _SetValues

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollbarPeer
 * Method:    setPageIncrement
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollbarPeer_setPageIncrement(JNIEnv *env, jobject self,
                                                     jint increment)
{
    TRY;

    SetIncrementStruct *sis = new SetIncrementStruct;
    sis->scrollbar = env->NewGlobalRef(self);
    sis->increment = increment;

    AwtToolkit::GetInstance().SyncCall(AwtScrollbar::_SetPageIncrement, sis);
    // global ref and svs are deleted in _SetValues

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollbarPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollbarPeer_create(JNIEnv *env, jobject self,
                                           jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtScrollbar::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollbarPeer
 * Method:    getScrollbarSize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WScrollbarPeer_getScrollbarSize(JNIEnv *env, jclass clazz, jint orientation)
{
    if (orientation == java_awt_Scrollbar_VERTICAL) {
        return ::GetSystemMetrics(SM_CXVSCROLL);
    } else {
        return ::GetSystemMetrics(SM_CYHSCROLL);
    }
}

} /* extern "C" */
