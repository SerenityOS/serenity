/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/ByteBuffer.h"
#include "AK/Types.h"

struct PtraceRegisters;

namespace Debug::Dwarf::Expression {

enum class Type {
    None,
    UnsignedInteger,
    Register,
};

struct Value {
    Type type;
    union {
        FlatPtr as_addr;
        u32 as_u32;
    } data { 0 };
};

enum class Operations : u8 {
    RegEbp = 0x75,
    FbReg = 0x91,
};

Value evaluate(ReadonlyBytes, PtraceRegisters const&);

}
