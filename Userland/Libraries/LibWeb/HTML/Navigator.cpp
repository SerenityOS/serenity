/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/Navigator.h>

namespace Web::HTML {

JS::NonnullGCPtr<Navigator> Navigator::create(JS::Realm& realm)
{
    return *realm.heap().allocate<Navigator>(realm, realm);
}

Navigator::Navigator(JS::Realm& realm)
    : PlatformObject(realm)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "Navigator"));
}

Navigator::~Navigator() = default;

}
