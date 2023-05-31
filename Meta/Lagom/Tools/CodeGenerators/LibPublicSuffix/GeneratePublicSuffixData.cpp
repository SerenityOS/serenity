/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(Core::InputBufferedFile&, Core::File&);
ErrorOr<void> generate_implementation_file(Core::InputBufferedFile&, Core::File&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView public_suffix_list_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(public_suffix_list_path, "Path to the public suffix list", "public-suffix-list-path", 'p', "public-suffix-list-path");
    args_parser.parse(arguments);

    auto identifier_data = TRY(open_file(public_suffix_list_path, Core::File::OpenMode::Read));

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(*identifier_data, *generated_header_file));
    TRY(generate_implementation_file(*identifier_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(Core::InputBufferedFile&, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/Trie.h>

namespace PublicSuffix {

class PublicSuffixData {
protected:
    PublicSuffixData();

public:
    PublicSuffixData(PublicSuffixData const&) = delete;
    PublicSuffixData& operator=(PublicSuffixData const&) = delete;

    static PublicSuffixData* the()
    {
        static PublicSuffixData* s_the;
        if (!s_the)
            s_the = new PublicSuffixData;
        return s_the;
    }

    ErrorOr<Optional<String>> get_public_suffix(StringView string);

private:
    Trie<char, DeprecatedString> m_dictionary;
};

} // namespace PublicSuffix

)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(Core::InputBufferedFile& input, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#include <LibPublicSuffix/PublicSuffixData.h>
#include <AK/Vector.h>
#include <AK/String.h>

namespace PublicSuffix {

static Vector<StringView> s_public_suffixes {)~~~");

    Array<u8, 1024> buffer {};

    while (TRY(input.can_read_line())) {
        auto line = TRY(input.read_line(buffer));

        if (line.starts_with("//"sv) || line.is_empty())
            continue;

        auto view = line.split_view("."sv);
        view.reverse();

        StringBuilder builder;
        builder.join("."sv, view);
        auto val = builder.string_view();

        generator.set("line", val);
        generator.append(R"~~~(
    {"@line@"sv},)~~~");
    }

    generator.append(R"~~~(
};

PublicSuffixData::PublicSuffixData()
    : m_dictionary('/', "")
{
    // FIXME: Reduce the depth of this trie
    for (auto str : s_public_suffixes) {
        MUST(m_dictionary.insert(str.begin(), str.end(), str, [](auto& parent, auto& it) -> Optional<DeprecatedString> { 
            return DeprecatedString::formatted("{}{}", parent.metadata_value(), *it);
        }));
    }
}

ErrorOr<Optional<String>> PublicSuffixData::get_public_suffix(StringView string)
{
    auto input = string.split_view("."sv);
    input.reverse();

    auto can_find = [&](StringView input) -> bool {
        auto it = input.begin();
        auto& node = m_dictionary.traverse_until_last_accessible_node(it, input.end());
        return it.is_end() && node.metadata().has_value();
    };

    StringBuilder overall_search_string;
    StringBuilder search_string;
    for (auto part : input) {
        search_string.clear();
        TRY(search_string.try_append(TRY(overall_search_string.to_string())));
        TRY(search_string.try_append(part));

        if (can_find(search_string.string_view())) {
            overall_search_string.append(TRY(String::from_utf8(part)));
            overall_search_string.append("."sv);
            continue;
        }

        search_string.clear();
        TRY(search_string.try_append(TRY(overall_search_string.to_string())));
        TRY(search_string.try_append("*"sv));

        if (can_find(search_string.string_view())) {
            overall_search_string.append(TRY(String::from_utf8(part)));
            overall_search_string.append("."sv);
            continue;
        }

        break;
    }

    auto view = overall_search_string.string_view().split_view("."sv);
    view.reverse();

    StringBuilder return_string_builder;
    return_string_builder.join('.', view);
    auto returnString = TRY(return_string_builder.to_string());
    if (!returnString.is_empty())
        return returnString;

    return Optional<String> {};
}

} // namespace PublicSuffix

)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
