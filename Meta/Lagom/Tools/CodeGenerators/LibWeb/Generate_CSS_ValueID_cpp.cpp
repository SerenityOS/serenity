/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.argc != 2) {
        warnln("usage: {} <path/to/CSS/Identifiers.json>", arguments.strings[0]);
        return 1;
    }

    auto json = TRY(read_entire_file_as_json(arguments.strings[1]));
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
    return 0;
}
