/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <ctype.h>

static String title_casify(const String& dashy_name)
{
    auto parts = dashy_name.split('-');
    StringBuilder builder;
    for (auto& part : parts) {
        if (part.is_empty())
            continue;
        builder.append(toupper(part[0]));
        if (part.length() == 1)
            continue;
        builder.append(part.substring_view(1, part.length() - 1));
    }
    return builder.to_string();
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        warnln("usage: {} <path/to/CSS/Properties.json>", argv[0]);
        return 1;
    }
    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::OpenMode::ReadOnly))
        return 1;

    auto json = JsonValue::from_string(file->read_all());
    VERIFY(json.has_value());
    VERIFY(json.value().is_object());

    auto& properties = json.value().as_object();

    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

PropertyID property_id_from_string(const StringView& string)
{
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"))
        return PropertyID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return PropertyID::Invalid;
}

const char* string_from_property_id(PropertyID property_id) {
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    case PropertyID::@name:titlecase@:
        return "@name@";
)~~~");
    });

    generator.append(R"~~~(
    default:
        return "(invalid CSS::PropertyID)";
    }
}

bool is_inherited_property(PropertyID property_id)
{
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

bool is_pseudo_property(PropertyID property_id)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        bool pseudo = false;
        if (value.as_object().has("pseudo")) {
            auto& pseudo_value = value.as_object().get("pseudo");
            VERIFY(pseudo_value.is_bool());
            pseudo = pseudo_value.as_bool();
        }

        if (pseudo) {
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

RefPtr<StyleValue> property_initial_value(PropertyID property_id)
{
    static HashMap<PropertyID, NonnullRefPtr<StyleValue>> initial_values;
    if (initial_values.is_empty()) {
        ParsingContext parsing_context;
)~~~");

    // NOTE: Parsing a shorthand property requires that its longhands are already available here.
    //       So, we do this in two passes: First longhands, then shorthands.
    //       Probably we should build a dependency graph and then handle them in order, but this
    //       works for now! :^)

    auto output_initial_value_code = [&](auto& name, auto& object) {
        if (object.has("initial")) {
            auto& initial_value = object.get("initial");
            VERIFY(initial_value.is_string());
            auto initial_value_string = initial_value.as_string();

            auto member_generator = generator.fork();
            member_generator.set("name:titlecase", title_casify(name));
            member_generator.set("initial_value_string", initial_value_string);
            member_generator.append(R"~~~(
        if (auto parsed_value = Parser(parsing_context, "@initial_value_string@").parse_as_css_value(PropertyID::@name:titlecase@))
            initial_values.set(PropertyID::@name:titlecase@, parsed_value.release_nonnull());
)~~~");
        }
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

    generator.append(R"~~~(
    }

    auto it = initial_values.find(property_id);
    if (it == initial_values.end())
        return nullptr;
    return it->value;
}

bool property_has_quirk(PropertyID property_id, Quirk quirk)
{
    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("quirks")) {
            auto& quirks_value = value.as_object().get("quirks");
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
    if (style_value.is_builtin() || style_value.is_custom_property())
        return true;

    switch (property_id) {
)~~~");

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        auto& object = value.as_object();
        bool has_valid_types = object.has("valid-types");
        auto has_valid_identifiers = object.has("valid-identifiers");
        if (has_valid_types || has_valid_identifiers) {
            auto property_generator = generator.fork();
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
                        auto type_name = type.as_string();
                        if (type_name == "color") {
                            property_generator.append(R"~~~(
        if (style_value.is_color())
            return true;
)~~~");
                        } else if (type_name == "image") {
                            property_generator.append(R"~~~(
        if (style_value.is_image())
            return true;
)~~~");
                        } else if (type_name == "length" || type_name == "percentage") {
                            // FIXME: Handle lengths and percentages separately
                            property_generator.append(R"~~~(
        if (style_value.is_length() || style_value.is_calculated())
            return true;
)~~~");
                        } else if (type_name == "number" || type_name == "integer") {
                            // FIXME: Handle integers separately
                            property_generator.append(R"~~~(
        if (style_value.is_numeric())
            return true;
)~~~");
                        } else if (type_name == "string") {
                            property_generator.append(R"~~~(
        if (style_value.is_string())
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
        if (value.as_object().has("max-values")) {
            auto max_values = value.as_object().get("max-values");
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

    outln("{}", generator.as_string_view());
}
