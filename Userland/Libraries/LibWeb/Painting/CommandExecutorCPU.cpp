/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Filters/StackBlurFilter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/CommandExecutorCPU.h>
#include <LibWeb/Painting/FilterPainting.h>
#include <LibWeb/Painting/RecordingPainter.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

CommandExecutorCPU::CommandExecutorCPU(Gfx::Bitmap& bitmap)
    : m_target_bitmap(bitmap)
{
    stacking_contexts.append({ .painter = AK::make<Gfx::Painter>(bitmap),
        .opacity = 1.0f,
        .destination = {},
        .scaling_mode = {} });
}

CommandResult CommandExecutorCPU::draw_glyph_run(Vector<Gfx::DrawGlyphOrEmoji> const& glyph_run, Color const& color, Gfx::FloatPoint translation, double scale)
{
    auto& painter = this->painter();
    for (auto& glyph_or_emoji : glyph_run) {
        auto transformed_glyph = glyph_or_emoji;
        transformed_glyph.visit([&](auto& glyph) {
            glyph.position = glyph.position.scaled(scale).translated(translation);
            glyph.font = *glyph.font->with_size(glyph.font->point_size() * static_cast<float>(scale));
        });
        if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
            auto& glyph = transformed_glyph.get<Gfx::DrawGlyph>();
            painter.draw_glyph(glyph.position, glyph.code_point, *glyph.font, color);
        } else {
            auto& emoji = transformed_glyph.get<Gfx::DrawEmoji>();
            painter.draw_emoji(emoji.position.to_type<int>(), *emoji.emoji, *emoji.font);
        }
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_text(Gfx::IntRect const& rect, String const& raw_text, Gfx::TextAlignment alignment, Color const& color, Gfx::TextElision elision, Gfx::TextWrapping wrapping, Optional<NonnullRefPtr<Gfx::Font>> const& font)
{
    auto& painter = this->painter();
    if (font.has_value()) {
        painter.draw_text(rect, raw_text, *font, alignment, color, elision, wrapping);
    } else {
        painter.draw_text(rect, raw_text, alignment, color, elision, wrapping);
    }
    return CommandResult::Continue;
}

template<typename Callback>
void apply_clip_paths_to_painter(Gfx::IntRect const& rect, Callback callback, Vector<Gfx::Path> const& clip_paths, Gfx::Painter& target_painter)
{
    // Setup a painter for a background canvas that we will paint to first.
    auto background_canvas = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, rect.size()).release_value_but_fixme_should_propagate_errors();
    Gfx::Painter painter(*background_canvas);

    // Offset the painter to paint in the correct location.
    painter.translate(-rect.location());

    // Paint the background canvas.
    callback(painter);

    // Apply the clip path to the target painter.
    Gfx::AntiAliasingPainter aa_painter(target_painter);
    for (auto const& clip_path : clip_paths) {
        auto fill_offset = clip_path.bounding_box().location().to_type<int>() - rect.location();
        auto paint_style = Gfx::BitmapPaintStyle::create(*background_canvas, fill_offset).release_value_but_fixme_should_propagate_errors();
        aa_painter.fill_path(clip_path, paint_style);
    }
}

CommandResult CommandExecutorCPU::fill_rect(Gfx::IntRect const& rect, Color const& color, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        painter.fill_rect(rect, color);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    auto& painter = this->painter();
    painter.draw_scaled_bitmap(dst_rect, bitmap, src_rect, 1, scaling_mode);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& immutable_bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        painter.draw_scaled_bitmap(dst_rect, immutable_bitmap.bitmap(), src_rect, 1, scaling_mode);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(dst_rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::set_clip_rect(Gfx::IntRect const& rect)
{
    auto& painter = this->painter();
    painter.clear_clip_rect();
    painter.add_clip_rect(rect);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::clear_clip_rect()
{
    painter().clear_clip_rect();
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::push_stacking_context(
    float opacity, bool is_fixed_position, Gfx::IntRect const& source_paintable_rect, Gfx::IntPoint post_transform_translation,
    CSS::ImageRendering image_rendering, StackingContextTransform transform, Optional<StackingContextMask> mask)
{
    painter().save();
    if (is_fixed_position)
        painter().translate(-painter().translation());

    if (mask.has_value()) {
        // TODO: Support masks and other stacking context features at the same time.
        // Note: Currently only SVG masking is implemented (which does not use CSS transforms anyway).
        auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, mask->mask_bitmap->size());
        if (bitmap_or_error.is_error())
            return CommandResult::Continue;
        auto bitmap = bitmap_or_error.release_value();
        stacking_contexts.append(StackingContext {
            .painter = AK::make<Gfx::Painter>(bitmap),
            .opacity = 1,
            .destination = source_paintable_rect.translated(post_transform_translation),
            .scaling_mode = Gfx::Painter::ScalingMode::None,
            .mask = mask });
        painter().translate(-source_paintable_rect.location());
        return CommandResult::Continue;
    }

    // FIXME: This extracts the affine 2D part of the full transformation matrix.
    // Use the whole matrix when we get better transformation support in LibGfx or use LibGL for drawing the bitmap
    auto affine_transform = Gfx::extract_2d_affine_transform(transform.matrix);

    if (opacity == 1.0f && affine_transform.is_identity_or_translation()) {
        // OPTIMIZATION: This is a simple translation use previous stacking context's painter.
        painter().translate(affine_transform.translation().to_rounded<int>() + post_transform_translation);
        stacking_contexts.append(StackingContext {
            .painter = MaybeOwned(painter()),
            .opacity = 1,
            .destination = {},
            .scaling_mode = {} });
        return CommandResult::Continue;
    }

    auto& current_painter = this->painter();
    auto source_rect = source_paintable_rect.to_type<float>().translated(-transform.origin);
    auto transformed_destination_rect = affine_transform.map(source_rect).translated(transform.origin);
    auto destination_rect = transformed_destination_rect.to_rounded<int>();

    // FIXME: We should find a way to scale the paintable, rather than paint into a separate bitmap,
    // then scale it. This snippet now copies the background at the destination, then scales it down/up
    // to the size of the source (which could add some artefacts, though just scaling the bitmap already does that).
    // We need to copy the background at the destination because a bunch of our rendering effects now rely on
    // being able to sample the painter (see border radii, shadows, filters, etc).
    Gfx::FloatPoint destination_clipped_fixup {};
    auto try_get_scaled_destination_bitmap = [&]() -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
        Gfx::IntRect actual_destination_rect;
        auto bitmap = TRY(current_painter.get_region_bitmap(destination_rect, Gfx::BitmapFormat::BGRA8888, actual_destination_rect));
        // get_region_bitmap() may clip to a smaller region if the requested rect goes outside the painter, so we need to account for that.
        destination_clipped_fixup = Gfx::FloatPoint { destination_rect.location() - actual_destination_rect.location() };
        destination_rect = actual_destination_rect;
        if (source_rect.size() != transformed_destination_rect.size()) {
            auto sx = static_cast<float>(source_rect.width()) / transformed_destination_rect.width();
            auto sy = static_cast<float>(source_rect.height()) / transformed_destination_rect.height();
            bitmap = TRY(bitmap->scaled(sx, sy));
            destination_clipped_fixup.scale_by(sx, sy);
        }
        return bitmap;
    };

    auto bitmap_or_error = try_get_scaled_destination_bitmap();
    if (bitmap_or_error.is_error()) {
        // NOTE: If the creation of the bitmap fails, we need to skip all painting commands that belong to this stacking context.
        //       We don't interrupt the execution of painting commands because get_region_bitmap() returns an error if the requested
        //       region is outside of the viewport (mmap fails to allocate a zero-size region), which means we can safely proceed
        //       with execution of commands outside of this stacking context.
        // FIXME: Change the get_region_bitmap() API to return ErrorOr<Optional<Bitmap>> and exit the execution of commands here
        //        if we run out of memory.
        painter().restore();
        return CommandResult::SkipStackingContext;
    }

    auto bitmap = bitmap_or_error.release_value();
    stacking_contexts.append(StackingContext {
        .painter = AK::make<Gfx::Painter>(bitmap),
        .opacity = opacity,
        .destination = destination_rect.translated(post_transform_translation),
        .scaling_mode = CSS::to_gfx_scaling_mode(image_rendering, destination_rect, destination_rect) });
    painter().translate(-source_paintable_rect.location() + destination_clipped_fixup.to_type<int>());

    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::pop_stacking_context()
{
    ScopeGuard restore_painter = [&] {
        painter().restore();
    };
    auto stacking_context = stacking_contexts.take_last();
    // Stacking contexts that don't own their painter are simple translations, and don't need to blit anything back.
    if (stacking_context.painter.is_owned()) {
        auto bitmap = stacking_context.painter->target();
        if (stacking_context.mask.has_value())
            bitmap->apply_mask(*stacking_context.mask->mask_bitmap, stacking_context.mask->mask_kind);
        auto destination_rect = stacking_context.destination;
        if (destination_rect.size() == bitmap->size()) {
            painter().blit(destination_rect.location(), *bitmap, bitmap->rect(), stacking_context.opacity);
        } else {
            painter().draw_scaled_bitmap(destination_rect, *bitmap, bitmap->rect(), stacking_context.opacity, stacking_context.scaling_mode);
        }
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_linear_gradient(Gfx::IntRect const& gradient_rect, Web::Painting::LinearGradientData const& linear_gradient_data, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        painter.fill_rect_with_linear_gradient(
            gradient_rect, linear_gradient_data.color_stops.list,
            linear_gradient_data.gradient_angle, linear_gradient_data.color_stops.repeat_length);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(gradient_rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_outer_box_shadow(PaintOuterBoxShadowParams const& outer_box_shadow_params)
{
    auto& painter = this->painter();
    Web::Painting::paint_outer_box_shadow(painter, outer_box_shadow_params);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_inner_box_shadow(PaintOuterBoxShadowParams const& outer_box_shadow_params)
{
    auto& painter = this->painter();
    Web::Painting::paint_inner_box_shadow(painter, outer_box_shadow_params);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_text_shadow(int blur_radius, Gfx::IntRect const& shadow_bounding_rect, Gfx::IntRect const& text_rect, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color const& color, int fragment_baseline, Gfx::IntPoint const& draw_location)
{
    // FIXME: Figure out the maximum bitmap size for all shadows and then allocate it once and reuse it?
    auto maybe_shadow_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, shadow_bounding_rect.size());
    if (maybe_shadow_bitmap.is_error()) {
        dbgln("Unable to allocate temporary bitmap {} for text-shadow rendering: {}", shadow_bounding_rect.size(), maybe_shadow_bitmap.error());
        return CommandResult::Continue;
    }
    auto shadow_bitmap = maybe_shadow_bitmap.release_value();

    Gfx::Painter shadow_painter { *shadow_bitmap };
    // FIXME: "Spread" the shadow somehow.
    Gfx::IntPoint const baseline_start(text_rect.x(), text_rect.y() + fragment_baseline);
    shadow_painter.translate(baseline_start);
    for (auto const& glyph_or_emoji : glyph_run) {
        if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
            auto const& glyph = glyph_or_emoji.get<Gfx::DrawGlyph>();
            shadow_painter.draw_glyph(glyph.position, glyph.code_point, *glyph.font, color);
        } else {
            auto const& emoji = glyph_or_emoji.get<Gfx::DrawEmoji>();
            shadow_painter.draw_emoji(emoji.position.to_type<int>(), *emoji.emoji, *emoji.font);
        }
    }

    // Blur
    Gfx::StackBlurFilter filter(*shadow_bitmap);
    filter.process_rgba(blur_radius, color);

    painter().blit(draw_location, *shadow_bitmap, shadow_bounding_rect);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::CornerRadius const& top_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& top_right_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_left_radius, Gfx::AntiAliasingPainter::CornerRadius const& bottom_right_radius, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        Gfx::AntiAliasingPainter aa_painter(painter);
        aa_painter.fill_rect_with_rounded_corners(
            rect,
            color,
            top_left_radius,
            top_right_radius,
            bottom_right_radius,
            bottom_left_radius);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::fill_path_using_color(Gfx::Path const& path, Color const& color, Gfx::Painter::WindingRule winding_rule, Gfx::FloatPoint const& aa_translation)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.translate(aa_translation);
    aa_painter.fill_path(path, color, winding_rule);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::fill_path_using_paint_style(Gfx::Path const& path, Gfx::PaintStyle const& paint_style, Gfx::Painter::WindingRule winding_rule, float opacity, Gfx::FloatPoint const& aa_translation)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.translate(aa_translation);
    aa_painter.fill_path(path, paint_style, opacity, winding_rule);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::stroke_path_using_color(Gfx::Path const& path, Color const& color, float thickness, Gfx::FloatPoint const& aa_translation)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.translate(aa_translation);
    aa_painter.stroke_path(path, color, thickness);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::stroke_path_using_paint_style(Gfx::Path const& path, Gfx::PaintStyle const& paint_style, float thickness, float opacity, Gfx::FloatPoint const& aa_translation)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.translate(aa_translation);
    aa_painter.stroke_path(path, paint_style, thickness, opacity);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_ellipse(Gfx::IntRect const& rect, Color const& color, int thickness)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.draw_ellipse(rect, color, thickness);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::fill_ellipse(Gfx::IntRect const& rect, Color const& color, Gfx::AntiAliasingPainter::BlendMode blend_mode)
{
    Gfx::AntiAliasingPainter aa_painter(painter());
    aa_painter.fill_ellipse(rect, color, blend_mode);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_line(Color const& color, Gfx::IntPoint const& from, Gfx::IntPoint const& to, int thickness, Gfx::Painter::LineStyle style, Color const& alternate_color)
{
    if (style == Gfx::Painter::LineStyle::Dotted) {
        Gfx::AntiAliasingPainter aa_painter(painter());
        aa_painter.draw_line(from, to, color, thickness, style, alternate_color);
    } else {
        painter().draw_line(from, to, color, thickness, style, alternate_color);
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_signed_distance_field(Gfx::IntRect const& rect, Color const& color, Gfx::GrayscaleBitmap const& sdf, float smoothing)
{
    painter().draw_signed_distance_field(rect, color, sdf, smoothing);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_frame(Gfx::IntRect const& rect, Palette const& palette, Gfx::FrameStyle style)
{
    Gfx::StylePainter::paint_frame(painter(), rect, palette, style);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::apply_backdrop_filter(Gfx::IntRect const& backdrop_region, Web::CSS::ResolvedBackdropFilter const& backdrop_filter)
{
    auto& painter = this->painter();

    // This performs the backdrop filter operation: https://drafts.fxtf.org/filter-effects-2/#backdrop-filter-operation

    // Note: The region bitmap can be smaller than the backdrop_region if it's at the edge of canvas.
    // Note: This is in DevicePixels, but we use an IntRect because `get_region_bitmap()` below writes to it.

    // FIXME: Go through the steps to find the "Backdrop Root Image"
    // https://drafts.fxtf.org/filter-effects-2/#BackdropRoot

    // 1. Copy the Backdrop Root Image into a temporary buffer, such as a raster image. Call this buffer T’.
    Gfx::IntRect actual_region {};
    auto maybe_backdrop_bitmap = painter.get_region_bitmap(backdrop_region, Gfx::BitmapFormat::BGRA8888, actual_region);
    if (actual_region.is_empty())
        return CommandResult::Continue;
    if (maybe_backdrop_bitmap.is_error()) {
        dbgln("Failed get region bitmap for backdrop-filter");
        return CommandResult::Continue;
    }
    auto backdrop_bitmap = maybe_backdrop_bitmap.release_value();

    // 2. Apply the backdrop-filter’s filter operations to the entire contents of T'.
    apply_filter_list(*backdrop_bitmap, backdrop_filter.filters);

    // FIXME: 3. If element B has any transforms (between B and the Backdrop Root), apply the inverse of those transforms to the contents of T’.

    // 4. Apply a clip to the contents of T’, using the border box of element B, including border-radius if specified. Note that the children of B are not considered for the sizing or location of this clip.
    // FIXME: 5. Draw all of element B, including its background, border, and any children elements, into T’.
    // FXIME: 6. If element B has any transforms, effects, or clips, apply those to T’.

    // 7. Composite the contents of T’ into element B’s parent, using source-over compositing.
    painter.blit(actual_region.location(), *backdrop_bitmap, backdrop_bitmap->rect());
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_rect(Gfx::IntRect const& rect, Color const& color, bool rough)
{
    painter().draw_rect(rect, color, rough);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_radial_gradient(Gfx::IntRect const& rect, Web::Painting::RadialGradientData const& radial_gradient_data, Gfx::IntPoint const& center, Gfx::IntSize const& size, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        painter.fill_rect_with_radial_gradient(rect, radial_gradient_data.color_stops.list, center, size, radial_gradient_data.color_stops.repeat_length);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_conic_gradient(Gfx::IntRect const& rect, Web::Painting::ConicGradientData const& conic_gradient_data, Gfx::IntPoint const& position, Vector<Gfx::Path> const& clip_paths)
{
    auto paint_op = [&](Gfx::Painter& painter) {
        painter.fill_rect_with_conic_gradient(rect, conic_gradient_data.color_stops.list, position, conic_gradient_data.start_angle, conic_gradient_data.color_stops.repeat_length);
    };
    if (clip_paths.is_empty()) {
        paint_op(painter());
    } else {
        apply_clip_paths_to_painter(rect, paint_op, clip_paths, painter());
    }
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::draw_triangle_wave(Gfx::IntPoint const& p1, Gfx::IntPoint const& p2, Color const& color, int amplitude, int thickness)
{
    painter().draw_triangle_wave(p1, p2, color, amplitude, thickness);
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::sample_under_corners(u32 id, CornerRadii const& corner_radii, Gfx::IntRect const& border_rect, CornerClip corner_clip)
{
    if (id >= m_corner_clippers.size())
        m_corner_clippers.resize(id + 1);

    auto clipper = BorderRadiusCornerClipper::create(corner_radii, border_rect.to_type<DevicePixels>(), corner_clip);
    if (clipper.is_error()) {
        m_corner_clippers[id] = nullptr;
        return CommandResult::Continue;
    }
    m_corner_clippers[id] = clipper.release_value();
    m_corner_clippers[id]->sample_under_corners(painter());
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::blit_corner_clipping(u32 id)
{
    if (!m_corner_clippers[id])
        return CommandResult::Continue;

    m_corner_clippers[id]->blit_corner_clipping(painter());
    m_corner_clippers[id] = nullptr;
    return CommandResult::Continue;
}

CommandResult CommandExecutorCPU::paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data)
{
    paint_all_borders(painter(), border_rect, corner_radii, borders_data);
    return CommandResult::Continue;
}

bool CommandExecutorCPU::would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
{
    return !painter().clip_rect().intersects(rect.translated(painter().translation()));
}

}
