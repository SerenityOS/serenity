/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMRectReadOnly.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Geometry {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRectReadOnly>> DOMRectReadOnly::construct_impl(JS::Realm& realm, double x, double y, double width, double height)
{
    return realm.heap().allocate<DOMRectReadOnly>(realm, realm, x, y, width, height);
}

// https://drafts.fxtf.org/geometry/#create-a-domrect-from-the-dictionary
JS::NonnullGCPtr<DOMRectReadOnly> DOMRectReadOnly::from_rect(JS::VM& vm, Geometry::DOMRectInit const& other)
{
    auto& realm = *vm.current_realm();
    return realm.heap().allocate<DOMRectReadOnly>(realm, realm, other.x, other.y, other.width, other.height);
}

DOMRectReadOnly::DOMRectReadOnly(JS::Realm& realm, double x, double y, double width, double height)
    : PlatformObject(realm)
    , m_rect(x, y, width, height)
{
}

DOMRectReadOnly::~DOMRectReadOnly() = default;

void DOMRectReadOnly::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMRectReadOnlyPrototype>(realm, "DOMRectReadOnly"));
}

}
