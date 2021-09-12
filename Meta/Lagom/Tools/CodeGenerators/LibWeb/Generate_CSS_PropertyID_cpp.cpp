/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

    properties.for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        if (value.as_object().has("initial")) {
            auto& initial_value = value.as_object().get("initial");
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
    });

    generator.append(R"~~~(
    }
    return initial_values.get(property_id).value_or(nullptr);
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

} // namespace Web::CSS

)~~~");

    outln("{}", generator.as_string_view());
}
