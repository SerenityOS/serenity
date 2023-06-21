/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AttributeValue.h"
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
    DIE(CompilationUnit const&, u32 offset, Optional<u32> parent_offset = {});

    u32 offset() const { return m_offset; }
    u32 size() const { return m_size; }
    bool has_children() const { return m_has_children; }
    EntryTag tag() const { return m_tag; }

    ErrorOr<Optional<AttributeValue>> get_attribute(Attribute const&) const;

    ErrorOr<void> for_each_child(Function<ErrorOr<void>(DIE const& child)> callback) const;

    bool is_null() const { return m_tag == EntryTag::None; }
    CompilationUnit const& compilation_unit() const { return m_compilation_unit; }
    Optional<u32> parent_offset() const { return m_parent_offset; }

private:
    ErrorOr<void> rehydrate_from(u32 offset, Optional<u32> parent_offset);
    CompilationUnit const& m_compilation_unit;
    u32 m_offset { 0 };
    u32 m_data_offset { 0 };
    size_t m_abbreviation_code { 0 };
    EntryTag m_tag { EntryTag::None };
    bool m_has_children { false };
    u32 m_size { 0 };
    Optional<u32> m_parent_offset;
};

}
