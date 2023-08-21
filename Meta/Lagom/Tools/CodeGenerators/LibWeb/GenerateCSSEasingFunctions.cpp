/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/GenericLexer.h>
#include <AK/SourceGenerator.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonObject&, Core::File&);
ErrorOr<void> generate_implementation_file(JsonObject&, Core::File&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView functions_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the EasingFunctions header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the EasingFunctions implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(functions_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(functions_json_path));
    VERIFY(json.is_object());
    auto easing_data = json.as_object();

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(easing_data, *generated_header_file));
    TRY(generate_implementation_file(easing_data, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonObject& easing_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Web::CSS {

)~~~");

    generator.appendln("enum class EasingFunction {");
    TRY(easing_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", TRY(title_casify(name)));
        member_generator.appendln("    @name:titlecase@,");
        return {};
    }));
    generator.appendln("};");

    generator.appendln("Optional<EasingFunction> easing_function_from_string(StringView);");
    generator.appendln("StringView to_string(EasingFunction);");

    generator.append(R"~~~(
enum class EasingFunctionParameterType {
    Integer,
    Number,
    NumberZeroToOne,
    StepPosition,
};

struct EasingFunctionParameter {
    EasingFunctionParameterType type;
    bool is_optional { false };
};

struct EasingFunctionMetadata {
    Vector<EasingFunctionParameter> parameters;
};
EasingFunctionMetadata easing_function_metadata(EasingFunction);
)~~~");

    generator.appendln("\n}");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& easing_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <LibWeb/CSS/EasingFunctions.h>
#include <AK/Assertions.h>

namespace Web::CSS {
)~~~");

    generator.append(R"~~~(
Optional<EasingFunction> easing_function_from_string(StringView name)
{
)~~~");
    TRY(easing_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name", TRY(String::from_deprecated_string(name)));
        member_generator.set("name:titlecase", TRY(title_casify(name)));
        member_generator.append(R"~~~(
    if (name.equals_ignoring_ascii_case("@name@"sv))
        return EasingFunction::@name:titlecase@;
)~~~");
        return {};
    }));
    generator.append(R"~~~(
    return {};
}
)~~~");

    generator.append(R"~~~(
StringView to_string(EasingFunction easing_function)
{
    switch (easing_function) {
)~~~");
    TRY(easing_data.try_for_each_member([&](auto& name, auto&) -> ErrorOr<void> {
        auto member_generator = generator.fork();
        member_generator.set("name", TRY(String::from_deprecated_string(name)));
        member_generator.set("name:titlecase", TRY(title_casify(name)));
        member_generator.append(R"~~~(
    case EasingFunction::@name:titlecase@:
        return "@name@"sv;
)~~~");
        return {};
    }));
    generator.append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~");

    generator.append(R"~~~(
EasingFunctionMetadata easing_function_metadata(EasingFunction easing_function)
{
    switch (easing_function) {
)~~~");
    TRY(easing_data.try_for_each_member([&](auto& name, auto& value) -> ErrorOr<void> {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", TRY(title_casify(name)));
        member_generator.append(R"~~~(
    case EasingFunction::@name:titlecase@:
        return EasingFunctionMetadata {
            .parameters = {)~~~");

        if (auto parameters = value.as_object().get_array("parameters"sv); parameters.has_value()) {
            bool first = true;
            // parameters: [ "<foo>", "<foo [0, 1]>" ]
            TRY(parameters.value().try_for_each([&](JsonValue const& value) -> ErrorOr<void> {
                GenericLexer lexer { value.as_string() };
                VERIFY(lexer.consume_specific('<'));
                auto parameter_type_name = lexer.consume_until([](char ch) { return ch == ' ' || ch == '>'; });
                auto has_bounds = false;
                auto is_optional = false;
                if (lexer.consume_specific(" [")) {
                    has_bounds = true;
                    auto contents = lexer.consume_until(']');
                    VERIFY(contents == "0, 1"sv);
                    VERIFY(lexer.consume_specific(']'));
                }
                VERIFY(lexer.consume_specific('>'));
                if (lexer.consume_specific('?'))
                    is_optional = true;

                StringView parameter_type = ""sv;
                if (parameter_type_name == "number"sv)
                    parameter_type = has_bounds ? "NumberZeroToOne"sv : "Number"sv;
                else if (parameter_type_name == "integer"sv)
                    parameter_type = "Integer"sv;
                else if (parameter_type_name == "step-position"sv)
                    parameter_type = "StepPosition"sv;
                else
                    VERIFY_NOT_REACHED();

                member_generator.append(first ? " "sv : ", "sv);
                first = false;

                member_generator.append(TRY(String::formatted(
                    "{{ EasingFunctionParameterType::{}, {} }}",
                    parameter_type,
                    is_optional ? "true"sv : "false"sv)));
                return {};
            }));
        }

        member_generator.append(R"~~~( }
    };
)~~~");
        return {};
    }));
    generator.append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~");

    generator.appendln("\n}");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
