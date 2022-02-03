/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/HTML/CanvasGradient.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/LineBox.h>

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

    void draw_image(const HTMLImageElement&, float x, float y);

    void scale(float sx, float sy);
    void translate(float x, float y);
    void rotate(float degrees);

    void set_line_width(float line_width) { m_drawing_state.line_width = line_width; }
    float line_width() const { return m_drawing_state.line_width; }

    void begin_path();
    void close_path();
    void move_to(float x, float y);
    void line_to(float x, float y);
    void quadratic_curve_to(float cx, float cy, float x, float y);
    void bezier_curve_to(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);

    DOM::ExceptionOr<void> arc(float x, float y, float radius, float start_angle, float end_angle, bool counter_clockwise);
    DOM::ExceptionOr<void> ellipse(float x, float y, float radius_x, float radius_y, float rotation, float start_angle, float end_angle, bool counter_clockwise);
    void rect(float x, float y, float width, float height);
    void stroke();

    void fill_text(const String&, float x, float y, Optional<double> max_width);
    void stroke_text(String const&, float x, float y, Optional<double> max_width);

    // FIXME: We should only have one fill(), really. Fix the wrapper generator!
    void fill(Gfx::Painter::WindingRule);
    void fill(const String& fill_rule);

    RefPtr<ImageData> create_image_data(int width, int height) const;
    void put_image_data(const ImageData&, float x, float y);

    void save();
    void restore();
    void reset();
    bool is_context_lost();

    void reset_to_default_state();

    HTMLCanvasElement* canvas() { return m_element; }

    RefPtr<TextMetrics> measure_text(String const& text);

    NonnullRefPtr<CanvasGradient> create_radial_gradient(double x0, double y0, double r0, double x1, double y1, double r1);
    NonnullRefPtr<CanvasGradient> create_linear_gradient(double x0, double y0, double x1, double y1);
    NonnullRefPtr<CanvasGradient> create_conic_gradient(double start_angle, double x, double y);

private:
    explicit CanvasRenderingContext2D(HTMLCanvasElement&);

    struct PreparedTextGlyph {
        unsigned int c;
        Gfx::IntPoint position;
    };

    struct PreparedText {
        Vector<PreparedTextGlyph> glyphs;
        Gfx::TextAlignment physical_alignment;
        Gfx::IntRect bounding_box;
    };

    void did_draw(const Gfx::FloatRect&);
    PreparedText prepare_text(String const& text, float max_width = INFINITY);

    OwnPtr<Gfx::Painter> painter();

    WeakPtr<HTMLCanvasElement> m_element;

    // https://html.spec.whatwg.org/multipage/canvas.html#drawing-state
    struct DrawingState {
        Gfx::AffineTransform transform;
        Gfx::Color fill_style { Gfx::Color::Black };
        Gfx::Color stroke_style { Gfx::Color::Black };
        float line_width { 1 };
    };

    DrawingState m_drawing_state;
    Vector<DrawingState> m_drawing_state_stack;

    // https://html.spec.whatwg.org/multipage/canvas.html#concept-canvas-context-lost
    bool m_context_lost { false };

    Gfx::Path m_path;
};

}
