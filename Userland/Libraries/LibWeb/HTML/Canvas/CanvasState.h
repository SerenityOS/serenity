/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Color.h>
#include <LibGfx/PaintStyle.h>
#include <LibGfx/PathClipper.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DPrototype.h>
#include <LibWeb/HTML/CanvasGradient.h>
#include <LibWeb/HTML/CanvasPattern.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasstate
class CanvasState {
public:
    virtual ~CanvasState() = default;

    void save();
    void restore();
    void reset();
    bool is_context_lost();

    using FillOrStrokeVariant = Variant<Gfx::Color, JS::Handle<CanvasGradient>, JS::Handle<CanvasPattern>>;

    struct FillOrStrokeStyle {
        FillOrStrokeStyle(Gfx::Color color)
            : m_fill_or_stroke_style(color)
        {
        }

        FillOrStrokeStyle(JS::Handle<CanvasGradient> gradient)
            : m_fill_or_stroke_style(gradient)
        {
        }

        FillOrStrokeStyle(JS::Handle<CanvasPattern> pattern)
            : m_fill_or_stroke_style(pattern)
        {
        }

        NonnullRefPtr<Gfx::PaintStyle> to_gfx_paint_style();

        Optional<Gfx::Color> as_color() const;
        Gfx::Color to_color_but_fixme_should_accept_any_paint_style() const;

        using JsFillOrStrokeStyle = Variant<String, JS::Handle<CanvasGradient>, JS::Handle<CanvasPattern>>;

        JsFillOrStrokeStyle to_js_fill_or_stroke_style() const
        {
            return m_fill_or_stroke_style.visit(
                [&](Gfx::Color color) -> JsFillOrStrokeStyle {
                    return color.to_string();
                },
                [&](auto handle) -> JsFillOrStrokeStyle {
                    return handle;
                });
        }

    private:
        FillOrStrokeVariant m_fill_or_stroke_style;
        RefPtr<Gfx::PaintStyle> m_color_paint_style { nullptr };
    };

    // https://html.spec.whatwg.org/multipage/canvas.html#drawing-state
    struct DrawingState {
        Gfx::AffineTransform transform;
        FillOrStrokeStyle fill_style { Gfx::Color::Black };
        FillOrStrokeStyle stroke_style { Gfx::Color::Black };
        float line_width { 1 };
        bool image_smoothing_enabled { true };
        Bindings::ImageSmoothingQuality image_smoothing_quality { Bindings::ImageSmoothingQuality::Low };
        float global_alpha = { 1 };
        Optional<Gfx::ClipPath> clip;
        RefPtr<CSS::StyleValue> font_style_value { nullptr };
        RefPtr<Gfx::Font const> current_font { nullptr };
        Bindings::CanvasTextAlign text_align { Bindings::CanvasTextAlign::Start };
        Bindings::CanvasTextBaseline text_baseline { Bindings::CanvasTextBaseline::Alphabetic };
    };
    DrawingState& drawing_state() { return m_drawing_state; }
    DrawingState const& drawing_state() const { return m_drawing_state; }

    void clear_drawing_state_stack() { m_drawing_state_stack.clear(); }
    void reset_drawing_state() { m_drawing_state = {}; }

    virtual void reset_to_default_state() = 0;

protected:
    CanvasState() = default;

private:
    DrawingState m_drawing_state;
    Vector<DrawingState> m_drawing_state_stack;

    // https://html.spec.whatwg.org/multipage/canvas.html#concept-canvas-context-lost
    bool m_context_lost { false };
};

}
