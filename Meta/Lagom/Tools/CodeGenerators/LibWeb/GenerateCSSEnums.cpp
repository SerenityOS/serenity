/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonObject& enums_data, Core::Stream::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& enums_data, Core::Stream::File& file);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView identifiers_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Enums header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Enums implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(identifiers_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(identifiers_json_path));
    VERIFY(json.is_object());
    auto enums_data = json.as_object();

    auto generated_header_file = TRY(Core::Stream::File::open(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::Stream::File::open(generated_implementation_path, Core::Stream::OpenMode::Write));

    TRY(generate_header_file(enums_data, *generated_header_file));
    TRY(generate_implementation_file(enums_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonObject& enums_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>

namespace Web::CSS {

enum class ValueID;

)~~~");

    enums_data.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_array());
        auto& members = value.as_array();

        auto enum_generator = generator.fork();
        enum_generator.set("name:titlecase", title_casify(name));
        enum_generator.set("name:snakecase", snake_casify(name));

        enum_generator.appendln("enum class @name:titlecase@ {");

        for (auto& member : members.values()) {
            auto member_name = member.to_string();
            // Don't include aliases in the enum.
            if (member_name.contains('='))
                continue;
            auto member_generator = enum_generator.fork();
            member_generator.set("member:titlecase", title_casify(member_name));
            member_generator.appendln("    @member:titlecase@,");
        }

        enum_generator.appendln("};");
        enum_generator.appendln("Optional<@name:titlecase@> value_id_to_@name:snakecase@(ValueID);");
        enum_generator.append("\n");
    });

    generator.appendln("}");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& enums_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {
)~~~");

    enums_data.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_array());
        auto& members = value.as_array();

        auto enum_generator = generator.fork();
        enum_generator.set("name:titlecase", title_casify(name));
        enum_generator.set("name:snakecase", snake_casify(name));

        enum_generator.append(R"~~~(
Optional<@name:titlecase@> value_id_to_@name:snakecase@(ValueID value_id)
{
    switch (value_id) {)~~~");

        for (auto& member : members.values()) {
            auto member_generator = enum_generator.fork();
            auto member_name = member.to_string();
            if (member_name.contains('=')) {
                auto parts = member_name.split_view('=');
                member_generator.set("valueid:titlecase", title_casify(parts[0]));
                member_generator.set("member:titlecase", title_casify(parts[1]));
            } else {
                member_generator.set("valueid:titlecase", title_casify(member_name));
                member_generator.set("member:titlecase", title_casify(member_name));
            }
            member_generator.append(R"~~~(
    case ValueID::@valueid:titlecase@:
        return @name:titlecase@::@member:titlecase@;)~~~");
        }

        enum_generator.append(R"~~~(
    default:
        return {};
    }
}
)~~~");
    });

    generator.appendln("}");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}
