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
    return realm.heap().allocate<DOMException>(realm, realm, name, message).release_allocated_value_but_fixme_should_propagate_errors();
}

JS::NonnullGCPtr<DOMException> DOMException::construct_impl(JS::Realm& realm, DeprecatedFlyString const& message, DeprecatedFlyString const& name)
{
    return realm.heap().allocate<DOMException>(realm, realm, name, message).release_allocated_value_but_fixme_should_propagate_errors();
}

DOMException::DOMException(JS::Realm& realm, DeprecatedFlyString const& name, DeprecatedFlyString const& message)
    : PlatformObject(realm)
    , m_name(name)
    , m_message(message)
{
}

DOMException::~DOMException() = default;

void DOMException::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMExceptionPrototype>(realm, "DOMException"));
}

}
