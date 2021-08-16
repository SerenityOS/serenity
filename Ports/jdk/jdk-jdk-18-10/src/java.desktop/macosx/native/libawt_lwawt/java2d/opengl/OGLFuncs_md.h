/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <dlfcn.h>
#include "J2D_GL/cglext.h"
#include "OGLFuncMacros.h"

#define OGL_LIB_HANDLE pLibGL
#define OGL_DECLARE_LIB_HANDLE() \
    static void *OGL_LIB_HANDLE = NULL
#define OGL_LIB_IS_UNINITIALIZED() \
    (OGL_LIB_HANDLE == NULL)
#define OGL_OPEN_LIB() \
    OGL_LIB_HANDLE = dlopen("/System/Library/Frameworks/OpenGL.framework/Versions/Current/Libraries/libGL.dylib", RTLD_LAZY | RTLD_GLOBAL)
#define OGL_CLOSE_LIB() \
    dlclose(OGL_LIB_HANDLE)
#define OGL_GET_PROC_ADDRESS(f) \
    dlsym(OGL_LIB_HANDLE, #f)
#define OGL_GET_EXT_PROC_ADDRESS(f) \
    OGL_GET_PROC_ADDRESS(f)

#define OGL_EXPRESS_PLATFORM_FUNCS(action)
#define OGL_EXPRESS_PLATFORM_EXT_FUNCS(action)

#endif /* OGLFuncs_md_h_Included */
