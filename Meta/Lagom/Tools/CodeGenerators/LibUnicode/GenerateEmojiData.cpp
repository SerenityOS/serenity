/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AnyOf.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibUnicode/Emoji.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

struct Emoji {
    StringIndexType name { 0 };
    Unicode::EmojiGroup group;
    u32 display_order { 0 };
    String code_points_name;
    Vector<u32> code_points;
};

struct EmojiData {
    UniqueStringStorage<StringIndexType> unique_strings;
    Vector<Emoji> emojis;
};

static ErrorOr<void> parse_emoji_test_data(Core::Stream::BufferedFile& file, EmojiData& emoji_data)
{
    static constexpr auto group_header = "# group: "sv;

    Array<u8, 1024> buffer;

    Unicode::EmojiGroup group;
    u32 display_order { 0 };

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty())
            continue;

        if (line.starts_with('#')) {
            if (line.starts_with(group_header)) {
                auto name = line.substring_view(group_header.length());
                group = Unicode::emoji_group_from_string(name);
            }

            continue;
        }

        auto status_index = line.find(';');
        VERIFY(status_index.has_value());

        auto emoji_and_name_index = line.find('#', *status_index);
        VERIFY(emoji_and_name_index.has_value());

        Emoji emoji {};
        emoji.group = group;
        emoji.display_order = display_order++;

        auto code_points = line.substring_view(0, *status_index).split_view(' ');
        TRY(emoji.code_points.try_ensure_capacity(code_points.size()));

        for (auto code_point : code_points) {
            auto value = AK::StringUtils::convert_to_uint_from_hex<u32>(code_point);
            VERIFY(value.has_value());

            emoji.code_points.unchecked_append(*value);
        }

        auto emoji_and_name = line.substring_view(*emoji_and_name_index + 1);

        auto emoji_and_name_spaces = emoji_and_name.find_all(" "sv);
        VERIFY(emoji_and_name_spaces.size() > 2);

        auto name = emoji_and_name.substring_view(emoji_and_name_spaces[2]).trim_whitespace();
        emoji.name = emoji_data.unique_strings.ensure(name.to_titlecase_string());
        emoji.code_points_name = String::join('_', code_points);

        TRY(emoji_data.emojis.try_append(move(emoji)));
    }

    return {};
}

static ErrorOr<void> parse_emoji_serenity_data(Core::Stream::BufferedFile& file, EmojiData& emoji_data)
{
    static constexpr auto code_point_header = "U+"sv;

    Array<u8, 1024> buffer;

    auto display_order = static_cast<u32>(emoji_data.emojis.size()) + 1u;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty())
            continue;

        auto index = line.find(code_point_header);
        if (!index.has_value())
            continue;

        line = line.substring_view(*index);
        StringBuilder builder;

        Emoji emoji {};
        emoji.group = Unicode::EmojiGroup::SerenityOS;
        emoji.display_order = display_order++;

        line.for_each_split_view(' ', false, [&](auto segment) {
            if (segment.starts_with(code_point_header)) {
                segment = segment.substring_view(code_point_header.length());

                auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segment);
                VERIFY(code_point.has_value());

                emoji.code_points.append(*code_point);
            } else {
                if (!builder.is_empty())
                    builder.append(' ');
                builder.append(segment);
            }
        });

        auto name = builder.build();
        if (!any_of(name, is_ascii_lower_alpha))
            name = name.to_titlecase();

        emoji.name = emoji_data.unique_strings.ensure(move(name));
        emoji.code_points_name = String::join('_', emoji.code_points);
        TRY(emoji_data.emojis.try_append(move(emoji)));
    }

    return {};
}

static ErrorOr<void> generate_emoji_data_header(Core::Stream::BufferedFile& file, EmojiData const&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_emoji_data_implementation(Core::Stream::BufferedFile& file, EmojiData const& emoji_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("emojis_size"sv, String::number(emoji_data.emojis.size()));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibUnicode/Emoji.h>
#include <LibUnicode/EmojiData.h>

namespace Unicode {
)~~~");

    emoji_data.unique_strings.generate(generator);

    generator.append(R"~~~(
struct EmojiData {
    constexpr Emoji to_unicode_emoji() const
    {
        Emoji emoji {};
        emoji.name = decode_string(name);
        emoji.group = static_cast<EmojiGroup>(group);
        emoji.display_order = display_order;
        emoji.code_points = code_points;

        return emoji;
    }

    @string_index_type@ name { 0 };
    u8 group { 0 };
    u32 display_order { 0 };
    Span<u32 const> code_points;
};
)~~~");

    for (auto const& emoji : emoji_data.emojis) {
        generator.set("name"sv, emoji.code_points_name);
        generator.set("size"sv, String::number(emoji.code_points.size()));

        generator.append(R"~~~(
static constexpr Array<u32, @size@> s_@name@ { {)~~~");

        bool first = true;
        for (auto code_point : emoji.code_points) {
            generator.append(first ? " "sv : ", "sv);
            generator.append(String::formatted("{:#x}", code_point));
            first = false;
        }

        generator.append(" } };"sv);
    }

    generator.append(R"~~~(

static constexpr Array<EmojiData, @emojis_size@> s_emojis { {)~~~");

    for (auto const& emoji : emoji_data.emojis) {
        generator.set("name"sv, String::number(emoji.name));
        generator.set("group"sv, String::number(to_underlying(emoji.group)));
        generator.set("display_order"sv, String::number(emoji.display_order));
        generator.set("code_points_name"sv, emoji.code_points_name);

        generator.append(R"~~~(
    { @name@, @group@, @display_order@, s_@code_points_name@ },)~~~");
    }

    generator.append(R"~~~(
} };

Optional<Emoji> find_emoji_for_code_points(Span<u32 const> code_points)
{
    for (auto& emoji : s_emojis) {
        if (emoji.code_points == code_points)
            return emoji.to_unicode_emoji();
    }

    return {};
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
    StringView emoji_test_path;
    StringView emoji_serenity_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(emoji_test_path, "Path to emoji-test.txt file", "emoji-test-path", 'e', "emoji-test-path");
    args_parser.add_option(emoji_serenity_path, "Path to emoji-serenity.txt file", "emoji-serenity-path", 's', "emoji-serenity-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::Stream::OpenMode::Write));
    auto emoji_test_file = TRY(open_file(emoji_test_path, Core::Stream::OpenMode::Read));
    auto emoji_serenity_file = TRY(open_file(emoji_serenity_path, Core::Stream::OpenMode::Read));

    EmojiData emoji_data {};
    TRY(parse_emoji_test_data(*emoji_test_file, emoji_data));
    TRY(parse_emoji_serenity_data(*emoji_serenity_file, emoji_data));

    TRY(generate_emoji_data_header(*generated_header_file, emoji_data));
    TRY(generate_emoji_data_implementation(*generated_implementation_file, emoji_data));

    return 0;
}
