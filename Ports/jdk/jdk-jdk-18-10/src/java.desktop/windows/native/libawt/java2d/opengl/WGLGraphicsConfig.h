/*
 * Copyright (c) 2004, 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef WGLGraphicsConfig_h_Included
#define WGLGraphicsConfig_h_Included

#include "jni.h"
#include "J2D_GL/gl.h"
#include "OGLSurfaceData.h"
#include "OGLContext.h"

/**
 * The WGLGraphicsConfigInfo structure contains information specific to a
 * given WGLGraphicsConfig (pixel format).
 *
 *     jint screen, pixfmt;
 * The screen and PixelFormat for the associated WGLGraphicsConfig.
 *
 *     OGLContext *context;
 * The context associated with this WGLGraphicsConfig.
 */
typedef struct _WGLGraphicsConfigInfo {
    jint       screen;
    jint       pixfmt;
    OGLContext *context;
} WGLGraphicsConfigInfo;

/**
 * The WGLCtxInfo structure contains the native WGLContext information
 * required by and is encapsulated by the platform-independent OGLContext
 * structure.
 *
 *     HGLRC context;
 * The core native WGL context.  Rendering commands have no effect until a
 * context is made current (active).
 *
 *     HPBUFFERARB scratchSurface;
 *     HDC         scratchSurfaceDC;
 * The scratch surface (and its associated HDC), which are used to make a
 * context current when we do not otherwise have a reference to an OpenGL
 * surface for the purposes of making a context current.
 */
typedef struct _WGLCtxInfo {
    HGLRC       context;
    HPBUFFERARB scratchSurface;
    HDC         scratchSurfaceDC;
} WGLCtxInfo;

/**
 * Utility methods
 */
HWND WGLGC_CreateScratchWindow(jint screennum);

/**
 * REMIND: ideally, this would be declared in AwtComponent.h, but including
 *         that C++ header file from C source files causes problems...
 */
extern HWND AwtComponent_GetHWnd(JNIEnv *env, jlong pData);

#endif /* WGLGraphicsConfig_h_Included */
