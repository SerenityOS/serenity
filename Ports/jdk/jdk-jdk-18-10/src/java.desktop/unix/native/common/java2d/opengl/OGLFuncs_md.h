/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#ifndef MACOSX
#include <dlfcn.h>
#endif
#include "jvm_md.h"
#include "J2D_GL/glx.h"
#include "OGLFuncMacros.h"

/**
 * GLX 1.2 functions
 */
typedef void (GLAPIENTRY *glXDestroyContextType)(Display *dpy, GLXContext ctx);
typedef GLXContext (GLAPIENTRY *glXGetCurrentContextType)(void);
typedef GLXDrawable (GLAPIENTRY *glXGetCurrentDrawableType)(void);
typedef Bool (GLAPIENTRY *glXIsDirectType)(Display *dpy, GLXContext ctx);
typedef Bool (GLAPIENTRY *glXQueryExtensionType)(Display *dpy, int *errorBase, int *eventBase);
typedef Bool (GLAPIENTRY *glXQueryVersionType)(Display *dpy, int *major, int *minor);
typedef void (GLAPIENTRY *glXSwapBuffersType)(Display *dpy, GLXDrawable drawable);
typedef const char * (GLAPIENTRY *glXGetClientStringType)(Display *dpy, int name);
typedef const char * (GLAPIENTRY *glXQueryServerStringType)(Display *dpy, int screen, int name);
typedef const char * (GLAPIENTRY *glXQueryExtensionsStringType)(Display *dpy, int screen);
typedef void (GLAPIENTRY *glXWaitGLType)(void);

/**
 * GLX 1.3 functions
 */
typedef GLXFBConfig * (GLAPIENTRY *glXGetFBConfigsType)(Display *dpy, int screen, int *nelements);
typedef GLXFBConfig * (GLAPIENTRY *glXChooseFBConfigType)(Display *dpy, int screen, const int *attrib_list, int *nelements);
typedef int (GLAPIENTRY *glXGetFBConfigAttribType)(Display *dpy, GLXFBConfig  config, int attribute, int *value);
typedef XVisualInfo * (GLAPIENTRY *glXGetVisualFromFBConfigType)(Display *dpy, GLXFBConfig  config);
typedef GLXWindow (GLAPIENTRY *glXCreateWindowType)(Display *dpy, GLXFBConfig config, Window win, const int *attrib_list);
typedef void (GLAPIENTRY *glXDestroyWindowType)(Display *dpy, GLXWindow win);
typedef GLXPbuffer (GLAPIENTRY *glXCreatePbufferType)(Display *dpy, GLXFBConfig config, const int *attrib_list);
typedef void (GLAPIENTRY *glXDestroyPbufferType)(Display *dpy, GLXPbuffer pbuffer);
typedef void (GLAPIENTRY *glXQueryDrawableType)(Display *dpy, GLXDrawable draw, int attribute, unsigned int *value);
typedef GLXContext (GLAPIENTRY *glXCreateNewContextType)(Display *dpy, GLXFBConfig config, int render_type, GLXContext share_list, Bool direct);
typedef Bool (GLAPIENTRY *glXMakeContextCurrentType)(Display *dpy, GLXDrawable draw, GLXDrawable read, GLXContext ctx);
typedef GLXDrawable (GLAPIENTRY *glXGetCurrentReadDrawableType)(void);
typedef int (GLAPIENTRY *glXQueryContextType)(Display *dpy, GLXContext ctx, int attribute, int *value);
typedef void (GLAPIENTRY *glXSelectEventType)(Display *dpy, GLXDrawable draw, unsigned long event_mask);
typedef void (GLAPIENTRY *glXGetSelectedEventType)(Display *dpy, GLXDrawable draw, unsigned long *event_mask);

/**
 * GLX extension functions
 */
typedef void * (GLAPIENTRY *glXGetProcAddressType)(const char *);

/*
 * Note: Historically we have used dlopen/dlsym() to load function pointers
 * from libgl.so, and things have worked fine.  However, we have run into at
 * least one case (on ATI's Linux drivers) where dlsym() will return NULL
 * when trying to load functions from the GL_ARB_fragment_shader extension.
 * Plausibly this is a bug in their drivers (other extension functions load
 * just fine on those same drivers), but for a number of years there has been
 * a glXGetProcAddressARB() extension available that is intended to be the
 * primary means for an application to load extension functions in a reliable
 * manner.  So while dlsym() will return NULL for those shader-related
 * functions, glXGetProcAddressARB() works just fine.
 *
 * I haven't used the glXGetProcAddress() approach in the past because it
 * seemed unnecessary (i.e. dlsym() was working fine), but upon further
 * reading I think we should use glXGetProcAddress() in favor of dlsym(),
 * not only to work around this "bug", but also to be safer going forward.
 *
 * Just to complicate matters, glXGetProcAddress() was proposed to be added
 * into the GLX 1.4 spec, which is still (as yet) unfinalized.  Sun's OGL 1.3
 * implementation reports its GLX version as 1.4, and therefore includes
 * the glXGetProcAddress() entrypoint, but does not include
 * GLX_ARB_get_proc_address in its extension string nor does it export the
 * glXGetProcAddressARB() entrypoint.  On the other hand, ATI's Linux drivers
 * (as well as Nvidia's Linux and Solaris drivers) currently report their
 * GLX version as 1.3, but they do export the glXGetProcAddressARB()
 * entrypoint and its associated extension string.  So to make this work
 * everywhere, we first try to load the glXGetProcAddress() entrypoint,
 * failing that we try the glXGetProcAddressARB() entrypoint, and if that
 * fails too, then we close libGL.so and do not bother trying to initialize
 * the rest of the OGL pipeline.
 */

#define OGL_LIB_HANDLE pLibGL
#define OGL_DECLARE_LIB_HANDLE() \
    static glXGetProcAddressType j2d_glXGetProcAddress; \
    static void *OGL_LIB_HANDLE = NULL
#define OGL_LIB_IS_UNINITIALIZED() \
    (OGL_LIB_HANDLE == NULL)
#define OGL_OPEN_LIB() \
do { \
    { \
        char *libGLPath = getenv("J2D_ALT_LIBGL_PATH"); \
        if (libGLPath == NULL) { \
            libGLPath = VERSIONED_JNI_LIB_NAME("GL", "1"); \
        } \
        OGL_LIB_HANDLE = dlopen(libGLPath, RTLD_LAZY | RTLD_LOCAL); \
    } \
    if (OGL_LIB_HANDLE) { \
        j2d_glXGetProcAddress = (glXGetProcAddressType) \
            dlsym(OGL_LIB_HANDLE, "glXGetProcAddress"); \
        if (j2d_glXGetProcAddress == NULL) { \
            j2d_glXGetProcAddress = (glXGetProcAddressType) \
                dlsym(OGL_LIB_HANDLE, "glXGetProcAddressARB"); \
            if (j2d_glXGetProcAddress == NULL) { \
                dlclose(OGL_LIB_HANDLE); \
                OGL_LIB_HANDLE = NULL; \
            } \
        } \
    } \
} while (0)
#define OGL_CLOSE_LIB() \
    dlclose(OGL_LIB_HANDLE)
#define OGL_GET_PROC_ADDRESS(f) \
    j2d_glXGetProcAddress(#f)
#define OGL_GET_EXT_PROC_ADDRESS(f) \
    OGL_GET_PROC_ADDRESS(f)

#define OGL_EXPRESS_PLATFORM_FUNCS(action) \
    OGL_##action##_FUNC(glXDestroyContext); \
    OGL_##action##_FUNC(glXGetCurrentContext); \
    OGL_##action##_FUNC(glXGetCurrentDrawable); \
    OGL_##action##_FUNC(glXIsDirect); \
    OGL_##action##_FUNC(glXQueryExtension); \
    OGL_##action##_FUNC(glXQueryVersion); \
    OGL_##action##_FUNC(glXSwapBuffers); \
    OGL_##action##_FUNC(glXGetClientString); \
    OGL_##action##_FUNC(glXQueryServerString); \
    OGL_##action##_FUNC(glXQueryExtensionsString); \
    OGL_##action##_FUNC(glXWaitGL); \
    OGL_##action##_FUNC(glXGetFBConfigs); \
    OGL_##action##_FUNC(glXChooseFBConfig); \
    OGL_##action##_FUNC(glXGetFBConfigAttrib); \
    OGL_##action##_FUNC(glXGetVisualFromFBConfig); \
    OGL_##action##_FUNC(glXCreateWindow); \
    OGL_##action##_FUNC(glXDestroyWindow); \
    OGL_##action##_FUNC(glXCreatePbuffer); \
    OGL_##action##_FUNC(glXDestroyPbuffer); \
    OGL_##action##_FUNC(glXQueryDrawable); \
    OGL_##action##_FUNC(glXCreateNewContext); \
    OGL_##action##_FUNC(glXMakeContextCurrent); \
    OGL_##action##_FUNC(glXGetCurrentReadDrawable); \
    OGL_##action##_FUNC(glXQueryContext); \
    OGL_##action##_FUNC(glXSelectEvent); \
    OGL_##action##_FUNC(glXGetSelectedEvent);

#define OGL_EXPRESS_PLATFORM_EXT_FUNCS(action)

#endif /* OGLFuncs_md_h_Included */
