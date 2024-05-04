/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CompilationUnit.h"
#include <AK/ByteReader.h>
#include <AK/MemoryStream.h>
#include <LibDebug/Dwarf/DIE.h>
#include <LibDebug/Dwarf/DwarfInfo.h>
#include <LibDebug/Dwarf/LineProgram.h>

namespace Debug::Dwarf {

CompilationUnit::CompilationUnit(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const& header)
    : m_dwarf_info(dwarf_info)
    , m_offset(offset)
    , m_header(header)
    , m_abbreviations(dwarf_info, header.abbrev_offset())
{
    VERIFY(header.version() < 5 || header.unit_type() == CompilationUnitType::Full);
}

ErrorOr<NonnullOwnPtr<CompilationUnit>> CompilationUnit::create(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const& header, ReadonlyBytes debug_line_data)
{
    auto compilation_unit = TRY(adopt_nonnull_own_or_enomem(new (nothrow) CompilationUnit(dwarf_info, offset, header)));
    TRY(compilation_unit->populate_line_program(debug_line_data));
    return compilation_unit;
}

ErrorOr<void> CompilationUnit::populate_line_program(ReadonlyBytes debug_line_data)
{
    auto die = root_die();

    auto res = TRY(die.get_attribute(Attribute::StmtList));
    if (!res.has_value())
        return EINVAL;
    VERIFY(res->form() == AttributeDataForm::SecOffset);

    FixedMemoryStream debug_line_stream { debug_line_data };
    TRY(debug_line_stream.seek(res->as_unsigned()));

    m_line_program = TRY(LineProgram::create(m_dwarf_info, debug_line_stream));

    return {};
}

CompilationUnit::~CompilationUnit() = default;

DIE CompilationUnit::root_die() const
{
    return DIE(*this, m_offset + m_header.header_size());
}

DIE CompilationUnit::get_die_at_offset(u32 die_offset) const
{
    VERIFY(die_offset >= offset() && die_offset < offset() + size());
    return DIE(*this, die_offset);
}

LineProgram const& CompilationUnit::line_program() const
{
    return *m_line_program;
}

ErrorOr<Optional<FlatPtr>> CompilationUnit::base_address() const
{
    if (m_has_cached_base_address)
        return m_cached_base_address;

    auto die = root_die();
    auto res = TRY(die.get_attribute(Attribute::LowPc));
    if (res.has_value()) {
        m_cached_base_address = TRY(res->as_addr());
    }
    m_has_cached_base_address = true;
    return m_cached_base_address;
}

ErrorOr<u64> CompilationUnit::address_table_base() const
{
    if (m_has_cached_address_table_base)
        return m_cached_address_table_base;

    auto die = root_die();
    auto res = TRY(die.get_attribute(Attribute::AddrBase));
    if (res.has_value()) {
        VERIFY(res->form() == AttributeDataForm::SecOffset);
        m_cached_address_table_base = res->as_unsigned();
    }
    m_has_cached_address_table_base = true;
    return m_cached_address_table_base;
}

ErrorOr<u64> CompilationUnit::string_offsets_base() const
{
    if (m_has_cached_string_offsets_base)
        return m_cached_string_offsets_base;

    auto die = root_die();
    auto res = TRY(die.get_attribute(Attribute::StrOffsetsBase));
    if (res.has_value()) {
        VERIFY(res->form() == AttributeDataForm::SecOffset);
        m_cached_string_offsets_base = res->as_unsigned();
    }
    m_has_cached_string_offsets_base = true;
    return m_cached_string_offsets_base;
}

ErrorOr<u64> CompilationUnit::range_lists_base() const
{
    if (m_has_cached_range_lists_base)
        return m_cached_range_lists_base;

    auto die = root_die();
    auto res = TRY(die.get_attribute(Attribute::RngListsBase));
    if (res.has_value()) {
        VERIFY(res->form() == AttributeDataForm::SecOffset);
        m_cached_range_lists_base = res->as_unsigned();
    }
    m_has_cached_range_lists_base = true;
    return m_cached_range_lists_base;
}

ErrorOr<FlatPtr> CompilationUnit::get_address(size_t index) const
{
    auto base = TRY(address_table_base());
    auto debug_addr_data = dwarf_info().debug_addr_data();
    VERIFY(base < debug_addr_data.size());
    auto addresses = debug_addr_data.slice(base);
    VERIFY(index * sizeof(FlatPtr) < addresses.size());
    FlatPtr value { 0 };
    ByteReader::load<FlatPtr>(addresses.offset_pointer(index * sizeof(FlatPtr)), value);
    return value;
}

ErrorOr<char const*> CompilationUnit::get_string(size_t index) const
{
    auto base = TRY(string_offsets_base());
    auto debug_str_offsets_data = dwarf_info().debug_str_offsets_data();
    VERIFY(base < debug_str_offsets_data.size());
    // FIXME: This assumes DWARF32
    auto offsets = debug_str_offsets_data.slice(base);
    VERIFY(index * sizeof(u32) < offsets.size());
    auto offset = ByteReader::load32(offsets.offset_pointer(index * sizeof(u32)));
    return bit_cast<char const*>(dwarf_info().debug_strings_data().offset(offset));
}
}
