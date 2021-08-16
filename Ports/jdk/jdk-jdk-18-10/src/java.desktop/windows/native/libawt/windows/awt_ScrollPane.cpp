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

#include "awt_ScrollPane.h"

#include "awt_Container.h"
#include "awt_Insets.h"
#include "awt_Panel.h"
#include "awt_Scrollbar.h"   // static #defines
#include "awt_Toolkit.h"
#include "awt_Window.h"

#include <java_awt_Adjustable.h>
#include <java_awt_ScrollPane.h>
#include <java_awt_ScrollPaneAdjustable.h>
#include <java_awt_event_AdjustmentEvent.h>


/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _GetOffset() method
struct GetOffsetStruct {
    jobject scrollpane;
    jint orient;
};
// struct for _SetScrollPos() method
struct SetScrollPosStruct {
    jobject scrollpane;
    jint x, y;
};
// struct for _SetSpans() method
struct SetSpansStruct {
    jobject scrollpane;
    jint parentWidth;
    jint parentHeight;
    jint childWidth;
    jint childHeight;
};
/************************************************************************
 * AwtScrollPane fields
 */

jfieldID AwtScrollPane::scrollbarDisplayPolicyID;
jfieldID AwtScrollPane::hAdjustableID;
jfieldID AwtScrollPane::vAdjustableID;
jfieldID AwtScrollPane::unitIncrementID;
jfieldID AwtScrollPane::blockIncrementID;
jmethodID AwtScrollPane::postScrollEventID;

/************************************************************************
 * AwtScrollPane methods
 */

AwtScrollPane::AwtScrollPane() {
}

LPCTSTR AwtScrollPane::GetClassName() {
    return TEXT("SunAwtScrollPane");
}

/* Create a new AwtScrollPane object and window.   */
AwtScrollPane* AwtScrollPane::Create(jobject self, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject target = NULL;
    AwtScrollPane* c = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtComponent* awtParent;

        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtComponent*)pData;

        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        c = new AwtScrollPane();

        {
            DWORD style = WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS;
            jint scrollbarDisplayPolicy =
                env->GetIntField(target, scrollbarDisplayPolicyID);

            if (scrollbarDisplayPolicy
                    == java_awt_ScrollPane_SCROLLBARS_ALWAYS) {
                style |= WS_HSCROLL | WS_VSCROLL;
            }
            DWORD exStyle = WS_EX_CLIENTEDGE;

            if (GetRTL()) {
                exStyle |= WS_EX_RIGHT | WS_EX_LEFTSCROLLBAR;
                if (GetRTLReadingOrder())
                    exStyle |= WS_EX_RTLREADING;
            }

            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);
            c->CreateHWnd(env, L"", style, exStyle,
                          x, y, width, height,
                          awtParent->GetHWnd(),
                          reinterpret_cast<HMENU>(static_cast<INT_PTR>(
                awtParent->CreateControlID())),
                          ::GetSysColor(COLOR_WINDOWTEXT),
                          ::GetSysColor(COLOR_WINDOW),
                          self);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    return c;
}

void AwtScrollPane::SetInsets(JNIEnv *env)
{
    RECT outside;
    RECT inside;
    ::GetWindowRect(GetHWnd(), &outside);
    ::GetClientRect(GetHWnd(), &inside);
    ::MapWindowPoints(GetHWnd(), 0, (LPPOINT)&inside, 2);

    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    jobject insets =
      (env)->GetObjectField(GetPeer(env), AwtPanel::insets_ID);

    DASSERT(!safe_ExceptionOccurred(env));

    if (insets != NULL && (inside.top-outside.top) != 0) {
        (env)->SetIntField(insets, AwtInsets::topID, inside.top - outside.top);
        (env)->SetIntField(insets, AwtInsets::leftID, inside.left - outside.left);
        (env)->SetIntField(insets, AwtInsets::bottomID, outside.bottom - inside.bottom);
        (env)->SetIntField(insets, AwtInsets::rightID, outside.right - inside.right);
    }

    env->DeleteLocalRef(insets);
}

void AwtScrollPane::SetScrollInfo(int orient, int max, int page,
                                  BOOL disableNoScroll)
{
    DTRACE_PRINTLN4("AwtScrollPane::SetScrollInfo %d, %d, %d, %d", orient, max, page, disableNoScroll);
    SCROLLINFO si;
    int posBefore;
    int posAfter;

    posBefore = GetScrollPos(orient);
    si.cbSize = sizeof(SCROLLINFO);
    si.nMin = 0;
    si.nMax = max;
    si.fMask = SIF_RANGE;
    if (disableNoScroll) {
        si.fMask |= SIF_DISABLENOSCROLL;
    }
    if (page > 0) {
        si.fMask |= SIF_PAGE;
        si.nPage = page;
    }
    ::SetScrollInfo(GetHWnd(), orient, &si, TRUE);
    // scroll position may have changed when thumb is at the end of the bar
    // and the page size changes
    posAfter = GetScrollPos(orient);
    if (posBefore != posAfter) {
        PostScrollEvent(orient, SB_THUMBPOSITION, posAfter);
    }
}

void AwtScrollPane::RecalcSizes(int parentWidth, int parentHeight,
                                int childWidth, int childHeight)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }

    /* Determine border width without scrollbars. */
    int horzBorder = ::GetSystemMetrics(SM_CXEDGE);;
    int vertBorder = ::GetSystemMetrics(SM_CYEDGE);;

    parentWidth -= (horzBorder * 2);
    parentHeight -= (vertBorder * 2);

    /* Enable each scrollbar as needed. */
    jobject target = AwtObject::GetTarget(env);
    jint policy = env->GetIntField(target,
                                   AwtScrollPane::scrollbarDisplayPolicyID);

    BOOL needsHorz=(policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS ||
                    (policy == java_awt_ScrollPane_SCROLLBARS_AS_NEEDED &&
                     childWidth > parentWidth));
    if (needsHorz) {
        parentHeight -= ::GetSystemMetrics(SM_CYHSCROLL);
    }
    BOOL needsVert=(policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS ||
                    (policy ==java_awt_ScrollPane_SCROLLBARS_AS_NEEDED &&
                     childHeight > parentHeight));
    if (needsVert) {
        parentWidth -= ::GetSystemMetrics(SM_CXVSCROLL);
    }
    /*
     * Since the vertical scrollbar may have reduced the parent width
     * enough to now require a horizontal scrollbar, we need to
     * recalculate the horizontal metrics and scrollbar boolean.
     */
    if (!needsHorz) {
        needsHorz = (policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS ||
                     (policy == java_awt_ScrollPane_SCROLLBARS_AS_NEEDED &&
                      childWidth > parentWidth));
        if (needsHorz) {
            parentHeight -= ::GetSystemMetrics(SM_CYHSCROLL);
        }
    }

    /* Now set ranges -- setting the min and max the same disables them. */
    if (needsHorz) {
        jobject hAdj =
            env->GetObjectField(target, AwtScrollPane::hAdjustableID);
        env->SetIntField(hAdj, AwtScrollPane::blockIncrementID, parentWidth);
        SetScrollInfo(SB_HORZ, childWidth - 1, parentWidth,
                      (policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS));
        env->DeleteLocalRef(hAdj);
    } else {
        /* Set scroll info to imitate the behaviour and since we don't
            need to display it, explicitly don't show the bar */
        SetScrollInfo(SB_HORZ, childWidth - 1, parentWidth,
                      (policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS));
        ::ShowScrollBar(GetHWnd(), SB_HORZ, false);
    }

    if (needsVert) {
        jobject vAdj =
            env->GetObjectField(target, AwtScrollPane::vAdjustableID);
        env->SetIntField(vAdj, AwtScrollPane::blockIncrementID, parentHeight);
        SetScrollInfo(SB_VERT, childHeight - 1, parentHeight,
                      (policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS));
        env->DeleteLocalRef(vAdj);
    } else {
        /* Set scroll info to imitate the behaviour and since we don't
            need to display it, explicitly don't show the bar */
        SetScrollInfo(SB_VERT, childHeight - 1, parentHeight,
                      (policy == java_awt_ScrollPane_SCROLLBARS_ALWAYS));
        ::ShowScrollBar(GetHWnd(), SB_VERT, false);
    }

    env->DeleteLocalRef(target);
}

void AwtScrollPane::Reshape(int x, int y, int w, int h)
{
    AwtComponent::Reshape(x, y, w, h);
}

void AwtScrollPane::Show(JNIEnv *env)
{
    SetInsets(env);
    SendMessage(WM_AWT_COMPONENT_SHOW);
}

void AwtScrollPane::PostScrollEvent(int orient, int scrollCode, int pos) {
    if (scrollCode == SB_ENDSCROLL) {
        return;
    }

    // convert Windows scroll bar ident to peer ident
    jint jorient;
    if (orient == SB_VERT) {
        jorient = java_awt_Adjustable_VERTICAL;
    } else if (orient == SB_HORZ) {
        jorient = java_awt_Adjustable_HORIZONTAL;
    } else {
        DASSERT(FALSE);
        return;
    }

    // convert Windows scroll code to adjustment type and isAdjusting status
    jint jscrollcode;
    jboolean jadjusting = JNI_FALSE;
    SCROLLINFO si;
    switch (scrollCode) {
      case SB_LINEUP:
          jscrollcode = java_awt_event_AdjustmentEvent_UNIT_DECREMENT;
          break;
      case SB_LINEDOWN:
          jscrollcode = java_awt_event_AdjustmentEvent_UNIT_INCREMENT;
          break;
      case SB_PAGEUP:
          jscrollcode = java_awt_event_AdjustmentEvent_BLOCK_DECREMENT;
          break;
      case SB_PAGEDOWN:
          jscrollcode = java_awt_event_AdjustmentEvent_BLOCK_INCREMENT;
          break;
      case SB_TOP:
          jscrollcode = java_awt_event_AdjustmentEvent_TRACK;
          ZeroMemory(&si, sizeof(si));
          si.cbSize = sizeof(si);
          si.fMask = SIF_RANGE;
          ::GetScrollInfo(GetHWnd(), orient, &si);
          pos = si.nMin;
          break;
      case SB_BOTTOM:
          jscrollcode = java_awt_event_AdjustmentEvent_TRACK;
          ZeroMemory(&si, sizeof(si));
          si.cbSize = sizeof(si);
          si.fMask = SIF_RANGE;
          ::GetScrollInfo(GetHWnd(), orient, &si);
          pos = si.nMax;
          break;
      case SB_THUMBTRACK:
          jscrollcode = java_awt_event_AdjustmentEvent_TRACK;
          jadjusting = JNI_TRUE;
          break;
      case SB_THUMBPOSITION:
          jscrollcode = java_awt_event_AdjustmentEvent_TRACK;
          break;
      default:
          DASSERT(FALSE);
          return;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    env->CallVoidMethod(GetPeer(env), AwtScrollPane::postScrollEventID,
                        jorient, jscrollcode, (jint)pos, jadjusting);
    DASSERT(!safe_ExceptionOccurred(env));
}

MsgRouting
AwtScrollPane::WmNcHitTest(UINT x, UINT y, LRESULT& retVal)
{
    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd())))) {
        retVal = HTCLIENT;
        return mrConsume;
    }
    return AwtCanvas::WmNcHitTest(x, y, retVal);
}

MsgRouting AwtScrollPane::WmVScroll(UINT scrollCode, UINT pos, HWND hScrollPane)
{
    // While user scrolls using tracker, SCROLLINFO.nPos is not changed, SCROLLINFO.nTrackPos is changed instead.
    int dragP = scrollCode == SB_THUMBPOSITION || scrollCode == SB_THUMBTRACK;
    int newPos = GetScrollPos(SB_VERT);
    if ( dragP ) {
        SCROLLINFO si;
        ZeroMemory(&si, sizeof(si));
        si.cbSize = sizeof(si);
        si.fMask = SIF_TRACKPOS;
        ::GetScrollInfo(GetHWnd(), SB_VERT, &si);
        newPos = si.nTrackPos;
    }
    PostScrollEvent(SB_VERT, scrollCode, newPos);
    return mrConsume;
}

MsgRouting AwtScrollPane::WmHScroll(UINT scrollCode, UINT pos, HWND hScrollPane)
{
    // While user scrolls using tracker, SCROLLINFO.nPos is not changed, SCROLLINFO.nTrackPos is changed instead.
    int dragP = scrollCode == SB_THUMBPOSITION || scrollCode == SB_THUMBTRACK;
    int newPos = GetScrollPos(SB_HORZ);
    if ( dragP ) {
        SCROLLINFO si;
        ZeroMemory(&si, sizeof(si));
        si.cbSize = sizeof(si);
        si.fMask = SIF_TRACKPOS;
        ::GetScrollInfo(GetHWnd(), SB_HORZ, &si);
        newPos = si.nTrackPos;
    }
    PostScrollEvent(SB_HORZ, scrollCode, newPos);
    return mrConsume;
}

MsgRouting AwtScrollPane::HandleEvent(MSG *msg, BOOL synthetic)
{
    // SunAwtScrollPane control doesn't cause activation on mouse/key events,
    // so we can safely (for synthetic focus) pass them to the system proc.
    return AwtComponent::HandleEvent(msg, synthetic);
}

int AwtScrollPane::GetScrollPos(int orient)
{
    SCROLLINFO si;
    ZeroMemory(&si, sizeof(si));
    si.cbSize = sizeof(si);
    si.fMask = SIF_POS;
    ::GetScrollInfo(GetHWnd(), orient, &si);
    return si.nPos;
}

jint AwtScrollPane::_GetOffset(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    GetOffsetStruct *gos = (GetOffsetStruct *)param;
    jobject self = gos->scrollpane;
    jint orient = gos->orient;

    jint result = 0;
    AwtScrollPane *s = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    s = (AwtScrollPane *)pData;
    if (::IsWindow(s->GetHWnd()))
    {
        DTRACE_PRINTLN2("%x: WScrollPanePeer.getOffset(%d)", self, orient);
        s->VerifyState();
        int nBar = (orient == java_awt_Adjustable_HORIZONTAL) ? SB_HORZ : SB_VERT;
        result = s->GetScrollPos(nBar);
    }
ret:
   env->DeleteGlobalRef(self);

   delete gos;

   return result;
}

void AwtScrollPane::_SetInsets(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtScrollPane *s = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    s = (AwtScrollPane *)pData;
    if (::IsWindow(s->GetHWnd()))
    {
        DTRACE_PRINTLN1("%x: WScrollPanePeer.setInsets()", self);
        s->SetInsets(env);
        s->VerifyState();
    }
ret:
   env->DeleteGlobalRef(self);
}

void AwtScrollPane::_SetScrollPos(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetScrollPosStruct *spss = (SetScrollPosStruct *)param;
    jobject self = spss->scrollpane;
    jint x = spss->x;
    jint y = spss->y;

    AwtScrollPane *s = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    s = (AwtScrollPane *)pData;
    if (::IsWindow(s->GetHWnd()))
    {
        DTRACE_PRINTLN3("%x: WScrollPanePeer.setScrollPosition(%d, %d)", self, x, y);
        SCROLLINFO si;
        ZeroMemory(&si, sizeof(si));
        si.fMask = SIF_POS;
        si.cbSize = sizeof(si);
        // set x
        si.nPos = x;
        ::SetScrollInfo(s->GetHWnd(), SB_HORZ, &si, TRUE);
        // set y
        si.nPos = y;
        ::SetScrollInfo(s->GetHWnd(), SB_VERT, &si, TRUE);
    }
ret:
   env->DeleteGlobalRef(self);

   delete spss;
}

void AwtScrollPane::_SetSpans(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetSpansStruct *sss = (SetSpansStruct *)param;
    jobject self = sss->scrollpane;
    jint parentWidth = sss->parentWidth;
    jint parentHeight = sss->parentHeight;
    jint childWidth = sss->childWidth;
    jint childHeight = sss->childHeight;

    AwtScrollPane *s = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    s = (AwtScrollPane *)pData;
    if (::IsWindow(s->GetHWnd()))
    {
        DTRACE_PRINTLN5("%x: WScrollPanePeer.setSpans(%d, %d, %d, %d)", self,
            parentWidth, parentHeight, childWidth, childHeight);
        s->RecalcSizes(parentWidth, parentHeight, childWidth, childHeight);
        s->VerifyState();
    }
ret:
   env->DeleteGlobalRef(self);

   delete sss;
}

#ifdef DEBUG
void AwtScrollPane::VerifyState()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(3) < 0) {
        return;
    }

    if (AwtToolkit::GetInstance().VerifyComponents() == FALSE) {
        return;
    }

    if (m_callbacksEnabled == FALSE) {
        /* Component is not fully setup yet. */
        return;
    }

    AwtComponent::VerifyState();

    jobject target = AwtObject::GetTarget(env);
    jobject child = JNU_CallMethodByName(env, NULL, GetPeer(env),
                                         "getScrollSchild",
                                         "()Ljava/awt/Component;").l;

    DASSERT(!safe_ExceptionOccurred(env));

    if (child != NULL) {
        jobject childPeer =
            (env)->GetObjectField(child, AwtComponent::peerID);
        PDATA pData;
        JNI_CHECK_PEER_RETURN(childPeer);
        AwtComponent* awtChild = (AwtComponent *)pData;

        /* Verify child window is positioned correctly. */
        RECT rect, childRect;
        ::GetClientRect(GetHWnd(), &rect);
        ::MapWindowPoints(GetHWnd(), 0, (LPPOINT)&rect, 2);
        ::GetWindowRect(awtChild->GetHWnd(), &childRect);
        DASSERT(childRect.left <= rect.left && childRect.top <= rect.top);

        env->DeleteLocalRef(childPeer);
    }
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(child);
}
#endif

/************************************************************************
 * ScrollPane native methods
 */

extern "C" {

/*
 * Class:     java_awt_ScrollPane
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_ScrollPane_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtScrollPane::scrollbarDisplayPolicyID =
        env->GetFieldID(cls, "scrollbarDisplayPolicy", "I");
    DASSERT(AwtScrollPane::scrollbarDisplayPolicyID != NULL);
    CHECK_NULL(AwtScrollPane::scrollbarDisplayPolicyID);

    AwtScrollPane::hAdjustableID =
        env->GetFieldID(cls, "hAdjustable", "Ljava/awt/ScrollPaneAdjustable;");
    DASSERT(AwtScrollPane::hAdjustableID != NULL);
    CHECK_NULL(AwtScrollPane::hAdjustableID);

    AwtScrollPane::vAdjustableID =
        env->GetFieldID(cls, "vAdjustable", "Ljava/awt/ScrollPaneAdjustable;");
    DASSERT(AwtScrollPane::vAdjustableID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * ScrollPaneAdjustable native methods
 */

extern "C" {

/*
 * Class:     java_awt_ScrollPaneAdjustable
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_ScrollPaneAdjustable_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtScrollPane::unitIncrementID = env->GetFieldID(cls,"unitIncrement", "I");
    DASSERT(AwtScrollPane::unitIncrementID != NULL);
    CHECK_NULL(AwtScrollPane::unitIncrementID);

    AwtScrollPane::blockIncrementID =
        env->GetFieldID(cls,"blockIncrement", "I");
    DASSERT(AwtScrollPane::blockIncrementID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * ScrollPanePeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollPanePeer_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtScrollPane::postScrollEventID =
        env->GetMethodID(cls, "postScrollEvent", "(IIIZ)V");
    DASSERT(AwtScrollPane::postScrollEventID != NULL);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollPanePeer_create(JNIEnv *env, jobject self,
                                            jobject parent)
{
    TRY;

    DTRACE_PRINTLN2("%x: WScrollPanePeer.create(%x)", self, parent);

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtScrollPane::Create);
    PDATA pData;
    JNI_CHECK_PEER_CREATION_RETURN(self);
    ((AwtScrollPane*)pData)->VerifyState();

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    getOffset
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WScrollPanePeer_getOffset(JNIEnv *env, jobject self,
                                               jint orient)
{
    TRY;

    GetOffsetStruct *gos = new GetOffsetStruct;
    gos->scrollpane = env->NewGlobalRef(self);
    gos->orient = orient;

    return static_cast<jint>(reinterpret_cast<INT_PTR>(AwtToolkit::GetInstance().SyncCall(
        (void *(*)(void *))AwtScrollPane::_GetOffset, gos)));
    // global ref and gos are deleted in _GetOffset()

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    setInsets
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollPanePeer_setInsets(JNIEnv *env, jobject self)
{
    TRY

    AwtToolkit::GetInstance().SyncCall(AwtScrollPane::_SetInsets,
        env->NewGlobalRef(self));
    // global ref is deleted in _SetInsets()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    setScrollPosition
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollPanePeer_setScrollPosition(JNIEnv *env,
                                                       jobject self,
                                                       jint x, jint y)
{
    TRY;

    SetScrollPosStruct *ssps = new SetScrollPosStruct;
    ssps->scrollpane = env->NewGlobalRef(self);
    ssps->x = x;
    ssps->y = y;

    AwtToolkit::GetInstance().SyncCall(AwtScrollPane::_SetScrollPos, ssps);
    // global ref and ssps are deleted in _SetScrollPos()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    _getHScrollbarHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WScrollPanePeer__1getHScrollbarHeight(JNIEnv *env,
                                                           jobject self)
{
    TRY;

    DTRACE_PRINTLN1("%x: WScrollPanePeer._getHScrollbarHeight()", self);
    return ::GetSystemMetrics(SM_CYHSCROLL);

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    _getVScrollbarWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WScrollPanePeer__1getVScrollbarWidth(JNIEnv *env,
                                                          jobject self)
{
    TRY;

    DTRACE_PRINTLN1("%x: WScrollPanePeer._getVScrollbarHeight()", self);
    return ::GetSystemMetrics(SM_CXVSCROLL);

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WScrollPanePeer
 * Method:    setSpans
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WScrollPanePeer_setSpans(JNIEnv *env, jobject self,
                                              jint parentWidth,
                                              jint parentHeight,
                                              jint childWidth,
                                              jint childHeight)
{
    TRY;

    SetSpansStruct *sss = new SetSpansStruct;
    sss->scrollpane = env->NewGlobalRef(self);
    sss->parentWidth = parentWidth;
    sss->parentHeight = parentHeight;
    sss->childWidth = childWidth;
    sss->childHeight = childHeight;

    AwtToolkit::GetInstance().SyncCall(AwtScrollPane::_SetSpans, sss);
    // global ref and sss are deleted in _SetSpans

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
