/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GLContext.h"
#include "SoftwareGLContext.h"
#include <AK/Debug.h>
#include <LibGfx/Bitmap.h>

__attribute__((visibility("hidden"))) GL::GLContext* g_gl_context;

namespace GL {

GLContext::~GLContext()
{
    dbgln_if(GL_DEBUG, "GLContext::~GLContext() {:p}", this);
    if (g_gl_context == this)
        make_context_current(nullptr);
}

NonnullOwnPtr<GLContext> create_context(Gfx::Bitmap& bitmap)
{
    auto context = make<SoftwareGLContext>(bitmap);
    dbgln_if(GL_DEBUG, "GL::create_context({}) -> {:p}", bitmap.size(), context.ptr());

    if (!g_gl_context)
        make_context_current(context);

    return context;
}

void make_context_current(GLContext* context)
{
    if (g_gl_context == context)
        return;

    dbgln_if(GL_DEBUG, "GL::make_context_current({:p})", context);
    g_gl_context = context;
}

void present_context(GLContext* context)
{
    context->present();
}

}
