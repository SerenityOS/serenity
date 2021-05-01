/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AbbreviationsMap.h"
#include <AK/Types.h>

namespace Debug::Dwarf {

class DwarfInfo;
class DIE;

class CompilationUnit {
public:
    CompilationUnit(const DwarfInfo& dwarf_info, u32 offset, const CompilationUnitHeader&);

    u32 offset() const { return m_offset; }
    u32 size() const { return m_header.length() + sizeof(u32); }

    DIE root_die() const;

    const DwarfInfo& dwarf_info() const { return m_dwarf_info; }
    const AbbreviationsMap& abbreviations_map() const { return m_abbreviations; }

private:
    const DwarfInfo& m_dwarf_info;
    u32 m_offset { 0 };
    CompilationUnitHeader m_header;
    AbbreviationsMap m_abbreviations;
};

}
