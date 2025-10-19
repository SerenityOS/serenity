/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibPDF/ColorSpace.h>
#include <LibPDF/Document.h>
#include <LibPDF/Fonts/PDFFont.h>
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
    Vector<float> pattern;
    float phase;
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
    float character_spacing { 0.0f };
    float word_spacing { 0.0f };
    float horizontal_scaling { 1.0f };
    float leading { 0.0f };
    float font_size { 12.0f };
    RefPtr<PDFFont> font;
    TextRenderingMode rendering_mode { TextRenderingMode::Fill };
    float rise { 0.0f };
    bool knockout { true };
};

struct ClippingState {
    Gfx::IntRect clip_bounding_box;
    bool has_own_clip { false };
    RefPtr<Gfx::Bitmap> clip_path_alpha {};
};

struct TransparencyGroupAttributes {
    RefPtr<ColorSpace> color_space;
    bool is_isolated { false };
    bool is_knockout { false };
};

enum class BlendMode {
    // TABLE 7.2 Standard separable blend modes
    Normal,
    Multiply,
    Screen,
    Overlay,
    Darken,
    Lighten,
    ColorDodge,
    ColorBurn,
    HardLight,
    SoftLight,
    Difference,
    Exclusion,

    // TABLE 7.3 Standard nonseparable blend modes
    Hue,
    Saturation,
    Color,
    Luminosity,
};

enum class AlphaSource {
    Opacity,
    Shape,
};

struct GraphicsState {
    Gfx::AffineTransform ctm;
    ClippingState clipping_state;
    RefPtr<ColorSpace> stroke_color_space { DeviceGrayColorSpace::the() };
    RefPtr<ColorSpace> paint_color_space { DeviceGrayColorSpace::the() };
    ColorOrStyle stroke_style { Color::Black };
    ColorOrStyle paint_style { Color::Black };
    ByteString color_rendering_intent { "RelativeColorimetric"sv };
    float flatness_tolerance { 1.0f };
    float line_width { 1.0f };
    LineCapStyle line_cap_style { LineCapStyle::ButtCap };
    LineJoinStyle line_join_style { LineJoinStyle::Miter };
    float miter_limit { 10.0f };
    LineDashPattern line_dash_pattern { {}, 0 };
    TextState text_state {};

    BlendMode blend_mode { BlendMode::Normal };

    struct SMask {
        // TABLE 7.10 Entries in a soft-mask dictionary
        enum class Type {
            Alpha,
            Luminosity,
        };
        Type type { Type::Alpha };
        NonnullRefPtr<StreamObject> group;
        TransparencyGroupAttributes group_attributes;
        Vector<float, 4> background_color { 0.0f, 0.0f, 0.0f, 0.0f };
        RefPtr<Function> transfer_function {}; // nullptr means identity function.
    };
    Optional<SMask> soft_mask {};

    float stroke_alpha_constant { 1.0f };
    float paint_alpha_constant { 1.0f };
    AlphaSource alpha_source { AlphaSource::Opacity };
};

struct RenderingPreferences {
    bool show_clipping_paths { false };
    bool show_images { true };
    bool show_hidden_text { false };

    bool apply_clip { true };

    bool use_constant_alpha { true };

    unsigned hash() const
    {
        return static_cast<unsigned>(show_clipping_paths) | static_cast<unsigned>(show_images) << 1;
    }
};

class Renderer {
    friend class PatternColorSpace;

public:
    static PDFErrorsOr<void> render(Document&, Page const&, RefPtr<Gfx::Bitmap>, Color background_color, RenderingPreferences preferences);

    static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> apply_page_rotation(NonnullRefPtr<Gfx::Bitmap>, Page const&, int extra_degrees = 0);

    struct FontCacheKey {
        NonnullRefPtr<DictObject> font_dictionary;
        float font_size;

        bool operator==(FontCacheKey const&) const = default;
    };

    ALWAYS_INLINE GraphicsState const& state() const { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE TextState const& text_state() const { return state().text_state; }

    Gfx::AffineTransform const& calculate_text_rendering_matrix() const;

    PDFErrorOr<void> render_type3_glyph(Gfx::FloatPoint, StreamObject const&, Gfx::AffineTransform const&, Optional<NonnullRefPtr<DictObject>>);

    bool show_hidden_text() const { return m_rendering_preferences.show_hidden_text; }

private:
    Renderer(RefPtr<Document>, Page const&, RefPtr<Gfx::Bitmap>, Color background_color, RenderingPreferences);

    PDFErrorsOr<void> render();

    PDFErrorOr<void> handle_operator(Operator const&, Optional<NonnullRefPtr<DictObject>> = {});
#define V(name, snake_name, symbol) \
    PDFErrorOr<void> handle_##snake_name(ReadonlySpan<Value> args, Optional<NonnullRefPtr<DictObject>> = {});
    ENUMERATE_OPERATORS(V)
#undef V
    PDFErrorOr<void> handle_text_next_line_show_string(ReadonlySpan<Value> args, Optional<NonnullRefPtr<DictObject>> = {});
    PDFErrorOr<void> handle_text_next_line_show_string_set_spacing(ReadonlySpan<Value> args, Optional<NonnullRefPtr<DictObject>> = {});

    PDFErrorOr<void> prepare_clipped_bitmap_painter();
    void copy_current_clip_path_content_to_output();

    PDFErrorOr<void> add_clip_path(Gfx::Path, Gfx::WindingRule);
    void finalize_clip_before_graphics_state_restore();
    PDFErrorOr<void> restore_previous_clip_after_graphics_state_restore();

    void begin_path_paint();
    PDFErrorOr<void> end_path_paint();
    void stroke_current_path();
    void fill_current_path(Gfx::WindingRule);
    void fill_and_stroke_current_path(Gfx::WindingRule);
    PDFErrorOr<GraphicsState::SMask> read_smask_dict(NonnullRefPtr<DictObject> dict);
    PDFErrorOr<void> set_graphics_state_from_dict(NonnullRefPtr<DictObject>);
    PDFErrorOr<void> show_text(ByteString const&);

    struct LoadedImage {
        NonnullRefPtr<Gfx::Bitmap> bitmap;
        bool is_image_mask = false;
    };
    PDFErrorOr<LoadedImage> load_image(NonnullRefPtr<StreamObject>);
    PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> make_mask_bitmap_from_array(NonnullRefPtr<ArrayObject>, NonnullRefPtr<StreamObject>);
    PDFErrorOr<TransparencyGroupAttributes> read_transparency_group_attributes(NonnullRefPtr<DictObject>);
    PDFErrorOr<void> paint_form_xobject(NonnullRefPtr<StreamObject>);
    PDFErrorOr<void> paint_image_xobject(NonnullRefPtr<StreamObject>);
    void paint_empty_image(Gfx::IntSize);
    PDFErrorOr<NonnullRefPtr<ColorSpace>> get_color_space_from_resources(Value const&, NonnullRefPtr<DictObject>);
    PDFErrorOr<NonnullRefPtr<ColorSpace>> get_color_space_from_document(NonnullRefPtr<Object>);

    static ColorOrStyle style_with_alpha(ColorOrStyle style, float alpha)
    {
        if (style.has<Color>())
            return style.get<Color>().with_alpha(round_to<u8>(clamp(alpha * 255, 0, 255)));
        return style;
    }

    ALWAYS_INLINE GraphicsState& state() { return m_graphics_state_stack.last(); }
    ALWAYS_INLINE TextState& text_state() { return state().text_state; }

    Gfx::Painter& painter();
    Gfx::AntiAliasingPainter& anti_aliasing_painter();

    template<typename T>
    ALWAYS_INLINE Gfx::Point<T> map(T x, T y) const;

    template<typename T>
    ALWAYS_INLINE Gfx::Size<T> map(Gfx::Size<T>) const;

    template<typename T>
    ALWAYS_INLINE Gfx::Rect<T> map(Gfx::Rect<T>) const;

    Gfx::Path map(Gfx::Path const&) const;

    float line_width() const;
    Gfx::Path::CapStyle line_cap_style() const;
    Gfx::Path::JoinStyle line_join_style() const;
    Gfx::Path::StrokeStyle stroke_style() const;

    Gfx::AffineTransform calculate_image_space_transformation(Gfx::IntSize);

    PDFErrorOr<NonnullRefPtr<PDFFont>> get_font(FontCacheKey const&);
    PDFErrorOr<void> set_font(NonnullRefPtr<DictObject> font_dictionary, float font_size);

    PDFErrorOr<void> set_blend_mode(ReadonlySpan<Value>);

    class ScopedState;

    RefPtr<Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;

    RefPtr<Gfx::Bitmap> m_clipped_bitmap;
    Optional<Gfx::Painter> m_clipped_bitmap_painter;
    Optional<Gfx::AntiAliasingPainter> m_clipped_bitmap_anti_aliasing_painter;

    Page const& m_page;
    Gfx::Painter m_painter;
    Gfx::AntiAliasingPainter m_anti_aliasing_painter;
    RenderingPreferences m_rendering_preferences;

    // "In PDF (unlike PostScript), the current path is not part of the graphics state and is not saved and
    //  restored along with the other graphics state parameters."
    Gfx::Path m_current_path;
    enum class AddPathAsClip {
        No,
        Nonzero,
        EvenOdd,
    };
    AddPathAsClip m_add_path_as_clip { AddPathAsClip::No };

    Vector<GraphicsState> m_graphics_state_stack;
    Gfx::AffineTransform m_text_matrix;
    Gfx::AffineTransform m_text_line_matrix;

    bool mutable m_text_rendering_matrix_is_dirty { true };
    Gfx::AffineTransform mutable m_text_rendering_matrix;

    HashMap<FontCacheKey, NonnullRefPtr<PDFFont>> m_font_cache;

    // Only used for m_rendering_preferences.show_clipping_paths.
    void show_clipping_paths();
    Vector<Gfx::Path> m_clip_paths_to_show_for_debugging;
    // Used to offset the PaintStyle's origin when rendering a pattern.
    RefPtr<Gfx::PaintStyle> m_original_paint_style;
};

}

namespace AK {

template<>
struct Traits<PDF::Renderer::FontCacheKey> : public DefaultTraits<PDF::Renderer::FontCacheKey> {
    static unsigned hash(PDF::Renderer::FontCacheKey const& key)
    {
        return pair_int_hash(ptr_hash(key.font_dictionary.ptr()), int_hash(bit_cast<u32>(key.font_size)));
    }
};

template<>
struct Formatter<PDF::LineCapStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::LineCapStyle const& style)
    {
        switch (style) {
        case PDF::LineCapStyle::ButtCap:
            return builder.put_string("LineCapStyle::ButtCap"sv);
        case PDF::LineCapStyle::RoundCap:
            return builder.put_string("LineCapStyle::RoundCap"sv);
        case PDF::LineCapStyle::SquareCap:
            return builder.put_string("LineCapStyle::SquareCap"sv);
        }
    }
};

template<>
struct Formatter<PDF::LineJoinStyle> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::LineJoinStyle const& style)
    {
        switch (style) {
        case PDF::LineJoinStyle::Miter:
            return builder.put_string("LineJoinStyle::Miter"sv);
        case PDF::LineJoinStyle::Round:
            return builder.put_string("LineJoinStyle::Round"sv);
        case PDF::LineJoinStyle::Bevel:
            return builder.put_string("LineJoinStyle::Bevel"sv);
        }
    }
};

template<>
struct Formatter<PDF::LineDashPattern> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::LineDashPattern const& pattern)
    {
        StringBuilder builder;
        builder.append('[');
        bool first = true;

        for (auto& i : pattern.pattern) {
            if (!first)
                builder.append(", "sv);
            first = false;
            builder.appendff("{}", i);
        }

        builder.appendff("] {}", pattern.phase);
        return format_builder.put_string(builder.to_byte_string());
    }
};

template<>
struct Formatter<PDF::TextRenderingMode> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::TextRenderingMode const& style)
    {
        switch (style) {
        case PDF::TextRenderingMode::Fill:
            return builder.put_string("TextRenderingMode::Fill"sv);
        case PDF::TextRenderingMode::Stroke:
            return builder.put_string("TextRenderingMode::Stroke"sv);
        case PDF::TextRenderingMode::FillThenStroke:
            return builder.put_string("TextRenderingMode::FillThenStroke"sv);
        case PDF::TextRenderingMode::Invisible:
            return builder.put_string("TextRenderingMode::Invisible"sv);
        case PDF::TextRenderingMode::FillAndClip:
            return builder.put_string("TextRenderingMode::FillAndClip"sv);
        case PDF::TextRenderingMode::StrokeAndClip:
            return builder.put_string("TextRenderingMode::StrokeAndClip"sv);
        case PDF::TextRenderingMode::FillStrokeAndClip:
            return builder.put_string("TextRenderingMode::FillStrokeAndClip"sv);
        case PDF::TextRenderingMode::Clip:
            return builder.put_string("TextRenderingMode::Clip"sv);
        }
    }
};

template<>
struct Formatter<PDF::TextState> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::TextState const& state)
    {
        StringBuilder builder;
        builder.append("TextState {\n"sv);
        builder.appendff("    character_spacing={}\n", state.character_spacing);
        builder.appendff("    word_spacing={}\n", state.word_spacing);
        builder.appendff("    horizontal_scaling={}\n", state.horizontal_scaling);
        builder.appendff("    leading={}\n", state.leading);
        builder.appendff("    font_size={}\n", state.font_size);
        builder.appendff("    rendering_mode={}\n", state.rendering_mode);
        builder.appendff("    rise={}\n", state.rise);
        builder.appendff("    knockout={}\n", state.knockout);
        builder.append(" }"sv);
        return format_builder.put_string(builder.to_byte_string());
    }
};

template<>
struct Formatter<PDF::GraphicsState> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::GraphicsState const& state)
    {
        StringBuilder builder;
        builder.append("GraphicsState {\n"sv);
        builder.appendff("  ctm={}\n", state.ctm);
        if (state.stroke_style.has<Color>()) {
            builder.appendff("  stroke_style={}\n", state.stroke_style.get<Color>());
        } else {
            builder.appendff("  stroke_style={}\n", state.stroke_style.get<NonnullRefPtr<Gfx::PaintStyle>>());
        }
        if (state.paint_style.has<Color>()) {
            builder.appendff("  paint_style={}\n", state.paint_style.get<Color>());
        } else {
            builder.appendff("  paint_style={}\n", state.paint_style.get<NonnullRefPtr<Gfx::PaintStyle>>());
        }
        builder.appendff("  color_rendering_intent={}\n", state.color_rendering_intent);
        builder.appendff("  flatness_tolerance={}\n", state.flatness_tolerance);
        builder.appendff("  line_width={}\n", state.line_width);
        builder.appendff("  line_cap_style={}\n", state.line_cap_style);
        builder.appendff("  line_join_style={}\n", state.line_join_style);
        builder.appendff("  miter_limit={}\n", state.miter_limit);
        builder.appendff("  line_dash_pattern={}\n", state.line_dash_pattern);
        builder.appendff("  text_state={}\n", state.text_state);
        builder.append('}');
        return format_builder.put_string(builder.to_byte_string());
    }
};

}
