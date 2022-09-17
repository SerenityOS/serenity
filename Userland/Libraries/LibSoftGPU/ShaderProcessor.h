/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/SIMD.h>
#include <AK/Vector.h>
#include <LibGPU/Config.h>
#include <LibSoftGPU/PixelQuad.h>
#include <LibSoftGPU/Sampler.h>

namespace SoftGPU {

class Shader;

class ShaderProcessor final {
public:
    ShaderProcessor(Array<Sampler, GPU::NUM_TEXTURE_UNITS>& samplers)
        : m_samplers { samplers }
    {
    }

    void execute(PixelQuad&, Shader const&);

    ALWAYS_INLINE AK::SIMD::f32x4 get_register(u16 index) const { return m_registers[index]; }
    ALWAYS_INLINE void set_register(u16 index, AK::SIMD::f32x4 value) { m_registers[index] = value; }

private:
    void op_input(PixelQuad const&, Instruction::Arguments);
    void op_output(PixelQuad&, Instruction::Arguments);
    void op_sample2d(Instruction::Arguments);
    void op_swizzle(Instruction::Arguments);
    void op_add(Instruction::Arguments);
    void op_sub(Instruction::Arguments);
    void op_mul(Instruction::Arguments);
    void op_div(Instruction::Arguments);

    Array<Sampler, GPU::NUM_TEXTURE_UNITS>& m_samplers;
    AK::SIMD::f32x4 m_registers[1024];
};

}
