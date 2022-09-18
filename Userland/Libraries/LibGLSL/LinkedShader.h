/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGLSL/ObjectFile.h>
#include <LibGPU/IR.h>

namespace GLSL {

// FIXME: Implement this class

class LinkedShader final {
public:
    LinkedShader(const GPU::IR::Shader& intermediate_shader_representation)
        : m_intermediate_shader_representation { intermediate_shader_representation }
    {
    }

    GPU::IR::Shader const& intermediate_shader_representation() const { return m_intermediate_shader_representation; }

private:
    GPU::IR::Shader m_intermediate_shader_representation;
};

}
