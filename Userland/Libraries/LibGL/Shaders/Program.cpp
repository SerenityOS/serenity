/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Shaders/Program.h>

namespace GL {

NonnullRefPtr<Program> Program::create()
{
    return adopt_ref(*new Program());
}

bool Program::is_shader_attached(Shader const& shader) const
{
    switch (shader.type()) {
    case GL_VERTEX_SHADER:
        return m_vertex_shaders.contains_slow(shader);
        break;

    case GL_FRAGMENT_SHADER:
        return m_fragment_shaders.contains_slow(shader);
        break;

    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> Program::attach_shader(Shader& shader)
{
    if (is_shader_attached(shader))
        return Error::from_string_literal("Trying to attach a shader that is already attached");

    switch (shader.type()) {
    case GL_VERTEX_SHADER:
        m_vertex_shaders.append(shader);
        break;

    case GL_FRAGMENT_SHADER:
        m_fragment_shaders.append(shader);
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

ErrorOr<void> Program::link()
{
    TODO();
    return {};
}

}
