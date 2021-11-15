/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
        warnln("usage: {} <path/to/CSS/Identifiers.json>", argv[0]);
        return 1;
    }
    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::OpenMode::ReadOnly))
        return 1;

    auto json = JsonValue::from_string(file->read_all()).release_value_but_fixme_should_propagate_errors();
    VERIFY(json.is_array());

    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Assertions.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {

ValueID value_id_from_string(StringView string)
{
)~~~");

    json.as_array().for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name", name.to_string());
        member_generator.set("name:titlecase", title_casify(name.to_string()));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"))
        return ValueID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return ValueID::Invalid;
}

const char* string_from_value_id(ValueID value_id) {
    switch (value_id) {
)~~~");

    json.as_array().for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name", name.to_string());
        member_generator.set("name:titlecase", title_casify(name.to_string()));
        member_generator.append(R"~~~(
    case ValueID::@name:titlecase@:
        return "@name@";
        )~~~");
    });

    generator.append(R"~~~(
    default:
        return "(invalid CSS::ValueID)";
    }
}

} // namespace Web::CSS
)~~~");

    outln("{}", generator.as_string_view());
}
