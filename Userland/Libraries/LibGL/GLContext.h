/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GL/gl.h"
#include <AK/OwnPtr.h>
#include <LibGfx/Matrix4x4.h>

namespace GL {

class GLContext {
public:
    virtual ~GLContext() { }

    virtual void gl_begin(GLenum mode) = 0;
    virtual void gl_clear(GLbitfield mask) = 0;
    virtual void gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) = 0;
    virtual void gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a) = 0;
    virtual void gl_end() = 0;
    virtual void gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val) = 0;
    virtual GLenum gl_get_error() = 0;
    virtual GLubyte* gl_get_string(GLenum name) = 0;
    virtual void gl_load_identity() = 0;
    virtual void gl_load_matrix(const FloatMatrix4x4& matrix) = 0;
    virtual void gl_matrix_mode(GLenum mode) = 0;
    virtual void gl_push_matrix() = 0;
    virtual void gl_pop_matrix() = 0;
    virtual void gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_scale(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_translate(GLdouble x, GLdouble y, GLdouble z) = 0;
    virtual void gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w) = 0;
    virtual void gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height) = 0;
};

OwnPtr<GLContext> create_context();
void make_context_current(GLContext*);

}
