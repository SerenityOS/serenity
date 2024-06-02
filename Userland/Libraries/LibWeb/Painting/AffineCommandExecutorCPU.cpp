/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/AffineCommandExecutorCPU.h>

namespace Web::Painting {

// This executor is hopes to handle (at least) 2D CSS transforms. All commands
// implemented here are required to support affine transformations, if that is
// not possible the implementation should say in CommandExecutorCPU. Note: The
// transform can be assumed to be non-identity or translation, so there's no
// need to add fast paths here (those will be handled in the normal executor).

bool AffineCommandExecutorCPU::needs_expensive_clipping(Gfx::IntRect bounding_rect) const
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

void AffineCommandExecutorCPU::prepare_clipping(Gfx::IntRect bounding_rect)
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
    m_painter = Gfx::Painter(*m_expensive_clipping_mask);
    m_painter.translate(-clip_bounds.top_left());
    Gfx::Path clip_path;
    clip_path.quad(current_stacking_context.clip.quad);
    aa_painter().fill_path(clip_path, Gfx::Color::Black, Gfx::Painter::WindingRule::EvenOdd);

    // Prepare painter:
    m_painter = Gfx::Painter(*m_expensive_clipping_target);
    m_painter.translate(-clip_bounds.top_left());
}

void AffineCommandExecutorCPU::flush_clipping()
{
    if (!m_expensive_clipping_target)
        return;
    auto& current_stacking_context = stacking_context();
    m_painter = Gfx::Painter(*current_stacking_context.target);
    m_painter.translate(-current_stacking_context.origin);
    m_expensive_clipping_target->apply_mask(*m_expensive_clipping_mask, Gfx::Bitmap::MaskKind::Alpha);
    m_painter.blit(current_stacking_context.clip.bounds.top_left(), *m_expensive_clipping_target, m_expensive_clipping_target->rect());
    m_expensive_clipping_target = nullptr;
    m_expensive_clipping_mask = nullptr;
    m_painter.add_clip_rect(current_stacking_context.clip.bounds);
}

AffineCommandExecutorCPU::AffineCommandExecutorCPU(Gfx::Bitmap& bitmap, Gfx::AffineTransform transform, Gfx::IntRect clip)
    : m_painter(bitmap)
{
    m_painter.add_clip_rect(clip);
    m_stacking_contexts.append(StackingContext {
        .transform = transform,
        .clip = Clip {
            .quad = Gfx::AffineTransform {}.map_to_quad(clip.to_type<float>()),
            .bounds = clip,
            .is_rectangular = true },
        .target = bitmap });
}

CommandResult AffineCommandExecutorCPU::draw_glyph_run(DrawGlyphRun const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_text(DrawText const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::fill_rect(FillRect const& command)
{
    prepare_clipping(command.bounding_rect());
    // FIXME: Support clip_paths.
    Gfx::Path path;
    path.rect(command.rect.to_type<float>());
    aa_painter().fill_path(path.copy_transformed(stacking_context().transform), command.color, Gfx::Painter::WindingRule::EvenOdd);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_scaled_bitmap(DrawScaledBitmap const& command)
{
    prepare_clipping(command.bounding_rect());
    m_painter.draw_scaled_bitmap_with_transform(command.dst_rect, command.bitmap, command.src_rect.to_type<float>(), stacking_context().transform, 1.0f, command.scaling_mode);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_scaled_immutable_bitmap(DrawScaledImmutableBitmap const& command)
{
    prepare_clipping(command.bounding_rect());
    m_painter.draw_scaled_bitmap_with_transform(command.dst_rect, command.bitmap->bitmap(), command.src_rect.to_type<float>(), stacking_context().transform, 1.0f, command.scaling_mode);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::set_clip_rect(SetClipRect const& clip)
{
    flush_clipping();
    auto& current_stacking_context = stacking_context();
    m_painter.clear_clip_rect();
    auto clip_quad = current_stacking_context.transform.map_to_quad(clip.rect.to_type<float>());
    current_stacking_context.clip.bounds = enclosing_int_rect(clip_quad.bounding_rect());
    // FIXME: Flips and rotations by x*90° should also be marked as rectangular.
    current_stacking_context.clip.is_rectangular = current_stacking_context.transform.is_identity_or_translation_or_scale();
    current_stacking_context.clip.quad = clip_quad;
    m_painter.add_clip_rect(current_stacking_context.clip.bounds);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::clear_clip_rect(ClearClipRect const&)
{
    flush_clipping();
    auto& current_stacking_context = stacking_context();
    m_painter.clear_clip_rect();
    current_stacking_context.clip.bounds = m_painter.target()->rect();
    current_stacking_context.clip.quad = Gfx::AffineTransform {}.map_to_quad(current_stacking_context.clip.bounds.to_type<float>());
    current_stacking_context.clip.is_rectangular = true;
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::push_stacking_context(PushStackingContext const& command)
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
                              .intersected(current_stacking_context.target->rect().translated(current_stacking_context.origin));
        if (paint_rect.is_empty())
            return CommandResult::SkipStackingContext;
        auto new_target = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, paint_rect.size()).release_value_but_fixme_should_propagate_errors();
        new_stacking_context.target = new_target;
        new_stacking_context.origin = paint_rect.top_left();
        m_painter = Gfx::Painter(new_target);
        m_painter.translate(-new_stacking_context.origin);
    }

    m_stacking_contexts.append(new_stacking_context);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::pop_stacking_context(PopStackingContext const&)
{
    auto active_stacking_contexts = m_stacking_contexts.size() - 1;
    bool last_stacking_context = active_stacking_contexts <= 1;
    auto prev_stacking_context = stacking_context();
    bool need_to_flush_clipping = last_stacking_context
        || stacking_context().clip != m_stacking_contexts[active_stacking_contexts - 1].clip
        || prev_stacking_context.opacity < 1.0f;
    if (need_to_flush_clipping)
        flush_clipping();
    m_stacking_contexts.take_last();
    if (need_to_flush_clipping) {
        m_painter.clear_clip_rect();
        m_painter.add_clip_rect(stacking_context().clip.bounds);
    }
    if (prev_stacking_context.opacity < 1.0f) {
        auto& current_stacking_context = stacking_context();
        m_painter = Gfx::Painter(current_stacking_context.target);
        m_painter.translate(-current_stacking_context.origin);
        auto stacking_context_rect = prev_stacking_context.target->rect().translated(prev_stacking_context.origin);
        prepare_clipping(stacking_context_rect);
        m_painter.blit(prev_stacking_context.origin, *prev_stacking_context.target, prev_stacking_context.target->rect(), prev_stacking_context.opacity);
    }
    return last_stacking_context ? CommandResult::ContinueWithParentExecutor : CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_linear_gradient(PaintLinearGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_outer_box_shadow(PaintOuterBoxShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_inner_box_shadow(PaintInnerBoxShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_text_shadow(PaintTextShadow const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::fill_rect_with_rounded_corners(FillRectWithRoundedCorners const& command)
{
    prepare_clipping(command.bounding_rect());
    Gfx::Path path;
    path.rounded_rect(command.rect.to_type<float>(), command.top_left_radius, command.top_right_radius, command.bottom_right_radius, command.bottom_left_radius);
    path = path.copy_transformed(stacking_context().transform);
    aa_painter().fill_path(path, command.color, Gfx::Painter::WindingRule::EvenOdd);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::fill_path_using_color(FillPathUsingColor const& command)
{
    prepare_clipping(command.bounding_rect());
    auto path_transform = Gfx::AffineTransform(stacking_context().transform).multiply(Gfx::AffineTransform {}.set_translation(command.aa_translation));
    aa_painter().fill_path(command.path.copy_transformed(path_transform), command.color, command.winding_rule);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::fill_path_using_paint_style(FillPathUsingPaintStyle const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::stroke_path_using_color(StrokePathUsingColor const& command)
{
    prepare_clipping(command.bounding_rect());
    auto path_transform = Gfx::AffineTransform(stacking_context().transform).multiply(Gfx::AffineTransform {}.set_translation(command.aa_translation));
    aa_painter().stroke_path(command.path.copy_transformed(path_transform), command.color, command.thickness);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::stroke_path_using_paint_style(StrokePathUsingPaintStyle const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_ellipse(DrawEllipse const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::fill_ellipse(FillEllipse const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_line(DrawLine const& command)
{
    prepare_clipping(Gfx::IntRect::from_two_points(command.from, command.to).inflated(command.thickness, command.thickness));
    // FIXME: Implement other line styles.
    Gfx::Path path;
    path.move_to(command.from.to_type<float>());
    path.line_to(command.to.to_type<float>());
    aa_painter().stroke_path(path, command.color, command.thickness);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_signed_distance_field(DrawSignedDistanceField const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::apply_backdrop_filter(ApplyBackdropFilter const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_rect(DrawRect const& command)
{
    prepare_clipping(command.bounding_rect());
    Gfx::Path path;
    path.rect(command.rect.to_type<float>());
    aa_painter().stroke_path(path.copy_transformed(stacking_context().transform), command.color, 1);
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_radial_gradient(PaintRadialGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::paint_conic_gradient(PaintConicGradient const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::draw_triangle_wave(DrawTriangleWave const&)
{
    // FIXME: Implement.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::sample_under_corners(SampleUnderCorners const&)
{
    // FIXME: Implement? -- Likely not a good approach for transforms.
    return CommandResult::Continue;
}

CommandResult AffineCommandExecutorCPU::blit_corner_clipping(BlitCornerClipping const&)
{
    // FIXME: Implement? -- Likely not a good approach for transforms.
    return CommandResult::Continue;
}

bool AffineCommandExecutorCPU::would_be_fully_clipped_by_painter(Gfx::IntRect rect) const
{
    auto const& current_stacking_context = stacking_context();
    auto transformed_rect = current_stacking_context.transform.map(rect.to_type<float>()).to_type<int>();
    return transformed_rect.intersected(current_stacking_context.clip.bounds).is_empty();
}

}
