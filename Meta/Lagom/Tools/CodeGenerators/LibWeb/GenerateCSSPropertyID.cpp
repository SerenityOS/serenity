/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<void> generate_header_file(JsonObject& properties, Core::Stream::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& properties, Core::Stream::File& file);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView properties_json_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the PropertyID header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the PropertyID implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(properties_json_path, "Path to the JSON file to read from", "json-path", 'j', "json-path");
    args_parser.parse(arguments);

    auto json = TRY(read_entire_file_as_json(properties_json_path));
    VERIFY(json.is_object());
    auto properties = json.as_object();

    auto generated_header_file = TRY(Core::Stream::File::open(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::Stream::File::open(generated_implementation_path, Core::Stream::OpenMode::Write));

    TRY(generate_header_file(properties, *generated_header_file));
    TRY(generate_implementation_file(properties, *generated_implementation_file));

    return 0;
}

ErrorOr<void> generate_header_file(JsonObject& properties, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

enum class PropertyID {
    Invalid,
    Custom,
)~~~");

    Vector<String> shorthand_property_ids;
    Vector<String> longhand_property_ids;

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("longhands"sv))
            shorthand_property_ids.append(name);
        else
            longhand_property_ids.append(name);
    });

    auto first_property_id = shorthand_property_ids.first();
    auto last_property_id = longhand_property_ids.last();

    for (auto& name : shorthand_property_ids) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
    }

    for (auto& name : longhand_property_ids) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
    }

    generator.set("first_property_id", title_casify(first_property_id));
    generator.set("last_property_id", title_casify(last_property_id));

    generator.set("first_shorthand_property_id", title_casify(shorthand_property_ids.first()));
    generator.set("last_shorthand_property_id", title_casify(shorthand_property_ids.last()));

    generator.set("first_longhand_property_id", title_casify(longhand_property_ids.first()));
    generator.set("last_longhand_property_id", title_casify(longhand_property_ids.last()));

    generator.append(R"~~~(
};

PropertyID property_id_from_camel_case_string(StringView);
PropertyID property_id_from_string(StringView);
StringView string_from_property_id(PropertyID);
bool is_inherited_property(PropertyID);
NonnullRefPtr<StyleValue> property_initial_value(PropertyID);

bool property_accepts_value(PropertyID, StyleValue&);
size_t property_maximum_value_count(PropertyID);

bool property_affects_layout(PropertyID);
bool property_affects_stacking_context(PropertyID);

constexpr PropertyID first_property_id = PropertyID::@first_property_id@;
constexpr PropertyID last_property_id = PropertyID::@last_property_id@;
constexpr PropertyID first_shorthand_property_id = PropertyID::@first_shorthand_property_id@;
constexpr PropertyID last_shorthand_property_id = PropertyID::@last_shorthand_property_id@;
constexpr PropertyID first_longhand_property_id = PropertyID::@first_longhand_property_id@;
constexpr PropertyID last_longhand_property_id = PropertyID::@last_longhand_property_id@;

enum class Quirk {
    // https://quirks.spec.whatwg.org/#the-hashless-hex-color-quirk
    HashlessHexColor,
    // https://quirks.spec.whatwg.org/#the-unitless-length-quirk
    UnitlessLength,
};
bool property_has_quirk(PropertyID, Quirk);

} // namespace Web::CSS

namespace AK {
template<>
struct Traits<Web::CSS::PropertyID> : public GenericTraits<Web::CSS::PropertyID> {
    static unsigned hash(Web::CSS::PropertyID property_id) { return int_hash((unsigned)property_id); }
};
} // namespace AK
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<void> generate_implementation_file(JsonObject& properties, Core::Stream::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

PropertyID property_id_from_camel_case_string(StringView string)
{
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.set("name:camelcase", camel_casify(name));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name:camelcase@"sv))
        return PropertyID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return PropertyID::Invalid;
}

PropertyID property_id_from_string(StringView string)
{
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"sv))
        return PropertyID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return PropertyID::Invalid;
}

StringView string_from_property_id(PropertyID property_id) {
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return "@name@"sv;
)~~~");
    });

    generator.append(R"~~~(
    default:
        return "(invalid CSS::PropertyID)"sv;
    }
}

bool is_inherited_property(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        bool inherited = false;
        if (value.as_object().has("inherited"sv)) {
            auto& inherited_value = value.as_object().get("inherited"sv);
            VERIFY(inherited_value.is_bool());
            inherited = inherited_value.as_bool();
        }

        if (inherited) {
            auto member_generator = generator.fork();
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return true;
)~~~");
        }
    });

    generator.append(R"~~~(
    default:
        return false;
    }
}

bool property_affects_layout(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        bool affects_layout = true;
        if (value.as_object().has("affects-layout"sv))
            affects_layout = value.as_object().get("affects-layout"sv).to_bool();

        if (affects_layout) {
            auto member_generator = generator.fork();
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
)~~~");
        }
    });

    generator.append(R"~~~(
        return true;
    default:
        return false;
    }
}

bool property_affects_stacking_context(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        bool affects_stacking_context = false;
        if (value.as_object().has("affects-stacking-context"sv))
            affects_stacking_context = value.as_object().get("affects-stacking-context"sv).to_bool();

        if (affects_stacking_context) {
            auto member_generator = generator.fork();
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
)~~~");
        }
    });

    generator.append(R"~~~(
        return true;
    default:
        return false;
    }
}

NonnullRefPtr<StyleValue> property_initial_value(PropertyID property_id)
{
    static Array<RefPtr<StyleValue>, to_underlying(last_property_id) + 1> initial_values;
    static bool initialized = false;
    if (!initialized) {
        initialized = true;
        Parser::ParsingContext parsing_context;
)~~~");

    // NOTE: Parsing a shorthand property requires that its longhands are already available here.
    //       So, we do this in two passes: First longhands, then shorthands.
    //       Probably we should build a dependency graph and then handle them in order, but this
    //       works for now! :^)

    auto output_initial_value_code = [&](auto& name, auto& object) {
        if (!object.has("initial"sv)) {
            dbgln("No initial value specified for property '{}'", name);
            VERIFY_NOT_REACHED();
        }
        auto& initial_value = object.get("initial"sv);
        VERIFY(initial_value.is_string());
        auto initial_value_string = initial_value.as_string();

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.set("initial_value_string", initial_value_string);
        member_generator.append(R"~~~(
        {
            auto parsed_value = parse_css_value(parsing_context, "@initial_value_string@"sv, PropertyID::@name:titlecase@);
            VERIFY(!parsed_value.is_null());
            initial_values[to_underlying(PropertyID::@name:titlecase@)] = parsed_value.release_nonnull();
        }
)~~~");
    };

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("longhands"sv))
            return;
        output_initial_value_code(name, value.as_object());
    });

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (!value.as_object().has("longhands"sv))
            return;
        output_initial_value_code(name, value.as_object());
    });

    generator.append(R"~~~(
    }

    return *initial_values[to_underlying(property_id)];
}

bool property_has_quirk(PropertyID property_id, Quirk quirk)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("quirks"sv)) {
            auto& quirks_value = value.as_object().get("quirks"sv);
            VERIFY(quirks_value.is_array());
            auto& quirks = quirks_value.as_array();

            if (!quirks.is_empty()) {
                auto property_generator = generator.fork();
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

    generator.append(R"~~~(
    default:
        return false;
    }
}

bool property_accepts_value(PropertyID property_id, StyleValue& style_value)
{
    if (style_value.is_builtin())
        return true;

    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        auto& object = value.as_object();
        bool has_valid_types = object.has("valid-types"sv);
        auto has_valid_identifiers = object.has("valid-identifiers"sv);
        if (has_valid_types || has_valid_identifiers) {
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@: {
)~~~");

            auto output_numeric_value_check = [](SourceGenerator& generator, StringView type_check_function, StringView value_getter, Span<StringView> resolved_type_names, StringView min_value, StringView max_value) {
                auto test_generator = generator.fork();
                test_generator.set("type_check_function", type_check_function);
                test_generator.set("value_getter", value_getter);
                test_generator.append(R"~~~(
        if ((style_value.@type_check_function@())~~~");
                if (!min_value.is_empty() && min_value != "-∞") {
                    test_generator.set("minvalue", min_value);
                    test_generator.append(" && (style_value.@value_getter@ >= @minvalue@)");
                }
                if (!max_value.is_empty() && max_value != "∞") {
                    test_generator.set("maxvalue", max_value);
                    test_generator.append(" && (style_value.@value_getter@ <= @maxvalue@)");
                }
                test_generator.append(")");
                if (!resolved_type_names.is_empty()) {
                    test_generator.append(R"~~~(
        || (style_value.is_calculated() && ()~~~");
                    bool first = true;
                    for (auto& type_name : resolved_type_names) {
                        test_generator.set("resolved_type_name", type_name);
                        if (!first)
                            test_generator.append(" || ");
                        test_generator.append("style_value.as_calculated().resolved_type() == CalculatedStyleValue::ResolvedType::@resolved_type_name@");
                        first = false;
                    }
                    test_generator.append("))");
                }
                test_generator.append(R"~~~() {
            return true;
        }
)~~~");
            };

            if (has_valid_types) {
                auto valid_types_value = object.get("valid-types"sv);
                VERIFY(valid_types_value.is_array());
                auto valid_types = valid_types_value.as_array();
                if (!valid_types.is_empty()) {
                    for (auto& type : valid_types.values()) {
                        VERIFY(type.is_string());
                        auto type_parts = type.as_string().split_view(' ');
                        auto type_name = type_parts.first();
                        auto type_args = type_parts.size() > 1 ? type_parts[1] : ""sv;
                        StringView min_value;
                        StringView max_value;
                        if (!type_args.is_empty()) {
                            VERIFY(type_args.starts_with('[') && type_args.ends_with(']'));
                            auto comma_index = type_args.find(',').value();
                            min_value = type_args.substring_view(1, comma_index - 1);
                            max_value = type_args.substring_view(comma_index + 1, type_args.length() - comma_index - 2);
                        }

                        if (type_name == "angle") {
                            output_numeric_value_check(property_generator, "is_angle"sv, "as_angle().angle().to_degrees()"sv, Array { "Angle"sv }, min_value, max_value);
                        } else if (type_name == "color") {
                            property_generator.append(R"~~~(
        if (style_value.has_color())
            return true;
)~~~");
                        } else if (type_name == "filter-value-list") {
                            property_generator.append(R"~~~(
        if (style_value.is_filter_value_list())
            return true;
)~~~");
                        } else if (type_name == "frequency") {
                            output_numeric_value_check(property_generator, "is_frequency"sv, "as_frequency().frequency().to_hertz()"sv, Array { "Frequency"sv }, min_value, max_value);
                        } else if (type_name == "image") {
                            property_generator.append(R"~~~(
        if (style_value.is_abstract_image())
            return true;
)~~~");
                        } else if (type_name == "integer") {
                            output_numeric_value_check(property_generator, "has_integer"sv, "to_integer()"sv, Array { "Integer"sv }, min_value, max_value);
                        } else if (type_name == "length") {
                            output_numeric_value_check(property_generator, "has_length"sv, "to_length().raw_value()"sv, Array { "Length"sv }, min_value, max_value);
                        } else if (type_name == "number") {
                            output_numeric_value_check(property_generator, "has_number"sv, "to_number()"sv, Array { "Integer"sv, "Number"sv }, min_value, max_value);
                        } else if (type_name == "percentage") {
                            output_numeric_value_check(property_generator, "is_percentage"sv, "as_percentage().percentage().value()"sv, Array { "Percentage"sv }, min_value, max_value);
                        } else if (type_name == "rect") {
                            property_generator.append(R"~~~(
        if (style_value.has_rect())
            return true;
)~~~");
                        } else if (type_name == "resolution") {
                            output_numeric_value_check(property_generator, "is_resolution"sv, "as_resolution().resolution().to_dots_per_pixel()"sv, Array<StringView, 0> {}, min_value, max_value);
                        } else if (type_name == "string") {
                            property_generator.append(R"~~~(
        if (style_value.is_string())
            return true;
)~~~");
                        } else if (type_name == "time") {
                            output_numeric_value_check(property_generator, "is_time"sv, "as_time().time().to_seconds()"sv, Array { "Time"sv }, min_value, max_value);
                        } else if (type_name == "url") {
                            // FIXME: Handle urls!
                        } else {
                            // Assume that any other type names are defined in Enums.json.
                            // If they're not, the output won't compile, but that's fine since it's invalid.
                            property_generator.set("type_name:snakecase", snake_casify(type_name));
                            property_generator.append(R"~~~(
        if (auto converted_identifier = value_id_to_@type_name:snakecase@(style_value.to_identifier()); converted_identifier.has_value())
            return true;
)~~~");
                        }
                    }
                }
            }

            if (has_valid_identifiers) {
                auto valid_identifiers_value = object.get("valid-identifiers"sv);
                VERIFY(valid_identifiers_value.is_array());
                auto valid_identifiers = valid_identifiers_value.as_array();
                if (!valid_identifiers.is_empty()) {
                    property_generator.append(R"~~~(
        switch (style_value.to_identifier()) {
)~~~");
                    for (auto& identifier : valid_identifiers.values()) {
                        VERIFY(identifier.is_string());
                        auto identifier_generator = generator.fork();
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

            generator.append(R"~~~(
        return false;
    }
)~~~");
        }
    });

    generator.append(R"~~~(
    default:
        return true;
    }
}

size_t property_maximum_value_count(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("max-values"sv)) {
            auto max_values = value.as_object().get("max-values"sv);
            VERIFY(max_values.is_number() && !max_values.is_double());
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.set("max_values", max_values.to_string());
            property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return @max_values@;
)~~~");
        }
    });

    generator.append(R"~~~(
    default:
        return 1;
    }
}

} // namespace Web::CSS

)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}
