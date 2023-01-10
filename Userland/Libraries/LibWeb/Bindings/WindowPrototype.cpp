/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/WindowPrototype.h>

namespace Web::Bindings {

WindowPrototype::WindowPrototype(JS::Realm& realm)
    : JS::Object(realm, nullptr)
{
}

void WindowPrototype::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::EventTargetPrototype>(realm, "EventTarget"));
}

}
