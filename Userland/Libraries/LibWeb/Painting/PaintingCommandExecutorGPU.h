/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MaybeOwned.h>
#include <LibAccelGfx/Painter.h>
#include <LibWeb/Painting/RecordingPainter.h>

namespace Web::Painting {

class PaintingCommandExecutorGPU : public PaintingCommandExecutor {
public:
    CommandResult draw_glyph_run(Gfx::FloatPoint, String, Gfx::Font const&, Color const&) override;
    CommandResult draw_text(Gfx::IntRect const& rect, String const& raw_text, Gfx::TextAlignment alignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&) override;
    CommandResult fill_rect(Gfx::IntRect const& rect, Color const&) override;
    CommandResult draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode) override;
    CommandResult draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const&, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode) override;
    CommandResult set_clip_rect(Gfx::IntRect const& rect) override;
    CommandResult clear_clip_rect() override;
    CommandResult set_font(Gfx::Font const&) override;
    CommandResult push_stacking_context(float opacity, bool, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation, CSS::ImageRendering image_rendering, StackingContextTransform transform, Optional<StackingContextMask> mask) override;
    CommandResult pop_stacking_context() override;
    CommandResult paint_linear_gradient(Gfx::IntRect const&, Web::Painting::LinearGradientData const&) override;
    CommandResult paint_outer_box_shadow(PaintOuterBoxShadowParams const&) override;
    CommandResult paint_inner_box_shadow(PaintOuterBoxShadowParams const&) override;
    CommandResult paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, String const& text, Gfx::Font const&, Color const&, int fragment_baseline, Gfx::IntPoint const& draw_location) override;
    CommandResult fill_rect_with_rounded_corners(Gfx::IntRect const&, Color const&, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius, Optional<Gfx::FloatPoint> const& aa_translation) override;
    CommandResult fill_path_using_color(Gfx::Path const&, Color const&, Gfx::Painter::WindingRule winding_rule, Optional<Gfx::FloatPoint> const& aa_translation) override;
    CommandResult fill_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const& paint_style, Gfx::Painter::WindingRule winding_rule, float opacity, Optional<Gfx::FloatPoint> const& aa_translation) override;
    CommandResult stroke_path_using_color(Gfx::Path const&, Color const& color, float thickness, Optional<Gfx::FloatPoint> const& aa_translation) override;
    CommandResult stroke_path_using_paint_style(Gfx::Path const& path, Gfx::PaintStyle const& paint_style, float thickness, float opacity, Optional<Gfx::FloatPoint> const& aa_translation) override;
    CommandResult draw_ellipse(Gfx::IntRect const& rect, Color const& color, int thickness) override;
    CommandResult fill_ellipse(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::BlendMode blend_mode) override;
    CommandResult draw_line(Color const&, Gfx::IntPoint const& from, Gfx::IntPoint const& to, int thickness, Gfx::Painter::LineStyle style, Color const& alternate_color) override;
    CommandResult draw_signed_distance_field(Gfx::IntRect const& rect, Color const&, Gfx::GrayscaleBitmap const& sdf, float smoothing) override;
    CommandResult paint_progressbar(Gfx::IntRect const& frame_rect, Gfx::IntRect const& progress_rect, Palette const& palette, int min, int max, int value, StringView const& text) override;
    CommandResult paint_frame(Gfx::IntRect const& rect, Palette const&, Gfx::FrameStyle) override;
    CommandResult apply_backdrop_filter(Gfx::IntRect const& backdrop_region, Web::CSS::ResolvedBackdropFilter const& backdrop_filter) override;
    CommandResult draw_rect(Gfx::IntRect const& rect, Color const&, bool rough) override;
    CommandResult paint_radial_gradient(Gfx::IntRect const& rect, Web::Painting::RadialGradientData const& radial_gradient_data, Gfx::IntPoint const& center, Gfx::IntSize const& size) override;
    CommandResult paint_conic_gradient(Gfx::IntRect const& rect, Web::Painting::ConicGradientData const& conic_gradient_data, Gfx::IntPoint const& position) override;
    CommandResult draw_triangle_wave(Gfx::IntPoint const& p1, Gfx::IntPoint const& p2, Color const&, int amplitude, int thickness) override;
    CommandResult sample_under_corners(BorderRadiusCornerClipper&) override;
    CommandResult blit_corner_clipping(BorderRadiusCornerClipper&) override;
    CommandResult paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data) override;

    bool would_be_fully_clipped_by_painter(Gfx::IntRect) const override;

    virtual bool needs_prepare_glyphs_texture() const override { return true; }
    void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const&) override;

    bool needs_update_immutable_bitmap_texture_cache() const override { return true; }
    void update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>&) override;

    PaintingCommandExecutorGPU(Gfx::Bitmap& bitmap);
    ~PaintingCommandExecutorGPU() override;

private:
    Gfx::Bitmap& m_target_bitmap;

    struct StackingContext {
        RefPtr<AccelGfx::Canvas> canvas;
        MaybeOwned<AccelGfx::Painter> painter;
        float opacity;
        Gfx::IntRect destination;
        int stacking_context_depth { 0 };
    };

    [[nodiscard]] AccelGfx::Painter const& painter() const { return *m_stacking_contexts.last().painter; }
    [[nodiscard]] AccelGfx::Painter& painter() { return *m_stacking_contexts.last().painter; }

    Vector<StackingContext> m_stacking_contexts;
};

}
