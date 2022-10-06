/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMPoint.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMPoint> DOMPoint::construct_impl(JS::Realm& realm, double x, double y, double z, double w)
{
    return *realm.heap().allocate<DOMPoint>(realm, realm, x, y, z, w);
}

DOMPoint::DOMPoint(JS::Realm& realm, double x, double y, double z, double w)
    : DOMPointReadOnly(realm, x, y, z, w)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "DOMPoint"));
}

// https://drafts.fxtf.org/geometry/#dom-dompoint-frompoint
JS::NonnullGCPtr<DOMPoint> DOMPoint::from_point(JS::VM& vm, DOMPointInit const& other)
{
    // The fromPoint(other) static method on DOMPoint must create a DOMPoint from the dictionary other.
    return construct_impl(*vm.current_realm(), other.x, other.y, other.z, other.w);
}

DOMPoint::~DOMPoint() = default;

}
