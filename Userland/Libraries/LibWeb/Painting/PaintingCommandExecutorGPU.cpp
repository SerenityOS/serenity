/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/PaintingCommandExecutorGPU.h>

namespace Web::Painting {

PaintingCommandExecutorGPU::PaintingCommandExecutorGPU(AccelGfx::Painter& painter)
    : m_painter(painter)
{
}

PaintingCommandExecutorGPU::~PaintingCommandExecutorGPU()
{
}

CommandResult PaintingCommandExecutorGPU::draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color)
{
    painter().draw_glyph_run(glyph_run, color);
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

CommandResult PaintingCommandExecutorGPU::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, float, Gfx::Painter::ScalingMode scaling_mode)
{
    // FIXME: We should avoid using Gfx::Painter specific enums in painting commands
    AccelGfx::Painter::ScalingMode accel_scaling_mode;
    switch (scaling_mode) {
    case Gfx::Painter::ScalingMode::NearestNeighbor:
    case Gfx::Painter::ScalingMode::BoxSampling:
    case Gfx::Painter::ScalingMode::SmoothPixels:
    case Gfx::Painter::ScalingMode::None:
        accel_scaling_mode = AccelGfx::Painter::ScalingMode::NearestNeighbor;
        break;
    case Gfx::Painter::ScalingMode::BilinearBlend:
        accel_scaling_mode = AccelGfx::Painter::ScalingMode::Bilinear;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    painter().draw_scaled_bitmap(dst_rect, bitmap, src_rect, accel_scaling_mode);
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::set_clip_rect(Gfx::IntRect const& rect)
{
    painter().set_clip_rect(rect);
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::clear_clip_rect()
{
    painter().clear_clip_rect();
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::set_font(Gfx::Font const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::push_stacking_context(float, bool, Gfx::IntRect const&, Gfx::IntPoint, CSS::ImageRendering, StackingContextTransform, Optional<StackingContextMask>)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::pop_stacking_context()
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::paint_linear_gradient(Gfx::IntRect const& rect, Web::Painting::LinearGradientData const& data)
{
    painter().fill_rect_with_linear_gradient(rect, data.color_stops.list, data.gradient_angle, data.color_stops.repeat_length);
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

CommandResult PaintingCommandExecutorGPU::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius, Optional<Gfx::FloatPoint> const&)
{
    painter().fill_rect_with_rounded_corners(
        rect, color,
        { static_cast<float>(top_left_radius.horizontal_radius), static_cast<float>(top_left_radius.vertical_radius) },
        { static_cast<float>(top_right_radius.horizontal_radius), static_cast<float>(top_right_radius.vertical_radius) },
        { static_cast<float>(bottom_left_radius.horizontal_radius), static_cast<float>(bottom_left_radius.vertical_radius) },
        { static_cast<float>(bottom_right_radius.horizontal_radius), static_cast<float>(bottom_right_radius.vertical_radius) });
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

CommandResult PaintingCommandExecutorGPU::fill_ellipse(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::BlendMode)
{
    auto horizontal_radius = static_cast<float>(rect.width() / 2);
    auto vertical_radius = static_cast<float>(rect.height() / 2);
    painter().fill_rect_with_rounded_corners(
        rect, color,
        { horizontal_radius, vertical_radius },
        { horizontal_radius, vertical_radius },
        { horizontal_radius, vertical_radius },
        { horizontal_radius, vertical_radius });
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_line(Color const& color, Gfx::IntPoint const& a, Gfx::IntPoint const& b, int thickness, Gfx::Painter::LineStyle, Color const&)
{
    // FIXME: Pass line style and alternate color once AccelGfx::Painter supports it
    painter().draw_line(a, b, thickness, color);
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

CommandResult PaintingCommandExecutorGPU::paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data)
{
    // FIXME: Add support for corner radiuses
    (void)corner_radii;

    Gfx::IntRect top_border_rect = {
        border_rect.x(),
        border_rect.y(),
        border_rect.width(),
        borders_data.top.width
    };
    Gfx::IntRect right_border_rect = {
        border_rect.x() + (border_rect.width() - borders_data.right.width),
        border_rect.y(),
        borders_data.right.width,
        border_rect.height()
    };
    Gfx::IntRect bottom_border_rect = {
        border_rect.x(),
        border_rect.y() + (border_rect.height() - borders_data.bottom.width),
        border_rect.width(),
        borders_data.bottom.width
    };
    Gfx::IntRect left_border_rect = {
        border_rect.x(),
        border_rect.y(),
        borders_data.left.width,
        border_rect.height()
    };

    painter().fill_rect(top_border_rect, borders_data.top.color);
    painter().fill_rect(right_border_rect, borders_data.right.color);
    painter().fill_rect(bottom_border_rect, borders_data.bottom.color);
    painter().fill_rect(left_border_rect, borders_data.left.color);

    return CommandResult::Continue;
}

bool PaintingCommandExecutorGPU::would_be_fully_clipped_by_painter(Gfx::IntRect) const
{
    // FIXME
    return false;
}

void PaintingCommandExecutorGPU::prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs)
{
    m_painter.prepare_glyph_texture(unique_glyphs);
}

}
