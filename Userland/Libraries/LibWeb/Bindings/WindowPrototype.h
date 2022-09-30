/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class WindowPrototype final : public JS::Object {
    JS_OBJECT(WindowPrototype, JS::Object);

public:
    explicit WindowPrototype(JS::Realm& realm)
        : JS::Object(cached_web_prototype(realm, "EventTarget"))
    {
    }
};

}
