/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAccelGfx/Context.h>

namespace AccelGfx {

Context& Context::the()
{
    static OwnPtr<Context> s_the;
    if (!s_the)
        s_the = Context::create();
    return *s_the;
}

OwnPtr<Context> Context::create()
{
    EGLDisplay egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    EGLint major;
    EGLint minor;
    eglInitialize(egl_display, &major, &minor);

    EGLBoolean ok = eglBindAPI(EGL_OPENGL_API);
    if (ok == EGL_FALSE) {
        dbgln("eglBindAPI failed");
        VERIFY_NOT_REACHED();
    }

    static EGLint const config_attributes[] = {
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_DEPTH_SIZE, 8,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
        EGL_NONE
    };

    EGLConfig egl_config;
    EGLint num_configs;
    eglChooseConfig(egl_display, config_attributes, &egl_config, 1, &num_configs);

    static EGLint const context_attributes[] = {
        EGL_CONTEXT_MAJOR_VERSION, 3,
        EGL_CONTEXT_MINOR_VERSION, 3,
        EGL_NONE
    };
    EGLContext egl_context = eglCreateContext(egl_display, egl_config, EGL_NO_CONTEXT, context_attributes);
    if (egl_context == EGL_FALSE) {
        dbgln("eglCreateContext failed");
        VERIFY_NOT_REACHED();
    }

    EGLBoolean result = eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, egl_context);
    if (result == EGL_FALSE) {
        dbgln("eglMakeCurrent failed");
        VERIFY_NOT_REACHED();
    }

    return make<Context>(egl_display, egl_context, egl_config);
}

}
