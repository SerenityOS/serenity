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

Context::Surface Context::create_surface(int width, int height)
{
    EGLint const pbuffer_attributes[] = {
        EGL_WIDTH,
        width,
        EGL_HEIGHT,
        height,
        EGL_NONE,
    };

    auto egl_surface = eglCreatePbufferSurface(m_egl_display, m_egl_config, pbuffer_attributes);
    return { egl_surface };
}

void Context::destroy_surface(Surface surface)
{
    if (surface.egl_surface)
        eglDestroySurface(m_egl_display, surface.egl_surface);
}

void Context::set_active_surface(Surface surface)
{
    VERIFY(eglMakeCurrent(m_egl_display, surface.egl_surface, surface.egl_surface, m_egl_context));
}

OwnPtr<Context> Context::create()
{
    EGLDisplay egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    EGLint major;
    EGLint minor;
    eglInitialize(egl_display, &major, &minor);

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
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    EGLContext egl_context = eglCreateContext(egl_display, egl_config, EGL_NO_CONTEXT, context_attributes);

    EGLBoolean result = eglMakeCurrent(egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, egl_context);
    if (result == EGL_FALSE) {
        dbgln("eglMakeCurrent failed");
        VERIFY_NOT_REACHED();
    }

    return make<Context>(egl_display, egl_context, egl_config);
}

}
