/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <WebDriver/TimeoutsConfiguration.h>

namespace WebDriver {

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

}
