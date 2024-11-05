/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Painter.h>
#include <LibGfx/Quad.h>
#include <LibWeb/Painting/DisplayListRecorder.h>

namespace Web::Painting {

class AffineDisplayListPlayerCPU : public DisplayListPlayer {
public:
    CommandResult draw_glyph_run(DrawGlyphRun const&) override;
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

    virtual void prepare_to_execute(size_t) override { }

    bool needs_update_immutable_bitmap_texture_cache() const override { return false; }
    void update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>&) override {};

    AffineDisplayListPlayerCPU(Gfx::Bitmap& bitmap, Gfx::AffineTransform transform, Gfx::IntRect clip);
    virtual ~AffineDisplayListPlayerCPU() override = default;

private:
    struct Clip {
        Gfx::FloatQuad quad;
        Gfx::IntRect bounds;
        bool is_rectangular = false;

        bool operator==(Clip const&) const = default;
    };

    // FIXME: Support masking.
    struct StackingContext {
        Gfx::AffineTransform transform;
        Clip clip;
        NonnullRefPtr<Gfx::Bitmap> target;
        Gfx::IntPoint origin = {};
        float opacity = 1.0f;

        Gfx::IntRect rect() const
        {
            return target->rect().translated(origin);
        }
    };

    Gfx::AntiAliasingPainter aa_painter()
    {
        return Gfx::AntiAliasingPainter(m_painter);
    }

    Gfx::Painter& painter()
    {
        return m_painter;
    }

    StackingContext& stacking_context()
    {
        return m_stacking_contexts.last();
    }

    StackingContext const& stacking_context() const
    {
        return m_stacking_contexts.last();
    }

    void prepare_clipping(Gfx::IntRect bounding_rect);

    void flush_clipping(Optional<StackingContext const&> = {});

    bool needs_expensive_clipping(Gfx::IntRect bounding_rect) const;

    void set_target(Gfx::IntPoint origin, Gfx::Bitmap& bitmap)
    {
        m_painter = Gfx::Painter(bitmap);
        m_painter.translate(-origin);
    }

    Gfx::Painter m_painter;
    Vector<StackingContext> m_stacking_contexts;
    RefPtr<Gfx::Bitmap> m_expensive_clipping_target;
    RefPtr<Gfx::Bitmap> m_expensive_clipping_mask;
};

}
