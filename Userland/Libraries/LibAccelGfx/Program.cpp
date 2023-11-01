/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define GL_GLEXT_PROTOTYPES

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <GL/gl.h>
#include <GL/glext.h>
#include <LibAccelGfx/Program.h>

namespace AccelGfx {

static GLuint create_shader(GLenum type, char const* source)
{
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    int success;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char buffer[512];
        glGetShaderInfoLog(shader, sizeof(buffer), nullptr, buffer);
        dbgln("GLSL shader compilation failed: {}", buffer);
        VERIFY_NOT_REACHED();
    }

    return shader;
}

Program Program::create(char const* vertex_shader_source, char const* fragment_shader_source)
{
    GLuint program = glCreateProgram();

    auto vertex_shader = create_shader(GL_VERTEX_SHADER, vertex_shader_source);
    auto fragment_shader = create_shader(GL_FRAGMENT_SHADER, fragment_shader_source);

    glAttachShader(program, vertex_shader);
    glAttachShader(program, fragment_shader);
    glLinkProgram(program);

    int linked;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        char buffer[512];
        glGetProgramInfoLog(program, sizeof(buffer), nullptr, buffer);
        dbgln("GLSL program linking failed: {}", buffer);
        VERIFY_NOT_REACHED();
    }

    glDeleteShader(vertex_shader);
    glDeleteShader(fragment_shader);

    return Program { program };
}

void Program::use()
{
    glUseProgram(m_id);
}

GLuint Program::get_attribute_location(char const* name)
{
    return glGetAttribLocation(m_id, name);
}

GLuint Program::get_uniform_location(char const* name)
{
    return glGetUniformLocation(m_id, name);
}

Program::~Program()
{
    glDeleteProgram(m_id);
}

}
