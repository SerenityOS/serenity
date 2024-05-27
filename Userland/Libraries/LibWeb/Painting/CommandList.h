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

    virtual CommandResult draw_glyph_run(DrawGlyphRun const&) = 0;
    virtual CommandResult draw_text(DrawText const&) = 0;
    virtual CommandResult fill_rect(FillRect const&) = 0;
    virtual CommandResult draw_scaled_bitmap(DrawScaledBitmap const&) = 0;
    virtual CommandResult draw_scaled_immutable_bitmap(DrawScaledImmutableBitmap const&) = 0;
    virtual CommandResult set_clip_rect(SetClipRect const&) = 0;
    virtual CommandResult clear_clip_rect(ClearClipRect const&) = 0;
    virtual CommandResult push_stacking_context(PushStackingContext const&) = 0;
    virtual CommandResult pop_stacking_context(PopStackingContext const&) = 0;
    virtual CommandResult paint_linear_gradient(PaintLinearGradient const&) = 0;
    virtual CommandResult paint_radial_gradient(PaintRadialGradient const&) = 0;
    virtual CommandResult paint_conic_gradient(PaintConicGradient const&) = 0;
    virtual CommandResult paint_outer_box_shadow(PaintOuterBoxShadow const&) = 0;
    virtual CommandResult paint_inner_box_shadow(PaintInnerBoxShadow const&) = 0;
    virtual CommandResult paint_text_shadow(PaintTextShadow const&) = 0;
    virtual CommandResult fill_rect_with_rounded_corners(FillRectWithRoundedCorners const&) = 0;
    virtual CommandResult fill_path_using_color(FillPathUsingColor const&) = 0;
    virtual CommandResult fill_path_using_paint_style(FillPathUsingPaintStyle const&) = 0;
    virtual CommandResult stroke_path_using_color(StrokePathUsingColor const&) = 0;
    virtual CommandResult stroke_path_using_paint_style(StrokePathUsingPaintStyle const&) = 0;
    virtual CommandResult draw_ellipse(DrawEllipse const&) = 0;
    virtual CommandResult fill_ellipse(FillEllipse const&) = 0;
    virtual CommandResult draw_line(DrawLine const&) = 0;
    virtual CommandResult draw_signed_distance_field(DrawSignedDistanceField const&) = 0;
    virtual CommandResult apply_backdrop_filter(ApplyBackdropFilter const&) = 0;
    virtual CommandResult draw_rect(DrawRect const&) = 0;
    virtual CommandResult draw_triangle_wave(DrawTriangleWave const&) = 0;
    virtual CommandResult sample_under_corners(SampleUnderCorners const&) = 0;
    virtual CommandResult blit_corner_clipping(BlitCornerClipping const&) = 0;
    virtual CommandResult paint_borders(PaintBorders const&) = 0;
    virtual bool would_be_fully_clipped_by_painter(Gfx::IntRect) const = 0;
    virtual bool needs_prepare_glyphs_texture() const { return false; }
    virtual void prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs) = 0;
    virtual void prepare_to_execute([[maybe_unused]] size_t corner_clip_max_depth) { }
    virtual bool needs_update_immutable_bitmap_texture_cache() const = 0;
    virtual void update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>&) = 0;
};

class CommandList {
public:
    void append(Command&& command, Optional<i32> scroll_frame_id);

    void apply_scroll_offsets(Vector<Gfx::IntPoint> const& offsets_by_frame_id);
    void mark_unnecessary_commands();
    void execute(CommandExecutor&);

    size_t corner_clip_max_depth() const { return m_corner_clip_max_depth; }
    void set_corner_clip_max_depth(size_t depth) { m_corner_clip_max_depth = depth; }

private:
    struct CommandListItem {
        Optional<i32> scroll_frame_id;
        Command command;
        bool skip { false };
    };

    size_t m_corner_clip_max_depth { 0 };
    AK::SegmentedVector<CommandListItem, 512> m_commands;
};

}
