/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/OwnPtr.h>

// Make sure egl.h doesn't give us definitions from X11 headers
#define EGL_NO_X11
#include <EGL/egl.h>
#undef EGL_NO_X11

namespace AccelGfx {

class Context {
public:
    static Context& the();

    static OwnPtr<Context> create();

    Context(EGLDisplay egl_display, EGLContext egl_context, EGLConfig egl_config)
        : m_egl_display(egl_display)
        , m_egl_context(egl_context)
        , m_egl_config(egl_config)
    {
    }

private:
    EGLDisplay m_egl_display { nullptr };
    EGLContext m_egl_context { nullptr };
    EGLConfig m_egl_config { nullptr };
};

}
