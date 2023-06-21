/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibDebug/Dwarf/DwarfTypes.h>

namespace Debug::Dwarf {

class CompilationUnit;

class AttributeValue {
    friend class DwarfInfo;

public:
    enum class Type : u8 {
        UnsignedNumber,
        SignedNumber,
        String,
        DieReference, // Reference to another DIE in the same compilation unit
        Boolean,
        DwarfExpression,
        SecOffset,
        RawBytes,
        Address
    };

    Type type() const { return m_type; }
    AttributeDataForm form() const { return m_form; }

    ErrorOr<FlatPtr> as_addr() const;
    u64 as_unsigned() const { return m_data.as_unsigned; }
    i64 as_signed() const { return m_data.as_signed; }
    ErrorOr<char const*> as_string() const;
    bool as_bool() const { return m_data.as_bool; }
    ReadonlyBytes as_raw_bytes() const { return m_data.as_raw_bytes; }

private:
    Type m_type;
    union {
        FlatPtr as_addr;
        u64 as_unsigned;
        i64 as_signed;
        char const* as_string; // points to bytes in the memory mapped elf image
        bool as_bool;
        ReadonlyBytes as_raw_bytes;
    } m_data {};

    AttributeDataForm m_form {};

    CompilationUnit const* m_compilation_unit { nullptr };
};

}
