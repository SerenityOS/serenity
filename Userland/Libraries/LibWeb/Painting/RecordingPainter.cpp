/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Filters/StackBlurFilter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/FilterPainting.h>
#include <LibWeb/Painting/RecordingPainter.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

struct CommandExecutionState {
    struct StackingContext {
        Gfx::Painter painter;
        Gfx::IntRect destination;
        float opacity;
    };

    [[nodiscard]] Gfx::Painter const& painter() const { return stacking_contexts.last().painter; }
    [[nodiscard]] Gfx::Painter& painter() { return stacking_contexts.last().painter; }

    [[nodiscard]] bool would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
    {
        return !painter().clip_rect().intersects(rect.translated(painter().translation()));
    }

    Vector<StackingContext> stacking_contexts;
};

CommandResult FillRectWithRoundedCorners::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    if (aa_translation.has_value())
        aa_painter.translate(*aa_translation);
    aa_painter.fill_rect_with_rounded_corners(
        rect,
        color,
        top_left_radius,
        top_right_radius,
        bottom_right_radius,
        bottom_left_radius);
    return CommandResult::Continue;
}

CommandResult DrawText::execute(CommandExecutionState& state) const
{
    if (state.would_be_fully_clipped_by_painter(rect))
        return CommandResult::Continue;
    auto& painter = state.painter();
    if (font.has_value()) {
        painter.draw_text(rect, raw_text, *font, alignment, color, elision, wrapping);
    } else {
        painter.draw_text(rect, raw_text, alignment, color, elision, wrapping);
    }
    return CommandResult::Continue;
}

CommandResult DrawTextRun::execute(CommandExecutionState& state) const
{
    if (state.would_be_fully_clipped_by_painter(rect))
        return CommandResult::Continue;
    auto& painter = state.painter();
    painter.draw_text_run(baseline_start, Utf8View(string), font, color);
    return CommandResult::Continue;
}

CommandResult FillPathUsingColor::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    if (aa_translation.has_value())
        aa_painter.translate(*aa_translation);
    aa_painter.fill_path(path, color, winding_rule);
    return CommandResult::Continue;
}

CommandResult FillPathUsingPaintStyle::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    if (aa_translation.has_value())
        aa_painter.translate(*aa_translation);
    aa_painter.fill_path(path, paint_style, opacity, winding_rule);
    return CommandResult::Continue;
}

CommandResult StrokePathUsingColor::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    if (aa_translation.has_value())
        aa_painter.translate(*aa_translation);
    aa_painter.stroke_path(path, color, thickness);
    return CommandResult::Continue;
}

CommandResult StrokePathUsingPaintStyle::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    if (aa_translation.has_value())
        aa_painter.translate(*aa_translation);
    aa_painter.stroke_path(path, paint_style, thickness, opacity);
    return CommandResult::Continue;
}

CommandResult FillRect::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.fill_rect(rect, color);
    return CommandResult::Continue;
}

CommandResult DrawScaledBitmap::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.draw_scaled_bitmap(dst_rect, bitmap, src_rect, opacity, scaling_mode);
    return CommandResult::Continue;
}

CommandResult Translate::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.translate(translation_delta);
    return CommandResult::Continue;
}

CommandResult SaveState::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.save();
    return CommandResult::Continue;
}

CommandResult RestoreState::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.restore();
    return CommandResult::Continue;
}

CommandResult AddClipRect::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.add_clip_rect(rect);
    return CommandResult::Continue;
}

CommandResult ClearClipRect::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.clear_clip_rect();
    return CommandResult::Continue;
}

CommandResult SetFont::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.set_font(font);
    return CommandResult::Continue;
}

CommandResult PushStackingContext::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    if (has_fixed_position) {
        painter.translate(-painter.translation());
    }
    if (semitransparent_or_has_non_identity_transform) {
        auto destination_rect = transformed_destination_rect.to_rounded<int>();

        // FIXME: We should find a way to scale the paintable, rather than paint into a separate bitmap,
        // then scale it. This snippet now copies the background at the destination, then scales it down/up
        // to the size of the source (which could add some artefacts, though just scaling the bitmap already does that).
        // We need to copy the background at the destination because a bunch of our rendering effects now rely on
        // being able to sample the painter (see border radii, shadows, filters, etc).
        Gfx::FloatPoint destination_clipped_fixup {};
        auto try_get_scaled_destination_bitmap = [&]() -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
            Gfx::IntRect actual_destination_rect;
            auto bitmap = TRY(painter.get_region_bitmap(destination_rect, Gfx::BitmapFormat::BGRA8888, actual_destination_rect));
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
            return CommandResult::SkipStackingContext;
        }
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();

        Gfx::Painter stacking_context_painter(bitmap);

        stacking_context_painter.translate(painter_location + destination_clipped_fixup.to_type<int>());

        state.stacking_contexts.append(CommandExecutionState::StackingContext {
            .painter = stacking_context_painter,
            .destination = destination_rect,
            .opacity = opacity,
        });
    } else {
        state.painter().save();
    }

    return CommandResult::Continue;
}

CommandResult PopStackingContext::execute(CommandExecutionState& state) const
{
    if (semitransparent_or_has_non_identity_transform) {
        auto stacking_context = state.stacking_contexts.take_last();
        auto bitmap = stacking_context.painter.target();
        auto destination_rect = stacking_context.destination;

        if (destination_rect.size() == bitmap->size()) {
            state.painter().blit(destination_rect.location(), *bitmap, bitmap->rect(), stacking_context.opacity);
        } else {
            state.painter().draw_scaled_bitmap(destination_rect, *bitmap, bitmap->rect(), stacking_context.opacity, scaling_mode);
        }
    } else {
        state.painter().restore();
    }

    return CommandResult::Continue;
}

CommandResult PushStackingContextWithMask::execute(CommandExecutionState& state) const
{
    auto bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, paint_rect.size());
    if (bitmap_or_error.is_error())
        return CommandResult::Continue;
    auto bitmap = bitmap_or_error.release_value();

    Gfx::Painter stacking_context_painter(bitmap);

    stacking_context_painter.translate(-paint_rect.location());

    state.stacking_contexts.append(CommandExecutionState::StackingContext {
        .painter = stacking_context_painter,
        .destination = {},
        .opacity = 1,
    });

    return CommandResult::Continue;
}

CommandResult PopStackingContextWithMask::execute(CommandExecutionState& state) const
{
    auto stacking_context = state.stacking_contexts.take_last();
    auto bitmap = stacking_context.painter.target();
    if (mask_bitmap)
        bitmap->apply_mask(*mask_bitmap, mask_kind);
    state.painter().blit(paint_rect.location(), *bitmap, bitmap->rect(), opacity);
    return CommandResult::Continue;
}

CommandResult PaintLinearGradient::execute(CommandExecutionState& state) const
{
    auto const& data = linear_gradient_data;
    state.painter().fill_rect_with_linear_gradient(
        gradient_rect, data.color_stops.list,
        data.gradient_angle, data.color_stops.repeat_length);
    return CommandResult::Continue;
}

CommandResult PaintRadialGradient::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.fill_rect_with_radial_gradient(rect, radial_gradient_data.color_stops.list, center, size, radial_gradient_data.color_stops.repeat_length);
    return CommandResult::Continue;
}

CommandResult PaintConicGradient::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.fill_rect_with_conic_gradient(rect, conic_gradient_data.color_stops.list, position, conic_gradient_data.start_angle, conic_gradient_data.color_stops.repeat_length);
    return CommandResult::Continue;
}

Gfx::IntRect PaintOuterBoxShadow::bounding_rect() const
{
    return get_outer_box_shadow_bounding_rect(outer_box_shadow_params);
}

CommandResult PaintOuterBoxShadow::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    paint_outer_box_shadow(painter, outer_box_shadow_params);
    return CommandResult::Continue;
}

CommandResult PaintInnerBoxShadow::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    paint_inner_box_shadow(painter, outer_box_shadow_params);
    return CommandResult::Continue;
}

CommandResult PaintTextShadow::execute(CommandExecutionState& state) const
{
    if (state.would_be_fully_clipped_by_painter(text_rect))
        return CommandResult::Continue;

    // FIXME: Figure out the maximum bitmap size for all shadows and then allocate it once and reuse it?
    auto maybe_shadow_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, shadow_bounding_rect.size());
    if (maybe_shadow_bitmap.is_error()) {
        dbgln("Unable to allocate temporary bitmap {} for text-shadow rendering: {}", shadow_bounding_rect.size(), maybe_shadow_bitmap.error());
        return CommandResult::Continue;
    }
    auto shadow_bitmap = maybe_shadow_bitmap.release_value();

    Gfx::Painter shadow_painter { *shadow_bitmap };
    // FIXME: "Spread" the shadow somehow.
    Gfx::IntPoint baseline_start(text_rect.x(), text_rect.y() + fragment_baseline);
    shadow_painter.draw_text_run(baseline_start, Utf8View(text), font, color);

    // Blur
    Gfx::StackBlurFilter filter(*shadow_bitmap);
    filter.process_rgba(blur_radius, color);

    auto& painter = state.painter();
    painter.blit(draw_location, *shadow_bitmap, shadow_bounding_rect);
    return CommandResult::Continue;
}

CommandResult DrawEllipse::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    aa_painter.draw_ellipse(rect, color, thickness);
    return CommandResult::Continue;
}

CommandResult FillElipse::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::AntiAliasingPainter aa_painter(painter);
    aa_painter.fill_ellipse(rect, color, blend_mode);
    return CommandResult::Continue;
}

CommandResult DrawLine::execute(CommandExecutionState& state) const
{
    if (style == Gfx::Painter::LineStyle::Dotted) {
        Gfx::AntiAliasingPainter aa_painter(state.painter());
        aa_painter.draw_line(from, to, color, thickness, style, alternate_color);
    } else {
        state.painter().draw_line(from, to, color, thickness, style, alternate_color);
    }
    return CommandResult::Continue;
}

CommandResult DrawSignedDistanceField::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.draw_signed_distance_field(rect, color, sdf, smoothing);
    return CommandResult::Continue;
}

CommandResult PaintProgressbar::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::StylePainter::paint_progressbar(painter, progress_rect, palette, min, max, value, text);
    Gfx::StylePainter::paint_frame(painter, frame_rect, palette, Gfx::FrameStyle::RaisedBox);
    return CommandResult::Continue;
}

CommandResult PaintFrame::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    Gfx::StylePainter::paint_frame(painter, rect, palette, style);
    return CommandResult::Continue;
}

CommandResult ApplyBackdropFilter::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();

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

CommandResult DrawRect::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.draw_rect(rect, color, rough);
    return CommandResult::Continue;
}

CommandResult DrawTriangleWave::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    painter.draw_triangle_wave(p1, p2, color, amplitude, thickness);
    return CommandResult::Continue;
}

Gfx::IntRect SampleUnderCorners::bounding_rect() const
{
    return corner_clipper->border_rect().to_type<int>();
}

CommandResult SampleUnderCorners::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    corner_clipper->sample_under_corners(painter);
    return CommandResult::Continue;
}

Gfx::IntRect BlitCornerClipping::bounding_rect() const
{
    return corner_clipper->border_rect().to_type<int>();
}

CommandResult BlitCornerClipping::execute(CommandExecutionState& state) const
{
    auto& painter = state.painter();
    corner_clipper->blit_corner_clipping(painter);
    return CommandResult::Continue;
}

void RecordingPainter::sample_under_corners(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper)
{
    push_command(SampleUnderCorners { corner_clipper });
}

void RecordingPainter::blit_corner_clipping(NonnullRefPtr<BorderRadiusCornerClipper> corner_clipper)
{
    push_command(BlitCornerClipping { corner_clipper });
}

void RecordingPainter::fill_rect(Gfx::IntRect const& rect, Color color)
{
    push_command(FillRect {
        .rect = rect,
        .color = color,
    });
}

void RecordingPainter::fill_path(FillPathUsingColorParams params)
{
    auto path_bounding_rect = params.path.bounding_box().translated(params.translation.value_or({})).to_type<int>();
    push_command(FillPathUsingColor {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .color = params.color,
        .winding_rule = params.winding_rule,
        .aa_translation = params.translation,
    });
}

void RecordingPainter::fill_path(FillPathUsingPaintStyleParams params)
{
    auto path_bounding_rect = params.path.bounding_box().translated(params.translation.value_or({})).to_type<int>();
    push_command(FillPathUsingPaintStyle {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .paint_style = params.paint_style,
        .winding_rule = params.winding_rule,
        .opacity = params.opacity,
        .aa_translation = params.translation,
    });
}

void RecordingPainter::stroke_path(StrokePathUsingColorParams params)
{
    auto path_bounding_rect = params.path.bounding_box().translated(params.translation.value_or({})).to_type<int>();
    push_command(StrokePathUsingColor {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .color = params.color,
        .thickness = params.thickness,
        .aa_translation = params.translation,
    });
}

void RecordingPainter::stroke_path(StrokePathUsingPaintStyleParams params)
{
    auto path_bounding_rect = params.path.bounding_box().translated(params.translation.value_or({})).to_type<int>();
    push_command(StrokePathUsingPaintStyle {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .paint_style = params.paint_style,
        .thickness = params.thickness,
        .aa_translation = params.translation,
    });
}

void RecordingPainter::draw_ellipse(Gfx::IntRect const& a_rect, Color color, int thickness)
{
    push_command(DrawEllipse {
        .rect = a_rect,
        .color = color,
        .thickness = thickness,
    });
}

void RecordingPainter::fill_ellipse(Gfx::IntRect const& a_rect, Color color, Gfx::AntiAliasingPainter::BlendMode blend_mode)
{
    push_command(FillElipse {
        .rect = a_rect,
        .color = color,
        .blend_mode = blend_mode,
    });
}

void RecordingPainter::fill_rect_with_linear_gradient(Gfx::IntRect const& gradient_rect, LinearGradientData const& data)
{
    push_command(PaintLinearGradient {
        .gradient_rect = gradient_rect,
        .linear_gradient_data = data,
    });
}

void RecordingPainter::fill_rect_with_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const& data, Gfx::IntPoint const& position)
{
    push_command(PaintConicGradient {
        .rect = rect,
        .conic_gradient_data = data,
        .position = position });
}

void RecordingPainter::fill_rect_with_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const& data, Gfx::IntPoint center, Gfx::IntSize size)
{
    push_command(PaintRadialGradient {
        .rect = rect,
        .radial_gradient_data = data,
        .center = center,
        .size = size });
}

void RecordingPainter::draw_rect(Gfx::IntRect const& rect, Color color, bool rough)
{
    push_command(DrawRect {
        .rect = rect,
        .color = color,
        .rough = rough });
}

void RecordingPainter::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, float opacity, Gfx::Painter::ScalingMode scaling_mode)
{
    push_command(DrawScaledBitmap {
        .dst_rect = dst_rect,
        .bitmap = bitmap,
        .src_rect = src_rect,
        .opacity = opacity,
        .scaling_mode = scaling_mode,
    });
}

void RecordingPainter::draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness, Gfx::Painter::LineStyle style, Color alternate_color)
{
    push_command(DrawLine {
        .color = color,
        .from = from,
        .to = to,
        .thickness = thickness,
        .style = style,
        .alternate_color = alternate_color,
    });
}

void RecordingPainter::draw_text(Gfx::IntRect const& rect, StringView raw_text, Gfx::TextAlignment alignment, Color color, Gfx::TextElision elision, Gfx::TextWrapping wrapping)
{
    push_command(DrawText {
        .rect = rect,
        .raw_text = String::from_utf8(raw_text).release_value_but_fixme_should_propagate_errors(),
        .alignment = alignment,
        .color = color,
        .elision = elision,
        .wrapping = wrapping,
    });
}

void RecordingPainter::draw_text(Gfx::IntRect const& rect, StringView raw_text, Gfx::Font const& font, Gfx::TextAlignment alignment, Color color, Gfx::TextElision elision, Gfx::TextWrapping wrapping)
{
    push_command(DrawText {
        .rect = rect,
        .raw_text = String::from_utf8(raw_text).release_value_but_fixme_should_propagate_errors(),
        .alignment = alignment,
        .color = color,
        .elision = elision,
        .wrapping = wrapping,
        .font = font,
    });
}

void RecordingPainter::draw_signed_distance_field(Gfx::IntRect const& dst_rect, Color color, Gfx::GrayscaleBitmap const& sdf, float smoothing)
{
    push_command(DrawSignedDistanceField {
        .rect = dst_rect,
        .color = color,
        .sdf = sdf,
        .smoothing = smoothing,
    });
}

void RecordingPainter::draw_text_run(Gfx::IntPoint baseline_start, Utf8View string, Gfx::Font const& font, Color color, Gfx::IntRect const& rect)
{
    push_command(DrawTextRun {
        .color = color,
        .baseline_start = baseline_start,
        .string = String::from_utf8(string.as_string()).release_value_but_fixme_should_propagate_errors(),
        .font = font,
        .rect = rect,
    });
}

void RecordingPainter::add_clip_rect(Gfx::IntRect const& rect)
{
    push_command(AddClipRect {
        .rect = rect,
    });
}

void RecordingPainter::clear_clip_rect()
{
    push_command(ClearClipRect {});
}

void RecordingPainter::translate(int dx, int dy)
{
    push_command(Translate {
        .translation_delta = Gfx::IntPoint { dx, dy },
    });
}

void RecordingPainter::translate(Gfx::IntPoint delta)
{
    push_command(Translate {
        .translation_delta = delta,
    });
}

void RecordingPainter::set_font(Gfx::Font const& font)
{
    push_command(SetFont { .font = font });
}

void RecordingPainter::save()
{
    push_command(SaveState {});
}

void RecordingPainter::restore()
{
    push_command(RestoreState {});
}

void RecordingPainter::push_stacking_context(PushStackingContextParams params)
{
    push_command(PushStackingContext {
        .semitransparent_or_has_non_identity_transform = params.semitransparent_or_has_non_identity_transform,
        .has_fixed_position = params.has_fixed_position,
        .opacity = params.opacity,
        .source_rect = params.source_rect,
        .transformed_destination_rect = params.transformed_destination_rect,
        .painter_location = params.painter_location,
    });
}

void RecordingPainter::pop_stacking_context(PopStackingContextParams params)
{
    push_command(PopStackingContext {
        .semitransparent_or_has_non_identity_transform = params.semitransparent_or_has_non_identity_transform,
        .scaling_mode = params.scaling_mode,
    });
}

void RecordingPainter::paint_progressbar(Gfx::IntRect frame_rect, Gfx::IntRect progress_rect, Palette palette, int min, int max, int value, StringView text)
{
    push_command(PaintProgressbar {
        .frame_rect = frame_rect,
        .progress_rect = progress_rect,
        .palette = palette,
        .min = min,
        .max = max,
        .value = value,
        .text = text,
    });
}

void RecordingPainter::paint_frame(Gfx::IntRect rect, Palette palette, Gfx::FrameStyle style)
{
    push_command(PaintFrame { rect, palette, style });
}

void RecordingPainter::apply_backdrop_filter(Gfx::IntRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter)
{
    push_command(ApplyBackdropFilter {
        .backdrop_region = backdrop_region,
        .border_radii_data = border_radii_data,
        .backdrop_filter = backdrop_filter,
    });
}

void RecordingPainter::paint_outer_box_shadow_params(PaintOuterBoxShadowParams params)
{
    push_command(PaintOuterBoxShadow {
        .outer_box_shadow_params = params,
    });
}

void RecordingPainter::paint_inner_box_shadow_params(PaintOuterBoxShadowParams params)
{
    push_command(PaintInnerBoxShadow {
        .outer_box_shadow_params = params,
    });
}

void RecordingPainter::paint_text_shadow(int blur_radius, Gfx::IntRect bounding_rect, Gfx::IntRect text_rect, Utf8View text, Gfx::Font const& font, Color color, int fragment_baseline, Gfx::IntPoint draw_location)
{
    push_command(PaintTextShadow {
        .blur_radius = blur_radius,
        .shadow_bounding_rect = bounding_rect,
        .text_rect = text_rect,
        .text = String::from_utf8(text.as_string()).release_value_but_fixme_should_propagate_errors(),
        .font = font,
        .color = color,
        .fragment_baseline = fragment_baseline,
        .draw_location = draw_location });
}

void RecordingPainter::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius)
{
    push_command(FillRectWithRoundedCorners {
        .rect = rect,
        .color = color,
        .top_left_radius = top_left_radius,
        .top_right_radius = top_right_radius,
        .bottom_left_radius = bottom_left_radius,
        .bottom_right_radius = bottom_right_radius,
    });
}

void RecordingPainter::fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int radius)
{
    fill_rect_with_rounded_corners(a_rect, color, radius, radius, radius, radius);
}

void RecordingPainter::fill_rect_with_rounded_corners(Gfx::IntRect const& a_rect, Color color, int top_left_radius, int top_right_radius, int bottom_right_radius, int bottom_left_radius)
{
    fill_rect_with_rounded_corners(a_rect, color,
        { top_left_radius, top_left_radius },
        { top_right_radius, top_right_radius },
        { bottom_right_radius, bottom_right_radius },
        { bottom_left_radius, bottom_left_radius });
}

void RecordingPainter::push_stacking_context_with_mask(Gfx::IntRect paint_rect)
{
    push_command(PushStackingContextWithMask { .paint_rect = paint_rect });
}

void RecordingPainter::pop_stacking_context_with_mask(RefPtr<Gfx::Bitmap> mask_bitmap, Gfx::Bitmap::MaskKind mask_kind, Gfx::IntRect paint_rect, float opacity)
{
    push_command(PopStackingContextWithMask {
        .paint_rect = paint_rect,
        .mask_bitmap = mask_bitmap,
        .mask_kind = mask_kind,
        .opacity = opacity });
}

void RecordingPainter::draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness = 1)
{
    push_command(DrawTriangleWave {
        .p1 = a_p1,
        .p2 = a_p2,
        .color = color,
        .amplitude = amplitude,
        .thickness = thickness });
}

static Optional<Gfx::IntRect> command_bounding_rectangle(PaintingCommand const& command)
{
    return command.visit(
        [&](auto const& command) -> Optional<Gfx::IntRect> {
            if constexpr (requires { command.bounding_rect(); })
                return command.bounding_rect();
            else
                return {};
        });
}

void RecordingPainter::execute(Gfx::Bitmap& bitmap)
{
    CommandExecutionState state;
    state.stacking_contexts.append(CommandExecutionState::StackingContext {
        .painter = Gfx::Painter(bitmap),
        .destination = Gfx::IntRect { 0, 0, 0, 0 },
        .opacity = 1,
    });

    size_t next_command_index = 0;
    while (next_command_index < m_painting_commands.size()) {
        auto& command = m_painting_commands[next_command_index++];
        auto bounding_rect = command_bounding_rectangle(command);
        if (bounding_rect.has_value() && state.would_be_fully_clipped_by_painter(*bounding_rect))
            continue;
        auto result = command.visit([&](auto const& command) { return command.execute(state); });
        if (result == CommandResult::SkipStackingContext) {
            auto stacking_context_nesting_level = 1;
            while (next_command_index < m_painting_commands.size()) {
                if (m_painting_commands[next_command_index].has<PushStackingContext>()) {
                    stacking_context_nesting_level++;
                } else if (m_painting_commands[next_command_index].has<PopStackingContext>()) {
                    stacking_context_nesting_level--;
                }

                next_command_index++;

                if (stacking_context_nesting_level == 0)
                    break;
            }
        }
    }

    VERIFY(state.stacking_contexts.size() == 1);
}

}
