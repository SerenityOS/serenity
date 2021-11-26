/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Geometry/DOMRectReadOnly.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#DOMRect
class DOMRect final
    : public DOMRectReadOnly {
public:
    using WrapperType = Bindings::DOMRectWrapper;

    static NonnullRefPtr<DOMRect> create_with_global_object(Bindings::WindowObject&, double x = 0, double y = 0, double width = 0, double height = 0)
    {
        return DOMRect::create(x, y, width, height);
    }

    static NonnullRefPtr<DOMRect> create(double x = 0, double y = 0, double width = 0, double height = 0)
    {
        return adopt_ref(*new DOMRect(x, y, width, height));
    }

    static NonnullRefPtr<DOMRect> create(Gfx::FloatRect const& rect)
    {
        return adopt_ref(*new DOMRect(rect.x(), rect.y(), rect.width(), rect.height()));
    }

    double x() const { return m_rect.x(); }
    double y() const { return m_rect.y(); }
    double width() const { return m_rect.width(); }
    double height() const { return m_rect.height(); }

    void set_x(double x) { m_rect.set_x(x); }
    void set_y(double y) { m_rect.set_y(y); }
    void set_width(double width) { m_rect.set_width(width); }
    void set_height(double height) { m_rect.set_height(height); }

private:
    DOMRect(float x, float y, float width, float height)
        : DOMRectReadOnly(x, y, width, height)
    {
    }
};
}
