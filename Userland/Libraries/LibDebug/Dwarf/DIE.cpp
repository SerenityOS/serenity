/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DIE.h"
#include "CompilationUnit.h"
#include "DwarfInfo.h"
#include <AK/ByteBuffer.h>
#include <AK/LEB128.h>
#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

DIE::DIE(CompilationUnit const& unit, u32 offset, Optional<u32> parent_offset)
    : m_compilation_unit(unit)
{
    rehydrate_from(offset, parent_offset).release_value_but_fixme_should_propagate_errors();
}

ErrorOr<void> DIE::rehydrate_from(u32 offset, Optional<u32> parent_offset)
{
    m_offset = offset;

    FixedMemoryStream stream { m_compilation_unit.dwarf_info().debug_info_data() };
    // Note: We can't just slice away from the input data here, since get_attribute_value will try to recover the original offset using seek().
    TRY(stream.seek(m_offset));
    m_abbreviation_code = TRY(stream.read_value<LEB128<size_t>>());
    m_data_offset = TRY(stream.tell());

    if (m_abbreviation_code == 0) {
        // An abbreviation code of 0 ( = null DIE entry) means the end of a chain of siblings
        m_tag = EntryTag::None;
    } else {
        auto abbreviation_info = m_compilation_unit.abbreviations_map().get(m_abbreviation_code);
        VERIFY(abbreviation_info);

        m_tag = abbreviation_info->tag;
        m_has_children = abbreviation_info->has_children;

        // We iterate the attributes data only to calculate this DIE's size
        for (auto& attribute_spec : abbreviation_info->attribute_specifications) {
            TRY(m_compilation_unit.dwarf_info().get_attribute_value(attribute_spec.form, attribute_spec.value, stream, &m_compilation_unit));
        }
    }
    m_size = TRY(stream.tell()) - m_offset;
    m_parent_offset = parent_offset;
    return {};
}

ErrorOr<Optional<AttributeValue>> DIE::get_attribute(Attribute const& attribute) const
{
    FixedMemoryStream stream { m_compilation_unit.dwarf_info().debug_info_data() };
    // Note: We can't just slice away from the input data here, since get_attribute_value will try to recover the original offset using seek().
    TRY(stream.seek(m_data_offset));

    auto abbreviation_info = m_compilation_unit.abbreviations_map().get(m_abbreviation_code);
    VERIFY(abbreviation_info);

    for (auto const& attribute_spec : abbreviation_info->attribute_specifications) {
        auto value = TRY(m_compilation_unit.dwarf_info().get_attribute_value(attribute_spec.form, attribute_spec.value, stream, &m_compilation_unit));
        if (attribute_spec.attribute == attribute) {
            return value;
        }
    }
    return Optional<AttributeValue> {};
}

ErrorOr<void> DIE::for_each_child(Function<ErrorOr<void>(DIE const& child)> callback) const
{
    if (!m_has_children)
        return {};

    auto current_child = DIE(m_compilation_unit, m_offset + m_size, m_offset);
    while (true) {
        TRY(callback(current_child));
        if (current_child.is_null())
            break;
        if (!current_child.has_children()) {
            TRY(current_child.rehydrate_from(current_child.offset() + current_child.size(), m_offset));
            continue;
        }

        auto sibling = TRY(current_child.get_attribute(Attribute::Sibling));
        u32 sibling_offset = 0;
        if (sibling.has_value()) {
            sibling_offset = sibling.value().as_unsigned();
        } else {
            // NOTE: According to the spec, the compiler doesn't have to supply the sibling information.
            // When it doesn't, we have to recursively iterate the current child's children to find where they end
            TRY(current_child.for_each_child([&](DIE const& sub_child) -> ErrorOr<void> {
                sibling_offset = sub_child.offset() + sub_child.size();
                return {};
            }));
        }
        TRY(current_child.rehydrate_from(sibling_offset, m_offset));
    }

    return {};
}

}
