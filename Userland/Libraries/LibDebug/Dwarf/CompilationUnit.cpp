/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CompilationUnit.h"
#include "DIE.h"

namespace Debug::Dwarf {

CompilationUnit::CompilationUnit(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const& header, NonnullOwnPtr<LineProgram>&& line_program)
    : m_dwarf_info(dwarf_info)
    , m_offset(offset)
    , m_header(header)
    , m_abbreviations(dwarf_info, header.abbrev_offset())
    , m_line_program(move(line_program))
{
    VERIFY(header.version() < 5 || header.unit_type() == CompilationUnitType::Full);
}

DIE CompilationUnit::root_die() const
{
    return DIE(*this, m_offset + m_header.header_size());
}

DIE CompilationUnit::get_die_at_offset(u32 die_offset) const
{
    VERIFY(die_offset >= offset() && die_offset < offset() + size());
    return DIE(*this, die_offset);
}

}
