/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/PaintingCommandExecutorGPU.h>

namespace Web::Painting {

PaintingCommandExecutorGPU::PaintingCommandExecutorGPU(Gfx::Bitmap& bitmap)
    : m_target_bitmap(bitmap)
    , m_canvas(AccelGfx::Canvas::create(AccelGfx::Context::the(), bitmap))
    , m_painter(m_canvas)
{
}

PaintingCommandExecutorGPU::~PaintingCommandExecutorGPU()
{
    m_canvas.flush();
}

CommandResult PaintingCommandExecutorGPU::draw_text_run(Color const&, Gfx::IntPoint const&, String const&, Gfx::Font const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_text(Gfx::IntRect const&, String const&, Gfx::TextAlignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::fill_rect(Gfx::IntRect const& rect, Color const& color)
{
    painter().fill_rect(rect, color);
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_scaled_bitmap(Gfx::IntRect const&, Gfx::Bitmap const&, Gfx::IntRect const&, float, Gfx::Painter::ScalingMode)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::set_clip_rect(Gfx::IntRect const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::clear_clip_rect()
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::set_font(Gfx::Font const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::push_stacking_context(bool, float, Gfx::FloatRect const&, Gfx::FloatRect const&, Gfx::IntPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::pop_stacking_context(bool, Gfx::Painter::ScalingMode)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::push_stacking_context_with_mask(Gfx::IntRect const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::pop_stacking_context_with_mask(Gfx::IntRect const&, RefPtr<Gfx::Bitmap> const&, Gfx::Bitmap::MaskKind, float)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_linear_gradient(Gfx::IntRect const&, Web::Painting::LinearGradientData const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_outer_box_shadow(PaintOuterBoxShadowParams const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_inner_box_shadow(PaintOuterBoxShadowParams const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_text_shadow(int, Gfx::IntRect const&, Gfx::IntRect const&, String const&, Gfx::Font const&, Color const&, int, Gfx::IntPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::fill_rect_with_rounded_corners(Gfx::IntRect const&, Color const&, Gfx::AntiAliasingPainter::CornerRadius const&, Gfx::AntiAliasingPainter::CornerRadius const&, Gfx::AntiAliasingPainter::CornerRadius const&, Gfx::AntiAliasingPainter::CornerRadius const&, Optional<Gfx::FloatPoint> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::fill_path_using_color(Gfx::Path const&, Color const&, Gfx::Painter::WindingRule, Optional<Gfx::FloatPoint> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::fill_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const&, Gfx::Painter::WindingRule, float, Optional<Gfx::FloatPoint> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::stroke_path_using_color(Gfx::Path const&, Color const&, float, Optional<Gfx::FloatPoint> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::stroke_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const&, float, float, Optional<Gfx::FloatPoint> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_ellipse(Gfx::IntRect const&, Color const&, int)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::fill_ellipse(Gfx::IntRect const&, Color const&, Gfx::AntiAliasingPainter::BlendMode)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_line(Color const&, Gfx::IntPoint const&, Gfx::IntPoint const&, int, Gfx::Painter::LineStyle, Color const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_signed_distance_field(Gfx::IntRect const&, Color const&, Gfx::GrayscaleBitmap const&, float)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_progressbar(Gfx::IntRect const&, Gfx::IntRect const&, Palette const&, int, int, int, StringView const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_frame(Gfx::IntRect const&, Palette const&, Gfx::FrameStyle)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::apply_backdrop_filter(Gfx::IntRect const&, Web::CSS::ResolvedBackdropFilter const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_rect(Gfx::IntRect const&, Color const&, bool)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_radial_gradient(Gfx::IntRect const&, Web::Painting::RadialGradientData const&, Gfx::IntPoint const&, Gfx::IntSize const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_conic_gradient(Gfx::IntRect const&, Web::Painting::ConicGradientData const&, Gfx::IntPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_triangle_wave(Gfx::IntPoint const&, Gfx::IntPoint const&, Color const&, int, int)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::sample_under_corners(BorderRadiusCornerClipper&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::blit_corner_clipping(BorderRadiusCornerClipper&)
{
    // FIXME
    return CommandResult::Continue;
}

bool PaintingCommandExecutorGPU::would_be_fully_clipped_by_painter(Gfx::IntRect) const
{
    // FIXME
    return false;
}

}
