/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Path.h>
#include <LibWeb/HTML/Canvas/CanvasState.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvaspath
class CanvasPath {
public:
    ~CanvasPath() = default;

    void close_path();

    void move_to(float x, float y);
    void line_to(float x, float y);
    void quadratic_curve_to(float cx, float cy, float x, float y);
    void bezier_curve_to(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);
    void rect(float x, float y, float width, float height);
    WebIDL::ExceptionOr<void> arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise);
    WebIDL::ExceptionOr<void> ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise);

    Gfx::Path& path() { return m_path; }
    Gfx::Path const& path() const { return m_path; }

protected:
    explicit CanvasPath(Bindings::PlatformObject& self)
        : m_self(self)
    {
    }

    explicit CanvasPath(Bindings::PlatformObject& self, CanvasState const& canvas_state)
        : m_self(self)
        , m_canvas_state(canvas_state)
    {
    }

private:
    Gfx::AffineTransform active_transform() const;

    JS::NonnullGCPtr<Bindings::PlatformObject> m_self;
    Optional<CanvasState const&> m_canvas_state;
    Gfx::Path m_path;
};

}
