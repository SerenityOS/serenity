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

#include "DIE.h"
#include "CompilationUnit.h"
#include "DwarfInfo.h"
#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>

namespace Debug::Dwarf {

DIE::DIE(const CompilationUnit& unit, u32 offset)
    : m_compilation_unit(unit)
    , m_offset(offset)
{
    InputMemoryStream stream(m_compilation_unit.dwarf_info().debug_info_data());
    stream.discard_or_error(m_offset);
    stream.read_LEB128_unsigned(m_abbreviation_code);
    m_data_offset = stream.offset();

    if (m_abbreviation_code == 0) {
        // An abbreviation code of 0 ( = null DIE entry) means the end of a chain of siblings
        m_tag = EntryTag::None;
    } else {
        auto abbreviation_info = m_compilation_unit.abbreviations_map().get(m_abbreviation_code);
        ASSERT(abbreviation_info.has_value());

        m_tag = abbreviation_info.value().tag;
        m_has_children = abbreviation_info.value().has_children;

        // We iterate the attributes data only to calculate this DIE's size
        for (auto attribute_spec : abbreviation_info.value().attribute_specifications) {
            get_attribute_value(attribute_spec.form, stream);
        }
    }
    m_size = stream.offset() - m_offset;
}

DIE::AttributeValue DIE::get_attribute_value(AttributeDataForm form,
    InputMemoryStream& debug_info_stream) const
{
    AttributeValue value;

    auto assign_raw_bytes_value = [&](size_t length) {
        value.data.as_raw_bytes.length = length;
        value.data.as_raw_bytes.bytes = reinterpret_cast<const u8*>(m_compilation_unit.dwarf_info().debug_info_data().data()
            + debug_info_stream.offset());

        debug_info_stream.discard_or_error(length);
    };

    switch (form) {
    case AttributeDataForm::StringPointer: {
        u32 offset;
        debug_info_stream >> offset;
        value.type = AttributeValue::Type::String;

        auto strings_data = m_compilation_unit.dwarf_info().debug_strings_data();
        value.data.as_string = reinterpret_cast<const char*>(strings_data.data() + offset);
        break;
    }
    case AttributeDataForm::Data1: {
        u8 data;
        debug_info_stream >> data;
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Data2: {
        u16 data;
        debug_info_stream >> data;
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Addr: {
        u32 address;
        debug_info_stream >> address;
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = address;
        break;
    }
    case AttributeDataForm::SecOffset: {
        u32 data;
        debug_info_stream >> data;
        value.type = AttributeValue::Type::SecOffset;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Data4: {
        u32 data;
        debug_info_stream >> data;
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Ref4: {
        u32 data;
        debug_info_stream >> data;
        value.type = AttributeValue::Type::DieReference;
        value.data.as_u32 = data + m_compilation_unit.offset();
        break;
    }
    case AttributeDataForm::FlagPresent: {
        value.type = AttributeValue::Type::Boolean;
        value.data.as_bool = true;
        break;
    }
    case AttributeDataForm::ExprLoc: {
        size_t length;
        debug_info_stream.read_LEB128_unsigned(length);
        value.type = AttributeValue::Type::DwarfExpression;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::String: {
        String str;
        u32 str_offset = debug_info_stream.offset();
        debug_info_stream >> str;
        value.type = AttributeValue::Type::String;
        value.data.as_string = reinterpret_cast<const char*>(str_offset + m_compilation_unit.dwarf_info().debug_info_data().data());
        break;
    }
    case AttributeDataForm::Block1: {
        value.type = AttributeValue::Type::RawBytes;
        u8 length;
        debug_info_stream >> length;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block2: {
        value.type = AttributeValue::Type::RawBytes;
        u16 length;
        debug_info_stream >> length;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block4: {
        value.type = AttributeValue::Type::RawBytes;
        u32 length;
        debug_info_stream >> length;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block: {
        value.type = AttributeValue::Type::RawBytes;
        size_t length;
        debug_info_stream.read_LEB128_unsigned(length);
        assign_raw_bytes_value(length);
        break;
    }
    default:
        dbg() << "Unimplemented AttributeDataForm: " << (u32)form;
        ASSERT_NOT_REACHED();
    }
    return value;
}

Optional<DIE::AttributeValue> DIE::get_attribute(const Attribute& attribute) const
{
    InputMemoryStream stream { m_compilation_unit.dwarf_info().debug_info_data() };
    stream.discard_or_error(m_data_offset);

    auto abbreviation_info = m_compilation_unit.abbreviations_map().get(m_abbreviation_code);
    ASSERT(abbreviation_info.has_value());

    for (const auto& attribute_spec : abbreviation_info.value().attribute_specifications) {
        auto value = get_attribute_value(attribute_spec.form, stream);
        if (attribute_spec.attribute == attribute) {
            return value;
        }
    }
    return {};
}

void DIE::for_each_child(Function<void(const DIE& child)> callback) const
{
    if (!m_has_children)
        return;

    NonnullOwnPtr<DIE> current_child = make<DIE>(m_compilation_unit, m_offset + m_size);
    while (true) {
        callback(*current_child);
        if (current_child->is_null())
            break;
        if (!current_child->has_children()) {
            current_child = make<DIE>(m_compilation_unit, current_child->offset() + current_child->size());
            continue;
        }

        auto sibling = current_child->get_attribute(Attribute::Sibling);
        u32 sibling_offset = 0;
        if (sibling.has_value()) {
            sibling_offset = sibling.value().data.as_u32;
        }

        if (!sibling.has_value()) {
            // NOTE: According to the spec, the compiler doesn't have to supply the sibling information.
            // When it doesn't, we have to recursively iterate the current child's children to find where they end
            current_child->for_each_child([&](const DIE& sub_child) {
                sibling_offset = sub_child.offset() + sub_child.size();
            });
        }
        current_child = make<DIE>(m_compilation_unit, sibling_offset);
    }
}

DIE DIE::get_die_at_offset(u32 offset) const
{
    ASSERT(offset >= m_compilation_unit.offset() && offset < m_compilation_unit.offset() + m_compilation_unit.size());
    return DIE(m_compilation_unit, offset);
}

}
