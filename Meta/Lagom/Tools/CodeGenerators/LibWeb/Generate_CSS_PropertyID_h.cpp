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

    auto json = JsonValue::from_string(file->read_all()).release_value_but_fixme_should_propagate_errors();
    VERIFY(json.is_object());

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

    json.as_object().for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        if (value.as_object().has("longhands"))
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
const char* string_from_property_id(PropertyID);
bool is_inherited_property(PropertyID);
NonnullRefPtr<StyleValue> property_initial_value(PropertyID);

bool property_accepts_value(PropertyID, StyleValue&);
size_t property_maximum_value_count(PropertyID);

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

    outln("{}", generator.as_string_view());
}
