/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
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
        warnln("usage: {} <path/to/CSS/MediaFeatures.json", arguments.strings[0]);
        return 1;
    }

    auto json = TRY(read_entire_file_as_json(arguments.strings[1]));
    VERIFY(json.is_object());

    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.append(R"~~~(#pragma once

#include <AK/StringView.h>
#include <AK/Traits.h>

namespace Web::CSS {

enum class MediaFeatureID {)~~~");

    json.as_object().for_each_member([&](auto& name, auto&) {
        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    @name:titlecase@,)~~~");
    });

    generator.append(R"~~~(
};

Optional<MediaFeatureID> media_feature_id_from_string(StringView);
char const* string_from_media_feature_id(MediaFeatureID);

bool media_feature_type_is_range(MediaFeatureID);

}
)~~~");

    outln("{}", generator.as_string_view());

    return 0;
}
