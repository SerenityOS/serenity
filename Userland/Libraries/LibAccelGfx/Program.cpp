/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <LibAccelGfx/GL.h>
#include <LibAccelGfx/Program.h>

namespace AccelGfx {

Program Program::create(char const* vertex_shader_source, char const* fragment_shader_source)
{
    auto vertex_shader = GL::create_shader(GL::ShaderType::Vertex, vertex_shader_source);
    auto fragment_shader = GL::create_shader(GL::ShaderType::Fragment, fragment_shader_source);

    auto program = GL::create_program(vertex_shader, fragment_shader);

    return Program { program };
}

void Program::use()
{
    GL::use_program(m_program);
}

GL::VertexAttribute Program::get_attribute_location(char const* name)
{
    return GL::get_attribute_location(m_program, name);
}

GL::Uniform Program::get_uniform_location(char const* name)
{
    return GL::get_uniform_location(m_program, name);
}

Program::~Program()
{
    GL::delete_program(m_program);
}

}
