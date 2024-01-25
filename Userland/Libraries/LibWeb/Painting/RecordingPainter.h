/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/SegmentedVector.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/GrayscaleBitmap.h>
#include <LibGfx/ImmutableBitmap.h>
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
#include <LibWeb/CSS/Enums.h>
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

    void translate_by(Gfx::IntPoint const& offset);
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
    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct FillRect {
    Gfx::IntRect rect;
    Color color;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct DrawScaledBitmap {
    Gfx::IntRect dst_rect;
    NonnullRefPtr<Gfx::Bitmap> bitmap;
    Gfx::IntRect src_rect;
    Gfx::Painter::ScalingMode scaling_mode;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return dst_rect; }
    void translate_by(Gfx::IntPoint const& offset) { dst_rect.translate_by(offset); }
};

struct DrawScaledImmutableBitmap {
    Gfx::IntRect dst_rect;
    NonnullRefPtr<Gfx::ImmutableBitmap> bitmap;
    Gfx::IntRect src_rect;
    Gfx::Painter::ScalingMode scaling_mode;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return dst_rect; }
    void translate_by(Gfx::IntPoint const& offset) { dst_rect.translate_by(offset); }
};

struct SetClipRect {
    Gfx::IntRect rect;
};

struct ClearClipRect { };

struct StackingContextTransform {
    Gfx::FloatPoint origin;
    Gfx::FloatMatrix4x4 matrix;
};

struct StackingContextMask {
    NonnullRefPtr<Gfx::Bitmap> mask_bitmap;
    Gfx::Bitmap::MaskKind mask_kind;
};

struct PushStackingContext {
    float opacity;
    bool is_fixed_position;
    // The bounding box of the source paintable (pre-transform).
    Gfx::IntRect source_paintable_rect;
    // A translation to be applied after the stacking context has been transformed.
    Gfx::IntPoint post_transform_translation;
    CSS::ImageRendering image_rendering;
    StackingContextTransform transform;
    Optional<StackingContextMask> mask = {};

    void translate_by(Gfx::IntPoint const& offset)
    {
        post_transform_translation.translate_by(offset);
    }
};

struct PopStackingContext { };

struct PaintLinearGradient {
    Gfx::IntRect gradient_rect;
    LinearGradientData linear_gradient_data;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return gradient_rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        gradient_rect.translate_by(offset);
    }
};

struct PaintOuterBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;
    void translate_by(Gfx::IntPoint const& offset);
};

struct PaintInnerBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;

    void translate_by(Gfx::IntPoint const& offset);
};

struct PaintTextShadow {
    int blur_radius;
    Gfx::IntRect shadow_bounding_rect;
    Gfx::IntRect text_rect;
    Vector<Gfx::DrawGlyphOrEmoji> glyph_run;
    Color color;
    int fragment_baseline;
    Gfx::IntPoint draw_location;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return { draw_location, shadow_bounding_rect.size() }; }
    void translate_by(Gfx::IntPoint const& offset) { draw_location.translate_by(offset); }
};

struct FillRectWithRoundedCorners {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::CornerRadius top_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius top_right_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }
    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct FillPathUsingColor {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    Color color;
    Gfx::Painter::WindingRule winding_rule;
    Gfx::FloatPoint aa_translation;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        path_bounding_rect.translate_by(offset);
        aa_translation.translate_by(offset.to_type<float>());
    }
};

struct FillPathUsingPaintStyle {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    Gfx::Painter::WindingRule winding_rule;
    float opacity;
    Gfx::FloatPoint aa_translation;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        path_bounding_rect.translate_by(offset);
        aa_translation.translate_by(offset.to_type<float>());
    }
};

struct StrokePathUsingColor {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    Color color;
    float thickness;
    Gfx::FloatPoint aa_translation;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        path_bounding_rect.translate_by(offset);
        aa_translation.translate_by(offset.to_type<float>());
    }
};

struct StrokePathUsingPaintStyle {
    Gfx::IntRect path_bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    float thickness;
    float opacity = 1.0f;
    Gfx::FloatPoint aa_translation;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return path_bounding_rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        path_bounding_rect.translate_by(offset);
        aa_translation.translate_by(offset.to_type<float>());
    }
};

struct DrawEllipse {
    Gfx::IntRect rect;
    Color color;
    int thickness;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        rect.translate_by(offset);
    }
};

struct FillEllipse {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::BlendMode blend_mode;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        rect.translate_by(offset);
    }
};

struct DrawLine {
    Color color;
    Gfx::IntPoint from;
    Gfx::IntPoint to;
    int thickness;
    Gfx::Painter::LineStyle style;
    Color alternate_color;

    void translate_by(Gfx::IntPoint const& offset)
    {
        from.translate_by(offset);
        to.translate_by(offset);
    }
};

struct DrawSignedDistanceField {
    Gfx::IntRect rect;
    Color color;
    Gfx::GrayscaleBitmap sdf;
    float smoothing;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        rect.translate_by(offset);
    }
};

struct PaintFrame {
    Gfx::IntRect rect;
    Palette palette;
    Gfx::FrameStyle style;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct ApplyBackdropFilter {
    Gfx::IntRect backdrop_region;
    BorderRadiiData border_radii_data;
    CSS::ResolvedBackdropFilter backdrop_filter;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return backdrop_region; }

    void translate_by(Gfx::IntPoint const& offset)
    {
        backdrop_region.translate_by(offset);
    }
};

struct DrawRect {
    Gfx::IntRect rect;
    Color color;
    bool rough;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct PaintRadialGradient {
    Gfx::IntRect rect;
    RadialGradientData radial_gradient_data;
    Gfx::IntPoint center;
    Gfx::IntSize size;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct PaintConicGradient {
    Gfx::IntRect rect;
    ConicGradientData conic_gradient_data;
    Gfx::IntPoint position;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return rect; }

    void translate_by(Gfx::IntPoint const& offset) { rect.translate_by(offset); }
};

struct DrawTriangleWave {
    Gfx::IntPoint p1;
    Gfx::IntPoint p2;
    Color color;
    int amplitude;
    int thickness;

    void translate_by(Gfx::IntPoint const& offset)
    {
        p1.translate_by(offset);
        p2.translate_by(offset);
    }
};

struct SampleUnderCorners {
    u32 id;
    CornerRadii corner_radii;
    Gfx::IntRect border_rect;
    CornerClip corner_clip;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;

    void translate_by(Gfx::IntPoint const& offset)
    {
        border_rect.translate_by(offset);
    }
};

struct BlitCornerClipping {
    u32 id;
    Gfx::IntRect border_rect;

    [[nodiscard]] Gfx::IntRect bounding_rect() const;

    void translate_by(Gfx::IntPoint const& offset)
    {
        border_rect.translate_by(offset);
    }
};

struct PaintBorders {
    DevicePixelRect border_rect;
    CornerRadii corner_radii;
    BordersDataDevicePixels borders_data;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return border_rect.to_type<int>(); }

    void translate_by(Gfx::IntPoint const& offset)
    {
        border_rect.translate_by(offset.to_type<DevicePixels>());
    }
};

using PaintingCommand = Variant<
    DrawGlyphRun,
    DrawText,
    FillRect,
    DrawScaledBitmap,
    DrawScaledImmutableBitmap,
    SetClipRect,
    ClearClipRect,
    PushStackingContext,
    PopStackingContext,
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
    FillEllipse,
    DrawLine,
    DrawSignedDistanceField,
    PaintFrame,
    ApplyBackdropFilter,
    DrawRect,
    DrawTriangleWave,
    SampleUnderCorners,
    BlitCornerClipping,
    PaintBorders>;

class PaintingCommandExecutor {
public:
    virtual ~PaintingCommandExecutor() = default;

    virtual CommandResult draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const&) = 0;
    virtual CommandResult draw_text(Gfx::IntRect const&, String const&, Gfx::TextAlignment alignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&) = 0;
    virtual CommandResult fill_rect(Gfx::IntRect const&, Color const&) = 0;
    virtual CommandResult draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode) = 0;
    virtual CommandResult draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const&, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode) = 0;
    virtual CommandResult set_clip_rect(Gfx::IntRect const& rect) = 0;
    virtual CommandResult clear_clip_rect() = 0;
    virtual CommandResult push_stacking_context(float opacity, bool is_fixed_position, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation, CSS::ImageRendering image_rendering, StackingContextTransform transform, Optional<StackingContextMask> mask) = 0;
    virtual CommandResult pop_stacking_context() = 0;
    virtual CommandResult paint_linear_gradient(Gfx::IntRect const&, LinearGradientData const&) = 0;
    virtual CommandResult paint_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const&, Gfx::IntPoint const& center, Gfx::IntSize const& size) = 0;
    virtual CommandResult paint_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const&, Gfx::IntPoint const& position) = 0;
    virtual CommandResult paint_outer_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_inner_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, Span<Gfx::DrawGlyphOrEmoji const>, Color const&, int fragment_baseline, Gfx::IntPoint const& draw_location) = 0;
    virtual CommandResult fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius) = 0;
    virtual CommandResult fill_path_using_color(Gfx::Path const&, Color const& color, Gfx::Painter::WindingRule, Gfx::FloatPoint const& aa_translation) = 0;
    virtual CommandResult fill_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const& paint_style, Gfx::Painter::WindingRule winding_rule, float opacity, Gfx::FloatPoint const& aa_translation) = 0;
    virtual CommandResult stroke_path_using_color(Gfx::Path const&, Color const& color, float thickness, Gfx::FloatPoint const& aa_translation) = 0;
    virtual CommandResult stroke_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const& paint_style, float thickness, float opacity, Gfx::FloatPoint const& aa_translation) = 0;
    virtual CommandResult draw_ellipse(Gfx::IntRect const&, Color const&, int thickness) = 0;
    virtual CommandResult fill_ellipse(Gfx::IntRect const&, Color const&, Gfx::AntiAliasingPainter::BlendMode blend_mode) = 0;
    virtual CommandResult draw_line(Color const& color, Gfx::IntPoint const& from, Gfx::IntPoint const& to, int thickness, Gfx::Painter::LineStyle, Color const& alternate_color) = 0;
    virtual CommandResult draw_signed_distance_field(Gfx::IntRect const& rect, Color const&, Gfx::GrayscaleBitmap const&, float smoothing) = 0;
    virtual CommandResult paint_frame(Gfx::IntRect const& rect, Palette const&, Gfx::FrameStyle) = 0;
    virtual CommandResult apply_backdrop_filter(Gfx::IntRect const& backdrop_region, Web::CSS::ResolvedBackdropFilter const& backdrop_filter) = 0;
    virtual CommandResult draw_rect(Gfx::IntRect const& rect, Color const&, bool rough) = 0;
    virtual CommandResult draw_triangle_wave(Gfx::IntPoint const& p1, Gfx::IntPoint const& p2, Color const& color, int amplitude, int thickness) = 0;
    virtual CommandResult sample_under_corners(u32 id, CornerRadii const&, Gfx::IntRect const&, CornerClip) = 0;
    virtual CommandResult blit_corner_clipping(u32 id) = 0;
    virtual CommandResult paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data) = 0;

    virtual bool would_be_fully_clipped_by_painter(Gfx::IntRect) const = 0;

    virtual bool needs_prepare_glyphs_texture() const { return false; }
    virtual void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs) = 0;

    virtual void prepare_to_execute() { }

    virtual bool needs_update_immutable_bitmap_texture_cache() const = 0;
    virtual void update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>&) = 0;
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

    void draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor);
    void draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor);

    void draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness = 1, Gfx::Painter::LineStyle style = Gfx::Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);

    void draw_text(Gfx::IntRect const&, String, Gfx::Font const&, Gfx::TextAlignment = Gfx::TextAlignment::TopLeft, Color = Color::Black, Gfx::TextElision = Gfx::TextElision::None, Gfx::TextWrapping = Gfx::TextWrapping::DontWrap);

    void draw_signed_distance_field(Gfx::IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing);

    // Streamlined text drawing routine that does no wrapping/elision/alignment.
    void draw_text_run(Gfx::IntPoint baseline_start, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color color, Gfx::IntRect const& rect);

    void add_clip_rect(Gfx::IntRect const& rect);

    void translate(int dx, int dy);
    void translate(Gfx::IntPoint delta);

    void set_scroll_frame_id(i32 id)
    {
        state().scroll_frame_id = id;
    }

    void save();
    void restore();

    struct PushStackingContextParams {
        float opacity;
        bool is_fixed_position;
        Gfx::IntRect source_paintable_rect;
        CSS::ImageRendering image_rendering;
        StackingContextTransform transform;
        Optional<StackingContextMask> mask = {};
    };
    void push_stacking_context(PushStackingContextParams params);
    void pop_stacking_context();

    void sample_under_corners(u32 id, CornerRadii corner_radii, Gfx::IntRect border_rect, CornerClip corner_clip);
    void blit_corner_clipping(u32 id, Gfx::IntRect border_rect);

    void paint_frame(Gfx::IntRect rect, Palette palette, Gfx::FrameStyle style);

    void apply_backdrop_filter(Gfx::IntRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter);

    void paint_outer_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_inner_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_text_shadow(int blur_radius, Gfx::IntRect bounding_rect, Gfx::IntRect text_rect, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color color, int fragment_baseline, Gfx::IntPoint draw_location);

    void fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);

    void draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness);

    void paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data);

    void execute(PaintingCommandExecutor&);

    RecordingPainter()
    {
        m_state_stack.append(State());
    }

    void apply_scroll_offsets(Vector<Gfx::IntPoint> const& offsets_by_frame_id);

private:
    struct State {
        Gfx::AffineTransform translation;
        Optional<Gfx::IntRect> clip_rect;
        Optional<i32> scroll_frame_id;
    };
    State& state() { return m_state_stack.last(); }
    State const& state() const { return m_state_stack.last(); }

    void push_command(PaintingCommand command)
    {
        m_painting_commands.append({ state().scroll_frame_id, move(command) });
    }

    struct PaintingCommandWithScrollFrame {
        Optional<i32> scroll_frame_id;
        PaintingCommand command;
    };

    AK::SegmentedVector<PaintingCommandWithScrollFrame, 512> m_painting_commands;
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
