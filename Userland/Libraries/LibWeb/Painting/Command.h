/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
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
        source_paintable_rect.translate_by(offset);
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

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return border_rect; }

    void translate_by(Gfx::IntPoint const& offset) { border_rect.translate_by(offset); }
};

struct BlitCornerClipping {
    u32 id;
    Gfx::IntRect border_rect;

    [[nodiscard]] Gfx::IntRect bounding_rect() const { return border_rect; }

    void translate_by(Gfx::IntPoint const& offset) { border_rect.translate_by(offset); }
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

using Command = Variant<
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

}
