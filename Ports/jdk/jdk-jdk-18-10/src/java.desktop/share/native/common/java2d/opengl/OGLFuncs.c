/*
 * Copyright (c) 2004, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HEADLESS

#include "OGLFuncs.h"

OGL_EXPRESS_ALL_FUNCS(DECLARE)

OGL_DECLARE_LIB_HANDLE();

jboolean
OGLFuncs_OpenLibrary()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "OGLFuncs_OpenLibrary");

    OGL_OPEN_LIB();
    if (OGL_LIB_IS_UNINITIALIZED()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLFuncs_OpenLibrary: could not open library");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

void
OGLFuncs_CloseLibrary()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "OGLFuncs_CloseLibrary");

    if (OGL_LIB_IS_UNINITIALIZED()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLFuncs_CloseLibrary: library not yet initialized");
        return;
    }

    if (OGL_CLOSE_LIB()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLFuncs_CloseLibrary: could not close library");
    }
}

jboolean
OGLFuncs_InitPlatformFuncs()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "OGLFuncs_InitPlatformFuncs");

    if (OGL_LIB_IS_UNINITIALIZED()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLFuncs_InitPlatformFuncs: library not yet initialized");
        return JNI_FALSE;
    }

    OGL_EXPRESS_PLATFORM_FUNCS(INIT_AND_CHECK)

    J2dTraceLn(J2D_TRACE_VERBOSE,
        "OGLFuncs_InitPlatformFuncs: successfully loaded platform symbols");

    return JNI_TRUE;
}

jboolean
OGLFuncs_InitBaseFuncs()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "OGLFuncs_InitBaseFuncs");

    if (OGL_LIB_IS_UNINITIALIZED()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLFuncs_InitBaseFuncs: library not yet initialized");
        return JNI_FALSE;
    }

    OGL_EXPRESS_BASE_FUNCS(INIT_AND_CHECK)

    J2dTraceLn(J2D_TRACE_VERBOSE,
        "OGLFuncs_InitBaseFuncs: successfully loaded base symbols");

    return JNI_TRUE;
}

jboolean
OGLFuncs_InitExtFuncs()
{
    J2dRlsTraceLn(J2D_TRACE_INFO, "OGLFuncs_InitExtFuncs");

    if (OGL_LIB_IS_UNINITIALIZED()) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLFuncs_InitExtFuncs: library not yet initialized");
        return JNI_FALSE;
    }

    OGL_EXPRESS_EXT_FUNCS(INIT)
    OGL_EXPRESS_PLATFORM_EXT_FUNCS(INIT_AND_CHECK)

    J2dTraceLn(J2D_TRACE_VERBOSE,
        "OGLFuncs_InitExtFuncs: successfully loaded ext symbols");

    return JNI_TRUE;
}

#endif /* !HEADLESS */
