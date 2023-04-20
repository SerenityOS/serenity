/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/WebDriver/Contexts.h>

namespace Web::WebDriver {

// https://w3c.github.io/webdriver/#dfn-windowproxy-reference-object
JsonObject window_proxy_reference_object(HTML::WindowProxy const& window)
{
    // 1. Let identifier be the web window identifier if the associated browsing context of window is a top-level browsing context.
    //    Otherwise let it be the web frame identifier.
    auto identifier = window.associated_browsing_context()->is_top_level()
        ? WEB_WINDOW_IDENTIFIER
        : WEB_FRAME_IDENTIFIER;

    // 2. Return a JSON Object initialized with the following properties:
    JsonObject object;

    // identifier
    //    Associated window handle of the window’s browsing context.
    object.set(identifier, window.associated_browsing_context()->window_handle().to_deprecated_string());

    return object;
}

}
