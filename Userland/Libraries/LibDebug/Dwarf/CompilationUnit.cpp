/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CompilationUnit.h"
#include "DIE.h"

namespace Debug::Dwarf {

CompilationUnit::CompilationUnit(const DwarfInfo& dwarf_info, u32 offset, const CompilationUnitHeader& header)
    : m_dwarf_info(dwarf_info)
    , m_offset(offset)
    , m_header(header)
    , m_abbreviations(dwarf_info, header.abbrev_offset())
{
    VERIFY(header.version() < 5 || header.unit_type() == CompilationUnitType::Full);
}

DIE CompilationUnit::root_die() const
{
    return DIE(*this, m_offset + m_header.header_size());
}

}
