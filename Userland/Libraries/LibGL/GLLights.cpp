/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
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

void glMaterialf(GLenum face, GLenum pname, GLfloat param)
{
    VERIFY(face == GL_SHININESS);
    g_gl_context->gl_materialv(face, pname, &param);
}

void glMaterialfv(GLenum face, GLenum pname, GLfloat const* params)
{
    g_gl_context->gl_materialv(face, pname, params);
}

void glShadeModel(GLenum mode)
{
    g_gl_context->gl_shade_model(mode);
}
