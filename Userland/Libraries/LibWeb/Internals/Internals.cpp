/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/InternalsPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Internals/Internals.h>

namespace Web::Internals {

Internals::Internals(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

Internals::~Internals() = default;

JS::ThrowCompletionOr<void> Internals::initialize(JS::Realm& realm)
{
    TRY(Base::initialize(realm));
    Object::set_prototype(&Bindings::ensure_web_prototype<Bindings::InternalsPrototype>(realm, "Internals"));
    return {};
}

void Internals::gc()
{
    vm().heap().collect_garbage();
}

}
