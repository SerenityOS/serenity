/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Shader.h>
#include <LibSoftGPU/ShaderProcessor.h>

namespace SoftGPU {

using AK::SIMD::f32x4;

void ShaderProcessor::execute(PixelQuad& quad, Shader const& shader)
{
    auto& instructions = shader.instructions();
    for (size_t program_counter = 0; program_counter < instructions.size(); ++program_counter) {
        auto instruction = instructions[program_counter];
        switch (instruction.operation) {
        case Opcode::Input:
            op_input(quad, instruction.arguments);
            break;
        case Opcode::Output:
            op_output(quad, instruction.arguments);
            break;
        case Opcode::Sample2D:
            op_sample2d(instruction.arguments);
            break;
        case Opcode::Swizzle:
            op_swizzle(instruction.arguments);
            break;
        case Opcode::Add:
            op_add(instruction.arguments);
            break;
        case Opcode::Sub:
            op_sub(instruction.arguments);
            break;
        case Opcode::Mul:
            op_mul(instruction.arguments);
            break;
        case Opcode::Div:
            op_div(instruction.arguments);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

void ShaderProcessor::op_input(PixelQuad const& quad, Instruction::Arguments arguments)
{
    set_register(arguments.input.target_register, quad.get_input_float(arguments.input.input_index));
    set_register(arguments.input.target_register + 1, quad.get_input_float(arguments.input.input_index + 1));
    set_register(arguments.input.target_register + 2, quad.get_input_float(arguments.input.input_index + 2));
    set_register(arguments.input.target_register + 3, quad.get_input_float(arguments.input.input_index + 3));
}

void ShaderProcessor::op_output(PixelQuad& quad, Instruction::Arguments arguments)
{
    quad.set_output(arguments.output.output_index, get_register(arguments.output.source_register));
    quad.set_output(arguments.output.output_index + 1, get_register(arguments.output.source_register + 1));
    quad.set_output(arguments.output.output_index + 2, get_register(arguments.output.source_register + 2));
    quad.set_output(arguments.output.output_index + 3, get_register(arguments.output.source_register + 3));
}

void ShaderProcessor::op_sample2d(Instruction::Arguments arguments)
{
    Vector2<AK::SIMD::f32x4> coordinates = {
        get_register(arguments.sample.coordinates_register),
        get_register(arguments.sample.coordinates_register + 1),
    };
    auto sample = m_samplers[arguments.sample.sampler_index].sample_2d(coordinates);
    set_register(arguments.sample.target_register, sample.x());
    set_register(arguments.sample.target_register + 1, sample.y());
    set_register(arguments.sample.target_register + 2, sample.z());
    set_register(arguments.sample.target_register + 3, sample.w());
}

void ShaderProcessor::op_swizzle(Instruction::Arguments arguments)
{
    f32x4 inputs[] {
        get_register(arguments.swizzle.source_register),
        get_register(arguments.swizzle.source_register + 1),
        get_register(arguments.swizzle.source_register + 2),
        get_register(arguments.swizzle.source_register + 3)
    };

    set_register(arguments.swizzle.target_register, inputs[swizzle_index(arguments.swizzle.pattern, 0)]);
    set_register(arguments.swizzle.target_register + 1, inputs[swizzle_index(arguments.swizzle.pattern, 1)]);
    set_register(arguments.swizzle.target_register + 2, inputs[swizzle_index(arguments.swizzle.pattern, 2)]);
    set_register(arguments.swizzle.target_register + 3, inputs[swizzle_index(arguments.swizzle.pattern, 3)]);
}

#define SHADER_BINOP(NAME, OP)                                                            \
    void ShaderProcessor::op_##NAME(Instruction::Arguments arguments)                     \
    {                                                                                     \
        auto const target = arguments.binop.target_register;                              \
        auto const source1 = arguments.binop.source_register1;                            \
        auto const source2 = arguments.binop.source_register2;                            \
        set_register(target, get_register(source1) OP get_register(source2));             \
        set_register(target + 1, get_register(source1 + 1) OP get_register(source2 + 1)); \
        set_register(target + 2, get_register(source1 + 2) OP get_register(source2 + 2)); \
        set_register(target + 3, get_register(source1 + 3) OP get_register(source2 + 3)); \
    }

SHADER_BINOP(add, +)
SHADER_BINOP(sub, -)
SHADER_BINOP(mul, *)
SHADER_BINOP(div, /)

#undef SHADER_BINOP

}
