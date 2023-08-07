/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Geometry {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> DOMRect::construct_impl(JS::Realm& realm, double x, double y, double width, double height)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<DOMRect>(realm, realm, x, y, width, height));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> DOMRect::create(JS::Realm& realm, Gfx::FloatRect const& rect)
{
    return construct_impl(realm, rect.x(), rect.y(), rect.width(), rect.height());
}

// https://drafts.fxtf.org/geometry/#create-a-domrect-from-the-dictionary
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> DOMRect::from_rect(JS::VM& vm, Geometry::DOMRectInit const& other)
{
    auto& realm = *vm.current_realm();
    return MUST_OR_THROW_OOM(realm.heap().allocate<DOMRect>(realm, realm, other.x, other.y, other.width, other.height));
}

DOMRect::DOMRect(JS::Realm& realm, double x, double y, double width, double height)
    : DOMRectReadOnly(realm, x, y, width, height)
{
}

DOMRect::~DOMRect() = default;

void DOMRect::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMRectPrototype>(realm, "DOMRect"));
}

}
