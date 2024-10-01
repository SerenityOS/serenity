/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MaybeOwned.h>
#include <LibGfx/ScalingMode.h>
#include <LibWeb/Painting/AffineCommandExecutorCPU.h>
#include <LibWeb/Painting/RecordingPainter.h>

namespace Web::Painting {

class CommandExecutorCPU : public CommandExecutor {
public:
    CommandResult draw_glyph_run(DrawGlyphRun const&) override;
    CommandResult draw_text(DrawText const&) override;
    CommandResult fill_rect(FillRect const&) override;
    CommandResult draw_scaled_bitmap(DrawScaledBitmap const&) override;
    CommandResult draw_scaled_immutable_bitmap(DrawScaledImmutableBitmap const&) override;
    CommandResult set_clip_rect(SetClipRect const&) override;
    CommandResult clear_clip_rect(ClearClipRect const&) override;
    CommandResult push_stacking_context(PushStackingContext const&) override;
    CommandResult pop_stacking_context(PopStackingContext const&) override;
    CommandResult paint_linear_gradient(PaintLinearGradient const&) override;
    CommandResult paint_outer_box_shadow(PaintOuterBoxShadow const&) override;
    CommandResult paint_inner_box_shadow(PaintInnerBoxShadow const&) override;
    CommandResult paint_text_shadow(PaintTextShadow const&) override;
    CommandResult fill_rect_with_rounded_corners(FillRectWithRoundedCorners const&) override;
    CommandResult fill_path_using_color(FillPathUsingColor const&) override;
    CommandResult fill_path_using_paint_style(FillPathUsingPaintStyle const&) override;
    CommandResult stroke_path_using_color(StrokePathUsingColor const&) override;
    CommandResult stroke_path_using_paint_style(StrokePathUsingPaintStyle const&) override;
    CommandResult draw_ellipse(DrawEllipse const&) override;
    CommandResult fill_ellipse(FillEllipse const&) override;
    CommandResult draw_line(DrawLine const&) override;
    CommandResult apply_backdrop_filter(ApplyBackdropFilter const&) override;
    CommandResult draw_rect(DrawRect const&) override;
    CommandResult paint_radial_gradient(PaintRadialGradient const&) override;
    CommandResult paint_conic_gradient(PaintConicGradient const&) override;
    CommandResult draw_triangle_wave(DrawTriangleWave const&) override;
    CommandResult sample_under_corners(SampleUnderCorners const&) override;
    CommandResult blit_corner_clipping(BlitCornerClipping const&) override;

    bool would_be_fully_clipped_by_painter(Gfx::IntRect) const override;

    bool needs_prepare_glyphs_texture() const override { return false; }
    void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const&) override {};

    virtual void prepare_to_execute(size_t corner_clip_max_depth) override;

    bool needs_update_immutable_bitmap_texture_cache() const override { return false; }
    void update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>&) override {};

    CommandExecutorCPU(Gfx::Bitmap& bitmap, bool enable_affine_command_executor = false);

    CommandExecutor& nested_executor() override
    {
        return *m_affine_command_executor;
    }

private:
    Gfx::Bitmap& m_target_bitmap;
    bool m_enable_affine_command_executor { false };

    Vector<RefPtr<BorderRadiusCornerClipper>> m_corner_clippers_stack;

    struct StackingContext {
        MaybeOwned<Gfx::Painter> painter;
        float opacity;
        Gfx::IntRect destination;
        Gfx::ScalingMode scaling_mode;
        Optional<StackingContextMask> mask = {};
    };

    [[nodiscard]] Gfx::Painter const& painter() const { return *stacking_contexts.last().painter; }
    [[nodiscard]] Gfx::Painter& painter() { return *stacking_contexts.last().painter; }

    Vector<StackingContext> stacking_contexts;
    Optional<AffineCommandExecutorCPU> m_affine_command_executor;
};

}
