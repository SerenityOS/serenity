/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibWeb/Painting/AffineDisplayListPlayerCPU.h>
namespace Web::Painting {

// This executor is hopes to handle (at least) 2D CSS transforms. All commands
// implemented here are required to support affine transformations, if that is
// not possible the implementation should say in CommandExecutorCPU. Note: The
// transform can be assumed to be non-identity or translation, so there's no
// need to add fast paths here (those will be handled in the normal executor).

bool AffineDisplayListPlayerCPU::needs_expensive_clipping(Gfx::IntRect bounding_rect) const
{
    auto& current_stacking_context = stacking_context();
    if (current_stacking_context.clip.is_rectangular)
        return false;
    auto dest = current_stacking_context.transform.map_to_quad(bounding_rect.to_type<float>());
    for (auto point : { dest.p1(), dest.p2(), dest.p3(), dest.p4() }) {
        if (!current_stacking_context.clip.quad.contains(point))
            return true;
    }
    return false;
}

void AffineDisplayListPlayerCPU::prepare_clipping(Gfx::IntRect bounding_rect)
{
    if (m_expensive_clipping_target)
        return;
    if (!needs_expensive_clipping(bounding_rect))
        return;
    auto& current_stacking_context = stacking_context();
    auto clip_bounds = current_stacking_context.clip.bounds;
    if (clip_bounds.is_empty())
        return;
    m_expensive_clipping_target = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, clip_bounds.size()).release_value_but_fixme_should_propagate_errors();
    m_expensive_clipping_mask = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, clip_bounds.size()).release_value_but_fixme_should_propagate_errors();

    // Prepare clip mask:
    set_target(clip_bounds.top_left(), *m_expensive_clipping_mask);
    Gfx::Path clip_path;
    clip_path.quad(current_stacking_context.clip.quad);
    aa_painter().fill_path(clip_path, Gfx::Color::Black, Gfx::WindingRule::EvenOdd);

    // Prepare painter:
    set_target(clip_bounds.top_left(), *m_expensive_clipping_target);
}

void AffineDisplayListPlayerCPU::flush_clipping(Optional<StackingContext const&> current_stacking_context)
{
    if (!m_expensive_clipping_target)
        return;
    if (!current_stacking_context.has_value())
        current_stacking_context = stacking_context();
    set_target(current_stacking_context->origin, *current_stacking_context->target);
    m_expensive_clipping_target->apply_mask(*m_expensive_clipping_mask, Gfx::Bitmap::MaskKind::Alpha);
    painter().blit(current_stacking_context->clip.bounds.top_left(), *m_expensive_clipping_target, m_expensive_clipping_target->rect());
    painter().add_clip_rect(current_stacking_context->clip.bounds);
    m_expensive_clipping_target = nullptr;
    m_expensive_clipping_mask = nullptr;
}

AffineDisplayListPlayerCPU::AffineDisplayListPlayerCPU(Gfx::Bitmap& bitmap, Gfx::AffineTransform transform, Gfx::IntRect clip)
    : m_painter(bitmap)
{
    painter().add_clip_rect(clip);
    m_stacking_contexts.append(StackingContext {
        .transform = transform,
        .clip = Clip {
            .quad = Gfx::AffineTransform {}.map_to_quad(clip.to_type<float>()),
            .bounds = clip,
            .is_rectangular = true },
        .target = bitmap });
}

CommandResult AffineDisplayListPlayerCPU::draw_glyph_run(DrawGlyphRun const& command)
{
    prepare_clipping(command.bounding_rect());
    auto scale = Gfx::AffineTransform {}.scale(command.scale, command.scale);
    auto const& glyphs = command.glyph_run->glyphs();
    Gfx::Path path;
    auto const& font = command.glyph_run->font();
    for (auto const& glyph_or_emoji : glyphs) {
        if (auto* glyph = glyph_or_emoji.get_pointer<Gfx::DrawGlyph>()) {
            if (!is<Gfx::ScaledFont>(font))
                return CommandResult::Continue;
            auto& scaled_font = static_cast<Gfx::ScaledFont const&>(font);
            auto position = glyph->position.translated(scaled_font.glyph_left_bearing(glyph->code_point), 0);
            auto glyph_id = scaled_font.glyph_id_for_code_point(glyph->code_point);
            Gfx::Path glyph_path;
            scaled_font.append_glyph_path_to(glyph_path, glyph_id);
            glyph_path.transform(Gfx::AffineTransform(scale).translate(position));
            path.append_path(glyph_path);
        } else {
            // TODO: Implement bitmap emojis via transformed bitmaps.
        }
    }
    auto path_transform = Gfx::AffineTransform(stacking_context().transform).multiply(Gfx::AffineTransform {}.set_translation(command.translation));
    path.transform(path_transform);
    aa_painter().fill_path(path, command.color);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::fill_rect(FillRect const& command)
{
    prepare_clipping(command.bounding_rect());
    // FIXME: Support clip_paths.
    Gfx::Path path;
    path.rect(command.rect.to_type<float>());
    aa_painter().fill_path(path.copy_transformed(stacking_context().transform), command.color, Gfx::WindingRule::EvenOdd);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_scaled_bitmap(DrawScaledBitmap const& command)
{
    prepare_clipping(command.bounding_rect());
    painter().draw_scaled_bitmap_with_transform(command.dst_rect, command.bitmap, command.src_rect.to_type<float>(), stacking_context().transform, 1.0f, command.scaling_mode);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_scaled_immutable_bitmap(DrawScaledImmutableBitmap const& command)
{
    prepare_clipping(command.bounding_rect());
    painter().draw_scaled_bitmap_with_transform(command.dst_rect, command.bitmap->bitmap(), command.src_rect.to_type<float>(), stacking_context().transform, 1.0f, command.scaling_mode);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::set_clip_rect(SetClipRect const& clip)
{
    flush_clipping();
    auto& current_stacking_context = stacking_context();
    painter().clear_clip_rect();
    auto clip_quad = current_stacking_context.transform.map_to_quad(clip.rect.to_type<float>());
    current_stacking_context.clip.bounds = enclosing_int_rect(clip_quad.bounding_rect());
    // FIXME: Flips and rotations by x*90° should also be marked as rectangular.
    current_stacking_context.clip.is_rectangular = current_stacking_context.transform.is_identity_or_translation_or_scale(Gfx::AffineTransform::AllowNegativeScaling::Yes);
    current_stacking_context.clip.quad = clip_quad;
    painter().add_clip_rect(current_stacking_context.clip.bounds);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::clear_clip_rect(ClearClipRect const&)
{
    flush_clipping();
    auto& current_stacking_context = stacking_context();
    painter().clear_clip_rect();
    current_stacking_context.clip.bounds = current_stacking_context.rect();
    current_stacking_context.clip.quad = Gfx::AffineTransform {}.map_to_quad(current_stacking_context.clip.bounds.to_type<float>());
    current_stacking_context.clip.is_rectangular = true;
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::push_stacking_context(PushStackingContext const& command)
{
    // FIXME: Support masks (not possible to do while PushStackingContext takes a bitmap mask).
    // Note: Image rendering is not relevant as this does not transform via a bitmap.
    // Note: `position: fixed` does not apply when CSS transforms are involved.
    if (command.opacity == 0.0f)
        return CommandResult::SkipStackingContext;

    // FIXME: Attempt to support 3D transforms... Somehow?
    auto affine_transform = Gfx::extract_2d_affine_transform(command.transform.matrix);
    auto new_transform = Gfx::AffineTransform {}
                             .set_translation(command.post_transform_translation.to_type<float>())
                             .translate(command.transform.origin)
                             .multiply(affine_transform)
                             .translate(-command.transform.origin);

    auto const& current_stacking_context = stacking_context();

    StackingContext new_stacking_context {
        .transform = Gfx::AffineTransform(current_stacking_context.transform).multiply(new_transform),
        .clip = current_stacking_context.clip,
        .target = current_stacking_context.target,
        .origin = current_stacking_context.origin,
        .opacity = command.opacity
    };

    if (command.opacity < 1.0f) {
        flush_clipping();
        auto paint_rect = enclosing_int_rect(new_stacking_context.transform.map(command.source_paintable_rect.to_type<float>()))
                              .intersected(current_stacking_context.rect());
        if (paint_rect.is_empty())
            return CommandResult::SkipStackingContext;
        auto new_target = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, paint_rect.size()).release_value_but_fixme_should_propagate_errors();
        new_stacking_context.target = new_target;
        new_stacking_context.origin = paint_rect.top_left();
        set_target(new_stacking_context.origin, *new_target);
    }

    m_stacking_contexts.append(new_stacking_context);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::pop_stacking_context(PopStackingContext const&)
{
    auto active_stacking_contexts = m_stacking_contexts.size() - 1;
    bool is_final_stacking_context = active_stacking_contexts <= 1;
    auto prev_stacking_context = m_stacking_contexts.take_last();
    auto& current_stacking_context = stacking_context();
    bool need_to_flush_clipping = is_final_stacking_context
        || prev_stacking_context.clip != current_stacking_context.clip
        || prev_stacking_context.opacity < 1.0f;
    if (need_to_flush_clipping) {
        flush_clipping(prev_stacking_context);
        painter().clear_clip_rect();
        painter().add_clip_rect(stacking_context().clip.bounds);
    }
    if (prev_stacking_context.opacity < 1.0f) {
        set_target(current_stacking_context.origin, *current_stacking_context.target);
        prepare_clipping(prev_stacking_context.rect());
        painter().blit(prev_stacking_context.origin, *prev_stacking_context.target, prev_stacking_context.target->rect(), prev_stacking_context.opacity);
    }
    return is_final_stacking_context ? CommandResult::ContinueWithParentExecutor : CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_linear_gradient(PaintLinearGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_outer_box_shadow(PaintOuterBoxShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_inner_box_shadow(PaintInnerBoxShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_text_shadow(PaintTextShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::fill_rect_with_rounded_corners(FillRectWithRoundedCorners const& command)
{
    prepare_clipping(command.bounding_rect());
    Gfx::Path path;
    path.rounded_rect(command.rect.to_type<float>(), command.top_left_radius, command.top_right_radius, command.bottom_right_radius, command.bottom_left_radius);
    path = path.copy_transformed(stacking_context().transform);
    aa_painter().fill_path(path, command.color, Gfx::WindingRule::EvenOdd);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::fill_path_using_color(FillPathUsingColor const& command)
{
    prepare_clipping(command.bounding_rect());
    auto path_transform = Gfx::AffineTransform(stacking_context().transform).multiply(Gfx::AffineTransform {}.set_translation(command.aa_translation));
    aa_painter().fill_path(command.path.copy_transformed(path_transform), command.color, command.winding_rule);
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::fill_path_using_paint_style(FillPathUsingPaintStyle const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::stroke_path_using_color(StrokePathUsingColor const& command)
{
    prepare_clipping(command.bounding_rect());
    auto path_transform = Gfx::AffineTransform(stacking_context().transform).multiply(Gfx::AffineTransform {}.set_translation(command.aa_translation));
    // FIXME: Pass command.cap_style, command.join_style, command.miter_limit here!
    aa_painter().stroke_path(command.path.copy_transformed(path_transform), command.color, { command.thickness });
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::stroke_path_using_paint_style(StrokePathUsingPaintStyle const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_ellipse(DrawEllipse const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::fill_ellipse(FillEllipse const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_line(DrawLine const& command)
{
    prepare_clipping(Gfx::IntRect::from_two_points(command.from, command.to).inflated(command.thickness, command.thickness));
    // FIXME: Implement other line styles.
    Gfx::Path path;
    path.move_to(command.from.to_type<float>());
    path.line_to(command.to.to_type<float>());
    // FIXME: Probably want to use butt linecaps here?
    aa_painter().stroke_path(path, command.color, { static_cast<float>(command.thickness) });
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::apply_backdrop_filter(ApplyBackdropFilter const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_rect(DrawRect const& command)
{
    prepare_clipping(command.bounding_rect());
    Gfx::Path path;
    path.rect(command.rect.to_type<float>());
    // FIXME: Probably want to use miter linejoins here?
    aa_painter().stroke_path(path.copy_transformed(stacking_context().transform), command.color, { 1 });
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_radial_gradient(PaintRadialGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::paint_conic_gradient(PaintConicGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::draw_triangle_wave(DrawTriangleWave const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::sample_under_corners(SampleUnderCorners const&)
{
    // FIXME: Implement? -- Likely not a good approach for transforms.
    return CommandResult::Continue;
}

CommandResult AffineDisplayListPlayerCPU::blit_corner_clipping(BlitCornerClipping const&)
{
    // FIXME: Implement? -- Likely not a good approach for transforms.
    return CommandResult::Continue;
}

bool AffineDisplayListPlayerCPU::would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
{
    auto const& current_stacking_context = stacking_context();
    auto transformed_rect = current_stacking_context.transform.map(rect.to_type<float>()).to_type<int>();
    return transformed_rect.intersected(current_stacking_context.clip.bounds).is_empty();
}

}
