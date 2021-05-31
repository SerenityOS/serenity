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
    m_debug_info_data = section_data(".debug_info"sv);
    m_abbreviation_data = section_data(".debug_abbrev"sv);
    m_debug_strings_data = section_data(".debug_str"sv);
    m_debug_line_strings_data = section_data(".debug_line_str"sv);

    populate_compilation_units();
}

ReadonlyBytes DwarfInfo::section_data(const StringView& section_name) const
{
    auto section = m_elf.lookup_section(section_name);
    if (!section.has_value())
        return {};
    return section->bytes();
}

void DwarfInfo::populate_compilation_units()
{
    if (!m_debug_info_data.data())
        return;

    InputMemoryStream stream { m_debug_info_data };
    while (!stream.eof()) {
        auto unit_offset = stream.offset();
        CompilationUnitHeader compilation_unit_header {};

        stream >> compilation_unit_header;
        VERIFY(compilation_unit_header.common.version <= 5);
        VERIFY(compilation_unit_header.address_size() == sizeof(u32));

        u32 length_after_header = compilation_unit_header.length() - (compilation_unit_header.header_size() - offsetof(CompilationUnitHeader, common.version));
        m_compilation_units.empend(*this, unit_offset, compilation_unit_header);
        stream.discard_or_error(length_after_header);
    }
}

AttributeValue DwarfInfo::get_attribute_value(AttributeDataForm form, ssize_t implicit_const_value,
    InputMemoryStream& debug_info_stream, const CompilationUnit* unit) const
{
    AttributeValue value;

    auto assign_raw_bytes_value = [&](size_t length) {
        value.data.as_raw_bytes.length = length;
        value.data.as_raw_bytes.bytes = reinterpret_cast<const u8*>(debug_info_data().data()
            + debug_info_stream.offset());

        debug_info_stream.discard_or_error(length);
    };

    switch (form) {
    case AttributeDataForm::StringPointer: {
        u32 offset;
        debug_info_stream >> offset;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::String;

        auto strings_data = debug_strings_data();
        value.data.as_string = reinterpret_cast<const char*>(strings_data.data() + offset);
        break;
    }
    case AttributeDataForm::Data1: {
        u8 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Data2: {
        u16 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Addr: {
        u32 address;
        debug_info_stream >> address;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = address;
        break;
    }
    case AttributeDataForm::SData: {
        ssize_t data;
        debug_info_stream.read_LEB128_signed(data);
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::SignedNumber;
        value.data.as_i32 = data;
        break;
    }
    case AttributeDataForm::UData: {
        size_t data;
        debug_info_stream.read_LEB128_unsigned(data);
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::SecOffset: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::SecOffset;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Data4: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::UnsignedNumber;
        value.data.as_u32 = data;
        break;
    }
    case AttributeDataForm::Data8: {
        u64 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::LongUnsignedNumber;
        value.data.as_u64 = data;
        break;
    }
    case AttributeDataForm::Ref4: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::DieReference;
        VERIFY(unit);
        value.data.as_u32 = data + unit->offset();
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
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::DwarfExpression;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::String: {
        String str;
        u32 str_offset = debug_info_stream.offset();
        debug_info_stream >> str;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::String;
        value.data.as_string = reinterpret_cast<const char*>(str_offset + debug_info_data().data());
        break;
    }
    case AttributeDataForm::Block1: {
        value.type = AttributeValue::Type::RawBytes;
        u8 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block2: {
        value.type = AttributeValue::Type::RawBytes;
        u16 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block4: {
        value.type = AttributeValue::Type::RawBytes;
        u32 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block: {
        value.type = AttributeValue::Type::RawBytes;
        size_t length;
        debug_info_stream.read_LEB128_unsigned(length);
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::LineStrP: {
        u32 offset;
        debug_info_stream >> offset;
        VERIFY(!debug_info_stream.has_any_error());
        value.type = AttributeValue::Type::String;

        auto strings_data = debug_line_strings_data();
        value.data.as_string = reinterpret_cast<const char*>(strings_data.data() + offset);
        break;
    }
    case AttributeDataForm::ImplicitConst: {
        /* Value is part of the abbreviation record. */
        value.type = AttributeValue::Type::SignedNumber;
        value.data.as_i32 = implicit_const_value;
        break;
    }
    default:
        dbgln("Unimplemented AttributeDataForm: {}", (u32)form);
        VERIFY_NOT_REACHED();
    }
    return value;
}

}
