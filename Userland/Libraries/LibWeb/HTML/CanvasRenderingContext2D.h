/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::HTML {

class CanvasRenderingContext2D
    : public RefCounted<CanvasRenderingContext2D>
    , public Bindings::Wrappable {

    AK_MAKE_NONCOPYABLE(CanvasRenderingContext2D);
    AK_MAKE_NONMOVABLE(CanvasRenderingContext2D);

public:
    using WrapperType = Bindings::CanvasRenderingContext2DWrapper;

    static NonnullRefPtr<CanvasRenderingContext2D> create(HTMLCanvasElement& element) { return adopt_ref(*new CanvasRenderingContext2D(element)); }
    ~CanvasRenderingContext2D();

    void set_fill_style(String);
    String fill_style() const;

    void set_stroke_style(String);
    String stroke_style() const;

    void fill_rect(float x, float y, float width, float height);
    void stroke_rect(float x, float y, float width, float height);
    void clear_rect(float x, float y, float width, float height);

    void draw_image(HTMLImageElement const&, float x, float y);

    void scale(float sx, float sy);
    void translate(float x, float y);
    void rotate(float degrees);

    void set_line_width(float line_width) { m_line_width = line_width; }
    float line_width() const { return m_line_width; }

    void begin_path();
    void close_path();
    void move_to(float x, float y);
    void line_to(float x, float y);
    void quadratic_curve_to(float cx, float cy, float x, float y);

    DOM::ExceptionOr<void> arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise);
    DOM::ExceptionOr<void> ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise);
    void rect(float x, float y, float width, float height);
    void stroke();

    void fill_text(String const&, float x, float y, Optional<double> max_width);

    // FIXME: We should only have one fill(), really. Fix the wrapper generator!
    void fill(Gfx::Painter::WindingRule);
    void fill(String const& fill_rule);

    RefPtr<ImageData> create_image_data(int width, int height) const;
    void put_image_data(ImageData const&, float x, float y);

    HTMLCanvasElement* canvas() { return m_element; }

private:
    explicit CanvasRenderingContext2D(HTMLCanvasElement&);

    void did_draw(const Gfx::FloatRect&);

    OwnPtr<Gfx::Painter> painter();

    WeakPtr<HTMLCanvasElement> m_element;

    Gfx::AffineTransform m_transform;
    Gfx::Color m_fill_style;
    Gfx::Color m_stroke_style;
    float m_line_width { 1 };

    Gfx::Path m_path;
};

}
