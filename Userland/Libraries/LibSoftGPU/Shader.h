/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGPU/Shader.h>
#include <LibSoftGPU/ISA.h>

namespace SoftGPU {

class Shader final : public GPU::Shader {
public:
    Shader(void const* ownership_token, Vector<Instruction> const&);

    Vector<Instruction> const& instructions() const { return m_instructions; }

private:
    Vector<Instruction> m_instructions;
};

}
