/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Maxwell Trussell <maxtrussell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static JsonValue query(JsonValue const& value, Vector<StringView>& key_parts, size_t key_index = 0);
static void print(JsonValue const& value, int spaces_per_indent, int indent = 0, bool use_color = true);
static void print_indent(int indent, int spaces_per_indent)
{
    for (int i = 0; i < indent * spaces_per_indent; ++i)
        out(" ");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path;
    StringView dotted_key;
    StringView colorize_output_option = "auto"sv;
    u32 spaces_in_indent = 4;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Pretty-print a JSON file with syntax-coloring and indentation.");
    args_parser.add_option(dotted_key, "Dotted query key", "query", 'q', "foo.*.bar");
    args_parser.add_option(spaces_in_indent, "Indent size", "indent-size", 'i', "spaces_in_indent");
    args_parser.add_option(colorize_output_option, "Choose when to color the output. Valid options are 'always', 'never', or 'auto' (default)", nullptr, 'R', "when");
    args_parser.add_positional_argument(path, "Path to JSON file", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!dotted_key.is_empty()) {
        auto key_parts = dotted_key.split_view('.');
        json = query(json, key_parts);
    }

    bool colorize_output = false;
    if (!colorize_output_option.is_null()) {
        if (colorize_output_option == "always") {
            colorize_output = true;
        } else if (colorize_output_option == "auto") {
            colorize_output = TRY(Core::System::isatty(STDOUT_FILENO));
        } else if (colorize_output_option == "never") {
            colorize_output = false;
        } else {
            warnln("Unknown value '{}' for -R, should be one of 'always', 'never', or 'auto' (default)", colorize_output_option);
            return 1;
        }
    }

    print(json, spaces_in_indent, 0, colorize_output);
    outln();

    return 0;
}

void print(JsonValue const& value, int spaces_per_indent, int indent, bool use_color)
{
    if (value.is_object()) {
        size_t printed_members = 0;
        auto& object = value.as_object();
        outln("{{");
        object.for_each_member([&](auto& member_name, auto& member_value) {
            ++printed_members;
            print_indent(indent + 1, spaces_per_indent);
            if (use_color)
                out("\"\033[33;1m{}\033[0m\": ", member_name);
            else
                out("\"{}\": ", member_name);
            print(member_value, spaces_per_indent, indent + 1, use_color);
            if (printed_members < static_cast<size_t>(object.size()))
                out(",");
            outln();
        });
        print_indent(indent, spaces_per_indent);
        out("}}");
        return;
    }
    if (value.is_array()) {
        size_t printed_entries = 0;
        auto array = value.as_array();
        outln("[");
        array.for_each([&](auto& entry_value) {
            ++printed_entries;
            print_indent(indent + 1, spaces_per_indent);
            print(entry_value, spaces_per_indent, indent + 1, use_color);
            if (printed_entries < static_cast<size_t>(array.size()))
                out(",");
            outln();
        });
        print_indent(indent, spaces_per_indent);
        out("]");
        return;
    }
    if (use_color) {
        if (value.is_string())
            out("\033[31;1m");
        else if (value.is_number())
            out("\033[35;1m");
        else if (value.is_bool())
            out("\033[32;1m");
        else if (value.is_null())
            out("\033[34;1m");
    }
    out("{}", value);
    if (use_color)
        out("\033[0m");
}

JsonValue query(JsonValue const& value, Vector<StringView>& key_parts, size_t key_index)
{
    if (key_index == key_parts.size())
        return value;
    auto key = key_parts[key_index++];

    if (key == "*"sv) {
        Vector<JsonValue> matches;
        if (value.is_object()) {
            value.as_object().for_each_member([&](auto&, auto& member_value) {
                matches.append(query(member_value, key_parts, key_index));
            });
        } else if (value.is_array()) {
            value.as_array().for_each([&](auto& member) {
                matches.append(query(member, key_parts, key_index));
            });
        }
        return JsonValue(JsonArray(matches));
    }

    JsonValue result {};
    if (value.is_object()) {
        result = value.as_object().get(key).value_or({});
    } else if (value.is_array()) {
        auto key_as_index = key.to_number<int>();
        if (key_as_index.has_value())
            result = value.as_array().at(key_as_index.value());
    }
    return query(result, key_parts, key_index);
}
