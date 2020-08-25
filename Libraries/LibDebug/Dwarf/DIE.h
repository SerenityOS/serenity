/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        enum class Type {
            UnsignedNumber,
            SignedNumber,
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
