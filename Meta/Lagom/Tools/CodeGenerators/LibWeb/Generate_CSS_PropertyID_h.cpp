/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/ByteBuffer.h>
#include <YAK/JsonObject.h>
#include <YAK/SourceGenerator.h>
#include <YAK/StringBuilder.h>
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

    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(
#pragma once

#include <YAK/StringView.h>
#include <YAK/Traits.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

enum class PropertyID {
    Invalid,
    Custom,
)~~~");

    json.value().as_object().for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
    });

    generator.append(R"~~~(
};

PropertyID property_id_from_string(const StringView&);
const char* string_from_property_id(PropertyID);
bool is_inherited_property(PropertyID);
bool is_pseudo_property(PropertyID);
RefPtr<StyleValue> property_initial_value(PropertyID);

} // namespace Web::CSS

namespace YAK {
template<>
struct Traits<Web::CSS::PropertyID> : public GenericTraits<Web::CSS::PropertyID> {
    static unsigned hash(Web::CSS::PropertyID property_id) { return int_hash((unsigned)property_id); }
};
} // namespace YAK
)~~~");

    outln("{}", generator.as_string_view());
}
