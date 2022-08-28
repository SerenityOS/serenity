/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Point.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#dompointreadonly
class DOMPointReadOnly
    : public RefCounted<DOMPointReadOnly>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMPointReadOnlyWrapper;

    static NonnullRefPtr<DOMPointReadOnly> create_with_global_object(HTML::Window&, double x = 0, double y = 0, double z = 0, double w = 0)
    {
        return DOMPointReadOnly::create(x, y, z, w);
    }

    static NonnullRefPtr<DOMPointReadOnly> create(double x = 0, double y = 0, double z = 0, double w = 0)
    {
        return adopt_ref(*new DOMPointReadOnly(x, y, z, w));
    }

    double x() const { return m_x; }
    double y() const { return m_y; }
    double z() const { return m_z; }
    double w() const { return m_w; }

protected:
    DOMPointReadOnly(double x, double y, double z, double w)
        : m_x(x)
        , m_y(y)
        , m_z(z)
        , m_w(w)
    {
    }

    double m_x;
    double m_y;
    double m_z;
    double m_w;
};

}
