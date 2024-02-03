/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/WebDriver/Contexts.h>

namespace Web::WebDriver {

// https://w3c.github.io/webdriver/#dfn-windowproxy-reference-object
JsonObject window_proxy_reference_object(HTML::WindowProxy const& window)
{
    // 1. Let identifier be the web window identifier if the associated browsing context of window is a top-level browsing context.
    //    Otherwise let it be the web frame identifier.

    // NOTE: We look at the active browsing context's active document's node navigable instead.
    //      Because a Browsing context's top-level traversable is this navigable's top level traversable.
    //      Ref: https://html.spec.whatwg.org/multipage/document-sequences.html#bc-traversable
    auto traversable_navigable = window.associated_browsing_context()->active_document()->navigable()->traversable_navigable();

    auto identifier = traversable_navigable->is_top_level_traversable()
        ? WEB_WINDOW_IDENTIFIER
        : WEB_FRAME_IDENTIFIER;

    // 2. Return a JSON Object initialized with the following properties:
    JsonObject object;

    // identifier
    //    Associated window handle of the window’s browsing context.
    object.set(identifier, traversable_navigable->window_handle().to_byte_string());

    return object;
}

}
