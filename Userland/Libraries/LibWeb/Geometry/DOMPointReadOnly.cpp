/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMPointReadOnly.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMPointReadOnly> DOMPointReadOnly::construct_impl(JS::Realm& realm, double x, double y, double z, double w)
{
    return *realm.heap().allocate<DOMPointReadOnly>(realm, realm, x, y, z, w);
}

DOMPointReadOnly::DOMPointReadOnly(JS::Realm& realm, double x, double y, double z, double w)
    : PlatformObject(realm)
    , m_x(x)
    , m_y(y)
    , m_z(z)
    , m_w(w)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "DOMPointReadOnly"));
}

DOMPointReadOnly::~DOMPointReadOnly() = default;

}
