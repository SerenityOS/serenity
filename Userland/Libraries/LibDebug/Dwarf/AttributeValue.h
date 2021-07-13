/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibDebug/Dwarf/DwarfTypes.h>

namespace Debug::Dwarf {

struct AttributeValue {
    enum class Type : u8 {
        UnsignedNumber,
        SignedNumber,
        LongUnsignedNumber,
        String,
        DieReference, // Reference to another DIE in the same compilation unit
        Boolean,
        DwarfExpression,
        SecOffset,
        RawBytes,
    } type;

    union {
        FlatPtr as_addr;
        u32 as_u32;
        i32 as_i32;
        u64 as_u64;
        const char* as_string; // points to bytes in the memory mapped elf image
        bool as_bool;
        struct {
            u32 length;
            const u8* bytes; // points to bytes in the memory mapped elf image
        } as_raw_bytes;
    } data {};

    AttributeDataForm form {};
};

}
