/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

struct Locale {
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    HashMap<String, Locale> locales;
};

static ErrorOr<void> parse_all_locales(String dates_path, UnicodeLocaleData& locale_data)
{
    auto dates_iterator = TRY(path_to_dir_iterator(move(dates_path)));

    auto remove_variants_from_path = [&](String path) -> ErrorOr<String> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(locale_data.unique_strings.get(parsed_locale.language));
        if (auto script = locale_data.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = locale_data.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (dates_iterator.has_next()) {
        auto dates_path = TRY(next_path_from_dir_iterator(dates_iterator));
        auto language = TRY(remove_variants_from_path(dates_path));

        [[maybe_unused]] auto& locale = locale_data.locales.ensure(language);
    }

    return {};
}

static void generate_unicode_locale_header(Core::File& file, UnicodeLocaleData&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/StringView.h>
#include <LibUnicode/Forward.h>

namespace Unicode {
}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);

    generator.append(R"~~~(
#include <AK/Array.h>
#include <LibUnicode/UnicodeDateTimeFormat.h>

namespace Unicode::Detail {
)~~~");

    locale_data.unique_strings.generate(generator);

    generator.append(R"~~~(
}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView dates_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(dates_path, "Path to cldr-dates directory", "dates-path", 'd', "dates-path");
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

    UnicodeLocaleData locale_data;
    TRY(parse_all_locales(dates_path, locale_data));

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
