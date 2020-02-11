/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <stdio.h>

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

    if (argc != 2) {
        fprintf(stderr, "usage: gron <file>\n");
        return 0;
    }
    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Couldn't open %s for reading: %s\n", argv[1], file->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);

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
    print("json", json, trail);
    return 0;
}

static void print(const String& name, const JsonValue& value, Vector<String>& trail)
{
    for (int i = 0; i < trail.size(); ++i)
        printf("%s", trail[i].characters());

    printf("%s%s%s = ", color_name, name.characters(), color_off);

    if (value.is_object()) {
        printf("%s{}%s;\n", color_brace, color_off);
        trail.append(String::format("%s%s%s.", color_name, name.characters(), color_off));
        value.as_object().for_each_member([&](auto& on, auto& ov) { print(on, ov, trail); });
        trail.take_last();
        return;
    }
    if (value.is_array()) {
        printf("%s[]%s;\n", color_brace, color_off);
        trail.append(String::format("%s%s%s", color_name, name.characters(), color_off));
        for (int i = 0; i < value.as_array().size(); ++i) {
            auto element_name = String::format("%s%s[%s%s%d%s%s]%s", color_off, color_brace, color_off, color_index, i, color_off, color_brace, color_off);
            print(element_name, value.as_array()[i], trail);
        }
        trail.take_last();
        return;
    }
    switch (value.type()) {
    case JsonValue::Type::Null:
    case JsonValue::Type::Undefined:
        printf("%s", color_null);
        break;
    case JsonValue::Type::Bool:
        printf("%s", color_bool);
        break;
    case JsonValue::Type::String:
        printf("%s", color_string);
        break;
    default:
        printf("%s", color_index);
        break;
    }

    printf("%s%s;\n", value.serialized<StringBuilder>().characters(), color_off);
}
