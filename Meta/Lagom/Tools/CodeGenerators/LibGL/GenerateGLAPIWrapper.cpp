/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/JsonObject.h>
#include <AK/NumericLimits.h>
#include <AK/Optional.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>

namespace {

struct ArgumentDefinition {
    Optional<ByteString> name;
    Optional<ByteString> cpp_type;
    ByteString expression;
    Optional<ByteString> cast_to;
};

struct FunctionDefinition {
    ByteString name;
    ByteString return_type;
    Vector<ArgumentDefinition> arguments;
    ByteString implementation;
    bool unimplemented;
    ByteString variant_gl_type;
};

struct VariantType {
    ByteString encoded_type;
    Optional<ByteString> implementation;
    bool unimplemented;
};

struct Variants {
    Vector<ByteString> api_suffixes { "" };
    Vector<u32> argument_counts { NumericLimits<u32>::max() };
    Vector<ByteString> argument_defaults { "" };
    bool convert_range { false };
    Vector<VariantType> types {
        {
            .encoded_type = "",
            .implementation = Optional<ByteString> {},
            .unimplemented = false,
        }
    };
    ByteString pointer_argument { "" };
};

struct EncodedTypeEntry {
    StringView encoded_type;
    StringView cpp_type;
    StringView gl_type;
};

// clang-format off
constexpr static Array<EncodedTypeEntry, 9> type_definitions = {
    EncodedTypeEntry { "b"sv,  "GLbyte"sv,   "GL_BYTE"sv },
    EncodedTypeEntry { "d"sv,  "GLdouble"sv, "GL_DOUBLE"sv },
    EncodedTypeEntry { "f"sv,  "GLfloat"sv,  "GL_FLOAT"sv },
    EncodedTypeEntry { "i"sv,  "GLint"sv,    "GL_INT"sv },
    EncodedTypeEntry { "s"sv,  "GLshort"sv,  "GL_SHORT"sv },
    EncodedTypeEntry { "ub"sv, "GLubyte"sv,  "GL_UNSIGNED_BYTE"sv },
    EncodedTypeEntry { "ui"sv, "GLuint"sv,   "GL_UNSIGNED_INT"sv },
    EncodedTypeEntry { "us"sv, "GLushort"sv, "GL_UNSIGNED_SHORT"sv },
    EncodedTypeEntry { "x"sv,  "GLfixed"sv,  "GL_INT"sv },
};
// clang-format on

struct EncodedType {
    EncodedTypeEntry type_entry;
    ByteString cpp_type;
    ByteString function_name_suffix;
    bool is_pointer;
    bool is_const_pointer;
};

Vector<ByteString> get_name_list(Optional<JsonValue const&> name_definition)
{
    if (!name_definition.has_value() || name_definition->is_null())
        return {};

    Vector<ByteString, 1> names;
    if (name_definition->is_string()) {
        names.append(name_definition->as_string());
    } else if (name_definition->is_array()) {
        name_definition->as_array().for_each([&names](auto& value) {
            VERIFY(value.is_string());
            names.append(value.as_string());
        });
    } else {
        VERIFY_NOT_REACHED();
    }
    return names;
}

Optional<EncodedType> get_encoded_type(ByteString encoded_type)
{
    bool is_const_pointer = !encoded_type.ends_with('!');
    if (!is_const_pointer)
        encoded_type = encoded_type.substring_view(0, encoded_type.length() - 1);
    ByteString function_name_suffix = encoded_type;

    bool is_pointer = encoded_type.ends_with('v');
    if (is_pointer)
        encoded_type = encoded_type.substring_view(0, encoded_type.length() - 1);

    VERIFY(is_const_pointer || is_pointer);

    Optional<EncodedTypeEntry> type_definition;
    for (size_t i = 0; i < type_definitions.size(); ++i) {
        if (type_definitions[i].encoded_type == encoded_type) {
            type_definition = type_definitions[i];
            break;
        }
    }

    if (!type_definition.has_value())
        return {};

    return EncodedType {
        .type_entry = type_definition.value(),
        .cpp_type = ByteString::formatted(
            "{}{}{}",
            type_definition->cpp_type,
            is_pointer && is_const_pointer ? " const" : "",
            is_pointer ? "*" : ""),
        .function_name_suffix = function_name_suffix,
        .is_pointer = is_pointer,
        .is_const_pointer = is_const_pointer,
    };
}

ByteString wrap_expression_in_range_conversion(ByteString source_type, ByteString target_type, ByteString expression)
{
    VERIFY(target_type == "GLfloat" || target_type == "GLdouble");

    // No range conversion required
    if (source_type == target_type || source_type == "GLdouble")
        return expression;

    if (source_type == "GLbyte")
        return ByteString::formatted("({} + 128.) / 127.5 - 1.", expression);
    else if (source_type == "GLfloat")
        return ByteString::formatted("static_cast<GLdouble>({})", expression);
    else if (source_type == "GLint")
        return ByteString::formatted("({} + 2147483648.) / 2147483647.5 - 1.", expression);
    else if (source_type == "GLshort")
        return ByteString::formatted("({} + 32768.) / 32767.5 - 1.", expression);
    else if (source_type == "GLubyte")
        return ByteString::formatted("{} / 255.", expression);
    else if (source_type == "GLuint")
        return ByteString::formatted("{} / 4294967296.", expression);
    else if (source_type == "GLushort")
        return ByteString::formatted("{} / 65536.", expression);
    VERIFY_NOT_REACHED();
}

Variants read_variants_settings(JsonObject const& variants_obj)
{
    Variants variants;

    if (variants_obj.has_array("argument_counts"sv)) {
        variants.argument_counts.clear_with_capacity();
        variants_obj.get_array("argument_counts"sv)->for_each([&](auto const& argument_count_value) {
            variants.argument_counts.append(argument_count_value.get_u32().value());
        });
    }
    if (variants_obj.has_array("argument_defaults"sv)) {
        variants.argument_defaults.clear_with_capacity();
        variants_obj.get_array("argument_defaults"sv)->for_each([&](auto const& argument_default_value) {
            variants.argument_defaults.append(argument_default_value.as_string());
        });
    }
    if (variants_obj.has_bool("convert_range"sv)) {
        variants.convert_range = variants_obj.get_bool("convert_range"sv).value();
    }
    if (variants_obj.has_array("api_suffixes"sv)) {
        variants.api_suffixes.clear_with_capacity();
        variants_obj.get_array("api_suffixes"sv)->for_each([&](auto const& suffix_value) {
            variants.api_suffixes.append(suffix_value.as_string());
        });
    }
    if (variants_obj.has_string("pointer_argument"sv)) {
        variants.pointer_argument = variants_obj.get_byte_string("pointer_argument"sv).value();
    }
    if (variants_obj.has_object("types"sv)) {
        variants.types.clear_with_capacity();
        variants_obj.get_object("types"sv)->for_each_member([&](auto const& key, auto const& type_value) {
            auto const& type = type_value.as_object();
            variants.types.append(VariantType {
                .encoded_type = key,
                .implementation = type.get_byte_string("implementation"sv),
                .unimplemented = type.get_bool("unimplemented"sv).value_or(false),
            });
        });
    }

    return variants;
}

Vector<ArgumentDefinition> copy_arguments_for_variant(Vector<ArgumentDefinition> arguments, Variants variants,
    u32 argument_count, EncodedType encoded_type)
{
    Vector<ArgumentDefinition> variant_arguments = arguments;
    auto base_cpp_type = encoded_type.type_entry.cpp_type;

    size_t variadic_index = 0;
    for (size_t i = 0; i < variant_arguments.size(); ++i) {
        // Skip arguments with a fixed type
        if (variant_arguments[i].cpp_type.has_value())
            continue;

        variant_arguments[i].cpp_type = encoded_type.cpp_type;
        auto cast_to = variant_arguments[i].cast_to;

        // Pointer argument
        if (encoded_type.is_pointer) {
            variant_arguments[i].name = (variadic_index == 0) ? variants.pointer_argument : Optional<ByteString> {};

            if (variadic_index >= argument_count) {
                // If this variable argument is past the argument count, fall back to the defaults
                variant_arguments[i].expression = variants.argument_defaults[variadic_index];
                variant_arguments[i].cast_to = Optional<ByteString> {};

            } else if (argument_count == 1 && variants.argument_counts.size() == 1) {
                // Otherwise, if the pointer is the only variadic argument, pass it through unchanged
                variant_arguments[i].cast_to = Optional<ByteString> {};

            } else {
                // Otherwise, index into the pointer argument
                auto indexed_expression = ByteString::formatted("{}[{}]", variants.pointer_argument, variadic_index);
                if (variants.convert_range && cast_to.has_value())
                    indexed_expression = wrap_expression_in_range_conversion(base_cpp_type, cast_to.value(), indexed_expression);
                variant_arguments[i].expression = indexed_expression;
            }

        } else {
            // Regular argument
            if (variadic_index >= argument_count) {
                // If the variable argument is past the argument count, fall back to the defaults
                variant_arguments[i].name = Optional<ByteString> {};
                variant_arguments[i].expression = variants.argument_defaults[variadic_index];
                variant_arguments[i].cast_to = Optional<ByteString> {};

            } else if (variants.convert_range && cast_to.has_value()) {
                // Otherwise, if we need to convert the input values, wrap the expression in a range conversion
                variant_arguments[i].expression = wrap_expression_in_range_conversion(
                    base_cpp_type,
                    cast_to.value(),
                    variant_arguments[i].expression);
            }
        }

        // Determine if we can skip casting to the target type
        if (cast_to == base_cpp_type || (variants.convert_range && cast_to == "GLdouble"))
            variant_arguments[i].cast_to = Optional<ByteString> {};

        variadic_index++;
    }

    return variant_arguments;
}

Vector<FunctionDefinition> create_function_definitions(ByteString function_name, JsonObject const& function_definition)
{
    // A single function definition can expand to multiple generated functions by way of:
    //   - differing API suffices (ARB, EXT, etc.);
    //   - differing argument counts;
    //   - differing argument types.
    // These can all be combined.

    // Parse base argument definitions first; these may later be modified by variants
    Vector<ArgumentDefinition> argument_definitions;
    JsonArray const& arguments = function_definition.get_array("arguments"sv).value_or(JsonArray {});
    arguments.for_each([&argument_definitions](auto const& argument_value) {
        VERIFY(argument_value.is_object());
        auto const& argument = argument_value.as_object();

        auto type = argument.get_byte_string("type"sv);
        auto argument_names = get_name_list(argument.get("name"sv));
        auto expression = argument.get_byte_string("expression"sv).value_or("@argument_name@");
        auto cast_to = argument.get_byte_string("cast_to"sv);

        // Add an empty dummy name when all we have is an expression
        if (argument_names.is_empty() && !expression.is_empty())
            argument_names.append("");

        for (auto const& argument_name : argument_names) {
            argument_definitions.append({ .name = argument_name.is_empty() ? Optional<ByteString> {} : argument_name,
                .cpp_type = type,
                .expression = expression,
                .cast_to = cast_to });
        }
    });

    // Create functions for each name and/or variant
    Vector<FunctionDefinition> functions;

    function_name = function_definition.get_byte_string("name"sv).value_or(function_name);
    auto return_type = function_definition.get_byte_string("return_type"sv).value_or("void");
    auto function_implementation = function_definition.get_byte_string("implementation"sv).value_or(function_name.to_snakecase());
    auto function_unimplemented = function_definition.get_bool("unimplemented"sv).value_or(false);

    if (!function_definition.has("variants"sv)) {
        functions.append({
            .name = function_name,
            .return_type = return_type,
            .arguments = argument_definitions,
            .implementation = function_implementation,
            .unimplemented = function_unimplemented,
            .variant_gl_type = "",
        });
        return functions;
    }

    // Read variants settings for this function
    auto variants_obj = function_definition.get_object("variants"sv).value();
    auto variants = read_variants_settings(variants_obj);

    for (auto argument_count : variants.argument_counts) {
        for (auto const& variant_type : variants.types) {
            auto encoded_type = get_encoded_type(variant_type.encoded_type);
            auto variant_arguments = encoded_type.has_value()
                ? copy_arguments_for_variant(argument_definitions, variants, argument_count, encoded_type.value())
                : argument_definitions;
            auto variant_type_implementation = variant_type.implementation.has_value()
                ? variant_type.implementation.value()
                : function_implementation;

            for (auto const& api_suffix : variants.api_suffixes) {
                functions.append({
                    .name = ByteString::formatted(
                        "{}{}{}{}",
                        function_name,
                        variants.argument_counts.size() > 1 ? ByteString::formatted("{}", argument_count) : "",
                        encoded_type.has_value() && variants.types.size() > 1 ? encoded_type->function_name_suffix : "",
                        api_suffix),
                    .return_type = return_type,
                    .arguments = variant_arguments,
                    .implementation = variant_type_implementation,
                    .unimplemented = variant_type.unimplemented || function_unimplemented,
                    .variant_gl_type = encoded_type.has_value() ? encoded_type->type_entry.gl_type : ""sv,
                });
            }
        }
    }

    return functions;
}

ErrorOr<void> generate_header_file(JsonObject& api_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.appendln("#pragma once");
    generator.append("\n");
    generator.appendln("#include <LibGL/GL/glplatform.h>");
    generator.append("\n");
    generator.appendln("#ifdef __cplusplus");
    generator.appendln("extern \"C\" {");
    generator.appendln("#endif");
    generator.append("\n");

    api_data.for_each_member([&](auto& function_name, auto& value) {
        VERIFY(value.is_object());
        auto const& function = value.as_object();
        auto function_definitions = create_function_definitions(function_name, function);

        for (auto const& function_definition : function_definitions) {
            auto function_generator = generator.fork();

            function_generator.set("name", function_definition.name);
            function_generator.set("return_type", function_definition.return_type);

            function_generator.append("GLAPI @return_type@ gl@name@(");

            bool first = true;
            for (auto const& argument_definition : function_definition.arguments) {
                if (!argument_definition.name.has_value() || !argument_definition.cpp_type.has_value())
                    continue;

                auto argument_generator = function_generator.fork();
                argument_generator.set("argument_type", argument_definition.cpp_type.value());
                argument_generator.set("argument_name", argument_definition.name.value());

                if (!first)
                    argument_generator.append(", ");
                first = false;
                argument_generator.append("@argument_type@ @argument_name@");
            }

            function_generator.appendln(");");
        }
    });

    generator.appendln("#ifdef __cplusplus");
    generator.appendln("}");
    generator.appendln("#endif");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& api_data, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.appendln("#include <LibGL/GL/glapi.h>");
    generator.appendln("#include <LibGL/GLContext.h>");
    generator.append("\n");
    generator.appendln("extern GL::GLContext* g_gl_context;");
    generator.append("\n");

    api_data.for_each_member([&](auto& function_name, auto& value) {
        VERIFY(value.is_object());
        JsonObject const& function = value.as_object();
        auto function_definitions = create_function_definitions(function_name, function);

        for (auto const& function_definition : function_definitions) {
            auto function_generator = generator.fork();
            auto return_type = function_definition.return_type;

            function_generator.set("name"sv, function_definition.name);
            function_generator.set("return_type"sv, return_type);
            function_generator.set("implementation"sv, function_definition.implementation);
            function_generator.set("variant_gl_type"sv, function_definition.variant_gl_type);

            function_generator.append("@return_type@ gl@name@(");

            bool first = true;
            for (auto const& argument_definition : function_definition.arguments) {
                if (!argument_definition.name.has_value() || !argument_definition.cpp_type.has_value())
                    continue;

                auto argument_generator = function_generator.fork();
                argument_generator.set("argument_type", argument_definition.cpp_type.value());
                argument_generator.set("argument_name", argument_definition.name.value());

                if (!first)
                    argument_generator.append(", ");
                first = false;
                argument_generator.append("@argument_type@ @argument_name@");
            }
            function_generator.appendln(")");
            function_generator.appendln("{");

            if (function_definition.unimplemented) {
                function_generator.append("    dbgln(\"gl@name@(");

                first = true;
                for (auto const& argument_definition : function_definition.arguments) {
                    if (!argument_definition.name.has_value())
                        continue;
                    if (!first)
                        function_generator.append(", ");
                    first = false;
                    if (argument_definition.cpp_type.value().ends_with('*'))
                        function_generator.append("{:p}");
                    else if (argument_definition.cpp_type.value() == "GLenum")
                        function_generator.append("{:#x}");
                    else
                        function_generator.append("{}");
                }

                function_generator.append("): unimplemented\"");

                for (auto const& argument_definition : function_definition.arguments) {
                    if (!argument_definition.name.has_value())
                        continue;

                    function_generator.append(", ");
                    function_generator.append(argument_definition.name.value());
                }

                function_generator.appendln(");");
                function_generator.appendln("    TODO();");
            } else {
                function_generator.appendln("    if (!g_gl_context)");
                if (return_type.ends_with('*'))
                    function_generator.appendln("        return nullptr;");
                else if (return_type == "GLboolean"sv)
                    function_generator.appendln("        return GL_FALSE;");
                else if (return_type == "GLenum"sv)
                    function_generator.appendln("        return GL_INVALID_OPERATION;");
                else if (return_type == "GLuint"sv)
                    function_generator.appendln("        return 0;");
                else if (return_type == "void"sv)
                    function_generator.appendln("        return;");
                else
                    VERIFY_NOT_REACHED();
                function_generator.append("    ");
                if (return_type != "void"sv)
                    function_generator.append("return ");
                function_generator.append("g_gl_context->gl_@implementation@(");

                first = true;
                for (auto const& argument_definition : function_definition.arguments) {
                    auto argument_generator = function_generator.fork();

                    auto cast_to = argument_definition.cast_to;
                    argument_generator.set("argument_name", argument_definition.name.value_or(""));
                    argument_generator.set("cast_to", cast_to.value_or(""));

                    if (!first)
                        argument_generator.append(", ");
                    first = false;

                    if (cast_to.has_value())
                        argument_generator.append("static_cast<@cast_to@>(");
                    argument_generator.append(argument_definition.expression);
                    if (cast_to.has_value())
                        argument_generator.append(")");
                }

                function_generator.appendln(");");
            }

            function_generator.appendln("}");
            function_generator.append("\n");
        }
    });

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<JsonValue> read_entire_file_as_json(StringView filename)
{
    auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Read));
    auto json_size = TRY(file->size());
    auto json_data = TRY(ByteBuffer::create_uninitialized(json_size));
    TRY(file->read_until_filled(json_data.bytes()));
    return JsonValue::from_string(json_data);
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView api_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the OpenGL API header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the OpenGL API implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(api_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(api_json_path));
    VERIFY(json.is_object());
    auto api_data = json.as_object();

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(api_data, *generated_header_file));
    TRY(generate_implementation_file(api_data, *generated_implementation_file));

    return 0;
}
