/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/MemoryStream.h>
#include <AK/SetOnce.h>
#include <AK/Stack.h>

#include <LibHID/ReportDescriptorDefinitions.h>

namespace HID {

// https://www.usb.org/document-library/device-class-definition-hid-111

class ItemStream : public FixedMemoryStream {
public:
    using FixedMemoryStream::FixedMemoryStream;

    ErrorOr<ItemHeader> read_item_header()
    {
        return read_value<ItemHeader>();
    }

    template<typename T>
    ErrorOr<T> read_item_data(ItemHeader item_header)
    {
        VERIFY(item_header.type != ItemType::Reserved && item_header.tag != TAG_LONG_ITEM);

        if (item_header.real_size() > sizeof(T))
            return Error::from_errno(EINVAL);

        // Short items have a max data length of 4.
        alignas(T) u8 buffer[4] {};
        TRY(read_until_filled({ &buffer, item_header.real_size() }));

        return *reinterpret_cast<T*>(&buffer);
    }

    ErrorOr<u32> read_item_data_unsigned(ItemHeader item_header)
    {
        VERIFY(item_header.type != ItemType::Reserved && item_header.tag != TAG_LONG_ITEM);

        switch (item_header.real_size()) {
        case 0:
            return 0;
        case 1:
            return read_value<LittleEndian<u8>>();
        case 2:
            return read_value<LittleEndian<u16>>();
        case 4:
            return read_value<LittleEndian<u32>>();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<i32> read_item_data_signed(ItemHeader item_header)
    {
        VERIFY(item_header.type != ItemType::Reserved && item_header.tag != TAG_LONG_ITEM);

        switch (item_header.real_size()) {
        case 0:
            return 0;
        case 1:
            return read_value<LittleEndian<i8>>();
        case 2:
            return read_value<LittleEndian<i16>>();
        case 4:
            return read_value<LittleEndian<i32>>();
        default:
            VERIFY_NOT_REACHED();
        }
    }
};

#ifndef KERNEL
ErrorOr<void> dump_report_descriptor(ReadonlyBytes);
#endif

// 5.4 Item Parser

struct ItemStateTable {
    // 6.2.2.7 Global Items
    struct {
        Optional<u16> usage_page;
        Optional<i32> logical_minimum;
        Optional<i32> logical_maximum;
        Optional<u32> physical_minimum;
        Optional<u32> physical_maximum;
        Optional<u32> unit_exponent;
        Optional<u32> unit;
        Optional<u32> report_size;
        Optional<u8> report_id;
        Optional<u32> report_count;
    } global;

    // 6.2.2.8 Local Items
    struct {
        Vector<u32, 4> usages;
        Optional<u32> usage_minimum;
        Optional<u32> usage_maximum;
        Optional<u32> designator_index;
        Optional<u32> degignator_minimum;
        Optional<u32> designator_maximum;
        Optional<u32> string_index;
        Optional<u32> string_minimum;
        Optional<u32> string_maximum;
    } local;
};

enum class FieldType {
    Input = to_underlying(MainItemTag::Input),
    Output = to_underlying(MainItemTag::Output),
    Feature = to_underlying(MainItemTag::Feature),
};

struct Field {
    size_t start_bit_index;
    size_t end_bit_index;

    bool is_array;
    bool is_relative;

    i32 logical_minimum;
    i32 logical_maximum;

    // For variable items
    Optional<u32> usage;

    // For array items
    Optional<u32> usage_minimum;
    Optional<u32> usage_maximum;
};

struct Collection {
    Collection* parent;
    CollectionType type;
    u32 usage;
    Vector<Field> fields;
    Vector<Collection> child_collections;
};

struct Report {
    size_t size_in_bits;
    Vector<Field> fields;
};

struct ApplicationCollection : Collection {
    // The key for these HashMaps is the Report ID.
    // Report ID 0 is reserved by the HID spec. We use that ID if no Report ID items are present.
    HashMap<u8, Report> input_reports;
    HashMap<u8, Report> output_reports;
    HashMap<u8, Report> feature_reports;
};

struct ParsedReportDescriptor {
    Vector<ApplicationCollection> application_collections;

    bool uses_report_ids { false };
};

class ReportDescriptorParser {
public:
    ReportDescriptorParser(ReadonlyBytes);

    ErrorOr<ParsedReportDescriptor> parse();

private:
    template<typename ItemData>
    ErrorOr<void> add_report_fields(FieldType, ItemData);

    ItemStream m_stream;

    Vector<ItemStateTable> m_item_state_table_stack;
    ItemStateTable m_current_item_state_table {};
    Collection* m_current_collection { nullptr };
    ApplicationCollection* m_current_application_collection { nullptr };
    ParsedReportDescriptor m_parsed;

    SetOnce m_input_output_or_feature_item_seen;

    size_t m_total_report_field_count { 0 };
    size_t m_current_collection_tree_depth { 0 };
};

}
