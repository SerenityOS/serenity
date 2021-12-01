/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"
#include <AK/Debug.h>

extern GL::GLContext* g_gl_context;

void glLightf(GLenum light, GLenum pname, GLfloat param)
{
    // FIXME: implement
    dbgln_if(GL_DEBUG, "glLightf({}, {}, {}): unimplemented", light, pname, param);
}

void glLightfv(GLenum light, GLenum pname, GLfloat* param)
{
    // FIXME: implement
    dbgln_if(GL_DEBUG, "glLightfv({}, {}, {}): unimplemented", light, pname, param);
}

void glShadeModel(GLenum mode)
{
    g_gl_context->gl_shade_model(mode);
}
