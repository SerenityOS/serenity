/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

struct ApprovalDate {
    unsigned year;
    unsigned month;
    unsigned day;
};

struct PnpIdData {
    String manufacturer_name;
    ApprovalDate approval_date;
};

static ErrorOr<ApprovalDate> parse_approval_date(StringView date)
{
    auto parts = date.trim_whitespace().split_view('/', SplitBehavior::KeepEmpty);
    if (parts.size() != 3)
        return Error::from_string_literal("Failed to parse approval date parts (mm/dd/yyyy)");

    auto month = parts[0].to_number<unsigned>();
    if (!month.has_value())
        return Error::from_string_literal("Failed to parse month from approval date");
    if (month.value() == 0 || month.value() > 12)
        return Error::from_string_literal("Invalid month in approval date");

    auto day = parts[1].to_number<unsigned>();
    if (!day.has_value())
        return Error::from_string_literal("Failed to parse day from approval date");
    if (day.value() == 0 || day.value() > 31)
        return Error::from_string_literal("Invalid day in approval date");

    auto year = parts[2].to_number<unsigned>();
    if (!year.has_value())
        return Error::from_string_literal("Failed to parse year from approval date");
    if (year.value() < 1900 || year.value() > 2999)
        return Error::from_string_literal("Invalid year approval date");

    return ApprovalDate { .year = year.value(), .month = month.value(), .day = day.value() };
}

static ErrorOr<HashMap<String, PnpIdData>> parse_pnp_ids_database(Core::InputBufferedFile& pnp_ids_file)
{
    HashMap<String, PnpIdData> pnp_id_data;
    Array<u8, 1024> buffer;

    // The first line is just a header.
    (void)TRY(pnp_ids_file.read_line(buffer));

    while (TRY(pnp_ids_file.can_read_line())) {
        auto line = TRY(pnp_ids_file.read_line(buffer));

        auto segments = line.split_view(',');
        VERIFY(segments.size() >= 3);

        auto approval_date = TRY(parse_approval_date(segments.take_last()));
        auto manufacturer_id = MUST(String::from_utf8(segments.take_last()));
        auto manufacturer_name = MUST(MUST(String::join(',', segments)).trim("\""sv));

        pnp_id_data.set(move(manufacturer_id), { .manufacturer_name = move(manufacturer_name), .approval_date = approval_date });
    }

    if (pnp_id_data.size() <= 1)
        return Error::from_string_literal("Expected more than one row");

    return pnp_id_data;
}

static ErrorOr<void> generate_header(Core::InputBufferedFile& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Function.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace PnpIDs {
    struct PnpIDData {
        StringView manufacturer_id;
        StringView manufacturer_name;
        struct {
            u16 year { 0 };
            u8 month { 0 };
            u8 day { 0 };
        } approval_date;
    };

    Optional<PnpIDData> find_by_manufacturer_id(StringView);
    IterationDecision for_each(Function<IterationDecision(PnpIDData const&)>);
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_source(Core::InputBufferedFile& file, HashMap<String, PnpIdData> const& pnp_ids)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <LibEDID/PnpIDs.h>

namespace PnpIDs {

static constexpr PnpIDData s_pnp_ids[] = {)~~~");

    for (auto& pnp_id_data : pnp_ids) {
        generator.set("manufacturer_id", pnp_id_data.key);
        generator.set("manufacturer_name", pnp_id_data.value.manufacturer_name);
        generator.set("approval_year", String::number(pnp_id_data.value.approval_date.year));
        generator.set("approval_month", String::number(pnp_id_data.value.approval_date.month));
        generator.set("approval_day", String::number(pnp_id_data.value.approval_date.day));

        generator.append(R"~~~(
    { "@manufacturer_id@"sv, "@manufacturer_name@"sv, { @approval_year@, @approval_month@, @approval_day@ } },)~~~");
    }

    generator.append(R"~~~(
};

Optional<PnpIDData> find_by_manufacturer_id(StringView manufacturer_id)
{
    for (auto& pnp_data : s_pnp_ids) {
        if (pnp_data.manufacturer_id == manufacturer_id)
            return pnp_data;
    }
    return {};
}

IterationDecision for_each(Function<IterationDecision(PnpIDData const&)> callback)
{
    for (auto& pnp_data : s_pnp_ids) {
        auto decision = callback(pnp_data);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView pnp_ids_file_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(pnp_ids_file_path, "Path to the input PNP ID database file", "pnp-ids-file", 'p', "pnp-ids-file");
    args_parser.parse(arguments);

    auto open_file = [&](StringView path, Core::File::OpenMode mode = Core::File::OpenMode::Read) -> ErrorOr<NonnullOwnPtr<Core::InputBufferedFile>> {
        if (path.is_empty()) {
            args_parser.print_usage(stderr, arguments.strings[0]);
            return Error::from_string_literal("Must provide all command line options");
        }

        auto file = TRY(Core::File::open(path, mode));
        return Core::InputBufferedFile::create(move(file));
    };

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::ReadWrite));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::ReadWrite));
    auto pnp_ids_file = TRY(open_file(pnp_ids_file_path));

    auto pnp_id_map = TRY(parse_pnp_ids_database(*pnp_ids_file));

    TRY(generate_header(*generated_header_file));
    TRY(generate_source(*generated_implementation_file, pnp_id_map));

    return 0;
}
