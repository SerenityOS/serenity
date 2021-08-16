/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"

#include "awt_Object.h"    /* wop_pDataID */
#include "awt_Toolkit.h"
#include "awt_Button.h"
#include "awt_Canvas.h"
#include "awt_Window.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// Struct for _SetLabel() method
struct SetLabelStruct {
  jobject button;
  jstring label;
};

/************************************************************************
 * AwtButton fields
 */

/* java.awt.Button fields */
jfieldID AwtButton::labelID;


/************************************************************************
 * AwtButton methods
 */

AwtButton::AwtButton() {
    leftButtonDown = FALSE;
}

/* System provided button class */
LPCTSTR AwtButton::GetClassName() {
    return TEXT("BUTTON");
}

/* Create a new AwtButton object and window. */
AwtButton* AwtButton::Create(jobject self, jobject parent)
{
    DASSERT(AwtToolkit::IsMainThread());
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    /* the result */
    AwtButton *c = NULL;

    jobject target = NULL;
    jstring label = NULL;

    try {
        LPCWSTR labelStr;
        DWORD style;
        DWORD exStyle = 0;
        jint x, y, height, width;

        if (env->EnsureLocalCapacity(2) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtCanvas* awtParent;

        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "target", done);

        c = new AwtButton();

        label = (jstring)env->GetObjectField(target, AwtButton::labelID);

        x = env->GetIntField(target, AwtComponent::xID);
        y = env->GetIntField(target, AwtComponent::yID);
        width = env->GetIntField(target, AwtComponent::widthID);
        height = env->GetIntField(target, AwtComponent::heightID);

        if (label == NULL) {
            labelStr = L"";
        } else {
            labelStr = JNU_GetStringPlatformChars(env, label, JNI_FALSE);
        }
        style = 0;

        if (labelStr == NULL) {
            throw std::bad_alloc();
        }

        style = WS_CHILD | WS_CLIPSIBLINGS | BS_PUSHBUTTON | BS_OWNERDRAW;
        if (GetRTLReadingOrder())
            exStyle |= WS_EX_RTLREADING;

        c->CreateHWnd(env, labelStr, style, exStyle, x, y, width, height,
                      awtParent->GetHWnd(),
                      reinterpret_cast<HMENU>(static_cast<INT_PTR>(
                  awtParent->CreateControlID())),
                      ::GetSysColor(COLOR_BTNTEXT),
                      ::GetSysColor(COLOR_BTNFACE),
                      self);
        c->m_backgroundColorSet = TRUE;  // suppress inheriting parent's color
        c->UpdateBackground(env, target);
        if (label != NULL)
            JNU_ReleaseStringPlatformChars(env, label, labelStr);
    } catch (...) {
        env->DeleteLocalRef(target);
        if (label != NULL)
            env->DeleteLocalRef(label);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    if (label != NULL)
        env->DeleteLocalRef(label);
    return c;
}

MsgRouting
AwtButton::WmMouseDown(UINT flags, int x, int y, int button)
{
    // 4530087: keep track of the when the left mouse button is pressed
    if (button == LEFT_BUTTON) {
        leftButtonDown = TRUE;
    }
    return AwtComponent::WmMouseDown(flags, x, y, button);
}

MsgRouting
AwtButton::WmMouseUp(UINT flags, int x, int y, int button)
{
    MsgRouting mrResult = AwtComponent::WmMouseUp(flags, x, y, button);

    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd()))))
    {
        return mrConsume;
    }

    // 4530087: It is possible that a left mouse press happened on a Window
    // obscuring this AwtButton, and during event handling the Window was
    // removed.  This causes a WmMouseUp call to this AwtButton, even though
    // there was no accompanying WmMouseDown.  ActionEvents should ONLY be
    // notified (via NotifyListeners()) if the left button press happened on
    // this AwtButton.  --bchristi
    if (button == LEFT_BUTTON && leftButtonDown) {
        leftButtonDown = FALSE;

        POINT p = {x, y};
        RECT rect;
        ::GetClientRect(GetHWnd(), &rect);

        if (::PtInRect(&rect, p)) {
            NotifyListeners();
        }
    }

    return mrResult;
}

void
AwtButton::NotifyListeners()
{
    DoCallback("handleAction", "(JI)V", ::JVM_CurrentTimeMillis(NULL, 0),
               (jint)AwtComponent::GetActionModifiers());
}

MsgRouting
AwtButton::OwnerDrawItem(UINT /*ctrlId*/, DRAWITEMSTRUCT& drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->EnsureLocalCapacity(3) < 0) {
        /* is this OK? */
        return mrConsume;
    }

    jobject self = GetPeer(env);
    jobject target = env->GetObjectField(self, AwtObject::targetID);

    HDC hDC = drawInfo.hDC;
    RECT rect = drawInfo.rcItem;
    UINT nState;
    SIZE size;

    /* Draw Button */
    nState = DFCS_BUTTONPUSH;
    if (drawInfo.itemState & ODS_SELECTED)
        nState |= DFCS_PUSHED;

    ::FillRect(hDC, &rect, GetBackgroundBrush());
    UINT edgeType = (nState & DFCS_PUSHED) ? EDGE_SUNKEN : EDGE_RAISED;
    ::DrawEdge(hDC, &rect, edgeType, BF_RECT | BF_SOFT);

    /* Draw WindowText */
    jobject font = GET_FONT(target, self);
    jstring str = (jstring)env->GetObjectField(target, AwtButton::labelID);

    size = AwtFont::getMFStringSize(hDC, font, str);

    /* Check whether the button is disabled. */
    BOOL bEnabled = isEnabled();

    int adjust = (nState & DFCS_PUSHED) ? 1 : 0;
    int x = (rect.left + rect.right-size.cx) / 2 + adjust;
    int y = (rect.top + rect.bottom-size.cy) / 2 + adjust;

    if (bEnabled) {
        AwtComponent::DrawWindowText(hDC, font, str, x, y);
    } else {
        AwtComponent::DrawGrayText(hDC, font, str, x, y);
    }

    /* Draw focus rect */
    if (drawInfo.itemState & ODS_FOCUS){
        const int inf = 3; /* heuristic decision */
        RECT focusRect;
        VERIFY(::CopyRect(&focusRect, &rect));
        VERIFY(::InflateRect(&focusRect,-inf,-inf));
        if(::DrawFocusRect(hDC, &focusRect) == 0)
            VERIFY(::GetLastError() == 0);
    }

    /* Notify any subclasses */
    DoCallback("handlePaint", "(IIII)V", rect.left, rect.top,
               rect.right-rect.left, rect.bottom-rect.top);

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(str);

    return mrConsume;
}

MsgRouting AwtButton::WmPaint(HDC)
{
    /* Suppress peer notification, because it's handled in WmDrawItem. */
    return mrDoDefault;
}

BOOL AwtButton::IsFocusingMouseMessage(MSG *pMsg) {
    return pMsg->message == WM_LBUTTONDOWN || pMsg->message == WM_LBUTTONUP;
}

BOOL AwtButton::IsFocusingKeyMessage(MSG *pMsg) {
    return (pMsg->message == WM_KEYDOWN || pMsg->message == WM_KEYUP) &&
            pMsg->wParam == VK_SPACE;
}

MsgRouting AwtButton::HandleEvent(MSG *msg, BOOL synthetic)
{
    if (IsFocusingMouseMessage(msg)) {
        SendMessage(BM_SETSTATE, msg->message == WM_LBUTTONDOWN ? TRUE : FALSE, 0);
        delete msg;
        return mrConsume;
    }
    if (IsFocusingKeyMessage(msg)) {
        SendMessage(BM_SETSTATE, msg->message == WM_KEYDOWN ? TRUE : FALSE, 0);
        delete msg;
        return mrConsume;
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

void AwtButton::_SetLabel(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetLabelStruct *sls = (SetLabelStruct *)param;

    jobject button = sls->button;
    jstring label = sls->label;

    int badAlloc = 0;
    AwtComponent *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(button, done);

    c = (AwtComponent*)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        LPCTSTR labelStr = NULL;

        // By convension null label means empty string
        if (label == NULL) {
            labelStr = TEXT("");
        } else {
            labelStr = JNU_GetStringPlatformChars(env, label, JNI_FALSE);
        }

        if (labelStr == NULL) {
            badAlloc = 1;
        } else {
            c->SetText(labelStr);
            if (label != NULL) {
                JNU_ReleaseStringPlatformChars(env, label, labelStr);
            }
        }
    }

done:
    env->DeleteGlobalRef(button);
    if (label != NULL)
    {
        env->DeleteGlobalRef(label);
    }

    delete sls;

    if (badAlloc) {
        throw std::bad_alloc();
    }
}

/************************************************************************
 * WButtonPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WButtonPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WButtonPeer_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    cls = env->FindClass("java/awt/Button");
    if (cls == NULL) {
        return;
    }
    AwtButton::labelID = env->GetFieldID(cls, "label", "Ljava/lang/String;");
    DASSERT(AwtButton::labelID != NULL);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WButtonPeer
 * Method:    setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WButtonPeer_setLabel(JNIEnv *env, jobject self,
                                          jstring label)
{
    TRY;

    SetLabelStruct *sls = new SetLabelStruct;
    sls->button = env->NewGlobalRef(self);
    sls->label = (label != NULL) ? (jstring)env->NewGlobalRef(label) : NULL;

    AwtToolkit::GetInstance().SyncCall(AwtButton::_SetLabel, sls);
    // global refs and sls are deleted in _SetLabel()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WButtonPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WButtonPeer_create(JNIEnv *env, jobject self,
                                        jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(
        self, parent, (AwtToolkit::ComponentFactory)AwtButton::Create);

    CATCH_BAD_ALLOC;
}

}  /* extern "C" */
