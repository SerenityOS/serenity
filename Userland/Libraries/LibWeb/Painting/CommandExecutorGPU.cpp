/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAccelGfx/GlyphAtlas.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/CommandExecutorGPU.h>

namespace Web::Painting {

CommandExecutorGPU::CommandExecutorGPU(AccelGfx::Context& context, Gfx::Bitmap& bitmap)
    : m_target_bitmap(bitmap)
    , m_context(context)
{
    m_context.activate();
    auto canvas = AccelGfx::Canvas::create(bitmap.size());
    auto painter = AccelGfx::Painter::create(m_context, canvas);
    m_stacking_contexts.append({ .canvas = canvas,
        .painter = move(painter),
        .opacity = 1.0f,
        .destination = {},
        .transform = {} });
}

CommandExecutorGPU::~CommandExecutorGPU()
{
    m_context.activate();
    VERIFY(m_stacking_contexts.size() == 1);
    painter().flush(m_target_bitmap);
}

CommandResult CommandExecutorGPU::draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color, Gfx::FloatPoint translation, double scale)
{
    Vector<Gfx::DrawGlyphOrEmoji> transformed_glyph_run;
    transformed_glyph_run.ensure_capacity(glyph_run.size());
    for (auto& glyph : glyph_run) {
        auto transformed_glyph = glyph;
        transformed_glyph.visit([&](auto& glyph) {
            glyph.position = glyph.position.scaled(scale).translated(translation);
            glyph.font = *glyph.font->with_size(glyph.font->point_size() * static_cast<float>(scale));
        });
        transformed_glyph_run.append(transformed_glyph);
    }
    painter().draw_glyph_run(transformed_glyph_run, color);
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_text(Gfx::IntRect const&, String const&, Gfx::TextAlignment, Color const&, Gfx::TextElision, Gfx::TextWrapping, Optional<NonnullRefPtr<Gfx::Font>> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::fill_rect(Gfx::IntRect const& rect, Color const& color, Vector<Gfx::Path> const&)
{
    // FIXME: Support clip paths
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

CommandResult CommandExecutorGPU::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    painter().draw_scaled_bitmap(dst_rect, bitmap, src_rect, to_accelgfx_scaling_mode(scaling_mode));
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& immutable_bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode, Vector<Gfx::Path> const&)
{
    // TODO: Support clip paths
    painter().draw_scaled_immutable_bitmap(dst_rect, immutable_bitmap, src_rect, to_accelgfx_scaling_mode(scaling_mode));
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::set_clip_rect(Gfx::IntRect const& rect)
{
    painter().set_clip_rect(rect);
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::clear_clip_rect()
{
    painter().clear_clip_rect();
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::push_stacking_context(float opacity, bool is_fixed_position, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation, CSS::ImageRendering, StackingContextTransform transform, Optional<StackingContextMask>)
{
    if (source_paintable_rect.is_empty())
        return CommandResult::SkipStackingContext;

    m_stacking_contexts.last().stacking_context_depth++;
    painter().save();
    if (is_fixed_position) {
        auto const& translation = painter().transform().translation();
        painter().translate(-translation);
    }

    auto stacking_context_transform = Gfx::extract_2d_affine_transform(transform.matrix);

    Gfx::AffineTransform inverse_origin_translation;
    inverse_origin_translation.translate(-transform.origin);
    Gfx::AffineTransform origin_translation;
    origin_translation.translate(transform.origin);

    Gfx::AffineTransform final_transform = origin_translation;
    final_transform.multiply(stacking_context_transform);
    final_transform.multiply(inverse_origin_translation);
    if (opacity < 1 || !stacking_context_transform.is_identity_or_translation()) {
        // If, due to layout mistakes, we encounter an excessively large rectangle here, it must be skipped to prevent
        // framebuffer allocation failure.
        if (source_paintable_rect.width() > 10000 || source_paintable_rect.height() > 10000) {
            dbgln("FIXME: Skipping stacking context with excessively large paintable rect: {}", source_paintable_rect);
            return CommandResult::SkipStackingContext;
        }

        auto canvas = AccelGfx::Canvas::create(source_paintable_rect.size());
        auto painter = AccelGfx::Painter::create(m_context, canvas);
        painter->translate(-source_paintable_rect.location().to_type<float>());
        painter->clear(Color::Transparent);
        m_stacking_contexts.append({ .canvas = canvas,
            .painter = move(painter),
            .opacity = opacity,
            .destination = source_paintable_rect,
            .transform = final_transform });
    } else {
        painter().translate(stacking_context_transform.translation() + post_transform_translation.to_type<float>());
        m_stacking_contexts.append({ .canvas = {},
            .painter = MaybeOwned(painter()),
            .opacity = opacity,
            .destination = {},
            .transform = final_transform });
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::pop_stacking_context()
{
    auto stacking_context = m_stacking_contexts.take_last();
    VERIFY(stacking_context.stacking_context_depth == 0);
    if (stacking_context.painter.is_owned()) {
        painter().blit_canvas(stacking_context.destination, *stacking_context.canvas, stacking_context.opacity, stacking_context.transform);
    }
    painter().restore();
    m_stacking_contexts.last().stacking_context_depth--;
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_linear_gradient(Gfx::IntRect const& rect, Web::Painting::LinearGradientData const& data, Vector<Gfx::Path> const&)
{
    // FIXME: Support clip paths
    painter().fill_rect_with_linear_gradient(rect, data.color_stops.list, data.gradient_angle, data.color_stops.repeat_length);
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_outer_box_shadow(PaintOuterBoxShadowParams const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_inner_box_shadow(PaintOuterBoxShadowParams const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color const& color, int fragment_baseline, Gfx::IntPoint const& draw_location)
{
    auto text_shadow_canvas = AccelGfx::Canvas::create(shadow_bounding_rect.size());
    auto text_shadow_painter = AccelGfx::Painter::create(m_context, text_shadow_canvas);
    text_shadow_painter->clear(color.with_alpha(0));

    Gfx::FloatRect const shadow_location { draw_location, shadow_bounding_rect.size() };
    Gfx::IntPoint const baseline_start(text_rect.x(), text_rect.y() + fragment_baseline);
    text_shadow_painter->translate(baseline_start.to_type<float>());
    text_shadow_painter->draw_glyph_run(glyph_run, color);
    if (blur_radius == 0) {
        painter().blit_canvas(shadow_location, *text_shadow_canvas);
        return CommandResult::Continue;
    }

    auto horizontal_blur_canvas = AccelGfx::Canvas::create(shadow_bounding_rect.size());
    auto horizontal_blur_painter = AccelGfx::Painter::create(m_context, horizontal_blur_canvas);
    horizontal_blur_painter->clear(color.with_alpha(0));
    horizontal_blur_painter->blit_blurred_canvas(shadow_bounding_rect.to_type<float>(), *text_shadow_canvas, blur_radius, AccelGfx::Painter::BlurDirection::Horizontal);
    painter().blit_blurred_canvas(shadow_location, *horizontal_blur_canvas, blur_radius, AccelGfx::Painter::BlurDirection::Vertical);
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius, Vector<Gfx::Path> const&)
{
    // FIXME: Support clip paths
    painter().fill_rect_with_rounded_corners(
        rect, color,
        { static_cast<float>(top_left_radius.horizontal_radius), static_cast<float>(top_left_radius.vertical_radius) },
        { static_cast<float>(top_right_radius.horizontal_radius), static_cast<float>(top_right_radius.vertical_radius) },
        { static_cast<float>(bottom_left_radius.horizontal_radius), static_cast<float>(bottom_left_radius.vertical_radius) },
        { static_cast<float>(bottom_right_radius.horizontal_radius), static_cast<float>(bottom_right_radius.vertical_radius) });
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::fill_path_using_color(Gfx::Path const&, Color const&, Gfx::Painter::WindingRule, Gfx::FloatPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::fill_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const&, Gfx::Painter::WindingRule, float, Gfx::FloatPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::stroke_path_using_color(Gfx::Path const&, Color const&, float, Gfx::FloatPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::stroke_path_using_paint_style(Gfx::Path const&, Gfx::PaintStyle const&, float, float, Gfx::FloatPoint const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_ellipse(Gfx::IntRect const&, Color const&, int)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::fill_ellipse(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::BlendMode)
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

CommandResult CommandExecutorGPU::draw_line(Color const& color, Gfx::IntPoint const& a, Gfx::IntPoint const& b, int thickness, Gfx::Painter::LineStyle, Color const&)
{
    // FIXME: Pass line style and alternate color once AccelGfx::Painter supports it
    painter().draw_line(a, b, thickness, color);
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_signed_distance_field(Gfx::IntRect const&, Color const&, Gfx::GrayscaleBitmap const&, float)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_frame(Gfx::IntRect const&, Palette const&, Gfx::FrameStyle)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::apply_backdrop_filter(Gfx::IntRect const&, Web::CSS::ResolvedBackdropFilter const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_rect(Gfx::IntRect const&, Color const&, bool)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_radial_gradient(Gfx::IntRect const&, Web::Painting::RadialGradientData const&, Gfx::IntPoint const&, Gfx::IntSize const&, Vector<Gfx::Path> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_conic_gradient(Gfx::IntRect const&, Web::Painting::ConicGradientData const&, Gfx::IntPoint const&, Vector<Gfx::Path> const&)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::draw_triangle_wave(Gfx::IntPoint const&, Gfx::IntPoint const&, Color const&, int, int)
{
    // FIXME
    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::sample_under_corners(u32 id, CornerRadii const& corner_radii, Gfx::IntRect const& border_rect, CornerClip)
{
    m_corner_clippers.resize(id + 1);
    m_corner_clippers[id] = make<BorderRadiusCornerClipper>();
    auto& corner_clipper = *m_corner_clippers[id];

    auto const& top_left = corner_radii.top_left;
    auto const& top_right = corner_radii.top_right;
    auto const& bottom_right = corner_radii.bottom_right;
    auto const& bottom_left = corner_radii.bottom_left;

    auto sampling_config = calculate_border_radius_sampling_config(corner_radii, border_rect);
    auto const& page_locations = sampling_config.page_locations;
    auto const& bitmap_locations = sampling_config.bitmap_locations;

    auto top_left_corner_size = Gfx::IntSize { top_left.horizontal_radius, top_left.vertical_radius };
    auto top_right_corner_size = Gfx::IntSize { top_right.horizontal_radius, top_right.vertical_radius };
    auto bottom_right_corner_size = Gfx::IntSize { bottom_right.horizontal_radius, bottom_right.vertical_radius };
    auto bottom_left_corner_size = Gfx::IntSize { bottom_left.horizontal_radius, bottom_left.vertical_radius };

    corner_clipper.page_top_left_rect = { page_locations.top_left, top_left_corner_size };
    corner_clipper.page_top_right_rect = { page_locations.top_right, top_right_corner_size };
    corner_clipper.page_bottom_right_rect = { page_locations.bottom_right, bottom_right_corner_size };
    corner_clipper.page_bottom_left_rect = { page_locations.bottom_left, bottom_left_corner_size };

    corner_clipper.sample_canvas_top_left_rect = { bitmap_locations.top_left, top_left_corner_size };
    corner_clipper.sample_canvas_top_right_rect = { bitmap_locations.top_right, top_right_corner_size };
    corner_clipper.sample_canvas_bottom_right_rect = { bitmap_locations.bottom_right, bottom_right_corner_size };
    corner_clipper.sample_canvas_bottom_left_rect = { bitmap_locations.bottom_left, bottom_left_corner_size };

    corner_clipper.corners_sample_canvas = AccelGfx::Canvas::create(sampling_config.corners_bitmap_size);
    auto corner_painter = AccelGfx::Painter::create(m_context, *corner_clipper.corners_sample_canvas);
    corner_painter->clear(Color::White);

    corner_painter->fill_rect_with_rounded_corners(
        Gfx::IntRect { { 0, 0 }, sampling_config.corners_bitmap_size },
        Color::Transparent,
        { static_cast<float>(top_left.horizontal_radius), static_cast<float>(top_left.vertical_radius) },
        { static_cast<float>(top_right.horizontal_radius), static_cast<float>(top_right.vertical_radius) },
        { static_cast<float>(bottom_right.horizontal_radius), static_cast<float>(bottom_right.vertical_radius) },
        { static_cast<float>(bottom_left.horizontal_radius), static_cast<float>(bottom_left.vertical_radius) },
        AccelGfx::Painter::BlendingMode::AlphaOverride);

    auto const& target_canvas = painter().canvas();
    if (!corner_clipper.sample_canvas_top_left_rect.is_empty())
        corner_painter->blit_canvas(corner_clipper.sample_canvas_top_left_rect, target_canvas, painter().transform().map(corner_clipper.page_top_left_rect), 1.0f, {}, AccelGfx::Painter::BlendingMode::AlphaPreserve);
    if (!corner_clipper.sample_canvas_top_right_rect.is_empty())
        corner_painter->blit_canvas(corner_clipper.sample_canvas_top_right_rect, target_canvas, painter().transform().map(corner_clipper.page_top_right_rect), 1.0f, {}, AccelGfx::Painter::BlendingMode::AlphaPreserve);
    if (!corner_clipper.sample_canvas_bottom_right_rect.is_empty())
        corner_painter->blit_canvas(corner_clipper.sample_canvas_bottom_right_rect, target_canvas, painter().transform().map(corner_clipper.page_bottom_right_rect), 1.0f, {}, AccelGfx::Painter::BlendingMode::AlphaPreserve);
    if (!corner_clipper.sample_canvas_bottom_left_rect.is_empty())
        corner_painter->blit_canvas(corner_clipper.sample_canvas_bottom_left_rect, target_canvas, painter().transform().map(corner_clipper.page_bottom_left_rect), 1.0f, {}, AccelGfx::Painter::BlendingMode::AlphaPreserve);

    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::blit_corner_clipping(u32 id)
{
    auto const& corner_clipper = *m_corner_clippers[id];
    auto const& corner_sample_canvas = *corner_clipper.corners_sample_canvas;
    if (!corner_clipper.sample_canvas_top_left_rect.is_empty())
        painter().blit_canvas(corner_clipper.page_top_left_rect, corner_sample_canvas, corner_clipper.sample_canvas_top_left_rect);
    if (!corner_clipper.sample_canvas_top_right_rect.is_empty())
        painter().blit_canvas(corner_clipper.page_top_right_rect, corner_sample_canvas, corner_clipper.sample_canvas_top_right_rect);
    if (!corner_clipper.sample_canvas_bottom_right_rect.is_empty())
        painter().blit_canvas(corner_clipper.page_bottom_right_rect, corner_sample_canvas, corner_clipper.sample_canvas_bottom_right_rect);
    if (!corner_clipper.sample_canvas_bottom_left_rect.is_empty())
        painter().blit_canvas(corner_clipper.page_bottom_left_rect, corner_sample_canvas, corner_clipper.sample_canvas_bottom_left_rect);

    m_corner_clippers[id].clear();

    return CommandResult::Continue;
}

CommandResult CommandExecutorGPU::paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data)
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

bool CommandExecutorGPU::would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
{
    auto translation = painter().transform().translation().to_type<int>();
    return !painter().clip_rect().intersects(rect.translated(translation));
}

void CommandExecutorGPU::prepare_glyph_texture(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs)
{
    AccelGfx::GlyphAtlas::the().update(unique_glyphs);
}

void CommandExecutorGPU::prepare_to_execute()
{
    m_context.activate();
}

void CommandExecutorGPU::update_immutable_bitmap_texture_cache(HashMap<u32, Gfx::ImmutableBitmap const*>& immutable_bitmaps)
{
    painter().update_immutable_bitmap_texture_cache(immutable_bitmaps);
}

}
