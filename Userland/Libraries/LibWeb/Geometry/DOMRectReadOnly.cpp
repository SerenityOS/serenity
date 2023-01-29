/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMRectReadOnly.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMRectReadOnly> DOMRectReadOnly::construct_impl(JS::Realm& realm, double x, double y, double width, double height)
{
    return realm.heap().allocate<DOMRectReadOnly>(realm, realm, x, y, width, height).release_allocated_value_but_fixme_should_propagate_errors();
}

DOMRectReadOnly::DOMRectReadOnly(JS::Realm& realm, double x, double y, double width, double height)
    : PlatformObject(realm)
    , m_rect(x, y, width, height)
{
}

DOMRectReadOnly::~DOMRectReadOnly() = default;

JS::ThrowCompletionOr<void> DOMRectReadOnly::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMRectReadOnlyPrototype>(realm, "DOMRectReadOnly"));

    return {};
}

}
