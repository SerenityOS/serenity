/*
 * Copyright (c) 2020-2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DwarfInfo.h"
#include "AddressRanges.h"
#include "AttributeValue.h"
#include "CompilationUnit.h"

#include <AK/ByteReader.h>
#include <AK/MemoryStream.h>
#include <LibDebug/DebugInfo.h>

namespace Debug::Dwarf {

DwarfInfo::DwarfInfo(ELF::Image const& elf)
    : m_elf(elf)
{
    m_debug_info_data = section_data(".debug_info"sv);
    m_abbreviation_data = section_data(".debug_abbrev"sv);
    m_debug_strings_data = section_data(".debug_str"sv);
    m_debug_line_data = section_data(".debug_line"sv);
    m_debug_line_strings_data = section_data(".debug_line_str"sv);
    m_debug_range_lists_data = section_data(".debug_rnglists"sv);
    m_debug_str_offsets_data = section_data(".debug_str_offsets"sv);
    m_debug_addr_data = section_data(".debug_addr"sv);
    m_debug_ranges_data = section_data(".debug_ranges"sv);

    populate_compilation_units();
}

ReadonlyBytes DwarfInfo::section_data(StringView section_name) const
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

    InputMemoryStream debug_info_stream { m_debug_info_data };
    InputMemoryStream line_info_stream { m_debug_line_data };

    while (!debug_info_stream.eof()) {
        auto unit_offset = debug_info_stream.offset();
        CompilationUnitHeader compilation_unit_header {};

        debug_info_stream >> compilation_unit_header;
        VERIFY(compilation_unit_header.common.version <= 5);
        VERIFY(compilation_unit_header.address_size() == sizeof(FlatPtr));

        u32 length_after_header = compilation_unit_header.length() - (compilation_unit_header.header_size() - offsetof(CompilationUnitHeader, common.version));

        auto line_program = make<LineProgram>(*this, line_info_stream);

        // HACK: Clang generates line programs for embedded resource assembly files, but not compile units.
        // Meaning that for graphical applications, some line info data would be unread, triggering the assertion below.
        // As a fix, we don't create compilation units for line programs that come from resource files.
#ifdef __clang__
        if (line_program->source_files().size() == 1 && line_program->source_files()[0].name.view().contains("serenity_icon_"sv)) {
            debug_info_stream.seek(unit_offset);
        } else
#endif
        {
            m_compilation_units.append(make<CompilationUnit>(*this, unit_offset, compilation_unit_header, move(line_program)));
            debug_info_stream.discard_or_error(length_after_header);
        }
    }

    VERIFY(line_info_stream.eof());
}

AttributeValue DwarfInfo::get_attribute_value(AttributeDataForm form, ssize_t implicit_const_value,
    InputMemoryStream& debug_info_stream, const CompilationUnit* unit) const
{
    AttributeValue value;
    value.m_form = form;
    value.m_compilation_unit = unit;

    auto assign_raw_bytes_value = [&](size_t length) {
        value.m_data.as_raw_bytes = { debug_info_data().offset_pointer(debug_info_stream.offset()), length };

        debug_info_stream.discard_or_error(length);
        VERIFY(!debug_info_stream.has_any_error());
    };

    switch (form) {
    case AttributeDataForm::StringPointer: {
        u32 offset;
        debug_info_stream >> offset;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;

        auto strings_data = debug_strings_data();
        value.m_data.as_string = bit_cast<const char*>(strings_data.offset_pointer(offset));
        break;
    }
    case AttributeDataForm::Data1: {
        u8 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data2: {
        u16 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_signed = data;
        break;
    }
    case AttributeDataForm::Addr: {
        FlatPtr address;
        debug_info_stream >> address;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_addr = address;
        break;
    }
    case AttributeDataForm::SData: {
        i64 data;
        debug_info_stream.read_LEB128_signed(data);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::SignedNumber;
        value.m_data.as_signed = data;
        break;
    }
    case AttributeDataForm::UData: {
        u64 data;
        debug_info_stream.read_LEB128_unsigned(data);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::SecOffset: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::SecOffset;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data4: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data8: {
        u64 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data16: {
        value.m_type = AttributeValue::Type::RawBytes;
        assign_raw_bytes_value(16);
        VERIFY(!debug_info_stream.has_any_error());
        break;
    }
    case AttributeDataForm::Ref4: {
        u32 data;
        debug_info_stream >> data;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::DieReference;
        VERIFY(unit);
        value.m_data.as_unsigned = data + unit->offset();
        break;
    }
    case AttributeDataForm::FlagPresent: {
        value.m_type = AttributeValue::Type::Boolean;
        value.m_data.as_bool = true;
        break;
    }
    case AttributeDataForm::ExprLoc: {
        size_t length;
        debug_info_stream.read_LEB128_unsigned(length);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::DwarfExpression;
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::String: {
        String str;
        u32 str_offset = debug_info_stream.offset();
        debug_info_stream >> str;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_string = bit_cast<const char*>(debug_info_data().offset_pointer(str_offset));
        break;
    }
    case AttributeDataForm::Block1: {
        value.m_type = AttributeValue::Type::RawBytes;
        u8 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block2: {
        value.m_type = AttributeValue::Type::RawBytes;
        u16 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block4: {
        value.m_type = AttributeValue::Type::RawBytes;
        u32 length;
        debug_info_stream >> length;
        VERIFY(!debug_info_stream.has_any_error());
        assign_raw_bytes_value(length);
        break;
    }
    case AttributeDataForm::Block: {
        value.m_type = AttributeValue::Type::RawBytes;
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
        value.m_type = AttributeValue::Type::String;

        auto strings_data = debug_line_strings_data();
        value.m_data.as_string = bit_cast<const char*>(strings_data.offset_pointer(offset));
        break;
    }
    case AttributeDataForm::ImplicitConst: {
        /* Value is part of the abbreviation record. */
        value.m_type = AttributeValue::Type::SignedNumber;
        value.m_data.as_signed = implicit_const_value;
        break;
    }
    case AttributeDataForm::StrX1: {
        u8 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX2: {
        u16 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX4: {
        u32 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX: {
        size_t index;
        debug_info_stream.read_LEB128_unsigned(index);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX1: {
        u8 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX2: {
        u16 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX4: {
        u32 index;
        debug_info_stream >> index;
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX: {
        size_t index;
        debug_info_stream.read_LEB128_unsigned(index);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::RngListX: {
        size_t index;
        debug_info_stream.read_LEB128_unsigned(index);
        VERIFY(!debug_info_stream.has_any_error());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = index;
        break;
    }
    default:
        dbgln("Unimplemented AttributeDataForm: {}", to_underlying(form));
        VERIFY_NOT_REACHED();
    }
    return value;
}

void DwarfInfo::build_cached_dies() const
{
    auto insert_to_cache = [this](DIE const& die, DIERange const& range) {
        m_cached_dies_by_range.insert(range.start_address, DIEAndRange { die, range });
        m_cached_dies_by_offset.insert(die.offset(), die);
    };
    auto get_ranges_of_die = [this](DIE const& die) -> Vector<DIERange> {
        auto ranges = die.get_attribute(Attribute::Ranges);
        if (ranges.has_value()) {
            size_t offset;
            if (ranges->form() == AttributeDataForm::SecOffset) {
                offset = ranges->as_unsigned();
            } else {
                auto index = ranges->as_unsigned();
                auto base = die.compilation_unit().range_lists_base();
                // FIXME: This assumes that the format is DWARf32
                auto offsets = debug_range_lists_data().slice(base);
                offset = ByteReader::load32(offsets.offset_pointer(index * sizeof(u32))) + base;
            }

            Vector<DIERange> entries;
            if (die.compilation_unit().dwarf_version() == 5) {
                AddressRangesV5 address_ranges(debug_range_lists_data(), offset, die.compilation_unit());
                address_ranges.for_each_range([&entries](auto range) {
                    entries.empend(range.start, range.end);
                });
            } else {
                AddressRangesV4 address_ranges(debug_ranges_data(), offset, die.compilation_unit());
                address_ranges.for_each_range([&entries](auto range) {
                    entries.empend(range.start, range.end);
                });
            }
            return entries;
        }

        auto start = die.get_attribute(Attribute::LowPc);
        auto end = die.get_attribute(Attribute::HighPc);

        if (!start.has_value() || !end.has_value())
            return {};

        VERIFY(start->type() == Dwarf::AttributeValue::Type::Address);

        // DW_AT_high_pc attribute can have different meanings depending on the attribute form.
        // (Dwarf version 5, section 2.17.2).

        uint32_t range_end = 0;
        if (end->form() == Dwarf::AttributeDataForm::Addr)
            range_end = end->as_addr();
        else
            range_end = start->as_addr() + end->as_unsigned();

        return { DIERange { start.value().as_addr(), range_end } };
    };

    // If we simply use a lambda, type deduction fails because it's used recursively.
    Function<void(DIE const& die)> insert_to_cache_recursively;
    insert_to_cache_recursively = [&](DIE const& die) {
        if (die.offset() == 0 || die.parent_offset().has_value()) {
            auto ranges = get_ranges_of_die(die);
            for (auto& range : ranges) {
                insert_to_cache(die, range);
            }
        }
        die.for_each_child([&](DIE const& child) {
            if (!child.is_null()) {
                insert_to_cache_recursively(child);
            }
        });
    };

    for_each_compilation_unit([&](CompilationUnit const& compilation_unit) {
        insert_to_cache_recursively(compilation_unit.root_die());
    });

    m_built_cached_dies = true;
}

Optional<DIE> DwarfInfo::get_die_at_address(FlatPtr address) const
{
    if (!m_built_cached_dies)
        build_cached_dies();

    auto iter = m_cached_dies_by_range.find_largest_not_above_iterator(address);
    while (!iter.is_end() && !iter.is_begin() && iter->range.end_address < address) {
        --iter;
    }

    if (iter.is_end())
        return {};

    if (iter->range.start_address > address || iter->range.end_address < address) {
        return {};
    }

    return iter->die;
}

Optional<DIE> DwarfInfo::get_cached_die_at_offset(FlatPtr offset) const
{
    if (!m_built_cached_dies)
        build_cached_dies();

    auto* die = m_cached_dies_by_offset.find(offset);
    if (!die)
        return {};
    return *die;
}

}
