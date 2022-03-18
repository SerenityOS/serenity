/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibMain/Main.h>

void generate_code(SourceGenerator namespace_generator, JsonObject const& properties)
{
    {
        auto function_generator = namespace_generator.fork_function("PropertyID", "property_id_from_camel_case_string", "StringView string");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());

            auto member_generator = function_generator.fork();
            member_generator.set("name", name);
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.set("name:camelcase", camel_casify(name));
            member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name:camelcase@"sv))
        return PropertyID::@name:titlecase@;
)~~~");
        });

        function_generator.append(R"~~~(
    return PropertyID::Invalid;
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("PropertyID", "property_id_from_string", "StringView string");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());

            auto member_generator = function_generator.fork();
            member_generator.set("name", name);
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"))
        return PropertyID::@name:titlecase@;
)~~~");
        });

        function_generator.append(R"~~~(
    return PropertyID::Invalid;
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("const char*", "string_from_property_id", "PropertyID property_id");

        function_generator.append(R"~~~(
    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());

            auto member_generator = function_generator.fork();
            member_generator.set("name", name);
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return "@name@";
)~~~");
        });

        function_generator.append(R"~~~(
    default:
        return "(invalid CSS::PropertyID)";
    }
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("bool", "is_inherited_property", "PropertyID property_id");

        function_generator.append(R"~~~(
    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());

            bool inherited = false;
            if (value.as_object().has("inherited")) {
                auto& inherited_value = value.as_object().get("inherited");
                VERIFY(inherited_value.is_bool());
                inherited = inherited_value.as_bool();
            }

            if (inherited) {
                auto member_generator = function_generator.fork();
                member_generator.set("name:titlecase", title_casify(name));
                member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return true;
)~~~");
            }
        });

        function_generator.append(R"~~~(
    default:
        return false;
    }
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("bool", "property_affects_layout", "PropertyID property_id");
        function_generator.append(R"~~~(
    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());

            bool affects_layout = true;
            if (value.as_object().has("affects-layout"))
                affects_layout = value.as_object().get("affects-layout").to_bool();

            if (affects_layout) {
                auto member_generator = function_generator.fork();
                member_generator.set("name:titlecase", title_casify(name));
                member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
)~~~");
            }
        });

        function_generator.append(R"~~~(
        return true;
    default:
        return false;
    }
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("NonnullRefPtr<StyleValue>", "property_initial_value", "PropertyID property_id");
        function_generator.append(R"~~~(
    static Array<RefPtr<StyleValue>, to_underlying(last_property_id) + 1> initial_values;
    static bool initialized = false;
    if (!initialized) {
        initialized = true;
        ParsingContext parsing_context;
)~~~");

        // NOTE: Parsing a shorthand property requires that its longhands are already available here.
        //       So, we do this in two passes: First longhands, then shorthands.
        //       Probably we should build a dependency graph and then handle them in order, but this
        //       works for now! :^)

        auto output_initial_value_code = [&](auto& name, auto& object) {
            if (!object.has("initial")) {
                dbgln("No initial value specified for property '{}'", name);
                VERIFY_NOT_REACHED();
            }
            auto& initial_value = object.get("initial");
            VERIFY(initial_value.is_string());
            auto initial_value_string = initial_value.as_string();

            auto member_generator = function_generator.fork();
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.set("initial_value_string", initial_value_string);
            member_generator.append(R"~~~(
        {
            auto parsed_value = Parser(parsing_context, "@initial_value_string@").parse_as_css_value(PropertyID::@name:titlecase@);
            VERIFY(!parsed_value.is_null());
            initial_values[to_underlying(PropertyID::@name:titlecase@)] = parsed_value.release_nonnull();
        }
)~~~");
        };

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());
            if (value.as_object().has("longhands"))
                return;
            output_initial_value_code(name, value.as_object());
        });

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());
            if (!value.as_object().has("longhands"))
                return;
            output_initial_value_code(name, value.as_object());
        });

        function_generator.append(R"~~~(
    }

    return *initial_values[to_underlying(property_id)];
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("bool", "property_has_quirk", "PropertyID property_id, Quirk quirk");
        function_generator.append(R"~~~(
    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());
            if (value.as_object().has("quirks")) {
                auto& quirks_value = value.as_object().get("quirks");
                VERIFY(quirks_value.is_array());
                auto& quirks = quirks_value.as_array();

                if (!quirks.is_empty()) {
                    auto property_generator = function_generator.fork();
                    property_generator.set("name:titlecase", title_casify(name));
                    property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@: {
        switch (quirk) {
)~~~");
                    for (auto& quirk : quirks.values()) {
                        VERIFY(quirk.is_string());
                        auto quirk_generator = property_generator.fork();
                        quirk_generator.set("quirk:titlecase", title_casify(quirk.as_string()));
                        quirk_generator.append(R"~~~(
        case Quirk::@quirk:titlecase@:
            return true;
)~~~");
                    }
                    property_generator.append(R"~~~(
        default:
            return false;
        }
    }
)~~~");
                }
            }
        });

        function_generator.append(R"~~~(
    default:
        return false;
    }
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("bool", "property_accepts_value", "PropertyID property_id, StyleValue& style_value");
        function_generator.append(R"~~~(
    if (style_value.is_builtin())
        return true;

    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());
            auto& object = value.as_object();
            bool has_valid_types = object.has("valid-types");
            auto has_valid_identifiers = object.has("valid-identifiers");
            if (has_valid_types || has_valid_identifiers) {
                auto property_generator = function_generator.fork();
                property_generator.set("name:titlecase", title_casify(name));
                property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@: {
)~~~");

                if (has_valid_types) {
                    auto valid_types_value = object.get("valid-types");
                    VERIFY(valid_types_value.is_array());
                    auto valid_types = valid_types_value.as_array();
                    if (!valid_types.is_empty()) {
                        for (auto& type : valid_types.values()) {
                            VERIFY(type.is_string());
                            auto type_parts = type.as_string().split_view(' ');
                            auto type_name = type_parts.first();
                            auto type_args = type_parts.size() > 1 ? type_parts[1] : ""sv;
                            if (type_name == "angle") {
                                property_generator.append(R"~~~(
        if (style_value.is_angle()
            || (style_value.is_calculated() && style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Angle))
            return true;
)~~~");
                            } else if (type_name == "color") {
                                property_generator.append(R"~~~(
        if (style_value.has_color())
            return true;
)~~~");
                            } else if (type_name == "frequency") {
                                property_generator.append(R"~~~(
        if (style_value.is_frequency()
            || (style_value.is_calculated() && style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Frequency))
            return true;
)~~~");
                            } else if (type_name == "image") {
                                property_generator.append(R"~~~(
        if (style_value.is_image())
            return true;
)~~~");
                            } else if (type_name == "length") {
                                property_generator.append(R"~~~(
        if (style_value.has_length()
            || (style_value.is_calculated() && style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Length))
            return true;
)~~~");
                            } else if (type_name == "number" || type_name == "integer") {
                                auto test_generator = property_generator.fork();
                                test_generator.set("numbertype", type_name);
                                StringView min_value;
                                StringView max_value;
                                if (!type_args.is_empty()) {
                                    VERIFY(type_args.starts_with('[') && type_args.ends_with(']'));
                                    auto comma_index = type_args.find(',').value();
                                    min_value = type_args.substring_view(1, comma_index - 1);
                                    max_value = type_args.substring_view(comma_index + 1, type_args.length() - comma_index - 2);
                                }
                                test_generator.append(R"~~~(
        if ((style_value.has_@numbertype@())~~~");
                                if (!min_value.is_empty()) {
                                    test_generator.set("minvalue", min_value);
                                    test_generator.append(" && (style_value.to_@numbertype@() >= @minvalue@)");
                                }
                                if (!max_value.is_empty()) {
                                    test_generator.set("maxvalue", max_value);
                                    test_generator.append(" && (style_value.to_@numbertype@() <= @maxvalue@)");
                                }
                                test_generator.append(R"~~~()
            || (style_value.is_calculated() && (style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Integer)~~~");
                                if (type_name == "number")
                                    test_generator.append(R"~~~( || style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Number)~~~");
                                test_generator.append(R"~~~()))
            return true;
)~~~");
                            } else if (type_name == "percentage") {
                                property_generator.append(R"~~~(
        if (style_value.is_percentage()
            || (style_value.is_calculated() && style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Percentage))
            return true;
)~~~");
                            } else if (type_name == "resolution") {
                                property_generator.append(R"~~~(
        if (style_value.is_resolution())
            return true;
)~~~");
                            } else if (type_name == "string") {
                                property_generator.append(R"~~~(
        if (style_value.is_string())
            return true;
)~~~");
                            } else if (type_name == "time") {
                                property_generator.append(R"~~~(
        if (style_value.is_time()
            || (style_value.is_calculated() && style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::Time))
            return true;
)~~~");
                            } else if (type_name == "url") {
                                // FIXME: Handle urls!
                            } else {
                                warnln("Unrecognized valid-type name: '{}'", type_name);
                                VERIFY_NOT_REACHED();
                            }
                        }
                    }
                }

                if (has_valid_identifiers) {
                    auto valid_identifiers_value = object.get("valid-identifiers");
                    VERIFY(valid_identifiers_value.is_array());
                    auto valid_identifiers = valid_identifiers_value.as_array();
                    if (!valid_identifiers.is_empty()) {
                        property_generator.append(R"~~~(
        switch (style_value.to_identifier()) {
)~~~");
                        for (auto& identifier : valid_identifiers.values()) {
                            VERIFY(identifier.is_string());
                            auto identifier_generator = function_generator.fork();
                            identifier_generator.set("identifier:titlecase", title_casify(identifier.as_string()));
                            identifier_generator.append(R"~~~(
        case ValueID::@identifier:titlecase@:
)~~~");
                        }
                        property_generator.append(R"~~~(
            return true;
        default:
            break;
        }
)~~~");
                    }
                }

                function_generator.append(R"~~~(
        return false;
    }
)~~~");
            }
        });

        function_generator.append(R"~~~(
    default:
        return true;
    }
)~~~");
    }

    {
        auto function_generator = namespace_generator.fork_function("size_t", "property_maximum_value_count", "PropertyID property_id");
        function_generator.append(R"~~~(
    switch (property_id) {
)~~~");

        properties.for_each_member([&](auto& name, auto& value) {
            VERIFY(value.is_object());
            if (value.as_object().has("max-values")) {
                auto max_values = value.as_object().get("max-values");
                VERIFY(max_values.is_number() && !max_values.is_double());
                auto property_generator = function_generator.fork();
                property_generator.set("name:titlecase", title_casify(name));
                property_generator.set("max_values", max_values.to_string());
                property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return @max_values@;
)~~~");
            }
        });

        function_generator.append(R"~~~(
    default:
        return 1;
    }
)~~~");
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.argc != 2) {
        warnln("usage: {} <path/to/CSS/Properties.json>", arguments.strings[0]);
        return 1;
    }

    auto json = TRY(read_entire_file_as_json(arguments.strings[1]));
    VERIFY(json.is_object());

    auto& properties = json.as_object();

    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append("\n"); // TODO: Remove

    generator.append_include("AK/Assertions.h");
    generator.append_include("LibWeb/CSS/Parser/Parser.h");
    generator.append_include("LibWeb/CSS/PropertyID.h");
    generator.append_include("LibWeb/CSS/StyleValue.h");

    generate_code(generator.fork_namespace("Web::CSS"), properties);
    generator.append("\n"); // TODO: Remove

    outln("{}", generator.as_string_view());
    return 0;
}
