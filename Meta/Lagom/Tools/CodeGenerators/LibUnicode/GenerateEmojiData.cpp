/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AnyOf.h>
#include <AK/ByteString.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibFileSystem/FileSystem.h>
#include <LibUnicode/Emoji.h>

struct Emoji {
    size_t name { 0 };
    Optional<size_t> image_path;
    Unicode::EmojiGroup group;
    ByteString subgroup;
    u32 display_order { 0 };
    Vector<u32> code_points;
    ByteString encoded_code_points;
    ByteString status;
    size_t code_point_array_index { 0 };
};

struct EmojiData {
    UniqueStringStorage unique_strings;
    Vector<Emoji> emojis;
    Vector<String> emoji_file_list;
};

static void set_image_path_for_emoji(StringView emoji_resource_path, EmojiData& emoji_data, Emoji& emoji)
{
    StringBuilder builder;

    for (auto code_point : emoji.code_points) {
        if (code_point == 0xfe0f)
            continue;
        if (!builder.is_empty())
            builder.append('_');
        builder.appendff("U+{:X}", code_point);
    }

    auto file = ByteString::formatted("{}.png", builder.to_byte_string());
    auto path = ByteString::formatted("{}/{}", emoji_resource_path, file);
    if (!FileSystem::exists(path))
        return;

    emoji.image_path = emoji_data.unique_strings.ensure(move(file));
}

static ErrorOr<void> parse_emoji_test_data(Core::InputBufferedFile& file, EmojiData& emoji_data)
{
    static constexpr auto group_header = "# group: "sv;
    static constexpr auto subgroup_header = "# subgroup: "sv;

    Array<u8, 1024> buffer;

    Unicode::EmojiGroup group;
    ByteString subgroup;
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

static ErrorOr<void> parse_emoji_serenity_data(Core::InputBufferedFile& file, EmojiData& emoji_data)
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

        TRY(line.for_each_split_view(' ', SplitBehavior::Nothing, [&](auto segment) -> ErrorOr<void> {
            if (segment.starts_with(code_point_header)) {
                segment = segment.substring_view(code_point_header.length());

                auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segment);
                VERIFY(code_point.has_value());

                TRY(emoji.code_points.try_append(*code_point));
            } else {
                if (!builder.is_empty())
                    TRY(builder.try_append(' '));
                TRY(builder.try_append(segment));
            }
            return {};
        }));

        auto name = builder.to_byte_string();
        if (!any_of(name, is_ascii_lower_alpha))
            name = name.to_titlecase();

        emoji.name = emoji_data.unique_strings.ensure(move(name));
        TRY(emoji_data.emojis.try_append(move(emoji)));
    }

    return {};
}

static ErrorOr<void> parse_emoji_file_list(Core::InputBufferedFile& file, EmojiData& emoji_data)
{
    HashTable<String> seen_emojis;
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty())
            continue;

        if (seen_emojis.contains(line)) {
            warnln("\x1b[1;31mError!\x1b[0m Duplicate emoji \x1b[35m{}\x1b[0m listed in emoji-file-list.txt.", line);
            return Error::from_errno(EEXIST);
        }

        emoji_data.emoji_file_list.append(TRY(String::from_utf8(line)));
        seen_emojis.set(emoji_data.emoji_file_list.last());
    }

    return {};
}

static ErrorOr<void> validate_emoji(StringView emoji_resource_path, EmojiData& emoji_data)
{
    TRY(Core::Directory::for_each_entry(emoji_resource_path, Core::DirIterator::SkipDots, [&](auto& entry, auto&) -> ErrorOr<IterationDecision> {
        auto lexical_path = LexicalPath(entry.name);
        if (lexical_path.extension() != "png")
            return IterationDecision::Continue;

        auto title = lexical_path.title();
        if (!title.starts_with("U+"sv))
            return IterationDecision::Continue;

        Vector<u32> code_points;
        TRY(title.for_each_split_view('_', SplitBehavior::Nothing, [&](auto segment) -> ErrorOr<void> {
            auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segment.substring_view(2));
            VERIFY(code_point.has_value());

            TRY(code_points.try_append(*code_point));
            return {};
        }));

        auto it = emoji_data.emojis.find_if([&](auto const& emoji) {
            return emoji.code_points == code_points;
        });

        if (it == emoji_data.emojis.end()) {
            warnln("\x1b[1;31mError!\x1b[0m Emoji data for \x1b[35m{}\x1b[0m not found. Please check emoji-test.txt and emoji-serenity.txt.", entry.name);
            return Error::from_errno(ENOENT);
        }

        if (!emoji_data.emoji_file_list.contains_slow(lexical_path.string().view())) {
            warnln("\x1b[1;31mError!\x1b[0m Emoji entry for \x1b[35m{}\x1b[0m not found. Please check emoji-file-list.txt.", lexical_path);
            return Error::from_errno(ENOENT);
        }

        return IterationDecision::Continue;
    }));

    return {};
}

static ErrorOr<void> generate_emoji_data_header(Core::InputBufferedFile& file, EmojiData const&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_emoji_data_implementation(Core::InputBufferedFile& file, EmojiData const& emoji_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("string_index_type"sv, emoji_data.unique_strings.type_that_fits());
    generator.set("emojis_size"sv, ByteString::number(emoji_data.emojis.size()));

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
    generator.set("total_code_point_count", ByteString::number(total_code_point_count));

    generator.append(R"~~~(
static constexpr Array<u32, @total_code_point_count@> s_emoji_code_points { {)~~~");

    bool first = true;
    for (auto const& emoji : emoji_data.emojis) {
        for (auto code_point : emoji.code_points) {
            generator.append(first ? " "sv : ", "sv);
            generator.append(ByteString::formatted("{:#x}", code_point));
            first = false;
        }
    }

    generator.append(" } };"sv);

    generator.append(R"~~~(
struct EmojiData {
    Emoji to_unicode_emoji() const
    {
        Emoji emoji {};
        emoji.name = decode_string(name);
        if (image_path != 0)
            emoji.image_path = decode_string(image_path);
        emoji.group = static_cast<EmojiGroup>(group);
        emoji.display_order = display_order;
        emoji.code_points = code_points();

        return emoji;
    }

    constexpr ReadonlySpan<u32> code_points() const
    {
        return ReadonlySpan<u32>(s_emoji_code_points.data() + code_point_start, code_point_count);
    }

    @string_index_type@ name { 0 };
    @string_index_type@ image_path { 0 };
    u8 group { 0 };
    u32 display_order { 0 };
    size_t code_point_start { 0 };
    size_t code_point_count { 0 };
};
)~~~");

    generator.append(R"~~~(

static constexpr Array<EmojiData, @emojis_size@> s_emojis { {)~~~");

    for (auto const& emoji : emoji_data.emojis) {
        generator.set("name"sv, ByteString::number(emoji.name));
        generator.set("image_path"sv, ByteString::number(emoji.image_path.value_or(0)));
        generator.set("group"sv, ByteString::number(to_underlying(emoji.group)));
        generator.set("display_order"sv, ByteString::number(emoji.display_order));
        generator.set("code_point_start"sv, ByteString::number(emoji.code_point_array_index));
        generator.set("code_point_count"sv, ByteString::number(emoji.code_points.size()));

        generator.append(R"~~~(
    { @name@, @image_path@, @group@, @display_order@, @code_point_start@, @code_point_count@ },)~~~");
    }

    generator.append(R"~~~(
} };

struct EmojiCodePointComparator {
    constexpr int operator()(ReadonlySpan<u32> code_points, EmojiData const& emoji)
    {
        auto emoji_code_points = emoji.code_points();

        if (code_points.size() != emoji_code_points.size())
            return static_cast<int>(code_points.size()) - static_cast<int>(emoji_code_points.size());

        for (size_t i = 0; i < code_points.size(); ++i) {
            if (code_points[i] != emoji_code_points[i])
                return static_cast<int>(code_points[i]) - static_cast<int>(emoji_code_points[i]);
        }

        return 0;
    }
};

Optional<Emoji> find_emoji_for_code_points(ReadonlySpan<u32> code_points)
{
    if (auto const* emoji = binary_search(s_emojis, code_points, nullptr, EmojiCodePointComparator {}))
        return emoji->to_unicode_emoji();
    return {};
}

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_emoji_installation(Core::InputBufferedFile& file, EmojiData const& emoji_data)
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
        generator.append(ByteString::join(" "sv, emoji.code_points, "U+{:X}"sv));
        generator.append(" @name@ (@status@)\n"sv);
    }

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView generated_installation_path;
    StringView emoji_test_path;
    StringView emoji_serenity_path;
    StringView emoji_file_list_path;
    StringView emoji_resource_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(generated_installation_path, "Path to the emoji.txt file to generate", "generated-installation-path", 'i', "generated-installation-path");
    args_parser.add_option(emoji_test_path, "Path to emoji-test.txt file", "emoji-test-path", 'e', "emoji-test-path");
    args_parser.add_option(emoji_serenity_path, "Path to emoji-serenity.txt file", "emoji-serenity-path", 's', "emoji-serenity-path");
    args_parser.add_option(emoji_file_list_path, "Path to the emoji-file-list.txt file", "emoji-file-list-path", 'f', "emoji-file-list-path");
    args_parser.add_option(emoji_resource_path, "Path to the /res/emoji directory", "emoji-resource-path", 'r', "emoji-resource-path");
    args_parser.parse(arguments);

    VERIFY(!emoji_resource_path.is_empty() && FileSystem::exists(emoji_resource_path));

    auto emoji_test_file = TRY(open_file(emoji_test_path, Core::File::OpenMode::Read));

    EmojiData emoji_data {};
    TRY(parse_emoji_test_data(*emoji_test_file, emoji_data));

    if (!emoji_serenity_path.is_empty() && !emoji_file_list_path.is_empty()) {
        auto emoji_serenity_file = TRY(open_file(emoji_serenity_path, Core::File::OpenMode::Read));
        TRY(parse_emoji_serenity_data(*emoji_serenity_file, emoji_data));

        auto emoji_file_list_file = TRY(open_file(emoji_file_list_path, Core::File::OpenMode::Read));
        TRY(parse_emoji_file_list(*emoji_file_list_file, emoji_data));

        TRY(validate_emoji(emoji_resource_path, emoji_data));
    }

    for (auto& emoji : emoji_data.emojis)
        set_image_path_for_emoji(emoji_resource_path, emoji_data, emoji);

    if (!generated_installation_path.is_empty()) {
        TRY(Core::Directory::create(LexicalPath { generated_installation_path }.parent(), Core::Directory::CreateDirectories::Yes));

        auto generated_installation_file = TRY(open_file(generated_installation_path, Core::File::OpenMode::Write));
        TRY(generate_emoji_installation(*generated_installation_file, emoji_data));
    }

    if (!generated_header_path.is_empty()) {
        auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
        TRY(generate_emoji_data_header(*generated_header_file, emoji_data));
    }

    if (!generated_implementation_path.is_empty()) {
        quick_sort(emoji_data.emojis, [](auto const& lhs, auto const& rhs) {
            if (lhs.code_points.size() != rhs.code_points.size())
                return lhs.code_points.size() < rhs.code_points.size();

            for (size_t i = 0; i < lhs.code_points.size(); ++i) {
                if (lhs.code_points[i] < rhs.code_points[i])
                    return true;
                if (lhs.code_points[i] > rhs.code_points[i])
                    return false;
            }

            return false;
        });

        size_t code_point_array_index { 0 };
        for (auto& emoji : emoji_data.emojis) {
            emoji.code_point_array_index = code_point_array_index;
            code_point_array_index += emoji.code_points.size();
        }

        auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));
        TRY(generate_emoji_data_implementation(*generated_implementation_file, emoji_data));
    }

    return 0;
}
