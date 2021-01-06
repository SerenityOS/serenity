/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftwareGLContext.h"

__attribute__((visibility("hidden"))) GL::GLContext* g_gl_context;

namespace GL {

GLContext::~GLContext()
{
    if (g_gl_context == this)
        make_context_current(nullptr);
}

OwnPtr<GLContext> create_context()
{
    auto context = adopt_own(*new GL::SoftwareGLContext());

    if (!g_gl_context)
        g_gl_context = context;

    return context;
}

void make_context_current(GLContext* context)
{
    g_gl_context = context;
}

}
