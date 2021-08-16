/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OGLFuncs_md_h_Included
#define OGLFuncs_md_h_Included

#include <windows.h>
#include "J2D_GL/wglext.h"
#include "OGLFuncMacros.h"
#include <jdk_util.h>

/**
 * Core WGL functions
 */
typedef HGLRC (GLAPIENTRY *wglCreateContextType)(HDC hdc);
typedef BOOL  (GLAPIENTRY *wglDeleteContextType)(HGLRC hglrc);
typedef BOOL  (GLAPIENTRY *wglMakeCurrentType)(HDC hdc, HGLRC hglrc);
typedef HGLRC (GLAPIENTRY *wglGetCurrentContextType)();
typedef HDC   (GLAPIENTRY *wglGetCurrentDCType)();
typedef PROC  (GLAPIENTRY *wglGetProcAddressType)(LPCSTR procName);
typedef BOOL  (GLAPIENTRY *wglShareListsType)(HGLRC hglrc1, HGLRC hglrc2);

/**
 * WGL extension function pointers
 */
typedef BOOL (GLAPIENTRY *wglChoosePixelFormatARBType)(HDC hdc, const int *pAttribIList, const FLOAT *pAttribFList, UINT nMaxFormats, int *piFormats, UINT *nNumFormats);
typedef BOOL (GLAPIENTRY *wglGetPixelFormatAttribivARBType)(HDC, int, int, UINT, const int *, int *);
typedef HPBUFFERARB (GLAPIENTRY *wglCreatePbufferARBType)(HDC, int, int, int, const int *);
typedef HDC  (GLAPIENTRY *wglGetPbufferDCARBType)(HPBUFFERARB);
typedef int  (GLAPIENTRY *wglReleasePbufferDCARBType)(HPBUFFERARB, HDC);
typedef BOOL (GLAPIENTRY *wglDestroyPbufferARBType)(HPBUFFERARB);
typedef BOOL (GLAPIENTRY *wglQueryPbufferARBType)(HPBUFFERARB, int, int *);
typedef BOOL (GLAPIENTRY *wglMakeContextCurrentARBType)(HDC, HDC, HGLRC);
typedef const char *(GLAPIENTRY *wglGetExtensionsStringARBType)(HDC hdc);

#define OGL_LIB_HANDLE hDllOpenGL
#define OGL_DECLARE_LIB_HANDLE() \
    static HMODULE OGL_LIB_HANDLE = 0
#define OGL_LIB_IS_UNINITIALIZED() \
    (OGL_LIB_HANDLE == 0)
#define OGL_OPEN_LIB() \
    OGL_LIB_HANDLE = JDK_LoadSystemLibrary("opengl32.dll")
#define OGL_CLOSE_LIB() \
    FreeLibrary(OGL_LIB_HANDLE)
#define OGL_GET_PROC_ADDRESS(f) \
    GetProcAddress(OGL_LIB_HANDLE, #f)
#define OGL_GET_EXT_PROC_ADDRESS(f) \
    j2d_wglGetProcAddress((LPCSTR)#f)

#define OGL_EXPRESS_PLATFORM_FUNCS(action) \
    OGL_##action##_FUNC(wglCreateContext); \
    OGL_##action##_FUNC(wglDeleteContext); \
    OGL_##action##_FUNC(wglMakeCurrent); \
    OGL_##action##_FUNC(wglGetCurrentContext); \
    OGL_##action##_FUNC(wglGetCurrentDC); \
    OGL_##action##_FUNC(wglGetProcAddress); \
    OGL_##action##_FUNC(wglShareLists);

#define OGL_EXPRESS_PLATFORM_EXT_FUNCS(action) \
    OGL_##action##_EXT_FUNC(wglChoosePixelFormatARB); \
    OGL_##action##_EXT_FUNC(wglGetPixelFormatAttribivARB); \
    OGL_##action##_EXT_FUNC(wglCreatePbufferARB); \
    OGL_##action##_EXT_FUNC(wglGetPbufferDCARB); \
    OGL_##action##_EXT_FUNC(wglReleasePbufferDCARB); \
    OGL_##action##_EXT_FUNC(wglDestroyPbufferARB); \
    OGL_##action##_EXT_FUNC(wglQueryPbufferARB); \
    OGL_##action##_EXT_FUNC(wglMakeContextCurrentARB); \
    OGL_##action##_EXT_FUNC(wglGetExtensionsStringARB);

#endif /* OGLFuncs_md_h_Included */
