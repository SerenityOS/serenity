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
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

class WindowPrototype final : public JS::Object {
    JS_OBJECT(WindowPrototype, JS::Object);

public:
    explicit WindowPrototype(JS::Realm& realm)
        : JS::Object(verify_cast<HTML::Window>(realm.global_object()).cached_web_prototype("EventTarget"))
    {
    }
};

}
