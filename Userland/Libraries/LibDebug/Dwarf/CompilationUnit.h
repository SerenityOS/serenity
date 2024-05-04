/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AbbreviationsMap.h"
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>

namespace Debug::Dwarf {

class DwarfInfo;
class DIE;
class LineProgram;

class CompilationUnit {
    AK_MAKE_NONCOPYABLE(CompilationUnit);
    AK_MAKE_NONMOVABLE(CompilationUnit);

public:
    static ErrorOr<NonnullOwnPtr<CompilationUnit>> create(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const&, ReadonlyBytes debug_line_data);
    ~CompilationUnit();

    u32 offset() const { return m_offset; }
    u32 size() const { return m_header.length() + sizeof(u32); }

    DIE root_die() const;
    DIE get_die_at_offset(u32 offset) const;

    ErrorOr<FlatPtr> get_address(size_t index) const;
    ErrorOr<char const*> get_string(size_t index) const;

    u8 dwarf_version() const { return m_header.version(); }

    DwarfInfo const& dwarf_info() const { return m_dwarf_info; }
    AbbreviationsMap const& abbreviations_map() const { return m_abbreviations; }
    LineProgram const& line_program() const;
    ErrorOr<Optional<FlatPtr>> base_address() const;

    // DW_AT_addr_base
    ErrorOr<u64> address_table_base() const;
    // DW_AT_str_offsets_base
    ErrorOr<u64> string_offsets_base() const;
    // DW_AT_rnglists_base
    ErrorOr<u64> range_lists_base() const;

private:
    CompilationUnit(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const&);
    ErrorOr<void> populate_line_program(ReadonlyBytes debug_line_data);

    DwarfInfo const& m_dwarf_info;
    u32 m_offset { 0 };
    CompilationUnitHeader m_header;
    AbbreviationsMap m_abbreviations;
    OwnPtr<LineProgram> m_line_program;
    mutable bool m_has_cached_base_address : 1 { false };
    mutable bool m_has_cached_address_table_base : 1 { false };
    mutable bool m_has_cached_string_offsets_base : 1 { false };
    mutable bool m_has_cached_range_lists_base : 1 { false };
    mutable Optional<FlatPtr> m_cached_base_address;
    mutable u64 m_cached_address_table_base { 0 };
    mutable u64 m_cached_string_offsets_base { 0 };
    mutable u64 m_cached_range_lists_base { 0 };
};

}
