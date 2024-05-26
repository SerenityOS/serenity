/*
 * Copyright (c) 2023-2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
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
#include <LibWeb/Painting/Command.h>
#include <LibWeb/Painting/CommandList.h>
#include <LibWeb/Painting/GradientData.h>
#include <LibWeb/Painting/PaintOuterBoxShadowParams.h>

namespace Web::Painting {

class RecordingPainter {
    AK_MAKE_NONCOPYABLE(RecordingPainter);
    AK_MAKE_NONMOVABLE(RecordingPainter);

public:
    void fill_rect(Gfx::IntRect const& rect, Color color, Vector<Gfx::Path> const& clip_paths = {});

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

    void fill_ellipse(Gfx::IntRect const& a_rect, Color color);

    void fill_rect_with_linear_gradient(Gfx::IntRect const& gradient_rect, LinearGradientData const& data, Vector<Gfx::Path> const& clip_paths = {});
    void fill_rect_with_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const& data, Gfx::IntPoint const& position, Vector<Gfx::Path> const& clip_paths = {});
    void fill_rect_with_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const& data, Gfx::IntPoint center, Gfx::IntSize size, Vector<Gfx::Path> const& clip_paths = {});

    void draw_rect(Gfx::IntRect const& rect, Color color, bool rough = false);

    void draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor);
    void draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode = Gfx::Painter::ScalingMode::NearestNeighbor, Vector<Gfx::Path> const& clip_paths = {});

    void draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness = 1, Gfx::Painter::LineStyle style = Gfx::Painter::LineStyle::Solid, Color alternate_color = Color::Transparent);

    void draw_text(Gfx::IntRect const&, String, Gfx::Font const&, Gfx::TextAlignment = Gfx::TextAlignment::TopLeft, Color = Color::Black, Gfx::TextElision = Gfx::TextElision::None, Gfx::TextWrapping = Gfx::TextWrapping::DontWrap);

    void draw_signed_distance_field(Gfx::IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing);

    // Streamlined text drawing routine that does no wrapping/elision/alignment.
    void draw_text_run(Gfx::IntPoint baseline_start, Gfx::GlyphRun const& glyph_run, Color color, Gfx::IntRect const& rect, double scale);

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
    void blit_corner_clipping(u32 id);

    void apply_backdrop_filter(Gfx::IntRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter);

    void paint_outer_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_inner_box_shadow_params(PaintOuterBoxShadowParams params);
    void paint_text_shadow(int blur_radius, Gfx::IntRect bounding_rect, Gfx::IntRect text_rect, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color color, int fragment_baseline, Gfx::IntPoint draw_location);

    void fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius, Vector<Gfx::Path> const& clip_paths = {});
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int radius, Vector<Gfx::Path> const& clip_paths = {});
    void fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius, Vector<Gfx::Path> const& clip_paths = {});

    void draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness);

    void paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data);

    RecordingPainter(CommandList& commands_list);
    ~RecordingPainter();

    CommandList& commands_list() { return m_command_list; }

    void append(Command&& command);

private:
    struct State {
        Gfx::AffineTransform translation;
        Optional<Gfx::IntRect> clip_rect;
        Optional<i32> scroll_frame_id;
    };
    State& state() { return m_state_stack.last(); }
    State const& state() const { return m_state_stack.last(); }

    struct CornerClipState {
        u32 id;
        Gfx::IntRect rect;
    };
    Vector<CornerClipState> m_corner_clip_state_stack;

    Vector<State> m_state_stack;
    CommandList& m_command_list;
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
