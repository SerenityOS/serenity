/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGL/Shaders/Shader.h>
#include <LibGLSL/LinkedShader.h>
#include <LibGPU/Device.h>
#include <LibGPU/Shader.h>

namespace GL {

class Program final : public RefCounted<Program> {
public:
    static NonnullRefPtr<Program> create();

    bool is_shader_attached(Shader const&) const;
    ErrorOr<void> attach_shader(Shader&);
    ErrorOr<void> link(GPU::Device&);
    bool link_status() const { return m_link_status; }
    size_t info_log_length() const;

private:
    bool m_link_status { false };
    Vector<NonnullRefPtr<Shader const>> m_vertex_shaders;
    Vector<NonnullRefPtr<Shader const>> m_fragment_shaders;
    Optional<String> m_info_log;
    OwnPtr<GLSL::LinkedShader> m_linked_vertex_shader;
    OwnPtr<GLSL::LinkedShader> m_linked_fragment_shader;
    RefPtr<GPU::Shader const> m_gpu_vertex_shader;
    RefPtr<GPU::Shader const> m_gpu_fragment_shader;
};

}
