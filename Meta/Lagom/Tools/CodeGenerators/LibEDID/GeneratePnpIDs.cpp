/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceGenerator.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

enum class PnpIdColumns {
    ManufacturerName,
    ManufacturerId,
    ApprovalDate,

    ColumnCount // Must be last
};

struct ApprovalDate {
    unsigned year;
    unsigned month;
    unsigned day;
};

struct PnpIdData {
    DeprecatedString manufacturer_name;
    ApprovalDate approval_date;
};

static ErrorOr<DeprecatedString> decode_html_entities(StringView const& str)
{
    static constexpr struct {
        StringView entity_name;
        StringView value;
    } s_html_entities[] = {
        { "amp"sv, "&"sv },
    };

    StringBuilder decoded_str;
    size_t start = 0;
    for (;;) {
        auto entity_start = str.find('&', start);
        if (!entity_start.has_value()) {
            decoded_str.append(str.substring_view(start));
            break;
        }

        auto entity_end = str.find(';', entity_start.value() + 1);
        if (!entity_end.has_value() || entity_end.value() == entity_start.value() + 1) {
            decoded_str.append(str.substring_view(start, entity_start.value() - start + 1));
            start = entity_start.value() + 1;
            continue;
        }

        if (str[entity_start.value() + 1] == '#') {
            auto entity_number = str.substring_view(entity_start.value() + 2, entity_end.value() - entity_start.value() - 2).to_uint();
            if (!entity_number.has_value()) {
                decoded_str.append(str.substring_view(start, entity_end.value() - start + 1));
                start = entity_end.value() + 1;
                continue;
            }

            if (entity_start.value() != start)
                decoded_str.append(str.substring_view(start, entity_start.value() - start));

            decoded_str.append_code_point(entity_number.value());
        } else {
            auto entity_name = str.substring_view(entity_start.value() + 1, entity_end.value() - entity_start.value() - 1);
            bool found_entity = false;
            for (auto& html_entity : s_html_entities) {
                if (html_entity.entity_name == entity_name) {
                    found_entity = true;
                    if (entity_start.value() != start)
                        decoded_str.append(str.substring_view(start, entity_start.value() - start));
                    decoded_str.append(html_entity.value);
                    break;
                }
            }

            if (!found_entity)
                return Error::from_string_literal("Failed to decode html entity");

            if (entity_start.value() != start)
                decoded_str.append(str.substring_view(start, entity_start.value() - start));
        }

        start = entity_end.value() + 1;
    }
    return decoded_str.to_deprecated_string();
}

static ErrorOr<ApprovalDate> parse_approval_date(StringView const& str)
{
    auto parts = str.trim_whitespace().split_view('/', SplitBehavior::KeepEmpty);
    if (parts.size() != 3)
        return Error::from_string_literal("Failed to parse approval date parts (mm/dd/yyyy)");

    auto month = parts[0].to_uint();
    if (!month.has_value())
        return Error::from_string_literal("Failed to parse month from approval date");
    if (month.value() == 0 || month.value() > 12)
        return Error::from_string_literal("Invalid month in approval date");

    auto day = parts[1].to_uint();
    if (!day.has_value())
        return Error::from_string_literal("Failed to parse day from approval date");
    if (day.value() == 0 || day.value() > 31)
        return Error::from_string_literal("Invalid day in approval date");

    auto year = parts[2].to_uint();
    if (!year.has_value())
        return Error::from_string_literal("Failed to parse year from approval date");
    if (year.value() < 1900 || year.value() > 2999)
        return Error::from_string_literal("Invalid year approval date");

    return ApprovalDate { .year = year.value(), .month = month.value(), .day = day.value() };
}

static ErrorOr<HashMap<DeprecatedString, PnpIdData>> parse_pnp_ids_database(Core::File& pnp_ids_file)
{
    auto pnp_ids_file_bytes = TRY(pnp_ids_file.read_until_eof());
    StringView pnp_ids_file_contents(pnp_ids_file_bytes);

    HashMap<DeprecatedString, PnpIdData> pnp_id_data;

    for (size_t row_content_offset = 0;;) {
        static auto const row_start_tag = "<tr class=\""sv;
        auto row_start = pnp_ids_file_contents.find(row_start_tag, row_content_offset);
        if (!row_start.has_value())
            break;

        auto row_start_tag_end = pnp_ids_file_contents.find(">"sv, row_start.value() + row_start_tag.length());
        if (!row_start_tag_end.has_value())
            return Error::from_string_literal("Incomplete row start tag");

        static auto const row_end_tag = "</tr>"sv;
        auto row_end = pnp_ids_file_contents.find(row_end_tag, row_start.value());
        if (!row_end.has_value())
            return Error::from_string_literal("No matching row end tag found");

        if (row_start_tag_end.value() > row_end.value() + row_end_tag.length())
            return Error::from_string_literal("Invalid row start tag");

        auto row_string = pnp_ids_file_contents.substring_view(row_start_tag_end.value() + 1, row_end.value() - row_start_tag_end.value() - 1);
        Vector<DeprecatedString, (size_t)PnpIdColumns::ColumnCount> columns;
        for (size_t column_row_offset = 0;;) {
            static auto const column_start_tag = "<td>"sv;
            auto column_start = row_string.find(column_start_tag, column_row_offset);
            if (!column_start.has_value())
                break;

            static auto const column_end_tag = "</td>"sv;
            auto column_end = row_string.find(column_end_tag, column_start.value() + column_start_tag.length());
            if (!column_end.has_value())
                return Error::from_string_literal("No matching column end tag found");

            auto column_content_row_offset = column_start.value() + column_start_tag.length();
            auto column_str = row_string.substring_view(column_content_row_offset, column_end.value() - column_content_row_offset).trim_whitespace();
            if (column_str.find('\"').has_value())
                return Error::from_string_literal("Found '\"' in column content, escaping not supported!");
            columns.append(column_str);

            column_row_offset = column_end.value() + column_end_tag.length();
        }

        if (columns.size() != (size_t)PnpIdColumns::ColumnCount)
            return Error::from_string_literal("Unexpected number of columns found");

        auto approval_date = TRY(parse_approval_date(columns[(size_t)PnpIdColumns::ApprovalDate]));
        auto decoded_manufacturer_name = TRY(decode_html_entities(columns[(size_t)PnpIdColumns::ManufacturerName]));
        auto hash_set_result = pnp_id_data.set(columns[(size_t)PnpIdColumns::ManufacturerId], PnpIdData { .manufacturer_name = decoded_manufacturer_name, .approval_date = move(approval_date) });
        if (hash_set_result != AK::HashSetResult::InsertedNewEntry)
            return Error::from_string_literal("Duplicate manufacturer ID encountered");

        row_content_offset = row_end.value() + row_end_tag.length();
    }

    if (pnp_id_data.size() <= 1)
        return Error::from_string_literal("Expected more than one row");

    return pnp_id_data;
}

static ErrorOr<void> generate_header(Core::File& file, HashMap<DeprecatedString, PnpIdData> const& pnp_ids)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("pnp_id_count", DeprecatedString::formatted("{}", pnp_ids.size()));
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
            u16 year{};
            u8 month{};
            u8 day{};
        } approval_date;
    };

    Optional<PnpIDData> find_by_manufacturer_id(StringView);
    IterationDecision for_each(Function<IterationDecision(PnpIDData const&)>);
    static constexpr size_t count = @pnp_id_count@;
}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_source(Core::File& file, HashMap<DeprecatedString, PnpIdData> const& pnp_ids)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include "PnpIDs.h"

namespace PnpIDs {

static constexpr PnpIDData s_pnp_ids[] = {
)~~~");

    for (auto& pnp_id_data : pnp_ids) {
        generator.set("manufacturer_id", pnp_id_data.key);
        generator.set("manufacturer_name", pnp_id_data.value.manufacturer_name);
        generator.set("approval_year", DeprecatedString::formatted("{}", pnp_id_data.value.approval_date.year));
        generator.set("approval_month", DeprecatedString::formatted("{}", pnp_id_data.value.approval_date.month));
        generator.set("approval_day", DeprecatedString::formatted("{}", pnp_id_data.value.approval_date.day));

        generator.append(R"~~~(
{ "@manufacturer_id@"sv, "@manufacturer_name@"sv, { @approval_year@, @approval_month@, @approval_day@ } },
)~~~");
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

    TRY(file.write(generator.as_string_view().bytes()));
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

    auto open_file = [&](StringView path, Core::File::OpenMode mode = Core::File::OpenMode::Read) -> ErrorOr<NonnullOwnPtr<Core::File>> {
        if (path.is_empty()) {
            args_parser.print_usage(stderr, arguments.argv[0]);
            return Error::from_string_literal("Must provide all command line options");
        }

        return Core::File::open(path, mode);
    };

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::ReadWrite));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::ReadWrite));
    auto pnp_ids_file = TRY(open_file(pnp_ids_file_path));

    auto pnp_id_map = TRY(parse_pnp_ids_database(*pnp_ids_file));

    TRY(generate_header(*generated_header_file, pnp_id_map));
    TRY(generate_source(*generated_implementation_file, pnp_id_map));
    return 0;
}
