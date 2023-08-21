/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonObject& enums_data, Core::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& enums_data, Core::File& file);

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

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(enums_data, *generated_header_file));
    TRY(generate_implementation_file(enums_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonObject& enums_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(generator.try_append(R"~~~(
#pragma once

#include <AK/Optional.h>

namespace Web::CSS {

enum class ValueID;

)~~~"));

    TRY(enums_data.try_for_each_member([&](auto& name, auto& value) -> ErrorOr<void> {
        VERIFY(value.is_array());
        auto& members = value.as_array();

        auto enum_generator = TRY(generator.fork());
        enum_generator.set("name:titlecase", TRY(title_casify(name)));
        enum_generator.set("name:snakecase", TRY(snake_casify(name)));

        // Find the smallest possible type to use.
        auto member_max_value = members.size() - 1;
        if (NumericLimits<u8>::max() >= member_max_value) {
            enum_generator.set("enum_type", "u8"_string);
        } else if (NumericLimits<u16>::max() >= member_max_value) {
            enum_generator.set("enum_type", "u16"_string);
        } else if (NumericLimits<u32>::max() >= member_max_value) {
            enum_generator.set("enum_type", "u32"_string);
        } else {
            enum_generator.set("enum_type", "u64"_string);
        }

        enum_generator.appendln("enum class @name:titlecase@ : @enum_type@ {");

        for (auto& member : members.values()) {
            auto member_name = member.to_deprecated_string();
            // Don't include aliases in the enum.
            if (member_name.contains('='))
                continue;
            auto member_generator = TRY(enum_generator.fork());
            member_generator.set("member:titlecase", TRY(title_casify(member_name)));
            member_generator.appendln("    @member:titlecase@,");
        }

        enum_generator.appendln("};");
        enum_generator.appendln("Optional<@name:titlecase@> value_id_to_@name:snakecase@(ValueID);");
        enum_generator.appendln("ValueID to_value_id(@name:titlecase@);");
        enum_generator.appendln("StringView to_string(@name:titlecase@);");
        TRY(enum_generator.try_append("\n"));
        return {};
    }));

    generator.appendln("}");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& enums_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(generator.try_append(R"~~~(
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {
)~~~"));

    TRY(enums_data.try_for_each_member([&](auto& name, auto& value) -> ErrorOr<void> {
        VERIFY(value.is_array());
        auto& members = value.as_array();

        auto enum_generator = TRY(generator.fork());
        enum_generator.set("name:titlecase", TRY(title_casify(name)));
        enum_generator.set("name:snakecase", TRY(snake_casify(name)));

        TRY(enum_generator.try_append(R"~~~(
Optional<@name:titlecase@> value_id_to_@name:snakecase@(ValueID value_id)
{
    switch (value_id) {)~~~"));

        for (auto& member : members.values()) {
            auto member_generator = TRY(enum_generator.fork());
            auto member_name = member.to_deprecated_string();
            if (member_name.contains('=')) {
                auto parts = member_name.split_view('=');
                member_generator.set("valueid:titlecase", TRY(title_casify(parts[0])));
                member_generator.set("member:titlecase", TRY(title_casify(parts[1])));
            } else {
                member_generator.set("valueid:titlecase", TRY(title_casify(member_name)));
                member_generator.set("member:titlecase", TRY(title_casify(member_name)));
            }
            TRY(member_generator.try_append(R"~~~(
    case ValueID::@valueid:titlecase@:
        return @name:titlecase@::@member:titlecase@;)~~~"));
        }

        TRY(enum_generator.try_append(R"~~~(
    default:
        return {};
    }
}
)~~~"));

        TRY(enum_generator.try_append(R"~~~(
ValueID to_value_id(@name:titlecase@ @name:snakecase@_value)
{
    switch (@name:snakecase@_value) {)~~~"));

        for (auto& member : members.values()) {
            auto member_generator = TRY(enum_generator.fork());
            auto member_name = member.to_deprecated_string();
            if (member_name.contains('='))
                continue;
            member_generator.set("member:titlecase", TRY(title_casify(member_name)));

            TRY(member_generator.try_append(R"~~~(
    case @name:titlecase@::@member:titlecase@:
        return ValueID::@member:titlecase@;)~~~"));
        }

        TRY(enum_generator.try_append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~"));

        TRY(enum_generator.try_append(R"~~~(
StringView to_string(@name:titlecase@ value)
{
    switch (value) {)~~~"));

        for (auto& member : members.values()) {
            auto member_generator = TRY(enum_generator.fork());
            auto member_name = member.to_deprecated_string();
            if (member_name.contains('='))
                continue;
            member_generator.set("member:css", TRY(String::from_deprecated_string(member_name)));
            member_generator.set("member:titlecase", TRY(title_casify(member_name)));

            TRY(member_generator.try_append(R"~~~(
    case @name:titlecase@::@member:titlecase@:
        return "@member:css@"sv;)~~~"));
        }

        TRY(enum_generator.try_append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~"));
        return {};
    }));

    generator.appendln("}");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
