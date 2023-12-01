/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAccelGfx/GlyphAtlas.h>
#include <LibWeb/Painting/PaintingCommandExecutorGPU.h>

namespace Web::Painting {

PaintingCommandExecutorGPU::PaintingCommandExecutorGPU(Gfx::Bitmap& bitmap)
    : m_target_bitmap(bitmap)
{
    auto painter = AccelGfx::Painter::create();
    auto canvas = AccelGfx::Canvas::create(bitmap.size());
    painter->set_target_canvas(canvas);
    m_stacking_contexts.append({ .canvas = canvas,
        .painter = move(painter),
        .opacity = 1.0f,
        .destination = {} });
}

PaintingCommandExecutorGPU::~PaintingCommandExecutorGPU()
{
    VERIFY(m_stacking_contexts.size() == 1);
    painter().flush(m_target_bitmap);
}

CommandResult PaintingCommandExecutorGPU::draw_glyph_run(Gfx::FloatPoint baseline_start, String text, Gfx::Font const& font, Color const& color)
{
    auto glyph_run = Gfx::get_glyph_run(baseline_start, Utf8View(text), font);
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

static AccelGfx::Painter::ScalingMode to_accelgfx_scaling_mode(Gfx::Painter::ScalingMode scaling_mode)
{
    switch (scaling_mode) {
    case Gfx::Painter::ScalingMode::NearestNeighbor:
    case Gfx::Painter::ScalingMode::BoxSampling:
    case Gfx::Painter::ScalingMode::SmoothPixels:
    case Gfx::Painter::ScalingMode::None:
        return AccelGfx::Painter::ScalingMode::NearestNeighbor;
    case Gfx::Painter::ScalingMode::BilinearBlend:
        return AccelGfx::Painter::ScalingMode::Bilinear;
    default:
        VERIFY_NOT_REACHED();
    }
}

CommandResult PaintingCommandExecutorGPU::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    painter().draw_scaled_bitmap(dst_rect, bitmap, src_rect, to_accelgfx_scaling_mode(scaling_mode));
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& immutable_bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    painter().draw_scaled_immutable_bitmap(dst_rect, immutable_bitmap, src_rect, to_accelgfx_scaling_mode(scaling_mode));
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

CommandResult PaintingCommandExecutorGPU::push_stacking_context(float opacity, bool is_fixed_position, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation, CSS::ImageRendering, StackingContextTransform transform, Optional<StackingContextMask>)
{
    m_stacking_contexts.last().stacking_context_depth++;
    painter().save();
    if (is_fixed_position) {
        auto const& translation = painter().transform().translation();
        painter().translate(-translation);
    }

    auto affine_transform = Gfx::extract_2d_affine_transform(transform.matrix);

    if (opacity < 1) {
        auto painter = AccelGfx::Painter::create();
        auto canvas = AccelGfx::Canvas::create(source_paintable_rect.size());
        painter->set_target_canvas(canvas);
        painter->translate(-source_paintable_rect.location().to_type<float>());
        painter->clear(Color::Transparent);

        auto source_rect = source_paintable_rect.to_type<float>().translated(-transform.origin);
        auto transformed_destination_rect = affine_transform.map(source_rect).translated(transform.origin);
        auto destination_rect = transformed_destination_rect.to_rounded<int>();

        m_stacking_contexts.append({ .canvas = canvas,
            .painter = move(painter),
            .opacity = opacity,
            .destination = destination_rect });
    } else {
        painter().translate(affine_transform.translation() + post_transform_translation.to_type<float>());
        m_stacking_contexts.append({ .canvas = {},
            .painter = MaybeOwned(painter()),
            .opacity = opacity,
            .destination = {} });
    }
    return CommandResult::Continue;
}

CommandResult PaintingCommandExecutorGPU::pop_stacking_context()
{
    auto stacking_context = m_stacking_contexts.take_last();
    VERIFY(stacking_context.stacking_context_depth == 0);
    if (stacking_context.painter.is_owned()) {
        painter().blit_canvas(stacking_context.destination, *stacking_context.canvas, stacking_context.opacity);
    }
    painter().restore();
    m_stacking_contexts.last().stacking_context_depth--;
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

    if (borders_data.top.width > 0)
        painter().fill_rect(top_border_rect, borders_data.top.color);
    if (borders_data.right.width > 0)
        painter().fill_rect(right_border_rect, borders_data.right.color);
    if (borders_data.bottom.width > 0)
        painter().fill_rect(bottom_border_rect, borders_data.bottom.color);
    if (borders_data.left.width > 0)
        painter().fill_rect(left_border_rect, borders_data.left.color);

    return CommandResult::Continue;
}

bool PaintingCommandExecutorGPU::would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
{
    auto translation = painter().transform().translation().to_type<int>();
    return !painter().clip_rect().intersects(rect.translated(translation));
}

void PaintingCommandExecutorGPU::prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs)
{
    AccelGfx::GlyphAtlas::the().update(unique_glyphs);
}

void PaintingCommandExecutorGPU::update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>& immutable_bitmaps)
{
    painter().update_immutable_bitmap_texture_cache(immutable_bitmaps);
}

}
