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
#include "awt_Canvas.h"
#include "awt_Win32GraphicsConfig.h"
#include "awt_Window.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

// Struct for _SetEraseBackground() method
struct SetEraseBackgroundStruct {
    jobject canvas;
    jboolean doErase;
    jboolean doEraseOnResize;
};

/************************************************************************
 * AwtCanvas methods
 */

AwtCanvas::AwtCanvas() {
    m_eraseBackground = JNI_TRUE;
    m_eraseBackgroundOnResize = JNI_TRUE;
}

AwtCanvas::~AwtCanvas() {
}

LPCTSTR AwtCanvas::GetClassName() {
    return TEXT("SunAwtCanvas");
}

/*
 * Create a new AwtCanvas object and window.
 */
AwtCanvas* AwtCanvas::Create(jobject self, jobject hParent)
{
    DASSERT(AwtToolkit::IsMainThread());
    TRY;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    jobject graphicsConfig = NULL;
    jclass canvasClass = NULL;
    jclass win32cls = NULL;

    AwtCanvas *canvas = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtComponent* parent;

        JNI_CHECK_PEER_GOTO(hParent, done);
        parent = (AwtCanvas*)pData;

        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        canvas = new AwtCanvas();

        {
            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);

            canvas->CreateHWnd(env, L"",
                               WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS, 0,
                               x, y, width, height,
                               parent->GetHWnd(),
                               NULL,
                               ::GetSysColor(COLOR_WINDOWTEXT),
                               ::GetSysColor(COLOR_WINDOW),
                               self);

        // Set the pixel format of the HWND if a GraphicsConfiguration
        // was provided to the Canvas constructor.

        canvasClass = env->FindClass("java/awt/Canvas");
        DASSERT(canvasClass != NULL);
        if (!canvasClass) {
            throw std::bad_alloc();
        }

        if ( env->IsInstanceOf( target, canvasClass ) ) {

            // Get GraphicsConfig from our target
            graphicsConfig = env->GetObjectField(target,
                AwtComponent::graphicsConfigID);
            if (graphicsConfig != NULL) {

                win32cls = env->FindClass("sun/awt/Win32GraphicsConfig");
                DASSERT (win32cls != NULL);
                if (!win32cls) {
                    throw std::bad_alloc();
                }

                if ( env->IsInstanceOf( graphicsConfig, win32cls ) ) {
                    // Get the visual ID member from our GC
                    jint visual = env->GetIntField(graphicsConfig,
                          AwtWin32GraphicsConfig::win32GCVisualID);
                    if (visual > 0) {
                        HDC hdc = ::GetDC(canvas->m_hwnd);
                        // Set our pixel format
                        PIXELFORMATDESCRIPTOR pfd;
                        BOOL ret = ::SetPixelFormat(hdc, (int)visual, &pfd);
                        ::ReleaseDC(canvas->m_hwnd, hdc);
                        //Since a GraphicsConfiguration was specified, we should
                        //throw an exception if the PixelFormat couldn't be set.
                        if (ret == FALSE) {
                            DASSERT(!safe_ExceptionOccurred(env));
                            jclass excCls = env->FindClass(
                             "java/lang/RuntimeException");
                            DASSERT(excCls);
                            env->ExceptionClear();
                            env->ThrowNew(excCls,
                             "\nUnable to set Pixel format on Canvas");
                            env->DeleteLocalRef(excCls);
                        }
                    }
                }
            }
        }
    }
    } catch (...) {
        env->DeleteLocalRef(target);
        env->DeleteLocalRef(graphicsConfig);
        env->DeleteLocalRef(canvasClass);
        env->DeleteLocalRef(win32cls);

        env->DeleteGlobalRef(self);
        env->DeleteGlobalRef(hParent);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(graphicsConfig);
    env->DeleteLocalRef(canvasClass);
    env->DeleteLocalRef(win32cls);
    return canvas;
    CATCH_BAD_ALLOC_RET(0);
}

MsgRouting AwtCanvas::WmEraseBkgnd(HDC hDC, BOOL& didErase)
{
    if (m_eraseBackground ||
        (m_eraseBackgroundOnResize && AwtWindow::IsResizing()))
    {
       RECT     rc;
       ::GetClipBox(hDC, &rc);
       ::FillRect(hDC, &rc, this->GetBackgroundBrush());
    }

    didErase = TRUE;
    return mrConsume;
}

/*
 * This routine is duplicated in AwtWindow.
 */
MsgRouting AwtCanvas::WmPaint(HDC)
{
    PaintUpdateRgn(NULL);
    return mrConsume;
}

MsgRouting AwtCanvas::HandleEvent(MSG *msg, BOOL synthetic)
{
    if (IsFocusingMouseMessage(msg)) {
        delete msg;
        return mrConsume;
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

void AwtCanvas::_SetEraseBackground(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetEraseBackgroundStruct *sebs = (SetEraseBackgroundStruct *)param;
    jobject canvas = sebs->canvas;
    jboolean doErase = sebs->doErase;
    jboolean doEraseOnResize = sebs->doEraseOnResize;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(canvas, ret);

    AwtCanvas *c = (AwtCanvas*)pData;
    c->m_eraseBackground = doErase;
    c->m_eraseBackgroundOnResize = doEraseOnResize;

ret:
    env->DeleteGlobalRef(canvas);
    delete sebs;
}


/************************************************************************
 * WCanvasPeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_windows_WCanvasPeer_create(JNIEnv *env, jobject self,
                                        jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtCanvas::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WCanvasPeer
 * Method:    setNativeBackgroundErase
 * Signature: (Z)V
 */
 JNIEXPORT void JNICALL
 Java_sun_awt_windows_WCanvasPeer_setNativeBackgroundErase(JNIEnv *env,
                                                           jobject self,
                                                           jboolean doErase,
                                                           jboolean doEraseOnResize)
{
    TRY;

    SetEraseBackgroundStruct *sebs = new SetEraseBackgroundStruct;
    sebs->canvas = env->NewGlobalRef(self);
    sebs->doErase = doErase;
    sebs->doEraseOnResize = doEraseOnResize;

    AwtToolkit::GetInstance().SyncCall(AwtCanvas::_SetEraseBackground, sebs);
    // sebs and global ref are deleted in _SetEraseBackground()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
