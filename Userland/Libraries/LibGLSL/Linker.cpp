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
    m_messages = TRY(String::from_utf8(""sv));

    GPU::IR::Shader shader;

    auto input_name = TRY(String::from_utf8("input0"sv));
    auto output_name = TRY(String::from_utf8("output0"sv));
    TRY(shader.inputs.try_append({ move(input_name), GPU::IR::StorageType::Vector4 }));
    TRY(shader.outputs.try_append({ move(output_name), GPU::IR::StorageType::Vector4 }));
    GPU::IR::Instruction instruction {
        GPU::IR::Opcode::Move,
        { { GPU::IR::StorageLocation::Input, 0 } },
        { GPU::IR::StorageLocation::Output, 0 }
    };
    TRY(shader.instructions.try_append(instruction));

    return adopt_own(*new LinkedShader(shader));
}

}
