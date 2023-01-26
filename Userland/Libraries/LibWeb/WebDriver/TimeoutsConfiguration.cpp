/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibWeb/WebDriver/TimeoutsConfiguration.h>

namespace Web::WebDriver {

// https://w3c.github.io/webdriver/#dfn-timeouts-object
JsonObject timeouts_object(TimeoutsConfiguration const& timeouts)
{
    //  The timeouts object for a timeouts configuration timeouts is an object initialized with the following properties:
    auto timeouts_object = JsonObject {};

    // "script"
    //     timeouts' script timeout value, if set, or its default value.
    if (timeouts.script_timeout.has_value())
        timeouts_object.set("script", *timeouts.script_timeout);
    else
        timeouts_object.set("script", JsonValue {});

    // "pageLoad"
    //     timeouts' page load timeout’s value, if set, or its default value.
    timeouts_object.set("pageLoad", timeouts.page_load_timeout);

    // "implicit"
    //     timeouts' implicit wait timeout’s value, if set, or its default value.
    timeouts_object.set("implicit", timeouts.implicit_wait_timeout);

    return timeouts_object;
}

// https://w3c.github.io/webdriver/#ref-for-dfn-json-deserialize-3
ErrorOr<TimeoutsConfiguration, Error> json_deserialize_as_a_timeouts_configuration(JsonValue const& value)
{
    constexpr i64 max_safe_integer = 9007199254740991;

    // 1. Let timeouts be a new timeouts configuration.
    auto timeouts = TimeoutsConfiguration {};

    // 2. If value is not a JSON Object, return error with error code invalid argument.
    if (!value.is_object())
        return Error::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    // 3. If value has a property with the key "script":
    if (value.as_object().has("script"sv)) {
        // 1. Let script duration be the value of property "script".
        auto script_duration = value.as_object().get("script"sv);

        // 2. If script duration is a number and less than 0 or greater than maximum safe integer, or it is not null, return error with error code invalid argument.
        if (script_duration.has_value() && script_duration->is_number() && (script_duration->to_i64() < 0 || script_duration->to_i64() > max_safe_integer))
            return Error::from_code(ErrorCode::InvalidArgument, "Invalid script duration");
        if (script_duration.has_value() && !script_duration->is_number() && !script_duration->is_null())
            return Error::from_code(ErrorCode::InvalidArgument, "Invalid script duration");

        // 3. Set timeouts’s script timeout to script duration.
        timeouts.script_timeout = (!script_duration.has_value() || script_duration->is_null()) ? Optional<u64> {} : script_duration->to_u64();
    }

    // 4. If value has a property with the key "pageLoad":
    if (value.as_object().has("pageLoad"sv)) {
        // 1. Let page load duration be the value of property "pageLoad".
        auto page_load_duration = value.as_object().get_i64("pageLoad"sv);

        // 2. If page load duration is less than 0 or greater than maximum safe integer, return error with error code invalid argument.
        if (!page_load_duration.has_value() || *page_load_duration < 0 || *page_load_duration > max_safe_integer)
            return Error::from_code(ErrorCode::InvalidArgument, "Invalid page load duration");

        // 3. Set timeouts’s page load timeout to page load duration.
        timeouts.page_load_timeout = static_cast<u64>(*page_load_duration);
    }

    // 5. If value has a property with the key "implicit":
    if (value.as_object().has("implicit"sv)) {
        // 1. Let implicit duration be the value of property "implicit".
        auto implicit_duration = value.as_object().get_i64("implicit"sv);

        // 2. If implicit duration is less than 0 or greater than maximum safe integer, return error with error code invalid argument.
        if (!implicit_duration.has_value() || *implicit_duration < 0 || *implicit_duration > max_safe_integer)
            return Error::from_code(ErrorCode::InvalidArgument, "Invalid implicit duration");

        // 3. Set timeouts’s implicit wait timeout to implicit duration.
        timeouts.implicit_wait_timeout = static_cast<u64>(*implicit_duration);
    }

    // 6. Return success with data timeouts.
    return timeouts;
}

}
