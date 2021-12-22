/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

static void generate_time_zone_data_header(Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_time_zone_data_implementation(Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <LibTimeZone/TimeZoneData.h>
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    Vector<StringView> time_zone_paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the time zone data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the time zone data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_positional_argument(time_zone_paths, "Paths to the time zone database files", "time-zone-paths");
    args_parser.parse(arguments);

    auto open_file = [&](StringView path) -> ErrorOr<NonnullRefPtr<Core::File>> {
        if (path.is_empty()) {
            args_parser.print_usage(stderr, arguments.argv[0]);
            return Error::from_string_literal("Must provide all command line options"sv);
        }

        return Core::File::open(path, Core::OpenMode::ReadWrite);
    };

    auto generated_header_file = TRY(open_file(generated_header_path));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path));

    generate_time_zone_data_header(generated_header_file);
    generate_time_zone_data_implementation(generated_implementation_file);

    return 0;
}
