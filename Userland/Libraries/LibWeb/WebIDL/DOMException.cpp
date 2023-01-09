/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::WebIDL {

JS::NonnullGCPtr<DOMException> DOMException::create(JS::Realm& realm, DeprecatedFlyString const& name, DeprecatedFlyString const& message)
{
    return realm.heap().allocate<DOMException>(realm, realm, name, message);
}

JS::NonnullGCPtr<DOMException> DOMException::construct_impl(JS::Realm& realm, DeprecatedFlyString const& message, DeprecatedFlyString const& name)
{
    return realm.heap().allocate<DOMException>(realm, realm, name, message);
}

DOMException::DOMException(JS::Realm& realm, DeprecatedFlyString const& name, DeprecatedFlyString const& message)
    : PlatformObject(realm)
    , m_name(name)
    , m_message(message)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "DOMException"));
}

DOMException::~DOMException() = default;

}
