/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGL/Shaders/Shader.h>

namespace GL {

class Program final : public RefCounted<Program> {
public:
    static NonnullRefPtr<Program> create();

    bool is_shader_attached(Shader const&) const;
    ErrorOr<void> attach_shader(Shader&);
    ErrorOr<void> link();
    bool link_status() const { return m_link_status; }

private:
    bool m_link_status { false };
    Vector<NonnullRefPtr<Shader>> m_vertex_shaders;
    Vector<NonnullRefPtr<Shader>> m_fragment_shaders;
};

}
