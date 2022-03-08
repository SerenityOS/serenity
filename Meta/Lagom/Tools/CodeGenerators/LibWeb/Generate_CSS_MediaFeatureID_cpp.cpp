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
    generator.append(R"~~~(#include <LibWeb/CSS/MediaFeatureID.h>

namespace Web::CSS {

Optional<MediaFeatureID> media_feature_id_from_string(StringView string)
{)~~~");

    json.as_object().for_each_member([&](auto& name, auto&) {
        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    if (string.equals_ignoring_case("@name@"sv))
        return MediaFeatureID::@name:titlecase@;
)~~~");
    });

    generator.append(R"~~~(
    return {};
}

char const* string_from_media_feature_id(MediaFeatureID media_feature_id)
{
    switch (media_feature_id) {)~~~");

    json.as_object().for_each_member([&](auto& name, auto&) {
        auto member_generator = generator.fork();
        member_generator.set("name", name);
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    case MediaFeatureID::@name:titlecase@:
        return "@name@";)~~~");
    });

    generator.append(R"~~~(
    }
    VERIFY_NOT_REACHED();
}

bool media_feature_type_is_range(MediaFeatureID media_feature_id)
{
    switch (media_feature_id) {)~~~");

    json.as_object().for_each_member([&](auto& name, auto& value) {
        VERIFY(value.is_object());
        auto& feature = value.as_object();

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        VERIFY(feature.has("type"));
        auto feature_type = feature.get("type");
        VERIFY(feature_type.is_string());
        member_generator.set("is_range", feature_type.as_string() == "range" ? "true" : "false");
        member_generator.append(R"~~~(
    case MediaFeatureID::@name:titlecase@:
        return @is_range@;)~~~");
    });

    generator.append(R"~~~(
    }
    VERIFY_NOT_REACHED();
}

}
)~~~");

    outln("{}", generator.as_string_view());

    return 0;
}
