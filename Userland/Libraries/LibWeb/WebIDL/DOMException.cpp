/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::WebIDL {

JS::NonnullGCPtr<DOMException> DOMException::create(JS::Realm& realm, FlyString const& name, FlyString const& message)
{
    return *realm.heap().allocate<DOMException>(realm, realm, name, message);
}

JS::NonnullGCPtr<DOMException> DOMException::construct_impl(JS::Realm& realm, FlyString const& message, FlyString const& name)
{
    return *realm.heap().allocate<DOMException>(realm, realm, name, message);
}

DOMException::DOMException(JS::Realm& realm, FlyString const& name, FlyString const& message)
    : PlatformObject(realm)
    , m_name(name)
    , m_message(message)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "DOMException"));
}

DOMException::~DOMException() = default;

}
