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
#pragma once

#include <AK/StringView.h>
#include <AK/Traits.h>

namespace Web::CSS {

enum class ValueID {
    Invalid,
)~~~");

    json.as_array().for_each([&](auto& name) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name.to_string()));

        member_generator.append(R"~~~(
    @name:titlecase@,
)~~~");
    });

    generator.append(R"~~~(
};

ValueID value_id_from_string(StringView);
const char* string_from_value_id(ValueID);

}

)~~~");

    outln("{}", generator.as_string_view());
    return 0;
}
