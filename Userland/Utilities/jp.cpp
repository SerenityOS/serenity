/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static void print(const JsonValue& value, int spaces_per_indent, int indent = 0, bool use_color = true);
static void print_indent(int indent, int spaces_per_indent)
{
    for (int i = 0; i < indent * spaces_per_indent; ++i)
        out(" ");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path;
    int spaces_in_indent = 4;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Pretty-print a JSON file with syntax-coloring and indentation.");
    args_parser.add_option(spaces_in_indent, "Indent size", "indent-size", 'i', "spaces_in_indent");
    args_parser.add_positional_argument(path, "Path to JSON file", "path", Core::ArgsParser::Required::No);
    VERIFY(spaces_in_indent >= 0);
    args_parser.parse(arguments);

    RefPtr<Core::File> file;
    if (path == nullptr)
        file = Core::File::standard_input();
    else
        file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = file->read_all();
    auto json = TRY(JsonValue::from_string(file_contents));

    print(json, spaces_in_indent, 0, isatty(STDOUT_FILENO));
    outln();

    return 0;
}

void print(const JsonValue& value, int spaces_per_indent, int indent, bool use_color)
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
    if (value.is_string())
        out("\"");
    out("{}", value.to_string());
    if (value.is_string())
        out("\"");
    if (use_color)
        out("\033[0m");
}
