/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGL/Shaders/Shader.h>

namespace GL {

class Program final : public RefCounted<Program> {
public:
    static NonnullRefPtr<Program> create();

    ErrorOr<void> attach_shader(Shader&);
    ErrorOr<void> link();

private:
    Vector<NonnullRefPtr<Shader>> m_vertex_shaders;
    Vector<NonnullRefPtr<Shader>> m_fragment_shaders;
};

}
