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

#define _JNI_IMPLEMENTATION_

#include "awt.h"
#include "awt_DrawingSurface.h"
#include "awt_Component.h"

jclass jawtVImgClass;
jclass jawtComponentClass;
jfieldID jawtPDataID;
jfieldID jawtSDataID;
jfieldID jawtSMgrID;


/* DSI */

jint JAWTDrawingSurfaceInfo::Init(JAWTDrawingSurface* parent)
{
    TRY;

    JNIEnv* env = parent->env;
    jobject target = parent->target;
    if (JNU_IsNull(env, target)) {
        DTRACE_PRINTLN("NULL target");
        return JAWT_LOCK_ERROR;
    }
    HWND newHwnd = AwtComponent::GetHWnd(env, target);
    if (!::IsWindow(newHwnd)) {
        DTRACE_PRINTLN("Bad HWND");
        return JAWT_LOCK_ERROR;
    }
    jint retval = 0;
    platformInfo = this;
    ds = parent;
    bounds.x = env->GetIntField(target, AwtComponent::xID);
    bounds.y = env->GetIntField(target, AwtComponent::yID);
    bounds.width = env->GetIntField(target, AwtComponent::widthID);
    bounds.height = env->GetIntField(target, AwtComponent::heightID);
    if (hwnd != newHwnd) {
        if (hwnd != NULL) {
            ::ReleaseDC(hwnd, hdc);
            retval = JAWT_LOCK_SURFACE_CHANGED;
        }
        hwnd = newHwnd;
        hdc = ::GetDCEx(hwnd, NULL, DCX_CACHE|DCX_CLIPCHILDREN|DCX_CLIPSIBLINGS);
    }
    clipSize = 1;
    clip = &bounds;
    int screen = AwtWin32GraphicsDevice::DeviceIndexForWindow(hwnd);
    hpalette = AwtWin32GraphicsDevice::GetPalette(screen);

    return retval;

    CATCH_BAD_ALLOC_RET(JAWT_LOCK_ERROR);
}

jint JAWTOffscreenDrawingSurfaceInfo::Init(JAWTOffscreenDrawingSurface* parent)
{
    TRY;

    return JAWT_LOCK_ERROR;

    CATCH_BAD_ALLOC_RET(JAWT_LOCK_ERROR);
}

/* Drawing Surface */

JAWTDrawingSurface::JAWTDrawingSurface(JNIEnv* pEnv, jobject rTarget)
{
    TRY_NO_VERIFY;

    env = pEnv;
    target = env->NewGlobalRef(rTarget);
    Lock = LockSurface;
    GetDrawingSurfaceInfo = GetDSI;
    FreeDrawingSurfaceInfo = FreeDSI;
    Unlock = UnlockSurface;
    info.hwnd = NULL;
    info.hdc = NULL;
    info.hpalette = NULL;

    CATCH_BAD_ALLOC;
}

JAWTDrawingSurface::~JAWTDrawingSurface()
{
    TRY_NO_VERIFY;

    env->DeleteGlobalRef(target);

    CATCH_BAD_ALLOC;
}

JAWT_DrawingSurfaceInfo* JNICALL JAWTDrawingSurface::GetDSI
    (JAWT_DrawingSurface* ds)
{
    TRY;

    if (ds == NULL) {
        DTRACE_PRINTLN("Drawing Surface is NULL");
        return NULL;
    }
    JAWTDrawingSurface* pds = static_cast<JAWTDrawingSurface*>(ds);
    return &(pds->info);

    CATCH_BAD_ALLOC_RET(NULL);
}

void JNICALL JAWTDrawingSurface::FreeDSI
    (JAWT_DrawingSurfaceInfo* dsi)
{
    TRY_NO_VERIFY;

    DASSERTMSG(dsi != NULL, "Drawing Surface Info is NULL\n");

    JAWTDrawingSurfaceInfo* jdsi = static_cast<JAWTDrawingSurfaceInfo*>(dsi);

    ::ReleaseDC(jdsi->hwnd, jdsi->hdc);

    CATCH_BAD_ALLOC;
}

jint JNICALL JAWTDrawingSurface::LockSurface
    (JAWT_DrawingSurface* ds)
{
    TRY;

    if (ds == NULL) {
        DTRACE_PRINTLN("Drawing Surface is NULL");
        return JAWT_LOCK_ERROR;
    }
    JAWTDrawingSurface* pds = static_cast<JAWTDrawingSurface*>(ds);
    jint val = pds->info.Init(pds);
    if ((val & JAWT_LOCK_ERROR) != 0) {
        return val;
    }
    val = AwtComponent::GetDrawState(pds->info.hwnd);
    AwtComponent::SetDrawState(pds->info.hwnd, 0);
    return val;

    CATCH_BAD_ALLOC_RET(JAWT_LOCK_ERROR);
}

void JNICALL JAWTDrawingSurface::UnlockSurface
    (JAWT_DrawingSurface* ds)
{
    TRY_NO_VERIFY;

    if (ds == NULL) {
        DTRACE_PRINTLN("Drawing Surface is NULL");
        return;
    }
    JAWTDrawingSurface* pds = static_cast<JAWTDrawingSurface*>(ds);

    CATCH_BAD_ALLOC;
}

JAWTOffscreenDrawingSurface::JAWTOffscreenDrawingSurface(JNIEnv* pEnv,
                                                         jobject rTarget)
{
    TRY_NO_VERIFY;
    env = pEnv;
    target = env->NewGlobalRef(rTarget);
    Lock = LockSurface;
    GetDrawingSurfaceInfo = GetDSI;
    FreeDrawingSurfaceInfo = FreeDSI;
    Unlock = UnlockSurface;
    info.dxSurface = NULL;
    info.dx7Surface = NULL;

    CATCH_BAD_ALLOC;
}

JAWTOffscreenDrawingSurface::~JAWTOffscreenDrawingSurface()
{
    env->DeleteGlobalRef(target);
}

JAWT_DrawingSurfaceInfo* JNICALL JAWTOffscreenDrawingSurface::GetDSI
    (JAWT_DrawingSurface* ds)
{
    TRY;

    if (ds == NULL) {
        DTRACE_PRINTLN("Drawing Surface is NULL");
        return NULL;
    }
    JAWTOffscreenDrawingSurface* pds =
        static_cast<JAWTOffscreenDrawingSurface*>(ds);
    return &(pds->info);

    CATCH_BAD_ALLOC_RET(NULL);
}

void JNICALL JAWTOffscreenDrawingSurface::FreeDSI
    (JAWT_DrawingSurfaceInfo* dsi)
{
}

jint JNICALL JAWTOffscreenDrawingSurface::LockSurface
    (JAWT_DrawingSurface* ds)
{
    return JAWT_LOCK_ERROR;
}

void JNICALL JAWTOffscreenDrawingSurface::UnlockSurface
    (JAWT_DrawingSurface* ds)
{
}

/* C exports */

extern "C" JNIEXPORT JAWT_DrawingSurface* JNICALL DSGetDrawingSurface
    (JNIEnv* env, jobject target)
{
    TRY;

    // See if the target component is a java.awt.Component
    if (env->IsInstanceOf(target, jawtComponentClass)) {
        return new JAWTDrawingSurface(env, target);
    }

    DTRACE_PRINTLN("GetDrawingSurface target must be a Component");
    return NULL;

    CATCH_BAD_ALLOC_RET(NULL);
}

extern "C" JNIEXPORT void JNICALL DSFreeDrawingSurface
    (JAWT_DrawingSurface* ds)
{
    TRY_NO_VERIFY;

    if (ds == NULL) {
        DTRACE_PRINTLN("Drawing Surface is NULL");
    }
    delete static_cast<JAWTDrawingSurface*>(ds);

    CATCH_BAD_ALLOC;
}

extern "C" JNIEXPORT void JNICALL DSLockAWT(JNIEnv* env)
{
    // Do nothing on Windows
}

extern "C" JNIEXPORT void JNICALL DSUnlockAWT(JNIEnv* env)
{
    // Do nothing on Windows
}

// EmbeddedFrame support

static char *const embeddedClassName = "sun/awt/windows/WEmbeddedFrame";

JNIEXPORT jobject JNICALL awt_CreateEmbeddedFrame
(JNIEnv* env, void* platformInfo)
{
    static jmethodID mid = NULL;
    static jclass cls;
    if (mid == NULL) {
        cls = env->FindClass(embeddedClassName);
        CHECK_NULL_RETURN(cls, NULL);
        mid = env->GetMethodID(cls, "<init>", "(J)V");
        CHECK_NULL_RETURN(mid, NULL);
    }
    return env->NewObject(cls, mid, platformInfo);
}

JNIEXPORT void JNICALL awt_SetBounds
(JNIEnv *env, jobject embeddedFrame, jint x, jint y, jint w, jint h)
{
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass cls = env->FindClass(embeddedClassName);
        CHECK_NULL(cls);
        mid = env->GetMethodID(cls, "setBoundsPrivate", "(IIII)V");
        CHECK_NULL(mid);
    }
    env->CallVoidMethod(embeddedFrame, mid, x, y, w, h);
}

JNIEXPORT void JNICALL awt_SynthesizeWindowActivation
(JNIEnv *env, jobject embeddedFrame, jboolean doActivate)
{
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass cls = env->FindClass(embeddedClassName);
        CHECK_NULL(cls);
        mid = env->GetMethodID(cls, "synthesizeWindowActivation", "(Z)V");
        CHECK_NULL(mid);
    }
    env->CallVoidMethod(embeddedFrame, mid, doActivate);
}
