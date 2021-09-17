/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AbbreviationsMap.h"
#include "DwarfInfo.h"

#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

AbbreviationsMap::AbbreviationsMap(DwarfInfo const& dwarf_info, u32 offset)
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

            if (current_attribute_specification.form == AttributeDataForm::ImplicitConst) {
                ssize_t data_value;
                abbreviation_stream.read_LEB128_signed(data_value);
                current_attribute_specification.value = data_value;
            }

            if (current_attribute_specification.attribute != Attribute::None) {
                abbrevation_entry.attribute_specifications.append(current_attribute_specification);
            }
        } while (current_attribute_specification.attribute != Attribute::None || current_attribute_specification.form != AttributeDataForm::None);

        m_entries.set((u32)abbreviation_code, move(abbrevation_entry));
    }
}

AbbreviationsMap::AbbreviationEntry const* AbbreviationsMap::get(u32 code) const
{
    auto it = m_entries.find(code);
    if (it == m_entries.end()) {
        return nullptr;
    }
    return &it->value;
}

}
