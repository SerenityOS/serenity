/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <AK/Function.h>
#include <AK/IntegralMath.h>
#include <AK/MemoryStream.h>
#include <LibHID/ReportDescriptorParser.h>
#include <LibHID/ReportParser.h>

namespace HID {

ErrorOr<void> parse_input_report(ParsedReportDescriptor const& report_descriptor, ApplicationCollection const& application_collection, ReadonlyBytes report_data, Function<ErrorOr<IterationDecision>(Field const&, i64)> callback)
{
    u8 report_id = 0;
    if (report_descriptor.uses_report_ids) {
        if (report_data.size() < 1)
            return Error::from_string_view_or_print_error_and_return_errno("Report is too small"sv, EINVAL);

        report_id = report_data[0];
    }

    auto maybe_report = application_collection.input_reports.get(report_id);
    if (!maybe_report.has_value())
        return {};

    for (auto const& field : maybe_report->fields) {
        // 8.4 Report Constraints: An item field cannot span more than 4 bytes in a report. For example, a 32-bit item must start on a byte boundary to satisfy this condition.
        // This means we can just load the containing byte-aligned 32-bit word and extract the bits from there.
        auto surrounding_word_byte_index = field.start_bit_index / 8;
        auto start_bit_index_in_word = field.start_bit_index % 8;

        auto field_size_in_bits = field.end_bit_index - field.start_bit_index;
        auto field_size_in_bytes = ceil_div(field_size_in_bits, 8zu);

        VERIFY(field_size_in_bytes <= 4);

        u32 surrounding_word = 0;
        for (size_t i = 0; i < field_size_in_bytes; i++) {
            if (surrounding_word_byte_index + i >= report_data.size())
                return Error::from_string_view_or_print_error_and_return_errno("Report is too small"sv, EINVAL);

            surrounding_word |= report_data[surrounding_word_byte_index + i] << (i * 8);
        }

        i64 field_value = (surrounding_word >> start_bit_index_in_word) & static_cast<u32>((1ull << field_size_in_bits) - 1);

        // 5.8 Format of Multibyte Numeric Values: If Logical Minimum and Logical Maximum are both positive values
        // then a sign bit is unnecessary in the report field and the contents of a field can be assumed to be an unsigned value.
        if (field.logical_minimum < 0)
            field_value = AK::sign_extend(static_cast<u64>(field_value), field_size_in_bits);

        if (TRY(callback(field, field_value)) == IterationDecision::Break)
            return {};
    }

    return {};
}

}
