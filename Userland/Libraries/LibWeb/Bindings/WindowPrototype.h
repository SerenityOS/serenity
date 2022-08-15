/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/EventTargetPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class WindowPrototype final : public JS::Object {
    JS_OBJECT(WindowPrototype, JS::Object);

public:
    explicit WindowPrototype(JS::Realm& realm)
        : JS::Object(static_cast<WindowObject&>(realm.global_object()).ensure_web_prototype<EventTargetPrototype>("EventTarget"))
    {
    }
};

}
