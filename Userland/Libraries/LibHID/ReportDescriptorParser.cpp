/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibHID/ReportDescriptorParser.h>

namespace HID {

#ifndef KERNEL
ErrorOr<void> dump_report_descriptor(ReadonlyBytes report_descriptor)
{
    ItemStream stream { report_descriptor };

    size_t indent_level = 0;
    while (!stream.is_eof()) {
        auto item_header = TRY(stream.read_item_header());

        switch (item_header.type) {
        case ItemType::Main:
            switch (static_cast<MainItemTag>(item_header.tag)) {
            case MainItemTag::Input: {
                auto input_item_data = TRY(stream.read_item_data<InputItemData>(item_header));

                StringBuilder args;
                TRY(args.try_append(input_item_data.constant ? "Constant, "sv : "Data, "sv));
                TRY(args.try_append(input_item_data.variable ? "Variable, "sv : "Array, "sv));
                TRY(args.try_append(input_item_data.relative ? "Relative, "sv : "Absolute, "sv));
                TRY(args.try_append(input_item_data.wrap ? "Wrap, "sv : "No Wrap, "sv));
                TRY(args.try_append(input_item_data.nonlinear ? "Nonlinear, "sv : "Linear, "sv));
                TRY(args.try_append(input_item_data.no_preferred_state ? "No Preferred, "sv : "Preferred State, "sv));
                TRY(args.try_append(input_item_data.has_null_state ? "Null state, "sv : "No Null Position, "sv));
                // Bit 7 is reserved
                TRY(args.try_append(input_item_data.buffered_bytes ? "Buffered Bytes"sv : "Bit Field"sv));

                outln("{: >{}}Input ({})", "", indent_level * 2, args.string_view());
                break;
            }

            case MainItemTag::Output: {
                auto output_item_data = TRY(stream.read_item_data<OutputItemData>(item_header));

                StringBuilder args;
                TRY(args.try_append(output_item_data.constant ? "Constant, "sv : "Data, "sv));
                TRY(args.try_append(output_item_data.variable ? "Variable, "sv : "Array, "sv));
                TRY(args.try_append(output_item_data.relative ? "Relative, "sv : "Absolute, "sv));
                TRY(args.try_append(output_item_data.wrap ? "Wrap, "sv : "No Wrap, "sv));
                TRY(args.try_append(output_item_data.nonlinear ? "Nonlinear, "sv : "Linear, "sv));
                TRY(args.try_append(output_item_data.no_preferred_state ? "No Preferred, "sv : "Preferred State, "sv));
                TRY(args.try_append(output_item_data.has_null_state ? "Null state, "sv : "No Null Position, "sv));
                TRY(args.try_append(output_item_data.volatile_ ? "Volatile, "sv : "Non Volatile, "sv));
                TRY(args.try_append(output_item_data.buffered_bytes ? "Buffered Bytes"sv : "Bit Field"sv));

                outln("{: >{}}Output ({})", "", indent_level * 2, args.string_view());
                break;
            }

            case MainItemTag::Feature: {
                auto feature_item_data = TRY(stream.read_item_data<FeatureItemData>(item_header));

                StringBuilder args;
                TRY(args.try_append(feature_item_data.constant ? "Constant, "sv : "Data, "sv));
                TRY(args.try_append(feature_item_data.variable ? "Variable, "sv : "Array, "sv));
                TRY(args.try_append(feature_item_data.relative ? "Relative, "sv : "Absolute, "sv));
                TRY(args.try_append(feature_item_data.wrap ? "Wrap, "sv : "No Wrap, "sv));
                TRY(args.try_append(feature_item_data.nonlinear ? "Nonlinear, "sv : "Linear, "sv));
                TRY(args.try_append(feature_item_data.no_preferred_state ? "No Preferred, "sv : "Preferred State, "sv));
                TRY(args.try_append(feature_item_data.has_null_state ? "Null state, "sv : "No Null Position, "sv));
                TRY(args.try_append(feature_item_data.volatile_ ? "Volatile, "sv : "Non Volatile, "sv));
                TRY(args.try_append(feature_item_data.buffered_bytes ? "Buffered Bytes"sv : "Bit Field"sv));

                outln("{: >{}}Feature ({})", "", indent_level * 2, args.string_view());
                break;
            }

            case MainItemTag::Collection: {
                auto collection_type = static_cast<CollectionType>(TRY(stream.read_item_data_unsigned(item_header)));
                outln("{: >{}}Collection ({:#x})", "", indent_level * 2, to_underlying(collection_type));
                indent_level++;
                break;
            }

            case MainItemTag::EndCollection:
                indent_level--;
                outln("{: >{}}End Collection", "", indent_level * 2);
                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown main item tag"sv, EINVAL);
            }
            break;

        case ItemType::Global:
            switch (static_cast<GlobalItemTag>(item_header.tag)) {
            case GlobalItemTag::UsagePage:
                // TODO: Pretty print the usage page name.
                outln("{: >{}}Usage Page ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case GlobalItemTag::LogicalMinimum:
                outln("{: >{}}Logical Minimum ({})", "", indent_level * 2, TRY(stream.read_item_data_signed(item_header)));
                break;

            case GlobalItemTag::LogicalMaximum:
                outln("{: >{}}Logical Maximum ({})", "", indent_level * 2, TRY(stream.read_item_data_signed(item_header)));
                break;

            case GlobalItemTag::PhysicalMinimum:
                outln("{: >{}}Physical Minimum ({})", "", indent_level * 2, TRY(stream.read_item_data_signed(item_header)));
                break;

            case GlobalItemTag::PhysicalMaximum:
                outln("{: >{}}Physical Maximum ({})", "", indent_level * 2, TRY(stream.read_item_data_signed(item_header)));
                break;

            case GlobalItemTag::UnitExponent:
                outln("{: >{}}Unit Exponent ({})", "", indent_level * 2, TRY(stream.read_item_data_signed(item_header)));
                break;

            case GlobalItemTag::Unit:
                // TODO: Pretty print the unit.
                outln("{: >{}}Unit ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case GlobalItemTag::ReportSize:
                outln("{: >{}}Report Size ({})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case GlobalItemTag::ReportID:
                outln("{: >{}}Report ID ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case GlobalItemTag::ReportCount:
                outln("{: >{}}Report Count ({})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case GlobalItemTag::Push:
                outln("{: >{}}Push", "", indent_level * 2);
                break;

            case GlobalItemTag::Pop:
                outln("{: >{}}Pop", "", indent_level * 2);
                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown global item tag"sv, EINVAL);
            }
            break;

        case ItemType::Local:
            switch (static_cast<LocalItemTag>(item_header.tag)) {
            case LocalItemTag::Usage:
                // TODO: Pretty print the usage name.
                outln("{: >{}}Usage ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::UsageMinimum:
                outln("{: >{}}Usage Minimum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::UsageMaximum:
                outln("{: >{}}Usage Maximum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::DesignatorIndex:
                outln("{: >{}}Designator Index ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::DesignatorMinimum:
                outln("{: >{}}Designator Minimum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::DesignatorMaximum:
                outln("{: >{}}Designator Maximum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::StringIndex:
                outln("{: >{}}String Index ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::StringMinimum:
                outln("{: >{}}String Minimum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::StringMaximum:
                outln("{: >{}}String Maximum ({:#x})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            case LocalItemTag::Delimiter:
                outln("{: >{}}Delimiter ({})", "", indent_level * 2, TRY(stream.read_item_data_unsigned(item_header)));
                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown local item tag"sv, EINVAL);
            }
            break;

        case ItemType::Reserved:
            if (item_header.tag == TAG_LONG_ITEM)
                return Error::from_string_view_or_print_error_and_return_errno("Long items are not supported"sv, EINVAL);
            else
                return Error::from_string_view_or_print_error_and_return_errno("Unsupported reserved item"sv, EINVAL);

        default:
            return Error::from_string_view_or_print_error_and_return_errno("Unknown item type"sv, EINVAL);
        }
    }

    return {};
}
#endif

ReportDescriptorParser::ReportDescriptorParser(ReadonlyBytes data)
    : m_stream(data)
{
}

ErrorOr<ParsedReportDescriptor> ReportDescriptorParser::parse()
{
    while (!m_stream.is_eof()) {
        auto item_header = TRY(m_stream.read_item_header());
        switch (item_header.type) {
        case ItemType::Main:
            switch (static_cast<MainItemTag>(item_header.tag)) {
            case MainItemTag::Input: {
                auto input_item_data = TRY(m_stream.read_item_data<InputItemData>(item_header));
                TRY(add_report_fields(FieldType::Input, input_item_data));
                m_input_output_or_feature_item_seen.set();
                break;
            }

            case MainItemTag::Output: {
                (void)TRY(m_stream.read_item_data<OutputItemData>(item_header));
                // TODO: Handle Output items.
                m_input_output_or_feature_item_seen.set();
                break;
            }

            case MainItemTag::Feature: {
                (void)TRY(m_stream.read_item_data<FeatureItemData>(item_header));
                // TODO: Handle Feature items.
                m_input_output_or_feature_item_seen.set();
                break;
            }

            case MainItemTag::Collection: {
                m_current_collection_tree_depth++;

                // Prevent generating collection trees with a huge depth.
                if (m_current_collection_tree_depth > 50)
                    return Error::from_string_view_or_print_error_and_return_errno("Report descriptor defines more than 50 nested collections"sv, E2BIG);

                auto collection_type = static_cast<CollectionType>(TRY(m_stream.read_item_data_unsigned(item_header)));

                // 6.2.2.6 Collection, End Collection Items: "[A] Usage item tag must be associated with any collection [...]."
                if (m_current_item_state_table.local.usages.size() == 0)
                    return Error::from_string_view_or_print_error_and_return_errno("Collection item without a preceding Usage item"sv, EINVAL);

                if (m_current_item_state_table.local.usages.size() > 1)
                    return Error::from_string_view_or_print_error_and_return_errno("Collection item with multiple usages"sv, EINVAL);

                auto usage = m_current_item_state_table.local.usages.first();

                if (m_current_collection == nullptr) {
                    // 8.4 Report Constraints: "Each top level collection must be an application collection and reports may not span more than one top level collection."
                    // FIXME: Maybe also check for the second condition somehow? We also expect that behaviour, as each ApplicationCollection has a HashMap of its reports.
                    if (collection_type != CollectionType::Application)
                        return Error::from_string_view_or_print_error_and_return_errno("Top-level collection with type != Application"sv, EINVAL);

                    ApplicationCollection new_collection {};
                    new_collection.parent = m_current_collection;
                    new_collection.type = collection_type;
                    new_collection.usage = usage;

                    TRY(m_parsed.application_collections.try_empend(move(new_collection)));
                    m_current_collection = &m_parsed.application_collections.last();
                    m_current_application_collection = &m_parsed.application_collections.last();
                } else {
                    Collection new_collection {};
                    new_collection.parent = m_current_collection;
                    new_collection.type = collection_type;
                    new_collection.usage = usage;

                    TRY(m_current_collection->child_collections.try_empend(move(new_collection)));
                    m_current_collection = &m_current_collection->child_collections.last();
                }

                break;
            }

            case MainItemTag::EndCollection:
                if (m_current_collection == nullptr)
                    return Error::from_string_view_or_print_error_and_return_errno("End Collection item with a corresponding Collection item"sv, EINVAL);

                m_current_collection = m_current_collection->parent;
                m_current_collection_tree_depth--;

                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown main item tag"sv, EINVAL);
            }

            // Reset the Local items in the current item state table.
            m_current_item_state_table.local = {};
            break;

        case ItemType::Global:
            switch (static_cast<GlobalItemTag>(item_header.tag)) {
            case GlobalItemTag::UsagePage:
                m_current_item_state_table.global.usage_page = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case GlobalItemTag::LogicalMinimum:
                m_current_item_state_table.global.logical_minimum = TRY(m_stream.read_item_data_signed(item_header));
                break;

            case GlobalItemTag::LogicalMaximum:
                m_current_item_state_table.global.logical_maximum = TRY(m_stream.read_item_data_signed(item_header));
                break;

            case GlobalItemTag::PhysicalMinimum:
                m_current_item_state_table.global.physical_maximum = TRY(m_stream.read_item_data_signed(item_header));
                break;

            case GlobalItemTag::PhysicalMaximum:
                m_current_item_state_table.global.physical_minimum = TRY(m_stream.read_item_data_signed(item_header));
                break;

            case GlobalItemTag::UnitExponent:
                m_current_item_state_table.global.unit_exponent = TRY(m_stream.read_item_data_signed(item_header));
                break;

            case GlobalItemTag::Unit:
                m_current_item_state_table.global.unit = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case GlobalItemTag::ReportSize: {
                auto report_size = TRY(m_stream.read_item_data_unsigned(item_header));

                // 8.4 Report Constraints: An item field cannot span more than 4 bytes in a report. For example, a 32-bit item must start on a byte boundary to satisfy this condition.
                if (report_size > 32)
                    return Error::from_string_view_or_print_error_and_return_errno("Report Size > 32"sv, EINVAL);

                m_current_item_state_table.global.report_size = report_size;
                break;
            }

            case GlobalItemTag::ReportID: {
                if (!m_parsed.uses_report_ids && m_input_output_or_feature_item_seen.was_set())
                    return Error::from_string_view_or_print_error_and_return_errno("Report ID item after the first Input/Output/Feature Item"sv, EINVAL);

                m_parsed.uses_report_ids = true;

                u8 const report_id = TRY(m_stream.read_item_data_unsigned(item_header));
                if (report_id == 0)
                    return Error::from_string_view_or_print_error_and_return_errno("Report ID item uses reserved ID 0"sv, EINVAL);

                m_current_item_state_table.global.report_id = report_id;
                break;
            }

            case GlobalItemTag::ReportCount:
                m_current_item_state_table.global.report_count = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case GlobalItemTag::Push:
                m_item_state_table_stack.append(m_current_item_state_table);
                break;

            case GlobalItemTag::Pop:
                if (m_item_state_table_stack.is_empty())
                    return Error::from_string_view_or_print_error_and_return_errno("Pop item without a corresponding Push item"sv, EINVAL);

                m_current_item_state_table = m_item_state_table_stack.take_last();
                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown global item tag"sv, EINVAL);
            }
            break;

        case ItemType::Local:
            switch (static_cast<LocalItemTag>(item_header.tag)) {
            case LocalItemTag::Usage: {
                u32 usage = TRY(m_stream.read_item_data_unsigned(item_header));
                if (item_header.real_size() != 4) {
                    if (!m_current_item_state_table.global.usage_page.has_value())
                        return Error::from_string_view_or_print_error_and_return_errno("Usage item without a preceding Usage Page item"sv, EINVAL); // FIXME: Are we supposed to handle this?
                    usage |= (static_cast<u32>(m_current_item_state_table.global.usage_page.value()) << 16);
                }
                TRY(m_current_item_state_table.local.usages.try_append(usage));
                break;
            }

            case LocalItemTag::UsageMinimum: {
                u32 usage_minimum = TRY(m_stream.read_item_data_unsigned(item_header));
                if (item_header.real_size() != 4) {
                    if (!m_current_item_state_table.global.usage_page.has_value())
                        return Error::from_string_view_or_print_error_and_return_errno("Usage Minimum item without a preceding Usage Page item"sv, EINVAL); // FIXME: Are we supposed to handle this?

                    usage_minimum |= (static_cast<u32>(m_current_item_state_table.global.usage_page.value()) << 16);
                }
                m_current_item_state_table.local.usage_minimum = usage_minimum;
                break;
            }

            case LocalItemTag::UsageMaximum: {
                u32 usage_maximum = TRY(m_stream.read_item_data_unsigned(item_header));
                if (item_header.real_size() != 4) {
                    if (!m_current_item_state_table.global.usage_page.has_value())
                        return Error::from_string_view_or_print_error_and_return_errno("Usage Maximum item without a preceding Usage Page item"sv, EINVAL); // FIXME: Are we supposed to handle this?

                    usage_maximum |= (static_cast<u32>(m_current_item_state_table.global.usage_page.value()) << 16);
                }
                m_current_item_state_table.local.usage_maximum = usage_maximum;
                break;
            }

            case LocalItemTag::DesignatorIndex:
                m_current_item_state_table.local.designator_index = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::DesignatorMinimum:
                m_current_item_state_table.local.degignator_minimum = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::DesignatorMaximum:
                m_current_item_state_table.local.designator_maximum = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::StringIndex:
                m_current_item_state_table.local.string_index = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::StringMinimum:
                m_current_item_state_table.local.string_minimum = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::StringMaximum:
                m_current_item_state_table.local.string_maximum = TRY(m_stream.read_item_data_unsigned(item_header));
                break;

            case LocalItemTag::Delimiter:
                (void)TRY(m_stream.read_item_data_unsigned(item_header));
                // TODO: Handle Delimiter items.
                break;

            default:
                return Error::from_string_view_or_print_error_and_return_errno("Unknown local item tag"sv, EINVAL);
            }
            break;

        case ItemType::Reserved:
            if (item_header.tag == TAG_LONG_ITEM)
                return Error::from_string_view_or_print_error_and_return_errno("Long items are not supported"sv, EINVAL);
            else
                return Error::from_string_view_or_print_error_and_return_errno("Unsupported reserved item"sv, EINVAL);

        default:
            return Error::from_string_view_or_print_error_and_return_errno("Unknown item type"sv, EINVAL);
        }
    }

    return m_parsed;
}

template<typename ItemData>
ErrorOr<void> ReportDescriptorParser::add_report_fields(FieldType field_type, ItemData item_data)
{
    if (m_current_collection == nullptr)
        return Error::from_string_view_or_print_error_and_return_errno("Input item without a preceding collection item"sv, EINVAL);

    // We always should have a current application collection if m_current_collection != nullptr.
    VERIFY(m_current_application_collection != nullptr);

    if (m_current_item_state_table.local.usage_minimum.has_value() && !m_current_item_state_table.local.usage_maximum.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Usage Minimum item without a corresponding Usage Maximum item"sv, EINVAL);

    if (m_current_item_state_table.local.usage_maximum.has_value() && !m_current_item_state_table.local.usage_minimum.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Usage Maximum item without a corresponding Usage Minimum item"sv, EINVAL);

    if (item_data.variable && m_current_item_state_table.local.usage_minimum.has_value() && m_current_item_state_table.local.usage_maximum.has_value()) {
        if (m_current_item_state_table.local.usage_maximum.value() - m_current_item_state_table.local.usage_minimum.value() + 1 != m_current_item_state_table.global.report_count)
            return Error::from_string_view_or_print_error_and_return_errno("Variable item with Usage Maximum - Usage Minimum + 1 != Report Count"sv, EINVAL); // TODO: How are we supposed to handle this?
    }

    if (!m_current_item_state_table.global.logical_minimum.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Input/Output/Feature item without a preceding Logical Minimum Item"sv, EINVAL);

    if (!m_current_item_state_table.global.logical_maximum.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Input/Output/Feature item without a preceding Logical Maximum Item"sv, EINVAL);

    if (!m_current_item_state_table.global.report_count.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Input/Output/Feature item without a preceding Report Count Item"sv, EINVAL);

    if (!m_current_item_state_table.global.report_size.has_value())
        return Error::from_string_view_or_print_error_and_return_errno("Input/Output/Feature item without a preceding Report Size Item"sv, EINVAL);

    auto& report_map = [this, field_type] -> HashMap<u8, Report>& {
        switch (field_type) {
        case FieldType::Input:
            return m_current_application_collection->input_reports;
        case FieldType::Output:
            return m_current_application_collection->output_reports;
        case FieldType::Feature:
            return m_current_application_collection->feature_reports;
        }
        VERIFY_NOT_REACHED();
    }();

    // FIXME: Since try_ensure does not return a reference to the contained value, we have to retrieve it separately.
    //        This is a try_ensure bug that should be fixed.
    (void)TRY(report_map.try_ensure(m_current_item_state_table.global.report_id.value_or(0), [this] {
        return Report {
            .size_in_bits = static_cast<size_t>(m_parsed.uses_report_ids ? 8 : 0),
            .fields = {},
        };
    }));
    auto& report = report_map.get(m_current_item_state_table.global.report_id.value_or(0)).release_value();

    size_t const field_size_in_bits = m_current_item_state_table.global.report_size.value();

    // Reject Report Counts above 1000 to avoid excessive loop iteration counts.
    if (m_current_item_state_table.global.report_count.value() > 1000)
        return Error::from_string_view_or_print_error_and_return_errno("Report Count > 1000"sv, E2BIG);

    for (size_t i = 0; i < m_current_item_state_table.global.report_count.value(); i++) {
        Optional<u32> usage;

        if (item_data.variable) {
            if (!m_current_item_state_table.local.usages.is_empty()) {
                if (i >= m_current_item_state_table.local.usages.size())
                    usage = m_current_item_state_table.local.usages.last();
                else
                    usage = m_current_item_state_table.local.usages[i];
            } else if (m_current_item_state_table.local.usage_minimum.has_value()) {
                usage = m_current_item_state_table.local.usage_minimum.value() + i;
            }
        }

        size_t const start_bit_index = report.size_in_bits;

        // Assume Input/Output/Feature items without a preceding usage item are used for padding (6.2.2.9 Padding).
        if (usage.has_value() || m_current_item_state_table.local.usage_minimum.has_value()) {
            if (item_data.variable)
                VERIFY(usage.has_value());

            auto field_usage_minimum = item_data.variable ? OptionalNone {} : m_current_item_state_table.local.usage_minimum;
            auto field_usage_maximum = item_data.variable ? OptionalNone {} : m_current_item_state_table.local.usage_maximum;

            Field field {
                .start_bit_index = start_bit_index,
                .end_bit_index = start_bit_index + field_size_in_bits,
                .is_array = !item_data.variable,
                .is_relative = static_cast<bool>(item_data.relative),
                .logical_minimum = m_current_item_state_table.global.logical_minimum.value(),
                .logical_maximum = m_current_item_state_table.global.logical_maximum.value(),
                .usage = usage,
                .usage_minimum = field_usage_minimum,
                .usage_maximum = field_usage_maximum,
            };

            // Reject reports descriptors with more than 1000 fields to prevent excessive field allocation.
            if (m_total_report_field_count > 1000)
                return Error::from_string_view_or_print_error_and_return_errno("Report descriptor defines more than 1000 fields"sv, E2BIG);

            TRY(report.fields.try_append(field));
            TRY(m_current_collection->fields.try_empend(move(field)));

            m_total_report_field_count++;
        }

        report.size_in_bits += field_size_in_bits;
    }

    return {};
}
}
