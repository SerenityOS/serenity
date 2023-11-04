/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/GrayscaleBitmap.h>
#include <LibGfx/PaintStyle.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/TextAlignment.h>
#include <LibGfx/TextDirection.h>
#include <LibGfx/TextElision.h>
#include <LibGfx/TextLayout.h>
#include <LibGfx/TextWrapping.h>
#include <LibWeb/Painting/BorderRadiiData.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/GradientData.h>
#include <LibWeb/Painting/PaintOuterBoxShadowParams.h>

namespace Web::Painting {

enum class CommandResult {
    Continue,
    SkipStackingContext,
};

struct DrawGlyphRun {
    Vector<Gfx::DrawGlyphOrEmoji> glyph_run;
    Color color;
    Gfx::IntRect rect;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct DrawText {
    Gfx::IntRect rect;
    String raw_text;
    Gfx::TextAlignment alignment;
    Color color;
    Gfx::TextElision elision;
    Gfx::TextWrapping wrapping;
    Optional<NonnullRefPtr<Gfx::Font>> font {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct FillRect {
    Gfx::IntRect rect;
    Color color;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct DrawScaledBitmap {
    Gfx::IntRect dst_rect;
    NonnullRefPtr<Gfx::Bitmap> bitmap;
    Gfx::IntRect src_rect;
    float opacity;
    Gfx::Painter::ScalingMode scaling_mode;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return dst_rect; }
};

struct SetClipRect {
    Gfx::IntRect rect;
};

struct ClearClipRect { };

struct SetFont {
    NonnullRefPtr<Gfx::Font> font;
};

struct PushStackingContext {
    bool semitransparent_or_has_non_identity_transform;
    bool has_fixed_position;
    float opacity;
    Gfx::FloatRect source_rect;
    Gfx::FloatRect transformed_destination_rect;
    Gfx::IntPoint painter_location;
};

struct PopStackingContext {
    bool semitransparent_or_has_non_identity_transform;
    Gfx::Painter::ScalingMode scaling_mode;
};

struct PushStackingContextWithMask {
    Gfx::IntRect paint_rect;
};

struct PopStackingContextWithMask {
    Gfx::IntRect paint_rect;
    RefPtr<Gfx::Bitmap> mask_bitmap;
    Gfx::Bitmap::MaskKind mask_kind;
    float opacity;
};

struct PaintLinearGradient {
    Gfx::IntRect gradient_rect;
    LinearGradientData linear_gradient_data;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return gradient_rect; }
};

struct PaintOuterBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;
};

struct PaintInnerBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;
};

struct PaintTextShadow {
    int blur_radius;
    Gfx::IntRect shadow_bounding_rect;
    Gfx::IntRect text_rect;
    String text;
    NonnullRefPtr<Gfx::Font> font;
    Color color;
    int fragment_baseline;
    Gfx::IntPoint draw_location;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return { draw_location, shadow_bounding_rect.size() }; }
};

struct FillRectWithRoundedCorners {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::CornerRadius top_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius top_right_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct FillPathUsingColor {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    Color color;
    Gfx::Painter::WindingRule winding_rule;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }
};

struct FillPathUsingPaintStyle {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    Gfx::Painter::WindingRule winding_rule;
    float opacity;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }
};

struct StrokePathUsingColor {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    Color color;
    float thickness;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }
};

struct StrokePathUsingPaintStyle {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    float thickness;
    float opacity = 1.0f;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }
};

struct DrawEllipse {
    Gfx::IntRect rect;
    Color color;
    int thickness;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct FillElipse {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::BlendMode blend_mode;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct DrawLine {
    Color color;
    Gfx::IntPoint from;
    Gfx::IntPoint to;
    int thickness;
    Gfx::Painter::LineStyle style;
    Color alternate_color;
};

struct DrawSignedDistanceField {
    Gfx::IntRect rect;
    Color color;
    Gfx::GrayscaleBitmap sdf;
    float smoothing;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct PaintProgressbar {
    Gfx::IntRect frame_rect;
    Gfx::IntRect progress_rect;
    Palette palette;
    int min;
    int max;
    int value;
    StringView text;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return frame_rect; }
};

struct PaintFrame {
    Gfx::IntRect rect;
    Palette palette;
    Gfx::FrameStyle style;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct ApplyBackdropFilter {
    Gfx::IntRect backdrop_region;
    BorderRadiiData border_radii_data;
    CSS::ResolvedBackdropFilter backdrop_filter;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return backdrop_region; }
};

struct DrawRect {
    Gfx::IntRect rect;
    Color color;
    bool rough;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct PaintRadialGradient {
    Gfx::IntRect rect;
    RadialGradientData radial_gradient_data;
    Gfx::IntPoint center;
    Gfx::IntSize size;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct PaintConicGradient {
    Gfx::IntRect rect;
    ConicGradientData conic_gradient_data;
    Gfx::IntPoint position;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
};

struct DrawTriangleWave {
    Gfx::IntPoint p1;
    Gfx::IntPoint p2;
    Color color;
    int amplitude;
    int thickness;
};

struct SampleUnderCorners {
    NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;
};

struct BlitCornerClipping {
    NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;
};

using PaintingCommand = Variant<
    DrawGlyphRun,
    DrawText,
    FillRect,
    DrawScaledBitmap,
    SetClipRect,
    ClearClipRect,
    SetFont,
    PushStackingContext,
    PopStackingContext,
    PushStackingContextWithMask,
    PopStackingContextWithMask,
    PaintLinearGradient,
    PaintRadialGradient,
    PaintConicGradient,
    PaintOuterBoxShadow,
    PaintInnerBoxShadow,
    PaintTextShadow,
    FillRectWithRoundedCorners,
    FillPathUsingColor,
    FillPathUsingPaintStyle,
    StrokePathUsingColor,
    StrokePathUsingPaintStyle,
    DrawEllipse,
    FillElipse,
    DrawLine,
    DrawSignedDistanceField,
    PaintProgressbar,
    PaintFrame,
    ApplyBackdropFilter,
    DrawRect,
    DrawTriangleWave,
    SampleUnderCorners,
    BlitCornerClipping>;

class PaintingCommandExecutor {
public:
    virtual ~PaintingCommandExecutor() = default;

    virtual CommandResult draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const&) = 0;
    virtual CommandResult draw_text(Gfx::IntRect const&, String const&, Gfx::TextAlignment alignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&) = 0;
    virtual CommandResult fill_rect(Gfx::IntRect const&, Color const&) = 0;
    virtual CommandResult draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, float opacity, Gfx::Painter::ScalingMode scaling_mode) = 0;
    virtual CommandResult set_clip_rect(Gfx::IntRect const& rect) = 0;
    virtual CommandResult clear_clip_rect() = 0;
    virtual CommandResult set_font(Gfx::Font const& font) = 0;
    virtual CommandResult push_stacking_context(bool semitransparent_or_has_non_identity_transform, float opacity, Gfx::FloatRect const& source_rect, Gfx::FloatRect const& transformed_destination_rect, Gfx::IntPoint const& painter_location) = 0;
    virtual CommandResult pop_stacking_context(bool semitransparent_or_has_non_identity_transform, Gfx::Painter::ScalingMode scaling_mode) = 0;
    virtual CommandResult push_stacking_context_with_mask(Gfx::IntRect const& paint_rect) = 0;
    virtual CommandResult pop_stacking_context_with_mask(Gfx::IntRect const& paint_rect, RefPtr<Gfx::Bitmap> const& mask_bitmap, Gfx::Bitmap::MaskKind mask_kind, float opacity) = 0;
    virtual CommandResult paint_linear_gradient(Gfx::IntRect const&, LinearGradientData const&) = 0;
    virtual CommandResult paint_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const&, Gfx::IntPoint const& center, Gfx::IntSize const& size) = 0;
    virtual CommandResult paint_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const&, Gfx::IntPoint const& position) = 0;
    virtual CommandResult paint_outer_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_inner_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, String const&, Gfx::Font const&, Color const&, int fragment_baseline, Gfx::IntPoint const& draw_location) = 0;
    virtual CommandResult fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius, Optional<Gfx::FloatPoint> const& aa_translation) = 0;
    virtual CommandResult fill_path_using_color(Gfx::Path const&, Color const& color, Gfx::Painter::WindingRule, Optional<Gfx::FloatPoint> const& aa_translation) = 0;
    virtual CommandResult fill_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const& paint_style, Gfx::Painter::WindingRule winding_rule, float opacity, Optional<Gfx::FloatPoint> const& aa_translation) = 0;
    virtual CommandResult stroke_path_using_color(Gfx::Path const&, Color const& color, float thickness, Optional<Gfx::FloatPoint> const& aa_translation) = 0;
    virtual CommandResult stroke_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const& paint_style, float thickness, float opacity, Optional<Gfx::FloatPoint> const& aa_translation) = 0;
    virtual CommandResult draw_ellipse(Gfx::IntRect const&, Color const&, int thickness) = 0;
    virtual CommandResult fill_ellipse(Gfx::IntRect const&, Color const&, Gfx::AntiAliasingPainter::BlendMode blend_mode) = 0;
    virtual CommandResult draw_line(Color const& color, Gfx::IntPoint const& from, Gfx::IntPoint const& to, int thickness, Gfx::Painter::LineStyle, Color const& alternate_color) = 0;
    virtual CommandResult draw_signed_distance_field(Gfx::IntRect const& rect, Color const&, Gfx::GrayscaleBitmap const&, float smoothing) = 0;
    virtual CommandResult paint_progressbar(Gfx::IntRect const& frame_rect, Gfx::IntRect const& progress_rect, Palette const& palette, int min, int max, int value, StringView const& text) = 0;
    virtual CommandResult paint_frame(Gfx::IntRect const& rect, Palette const&, Gfx::FrameStyle) = 0;
    virtual CommandResult apply_backdrop_filter(Gfx::IntRect const& backdrop_region, Web::CSS::ResolvedBackdropFilter const& backdrop_filter) = 0;
    virtual CommandResult draw_rect(Gfx::IntRect const& rect, Color const&, bool rough) = 0;
    virtual CommandResult draw_triangle_wave(Gfx::IntPoint const& p1, Gfx::IntPoint const& p2, Color const& color, int amplitude, int thickness) = 0;
    virtual CommandResult sample_under_corners(BorderRadiusCornerClipper&) = 0;
    virtual CommandResult blit_corner_clipping(BorderRadiusCornerClipper&) = 0;

    virtual bool would_be_fully_clipped_by_painter(Gfx::IntRect) const = 0;

    virtual bool needs_prepare_glyphs_texture() const { return false; }
    virtual void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs) = 0;
};

class RecordingPainter {
public:
    void fill_rect(Gfx::IntRect const& rect, Color color);

    struct FillPathUsingColorParams {
        Gfx::Path path;
        Gfx::Color color;
        Gfx::Painter::WindingRule winding_rule = Gfx::Painter::WindingRule::EvenOdd;
        Optional<Gfx::FloatPoint> translation = {};
    };
    void fill_path(FillPathUsingColorParams params);

    struct FillPathUsingPaintStyleParams {
        Gfx::Path path;
        NonnullRefPtr<Gfx::PaintStyle> paint_style;
        Gfx::Painter::WindingRule winding_rule = Gfx::Painter::WindingRule::EvenOdd;
        float opacity;
        Optional<Gfx::FloatPoint> translation = {};
    };
    void fill_path(FillPathUsingPaintStyleParams params);

    struct StrokePathUsingColorParams {
        Gfx::Path path;
        Gfx::Color color;
        float thickness;
        Optional<Gfx::FloatPoint> translation = {};
    };
    void stroke_path(StrokePathUsingColorParams params);

    struct StrokePathUsingPaintStyleParams {
        Gfx::Path path;
        NonnullRefPtr<Gfx::PaintStyle> paint_style;
        float thickness;
        float opacity;
        Optional<Gfx::FloatPoint> translation = {};
    };
    void stroke_path(StrokePathUsingPaintStyleParams params);

    void draw_ellipse(Gfx::IntRect const& a_rect, Color color, int thickness);

    void fill_ellipse(Gfx::IntRect const& a_rect, Color color, Gfx::AntiAliasingPainter::BlendMode blend_mode = Gfx::AntiAliasingPainter::BlendMode::Normal);

    void fill_rect_with_linear_gradient(Gfx::IntRect const& gradient_rect, LinearGradientData const& data);
    void fill_rect_with_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const& data, Gfx::IntPoint const& position);
    void fill_rect_with_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const& data, Gfx::IntPoint center, Gfx::IntSize size);

    void draw_rect(Gfx::IntRect const& rect, Color color, bool rough = false);

    void draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, float opacity = 1.0f, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor);

    void draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness = 1, Gfx::Painter::LineStyle style = Gfx::Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);

    void draw_text(Gfx::IntRect const&, StringView, Gfx::Font const&, Gfx::TextAlignment = Gfx::TextAlignment::TopLeft, Color = Color::Black, Gfx::TextElision = Gfx::TextElision::None, Gfx::TextWrapping = Gfx::TextWrapping::DontWrap);
    void draw_text(Gfx::IntRect const& rect, StringView raw_text, Gfx::TextAlignment alignment = Gfx::TextAlignment::TopLeft, Color color = Color::Black, Gfx::TextElision elision = Gfx::TextElision::None, Gfx::TextWrapping wrapping = Gfx::TextWrapping::DontWrap);

    void draw_signed_distance_field(Gfx::IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing);

    // Streamlined text drawing routine that does no wrapping/elision/alignment.
    void draw_text_run(Gfx::IntPoint baseline_start, Utf8View string, Gfx::Font const& font, Color color, Gfx::IntRect const& rect);

    void add_clip_rect(Gfx::IntRect const& rect);

    void translate(int dx, int dy);
    void translate(Gfx::IntPoint delta);

    void set_font(Gfx::Font const& font);

    void save();
    void restore();

    struct PushStackingContextParams {
        bool semitransparent_or_has_non_identity_transform;
        bool has_fixed_position;
        float opacity;
        Gfx::FloatRect source_rect;
        Gfx::FloatRect transformed_destination_rect;
        Gfx::IntPoint painter_location;
    };
    void push_stacking_context(PushStackingContextParams params);

    struct PopStackingContextParams {
        bool semitransparent_or_has_non_identity_transform;
        Gfx::Painter::ScalingMode scaling_mode;
    };
    void pop_stacking_context(PopStackingContextParams params);

    void push_stacking_context_with_mask(Gfx::IntRect paint_rect);
    void pop_stacking_context_with_mask(RefPtr<Gfx::Bitmap> mask_bitmap, Gfx::Bitmap::MaskKind mask_kind, Gfx::IntRect paint_rect, float opacity);

    void sample_under_corners(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper);
    void blit_corner_clipping(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper);

    void paint_progressbar(Gfx::IntRect frame_rect, Gfx::IntRect progress_rect, Palette palette, int min, int max, int value, StringView text);
    void paint_frame(Gfx::IntRect rect, Palette palette, Gfx::FrameStyle style);

    void apply_backdrop_filter(Gfx::IntRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter);

    void paint_outer_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_inner_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_text_shadow(int blur_radius, Gfx::IntRect bounding_rect, Gfx::IntRect text_rect, Utf8View text, Gfx::Font const& font, Color color, int fragment_baseline, Gfx::IntPoint draw_location);

    void fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);

    void draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness);

    void execute(PaintingCommandExecutor&);

    RecordingPainter()
    {
        m_state_stack.append(State());
    }

    struct State {
        Gfx::AffineTransform translation;
        Optional<Gfx::IntRect> clip_rect;
    };
    State& state() { return m_state_stack.last(); }
    State const& state() const { return m_state_stack.last(); }

private:
    void push_command(PaintingCommand command)
    {
        m_painting_commands.append(command);
    }

    Vector<PaintingCommand> m_painting_commands;
    Vector<State> m_state_stack;
};

class RecordingPainterStateSaver {
public:
    explicit RecordingPainterStateSaver(RecordingPainter& painter)
        : m_painter(painter)
    {
        m_painter.save();
    }

    ~RecordingPainterStateSaver()
    {
        m_painter.restore();
    }

private:
    RecordingPainter& m_painter;
};

}
