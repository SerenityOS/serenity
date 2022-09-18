/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace GPU::IR {

enum class Opcode {
    Move,
};

enum class StorageLocation {
    Constant,
    Uniform,
    Input,
    Output,
    Temporary,
};

enum class StorageType {
    Float,
    Vector2,
    Vector3,
    Vector4,
    Matrix3x3,
    Matrix4x4,
};

struct StorageReference final {
    StorageLocation location;
    size_t index;
};

struct Instruction final {
    Opcode operation;
    Vector<StorageReference> arguments;
    StorageReference result;
};

struct Constant final {
    StorageType type;
    union {
        float float_values[16];
    };
};

struct Uniform final {
    String name;
    StorageType type;
};

struct Input final {
    String name;
    StorageType type;
};

struct Output final {
    String name;
    StorageType type;
};

struct Temporary final {
    StorageType type;
};

struct Shader final {
    Vector<Constant> constants;
    Vector<Uniform> uniforms;
    Vector<Input> inputs;
    Vector<Output> outputs;
    Vector<Temporary> temporaries;
    Vector<Instruction> instructions;
};

}
