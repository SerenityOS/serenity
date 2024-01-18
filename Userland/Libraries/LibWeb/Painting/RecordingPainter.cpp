/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/RecordingPainter.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void DrawGlyphRun::translate_by(Gfx::IntPoint const& offset)
{
    for (auto& glyph : glyph_run) {
        glyph.visit([&](auto& glyph) {
            glyph.translate_by(offset.to_type<float>());
        });
    }
    rect.translate_by(offset);
}

Gfx::IntRect PaintOuterBoxShadow::bounding_rect() const
{
    return get_outer_box_shadow_bounding_rect(outer_box_shadow_params);
}

void PaintOuterBoxShadow::translate_by(Gfx::IntPoint const& offset)
{
    outer_box_shadow_params.device_content_rect.translate_by(offset.to_type<DevicePixels>());
}

void PaintInnerBoxShadow::translate_by(Gfx::IntPoint const& offset)
{
    outer_box_shadow_params.device_content_rect.translate_by(offset.to_type<DevicePixels>());
}

Gfx::IntRect SampleUnderCorners::bounding_rect() const
{
    return border_rect;
}

Gfx::IntRect BlitCornerClipping::bounding_rect() const
{
    return border_rect;
}

void RecordingPainter::sample_under_corners(u32 id, CornerRadii corner_radii, Gfx::IntRect border_rect, CornerClip corner_clip)
{
    push_command(SampleUnderCorners {
        id,
        corner_radii,
        border_rect = state().translation.map(border_rect),
        corner_clip });
}

void RecordingPainter::blit_corner_clipping(u32 id, Gfx::IntRect border_rect)
{
    push_command(BlitCornerClipping { id, border_rect = state().translation.map(border_rect) });
}

void RecordingPainter::fill_rect(Gfx::IntRect const& rect, Color color)
{
    push_command(FillRect {
        .rect = state().translation.map(rect),
        .color = color,
    });
}

void RecordingPainter::fill_path(FillPathUsingColorParams params)
{
    auto aa_translation = state().translation.map(params.translation.value_or(Gfx::FloatPoint {}));
    auto path_bounding_rect = params.path.bounding_box().translated(aa_translation).to_type<int>();
    push_command(FillPathUsingColor {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .color = params.color,
        .winding_rule = params.winding_rule,
        .aa_translation = aa_translation,
    });
}

void RecordingPainter::fill_path(FillPathUsingPaintStyleParams params)
{
    auto aa_translation = state().translation.map(params.translation.value_or(Gfx::FloatPoint {}));
    auto path_bounding_rect = params.path.bounding_box().translated(aa_translation).to_type<int>();
    push_command(FillPathUsingPaintStyle {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .paint_style = params.paint_style,
        .winding_rule = params.winding_rule,
        .opacity = params.opacity,
        .aa_translation = aa_translation,
    });
}

void RecordingPainter::stroke_path(StrokePathUsingColorParams params)
{
    auto aa_translation = state().translation.map(params.translation.value_or(Gfx::FloatPoint {}));
    auto path_bounding_rect = params.path.bounding_box().translated(aa_translation).to_type<int>();
    // Increase path bounding box by `thickness` to account for stroke.
    path_bounding_rect.inflate(params.thickness, params.thickness);
    push_command(StrokePathUsingColor {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .color = params.color,
        .thickness = params.thickness,
        .aa_translation = aa_translation,
    });
}

void RecordingPainter::stroke_path(StrokePathUsingPaintStyleParams params)
{
    auto aa_translation = state().translation.map(params.translation.value_or(Gfx::FloatPoint {}));
    auto path_bounding_rect = params.path.bounding_box().translated(aa_translation).to_type<int>();
    // Increase path bounding box by `thickness` to account for stroke.
    path_bounding_rect.inflate(params.thickness, params.thickness);
    push_command(StrokePathUsingPaintStyle {
        .path_bounding_rect = path_bounding_rect,
        .path = params.path,
        .paint_style = params.paint_style,
        .thickness = params.thickness,
        .opacity = params.opacity,
        .aa_translation = aa_translation,
    });
}

void RecordingPainter::draw_ellipse(Gfx::IntRect const& a_rect, Color color, int thickness)
{
    push_command(DrawEllipse {
        .rect = state().translation.map(a_rect),
        .color = color,
        .thickness = thickness,
    });
}

void RecordingPainter::fill_ellipse(Gfx::IntRect const& a_rect, Color color, Gfx::AntiAliasingPainter::BlendMode blend_mode)
{
    push_command(FillEllipse {
        .rect = state().translation.map(a_rect),
        .color = color,
        .blend_mode = blend_mode,
    });
}

void RecordingPainter::fill_rect_with_linear_gradient(Gfx::IntRect const& gradient_rect, LinearGradientData const& data)
{
    push_command(PaintLinearGradient {
        .gradient_rect = state().translation.map(gradient_rect),
        .linear_gradient_data = data,
    });
}

void RecordingPainter::fill_rect_with_conic_gradient(Gfx::IntRect const& rect, ConicGradientData const& data, Gfx::IntPoint const& position)
{
    push_command(PaintConicGradient {
        .rect = state().translation.map(rect),
        .conic_gradient_data = data,
        .position = position });
}

void RecordingPainter::fill_rect_with_radial_gradient(Gfx::IntRect const& rect, RadialGradientData const& data, Gfx::IntPoint center, Gfx::IntSize size)
{
    push_command(PaintRadialGradient {
        .rect = state().translation.map(rect),
        .radial_gradient_data = data,
        .center = center,
        .size = size });
}

void RecordingPainter::draw_rect(Gfx::IntRect const& rect, Color color, bool rough)
{
    push_command(DrawRect {
        .rect = state().translation.map(rect),
        .color = color,
        .rough = rough });
}

void RecordingPainter::draw_scaled_bitmap(Gfx::IntRect const& dst_rect, Gfx::Bitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    push_command(DrawScaledBitmap {
        .dst_rect = state().translation.map(dst_rect),
        .bitmap = bitmap,
        .src_rect = src_rect,
        .scaling_mode = scaling_mode,
    });
}

void RecordingPainter::draw_scaled_immutable_bitmap(Gfx::IntRect const& dst_rect, Gfx::ImmutableBitmap const& bitmap, Gfx::IntRect const& src_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    push_command(DrawScaledImmutableBitmap {
        .dst_rect = state().translation.map(dst_rect),
        .bitmap = bitmap,
        .src_rect = src_rect,
        .scaling_mode = scaling_mode,
    });
}

void RecordingPainter::draw_line(Gfx::IntPoint from, Gfx::IntPoint to, Color color, int thickness, Gfx::Painter::LineStyle style, Color alternate_color)
{
    push_command(DrawLine {
        .color = color,
        .from = state().translation.map(from),
        .to = state().translation.map(to),
        .thickness = thickness,
        .style = style,
        .alternate_color = alternate_color,
    });
}

void RecordingPainter::draw_text(Gfx::IntRect const& rect, String raw_text, Gfx::TextAlignment alignment, Color color, Gfx::TextElision elision, Gfx::TextWrapping wrapping)
{
    push_command(DrawText {
        .rect = state().translation.map(rect),
        .raw_text = move(raw_text),
        .alignment = alignment,
        .color = color,
        .elision = elision,
        .wrapping = wrapping,
    });
}

void RecordingPainter::draw_text(Gfx::IntRect const& rect, String raw_text, Gfx::Font const& font, Gfx::TextAlignment alignment, Color color, Gfx::TextElision elision, Gfx::TextWrapping wrapping)
{
    push_command(DrawText {
        .rect = state().translation.map(rect),
        .raw_text = move(raw_text),
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
        .rect = state().translation.map(dst_rect),
        .color = color,
        .sdf = sdf,
        .smoothing = smoothing,
    });
}

void RecordingPainter::draw_text_run(Gfx::IntPoint baseline_start, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color color, Gfx::IntRect const& rect)
{
    auto transformed_baseline_start = state().translation.map(baseline_start).to_type<float>();
    Vector<Gfx::DrawGlyphOrEmoji> translated_glyph_run;
    translated_glyph_run.ensure_capacity(glyph_run.size());
    for (auto glyph : glyph_run) {
        glyph.visit([&](auto& glyph) { glyph.position.translate_by(transformed_baseline_start); });
        translated_glyph_run.append(glyph);
    }
    push_command(DrawGlyphRun {
        .glyph_run = move(translated_glyph_run),
        .color = color,
        .rect = state().translation.map(rect),
    });
}

void RecordingPainter::add_clip_rect(Gfx::IntRect const& rect)
{
    auto prev_clip_rect = state().clip_rect;
    if (!state().clip_rect.has_value()) {
        state().clip_rect = state().translation.map(rect);
    } else {
        state().clip_rect->intersect(state().translation.map(rect));
    }

    if (prev_clip_rect != state().clip_rect)
        push_command(SetClipRect { .rect = *state().clip_rect });
}

void RecordingPainter::translate(int dx, int dy)
{
    m_state_stack.last().translation.translate(dx, dy);
}

void RecordingPainter::translate(Gfx::IntPoint delta)
{
    m_state_stack.last().translation.translate(delta.to_type<float>());
}

void RecordingPainter::set_font(Gfx::Font const& font)
{
    push_command(SetFont { .font = font });
}

void RecordingPainter::save()
{
    m_state_stack.append(m_state_stack.last());
}

void RecordingPainter::restore()
{
    auto prev_clip_rect = state().clip_rect;

    VERIFY(m_state_stack.size() > 1);
    m_state_stack.take_last();

    if (state().clip_rect != prev_clip_rect) {
        if (state().clip_rect.has_value())
            push_command(SetClipRect { .rect = *state().clip_rect });
        else
            push_command(ClearClipRect {});
    }
}

void RecordingPainter::push_stacking_context(PushStackingContextParams params)
{
    push_command(PushStackingContext {
        .opacity = params.opacity,
        .is_fixed_position = params.is_fixed_position,
        .source_paintable_rect = params.source_paintable_rect,
        // No translations apply to fixed-position stacking contexts.
        .post_transform_translation = params.is_fixed_position
            ? Gfx::IntPoint {}
            : state().translation.translation().to_rounded<int>(),
        .image_rendering = params.image_rendering,
        .transform = {
            .origin = params.transform.origin,
            .matrix = params.transform.matrix,
        },
        .mask = params.mask });
    m_state_stack.append(State());
}

void RecordingPainter::pop_stacking_context()
{
    m_state_stack.take_last();
    push_command(PopStackingContext {});
}

void RecordingPainter::paint_frame(Gfx::IntRect rect, Palette palette, Gfx::FrameStyle style)
{
    push_command(PaintFrame { state().translation.map(rect), palette, style });
}

void RecordingPainter::apply_backdrop_filter(Gfx::IntRect const& backdrop_region, BorderRadiiData const& border_radii_data, CSS::ResolvedBackdropFilter const& backdrop_filter)
{
    push_command(ApplyBackdropFilter {
        .backdrop_region = state().translation.map(backdrop_region),
        .border_radii_data = border_radii_data,
        .backdrop_filter = backdrop_filter,
    });
}

void RecordingPainter::paint_outer_box_shadow_params(PaintOuterBoxShadowParams params)
{
    params.device_content_rect = state().translation.map(params.device_content_rect.to_type<int>()).to_type<DevicePixels>();
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

void RecordingPainter::paint_text_shadow(int blur_radius, Gfx::IntRect bounding_rect, Gfx::IntRect text_rect, Span<Gfx::DrawGlyphOrEmoji const> glyph_run, Color color, int fragment_baseline, Gfx::IntPoint draw_location)
{
    push_command(PaintTextShadow {
        .blur_radius = blur_radius,
        .shadow_bounding_rect = bounding_rect,
        .text_rect = text_rect,
        .glyph_run = Vector<Gfx::DrawGlyphOrEmoji> { glyph_run },
        .color = color,
        .fragment_baseline = fragment_baseline,
        .draw_location = state().translation.map(draw_location) });
}

void RecordingPainter::fill_rect_with_rounded_corners(Gfx::IntRect const& rect, Color color, Gfx::AntiAliasingPainter::CornerRadius top_left_radius, Gfx::AntiAliasingPainter::CornerRadius top_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_right_radius, Gfx::AntiAliasingPainter::CornerRadius bottom_left_radius)
{
    if (!top_left_radius && !top_right_radius && !bottom_right_radius && !bottom_left_radius) {
        fill_rect(rect, color);
        return;
    }

    push_command(FillRectWithRoundedCorners {
        .rect = state().translation.map(rect),
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

void RecordingPainter::draw_triangle_wave(Gfx::IntPoint a_p1, Gfx::IntPoint a_p2, Color color, int amplitude, int thickness = 1)
{
    push_command(DrawTriangleWave {
        .p1 = state().translation.map(a_p1),
        .p2 = state().translation.map(a_p2),
        .color = color,
        .amplitude = amplitude,
        .thickness = thickness });
}

void RecordingPainter::paint_borders(DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const& borders_data)
{
    if (borders_data.top.width == 0 && borders_data.right.width == 0 && borders_data.bottom.width == 0 && borders_data.left.width == 0)
        return;
    push_command(PaintBorders { border_rect, corner_radii, borders_data });
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

void RecordingPainter::apply_scroll_offsets(Vector<Gfx::IntPoint> const& offsets_by_frame_id)
{
    for (auto& command_with_scroll_id : m_painting_commands) {
        if (command_with_scroll_id.scroll_frame_id.has_value()) {
            auto const& scroll_frame_id = command_with_scroll_id.scroll_frame_id.value();
            auto const& scroll_offset = offsets_by_frame_id[scroll_frame_id];
            command_with_scroll_id.command.visit(
                [&](auto& command) {
                    if constexpr (requires { command.translate_by(scroll_offset); })
                        command.translate_by(scroll_offset);
                });
        }
    }
}

void RecordingPainter::execute(PaintingCommandExecutor& executor)
{
    executor.prepare_to_execute();

    if (executor.needs_prepare_glyphs_texture()) {
        HashMap<Gfx::Font const*, HashTable<u32>> unique_glyphs;
        for (auto& command_with_scroll_id : m_painting_commands) {
            auto& command = command_with_scroll_id.command;
            if (command.has<DrawGlyphRun>()) {
                for (auto const& glyph_or_emoji : command.get<DrawGlyphRun>().glyph_run) {
                    if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
                        auto const& glyph = glyph_or_emoji.get<Gfx::DrawGlyph>();
                        unique_glyphs.ensure(glyph.font, [] { return HashTable<u32> {}; }).set(glyph.code_point);
                    }
                }
            }
        }
        executor.prepare_glyph_texture(unique_glyphs);
    }

    if (executor.needs_update_immutable_bitmap_texture_cache()) {
        HashMap<u32, Gfx::ImmutableBitmap const*> immutable_bitmaps;
        for (auto const& command_with_scroll_id : m_painting_commands) {
            auto& command = command_with_scroll_id.command;
            if (command.has<DrawScaledImmutableBitmap>()) {
                auto const& immutable_bitmap = command.get<DrawScaledImmutableBitmap>().bitmap;
                immutable_bitmaps.set(immutable_bitmap->id(), immutable_bitmap.ptr());
            }
        }
        executor.update_immutable_bitmap_texture_cache(immutable_bitmaps);
    }

    HashTable<u32> skipped_sample_corner_commands;
    size_t next_command_index = 0;
    while (next_command_index < m_painting_commands.size()) {
        auto& command_with_scroll_id = m_painting_commands[next_command_index++];
        auto& command = command_with_scroll_id.command;
        auto bounding_rect = command_bounding_rectangle(command);
        if (bounding_rect.has_value() && (bounding_rect->is_empty() || executor.would_be_fully_clipped_by_painter(*bounding_rect))) {
            if (command.has<SampleUnderCorners>()) {
                auto const& sample_under_corners = command.get<SampleUnderCorners>();
                skipped_sample_corner_commands.set(sample_under_corners.id);
            }
            continue;
        }

        auto result = command.visit(
            [&](DrawGlyphRun const& command) {
                return executor.draw_glyph_run(command.glyph_run, command.color);
            },
            [&](DrawText const& command) {
                return executor.draw_text(command.rect, command.raw_text, command.alignment, command.color, command.elision, command.wrapping, command.font);
            },
            [&](FillRect const& command) {
                return executor.fill_rect(command.rect, command.color);
            },
            [&](DrawScaledBitmap const& command) {
                return executor.draw_scaled_bitmap(command.dst_rect, command.bitmap, command.src_rect, command.scaling_mode);
            },
            [&](DrawScaledImmutableBitmap const& command) {
                return executor.draw_scaled_immutable_bitmap(command.dst_rect, command.bitmap, command.src_rect, command.scaling_mode);
            },
            [&](SetClipRect const& command) {
                return executor.set_clip_rect(command.rect);
            },
            [&](ClearClipRect const&) {
                return executor.clear_clip_rect();
            },
            [&](SetFont const& command) {
                return executor.set_font(command.font);
            },
            [&](PushStackingContext const& command) {
                return executor.push_stacking_context(command.opacity, command.is_fixed_position, command.source_paintable_rect, command.post_transform_translation, command.image_rendering, command.transform, command.mask);
            },
            [&](PopStackingContext const&) {
                return executor.pop_stacking_context();
            },
            [&](PaintLinearGradient const& command) {
                return executor.paint_linear_gradient(command.gradient_rect, command.linear_gradient_data);
            },
            [&](PaintRadialGradient const& command) {
                return executor.paint_radial_gradient(command.rect, command.radial_gradient_data, command.center, command.size);
            },
            [&](PaintConicGradient const& command) {
                return executor.paint_conic_gradient(command.rect, command.conic_gradient_data, command.position);
            },
            [&](PaintOuterBoxShadow const& command) {
                return executor.paint_outer_box_shadow(command.outer_box_shadow_params);
            },
            [&](PaintInnerBoxShadow const& command) {
                return executor.paint_inner_box_shadow(command.outer_box_shadow_params);
            },
            [&](PaintTextShadow const& command) {
                return executor.paint_text_shadow(command.blur_radius, command.shadow_bounding_rect, command.text_rect, command.glyph_run, command.color, command.fragment_baseline, command.draw_location);
            },
            [&](FillRectWithRoundedCorners const& command) {
                return executor.fill_rect_with_rounded_corners(command.rect, command.color, command.top_left_radius, command.top_right_radius, command.bottom_left_radius, command.bottom_right_radius);
            },
            [&](FillPathUsingColor const& command) {
                return executor.fill_path_using_color(command.path, command.color, command.winding_rule, command.aa_translation);
            },
            [&](FillPathUsingPaintStyle const& command) {
                return executor.fill_path_using_paint_style(command.path, command.paint_style, command.winding_rule, command.opacity, command.aa_translation);
            },
            [&](StrokePathUsingColor const& command) {
                return executor.stroke_path_using_color(command.path, command.color, command.thickness, command.aa_translation);
            },
            [&](StrokePathUsingPaintStyle const& command) {
                return executor.stroke_path_using_paint_style(command.path, command.paint_style, command.thickness, command.opacity, command.aa_translation);
            },
            [&](DrawEllipse const& command) {
                return executor.draw_ellipse(command.rect, command.color, command.thickness);
            },
            [&](FillEllipse const& command) {
                return executor.fill_ellipse(command.rect, command.color, command.blend_mode);
            },
            [&](DrawLine const& command) {
                return executor.draw_line(command.color, command.from, command.to, command.thickness, command.style, command.alternate_color);
            },
            [&](DrawSignedDistanceField const& command) {
                return executor.draw_signed_distance_field(command.rect, command.color, command.sdf, command.smoothing);
            },
            [&](PaintFrame const& command) {
                return executor.paint_frame(command.rect, command.palette, command.style);
            },
            [&](ApplyBackdropFilter const& command) {
                return executor.apply_backdrop_filter(command.backdrop_region, command.backdrop_filter);
            },
            [&](DrawRect const& command) {
                return executor.draw_rect(command.rect, command.color, command.rough);
            },
            [&](DrawTriangleWave const& command) {
                return executor.draw_triangle_wave(command.p1, command.p2, command.color, command.amplitude, command.thickness);
            },
            [&](SampleUnderCorners const& command) {
                return executor.sample_under_corners(command.id, command.corner_radii, command.border_rect, command.corner_clip);
            },
            [&](BlitCornerClipping const& command) {
                if (skipped_sample_corner_commands.contains(command.id)) {
                    // FIXME: If a sampling command falls outside the viewport and is not executed, the associated blit
                    //        should also be skipped if it is within the viewport. In a properly generated list of
                    //        painting commands, sample and blit commands should have matching rectangles, preventing
                    //        this discrepancy.
                    dbgln("Skipping blit_corner_clipping command because the sample_under_corners command was skipped.");
                    return CommandResult::Continue;
                }
                return executor.blit_corner_clipping(command.id);
            },
            [&](PaintBorders const& command) {
                return executor.paint_borders(command.border_rect, command.corner_radii, command.borders_data);
            });

        if (result == CommandResult::SkipStackingContext) {
            auto stacking_context_nesting_level = 1;
            while (next_command_index < m_painting_commands.size()) {
                if (m_painting_commands[next_command_index].command.has<PushStackingContext>()) {
                    stacking_context_nesting_level++;
                } else if (m_painting_commands[next_command_index].command.has<PopStackingContext>()) {
                    stacking_context_nesting_level--;
                }

                next_command_index++;

                if (stacking_context_nesting_level == 0)
                    break;
            }
        }
    }
}

}
