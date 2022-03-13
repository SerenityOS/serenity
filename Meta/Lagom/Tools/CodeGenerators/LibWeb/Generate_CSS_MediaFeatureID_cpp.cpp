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

bool media_feature_accepts_type(MediaFeatureID media_feature_id, MediaFeatureValueType value_type)
{
    switch (media_feature_id) {)~~~");

    json.as_object().for_each_member([&](auto& name, auto& member) {
        VERIFY(member.is_object());
        auto& feature = member.as_object();

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    case MediaFeatureID::@name:titlecase@:)~~~");

        bool have_output_value_type_switch = false;
        if (feature.has("values")) {
            auto append_value_type_switch_if_needed = [&]() {
                if (!have_output_value_type_switch) {
                    member_generator.append(R"~~~(
        switch (value_type) {)~~~");
                }
                have_output_value_type_switch = true;
            };
            auto& values = feature.get("values");
            VERIFY(values.is_array());
            auto& values_array = values.as_array();
            for (auto& type : values_array.values()) {
                VERIFY(type.is_string());
                auto type_name = type.as_string();
                // Skip identifiers.
                if (type_name[0] != '<')
                    continue;
                if (type_name == "<mq-boolean>") {
                    append_value_type_switch_if_needed();
                    member_generator.append(R"~~~(
        case MediaFeatureValueType::Boolean:
            return true;)~~~");
                } else if (type_name == "<integer>") {
                    append_value_type_switch_if_needed();
                    member_generator.append(R"~~~(
        case MediaFeatureValueType::Integer:
            return true;)~~~");
                } else if (type_name == "<length>") {
                    append_value_type_switch_if_needed();
                    member_generator.append(R"~~~(
        case MediaFeatureValueType::Length:
            return true;)~~~");
                } else if (type_name == "<ratio>") {
                    append_value_type_switch_if_needed();
                    member_generator.append(R"~~~(
        case MediaFeatureValueType::Ratio:
            return true;)~~~");
                } else if (type_name == "<resolution>") {
                    append_value_type_switch_if_needed();
                    member_generator.append(R"~~~(
        case MediaFeatureValueType::Resolution:
            return true;)~~~");
                } else {
                    warnln("Unrecognized media-feature value type: `{}`", type_name);
                    VERIFY_NOT_REACHED();
                }
            }
        }
        if (have_output_value_type_switch) {
            member_generator.append(R"~~~(
        default:
            return false;
        })~~~");
        } else {
            member_generator.append(R"~~~(
        return false;)~~~");
        }
    });

    generator.append(R"~~~(
    }
    VERIFY_NOT_REACHED();
}

bool media_feature_accepts_identifier(MediaFeatureID media_feature_id, ValueID identifier)
{
    switch (media_feature_id) {)~~~");

    json.as_object().for_each_member([&](auto& name, auto& member) {
        VERIFY(member.is_object());
        auto& feature = member.as_object();

        auto member_generator = generator.fork();
        member_generator.set("name:titlecase", title_casify(name));
        member_generator.append(R"~~~(
    case MediaFeatureID::@name:titlecase@:)~~~");

        bool have_output_identifier_switch = false;
        if (feature.has("values")) {
            auto append_identifier_switch_if_needed = [&]() {
                if (!have_output_identifier_switch) {
                    member_generator.append(R"~~~(
        switch (identifier) {)~~~");
                }
                have_output_identifier_switch = true;
            };
            auto& values = feature.get("values");
            VERIFY(values.is_array());
            auto& values_array = values.as_array();
            for (auto& identifier : values_array.values()) {
                VERIFY(identifier.is_string());
                auto identifier_name = identifier.as_string();
                // Skip types.
                if (identifier_name[0] == '<')
                    continue;
                append_identifier_switch_if_needed();

                auto ident_generator = member_generator.fork();
                ident_generator.set("identifier:titlecase", title_casify(identifier_name));
                ident_generator.append(R"~~~(
        case ValueID::@identifier:titlecase@:
            return true;)~~~");
            }
        }
        if (have_output_identifier_switch) {
            member_generator.append(R"~~~(
        default:
            return false;
        })~~~");
        } else {
            member_generator.append(R"~~~(
        return false;)~~~");
        }
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
