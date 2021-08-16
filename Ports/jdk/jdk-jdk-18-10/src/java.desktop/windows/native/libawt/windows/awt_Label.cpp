/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "awt_Label.h"
#include "awt_Canvas.h"
#include "awt_Win32GraphicsDevice.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// Struct for _SetText() method
struct SetTextStruct {
    jobject label;
    jstring text;
};
// Struct for _SetAlignment() method
struct SetAlignmentStruct {
    jobject label;
    jint alignment;
};
/************************************************************************
 * AwtLabel fields
 */

jfieldID AwtLabel::textID;
jfieldID AwtLabel::alignmentID;


/************************************************************************
 * AwtLabel methods
 */

AwtLabel::AwtLabel() {
    m_needPaint = FALSE;
}

LPCTSTR AwtLabel::GetClassName() {
    return TEXT("SunAwtLabel");
}

/* Create a new AwtLabel object and window. */
AwtLabel* AwtLabel::Create(jobject labelPeer, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtLabel* awtLabel = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtCanvas* awtParent;

        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        target  = env->GetObjectField(labelPeer, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "target", done);

        awtLabel = new AwtLabel();

        {
            DWORD style = WS_CHILD | WS_CLIPSIBLINGS;

            DWORD exStyle = 0;
            if (GetRTLReadingOrder())
                exStyle |= WS_EX_RTLREADING;

            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);
            awtLabel->CreateHWnd(env, L"", style, exStyle,
                                 x, y, width, height,
                                 awtParent->GetHWnd(),
                                 NULL,
                                 ::GetSysColor(COLOR_WINDOWTEXT),
                                 ::GetSysColor(COLOR_BTNFACE),
                                 labelPeer);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    return awtLabel;
}

void AwtLabel::DoPaint(HDC hDC, RECT& r)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    if ((r.right-r.left) > 0 && (r.bottom-r.top) > 0 &&
        m_peerObject != NULL && m_callbacksEnabled) {

        if (env->EnsureLocalCapacity(3) < 0)
            return;
        long x,y;
        SIZE size;

        /* self is sun.awt.windows.WLabelPeer  */

        jobject self = GetPeer(env);
        DASSERT(self);

        /* target is java.awt.Label */
        jobject target = env->GetObjectField(self, AwtObject::targetID);
        jobject font = GET_FONT(target, self);
        jstring text = (jstring)env->GetObjectField(target, AwtLabel::textID);

        size = AwtFont::getMFStringSize(hDC, font, text);
        ::SetTextColor(hDC, GetColor());
        /* Redraw whole label to eliminate display noise during resizing. */
        VERIFY(::GetClientRect(GetHWnd(), &r));
        VERIFY(::FillRect (hDC, &r, GetBackgroundBrush()));
        y = (r.top + r.bottom - size.cy) / 2;

        jint alignment = env->GetIntField(target, AwtLabel::alignmentID);
        switch (alignment) {
          case java_awt_Label_CENTER:
              x = (r.left + r.right - size.cx) / 2;
              break;
          case java_awt_Label_RIGHT:
              x = r.right - 2 - size.cx;
              break;
          case java_awt_Label_LEFT:
          default:
              x = r.left + 2;
              break;
        }
        /* draw string */
        if (isEnabled()) {
            AwtComponent::DrawWindowText(hDC, font, text, x, y);
        } else {
            AwtComponent::DrawGrayText(hDC, font, text, x, y);
        }
        DoCallback("handlePaint", "(IIII)V",
                   r.left, r.top, r.right-r.left, r.bottom-r.top);
        env->DeleteLocalRef(target);
        env->DeleteLocalRef(font);
        env->DeleteLocalRef(text);
    }
}

void AwtLabel::LazyPaint()
{
    if (m_callbacksEnabled && m_needPaint ) {
        ::InvalidateRect(GetHWnd(), NULL, TRUE);
        m_needPaint = FALSE;
    }
}

void AwtLabel::Enable(BOOL bEnable)
{
    ::EnableWindow(GetHWnd(), bEnable);
    // Fix for Bug #4038881 Labels don't enable and disable properly
    // Fix for Bug #4096745 disable()/enable() make AWT components blink
    // This fix is moved from awt_Component.cpp for Bug #4096745
    ::InvalidateRect(GetHWnd(), NULL, FALSE);
    CriticalSection::Lock l(GetLock());
    VerifyState();
}


MsgRouting AwtLabel::WmEraseBkgnd(HDC hDC, BOOL& didErase)
{
    RECT r;

    ::GetClipBox(hDC, &r);
    ::FillRect(hDC, &r, this->GetBackgroundBrush());
    didErase = TRUE;
    return mrConsume;
}

MsgRouting AwtLabel::WmPaint(HDC)
{
    PAINTSTRUCT ps;
    HDC hDC = ::BeginPaint(GetHWnd(), &ps);/* the passed-in HDC is ignored. */
    DASSERT(hDC);

    /* fix for 4408606 - incorrect color palette used in 256 color mode */

    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(GetHWnd());
    AwtWin32GraphicsDevice::SelectPalette(hDC, screen);

    RECT& r = ps.rcPaint;
    if (!m_callbacksEnabled) {
        m_needPaint = TRUE;
    } else {
        DoPaint(hDC, r);
    }
    VERIFY(::EndPaint(GetHWnd(), &ps));
    return mrConsume;
}

MsgRouting AwtLabel::WmPrintClient(HDC hDC, LPARAM)
{
    RECT r;

    // obtain valid DC from GDI stack
    ::RestoreDC(hDC, -1);

    ::GetClipBox(hDC, &r);
    DoPaint(hDC, r);
    return mrConsume;
}

void AwtLabel::_SetText(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetTextStruct *sts = (SetTextStruct *)param;
    jobject self = sts->label;
    jstring text = sts->text;

    AwtLabel *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtLabel *)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->SetText(JavaStringBuffer(env, text));
        VERIFY(::InvalidateRect(l->GetHWnd(), NULL, TRUE));
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(text);

    delete sts;
}

void AwtLabel::_SetAlignment(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetAlignmentStruct *sas = (SetAlignmentStruct *)param;
    jobject self = sas->label;
    jint alignment = sas->alignment;

    AwtLabel *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtLabel *)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        /*
         * alignment argument of multifont label is referred to in
         * WmDrawItem method
         */

        VERIFY(::InvalidateRect(l->GetHWnd(), NULL, TRUE));
    }
ret:
    env->DeleteGlobalRef(self);

    delete sas;
}

void AwtLabel::_LazyPaint(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtLabel *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtLabel *)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->LazyPaint();
    }
ret:
    env->DeleteGlobalRef(self);
}


/************************************************************************
 * Label native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Label_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    /* init field ids */
    AwtLabel::textID = env->GetFieldID(cls, "text", "Ljava/lang/String;");
    DASSERT(AwtLabel::textID != NULL);
    CHECK_NULL(AwtLabel::textID);

    AwtLabel::alignmentID = env->GetFieldID(cls, "alignment", "I");
    DASSERT(AwtLabel::alignmentID != NULL);
    CHECK_NULL(AwtLabel::alignmentID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WLabelPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WLabelPeer
 * Method:    setText
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WLabelPeer_setText(JNIEnv *env, jobject self,
                                        jstring text)
{
    TRY;

    SetTextStruct *sts = new SetTextStruct;
    sts->label = env->NewGlobalRef(self);
    sts->text = (jstring)env->NewGlobalRef(text);

    AwtToolkit::GetInstance().SyncCall(AwtLabel::_SetText, sts);
    // global refs and sts are deleted in _SetText()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WLabelPeer
 * Method:    setAlignment
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WLabelPeer_setAlignment(JNIEnv *env, jobject self,
                                             jint alignment)
{
    TRY;

    SetAlignmentStruct *sas = new SetAlignmentStruct;
    sas->label = env->NewGlobalRef(self);
    sas->alignment = alignment;

    AwtToolkit::GetInstance().SyncCall(AwtLabel::_SetAlignment, sas);
    // global ref and sas are deleted in _SetAlignment

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WLabelPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WLabelPeer_create(JNIEnv *env, jobject self,
                                       jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtLabel::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WLabelPeer
 * Method:    lazyPaint
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WLabelPeer_lazyPaint(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtLabel::_LazyPaint, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _LazyPaint

    CATCH_BAD_ALLOC;
}

} /* export "C" */
