/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <LibAccelGfx/GL.h>

namespace AccelGfx {

class Program {
    AK_MAKE_NONCOPYABLE(Program);

public:
    static Program create(char const* vertex_shader_source, char const* fragment_shader_source);

    void use();
    GL::VertexAttribute get_attribute_location(char const* name);
    GL::Uniform get_uniform_location(char const* name);

    ~Program();

private:
    Program(GL::Program program)
        : m_program(program)
    {
    }

    GL::Program m_program;
};

}
