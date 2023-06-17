/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/GenericLexer.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonObject& transforms_data, Core::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& transforms_data, Core::File& file);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView identifiers_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the TransformFunctions header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the TransformFunctions implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(identifiers_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(identifiers_json_path));
    VERIFY(json.is_object());
    auto transforms_data = json.as_object();

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(transforms_data, *generated_header_file));
    TRY(generate_implementation_file(transforms_data, *generated_implementation_file));

    return 0;
}

static ErrorOr<String> title_casify_transform_function(StringView input)
{
    // Transform function names look like `fooBar`, so we just have to make the first character uppercase.
    StringBuilder builder;
    TRY(builder.try_append(toupper(input[0])));
    TRY(builder.try_append(input.substring_view(1)));
    return builder.to_string();
}

ErrorOr<void> generate_header_file(JsonObject& transforms_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(generator.try_append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Web::CSS {

)~~~"));

    TRY(generator.try_appendln("enum class TransformFunction {"));
    TRY(transforms_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = TRY(generator.fork());
        TRY(member_generator.set("name:titlecase", TRY(title_casify_transform_function(name))));
        TRY(member_generator.try_appendln("    @name:titlecase@,"));
        return {};
    }));
    TRY(generator.try_appendln("};"));

    TRY(generator.try_appendln("Optional<TransformFunction> transform_function_from_string(StringView);"));
    TRY(generator.try_appendln("StringView to_string(TransformFunction);"));

    TRY(generator.try_append(R"~~~(
enum class TransformFunctionParameterType {
    Angle,
    Length,
    LengthPercentage,
    Number,
};

struct TransformFunctionParameter {
    TransformFunctionParameterType type;
    bool required;
};

struct TransformFunctionMetadata {
    Vector<TransformFunctionParameter> parameters;
};
TransformFunctionMetadata transform_function_metadata(TransformFunction);
)~~~"));

    TRY(generator.try_appendln("\n}"));

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& transforms_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    TRY(generator.try_append(R"~~~(
#include <LibWeb/CSS/TransformFunctions.h>
#include <AK/Assertions.h>

namespace Web::CSS {
)~~~"));

    TRY(generator.try_append(R"~~~(
Optional<TransformFunction> transform_function_from_string(StringView name)
{
)~~~"));
    TRY(transforms_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = TRY(generator.fork());
        TRY(member_generator.set("name", TRY(String::from_deprecated_string(name))));
        TRY(member_generator.set("name:titlecase", TRY(title_casify_transform_function(name))));
        TRY(member_generator.try_append(R"~~~(
    if (name.equals_ignoring_ascii_case("@name@"sv))
        return TransformFunction::@name:titlecase@;
)~~~"));
        return {};
    }));
    TRY(generator.try_append(R"~~~(
    return {};
}
)~~~"));

    TRY(generator.try_append(R"~~~(
StringView to_string(TransformFunction transform_function)
{
    switch (transform_function) {
)~~~"));
    TRY(transforms_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = TRY(generator.fork());
        TRY(member_generator.set("name", TRY(String::from_deprecated_string(name))));
        TRY(member_generator.set("name:titlecase", TRY(title_casify_transform_function(name))));
        TRY(member_generator.try_append(R"~~~(
    case TransformFunction::@name:titlecase@:
        return "@name@"sv;
)~~~"));
        return {};
    }));
    TRY(generator.try_append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~"));

    TRY(generator.try_append(R"~~~(
TransformFunctionMetadata transform_function_metadata(TransformFunction transform_function)
{
    switch (transform_function) {
)~~~"));
    TRY(transforms_data.try_for_each_member([&](auto& name, auto& value) -> ErrorOr<void> {
        VERIFY(value.is_object());

        auto member_generator = TRY(generator.fork());
        TRY(member_generator.set("name:titlecase", TRY(title_casify_transform_function(name))));
        TRY(member_generator.try_append(R"~~~(
    case TransformFunction::@name:titlecase@:
        return TransformFunctionMetadata {
            .parameters = {)~~~"));

        JsonArray const& parameters = value.as_object().get_array("parameters"sv).value();
        bool first = true;
        TRY(parameters.try_for_each([&](JsonValue const& value) -> ErrorOr<void> {
            GenericLexer lexer { value.as_object().get_deprecated_string("type"sv).value() };
            VERIFY(lexer.consume_specific('<'));
            auto parameter_type_name = lexer.consume_until('>');
            VERIFY(lexer.consume_specific('>'));

            StringView parameter_type = ""sv;
            if (parameter_type_name == "angle"sv)
                parameter_type = "Angle"sv;
            else if (parameter_type_name == "length"sv)
                parameter_type = "Length"sv;
            else if (parameter_type_name == "length-percentage"sv)
                parameter_type = "LengthPercentage"sv;
            else if (parameter_type_name == "number"sv)
                parameter_type = "Number"sv;
            else
                VERIFY_NOT_REACHED();

            TRY(member_generator.try_append(first ? " "sv : ", "sv));
            first = false;

            TRY(member_generator.try_append(TRY(String::formatted("{{ TransformFunctionParameterType::{}, {}}}", parameter_type, value.as_object().get("required"sv)->to_deprecated_string()))));
            return {};
        }));

        TRY(member_generator.try_append(R"~~~( }
    };
)~~~"));
        return {};
    }));
    TRY(generator.try_append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~"));

    TRY(generator.try_appendln("\n}"));

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
