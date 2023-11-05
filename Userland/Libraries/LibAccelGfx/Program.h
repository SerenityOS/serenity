/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <GL/gl.h>

namespace AccelGfx {

class Program {
    AK_MAKE_NONCOPYABLE(Program);

public:
    static Program create(char const* vertex_shader_source, char const* fragment_shader_source);

    void use();
    GLuint get_attribute_location(char const* name);
    GLuint get_uniform_location(char const* name);

    ~Program();

private:
    Program(GLuint id)
        : m_id(id)
    {
    }

    GLuint m_id { 0 };
};

}
