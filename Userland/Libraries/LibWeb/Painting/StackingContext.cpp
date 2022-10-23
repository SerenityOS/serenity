/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/ExtraMathConstants.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Painting {

static void paint_node(Layout::Node const& layout_node, PaintContext& context, PaintPhase phase)
{
    if (auto const* paintable = layout_node.paintable())
        paintable->paint(context, phase);
}

StackingContext::StackingContext(Layout::Box& box, StackingContext* parent)
    : m_box(box)
    , m_transform(combine_transformations(m_box.computed_values().transformations()))
    , m_transform_origin(compute_transform_origin())
    , m_parent(parent)
{
    VERIFY(m_parent != this);
    if (m_parent)
        m_parent->m_children.append(this);
}

void StackingContext::sort()
{
    quick_sort(m_children, [](auto& a, auto& b) {
        auto a_z_index = a->m_box.computed_values().z_index().value_or(0);
        auto b_z_index = b->m_box.computed_values().z_index().value_or(0);
        if (a_z_index == b_z_index)
            return a->m_box.is_before(b->m_box);
        return a_z_index < b_z_index;
    });

    for (auto* child : m_children)
        child->sort();
}

static PaintPhase to_paint_phase(StackingContext::StackingContextPaintPhase phase)
{
    // There are not a fully correct mapping since some stacking context phases are combined.
    switch (phase) {
    case StackingContext::StackingContextPaintPhase::Floats:
    case StackingContext::StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced:
    case StackingContext::StackingContextPaintPhase::BackgroundAndBorders:
        return PaintPhase::Background;
    case StackingContext::StackingContextPaintPhase::Foreground:
        return PaintPhase::Foreground;
    case StackingContext::StackingContextPaintPhase::FocusAndOverlay:
        return PaintPhase::Overlay;
    default:
        VERIFY_NOT_REACHED();
    }
}

void StackingContext::paint_descendants(PaintContext& context, Layout::Node const& box, StackingContextPaintPhase phase) const
{
    if (auto* paintable = box.paintable())
        paintable->before_children_paint(context, to_paint_phase(phase), Paintable::ShouldClipOverflow::Yes);

    box.for_each_child([&](auto& child) {
        // If `child` establishes its own stacking context, skip over it.
        if (is<Layout::Box>(child) && child.paintable() && static_cast<Layout::Box const&>(child).paint_box()->stacking_context())
            return;
        bool child_is_inline_or_replaced = child.is_inline() || is<Layout::ReplacedBox>(child);
        switch (phase) {
        case StackingContextPaintPhase::BackgroundAndBorders:
            if (!child_is_inline_or_replaced && !child.is_floating()) {
                paint_node(child, context, PaintPhase::Background);
                paint_node(child, context, PaintPhase::Border);
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::Floats:
            if (child.is_floating()) {
                paint_node(child, context, PaintPhase::Background);
                paint_node(child, context, PaintPhase::Border);
                paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
            }
            paint_descendants(context, child, phase);
            break;
        case StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced:
            if (child_is_inline_or_replaced) {
                paint_node(child, context, PaintPhase::Background);
                paint_node(child, context, PaintPhase::Border);
                paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
            }
            paint_descendants(context, child, phase);
            break;
        case StackingContextPaintPhase::Foreground:
            paint_node(child, context, PaintPhase::Foreground);
            paint_descendants(context, child, phase);
            break;
        case StackingContextPaintPhase::FocusAndOverlay:
            if (context.has_focus()) {
                paint_node(child, context, PaintPhase::FocusOutline);
            }
            paint_node(child, context, PaintPhase::Overlay);
            paint_descendants(context, child, phase);
            break;
        }
    });

    if (auto* paintable = box.paintable())
        paintable->after_children_paint(context, to_paint_phase(phase), Paintable::ShouldClipOverflow::Yes);
}

void StackingContext::paint_internal(PaintContext& context) const
{
    // For a more elaborate description of the algorithm, see CSS 2.1 Appendix E
    // Draw the background and borders for the context root (steps 1, 2)
    paint_node(m_box, context, PaintPhase::Background);
    paint_node(m_box, context, PaintPhase::Border);

    auto paint_child = [&](auto* child) {
        auto parent = child->m_box.parent();
        auto should_clip_overflow = child->m_box.is_absolutely_positioned() ? Paintable::ShouldClipOverflow::No : Paintable::ShouldClipOverflow::Yes;
        auto* paintable = parent ? parent->paintable() : nullptr;
        if (paintable)
            paintable->before_children_paint(context, PaintPhase::Foreground, should_clip_overflow);
        child->paint(context);
        if (paintable)
            paintable->after_children_paint(context, PaintPhase::Foreground, should_clip_overflow);
    };

    // Draw positioned descendants with negative z-indices (step 3)
    for (auto* child : m_children) {
        if (child->m_box.computed_values().z_index().has_value() && child->m_box.computed_values().z_index().value() < 0)
            paint_child(child);
    }

    // Draw the background and borders for block-level children (step 4)
    paint_descendants(context, m_box, StackingContextPaintPhase::BackgroundAndBorders);
    // Draw the non-positioned floats (step 5)
    paint_descendants(context, m_box, StackingContextPaintPhase::Floats);
    // Draw inline content, replaced content, etc. (steps 6, 7)
    paint_descendants(context, m_box, StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced);
    paint_node(m_box, context, PaintPhase::Foreground);
    paint_descendants(context, m_box, StackingContextPaintPhase::Foreground);

    // Draw positioned descendants with z-index `0` or `auto` in tree order. (step 8)
    // FIXME: There's more to this step that we have yet to understand and implement.
    m_box.paint_box()->for_each_in_subtree_of_type<PaintableBox>([&](PaintableBox const& paint_box) {
        if (!paint_box.layout_box().is_positioned())
            return TraversalDecision::Continue;
        auto const& z_index = paint_box.computed_values().z_index();
        if (z_index.has_value() && z_index.value() != 0)
            return TraversalDecision::Continue;
        if (auto* child = paint_box.stacking_context()) {
            paint_child(child);
            return TraversalDecision::SkipChildrenAndContinue;
        }
        // At this point, `paint_box` is a positioned descendant with z-index: auto
        // but no stacking context of its own.
        // FIXME: This is basically duplicating logic found elsewhere in this same function. Find a way to make this more elegant.
        paint_node(paint_box.layout_box(), context, PaintPhase::Background);
        paint_node(paint_box.layout_box(), context, PaintPhase::Border);
        paint_descendants(context, paint_box.layout_box(), StackingContextPaintPhase::BackgroundAndBorders);
        paint_descendants(context, paint_box.layout_box(), StackingContextPaintPhase::Floats);
        paint_descendants(context, paint_box.layout_box(), StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced);
        paint_node(paint_box.layout_box(), context, PaintPhase::Foreground);
        paint_descendants(context, paint_box.layout_box(), StackingContextPaintPhase::Foreground);
        paint_node(paint_box.layout_box(), context, PaintPhase::FocusOutline);
        paint_node(paint_box.layout_box(), context, PaintPhase::Overlay);
        paint_descendants(context, paint_box.layout_box(), StackingContextPaintPhase::FocusAndOverlay);

        return TraversalDecision::Continue;
    });

    // Draw other positioned descendants (step 9)
    for (auto* child : m_children) {
        if (child->m_box.computed_values().z_index().has_value() && child->m_box.computed_values().z_index().value() >= 1)
            paint_child(child);
    }

    paint_node(m_box, context, PaintPhase::FocusOutline);
    paint_node(m_box, context, PaintPhase::Overlay);
    paint_descendants(context, m_box, StackingContextPaintPhase::FocusAndOverlay);
}

Gfx::FloatMatrix4x4 StackingContext::get_transformation_matrix(CSS::Transformation const& transformation) const
{
    auto count = transformation.values.size();
    auto value = [this, transformation](size_t index, Optional<CSS::Length const&> reference_length = {}) -> float {
        return transformation.values[index].visit(
            [this, reference_length](CSS::LengthPercentage const& value) {
                return value.resolved(m_box, reference_length.value()).to_px(m_box);
            },
            [](CSS::Angle const& value) {
                return value.to_degrees() * static_cast<float>(M_DEG2RAD);
            },
            [](float value) {
                return value;
            });
    };

    auto reference_box = paintable().absolute_rect();
    auto width = CSS::Length::make_px(reference_box.width());
    auto height = CSS::Length::make_px(reference_box.height());

    switch (transformation.function) {
    case CSS::TransformFunction::Matrix:
        if (count == 6)
            return Gfx::FloatMatrix4x4(value(0), value(2), 0, value(4),
                value(1), value(3), 0, value(5),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Matrix3d:
        if (count == 16)
            return Gfx::FloatMatrix4x4(value(0), value(4), value(8), value(12),
                value(1), value(5), value(9), value(13),
                value(2), value(6), value(10), value(14),
                value(3), value(7), value(11), value(15));
        break;
    case CSS::TransformFunction::Translate:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, value(1, height),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, 1, 0, value(0, height),
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Scale:
        if (count == 1)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, value(0), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, value(1), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(value(0), 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, value(0), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::RotateX:
        if (count == 1)
            return Gfx::rotation_matrix({ 1.0f, 0.0f, 0.0f }, value(0));
        break;
    case CSS::TransformFunction::RotateY:
        if (count == 1)
            return Gfx::rotation_matrix({ 0.0f, 1.0f, 0.0f }, value(0));
        break;
    case CSS::TransformFunction::Rotate:
    case CSS::TransformFunction::RotateZ:
        if (count == 1)
            return Gfx::rotation_matrix({ 0.0f, 0.0f, 1.0f }, value(0));
        break;
    default:
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Unhandled transformation function {}", CSS::TransformationStyleValue::create(transformation.function, {})->to_string());
    }
    return Gfx::FloatMatrix4x4::identity();
}

Gfx::FloatMatrix4x4 StackingContext::combine_transformations(Vector<CSS::Transformation> const& transformations) const
{
    auto matrix = Gfx::FloatMatrix4x4::identity();

    for (auto const& transform : transformations)
        matrix = matrix * get_transformation_matrix(transform);

    return matrix;
}

// FIXME: This extracts the affine 2D part of the full transformation matrix.
//  Use the whole matrix when we get better transformation support in LibGfx or use LibGL for drawing the bitmap
Gfx::AffineTransform StackingContext::affine_transform_matrix() const
{
    auto* m = m_transform.elements();
    return Gfx::AffineTransform(m[0][0], m[1][0], m[0][1], m[1][1], m[0][3], m[1][3]);
}

void StackingContext::paint(PaintContext& context) const
{
    Gfx::PainterStateSaver saver(context.painter());
    if (m_box.is_fixed_position()) {
        context.painter().translate(-context.painter().translation());
    }

    auto opacity = m_box.computed_values().opacity();
    if (opacity == 0.0f)
        return;

    auto affine_transform = affine_transform_matrix();

    if (opacity < 1.0f || !affine_transform.is_identity_or_translation()) {
        auto transform_origin = this->transform_origin();
        auto source_rect = paintable().absolute_paint_rect().translated(-transform_origin);
        auto transformed_destination_rect = affine_transform.map(source_rect).translated(transform_origin);
        auto destination_rect = transformed_destination_rect.to_rounded<int>();

        // FIXME: We should find a way to scale the paintable, rather than paint into a separate bitmap,
        // then scale it. This snippet now copies the background at the destination, then scales it down/up
        // to the size of the source (which could add some artefacts, though just scaling the bitmap already does that).
        // We need to copy the background at the destination because a bunch of our rendering effects now rely on
        // being able to sample the painter (see border radii, shadows, filters, etc).
        Gfx::FloatPoint destination_clipped_fixup {};
        auto try_get_scaled_destination_bitmap = [&]() -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
            Gfx::IntRect actual_destination_rect;
            auto bitmap = TRY(context.painter().get_region_bitmap(destination_rect, Gfx::BitmapFormat::BGRA8888, actual_destination_rect));
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
        if (bitmap_or_error.is_error())
            return;
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(bitmap);
        painter.translate((-paintable().absolute_paint_rect().location() + destination_clipped_fixup).to_rounded<int>());
        auto paint_context = context.clone(painter);
        paint_internal(paint_context);

        if (destination_rect.size() == bitmap->size())
            context.painter().blit(destination_rect.location(), *bitmap, bitmap->rect(), opacity);
        else
            context.painter().draw_scaled_bitmap(destination_rect, *bitmap, bitmap->rect(), opacity, Gfx::Painter::ScalingMode::BilinearBlend);
    } else {
        Gfx::PainterStateSaver saver(context.painter());
        context.painter().translate(affine_transform.translation().to_rounded<int>());
        paint_internal(context);
    }
}

Gfx::FloatPoint StackingContext::compute_transform_origin() const
{
    auto style_value = m_box.computed_values().transform_origin();
    // FIXME: respect transform-box property
    auto reference_box = paintable().absolute_border_box_rect();
    auto x = reference_box.left() + style_value.x.resolved(m_box, CSS::Length::make_px(reference_box.width())).to_px(m_box);
    auto y = reference_box.top() + style_value.y.resolved(m_box, CSS::Length::make_px(reference_box.height())).to_px(m_box);
    return { x, y };
}

Optional<HitTestResult> StackingContext::hit_test(Gfx::FloatPoint const& position, HitTestType type) const
{
    if (!m_box.is_visible())
        return {};

    auto transform_origin = this->transform_origin();
    auto transformed_position = affine_transform_matrix().inverse().value_or({}).map(position - transform_origin) + transform_origin;

    // FIXME: Support more overflow variations.
    if (paintable().computed_values().overflow_x() == CSS::Overflow::Hidden && paintable().computed_values().overflow_y() == CSS::Overflow::Hidden) {
        if (!paintable().absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
            return {};
    }

    // NOTE: Hit testing basically happens in reverse painting order.
    // https://www.w3.org/TR/CSS22/visuren.html#z-index

    // 7. the child stacking contexts with positive stack levels (least positive first).
    // NOTE: Hit testing follows reverse painting order, that's why the conditions here are reversed.
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) < 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    Optional<HitTestResult> result;
    // 6. the child stacking contexts with stack level 0 and the positioned descendants with stack level 0.
    paintable().for_each_in_subtree_of_type<PaintableBox>([&](auto& paint_box) {
        // FIXME: Support more overflow variations.
        if (paint_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paint_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
            if (!paint_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                return TraversalDecision::SkipChildrenAndContinue;
        }

        auto& layout_box = paint_box.layout_box();
        if (layout_box.is_positioned() && !paint_box.stacking_context()) {
            if (auto candidate = paint_box.hit_test(transformed_position, type); candidate.has_value())
                result = move(candidate);
        }
        return TraversalDecision::Continue;
    });
    if (result.has_value() && result->paintable->visible_for_hit_testing())
        return result;

    // 5. the in-flow, inline-level, non-positioned descendants, including inline tables and inline blocks.
    if (m_box.children_are_inline() && is<Layout::BlockContainer>(m_box)) {
        auto result = paintable().hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 4. the non-positioned floats.
    paintable().for_each_in_subtree_of_type<PaintableBox>([&](auto const& paint_box) {
        // FIXME: Support more overflow variations.
        if (paint_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paint_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
            if (!paint_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                return TraversalDecision::SkipChildrenAndContinue;
        }

        auto& layout_box = paint_box.layout_box();
        if (layout_box.is_floating()) {
            if (auto candidate = paint_box.hit_test(transformed_position, type); candidate.has_value())
                result = move(candidate);
        }
        return TraversalDecision::Continue;
    });
    if (result.has_value() && result->paintable->visible_for_hit_testing())
        return result;

    // 3. the in-flow, non-inline-level, non-positioned descendants.
    if (!m_box.children_are_inline()) {
        paintable().for_each_in_subtree_of_type<PaintableBox>([&](auto const& paint_box) {
            // FIXME: Support more overflow variations.
            if (paint_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paint_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
                if (!paint_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                    return TraversalDecision::SkipChildrenAndContinue;
            }

            auto& layout_box = paint_box.layout_box();
            if (!layout_box.is_absolutely_positioned() && !layout_box.is_floating()) {
                if (auto candidate = paint_box.hit_test(transformed_position, type); candidate.has_value())
                    result = move(candidate);
            }
            return TraversalDecision::Continue;
        });
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 2. the child stacking contexts with negative stack levels (most negative first).
    // NOTE: Hit testing follows reverse painting order, that's why the conditions here are reversed.
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) >= 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 1. the background and borders of the element forming the stacking context.
    if (paintable().absolute_border_box_rect().contains(transformed_position)) {
        return HitTestResult {
            .paintable = paintable(),
        };
    }

    return {};
}

void StackingContext::dump(int indent) const
{
    StringBuilder builder;
    for (int i = 0; i < indent; ++i)
        builder.append(' ');
    builder.appendff("SC for {} {} [children: {}] (z-index: ", m_box.debug_description(), paintable().absolute_rect(), m_children.size());
    if (m_box.computed_values().z_index().has_value())
        builder.appendff("{}", m_box.computed_values().z_index().value());
    else
        builder.append("auto"sv);
    builder.append(')');

    auto affine_transform = affine_transform_matrix();
    if (!affine_transform.is_identity()) {
        builder.appendff(", transform: {}", affine_transform);
    }
    dbgln("{}", builder.string_view());
    for (auto& child : m_children)
        child->dump(indent + 1);
}

}
