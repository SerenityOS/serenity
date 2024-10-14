/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/Error.h>
#include <AK/SourceGenerator.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>

enum class MappingStatus : u8 {
    Valid,
    Ignored,
    Mapped,
    Deviation,
    Disallowed,
    DisallowedStd3Valid,
    DisallowedStd3Mapped,
};

static constexpr Array<StringView, 7> mapping_status_names { "Valid"sv, "Ignored"sv, "Mapped"sv, "Deviation"sv, "Disallowed"sv, "DisallowedStd3Valid"sv, "DisallowedStd3Mapped"sv };

enum class IDNA2008Status : u8 {
    NV8,
    XV8,
};

static constexpr Array<StringView, 2> idna_2008_status_names { "NV8"sv, "XV8"sv };

struct IDNAMapping {
    Unicode::CodePointRange code_points;
    MappingStatus status;
    IDNA2008Status idna_2008_status;
    Vector<u32> mapped_to {};
};

struct IDNAData {
    Vector<IDNAMapping> mapping_table;
};

static MappingStatus parse_mapping_status(StringView status)
{
    if (status == "valid"sv)
        return MappingStatus::Valid;
    if (status == "ignored"sv)
        return MappingStatus::Ignored;
    if (status == "mapped"sv)
        return MappingStatus::Mapped;
    if (status == "deviation"sv)
        return MappingStatus::Deviation;
    if (status == "disallowed"sv)
        return MappingStatus::Disallowed;
    if (status == "disallowed_STD3_valid"sv)
        return MappingStatus::DisallowedStd3Valid;
    if (status == "disallowed_STD3_mapped"sv)
        return MappingStatus::DisallowedStd3Mapped;
    VERIFY_NOT_REACHED();
}

static ErrorOr<void> parse_idna_mapping_table(Core::InputBufferedFile& file, Vector<IDNAMapping>& mapping_table)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring_view(0, *index);

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() >= 2);

        IDNAMapping idna_mapping {};
        idna_mapping.code_points = parse_code_point_range(segments[0].trim_whitespace());
        idna_mapping.status = parse_mapping_status(segments[1].trim_whitespace());

        if (segments.size() >= 3)
            idna_mapping.mapped_to = parse_code_point_list(segments[2].trim_whitespace());

        if (segments.size() >= 4) {
            auto trimmed = segments[3].trim_whitespace();
            if (trimmed == "NV8"sv) {
                idna_mapping.idna_2008_status = IDNA2008Status::NV8;
            } else {
                VERIFY(trimmed == "XV8"sv);
                idna_mapping.idna_2008_status = IDNA2008Status::XV8;
            }
        }

        TRY(mapping_table.try_append(move(idna_mapping)));
    }

    return {};
}

static ErrorOr<void> generate_idna_data_header(Core::InputBufferedFile& file, IDNAData&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

namespace Unicode::IDNA {
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_idna_data_implementation(Core::InputBufferedFile& file, IDNAData& idna_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("idna_table_size", String::number(idna_data.mapping_table.size()));

    generator.append(R"~~~(
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/Utf32View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/IDNA.h>
#include <LibUnicode/IDNAData.h>

namespace Unicode::IDNA {

struct MappingEntry {
    CodePointRange code_points {};
    MappingStatus status : 3 { MappingStatus::Valid };
    IDNA2008Status idna_2008_status : 1 { IDNA2008Status::NV8 };
    size_t mapping_offset : 20 { 0 };
    size_t mapping_length : 8 { 0 };
};

static constexpr Array<MappingEntry, @idna_table_size@> s_idna_mapping_table { {)~~~");

    {
        size_t mapping_offset = 0;
        for (auto const& mapping : idna_data.mapping_table) {
            generator.set("code_points", TRY(String::formatted("{:#x}, {:#x}", mapping.code_points.first, mapping.code_points.last)));
            generator.set("status", mapping_status_names[to_underlying(mapping.status)]);
            generator.set("idna_2008_status", idna_2008_status_names[to_underlying(mapping.idna_2008_status)]);

            if (mapping.mapped_to.is_empty()) {
                generator.set("mapping_offset", "0"sv);
                generator.set("mapping_length", "0"sv);
            } else {
                generator.set("mapping_offset", String::number(mapping_offset));
                generator.set("mapping_length", String::number(mapping.mapped_to.size()));
                mapping_offset += mapping.mapped_to.size();
            }

            generator.append(R"~~~(
    { { @code_points@ }, MappingStatus::@status@, IDNA2008Status::@idna_2008_status@, @mapping_offset@, @mapping_length@ },)~~~");
        }

        generator.set("mapping_length_total", String::number(mapping_offset));
    }

    generator.append(R"~~~(
} };

static constexpr Array<u32, @mapping_length_total@> s_mapping_code_points { )~~~");

    {
        for (auto const& mapping : idna_data.mapping_table) {
            if (mapping.mapped_to.is_empty())
                continue;

            for (u32 code_point : mapping.mapped_to)
                generator.append(TRY(String::formatted("{:#x}, ", code_point)));

            generator.append(R"~~~(
    )~~~");
        }
    }

    generator.append(R"~~~(
};

Optional<Mapping> get_idna_mapping(u32 code_point)
{
    auto* entry = binary_search(s_idna_mapping_table, code_point, nullptr, [](auto code_point, auto entry) {
        if (code_point < entry.code_points.first)
            return -1;
        if (code_point > entry.code_points.last)
            return 1;
        return 0;
    });

    if (!entry)
        return {};

    auto mapped_to = Utf32View { entry->mapping_length ?  s_mapping_code_points.data() + entry->mapping_offset : nullptr, entry->mapping_length };
    return Mapping { entry->status, entry->idna_2008_status, move(mapped_to) };
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
    StringView idna_mapping_table_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the IDNA Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the IDNA Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(idna_mapping_table_path, "Path to IdnaMappingTable.txt file", "idna-mapping-table-path", 'm', "idna-mapping-table-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));
    auto idna_mapping_table_file = TRY(open_file(idna_mapping_table_path, Core::File::OpenMode::Read));

    IDNAData idna_data {};
    TRY(parse_idna_mapping_table(*idna_mapping_table_file, idna_data.mapping_table));

    TRY(generate_idna_data_header(*generated_header_file, idna_data));
    TRY(generate_idna_data_implementation(*generated_implementation_file, idna_data));

    return 0;
}
