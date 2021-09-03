/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AbbreviationsMap.h"
#include "DIE.h"
#include "LineProgram.h"
#include <YAK/Noncopyable.h>
#include <YAK/Types.h>

namespace Debug::Dwarf {

class DwarfInfo;
class DIE;
class LineProgram;

class CompilationUnit {
    YAK_MAKE_NONCOPYABLE(CompilationUnit);
    YAK_MAKE_NONMOVABLE(CompilationUnit);

public:
    CompilationUnit(DwarfInfo const& dwarf_info, u32 offset, CompilationUnitHeader const&, NonnullOwnPtr<LineProgram>&& line_program);

    u32 offset() const { return m_offset; }
    u32 size() const { return m_header.length() + sizeof(u32); }

    DIE root_die() const;
    DIE get_die_at_offset(u32 offset) const;

    DwarfInfo const& dwarf_info() const { return m_dwarf_info; }
    AbbreviationsMap const& abbreviations_map() const { return m_abbreviations; }
    LineProgram const& line_program() const { return *m_line_program; }

private:
    DwarfInfo const& m_dwarf_info;
    u32 m_offset { 0 };
    CompilationUnitHeader m_header;
    AbbreviationsMap m_abbreviations;
    NonnullOwnPtr<LineProgram> m_line_program;
};

}
