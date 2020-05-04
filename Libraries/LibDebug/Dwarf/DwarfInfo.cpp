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

#include "DwarfInfo.h"

namespace Dwarf {

DwarfInfo::DwarfInfo(NonnullRefPtr<const ELF::Loader> elf)
    : m_elf(elf)
{
    m_debug_info_data = section_data(".debug_info");
    m_abbreviation_data = section_data(".debug_abbrev");
    m_debug_strings_data = section_data(".debug_str");

    populate_compilation_units();
}

ByteBuffer DwarfInfo::section_data(const String& section_name)
{
    auto section = m_elf->image().lookup_section(section_name);
    ASSERT(!section.is_undefined());
    return ByteBuffer::wrap(reinterpret_cast<const u8*>(section.raw_data()), section.size());
}

void DwarfInfo::populate_compilation_units()
{
    // We have to const_cast here because there isn't a version of
    // BufferStream that accepts a const ByteStream
    // We take care not to use BufferStream operations that modify the underlying buffer
    // TOOD: Add a variant of BufferStream that operates on a const ByteBuffer to AK
    BufferStream stream(const_cast<ByteBuffer&>(m_debug_info_data));
    while (!stream.at_end()) {
        auto unit_offset = stream.offset();
        CompilationUnitHeader compilation_unit_header {};

        stream.read_raw(reinterpret_cast<u8*>(&compilation_unit_header), sizeof(CompilationUnitHeader));
        ASSERT(compilation_unit_header.address_size == sizeof(u32));
        ASSERT(compilation_unit_header.version == 4);

        u32 length_after_header = compilation_unit_header.length - (sizeof(CompilationUnitHeader) - offsetof(CompilationUnitHeader, version));
        m_compilation_units.empend(*this, unit_offset, compilation_unit_header);
        stream.advance(length_after_header);
    }
}

}
