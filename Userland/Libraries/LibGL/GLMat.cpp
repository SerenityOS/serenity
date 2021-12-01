/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
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

/*
 * Transposes input matrices (column-major) to our Matrix (row-major).
 */
template<typename T>
static constexpr Matrix4x4<T> transpose_input_matrix(T const* matrix)
{
    return {
        matrix[0], matrix[4], matrix[8], matrix[12],
        matrix[1], matrix[5], matrix[9], matrix[13],
        matrix[2], matrix[6], matrix[10], matrix[14],
        matrix[3], matrix[7], matrix[11], matrix[15],
    };
}

void glMultMatrixf(GLfloat const* matrix)
{
    g_gl_context->gl_mult_matrix(transpose_input_matrix<float>(matrix));
}

void glLoadMatrixf(const GLfloat* matrix)
{
    g_gl_context->gl_load_matrix(transpose_input_matrix<float>(matrix));
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

void glOrtho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal)
{
    g_gl_context->gl_ortho(left, right, bottom, top, nearVal, farVal);
}
