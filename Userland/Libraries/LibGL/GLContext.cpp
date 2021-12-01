/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GLContext.h"
#include "SoftwareGLContext.h"
#include <LibGfx/Bitmap.h>

__attribute__((visibility("hidden"))) GL::GLContext* g_gl_context;

namespace GL {

GLContext::~GLContext()
{
    if (g_gl_context == this)
        make_context_current(nullptr);
}

OwnPtr<GLContext> create_context(Gfx::Bitmap& bitmap)
{
    auto context = adopt_own(*new SoftwareGLContext(bitmap));

    if (!g_gl_context)
        g_gl_context = context;

    return context;
}

void make_context_current(GLContext* context)
{
    g_gl_context = context;
}

void present_context(GLContext* context)
{
    context->present();
}

}
