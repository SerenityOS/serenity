/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMPointReadOnly.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMPointReadOnly> DOMPointReadOnly::construct_impl(JS::Realm& realm, double x, double y, double z, double w)
{
    return realm.heap().allocate<DOMPointReadOnly>(realm, realm, x, y, z, w);
}

DOMPointReadOnly::DOMPointReadOnly(JS::Realm& realm, double x, double y, double z, double w)
    : PlatformObject(realm)
    , m_x(x)
    , m_y(y)
    , m_z(z)
    , m_w(w)
{
}

// https://drafts.fxtf.org/geometry/#dom-dompointreadonly-frompoint
JS::NonnullGCPtr<DOMPointReadOnly> DOMPointReadOnly::from_point(JS::VM& vm, DOMPointInit const& other)
{
    // The fromPoint(other) static method on DOMPointReadOnly must create a DOMPointReadOnly from the dictionary other.
    return construct_impl(*vm.current_realm(), other.x, other.y, other.z, other.w);
}

DOMPointReadOnly::~DOMPointReadOnly() = default;

void DOMPointReadOnly::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMPointReadOnlyPrototype>(realm, "DOMPointReadOnly"));
}

}
