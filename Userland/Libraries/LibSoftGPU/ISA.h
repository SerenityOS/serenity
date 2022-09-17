/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace SoftGPU {

constexpr u8 swizzle_pattern(u8 a, u8 b, u8 c, u8 d)
{
    return a | (b << 2) | (c << 4) | (d << 6);
}

constexpr u8 swizzle_index(u8 pattern, u8 element)
{
    return (pattern & (3 << (element * 2))) >> (element * 2);
}

enum class Opcode : u8 {
    Input,
    Output,
    Sample2D,
    Swizzle,
    Add,
    Sub,
    Mul,
    Div,
};

struct Instruction final {
    union Arguments {
        struct {
            u16 target_register;
            u8 input_index;
        } input;
        struct {
            u16 source_register;
            u8 output_index;
        } output;
        struct {
            u16 target_register;
            u16 coordinates_register;
            u8 sampler_index;
        } sample;
        struct {
            u16 target_register;
            u16 source_register;
            u8 pattern;
        } swizzle;
        struct {
            u16 target_register;
            u16 source_register1;
            u16 source_register2;
        } binop;
    } arguments;
    Opcode operation;
};

}
