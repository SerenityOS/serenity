/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static bool use_color = false;
static void print(const String& name, const JsonValue&, Vector<String>& trail);

static const char* color_name = "";
static const char* color_index = "";
static const char* color_brace = "";
static const char* color_bool = "";
static const char* color_null = "";
static const char* color_string = "";
static const char* color_off = "";

int main(int argc, char** argv)
{
    if (pledge("stdio tty rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (isatty(STDOUT_FILENO))
        use_color = true;

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc != 2 || !strcmp(argv[1], "--help")) {
        warnln("usage: gron <file>");
        warnln("Print each value in a JSON file with its fully expanded key.");
        return argc != 2 ? 1 : 0;
    }
    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", file->name(), file->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());

    if (use_color) {
        color_name = "\033[33;1m";
        color_index = "\033[35;1m";
        color_brace = "\033[36m";
        color_bool = "\033[32;1m";
        color_string = "\033[31;1m";
        color_null = "\033[34;1m";
        color_off = "\033[0m";
    }

    Vector<String> trail;
    print("json", json.value(), trail);
    return 0;
}

static void print(const String& name, const JsonValue& value, Vector<String>& trail)
{
    for (size_t i = 0; i < trail.size(); ++i)
        out("{}", trail[i]);

    out("{}{}{} = ", color_name, name, color_off);

    if (value.is_object()) {
        outln("{}{{}}{};", color_brace, color_off);
        trail.append(String::formatted("{}{}{}.", color_name, name, color_off));
        value.as_object().for_each_member([&](auto& on, auto& ov) { print(on, ov, trail); });
        trail.take_last();
        return;
    }
    if (value.is_array()) {
        outln("{}[]{};", color_brace, color_off);
        trail.append(String::formatted("{}{}{}", color_name, name, color_off));
        for (int i = 0; i < value.as_array().size(); ++i) {
            auto element_name = String::formatted("{}{}[{}{}{}{}{}]{}", color_off, color_brace, color_off, color_index, i, color_off, color_brace, color_off);
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
