/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CompilationUnit.h"
#include "DwarfInfo.h"
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

    u32 offset() const { return m_offset; }
    u32 size() const { return m_size; }
    bool has_children() const { return m_has_children; }
    EntryTag tag() const { return m_tag; }

    Optional<AttributeValue> get_attribute(const Attribute&) const;

    void for_each_child(Function<void(const DIE& child)> callback) const;

    bool is_null() const { return m_tag == EntryTag::None; }

    DIE get_die_at_offset(u32 offset) const;

private:
    const CompilationUnit& m_compilation_unit;
    u32 m_offset { 0 };
    u32 m_data_offset { 0 };
    size_t m_abbreviation_code { 0 };
    EntryTag m_tag { EntryTag::None };
    bool m_has_children { false };
    u32 m_size { 0 };
};

}
