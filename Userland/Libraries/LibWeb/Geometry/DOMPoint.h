/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Geometry/DOMPointReadOnly.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#DOMPoint
class DOMPoint final : public DOMPointReadOnly {
public:
    using WrapperType = Bindings::DOMPointWrapper;

    static NonnullRefPtr<DOMPoint> create_with_global_object(HTML::Window&, double x = 0, double y = 0, double z = 0, double w = 0)
    {
        return DOMPoint::create(x, y, z, w);
    }

    static NonnullRefPtr<DOMPoint> create(double x = 0, double y = 0, double z = 0, double w = 0)
    {
        return adopt_ref(*new DOMPoint(x, y, z, w));
    }

    double x() const { return m_x; }
    double y() const { return m_y; }
    double z() const { return m_z; }
    double w() const { return m_w; }

    void set_x(double x) { m_x = x; }
    void set_y(double y) { m_y = y; }
    void set_z(double z) { m_z = z; }
    void set_w(double w) { m_w = w; }

private:
    DOMPoint(float x, float y, float z, float w)
        : DOMPointReadOnly(x, y, z, w)
    {
    }
};
}
