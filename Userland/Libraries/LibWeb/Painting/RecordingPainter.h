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
#include <LibGfx/TextWrapping.h>
#include <LibWeb/Painting/BorderRadiiData.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/GradientData.h>
#include <LibWeb/Painting/PaintOuterBoxShadowParams.h>

namespace Web::Painting {

struct CommandExecutionState;

enum class CommandResult {
    Continue,
    SkipStackingContext,
};

struct DrawTextRun {
    Color color;
    Gfx::IntPoint baseline_start;
    String string;
    NonnullRefPtr<Gfx::Font> font;
    Gfx::IntRect rect;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawText {
    Gfx::IntRect rect;
    String raw_text;
    Gfx::TextAlignment alignment;
    Color color;
    Gfx::TextElision elision;
    Gfx::TextWrapping wrapping;
    Optional<NonnullRefPtr<Gfx::Font>> font {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct FillRect {
    Gfx::IntRect rect;
    Color color;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawScaledBitmap {
    Gfx::IntRect dst_rect;
    NonnullRefPtr<Gfx::Bitmap> bitmap;
    Gfx::IntRect src_rect;
    float opacity;
    Gfx::Painter::ScalingMode scaling_mode;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct Translate {
    Gfx::IntPoint translation_delta;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct SaveState {
    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct RestoreState {
    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct AddClipRect {
    Gfx::IntRect rect;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct ClearClipRect {
    CommandResult execute(CommandExecutionState&) const;
};

struct SetFont {
    NonnullRefPtr<Gfx::Font> font;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PushStackingContext {
    bool semitransparent_or_has_non_identity_transform;
    bool has_fixed_position;
    float opacity;
    Gfx::FloatRect source_rect;
    Gfx::FloatRect transformed_destination_rect;
    DevicePixelPoint painter_location;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PopStackingContext {
    bool semitransparent_or_has_non_identity_transform;
    Gfx::Painter::ScalingMode scaling_mode;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PushStackingContextWithMask {
    DevicePixelRect paint_rect;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PopStackingContextWithMask {
    DevicePixelRect paint_rect;
    RefPtr<Gfx::Bitmap> mask_bitmap;
    Gfx::Bitmap::MaskKind mask_kind;
    float opacity;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintLinearGradient {
    Gfx::IntRect gradient_rect;
    LinearGradientData linear_gradient_data;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintOuterBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintInnerBoxShadow {
    PaintOuterBoxShadowParams outer_box_shadow_params;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintTextShadow {
    DevicePixels blur_radius;
    DevicePixelRect bounding_rect;
    DevicePixelRect text_rect;
    String text;
    NonnullRefPtr<Gfx::Font> font;
    Color color;
    DevicePixels fragment_baseline;
    DevicePixelPoint draw_location;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct FillRectWithRoundedCorners {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::CornerRadius top_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius top_right_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius;
    Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct FillPathUsingColor {
    Gfx::IntRect bounding_rect;
    Gfx::Path path;
    Color color;
    Gfx::Painter::WindingRule winding_rule;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct FillPathUsingPaintStyle {
    Gfx::IntRect bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    Gfx::Painter::WindingRule winding_rule;
    float opacity;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct StrokePathUsingColor {
    Gfx::IntRect bounding_rect;
    Gfx::Path path;
    Color color;
    float thickness;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct StrokePathUsingPaintStyle {
    Gfx::IntRect bounding_rect;
    Gfx::Path path;
    NonnullRefPtr<Gfx::PaintStyle> paint_style;
    float thickness;
    float opacity = 1.0f;
    Optional<Gfx::FloatPoint> aa_translation {};

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawEllipse {
    Gfx::IntRect rect;
    Color color;
    int thickness;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct FillElipse {
    Gfx::IntRect rect;
    Color color;
    Gfx::AntiAliasingPainter::BlendMode blend_mode;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawLine {
    Color color;
    Gfx::IntPoint from;
    Gfx::IntPoint to;
    int thickness;
    Gfx::Painter::LineStyle style;
    Color alternate_color;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawSignedDistanceField {
    Gfx::IntRect rect;
    Color color;
    Gfx::GrayscaleBitmap sdf;
    float smoothing;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintProgressbar {
    Gfx::IntRect frame_rect;
    Gfx::IntRect progress_rect;
    Palette palette;
    int min;
    int max;
    int value;
    StringView text;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintFrame {
    Gfx::IntRect rect;
    Palette palette;
    Gfx::FrameStyle style;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct ApplyBackdropFilter {
    Gfx::IntRect backdrop_region;
    BorderRadiiData border_radii_data;
    CSS::ResolvedBackdropFilter backdrop_filter;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawRect {
    Gfx::IntRect rect;
    Color color;
    bool rough;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintRadialGradient {
    Gfx::IntRect rect;
    RadialGradientData radial_gradient_data;
    Gfx::IntPoint center;
    Gfx::IntSize size;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct PaintConicGradient {
    Gfx::IntRect rect;
    ConicGradientData conic_gradient_data;
    Gfx::IntPoint position;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct DrawTriangleWave {
    Gfx::IntPoint p1;
    Gfx::IntPoint p2;
    Color color;
    int amplitude;
    int thickness;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct SampleUnderCorners {
    NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

struct BlitCornerClipping {
    NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper;

    [[nodiscard]] CommandResult execute(CommandExecutionState&) const;
};

using PaintingCommand = Variant<
    DrawTextRun,
    DrawText,
    FillRect,
    DrawScaledBitmap,
    Translate,
    SaveState,
    RestoreState,
    AddClipRect,
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
    void fill_rect_with_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const& data, DevicePixelPoint center, DevicePixelSize size);

    void draw_rect(Gfx::IntRect const& rect, Color color, bool rough = false);

    void draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, float opacity = 1.0f, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor);

    void draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness = 1, Gfx::Painter::LineStyle style = Gfx::Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);

    void draw_text(Gfx::IntRect const&, StringView, Gfx::Font const&, Gfx::TextAlignment = Gfx::TextAlignment::TopLeft, Color = Color::Black, Gfx::TextElision = Gfx::TextElision::None, Gfx::TextWrapping = Gfx::TextWrapping::DontWrap);
    void draw_text(Gfx::IntRect const& rect, StringView raw_text, Gfx::TextAlignment alignment = Gfx::TextAlignment::TopLeft, Color color = Color::Black, Gfx::TextElision elision = Gfx::TextElision::None, Gfx::TextWrapping wrapping = Gfx::TextWrapping::DontWrap);

    void draw_signed_distance_field(Gfx::IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing);

    // Streamlined text drawing routine that does no wrapping/elision/alignment.
    void draw_text_run(Gfx::IntPoint baseline_start, Utf8View string, Gfx::Font const& font, Color color, Gfx::IntRect const& rect);

    void add_clip_rect(Gfx::IntRect const& rect);
    void clear_clip_rect();

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
        DevicePixelPoint painter_location;
    };
    void push_stacking_context(PushStackingContextParams params);

    struct PopStackingContextParams {
        bool semitransparent_or_has_non_identity_transform;
        Gfx::Painter::ScalingMode scaling_mode;
    };
    void pop_stacking_context(PopStackingContextParams params);

    void push_stacking_context_with_mask(DevicePixelRect paint_rect);
    void pop_stacking_context_with_mask(RefPtr<Gfx::Bitmap> mask_bitmap, Gfx::Bitmap::MaskKind mask_kind, DevicePixelRect paint_rect, float opacity);

    void sample_under_corners(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper);
    void blit_corner_clipping(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper);

    void paint_progressbar(Gfx::IntRect frame_rect, Gfx::IntRect progress_rect, Palette palette, int min, int max, int value, StringView text);
    void paint_frame(Gfx::IntRect rect, Palette palette, Gfx::FrameStyle style);

    void apply_backdrop_filter(DevicePixelRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter);

    void paint_outer_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_inner_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_text_shadow(DevicePixels blur_radius, DevicePixelRect bounding_rect, DevicePixelRect text_rect, Utf8View text, Gfx::Font const& font, Color color, DevicePixels fragment_baseline, DevicePixelPoint draw_location);

    void fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int radius);
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius);

    void draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness);

    void execute(Gfx::Bitmap&);

private:
    void push_command(PaintingCommand command)
    {
        m_painting_commands.append(command);
    }

    Vector<PaintingCommand> m_painting_commands;
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
