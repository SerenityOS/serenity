/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/CharacterTypes.h>
#include <AK/GenericShorthands.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

void replace_logical_aliases(JsonObject& properties);
ErrorOr<void> generate_header_file(JsonObject& properties, Core::File& file);
ErrorOr<void> generate_implementation_file(JsonObject& properties, Core::File& file);
void generate_bounds_checking_function(JsonObject& properties, SourceGenerator& parent_generator, StringView css_type_name, StringView type_name, Optional<StringView> default_unit_name = {}, Optional<StringView> value_getter = {});

static bool type_name_is_enum(StringView type_name)
{
    return !AK::first_is_one_of(type_name,
        "angle"sv, "color"sv, "custom-ident"sv, "easing-function"sv, "flex"sv, "frequency"sv, "image"sv,
        "integer"sv, "length"sv, "number"sv, "paint"sv, "percentage"sv, "ratio"sv, "rect"sv,
        "resolution"sv, "string"sv, "time"sv, "url"sv);
}

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

    // Check we're in alphabetical order
    DeprecatedString most_recent_name = "";
    properties.for_each_member([&](auto& name, auto&) {
        if (name < most_recent_name) {
            warnln("`{}` is in the wrong position in `{}`. Please keep this list alphabetical!", name, properties_json_path);
            VERIFY_NOT_REACHED();
        }
        most_recent_name = name;
    });

    replace_logical_aliases(properties);

    auto generated_header_file = TRY(Core::File::open(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(Core::File::open(generated_implementation_path, Core::File::OpenMode::Write));

    TRY(generate_header_file(properties, *generated_header_file));
    TRY(generate_implementation_file(properties, *generated_implementation_file));

    return 0;
}

void replace_logical_aliases(JsonObject& properties)
{
    AK::HashMap<DeprecatedString, DeprecatedString> logical_aliases;
    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        const auto& value_as_object = value.as_object();
        const auto logical_alias_for = value_as_object.get_array("logical-alias-for"sv);
        if (logical_alias_for.has_value()) {
            auto const& aliased_properties = logical_alias_for.value();
            for (auto const& aliased_property : aliased_properties.values()) {
                logical_aliases.set(name, aliased_property.to_deprecated_string());
            }
        }
    });

    for (auto& [name, alias] : logical_aliases) {
        auto const maybe_alias_object = properties.get_object(alias);
        if (!maybe_alias_object.has_value()) {
            dbgln("No property '{}' found for logical alias '{}'", alias, name);
            VERIFY_NOT_REACHED();
        }
        JsonObject alias_object = maybe_alias_object.value();

        // Copy over anything the logical property overrides
        properties.get_object(name).value().for_each_member([&](auto& key, auto& value) {
            alias_object.set(key, value);
        });

        properties.set(name, alias_object);
    }
}

ErrorOr<void> generate_header_file(JsonObject& properties, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

enum class PropertyID {
    Invalid,
    Custom,
    All,
)~~~");

    Vector<DeprecatedString> shorthand_property_ids;
    Vector<DeprecatedString> longhand_property_ids;

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

Optional<PropertyID> property_id_from_camel_case_string(StringView);
Optional<PropertyID> property_id_from_string(StringView);
StringView string_from_property_id(PropertyID);
bool is_inherited_property(PropertyID);
NonnullRefPtr<StyleValue> property_initial_value(JS::Realm&, PropertyID);

enum class ValueType {
    Angle,
    Color,
    CustomIdent,
    EasingFunction,
    FilterValueList,
    Flex,
    Frequency,
    Image,
    Integer,
    Length,
    Number,
    Paint,
    Percentage,
    Position,
    Ratio,
    Rect,
    Resolution,
    String,
    Time,
    Url,
};
bool property_accepts_type(PropertyID, ValueType);
bool property_accepts_identifier(PropertyID, ValueID);
Optional<ValueType> property_resolves_percentages_relative_to(PropertyID);

// These perform range-checking, but are also safe to call with properties that don't accept that type. (They'll just return false.)
bool property_accepts_angle(PropertyID, Angle const&);
bool property_accepts_flex(PropertyID, Flex const&);
bool property_accepts_frequency(PropertyID, Frequency const&);
bool property_accepts_integer(PropertyID, i64 const&);
bool property_accepts_length(PropertyID, Length const&);
bool property_accepts_number(PropertyID, double const&);
bool property_accepts_percentage(PropertyID, Percentage const&);
bool property_accepts_resolution(PropertyID, Resolution const&);
bool property_accepts_time(PropertyID, Time const&);

bool property_is_shorthand(PropertyID);
Vector<PropertyID> longhands_for_shorthand(PropertyID);

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
struct Traits<Web::CSS::PropertyID> : public DefaultTraits<Web::CSS::PropertyID> {
    static unsigned hash(Web::CSS::PropertyID property_id) { return int_hash((unsigned)property_id); }
};
} // namespace AK
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

void generate_bounds_checking_function(JsonObject& properties, SourceGenerator& parent_generator, StringView css_type_name, StringView type_name, Optional<StringView> default_unit_name, Optional<StringView> value_getter)
{
    auto generator = parent_generator.fork();
    generator.set("css_type_name", css_type_name);
    generator.set("type_name", type_name);

    generator.append(R"~~~(
bool property_accepts_@css_type_name@(PropertyID property_id, [[maybe_unused]] @type_name@ const& value)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, JsonValue const& value) -> void {
        VERIFY(value.is_object());
        if (auto maybe_valid_types = value.as_object().get_array("valid-types"sv); maybe_valid_types.has_value() && !maybe_valid_types->is_empty()) {
            for (auto valid_type : maybe_valid_types->values()) {
                auto type_and_range = valid_type.as_string().split_view(' ');
                if (type_and_range.first() != css_type_name)
                    continue;

                auto property_generator = generator.fork();
                property_generator.set("property_name:titlecase", title_casify(name));

                property_generator.append(R"~~~(
    case PropertyID::@property_name:titlecase@:
        return )~~~");

                if (type_and_range.size() > 1) {
                    auto range = type_and_range[1];
                    VERIFY(range.starts_with('[') && range.ends_with(']') && range.contains(','));
                    auto comma_index = range.find(',').value();
                    StringView min_value_string = range.substring_view(1, comma_index - 1);
                    StringView max_value_string = range.substring_view(comma_index + 1, range.length() - comma_index - 2);

                    // If the min/max value is infinite, we can just skip that side of the check.
                    if (min_value_string == "-∞")
                        min_value_string = {};
                    if (max_value_string == "∞")
                        max_value_string = {};

                    if (min_value_string.is_empty() && max_value_string.is_empty()) {
                        property_generator.appendln("true;");
                        break;
                    }

                    auto output_check = [&](auto& value_string, StringView comparator) {
                        if (value_getter.has_value()) {
                            property_generator.set("value_number", value_string);
                            property_generator.set("value_getter", value_getter.value());
                            property_generator.set("comparator", comparator);
                            property_generator.append("@value_getter@ @comparator@ @value_number@");
                            return;
                        }

                        GenericLexer lexer { value_string };
                        auto value_number = lexer.consume_until(is_ascii_alpha);
                        auto value_unit = lexer.consume_while(is_ascii_alpha);
                        if (value_unit.is_empty())
                            value_unit = default_unit_name.value();
                        VERIFY(lexer.is_eof());
                        property_generator.set("value_number", value_number);
                        property_generator.set("value_unit", title_casify(value_unit));
                        property_generator.set("comparator", comparator);
                        property_generator.append("value @comparator@ @type_name@(@value_number@, @type_name@::Type::@value_unit@)");
                    };

                    if (!min_value_string.is_empty())
                        output_check(min_value_string, ">="sv);
                    if (!min_value_string.is_empty() && !max_value_string.is_empty())
                        property_generator.append(" && ");
                    if (!max_value_string.is_empty())
                        output_check(max_value_string, "<="sv);
                    property_generator.appendln(";");
                } else {
                    property_generator.appendln("true;");
                }
                break;
            }
        }
    });

    generator.append(R"~~~(
    default:
        return false;
    }
}
)~~~");
}

ErrorOr<void> generate_implementation_file(JsonObject& properties, Core::File& file)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::CSS {

Optional<PropertyID> property_id_from_camel_case_string(StringView string)
{
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.set("name:camelcase", camel_casify(name));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_ascii_case("@name:camelcase@"sv))
        return PropertyID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return {};
}

Optional<PropertyID> property_id_from_string(StringView string)
{
    if (Infra::is_ascii_case_insensitive_match(string, "all"sv))
        return PropertyID::All;
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    if (Infra::is_ascii_case_insensitive_match(string, "@name@"sv))
        return PropertyID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return {};
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
            auto inherited_value = value.as_object().get_bool("inherited"sv);
            VERIFY(inherited_value.has_value());
            inherited = inherited_value.value();
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
            affects_layout = value.as_object().get_bool("affects-layout"sv).value_or(false);

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
            affects_stacking_context = value.as_object().get_bool("affects-stacking-context"sv).value_or(false);

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

NonnullRefPtr<StyleValue> property_initial_value(JS::Realm& context_realm, PropertyID property_id)
{
    static Array<RefPtr<StyleValue>, to_underlying(last_property_id) + 1> initial_values;
    if (auto initial_value = initial_values[to_underlying(property_id)])
        return initial_value.release_nonnull();

    // Lazily parse initial values as needed.
    // This ensures the shorthands will always be able to get the initial values of their longhands.
    // This also now allows a longhand have its own longhand (like background-position-x).

    Parser::ParsingContext parsing_context(context_realm);
    switch (property_id) {
)~~~");

    auto output_initial_value_code = [&](auto& name, auto& object) {
        if (!object.has("initial"sv)) {
            dbgln("No initial value specified for property '{}'", name);
            VERIFY_NOT_REACHED();
        }
        auto initial_value = object.get_deprecated_string("initial"sv);
        VERIFY(initial_value.has_value());
        auto& initial_value_string = initial_value.value();

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.set("initial_value_string", initial_value_string);
        member_generator.append(
            R"~~~(        case PropertyID::@name:titlecase@:
        {
            auto parsed_value = parse_css_value(parsing_context, "@initial_value_string@"sv, PropertyID::@name:titlecase@);
            VERIFY(!parsed_value.is_null());
            auto initial_value = parsed_value.release_nonnull();
            initial_values[to_underlying(PropertyID::@name:titlecase@)] = initial_value;
            return initial_value;
        }
)~~~");
    };

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        output_initial_value_code(name, value.as_object());
    });

    generator.append(
        R"~~~(        default: VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

bool property_has_quirk(PropertyID property_id, Quirk quirk)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("quirks"sv)) {
            auto quirks_value = value.as_object().get_array("quirks"sv);
            VERIFY(quirks_value.has_value());
            auto& quirks = quirks_value.value();

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

bool property_accepts_type(PropertyID property_id, ValueType value_type)
{
    switch (property_id) {
)~~~");
    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        auto& object = value.as_object();
        if (auto maybe_valid_types = object.get_array("valid-types"sv); maybe_valid_types.has_value() && !maybe_valid_types->is_empty()) {
            auto& valid_types = maybe_valid_types.value();
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@: {
        switch (value_type) {
)~~~");

            bool did_output_accepted_type = false;
            for (auto& type : valid_types.values()) {
                VERIFY(type.is_string());
                auto type_name = type.as_string().split_view(' ').first();
                if (type_name_is_enum(type_name))
                    continue;

                if (type_name == "angle") {
                    property_generator.appendln("        case ValueType::Angle:");
                } else if (type_name == "color") {
                    property_generator.appendln("        case ValueType::Color:");
                } else if (type_name == "custom-ident") {
                    property_generator.appendln("        case ValueType::CustomIdent:");
                } else if (type_name == "easing-function") {
                    property_generator.appendln("        case ValueType::EasingFunction:");
                } else if (type_name == "flex") {
                    property_generator.appendln("        case ValueType::Flex:");
                } else if (type_name == "frequency") {
                    property_generator.appendln("        case ValueType::Frequency:");
                } else if (type_name == "image") {
                    property_generator.appendln("        case ValueType::Image:");
                } else if (type_name == "integer") {
                    property_generator.appendln("        case ValueType::Integer:");
                } else if (type_name == "length") {
                    property_generator.appendln("        case ValueType::Length:");
                } else if (type_name == "number") {
                    property_generator.appendln("        case ValueType::Number:");
                } else if (type_name == "paint") {
                    property_generator.appendln("        case ValueType::Paint:");
                } else if (type_name == "percentage") {
                    property_generator.appendln("        case ValueType::Percentage:");
                } else if (type_name == "ratio") {
                    property_generator.appendln("        case ValueType::Ratio:");
                } else if (type_name == "rect") {
                    property_generator.appendln("        case ValueType::Rect:");
                } else if (type_name == "resolution") {
                    property_generator.appendln("        case ValueType::Resolution:");
                } else if (type_name == "string") {
                    property_generator.appendln("        case ValueType::String:");
                } else if (type_name == "time") {
                    property_generator.appendln("        case ValueType::Time:");
                } else if (type_name == "url") {
                    property_generator.appendln("        case ValueType::Url:");
                } else {
                    VERIFY_NOT_REACHED();
                }
                did_output_accepted_type = true;
            }

            if (did_output_accepted_type)
                property_generator.appendln("            return true;");

            property_generator.append(R"~~~(
        default:
            return false;
        }
    }
)~~~");
        }
    });
    generator.append(R"~~~(
    default:
        return false;
    }
}

bool property_accepts_identifier(PropertyID property_id, ValueID identifier)
{
    switch (property_id) {
)~~~");
    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        auto& object = value.as_object();

        auto property_generator = generator.fork();
        property_generator.set("name:titlecase", title_casify(name));
        property_generator.appendln("    case PropertyID::@name:titlecase@: {");

        if (auto maybe_valid_identifiers = object.get_array("valid-identifiers"sv); maybe_valid_identifiers.has_value() && !maybe_valid_identifiers->is_empty()) {
            property_generator.appendln("        switch (identifier) {");
            auto& valid_identifiers = maybe_valid_identifiers.value();
            for (auto& identifier : valid_identifiers.values()) {
                auto identifier_generator = generator.fork();
                identifier_generator.set("identifier:titlecase", title_casify(identifier.as_string()));
                identifier_generator.appendln("        case ValueID::@identifier:titlecase@:");
            }
            property_generator.append(R"~~~(
            return true;
        default:
            break;
        }
)~~~");
        }

        if (auto maybe_valid_types = object.get_array("valid-types"sv); maybe_valid_types.has_value() && !maybe_valid_types->is_empty()) {
            auto& valid_types = maybe_valid_types.value();
            for (auto& valid_type : valid_types.values()) {
                auto type_name = valid_type.as_string().split_view(' ').first();
                if (!type_name_is_enum(type_name))
                    continue;

                auto type_generator = generator.fork();
                type_generator.set("type_name:snakecase", snake_casify(type_name));
                type_generator.append(R"~~~(
        if (value_id_to_@type_name:snakecase@(identifier).has_value())
            return true;
)~~~");
            }
        }
        property_generator.append(R"~~~(
        return false;
    }
)~~~");
    });
    generator.append(R"~~~(
    default:
        return false;
    }
}

Optional<ValueType> property_resolves_percentages_relative_to(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (auto resolved_type = value.as_object().get_deprecated_string("percentages-resolve-to"sv); resolved_type.has_value()) {
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.set("resolved_type:titlecase", title_casify(resolved_type.value()));
            property_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return ValueType::@resolved_type:titlecase@;
)~~~");
        }
    });

    generator.append(R"~~~(
    default:
        return {};
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
            VERIFY(max_values.has_value() && max_values->is_number() && !max_values->is_double());
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.set("max_values", max_values->to_deprecated_string());
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
})~~~");

    generate_bounds_checking_function(properties, generator, "angle"sv, "Angle"sv, "Deg"sv);
    generate_bounds_checking_function(properties, generator, "flex"sv, "Flex"sv, "Fr"sv);
    generate_bounds_checking_function(properties, generator, "frequency"sv, "Frequency"sv, "Hertz"sv);
    generate_bounds_checking_function(properties, generator, "integer"sv, "i64"sv, {}, "value"sv);
    generate_bounds_checking_function(properties, generator, "length"sv, "Length"sv, {}, "value.raw_value()"sv);
    generate_bounds_checking_function(properties, generator, "number"sv, "double"sv, {}, "value"sv);
    generate_bounds_checking_function(properties, generator, "percentage"sv, "Percentage"sv, {}, "value.value()"sv);
    generate_bounds_checking_function(properties, generator, "resolution"sv, "Resolution"sv, "Dpi"sv);
    generate_bounds_checking_function(properties, generator, "time"sv, "Time"sv, "S"sv);

    generator.append(R"~~~(
bool property_is_shorthand(PropertyID property_id)
{
    switch (property_id) {
)~~~");
    properties.for_each_member([&](auto& name, auto& value) {
        if (value.as_object().has("longhands"sv)) {
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            property_generator.append(R"~~~(
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
)~~~");

    generator.append(R"~~~(
Vector<PropertyID> longhands_for_shorthand(PropertyID property_id)
{
    switch (property_id) {
)~~~");
    properties.for_each_member([&](auto& name, auto& value) {
        if (value.as_object().has("longhands"sv)) {
            auto longhands = value.as_object().get("longhands"sv);
            VERIFY(longhands.has_value() && longhands->is_array());
            auto longhand_values = longhands->as_array();
            auto property_generator = generator.fork();
            property_generator.set("name:titlecase", title_casify(name));
            StringBuilder builder;
            bool first = true;
            longhand_values.for_each([&](auto& longhand) {
                if (first)
                    first = false;
                else
                    builder.append(", "sv);
                builder.appendff("PropertyID::{}", title_casify(longhand.to_deprecated_string()));
                return IterationDecision::Continue;
            });
            property_generator.set("longhands", builder.to_deprecated_string());
            property_generator.append(R"~~~(
        case PropertyID::@name:titlecase@:
                return { @longhands@ };
)~~~");
        }
    });

    generator.append(R"~~~(
        default:
                return { };
        }
}
)~~~");

    generator.append(R"~~~(

} // namespace Web::CSS
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}
