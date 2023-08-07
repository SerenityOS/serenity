/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Geometry/DOMRectReadOnly.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#DOMRect
class DOMRect final : public DOMRectReadOnly {
    WEB_PLATFORM_OBJECT(DOMRect, DOMRectReadOnly);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> construct_impl(JS::Realm&, double x = 0, double y = 0, double width = 0, double height = 0);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> create(JS::Realm&, Gfx::FloatRect const&);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRect>> from_rect(JS::VM&, DOMRectInit const&);

    virtual ~DOMRect() override;

    double x() const { return m_rect.x(); }
    double y() const { return m_rect.y(); }
    double width() const { return m_rect.width(); }
    double height() const { return m_rect.height(); }

    void set_x(double x) { m_rect.set_x(x); }
    void set_y(double y) { m_rect.set_y(y); }
    void set_width(double width) { m_rect.set_width(width); }
    void set_height(double height) { m_rect.set_height(height); }

private:
    DOMRect(JS::Realm&, double x, double y, double width, double height);

    virtual void initialize(JS::Realm&) override;
};

}
