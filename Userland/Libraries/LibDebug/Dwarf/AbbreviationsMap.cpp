/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AbbreviationsMap.h"
#include "DwarfInfo.h"

#include <AK/LEB128.h>
#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

AbbreviationsMap::AbbreviationsMap(DwarfInfo const& dwarf_info, u32 offset)
    : m_dwarf_info(dwarf_info)
    , m_offset(offset)
{
    populate_map().release_value_but_fixme_should_propagate_errors();
}

ErrorOr<void> AbbreviationsMap::populate_map()
{
    FixedMemoryStream abbreviation_stream { m_dwarf_info.abbreviation_data() };
    TRY(abbreviation_stream.discard(m_offset));

    while (!abbreviation_stream.is_eof()) {
        size_t abbreviation_code = TRY(abbreviation_stream.read_value<LEB128<size_t>>());
        // An abbreviation code of 0 marks the end of the
        // abbreviations for a given compilation unit
        if (abbreviation_code == 0)
            break;

        size_t tag = TRY(abbreviation_stream.read_value<LEB128<size_t>>());

        auto has_children = TRY(abbreviation_stream.read_value<u8>());

        AbbreviationEntry abbreviation_entry {};
        abbreviation_entry.tag = static_cast<EntryTag>(tag);
        abbreviation_entry.has_children = (has_children == 1);

        AttributeSpecification current_attribute_specification {};
        do {
            size_t attribute_value = TRY(abbreviation_stream.read_value<LEB128<size_t>>());
            size_t form_value = TRY(abbreviation_stream.read_value<LEB128<size_t>>());

            current_attribute_specification.attribute = static_cast<Attribute>(attribute_value);
            current_attribute_specification.form = static_cast<AttributeDataForm>(form_value);

            if (current_attribute_specification.form == AttributeDataForm::ImplicitConst) {
                ssize_t data_value = TRY(abbreviation_stream.read_value<LEB128<ssize_t>>());
                current_attribute_specification.value = data_value;
            }

            if (current_attribute_specification.attribute != Attribute::None) {
                abbreviation_entry.attribute_specifications.append(current_attribute_specification);
            }
        } while (current_attribute_specification.attribute != Attribute::None || current_attribute_specification.form != AttributeDataForm::None);

        m_entries.set(static_cast<u32>(abbreviation_code), move(abbreviation_entry));
    }

    return {};
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
