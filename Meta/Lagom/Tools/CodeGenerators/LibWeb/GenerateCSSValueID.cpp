/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonArray& identifier_data, Core::File& file);
ErrorOr<void> generate_implementation_file(JsonArray& identifier_data, Core::File& file);

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

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(identifier_data, *generated_header_file));
    TRY(generate_implementation_file(identifier_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonArray& identifier_data, Core::File& file)
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

    TRY(identifier_data.try_for_each([&](auto& name) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", TRY(title_casify(name.to_deprecated_string())));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
        return {};
    }));

    generator.append(R"~~~(
};

Optional<ValueID> value_id_from_string(StringView);
StringView string_from_value_id(ValueID);

}

)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonArray& identifier_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <AK/HashMap.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {

HashMap<StringView, ValueID, AK::CaseInsensitiveASCIIStringViewTraits> g_stringview_to_value_id_map {
)~~~");

    TRY(identifier_data.try_for_each([&](auto& name) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name", TRY(String::from_deprecated_string(name.to_deprecated_string())));
        member_generator.set("name:titlecase", TRY(title_casify(name.to_deprecated_string())));
        member_generator.append(R"~~~(
    {"@name@"sv, ValueID::@name:titlecase@},
)~~~");
        return {};
    }));

    generator.append(R"~~~(
};

Optional<ValueID> value_id_from_string(StringView string)
{
    return g_stringview_to_value_id_map.get(string);
}

StringView string_from_value_id(ValueID value_id) {
    switch (value_id) {
)~~~");

    TRY(identifier_data.try_for_each([&](auto& name) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name", TRY(String::from_deprecated_string(name.to_deprecated_string())));
        member_generator.set("name:titlecase", TRY(title_casify(name.to_deprecated_string())));
        member_generator.append(R"~~~(
    case ValueID::@name:titlecase@:
        return "@name@"sv;
        )~~~");
        return {};
    }));

    generator.append(R"~~~(
    default:
        return "(invalid CSS::ValueID)"sv;
    }
}

} // namespace Web::CSS
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
