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

// https://drafts.fxtf.org/geometry/#dictdef-domrectinit
struct DOMRectInit {
    double x { 0.0 };
    double y { 0.0 };
    double width { 0.0 };
    double height { 0.0 };
};

// https://drafts.fxtf.org/geometry/#domrectreadonly
class DOMRectReadOnly : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMRectReadOnly, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMRectReadOnly>> construct_impl(JS::Realm&, double x = 0, double y = 0, double width = 0, double height = 0);
    [[nodiscard]] static JS::NonnullGCPtr<DOMRectReadOnly> from_rect(JS::VM&, DOMRectInit const&);

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
    DOMRectReadOnly(JS::Realm&, double x, double y, double width, double height);

    virtual void initialize(JS::Realm&) override;

    Gfx::DoubleRect m_rect;
};
}
