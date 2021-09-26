/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

// FIXME: Split this into DOMRectReadOnly and DOMRect
// https://drafts.fxtf.org/geometry/#DOMRect
class DOMRect final
    : public RefCounted<DOMRect>
    , public Bindings::Wrappable {
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

    double top() const { return min(y(), y() + height()); }
    double right() const { return max(x(), x() + width()); }
    double bottom() const { return max(y(), y() + height()); }
    double left() const { return min(x(), x() + width()); }

private:
    DOMRect(float x, float y, float width, float height)
        : m_rect(x, y, width, height)
    {
    }

    Gfx::FloatRect m_rect;
};
}
