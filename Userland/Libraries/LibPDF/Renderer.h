/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/AffineTransform.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibPDF/Document.h>
#include <LibPDF/Object.h>

namespace PDF {

enum class LineCapStyle : u8 {
    ButtCap = 0,
    RoundCap = 1,
    SquareCap = 2,
};

enum class LineJoinStyle : u8 {
    Miter = 0,
    Round = 1,
    Bevel = 2,
};

struct LineDashPattern {
    Vector<int> pattern;
    int phase;
};

enum class TextRenderingMode : u8 {
    Fill = 0,
    Stroke = 1,
    FillThenStroke = 2,
    Invisible = 3,
    FillAndClip = 4,
    StrokeAndClip = 5,
    FillStrokeAndClip = 6,
    Clip = 7,
};

struct TextState {
    float character_spacing { 3.0f };
    float word_spacing { 5.0f };
    float horizontal_scaling { 1.0f };
    float leading { 0.0f };
    RefPtr<Gfx::Font> font;
    TextRenderingMode rendering_mode { TextRenderingMode::Fill };
    float rise { 0.0f };
    bool knockout { true };
};

struct GraphicsState {
    Gfx::AffineTransform ctm;
    Gfx::Color stroke_color { Gfx::Color::NamedColor::Black };
    Gfx::Color paint_color { Gfx::Color::NamedColor::Black };
    float line_width { 1.0f };
    LineCapStyle line_cap_style { LineCapStyle::ButtCap };
    LineJoinStyle line_join_style { LineJoinStyle::Miter };
    float miter_limit { 10.0f };
    LineDashPattern line_dash_pattern { {}, 0 };
    TextState text_state {};
};

class Renderer {
public:
    static void render(Document&, const Page&, RefPtr<Gfx::Bitmap>);

private:
    Renderer(RefPtr<Document>, const Page&, RefPtr<Gfx::Bitmap>);

    void render();

    void handle_command(const GraphicsCommand&);
#define V(name, snake_name, symbol) \
    void handle_##snake_name(const Vector<Value>& args);
    ENUMERATE_GRAPHICS_COMMANDS(V)
#undef V
    void handle_text_next_line_show_string(const Vector<Value>& args);

    // shift is the manual advance given in the TJ command array
    void show_text(const String&, int shift = 0);

    ALWAYS_INLINE const GraphicsState& state() const { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE GraphicsState& state() { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE const TextState& text_state() const { return state().text_state; }
    ALWAYS_INLINE TextState& text_state() { return state().text_state; }

    template<typename T>
    ALWAYS_INLINE Gfx::Point<T> map(T x, T y) const;

    template<typename T>
    ALWAYS_INLINE Gfx::Size<T> map(Gfx::Size<T>) const;

    const Gfx::AffineTransform& calculate_text_rendering_matrix();

    RefPtr<Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;
    const Page& m_page;
    Gfx::Painter m_painter;

    Gfx::Path m_path;
    Vector<GraphicsState> m_graphics_state_stack;
    Gfx::AffineTransform m_text_matrix;
    Gfx::AffineTransform m_text_line_matrix;
    Gfx::AffineTransform m_userspace_matrix;

    bool m_text_rendering_matrix_is_dirty { true };
    Gfx::AffineTransform m_text_rendering_matrix;
};

}
