/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CompilationUnit.h"
#include "DwarfTypes.h"
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/Types.h>

namespace Debug::Dwarf {

class CompilationUnit;

// DIE = Debugging Information Entry
class DIE {
public:
    DIE(const CompilationUnit&, u32 offset);

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
    };

    u32 offset() const { return m_offset; }
    u32 size() const { return m_size; }
    bool has_children() const { return m_has_children; }
    EntryTag tag() const { return m_tag; }

    Optional<AttributeValue> get_attribute(const Attribute&) const;

    void for_each_child(Function<void(const DIE& child)> callback) const;

    bool is_null() const { return m_tag == EntryTag::None; }

    DIE get_die_at_offset(u32 offset) const;

private:
    AttributeValue get_attribute_value(AttributeDataForm form,
        InputMemoryStream& debug_info_stream) const;

    const CompilationUnit& m_compilation_unit;
    u32 m_offset { 0 };
    u32 m_data_offset { 0 };
    size_t m_abbreviation_code { 0 };
    EntryTag m_tag { EntryTag::None };
    bool m_has_children { false };
    u32 m_size { 0 };
};

}
