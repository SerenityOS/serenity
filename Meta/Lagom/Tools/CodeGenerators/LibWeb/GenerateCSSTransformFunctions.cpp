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

ErrorOr<void> generate_header_file(JsonObject& transforms_data, Core::Stream::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& transforms_data, Core::Stream::File& file);

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

    auto generated_header_file = TRY(Core::Stream::File::open(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::Stream::File::open(generated_implementation_path, Core::Stream::OpenMode::Write));

    TRY(generate_header_file(transforms_data, *generated_header_file));
    TRY(generate_implementation_file(transforms_data, *generated_implementation_file));

    return 0;
}

static String title_casify_transform_function(StringView input)
{
    // Transform function names look like `fooBar`, so we just have to make the first character uppercase.
    StringBuilder builder;
    builder.append(toupper(input[0]));
    builder.append(input.substring_view(1));
    return builder.to_string();
}

ErrorOr<void> generate_header_file(JsonObject& transforms_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
namespace Web::CSS {

)~~~");

    generator.appendln("enum class TransformFunction {");
    transforms_data.for_each_member([&](auto& name, auto&) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify_transform_function(name));
        member_generator.appendln("    @name:titlecase@,");
    });
    generator.appendln("};");

    generator.appendln("\n}");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& transforms_data, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
namespace Web::CSS {
)~~~");

    transforms_data.for_each_member([&](auto& name, auto& value) {
        (void)name;
        (void)value;
    });

    generator.appendln("}");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}
