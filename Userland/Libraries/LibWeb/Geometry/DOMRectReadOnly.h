/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#domrectreadonly
class DOMRectReadOnly : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMRectReadOnly, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<DOMRectReadOnly> create_with_global_object(HTML::Window&, double x = 0, double y = 0, double width = 0, double height = 0);

    virtual ~DOMRectReadOnly() override;

    double x() const { return m_rect.x(); }
    double y() const { return m_rect.y(); }
    double width() const { return m_rect.width(); }
    double height() const { return m_rect.height(); }

    double top() const { return min(y(), y() + height()); }
    double right() const { return max(x(), x() + width()); }
    double bottom() const { return max(y(), y() + height()); }
    double left() const { return min(x(), x() + width()); }

protected:
    DOMRectReadOnly(HTML::Window&, double x, double y, double width, double height);

    Gfx::FloatRect m_rect;
};
}

WRAPPER_HACK(DOMRectReadOnly, Web::Geometry)
