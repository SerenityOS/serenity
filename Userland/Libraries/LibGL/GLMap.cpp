/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glMap1d(GLenum target, GLdouble u1, GLdouble u2, GLint stride, GLint order, GLdouble const* points)
{
    dbgln("glMap1d({:#x}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, stride, order, points);
    TODO();
}

void glMap1f(GLenum target, GLfloat u1, GLfloat u2, GLint stride, GLint order, GLfloat const* points)
{
    dbgln("glMap1f({:#x}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, stride, order, points);
    TODO();
}

void glMap2d(GLenum target, GLdouble u1, GLdouble u2, GLint ustride, GLint uorder, GLdouble v1, GLdouble v2, GLint vstride, GLint vorder, GLdouble const* points)
{
    dbgln("glMap2d({:#x}, {}, {}, {}, {}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    TODO();
}

void glMap2f(GLenum target, GLfloat u1, GLfloat u2, GLint ustride, GLint uorder, GLfloat v1, GLfloat v2, GLint vstride, GLint vorder, GLfloat const* points)
{
    dbgln("glMap2f({:#x}, {}, {}, {}, {}, {}, {}, {}, {}, {:p}): unimplemented", target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    TODO();
}

void glMapGrid1d(GLint un, GLdouble u1, GLdouble u2)
{
    dbgln("glMapGrid1d({}, {}, {}): unimplemented", un, u1, u2);
    TODO();
}

void glMapGrid1f(GLint un, GLfloat u1, GLfloat u2)
{
    dbgln("glMapGrid1f({}, {}, {}): unimplemented", un, u1, u2);
    TODO();
}

void glMapGrid2d(GLint un, GLdouble u1, GLdouble u2, GLint vn, GLdouble v1, GLdouble v2)
{
    dbgln("glMapGrid2d({}, {}, {}, {}, {}, {}): unimplemented", un, u1, u2, vn, v1, v2);
    TODO();
}

void glMapGrid2f(GLint un, GLfloat u1, GLfloat u2, GLint vn, GLfloat v1, GLfloat v2)
{
    dbgln("glMapGrid2f({}, {}, {}, {}, {}, {}): unimplemented", un, u1, u2, vn, v1, v2);
    TODO();
}

void glEvalCoord1d(GLdouble u)
{
    dbgln("glEvalCoord1d({}): unimplemented", u);
    TODO();
}

void glEvalCoord1f(GLfloat u)
{
    dbgln("glEvalCoord1f({}): unimplemented", u);
    TODO();
}

void glEvalCoord2d(GLdouble u, GLdouble v)
{
    dbgln("glEvalCoord2d({}, {}): unimplemented", u, v);
    TODO();
}

void glEvalCoord2f(GLfloat u, GLfloat v)
{
    dbgln("glEvalCoord2f({}, {}): unimplemented", u, v);
    TODO();
}

void glEvalMesh1(GLenum mode, GLint i1, GLint i2)
{
    dbgln("glEvalMesh1({:#x}, {}, {}): unimplemented", mode, i1, i2);
    TODO();
}

void glEvalMesh2(GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2)
{
    dbgln("glEvalMesh2({:#x}, {}, {}, {}, {}): unimplemented", mode, i1, i2, j1, j2);
    TODO();
}

void glEvalPoint1(GLint i)
{
    dbgln("glEvalPoint1({}): unimplemented", i);
    TODO();
}

void glEvalPoint2(GLint i, GLint j)
{
    dbgln("glEvalPoint2({}, {}): unimplemented", i, j);
    TODO();
}
