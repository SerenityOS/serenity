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
#include "awt_Toolkit.h"
#include "awt_Checkbox.h"
#include "awt_Canvas.h"
#include "awt_Window.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// Struct for _SetLabel() method
struct SetLabelStruct {
    jobject checkbox;
    jstring label;
};
// Struct for _SetState() method
struct SetStateStruct {
    jobject checkbox;
    jboolean state;
};

/************************************************************************
 * AwtCheckbox fields
 */

/* java.awt.Checkbox field IDs */
jfieldID AwtCheckbox::labelID;
jfieldID AwtCheckbox::groupID;
jfieldID AwtCheckbox::stateID;

const int AwtCheckbox::CHECK_SIZE = 13;

/************************************************************************
 * AwtCheckbox methods
 */

AwtCheckbox::AwtCheckbox() {

    m_fLButtonDowned = FALSE;
}

LPCTSTR AwtCheckbox::GetClassName() {
    return TEXT("BUTTON");  /* System provided checkbox class (a type of button) */
}

AwtCheckbox* AwtCheckbox::Create(jobject peer, jobject parent)
{
    DASSERT(AwtToolkit::IsMainThread());
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jstring label = NULL;
    jobject target = NULL;
    AwtCheckbox *checkbox = NULL;

    try {
        if (env->EnsureLocalCapacity(2) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtComponent* awtParent;
        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        target = env->GetObjectField(peer, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        checkbox = new AwtCheckbox();

        {
            DWORD style = WS_CHILD | WS_CLIPSIBLINGS | BS_OWNERDRAW;
            LPCWSTR defaultLabelStr = L"";
            LPCWSTR labelStr = defaultLabelStr;
            DWORD exStyle = 0;

            if (GetRTL()) {
                exStyle |= WS_EX_RIGHT;
                if (GetRTLReadingOrder())
                    exStyle |= WS_EX_RTLREADING;
            }

            label = (jstring)env->GetObjectField(target, AwtCheckbox::labelID);
            if (label != NULL) {
                labelStr = JNU_GetStringPlatformChars(env, label, 0);
            }
            if (labelStr != 0) {
                jint x = env->GetIntField(target, AwtComponent::xID);
                jint y = env->GetIntField(target, AwtComponent::yID);
                jint width = env->GetIntField(target, AwtComponent::widthID);
                jint height = env->GetIntField(target, AwtComponent::heightID);
                checkbox->CreateHWnd(env, labelStr, style, exStyle,
                                     x, y, width, height,
                                     awtParent->GetHWnd(),
                                     reinterpret_cast<HMENU>(static_cast<INT_PTR>(
                         awtParent->CreateControlID())),
                                     ::GetSysColor(COLOR_WINDOWTEXT),
                                     ::GetSysColor(COLOR_BTNFACE),
                                     peer);

                if (labelStr != defaultLabelStr) {
                    JNU_ReleaseStringPlatformChars(env, label, labelStr);
                }
            } else {
                throw std::bad_alloc();
            }
        }
    } catch (...) {
        env->DeleteLocalRef(label);
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(label);
    env->DeleteLocalRef(target);

    return checkbox;
}

MsgRouting
AwtCheckbox::WmMouseUp(UINT flags, int x, int y, int button)
{
    MsgRouting mrResult = AwtComponent::WmMouseUp(flags, x, y, button);

    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd()))))
    {
        return mrConsume;
    }

    POINT p = {x, y};
    RECT rect;
    ::GetClientRect(GetHWnd(), &rect);

    if (::PtInRect(&rect, p) && button == LEFT_BUTTON && m_fLButtonDowned) {
        WmNotify(BN_CLICKED);
    }
    m_fLButtonDowned = FALSE;
    return mrResult;
}

MsgRouting
AwtCheckbox::WmMouseDown(UINT flags, int x, int y, int button)
{
    m_fLButtonDowned = TRUE;
    return AwtComponent::WmMouseDown(flags, x, y, button);
}

MsgRouting
AwtCheckbox::WmNotify(UINT notifyCode)
{
    if (notifyCode == BN_CLICKED) {
        BOOL fChecked = !GetState();
        DoCallback("handleAction", "(Z)V", fChecked);
    }
    return mrDoDefault;
}

BOOL AwtCheckbox::GetState()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->EnsureLocalCapacity(2) < 0) {
        return NULL;
    }
    jobject target = GetTarget(env);
    jboolean result = JNI_FALSE;
    if (target != NULL) {
        result = env->GetBooleanField(target, AwtCheckbox::stateID);
    }

    env->DeleteLocalRef(target);

    return (BOOL)result;
}

int AwtCheckbox::GetCheckSize()
{
    /* using height of small icon for check mark size */
    return CHECK_SIZE;
}

MsgRouting
AwtCheckbox::OwnerDrawItem(UINT /*ctrlId*/, DRAWITEMSTRUCT& drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->EnsureLocalCapacity(4) < 0) {
        return mrConsume;
    }

    jobject self = GetPeer(env);
    jobject target = env->GetObjectField(self, AwtObject::targetID);

    HDC hDC = drawInfo.hDC;
    RECT rect = drawInfo.rcItem;
    int checkSize;
    UINT nState;
    SIZE size;

    jobject font = GET_FONT(target, self);
    jstring str = (jstring)env->GetObjectField(target, AwtCheckbox::labelID);
    size = AwtFont::getMFStringSize(hDC, font, str);

    jobject group = env->GetObjectField(target, AwtCheckbox::groupID);
    if (group != NULL)
        nState = DFCS_BUTTONRADIO;
    else
        nState = DFCS_BUTTONCHECK;

    if (GetState())
        nState |= DFCS_CHECKED;
    else
        nState &= ~DFCS_CHECKED;

    if (drawInfo.itemState & ODS_SELECTED)
        nState |= DFCS_PUSHED;

    if (drawInfo.itemAction & ODA_DRAWENTIRE) {
        VERIFY(::FillRect (hDC, &rect, GetBackgroundBrush()));
    }

    /* draw check mark */
    checkSize = GetCheckSize();
    RECT boxRect;

    boxRect.left = (GetRTL()) ? rect.right - checkSize : rect.left;
    boxRect.top = (rect.bottom - rect.top - checkSize)/2;
    boxRect.right = boxRect.left + checkSize;
    boxRect.bottom = boxRect.top + checkSize;
    ::DrawFrameControl(hDC, &boxRect, DFC_BUTTON, nState);

    /*
     * draw string
     *
     * 4 is a heuristic number
     */
    rect.left = rect.left + checkSize + checkSize/4;
    if (drawInfo.itemAction & ODA_DRAWENTIRE) {
        BOOL bEnabled = isEnabled();

        int x = (GetRTL()) ? rect.right - (checkSize + checkSize / 4 + size.cx)
                           : rect.left;
        int y = (rect.top + rect.bottom - size.cy) / 2;
        if (bEnabled) {
            AwtComponent::DrawWindowText(hDC, font, str, x, y);
        } else {
            AwtComponent::DrawGrayText(hDC, font, str, x, y);
        }
    }

    /* Draw focus rect */
    RECT focusRect;
    const int margin = 2; /*  2 is a heuristic number */

    focusRect.left = (GetRTL()) ? rect.right - checkSize - checkSize / 4 -
                                      2 * margin - size.cx
                                : rect.left - margin;
    focusRect.top = (rect.top+rect.bottom-size.cy)/2;
    focusRect.right = (GetRTL()) ? rect.right - checkSize - checkSize / 4 +
                                      margin
                                 : focusRect.left + size.cx + 2 * margin;
    focusRect.bottom = focusRect.top + size.cy;

    /*  draw focus rect */
    if ((drawInfo.itemState & ODS_FOCUS) &&
        ((drawInfo.itemAction & ODA_FOCUS)||
         (drawInfo.itemAction &ODA_DRAWENTIRE))) {
        if(::DrawFocusRect(hDC, &focusRect) == 0)
            VERIFY(::GetLastError() == 0);
    }
    /*  erase focus rect */
    else if (!(drawInfo.itemState & ODS_FOCUS) &&
             (drawInfo.itemAction & ODA_FOCUS)) {
        if(::DrawFocusRect(hDC, &focusRect) == 0)
            VERIFY(::GetLastError() == 0);
    }

    /*  Notify any subclasses */
    rect = drawInfo.rcItem;
    DoCallback("handlePaint", "(IIII)V", rect.left, rect.top,
               rect.right-rect.left, rect.bottom-rect.top);

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(str);
    env->DeleteLocalRef(group);

    return mrConsume;
}

MsgRouting AwtCheckbox::WmPaint(HDC)
{
    /*  Suppress peer notification, because it's handled in WmDrawItem. */
    return mrDoDefault;
}

BOOL AwtCheckbox::IsFocusingMouseMessage(MSG *pMsg) {
    return pMsg->message == WM_LBUTTONDOWN || pMsg->message == WM_LBUTTONUP;
}

BOOL AwtCheckbox::IsFocusingKeyMessage(MSG *pMsg) {
    return (pMsg->message == WM_KEYDOWN || pMsg->message == WM_KEYUP) &&
            pMsg->wParam == VK_SPACE;
}

MsgRouting AwtCheckbox::HandleEvent(MSG *msg, BOOL synthetic)
{
    if (IsFocusingMouseMessage(msg)) {
        SendMessage(BM_SETSTATE, (WPARAM)(msg->message == WM_LBUTTONDOWN ? TRUE : FALSE));
        delete msg;
        return mrConsume;
    }
    if (IsFocusingKeyMessage(msg)) {
        SendMessage(BM_SETSTATE, (WPARAM)(msg->message == WM_KEYDOWN ? TRUE : FALSE));
        if (msg->message == WM_KEYDOWN) {
            m_fLButtonDowned = TRUE;
        } else if (m_fLButtonDowned == TRUE) {
            WmNotify(BN_CLICKED);
            m_fLButtonDowned = TRUE;
        }
        delete msg;
        return mrConsume;
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

void AwtCheckbox::_SetLabel(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetLabelStruct *sls = (SetLabelStruct *)param;
    jobject checkbox = sls->checkbox;
    jstring label = sls->label;

    int badAlloc = 0;
    AwtCheckbox *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(checkbox, done);

    c = (AwtCheckbox *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        LPCTSTR labelStr = NULL;

        // By convension null label means empty string
        if (label == NULL)
        {
            labelStr = TEXT("");
        }
        else
        {
            labelStr = JNU_GetStringPlatformChars(env, label, JNI_FALSE);
        }

        if (labelStr == NULL)
        {
            badAlloc = 1;
        }
        else
        {
            c->SetText(labelStr);
            c->VerifyState();
            if (label != NULL) {
                JNU_ReleaseStringPlatformChars(env, label, labelStr);
            }
        }
    }

done:
    env->DeleteGlobalRef(checkbox);
    if (label != NULL)
    {
        env->DeleteGlobalRef(label);
    }

    delete sls;

    if (badAlloc) {
        throw std::bad_alloc();
    }
}

void AwtCheckbox::_SetCheckboxGroup(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject *jos = (jobject *)param;
    jobject checkbox = jos[0];
    jobject group = jos[1];

    AwtCheckbox *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(checkbox, done);

    c = (AwtCheckbox *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
/*
#ifdef DEBUG
        if (group != NULL) {
            DASSERT(IsInstanceOf((HObject*)group, "java/awt/CheckboxGroup"));
        }
#endif
*/
        long style = c->GetStyle();
        if (group == NULL) {
            style = style & ~BS_AUTORADIOBUTTON;
            style = style | BS_AUTOCHECKBOX;
        } else {
            style = style & ~BS_AUTOCHECKBOX;
            style = style | BS_AUTORADIOBUTTON;
        }
        c->SetStyle(style);
        c->SendMessage(BM_SETSTYLE, (WPARAM)BS_OWNERDRAW, (LPARAM)TRUE);
        c->VerifyState();
    }

done:
    env->DeleteGlobalRef(checkbox);
    if (group != NULL) {
      env->DeleteGlobalRef(group);
    }

    delete[] jos;
}

void AwtCheckbox::_SetState(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetStateStruct *sss = (SetStateStruct *)param;
    jobject checkbox = sss->checkbox;
    jboolean state = sss->state;

    AwtCheckbox *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(checkbox, done);

    c = (AwtCheckbox *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        /*
         * when multifont and group checkbox receive setState native
         * method, it must be redraw to display correct check mark
         */
        jobject target = env->GetObjectField(checkbox, AwtObject::targetID);
        jobject group = env->GetObjectField(target, AwtCheckbox::groupID);
        HWND hWnd = c->GetHWnd();
        if (group != NULL) {
            RECT rect;
            VERIFY(::GetWindowRect(hWnd, &rect));
            VERIFY(::ScreenToClient(hWnd, (LPPOINT)&rect));
            VERIFY(::ScreenToClient(hWnd, ((LPPOINT)&rect) + 1));
            VERIFY(::InvalidateRect(hWnd, &rect,TRUE));
            VERIFY(::UpdateWindow(hWnd));
        } else {
            c->SendMessage(BM_SETCHECK, (WPARAM)(state ? BST_CHECKED : BST_UNCHECKED));
            VERIFY(::InvalidateRect(hWnd, NULL, FALSE));
        }
        c->VerifyState();
        env->DeleteLocalRef(target);
        env->DeleteLocalRef(group);
    }

done:
    env->DeleteGlobalRef(checkbox);

    delete sss;
}

#ifdef DEBUG
void AwtCheckbox::VerifyState()
{
    if (AwtToolkit::GetInstance().VerifyComponents() == FALSE) {
        return;
    }

    if (m_callbacksEnabled == FALSE) {
        /*  Component is not fully setup yet. */
        return;
    }

    AwtComponent::VerifyState();
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }

    jobject target = GetTarget(env);

    /*  Check button style */
    DWORD style = ::GetWindowLong(GetHWnd(), GWL_STYLE);
    DASSERT(style & BS_OWNERDRAW);

    /*  Check label */
    int len = ::GetWindowTextLength(GetHWnd());
    LPTSTR peerStr;
    try {
        peerStr = new TCHAR[len+1];
    } catch (std::bad_alloc&) {
        env->DeleteLocalRef(target);
        throw;
    }

    GetText(peerStr, len+1);
    jstring label = (jstring)env->GetObjectField(target, AwtCheckbox::labelID);
    DASSERT(_tcscmp(peerStr, JavaStringBuffer(env, label)) == 0);
    delete [] peerStr;

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(label);
}
#endif


/************************************************************************
 * Checkbox native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WButtonPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_Checkbox_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtCheckbox::labelID =
      env->GetFieldID(cls, "label", "Ljava/lang/String;");
    DASSERT(AwtCheckbox::labelID != NULL);
    CHECK_NULL(AwtCheckbox::labelID);

    AwtCheckbox::groupID =
      env->GetFieldID(cls, "group", "Ljava/awt/CheckboxGroup;");
    DASSERT(AwtCheckbox::groupID != NULL);
    CHECK_NULL(AwtCheckbox::groupID);

    AwtCheckbox::stateID = env->GetFieldID(cls, "state", "Z");
    DASSERT(AwtCheckbox::stateID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WCheckboxPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WCheckboxPeer
 * Method:    getCheckMarkSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WCheckboxPeer_getCheckMarkSize(JNIEnv *env,
                                                          jclass cls)
{
    return (jint)AwtCheckbox::GetCheckSize();
}

/*
 * Class:     sun_awt_windows_WCheckboxPeer
 * Method:    setState
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WCheckboxPeer_setState(JNIEnv *env, jobject self,
                                            jboolean state)
{
    TRY;

    SetStateStruct *sss = new SetStateStruct;
    sss->checkbox = env->NewGlobalRef(self);
    sss->state = state;

    AwtToolkit::GetInstance().SyncCall(AwtCheckbox::_SetState, sss);
    // global refs and sss are deleted in _SetState()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WCheckboxPeer
 * Method:    setCheckboxGroup
 * Signature: (Ljava/awt/CheckboxGroup;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WCheckboxPeer_setCheckboxGroup(JNIEnv *env, jobject self,
                                                    jobject group)
{
    TRY;

    jobject *jos = new jobject[2];
    jos[0] = env->NewGlobalRef(self);
    jos[1] = env->NewGlobalRef(group);

    AwtToolkit::GetInstance().SyncCall(AwtCheckbox::_SetCheckboxGroup, jos);
    // global refs and jos are deleted in _SetLabel()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WCheckboxPeer
 * Method:    setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WCheckboxPeer_setLabel(JNIEnv *env, jobject self,
                                            jstring label)
{
    TRY;

    SetLabelStruct *sls = new SetLabelStruct;
    sls->checkbox = env->NewGlobalRef(self);
    sls->label = (label != NULL) ? (jstring)env->NewGlobalRef(label) : NULL;

    AwtToolkit::GetInstance().SyncCall(AwtCheckbox::_SetLabel, sls);
    // global refs and sls are deleted in _SetLabel()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WCheckboxPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WCheckboxPeer_create(JNIEnv *env, jobject self,
                                          jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtCheckbox::Create);
    PDATA pData;
    JNI_CHECK_PEER_CREATION_RETURN(self);

#ifdef DEBUG
    ((AwtComponent*)JNI_GET_PDATA(self))->VerifyState();
#endif

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
