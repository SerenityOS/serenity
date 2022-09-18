/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGLSL/Linker.h>

namespace GLSL {

ErrorOr<NonnullOwnPtr<LinkedShader>> Linker::link(Vector<ObjectFile const*> const&)
{
    // FIXME: implement this function
    m_messages = String::empty();

    GPU::IR::Shader shader;

    shader.inputs.append({ "input0"sv, GPU::IR::StorageType::Vector4 });
    shader.outputs.append({ "output0"sv, GPU::IR::StorageType::Vector4 });
    GPU::IR::Instruction instruction {
        GPU::IR::Opcode::Move,
        { { GPU::IR::StorageLocation::Input, 0 } },
        { GPU::IR::StorageLocation::Output, 0 }
    };
    shader.instructions.append(instruction);

    return adopt_own(*new LinkedShader(shader));
}

}
