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

#include "AbbreviationsMap.h"
#include "DwarfInfo.h"

#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

AbbreviationsMap::AbbreviationsMap(const DwarfInfo& dwarf_info, u32 offset)
    : m_dwarf_info(dwarf_info)
    , m_offset(offset)
{
    populate_map();
}

void AbbreviationsMap::populate_map()
{
    InputMemoryStream abbreviation_stream(m_dwarf_info.abbreviation_data());
    abbreviation_stream.discard_or_error(m_offset);

    while (!abbreviation_stream.eof()) {
        size_t abbreviation_code = 0;
        abbreviation_stream.read_LEB128_unsigned(abbreviation_code);
        // An abbreviation code of 0 marks the end of the
        // abbreviations for a given compilation unit
        if (abbreviation_code == 0)
            break;

        size_t tag {};
        abbreviation_stream.read_LEB128_unsigned(tag);

        u8 has_children = 0;
        abbreviation_stream >> has_children;

        AbbreviationEntry abbrevation_entry {};
        abbrevation_entry.tag = static_cast<EntryTag>(tag);
        abbrevation_entry.has_children = (has_children == 1);

        AttributeSpecification current_attribute_specification {};
        do {
            size_t attribute_value = 0;
            size_t form_value = 0;
            abbreviation_stream.read_LEB128_unsigned(attribute_value);
            abbreviation_stream.read_LEB128_unsigned(form_value);

            current_attribute_specification.attribute = static_cast<Attribute>(attribute_value);
            current_attribute_specification.form = static_cast<AttributeDataForm>(form_value);

            if (current_attribute_specification.attribute != Attribute::None) {
                abbrevation_entry.attribute_specifications.append(current_attribute_specification);
            }
        } while (current_attribute_specification.attribute != Attribute::None || current_attribute_specification.form != AttributeDataForm::None);

        m_entries.set((u32)abbreviation_code, abbrevation_entry);
    }
}

Optional<AbbreviationsMap::AbbreviationEntry> AbbreviationsMap::get(u32 code) const
{
    return m_entries.get(code);
}

}
