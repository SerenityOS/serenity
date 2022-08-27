/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGL/GLContext.h>

namespace GL {

GLuint GLContext::gl_create_shader(GLenum shader_type)
{
    dbgln("gl_create_shader({}) unimplemented ", shader_type);
    TODO();
    return 0;
}

void GLContext::gl_delete_shader(GLuint shader)
{
    dbgln("gl_delete_shader({}) unimplemented ", shader);
    TODO();
}

void GLContext::gl_shader_source(GLuint shader, GLsizei count, GLchar const** string, GLint const* length)
{
    dbgln("gl_shader_source({}, {}, {#x}, {#x}) unimplemented ", shader, count, string, length);
    TODO();
}

void GLContext::gl_compile_shader(GLuint shader)
{
    dbgln("gl_compile_shader({}) unimplemented ", shader);
    TODO();
}

GLuint GLContext::gl_create_program()
{
    dbgln("gl_create_program() unimplemented ");
    TODO();
    return 0;
}

void GLContext::gl_delete_program(GLuint program)
{
    dbgln("gl_delete_program({}) unimplemented ", program);
    TODO();
}

void GLContext::gl_attach_shader(GLuint program, GLuint shader)
{
    dbgln("gl_attach_shader({}, {}) unimplemented ", program, shader);
    TODO();
}

void GLContext::gl_link_program(GLuint program)
{
    dbgln("gl_link_program({}) unimplemented ", program);
    TODO();
}

}
