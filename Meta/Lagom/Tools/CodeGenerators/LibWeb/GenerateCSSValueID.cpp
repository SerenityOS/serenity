/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonArray& identifier_data, Core::Stream::File& file);
ErrorOr<void> generate_implementation_file(JsonArray& identifier_data, Core::Stream::File& file);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView identifiers_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the ValueID header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the ValueID implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(identifiers_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(identifiers_json_path));
    VERIFY(json.is_array());
    auto identifier_data = json.as_array();

    auto generated_header_file = TRY(Core::Stream::File::open(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::Stream::File::open(generated_implementation_path, Core::Stream::OpenMode::Write));

    TRY(generate_header_file(identifier_data, *generated_header_file));
    TRY(generate_implementation_file(identifier_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonArray& identifier_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#pragma once

#include <AK/StringView.h>
#include <AK/Traits.h>

namespace Web::CSS {

enum class ValueID {
    Invalid,
)~~~");

    identifier_data.for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name.to_string()));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
    });

    generator.append(R"~~~(
};

ValueID value_id_from_string(StringView);
const char* string_from_value_id(ValueID);

}

)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonArray& identifier_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {

ValueID value_id_from_string(StringView string)
{
)~~~");

    identifier_data.for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name", name.to_string());
        member_generator.set("name:titlecase", title_casify(name.to_string()));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"))
        return ValueID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return ValueID::Invalid;
}

const char* string_from_value_id(ValueID value_id) {
    switch (value_id) {
)~~~");

    identifier_data.for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name", name.to_string());
        member_generator.set("name:titlecase", title_casify(name.to_string()));
        member_generator.append(R"~~~(
    case ValueID::@name:titlecase@:
        return "@name@";
        )~~~");
    });

    generator.append(R"~~~(
    default:
        return "(invalid CSS::ValueID)";
    }
}

} // namespace Web::CSS
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}
