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
#include <AK/LEB128.h>
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

    populate_compilation_units().release_value_but_fixme_should_propagate_errors();
}

DwarfInfo::~DwarfInfo() = default;

ReadonlyBytes DwarfInfo::section_data(StringView section_name) const
{
    auto section = m_elf.lookup_section(section_name);
    if (!section.has_value())
        return {};
    return section->bytes();
}

ErrorOr<void> DwarfInfo::populate_compilation_units()
{
    if (!m_debug_info_data.data())
        return {};

    FixedMemoryStream debug_info_stream { m_debug_info_data };

    while (!debug_info_stream.is_eof()) {
        auto unit_offset = TRY(debug_info_stream.tell());

        auto compilation_unit_header = TRY(debug_info_stream.read_value<CompilationUnitHeader>());
        VERIFY(compilation_unit_header.common.version <= 5);
        VERIFY(compilation_unit_header.address_size() == sizeof(FlatPtr));

        u32 length_after_header = compilation_unit_header.length() - (compilation_unit_header.header_size() - offsetof(CompilationUnitHeader, common.version));

        m_compilation_units.append(TRY(CompilationUnit::create(*this, unit_offset, compilation_unit_header, m_debug_line_data)));
        TRY(debug_info_stream.discard(length_after_header));
    }

    return {};
}

ErrorOr<AttributeValue> DwarfInfo::get_attribute_value(AttributeDataForm form, ssize_t implicit_const_value,
    SeekableStream& debug_info_stream, CompilationUnit const* unit) const
{
    AttributeValue value;
    value.m_form = form;
    value.m_compilation_unit = unit;

    auto assign_raw_bytes_value = [&](size_t length) -> ErrorOr<void> {
        value.m_data.as_raw_bytes = { debug_info_data().offset_pointer(TRY(debug_info_stream.tell())), length };
        TRY(debug_info_stream.discard(length));
        return {};
    };

    switch (form) {
    case AttributeDataForm::StringPointer: {
        auto offset = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::String;

        auto strings_data = debug_strings_data();
        value.m_data.as_string = bit_cast<char const*>(strings_data.offset_pointer(offset));
        break;
    }
    case AttributeDataForm::Data1: {
        auto data = TRY(debug_info_stream.read_value<u8>());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data2: {
        auto data = TRY(debug_info_stream.read_value<u16>());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_signed = data;
        break;
    }
    case AttributeDataForm::Addr: {
        auto address = TRY(debug_info_stream.read_value<FlatPtr>());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_addr = address;
        break;
    }
    case AttributeDataForm::SData: {
        i64 data = TRY(debug_info_stream.read_value<LEB128<i64>>());
        value.m_type = AttributeValue::Type::SignedNumber;
        value.m_data.as_signed = data;
        break;
    }
    case AttributeDataForm::UData: {
        u64 data = TRY(debug_info_stream.read_value<LEB128<u64>>());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::SecOffset: {
        auto data = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::SecOffset;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data4: {
        auto data = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data8: {
        auto data = TRY(debug_info_stream.read_value<u64>());
        value.m_type = AttributeValue::Type::UnsignedNumber;
        value.m_data.as_unsigned = data;
        break;
    }
    case AttributeDataForm::Data16: {
        value.m_type = AttributeValue::Type::RawBytes;
        TRY(assign_raw_bytes_value(16));
        break;
    }
    case AttributeDataForm::Ref4: {
        auto data = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::DieReference;
        VERIFY(unit);
        value.m_data.as_unsigned = data + unit->offset();
        break;
    }
    case AttributeDataForm::RefUData: {
        auto data = TRY(debug_info_stream.read_value<LEB128<size_t>>());
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
        size_t length = TRY(debug_info_stream.read_value<LEB128<size_t>>());
        value.m_type = AttributeValue::Type::DwarfExpression;
        TRY(assign_raw_bytes_value(length));
        break;
    }
    case AttributeDataForm::String: {
        u32 str_offset = TRY(debug_info_stream.tell());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_string = bit_cast<char const*>(debug_info_data().offset_pointer(str_offset));
        TRY(debug_info_stream.discard(strlen(value.m_data.as_string) + 1));
        break;
    }
    case AttributeDataForm::Block1: {
        value.m_type = AttributeValue::Type::RawBytes;
        auto length = TRY(debug_info_stream.read_value<u8>());
        TRY(assign_raw_bytes_value(length));
        break;
    }
    case AttributeDataForm::Block2: {
        value.m_type = AttributeValue::Type::RawBytes;
        auto length = TRY(debug_info_stream.read_value<u16>());
        TRY(assign_raw_bytes_value(length));
        break;
    }
    case AttributeDataForm::Block4: {
        value.m_type = AttributeValue::Type::RawBytes;
        auto length = TRY(debug_info_stream.read_value<u32>());
        TRY(assign_raw_bytes_value(length));
        break;
    }
    case AttributeDataForm::Block: {
        value.m_type = AttributeValue::Type::RawBytes;
        size_t length = TRY(debug_info_stream.read_value<LEB128<size_t>>());
        TRY(assign_raw_bytes_value(length));
        break;
    }
    case AttributeDataForm::LineStrP: {
        auto offset = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::String;

        auto strings_data = debug_line_strings_data();
        value.m_data.as_string = bit_cast<char const*>(strings_data.offset_pointer(offset));
        break;
    }
    case AttributeDataForm::ImplicitConst: {
        /* Value is part of the abbreviation record. */
        value.m_type = AttributeValue::Type::SignedNumber;
        value.m_data.as_signed = implicit_const_value;
        break;
    }
    case AttributeDataForm::StrX1: {
        auto index = TRY(debug_info_stream.read_value<u8>());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX2: {
        auto index = TRY(debug_info_stream.read_value<u16>());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX4: {
        auto index = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::StrX: {
        size_t index = TRY(debug_info_stream.read_value<LEB128<size_t>>());
        value.m_type = AttributeValue::Type::String;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX1: {
        auto index = TRY(debug_info_stream.read_value<u8>());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX2: {
        auto index = TRY(debug_info_stream.read_value<u16>());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX4: {
        auto index = TRY(debug_info_stream.read_value<u32>());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::AddrX: {
        size_t index = TRY(debug_info_stream.read_value<LEB128<size_t>>());
        value.m_type = AttributeValue::Type::Address;
        value.m_data.as_unsigned = index;
        break;
    }
    case AttributeDataForm::LocListX:
    case AttributeDataForm::RngListX: {
        size_t index = TRY(debug_info_stream.read_value<LEB128<size_t>>());
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

ErrorOr<void> DwarfInfo::build_cached_dies() const
{
    auto insert_to_cache = [this](DIE const& die, DIERange const& range) {
        m_cached_dies_by_range.insert(range.start_address, DIEAndRange { die, range });
        m_cached_dies_by_offset.insert(die.offset(), die);
    };
    auto get_ranges_of_die = [this](DIE const& die) -> ErrorOr<Vector<DIERange>> {
        auto ranges = TRY(die.get_attribute(Attribute::Ranges));
        if (ranges.has_value()) {
            size_t offset;
            if (ranges->form() == AttributeDataForm::SecOffset) {
                offset = ranges->as_unsigned();
            } else {
                auto index = ranges->as_unsigned();
                auto base = TRY(die.compilation_unit().range_lists_base());
                // FIXME: This assumes that the format is DWARf32
                auto offsets = debug_range_lists_data().slice(base);
                offset = ByteReader::load32(offsets.offset_pointer(index * sizeof(u32))) + base;
            }

            Vector<DIERange> entries;
            if (die.compilation_unit().dwarf_version() == 5) {
                auto range_lists_stream = TRY(try_make<FixedMemoryStream>(debug_range_lists_data()));
                TRY(range_lists_stream->seek(offset));
                AddressRangesV5 address_ranges(move(range_lists_stream), die.compilation_unit());
                TRY(address_ranges.for_each_range([&entries](auto range) {
                    entries.empend(range.start, range.end);
                }));
            } else {
                auto ranges_stream = TRY(try_make<FixedMemoryStream>(debug_ranges_data()));
                TRY(ranges_stream->seek(offset));
                AddressRangesV4 address_ranges(move(ranges_stream), die.compilation_unit());
                TRY(address_ranges.for_each_range([&entries](auto range) {
                    entries.empend(range.start, range.end);
                }));
            }
            return entries;
        }

        auto start = TRY(die.get_attribute(Attribute::LowPc));
        auto end = TRY(die.get_attribute(Attribute::HighPc));

        if (!start.has_value() || !end.has_value())
            return Vector<DIERange> {};

        VERIFY(start->type() == Dwarf::AttributeValue::Type::Address);

        // DW_AT_high_pc attribute can have different meanings depending on the attribute form.
        // (Dwarf version 5, section 2.17.2).

        uint32_t range_end = 0;
        if (end->form() == Dwarf::AttributeDataForm::Addr)
            range_end = TRY(end->as_addr());
        else
            range_end = TRY(start->as_addr()) + end->as_unsigned();

        return Vector<DIERange> { DIERange { TRY(start.value().as_addr()), range_end } };
    };

    // If we simply use a lambda, type deduction fails because it's used recursively.
    Function<ErrorOr<void>(DIE const& die)> insert_to_cache_recursively;
    insert_to_cache_recursively = [&](DIE const& die) -> ErrorOr<void> {
        if (die.offset() == 0 || die.parent_offset().has_value()) {
            auto ranges = TRY(get_ranges_of_die(die));
            for (auto& range : ranges) {
                insert_to_cache(die, range);
            }
        }
        TRY(die.for_each_child([&](DIE const& child) -> ErrorOr<void> {
            if (!child.is_null()) {
                TRY(insert_to_cache_recursively(child));
            }
            return {};
        }));
        return {};
    };

    TRY(for_each_compilation_unit([&](CompilationUnit const& compilation_unit) -> ErrorOr<void> {
        TRY(insert_to_cache_recursively(compilation_unit.root_die()));
        return {};
    }));

    m_built_cached_dies = true;
    return {};
}

ErrorOr<Optional<DIE>> DwarfInfo::get_die_at_address(FlatPtr address) const
{
    if (!m_built_cached_dies)
        TRY(build_cached_dies());

    auto iter = m_cached_dies_by_range.find_largest_not_above_iterator(address);
    while (!iter.is_end() && !iter.is_begin() && iter->range.end_address < address) {
        --iter;
    }

    if (iter.is_end())
        return Optional<DIE> {};

    if (iter->range.start_address > address || iter->range.end_address < address) {
        return Optional<DIE> {};
    }

    return iter->die;
}

ErrorOr<Optional<DIE>> DwarfInfo::get_cached_die_at_offset(FlatPtr offset) const
{
    if (!m_built_cached_dies)
        TRY(build_cached_dies());

    auto* die = m_cached_dies_by_offset.find(offset);
    if (!die)
        return Optional<DIE> {};
    return *die;
}

}
