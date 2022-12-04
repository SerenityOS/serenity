/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AnyOf.h>
#include <AK/DeprecatedString.h>
#include <AK/SourceGenerator.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibCore/Stream.h>
#include <LibUnicode/Emoji.h>

struct Emoji {
    size_t name { 0 };
    Optional<DeprecatedString> image_path;
    Unicode::EmojiGroup group;
    DeprecatedString subgroup;
    u32 display_order { 0 };
    Vector<u32> code_points;
    DeprecatedString encoded_code_points;
    DeprecatedString status;
    size_t code_point_array_index { 0 };
};

struct EmojiData {
    UniqueStringStorage unique_strings;
    Vector<Emoji> emojis;
};

static void set_image_path_for_emoji(StringView emoji_resource_path, Emoji& emoji)
{
    StringBuilder builder;

    for (auto code_point : emoji.code_points) {
        if (code_point == 0xfe0f)
            continue;
        if (!builder.is_empty())
            builder.append('_');
        builder.appendff("U+{:X}", code_point);
    }

    auto path = DeprecatedString::formatted("{}/{}.png", emoji_resource_path, builder.build());
    if (Core::Stream::File::exists(path))
        emoji.image_path = move(path);
}

static ErrorOr<void> parse_emoji_test_data(Core::Stream::BufferedFile& file, EmojiData& emoji_data)
{
    static constexpr auto group_header = "# group: "sv;
    static constexpr auto subgroup_header = "# subgroup: "sv;

    Array<u8, 1024> buffer;

    Unicode::EmojiGroup group;
    DeprecatedString subgroup;
    u32 display_order { 0 };

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty())
            continue;

        if (line.starts_with('#')) {
            if (line.starts_with(group_header)) {
                auto name = line.substring_view(group_header.length());
                group = Unicode::emoji_group_from_string(name);
            } else if (line.starts_with(subgroup_header)) {
                subgroup = line.substring_view(subgroup_header.length());
            }

            continue;
        }

        auto status_index = line.find(';');
        VERIFY(status_index.has_value());

        auto emoji_and_name_index = line.find('#', *status_index);
        VERIFY(emoji_and_name_index.has_value());

        Emoji emoji {};
        emoji.group = group;
        emoji.subgroup = subgroup;
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
        emoji.encoded_code_points = emoji_and_name.substring_view(0, emoji_and_name_spaces[1]).trim_whitespace();
        emoji.status = line.substring_view(*status_index + 1, *emoji_and_name_index - *status_index - 1).trim_whitespace();

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

        line.for_each_split_view(' ', SplitBehavior::Nothing, [&](auto segment) {
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

    generator.set("string_index_type"sv, emoji_data.unique_strings.type_that_fits());
    generator.set("emojis_size"sv, DeprecatedString::number(emoji_data.emojis.size()));

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

    size_t total_code_point_count { 0 };
    for (auto const& emoji : emoji_data.emojis) {
        total_code_point_count += emoji.code_points.size();
    }
    generator.set("total_code_point_count", DeprecatedString::number(total_code_point_count));

    generator.append(R"~~~(
static constexpr Array<u32, @total_code_point_count@> s_emoji_code_points { {)~~~");

    bool first = true;
    for (auto const& emoji : emoji_data.emojis) {
        for (auto code_point : emoji.code_points) {
            generator.append(first ? " "sv : ", "sv);
            generator.append(DeprecatedString::formatted("{:#x}", code_point));
            first = false;
        }
    }

    generator.append(" } };"sv);

    generator.append(R"~~~(
struct EmojiData {
    constexpr Emoji to_unicode_emoji() const
    {
        Emoji emoji {};
        emoji.name = decode_string(name);
        emoji.group = static_cast<EmojiGroup>(group);
        emoji.display_order = display_order;
        emoji.code_points = code_points();

        return emoji;
    }

    constexpr Span<u32 const> code_points() const
    {
        return Span<u32 const>(s_emoji_code_points.data() + code_point_start, code_point_count);
    }

    @string_index_type@ name { 0 };
    u8 group { 0 };
    u32 display_order { 0 };
    size_t code_point_start { 0 };
    size_t code_point_count { 0 };
};
)~~~");

    generator.append(R"~~~(

static constexpr Array<EmojiData, @emojis_size@> s_emojis { {)~~~");

    for (auto const& emoji : emoji_data.emojis) {
        generator.set("name"sv, DeprecatedString::number(emoji.name));
        generator.set("group"sv, DeprecatedString::number(to_underlying(emoji.group)));
        generator.set("display_order"sv, DeprecatedString::number(emoji.display_order));
        generator.set("code_point_start"sv, DeprecatedString::number(emoji.code_point_array_index));
        generator.set("code_point_count"sv, DeprecatedString::number(emoji.code_points.size()));

        generator.append(R"~~~(
    { @name@, @group@, @display_order@, @code_point_start@, @code_point_count@ },)~~~");
    }

    generator.append(R"~~~(
} };

Optional<Emoji> find_emoji_for_code_points(Span<u32 const> code_points)
{
    for (auto& emoji : s_emojis) {
        if (emoji.code_points() == code_points)
            return emoji.to_unicode_emoji();
    }

    return {};
}

}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_emoji_installation(Core::Stream::BufferedFile& file, EmojiData const& emoji_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    auto current_group = Unicode::EmojiGroup::Unknown;
    StringView current_subgroup;

    for (auto const& emoji : emoji_data.emojis) {
        if (!emoji.image_path.has_value())
            continue;
        if (emoji.group == Unicode::EmojiGroup::SerenityOS)
            continue; // SerenityOS emojis are in emoji-serenity.txt

        if (current_group != emoji.group) {
            if (!builder.is_empty())
                generator.append("\n"sv);

            generator.set("group"sv, Unicode::emoji_group_to_string(emoji.group));
            generator.append("# group: @group@\n");

            current_group = emoji.group;
        }

        if (current_subgroup != emoji.subgroup) {
            generator.set("subgroup"sv, emoji.subgroup);
            generator.append("\n# subgroup: @subgroup@\n");

            current_subgroup = emoji.subgroup;
        }

        generator.set("emoji"sv, emoji.encoded_code_points);
        generator.set("name"sv, emoji_data.unique_strings.get(emoji.name));
        generator.set("status"sv, emoji.status);

        generator.append("@emoji@"sv);
        generator.append(" - "sv);
        generator.append(DeprecatedString::join(" "sv, emoji.code_points, "U+{:X}"sv));
        generator.append(" @name@ (@status@)\n"sv);
    }

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView generated_installation_path;
    StringView emoji_test_path;
    StringView emoji_serenity_path;
    StringView emoji_resource_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(generated_installation_path, "Path to the emoji.txt file to generate", "generated-installation-path", 'i', "generated-installation-path");
    args_parser.add_option(emoji_test_path, "Path to emoji-test.txt file", "emoji-test-path", 'e', "emoji-test-path");
    args_parser.add_option(emoji_serenity_path, "Path to emoji-serenity.txt file", "emoji-serenity-path", 's', "emoji-serenity-path");
    args_parser.add_option(emoji_resource_path, "Path to the /res/emoji directory", "emoji-resource-path", 'r', "emoji-resource-path");
    args_parser.parse(arguments);

    auto emoji_test_file = TRY(open_file(emoji_test_path, Core::Stream::OpenMode::Read));
    VERIFY(!emoji_resource_path.is_empty() && Core::Stream::File::exists(emoji_resource_path));

    EmojiData emoji_data {};
    TRY(parse_emoji_test_data(*emoji_test_file, emoji_data));

    if (!emoji_serenity_path.is_empty()) {
        auto emoji_serenity_file = TRY(open_file(emoji_serenity_path, Core::Stream::OpenMode::Read));
        TRY(parse_emoji_serenity_data(*emoji_serenity_file, emoji_data));
    }

    size_t code_point_array_index { 0 };
    for (auto& emoji : emoji_data.emojis) {
        emoji.code_point_array_index = code_point_array_index;
        code_point_array_index += emoji.code_points.size();
    }

    if (!generated_header_path.is_empty()) {
        auto generated_header_file = TRY(open_file(generated_header_path, Core::Stream::OpenMode::Write));
        TRY(generate_emoji_data_header(*generated_header_file, emoji_data));
    }
    if (!generated_implementation_path.is_empty()) {
        auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::Stream::OpenMode::Write));
        TRY(generate_emoji_data_implementation(*generated_implementation_file, emoji_data));
    }

    if (!generated_installation_path.is_empty()) {
        TRY(Core::Directory::create(LexicalPath { generated_installation_path }.parent(), Core::Directory::CreateDirectories::Yes));

        for (auto& emoji : emoji_data.emojis)
            set_image_path_for_emoji(emoji_resource_path, emoji);

        auto generated_installation_file = TRY(open_file(generated_installation_path, Core::Stream::OpenMode::Write));
        TRY(generate_emoji_installation(*generated_installation_file, emoji_data));
    }

    return 0;
}
