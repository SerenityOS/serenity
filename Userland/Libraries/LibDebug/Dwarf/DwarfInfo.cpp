/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DwarfInfo.h"

#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

DwarfInfo::DwarfInfo(const ELF::Image& elf)
    : m_elf(elf)
{
    m_debug_info_data = section_data(".debug_info");
    m_abbreviation_data = section_data(".debug_abbrev");
    m_debug_strings_data = section_data(".debug_str");

    populate_compilation_units();
}

ReadonlyBytes DwarfInfo::section_data(const String& section_name) const
{
    auto section = m_elf.lookup_section(section_name);
    if (section.is_undefined())
        return {};
    return section.bytes();
}

void DwarfInfo::populate_compilation_units()
{
    if (!m_debug_info_data.data())
        return;

    InputMemoryStream stream { m_debug_info_data };
    while (!stream.eof()) {
        auto unit_offset = stream.offset();
        CompilationUnitHeader compilation_unit_header {};

        stream >> Bytes { &compilation_unit_header, sizeof(compilation_unit_header) };
        VERIFY(compilation_unit_header.address_size == sizeof(u32));
        VERIFY(compilation_unit_header.version <= 4);

        u32 length_after_header = compilation_unit_header.length - (sizeof(CompilationUnitHeader) - offsetof(CompilationUnitHeader, version));
        m_compilation_units.empend(*this, unit_offset, compilation_unit_header);
        stream.discard_or_error(length_after_header);
    }
}

}
