/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibHID/ReportDescriptorParser.h>
#include <LibHID/ReportParser.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView report_descriptor_file_name;
    Optional<StringView> report_file_name;

    Core::ArgsParser args;
    args.add_positional_argument(report_descriptor_file_name, "HID Report Descriptor", "report-descriptor-file", Core::ArgsParser::Required::Yes);
    args.add_option(report_file_name, "Parse HID Input Report from file", "parse-report", 'r', "report-file");
    args.parse(arguments);

    auto report_descriptor_file = TRY(Core::File::open(report_descriptor_file_name, Core::File::OpenMode::Read));
    auto report_descriptor = TRY(report_descriptor_file->read_until_eof());

    if (!report_file_name.has_value())
        TRY(HID::dump_report_descriptor(report_descriptor));

    HID::ReportDescriptorParser parser { report_descriptor };
    auto parsed_descriptor = TRY(parser.parse());

    if (report_file_name.has_value()) {
        auto report_file = TRY(Core::File::open(report_file_name.value(), Core::File::OpenMode::Read));
        auto report = TRY(report_file->read_until_eof());

        for (auto const& application_collection : parsed_descriptor.application_collections) {
            outln("Application Collection (Usage {:#x}):", application_collection.usage);
            TRY(HID::parse_input_report(parsed_descriptor, application_collection, report, [](HID::Field const& field, i64 value) -> ErrorOr<IterationDecision> {
                if (field.is_array) {
                    if (!field.usage_minimum.has_value())
                        return Error::from_errno(ENOTSUP); // TODO: What are we supposed to do here?

                    outln("    Array: {:#x}", value + field.usage_minimum.value());
                } else {
                    outln("    {:#x}: {}", field.usage.value(), value);
                }

                return IterationDecision::Continue;
            }));
        }

        return 0;
    }

    outln();
    outln("Input Reports:");

    for (auto const& application_collection : parsed_descriptor.application_collections) {
        outln("Application Collection (Usage {:#x}):", application_collection.usage);
        for (auto const& [report_id, report] : application_collection.input_reports) {
            outln("    Report {:#x}:", report_id);
            for (auto const& field : report.fields) {
                if (field.end_bit_index - field.start_bit_index == 1)
                    outln("        Bit {}:", field.start_bit_index);
                else
                    outln("        Bits {}..{} ({} bits):", field.start_bit_index, field.end_bit_index, field.end_bit_index - field.start_bit_index);

                if (field.is_array)
                    outln("            Array");
                else
                    outln("            Variable");

                outln("            Logical Minimum: {}", field.logical_minimum);
                outln("            Logical Maximum: {}", field.logical_maximum);

                if (field.usage.has_value())
                    outln("            Usage: {:#x}", field.usage.value());
                if (field.usage_minimum.has_value())
                    outln("            Usage Minimum: {:#x}", field.usage_minimum.value());
                if (field.usage_maximum.has_value())
                    outln("            Usage Maximum: {:#x}", field.usage_maximum.value());

                outln();
            }
        }
    }

    return 0;
}
