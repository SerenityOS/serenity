/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibPDF/ColorSpace.h>
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
    FlyString font_family { "Liberation Serif" };
    String font_variant { "Regular" };
    float font_size { 12.0f };
    TextRenderingMode rendering_mode { TextRenderingMode::Fill };
    float rise { 0.0f };
    bool knockout { true };
};

struct GraphicsState {
    Gfx::AffineTransform ctm;
    RefPtr<ColorSpace> stroke_color_space { DeviceGrayColorSpace::the() };
    RefPtr<ColorSpace> paint_color_space { DeviceGrayColorSpace::the() };
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
    static void render(Document&, Page const&, RefPtr<Gfx::Bitmap>);

private:
    Renderer(RefPtr<Document>, Page const&, RefPtr<Gfx::Bitmap>);

    void render();

    void handle_command(Command const&);
#define V(name, snake_name, symbol) \
    void handle_##snake_name(Vector<Value> const& args);
    ENUMERATE_COMMANDS(V)
#undef V
    void handle_text_next_line_show_string(Vector<Value> const& args);
    void handle_text_next_line_show_string_set_spacing(Vector<Value> const& args);

    void set_graphics_state_from_dict(NonnullRefPtr<DictObject>);
    // shift is the manual advance given in the TJ command array
    void show_text(String const&, float shift = 0.0f);
    RefPtr<ColorSpace> get_color_space(Value const&);

    ALWAYS_INLINE GraphicsState const& state() const { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE GraphicsState& state() { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE TextState const& text_state() const { return state().text_state; }
    ALWAYS_INLINE TextState& text_state() { return state().text_state; }

    template<typename T>
    ALWAYS_INLINE Gfx::Point<T> map(T x, T y) const;

    template<typename T>
    ALWAYS_INLINE Gfx::Size<T> map(Gfx::Size<T>) const;

    template<typename T>
    ALWAYS_INLINE Gfx::Rect<T> map(Gfx::Rect<T>) const;

    Gfx::AffineTransform const& calculate_text_rendering_matrix();

    RefPtr<Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;
    Page const& m_page;
    Gfx::Painter m_painter;

    Gfx::Path m_current_path;
    Vector<GraphicsState> m_graphics_state_stack;
    Gfx::AffineTransform m_text_matrix;
    Gfx::AffineTransform m_text_line_matrix;

    bool m_text_rendering_matrix_is_dirty { true };
    Gfx::AffineTransform m_text_rendering_matrix;
};

}

namespace AK {

template<>
struct Formatter<PDF::LineCapStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::LineCapStyle const& style)
    {
        switch (style) {
        case PDF::LineCapStyle::ButtCap:
            return Formatter<StringView>::format(builder, "LineCapStyle::ButtCap");
        case PDF::LineCapStyle::RoundCap:
            return Formatter<StringView>::format(builder, "LineCapStyle::RoundCap");
        case PDF::LineCapStyle::SquareCap:
            return Formatter<StringView>::format(builder, "LineCapStyle::SquareCap");
        }
    }
};

template<>
struct Formatter<PDF::LineJoinStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::LineJoinStyle const& style)
    {
        switch (style) {
        case PDF::LineJoinStyle::Miter:
            return Formatter<StringView>::format(builder, "LineJoinStyle::Miter");
        case PDF::LineJoinStyle::Round:
            return Formatter<StringView>::format(builder, "LineJoinStyle::Round");
        case PDF::LineJoinStyle::Bevel:
            return Formatter<StringView>::format(builder, "LineJoinStyle::Bevel");
        }
    }
};

template<>
struct Formatter<PDF::LineDashPattern> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::LineDashPattern const& pattern)
    {
        StringBuilder builder;
        builder.append("[");
        bool first = true;

        for (auto& i : pattern.pattern) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.appendff("{}", i);
        }

        builder.appendff("] {}", pattern.phase);
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

template<>
struct Formatter<PDF::TextRenderingMode> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::TextRenderingMode const& style)
    {
        switch (style) {
        case PDF::TextRenderingMode::Fill:
            return Formatter<StringView>::format(builder, "TextRenderingMode::Fill");
        case PDF::TextRenderingMode::Stroke:
            return Formatter<StringView>::format(builder, "TextRenderingMode::Stroke");
        case PDF::TextRenderingMode::FillThenStroke:
            return Formatter<StringView>::format(builder, "TextRenderingMode::FillThenStroke");
        case PDF::TextRenderingMode::Invisible:
            return Formatter<StringView>::format(builder, "TextRenderingMode::Invisible");
        case PDF::TextRenderingMode::FillAndClip:
            return Formatter<StringView>::format(builder, "TextRenderingMode::FillAndClip");
        case PDF::TextRenderingMode::StrokeAndClip:
            return Formatter<StringView>::format(builder, "TextRenderingMode::StrokeAndClip");
        case PDF::TextRenderingMode::FillStrokeAndClip:
            return Formatter<StringView>::format(builder, "TextRenderingMode::FillStrokeAndClip");
        case PDF::TextRenderingMode::Clip:
            return Formatter<StringView>::format(builder, "TextRenderingMode::Clip");
        }
    }
};

template<>
struct Formatter<PDF::TextState> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::TextState const& state)
    {
        StringBuilder builder;
        builder.append("TextState {\n");
        builder.appendff("    character_spacing={}\n", state.character_spacing);
        builder.appendff("    word_spacing={}\n", state.word_spacing);
        builder.appendff("    horizontal_scaling={}\n", state.horizontal_scaling);
        builder.appendff("    leading={}\n", state.leading);
        builder.appendff("    font_family={}\n", state.font_family);
        builder.appendff("    font_variant={}\n", state.font_variant);
        builder.appendff("    font_size={}\n", state.font_size);
        builder.appendff("    rendering_mode={}\n", state.rendering_mode);
        builder.appendff("    rise={}\n", state.rise);
        builder.appendff("    knockout={}\n", state.knockout);
        builder.append(" }");
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

template<>
struct Formatter<PDF::GraphicsState> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::GraphicsState const& state)
    {
        StringBuilder builder;
        builder.append("GraphicsState {\n");
        builder.appendff("  ctm={}\n", state.ctm);
        builder.appendff("  stroke_color={}\n", state.stroke_color);
        builder.appendff("  paint_color={}\n", state.paint_color);
        builder.appendff("  line_width={}\n", state.line_width);
        builder.appendff("  line_cap_style={}\n", state.line_cap_style);
        builder.appendff("  line_join_style={}\n", state.line_join_style);
        builder.appendff("  miter_limit={}\n", state.miter_limit);
        builder.appendff("  line_dash_pattern={}\n", state.line_dash_pattern);
        builder.appendff("  text_state={}\n", state.text_state);
        builder.append("}");
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

}
