/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GL/gl.h"
#include "GLContext.h"

extern GL::GLContext* g_gl_context;

void glMatrixMode(GLenum mode)
{
    g_gl_context->gl_matrix_mode(mode);
}

/*
 * Push the current matrix (based on the current matrix mode)
 * to its' corresponding matrix stack in the current OpenGL
 * state context
 */
void glPushMatrix()
{
    g_gl_context->gl_push_matrix();
}

/*
 * Pop a matrix from the corresponding matrix stack into the
 * corresponding matrix in the state based on the current
 * matrix mode
 */
void glPopMatrix()
{
    g_gl_context->gl_pop_matrix();
}

void glLoadIdentity()
{
    g_gl_context->gl_load_identity();
}

/**
 * Create a viewing frustum (a.k.a a "Perspective Matrix") in the current matrix. This
 * is usually done to the projection matrix. The current matrix is then multiplied
 * by this viewing frustum matrix.
 *
 * https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glFrustum.xml
 *
 *
 * FIXME: We need to check for some values that could result in a division by zero
 */
void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal)
{
    g_gl_context->gl_frustum(left, right, bottom, top, nearVal, farVal);
}
