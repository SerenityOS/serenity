/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AK/Array.h"
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
template<typename I, typename O>
static constexpr Matrix4x4<O> transpose_input_matrix(I const* matrix)
{
    if constexpr (IsSame<I, O>) {
        // clang-format off
        return {
            matrix[0], matrix[4], matrix[8], matrix[12],
            matrix[1], matrix[5], matrix[9], matrix[13],
            matrix[2], matrix[6], matrix[10], matrix[14],
            matrix[3], matrix[7], matrix[11], matrix[15],
        };
        // clang-format on
    }

    Array<O, 16> elements;
    for (size_t i = 0; i < 16; ++i)
        elements[i] = static_cast<O>(matrix[i]);
    // clang-format off
    return {
        elements[0], elements[4], elements[8], elements[12],
        elements[1], elements[5], elements[9], elements[13],
        elements[2], elements[6], elements[10], elements[14],
        elements[3], elements[7], elements[11], elements[15],
    };
    // clang-format on
}

void glMultMatrixf(GLfloat const* matrix)
{
    g_gl_context->gl_mult_matrix(transpose_input_matrix<float, float>(matrix));
}

void glLoadMatrixd(GLdouble const* matrix)
{
    g_gl_context->gl_load_matrix(transpose_input_matrix<double, float>(matrix));
}

void glLoadMatrixf(GLfloat const* matrix)
{
    g_gl_context->gl_load_matrix(transpose_input_matrix<float, float>(matrix));
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

void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_rotate(angle, x, y, z);
}

void glScaled(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_scale(x, y, z);
}

void glScalef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_scale(x, y, z);
}

void glTranslated(GLdouble x, GLdouble y, GLdouble z)
{
    g_gl_context->gl_translate(x, y, z);
}

void glTranslatef(GLfloat x, GLfloat y, GLfloat z)
{
    g_gl_context->gl_translate(x, y, z);
}
