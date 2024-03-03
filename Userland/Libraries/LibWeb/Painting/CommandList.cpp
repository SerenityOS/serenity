/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/CommandList.h>

namespace Web::Painting {

void CommandList::append(Command&& command, Optional<i32> scroll_frame_id)
{
    m_commands.append({ scroll_frame_id, move(command) });
}

static Optional<Gfx::IntRect> command_bounding_rectangle(Command const& command)
{
    return command.visit(
        [&](auto const& command) -> Optional<Gfx::IntRect> {
            if constexpr (requires { command.bounding_rect(); })
                return command.bounding_rect();
            else
                return {};
        });
}

void CommandList::apply_scroll_offsets(Vector<Gfx::IntPoint> const& offsets_by_frame_id)
{
    for (auto& command_with_scroll_id : m_commands) {
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

void CommandList::execute(CommandExecutor& executor)
{
    executor.prepare_to_execute();

    if (executor.needs_prepare_glyphs_texture()) {
        HashMap<Gfx::Font const*, HashTable<u32>> unique_glyphs;
        for (auto& command_with_scroll_id : m_commands) {
            auto& command = command_with_scroll_id.command;
            if (command.has<DrawGlyphRun>()) {
                auto scale = command.get<DrawGlyphRun>().scale;
                for (auto const& glyph_or_emoji : command.get<DrawGlyphRun>().glyph_run->glyphs()) {
                    if (glyph_or_emoji.has<Gfx::DrawGlyph>()) {
                        auto const& glyph = glyph_or_emoji.get<Gfx::DrawGlyph>();
                        auto const& font = *glyph.font->with_size(glyph.font->point_size() * static_cast<float>(scale));
                        unique_glyphs.ensure(&font, [] { return HashTable<u32> {}; }).set(glyph.code_point);
                    }
                }
            }
        }
        executor.prepare_glyph_texture(unique_glyphs);
    }

    if (executor.needs_update_immutable_bitmap_texture_cache()) {
        HashMap<u32, Gfx::ImmutableBitmap const*> immutable_bitmaps;
        for (auto const& command_with_scroll_id : m_commands) {
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
    while (next_command_index < m_commands.size()) {
        auto& command_with_scroll_id = m_commands[next_command_index++];
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
                return executor.draw_glyph_run(command.glyph_run->glyphs(), command.color, command.translation, command.scale);
            },
            [&](DrawText const& command) {
                return executor.draw_text(command.rect, command.raw_text, command.alignment, command.color,
                    command.elision, command.wrapping, command.font);
            },
            [&](FillRect const& command) {
                return executor.fill_rect(command.rect, command.color, command.clip_paths);
            },
            [&](DrawScaledBitmap const& command) {
                return executor.draw_scaled_bitmap(command.dst_rect, command.bitmap, command.src_rect,
                    command.scaling_mode);
            },
            [&](DrawScaledImmutableBitmap const& command) {
                return executor.draw_scaled_immutable_bitmap(command.dst_rect, command.bitmap, command.src_rect,
                    command.scaling_mode, command.clip_paths);
            },
            [&](SetClipRect const& command) {
                return executor.set_clip_rect(command.rect);
            },
            [&](ClearClipRect const&) {
                return executor.clear_clip_rect();
            },
            [&](PushStackingContext const& command) {
                return executor.push_stacking_context(command.opacity, command.is_fixed_position,
                    command.source_paintable_rect,
                    command.post_transform_translation,
                    command.image_rendering, command.transform, command.mask);
            },
            [&](PopStackingContext const&) {
                return executor.pop_stacking_context();
            },
            [&](PaintLinearGradient const& command) {
                return executor.paint_linear_gradient(command.gradient_rect, command.linear_gradient_data, command.clip_paths);
            },
            [&](PaintRadialGradient const& command) {
                return executor.paint_radial_gradient(command.rect, command.radial_gradient_data,
                    command.center, command.size, command.clip_paths);
            },
            [&](PaintConicGradient const& command) {
                return executor.paint_conic_gradient(command.rect, command.conic_gradient_data,
                    command.position, command.clip_paths);
            },
            [&](PaintOuterBoxShadow const& command) {
                return executor.paint_outer_box_shadow(command.outer_box_shadow_params);
            },
            [&](PaintInnerBoxShadow const& command) {
                return executor.paint_inner_box_shadow(command.outer_box_shadow_params);
            },
            [&](PaintTextShadow const& command) {
                return executor.paint_text_shadow(command.blur_radius, command.shadow_bounding_rect,
                    command.text_rect, command.glyph_run, command.color,
                    command.fragment_baseline, command.draw_location);
            },
            [&](FillRectWithRoundedCorners const& command) {
                return executor.fill_rect_with_rounded_corners(command.rect, command.color,
                    command.top_left_radius,
                    command.top_right_radius,
                    command.bottom_left_radius,
                    command.bottom_right_radius,
                    command.clip_paths);
            },
            [&](FillPathUsingColor const& command) {
                return executor.fill_path_using_color(command.path, command.color, command.winding_rule,
                    command.aa_translation);
            },
            [&](FillPathUsingPaintStyle const& command) {
                return executor.fill_path_using_paint_style(command.path, command.paint_style,
                    command.winding_rule, command.opacity,
                    command.aa_translation);
            },
            [&](StrokePathUsingColor const& command) {
                return executor.stroke_path_using_color(command.path, command.color, command.thickness,
                    command.aa_translation);
            },
            [&](StrokePathUsingPaintStyle const& command) {
                return executor.stroke_path_using_paint_style(command.path, command.paint_style,
                    command.thickness, command.opacity,
                    command.aa_translation);
            },
            [&](DrawEllipse const& command) {
                return executor.draw_ellipse(command.rect, command.color, command.thickness);
            },
            [&](FillEllipse const& command) {
                return executor.fill_ellipse(command.rect, command.color, command.blend_mode);
            },
            [&](DrawLine const& command) {
                return executor.draw_line(command.color, command.from, command.to, command.thickness,
                    command.style, command.alternate_color);
            },
            [&](DrawSignedDistanceField const& command) {
                return executor.draw_signed_distance_field(command.rect, command.color, command.sdf,
                    command.smoothing);
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
                return executor.draw_triangle_wave(command.p1, command.p2, command.color, command.amplitude,
                    command.thickness);
            },
            [&](SampleUnderCorners const& command) {
                return executor.sample_under_corners(command.id, command.corner_radii, command.border_rect,
                    command.corner_clip);
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
            while (next_command_index < m_commands.size()) {
                if (m_commands[next_command_index].command.has<PushStackingContext>()) {
                    stacking_context_nesting_level++;
                } else if (m_commands[next_command_index].command.has<PopStackingContext>()) {
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
