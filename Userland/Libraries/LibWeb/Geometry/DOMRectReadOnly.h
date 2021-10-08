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

// https://drafts.fxtf.org/geometry/#domrectreadonly
class DOMRectReadOnly
    : public RefCounted<DOMRectReadOnly>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMRectReadOnlyWrapper;

    static NonnullRefPtr<DOMRectReadOnly> create_with_global_object(Bindings::WindowObject&, double x = 0, double y = 0, double width = 0, double height = 0)
    {
        return DOMRectReadOnly::create(x, y, width, height);
    }

    static NonnullRefPtr<DOMRectReadOnly> create(double x = 0, double y = 0, double width = 0, double height = 0)
    {
        return adopt_ref(*new DOMRectReadOnly(x, y, width, height));
    }

    double x() const { return m_rect.x(); }
    double y() const { return m_rect.y(); }
    double width() const { return m_rect.width(); }
    double height() const { return m_rect.height(); }

    double top() const { return min(y(), y() + height()); }
    double right() const { return max(x(), x() + width()); }
    double bottom() const { return max(y(), y() + height()); }
    double left() const { return min(x(), x() + width()); }

protected:
    DOMRectReadOnly(float x, float y, float width, float height)
        : m_rect(x, y, width, height)
    {
    }

    Gfx::FloatRect m_rect;
};
}
