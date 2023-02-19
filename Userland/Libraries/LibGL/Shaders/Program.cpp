/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Shaders/Program.h>
#include <LibGLSL/Linker.h>

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
    case GL_FRAGMENT_SHADER:
        return m_fragment_shaders.contains_slow(shader);
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
        TRY(m_vertex_shaders.try_append(shader));
        break;

    case GL_FRAGMENT_SHADER:
        TRY(m_fragment_shaders.try_append(shader));
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

ErrorOr<void> Program::link(GPU::Device& device)
{
    m_info_log = String {};

    GLSL::Linker linker;

    // Link vertex shader objects

    Vector<GLSL::ObjectFile const*> vertex_shader_object_files;
    for (auto const& vertex_shader : m_vertex_shaders)
        vertex_shader_object_files.append(vertex_shader->object_file());

    auto linked_vertex_shader_or_error = linker.link(vertex_shader_object_files);

    if (linked_vertex_shader_or_error.is_error()) {
        m_link_status = false;
        m_info_log = linker.messages();
        return linked_vertex_shader_or_error.release_error();
    }

    m_linked_vertex_shader = linked_vertex_shader_or_error.release_value();

    // Link fragment shader objects

    Vector<GLSL::ObjectFile const*> fragment_shader_object_files;
    for (auto fragment_shader : m_fragment_shaders)
        fragment_shader_object_files.append(fragment_shader->object_file());

    auto linked_fragment_shader_or_error = linker.link(fragment_shader_object_files);

    if (linked_fragment_shader_or_error.is_error()) {
        m_link_status = false;
        m_info_log = linker.messages();
        return linked_fragment_shader_or_error.release_error();
    }

    m_linked_fragment_shader = linked_fragment_shader_or_error.release_value();

    m_gpu_vertex_shader = TRY(device.create_shader(m_linked_vertex_shader->intermediate_shader_representation()));
    m_gpu_fragment_shader = TRY(device.create_shader(m_linked_fragment_shader->intermediate_shader_representation()));

    m_link_status = true;
    return {};
}

size_t Program::info_log_length() const
{
    if (!m_info_log.has_value())
        return 0;

    // Per the spec we return the size including the null terminator
    return m_info_log.value().bytes().size() + 1;
}

}
