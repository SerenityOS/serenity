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
#include <LibWeb/Painting/Command.h>
#include <LibWeb/Painting/GradientData.h>
#include <LibWeb/Painting/PaintOuterBoxShadowParams.h>

namespace Web::Painting {

enum class CommandResult {
    Continue,
    SkipStackingContext,
};

class CommandExecutor {
public:
    virtual ~CommandExecutor() = default;

    virtual CommandResult draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const&, Gfx::FloatPoint translation, double scale) = 0;
    virtual CommandResult draw_text(Gfx::IntRect const&, String const&, Gfx::TextAlignment alignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&) = 0;
    virtual CommandResult fill_rect(Gfx::IntRect const&, Color const&, Vector<Gfx::Path> const& clip_paths) = 0;
    virtual CommandResult draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode) = 0;
    virtual CommandResult draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const&, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode, Vector<Gfx::Path> const& clip_paths = {}) = 0;
    virtual CommandResult set_clip_rect(Gfx::IntRect const& rect) = 0;
    virtual CommandResult clear_clip_rect() = 0;
    virtual CommandResult push_stacking_context(float opacity, bool is_fixed_position, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation, CSS::ImageRendering image_rendering, StackingContextTransform transform, Optional<StackingContextMask> mask) = 0;
    virtual CommandResult pop_stacking_context() = 0;
    virtual CommandResult paint_linear_gradient(Gfx::IntRect const&, LinearGradientData const&, Vector<Gfx::Path> const& clip_paths = {}) = 0;
    virtual CommandResult paint_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const&, Gfx::IntPoint const& center, Gfx::IntSize const& size, Vector<Gfx::Path> const& clip_paths = {}) = 0;
    virtual CommandResult paint_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const&, Gfx::IntPoint const& position, Vector<Gfx::Path> const& clip_paths = {}) = 0;
    virtual CommandResult paint_outer_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_inner_box_shadow(PaintOuterBoxShadowParams const&) = 0;
    virtual CommandResult paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, Span<Gfx::DrawGlyphOrEmoji const>, Color const&, int fragment_baseline, Gfx::IntPoint const& draw_location) = 0;
    virtual CommandResult fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color,
        Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius,
        Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius,
        Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius,
        Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius,
        Vector<Gfx::Path> const& clip_paths = {})
        = 0;
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

class CommandList {
public:
    void append(Command&& command, Optional<i32> scroll_frame_id);

    void apply_scroll_offsets(Vector<Gfx::IntPoint> const& offsets_by_frame_id);
    void execute(CommandExecutor&);

private:
    struct CommandWithScrollFrame {
        Optional<i32> scroll_frame_id;
        Command command;
    };

    AK::SegmentedVector<CommandWithScrollFrame, 512> m_commands;
};

}
