/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/CrossOriginAbstractOperations.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/WindowObject.h>

namespace Web::Bindings {

// 7.2.3.1 CrossOriginProperties ( O ), https://html.spec.whatwg.org/multipage/browsers.html#crossoriginproperties-(-o-)
Vector<CrossOriginProperty> cross_origin_properties(Variant<LocationObject const*, WindowObject const*> const& object)
{
    // 1. Assert: O is a Location or Window object.

    return object.visit(
        // 2. If O is a Location object, then return « { [[Property]]: "href", [[NeedsGet]]: false, [[NeedsSet]]: true }, { [[Property]]: "replace" } ».
        [](LocationObject const*) -> Vector<CrossOriginProperty> {
            return {
                { .property = "href"sv, .needs_get = false, .needs_set = true },
                { .property = "replace"sv },
            };
        },
        // 3. Return « { [[Property]]: "window", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "self", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "location", [[NeedsGet]]: true, [[NeedsSet]]: true }, { [[Property]]: "close" }, { [[Property]]: "closed", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "focus" }, { [[Property]]: "blur" }, { [[Property]]: "frames", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "length", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "top", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "opener", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "parent", [[NeedsGet]]: true, [[NeedsSet]]: false }, { [[Property]]: "postMessage" } ».
        [](WindowObject const*) -> Vector<CrossOriginProperty> {
            return {
                { .property = "window"sv, .needs_get = true, .needs_set = false },
                { .property = "self"sv, .needs_get = true, .needs_set = false },
                { .property = "location"sv, .needs_get = true, .needs_set = true },
                { .property = "close"sv },
                { .property = "closed"sv, .needs_get = true, .needs_set = false },
                { .property = "focus"sv },
                { .property = "blur"sv },
                { .property = "frames"sv, .needs_get = true, .needs_set = false },
                { .property = "length"sv, .needs_get = true, .needs_set = false },
                { .property = "top"sv, .needs_get = true, .needs_set = false },
                { .property = "opener"sv, .needs_get = true, .needs_set = false },
                { .property = "parent"sv, .needs_get = true, .needs_set = false },
                { .property = "postMessage"sv },
            };
        });
}

}
