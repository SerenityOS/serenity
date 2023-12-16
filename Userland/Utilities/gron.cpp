/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static bool use_color = false;
static void print(StringView name, JsonValue const&, Vector<ByteString>& trail);

static StringView color_name = ""sv;
static StringView color_index = ""sv;
static StringView color_brace = ""sv;
static StringView color_bool = ""sv;
static StringView color_null = ""sv;
static StringView color_string = ""sv;
static StringView color_off = ""sv;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));

    if (isatty(STDOUT_FILENO))
        use_color = true;

    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print each value in a JSON file with its fully expanded key.");

    StringView path;
    args_parser.add_option(use_color, "Colorize output (default on tty)", "colorize", 'c');
    args_parser.add_option(Core::ArgsParser::Option {
        Core::ArgsParser::OptionArgumentMode::None,
        "Monochrome (don't colorize output)",
        "monochrome",
        'm',
        nullptr,
        [](StringView s) {
            VERIFY(s.is_empty());
            use_color = false;
            return true;
        },
    });
    args_parser.add_positional_argument(path, "Input", "input", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));

    if (use_color) {
        color_name = "\033[33;1m"sv;
        color_index = "\033[35;1m"sv;
        color_brace = "\033[36m"sv;
        color_bool = "\033[32;1m"sv;
        color_string = "\033[31;1m"sv;
        color_null = "\033[34;1m"sv;
        color_off = "\033[0m"sv;
    }

    Vector<ByteString> trail;
    print("json"sv, json, trail);
    return 0;
}

static void print(StringView name, JsonValue const& value, Vector<ByteString>& trail)
{
    for (size_t i = 0; i < trail.size(); ++i)
        out("{}", trail[i]);

    out("{}{}{} = ", color_name, name, color_off);

    if (value.is_object()) {
        outln("{}{{}}{};", color_brace, color_off);
        trail.append(ByteString::formatted("{}{}{}.", color_name, name, color_off));
        value.as_object().for_each_member([&](auto& on, auto& ov) { print(on, ov, trail); });
        trail.take_last();
        return;
    }
    if (value.is_array()) {
        outln("{}[]{};", color_brace, color_off);
        trail.append(ByteString::formatted("{}{}{}", color_name, name, color_off));
        for (size_t i = 0; i < value.as_array().size(); ++i) {
            auto element_name = ByteString::formatted("{}{}[{}{}{}{}{}]{}", color_off, color_brace, color_off, color_index, i, color_off, color_brace, color_off);
            print(element_name, value.as_array()[i], trail);
        }
        trail.take_last();
        return;
    }
    switch (value.type()) {
    case JsonValue::Type::Null:
        out("{}", color_null);
        break;
    case JsonValue::Type::Bool:
        out("{}", color_bool);
        break;
    case JsonValue::Type::String:
        out("{}", color_string);
        break;
    default:
        out("{}", color_index);
        break;
    }

    outln("{}{};", value.serialized<StringBuilder>(), color_off);
}
