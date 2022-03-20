/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
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

void StackingContext::paint_descendants(PaintContext& context, Layout::Node& box, StackingContextPaintPhase phase) const
{
    if (phase == StackingContextPaintPhase::Foreground) {
        if (auto* paintable = box.paintable())
            paintable->before_children_paint(context, PaintPhase::Foreground);
    }

    box.for_each_child([&](auto& child) {
        // If `child` establishes its own stacking context, skip over it.
        if (is<Layout::Box>(child) && child.paintable() && static_cast<Layout::Box const&>(child).paint_box()->stacking_context())
            return;
        bool child_is_inline_or_replaced = child.is_inline() || is<Layout::ReplacedBox>(child);
        switch (phase) {
        case StackingContextPaintPhase::BackgroundAndBorders:
            if (!child_is_inline_or_replaced && !child.is_floating() && !child.is_positioned()) {
                paint_node(child, context, PaintPhase::Background);
                paint_node(child, context, PaintPhase::Border);
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::Floats:
            if (!child.is_positioned()) {
                if (child.is_floating()) {
                    paint_node(child, context, PaintPhase::Background);
                    paint_node(child, context, PaintPhase::Border);
                    paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
                }
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced:
            if (!child.is_positioned()) {
                if (child_is_inline_or_replaced) {
                    paint_node(child, context, PaintPhase::Background);
                    paint_node(child, context, PaintPhase::Border);
                    paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
                }
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::Foreground:
            if (!child.is_positioned()) {
                paint_node(child, context, PaintPhase::Foreground);
                paint_descendants(context, child, phase);
            }
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

    if (phase == StackingContextPaintPhase::Foreground) {
        if (auto* paintable = box.paintable())
            paintable->after_children_paint(context, PaintPhase::Foreground);
    }
}

void StackingContext::paint_internal(PaintContext& context) const
{
    // For a more elaborate description of the algorithm, see CSS 2.1 Appendix E
    // Draw the background and borders for the context root (steps 1, 2)
    paint_node(m_box, context, PaintPhase::Background);
    paint_node(m_box, context, PaintPhase::Border);
    // Draw positioned descendants with negative z-indices (step 3)
    for (auto* child : m_children) {
        if (child->m_box.computed_values().z_index().has_value() && child->m_box.computed_values().z_index().value() < 0)
            child->paint(context);
    }
    // Draw the background and borders for block-level children (step 4)
    paint_descendants(context, m_box, StackingContextPaintPhase::BackgroundAndBorders);
    // Draw the non-positioned floats (step 5)
    paint_descendants(context, m_box, StackingContextPaintPhase::Floats);
    // Draw inline content, replaced content, etc. (steps 6, 7)
    paint_descendants(context, m_box, StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced);
    paint_node(m_box, context, PaintPhase::Foreground);
    paint_descendants(context, m_box, StackingContextPaintPhase::Foreground);
    // Draw other positioned descendants (steps 8, 9)
    for (auto* child : m_children) {
        if (child->m_box.computed_values().z_index().has_value() && child->m_box.computed_values().z_index().value() < 0)
            continue;
        child->paint(context);
    }

    paint_node(m_box, context, PaintPhase::FocusOutline);
    paint_node(m_box, context, PaintPhase::Overlay);
    paint_descendants(context, m_box, StackingContextPaintPhase::FocusAndOverlay);
}

Gfx::FloatMatrix4x4 StackingContext::get_transformation_matrix(CSS::Transformation const& transformation) const
{
    Vector<float> float_values;
    for (auto const& value : transformation.values) {
        value.visit(
            [&](CSS::Length const& value) {
                float_values.append(value.to_px(m_box));
            },
            [&](float value) {
                float_values.append(value);
            });
    }

    switch (transformation.function) {
    case CSS::TransformFunction::Matrix:
        if (float_values.size() == 6)
            return Gfx::FloatMatrix4x4(float_values[0], float_values[2], 0, float_values[4],
                float_values[1], float_values[3], 0, float_values[5],
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Translate:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, float_values[0],
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (float_values.size() == 2)
            return Gfx::FloatMatrix4x4(1, 0, 0, float_values[0],
                0, 1, 0, float_values[1],
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateX:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, float_values[0],
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::TranslateY:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, 1, 0, float_values[0],
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::Scale:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(float_values[0], 0, 0, 0,
                0, float_values[0], 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (float_values.size() == 2)
            return Gfx::FloatMatrix4x4(float_values[0], 0, 0, 0,
                0, float_values[1], 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleX:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(float_values[0], 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::ScaleY:
        if (float_values.size() == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                0, float_values[0], 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
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
Gfx::AffineTransform StackingContext::combine_transformations_2d(Vector<CSS::Transformation> const& transformations) const
{
    auto matrix = combine_transformations(transformations);
    auto* m = matrix.elements();
    return Gfx::AffineTransform(m[0][0], m[1][0], m[0][1], m[1][1], m[0][3], m[1][3]);
}

void StackingContext::paint(PaintContext& context) const
{
    Gfx::PainterStateSaver saver(context.painter());
    if (m_box.is_fixed_position()) {
        context.painter().translate(context.scroll_offset());
    }

    auto opacity = m_box.computed_values().opacity();
    if (opacity == 0.0f)
        return;

    auto affine_transform = combine_transformations_2d(m_box.computed_values().transformations());

    if (opacity < 1.0f || !affine_transform.is_identity()) {
        auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, context.painter().target()->size());
        if (bitmap_or_error.is_error())
            return;
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(bitmap);
        PaintContext paint_context(painter, context.palette(), context.scroll_offset());
        paint_internal(paint_context);

        // FIXME: Use the transform origin specified in CSS or SVG
        auto transform_origin = m_box.paint_box()->absolute_position();
        auto source_rect = m_box.paint_box()->absolute_border_box_rect().translated(-transform_origin);

        auto transformed_destination_rect = affine_transform.map(source_rect).translated(transform_origin);
        source_rect.translate_by(transform_origin);
        context.painter().draw_scaled_bitmap(Gfx::rounded_int_rect(transformed_destination_rect), *bitmap, source_rect, opacity, Gfx::Painter::ScalingMode::BilinearBlend);
    } else {
        paint_internal(context);
    }
}

HitTestResult StackingContext::hit_test(Gfx::FloatPoint const& position, HitTestType type) const
{
    // FIXME: Use the transform origin specified in CSS or SVG
    auto transform_origin = m_box.paint_box()->absolute_position();
    auto affine_transform = combine_transformations_2d(m_box.computed_values().transformations());
    auto transformed_position = affine_transform.inverse().value_or({}).map(position - transform_origin) + transform_origin;

    // NOTE: Hit testing basically happens in reverse painting order.
    // https://www.w3.org/TR/CSS22/visuren.html#z-index

    // 7. the child stacking contexts with positive stack levels (least positive first).
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) < 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.paintable)
            return result;
    }

    HitTestResult result;
    // 6. the child stacking contexts with stack level 0 and the positioned descendants with stack level 0.
    m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
        if (box.is_positioned() && !box.paint_box()->stacking_context()) {
            result = box.paint_box()->hit_test(transformed_position, type);
            if (result.paintable)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (result.paintable)
        return result;

    // 5. the in-flow, inline-level, non-positioned descendants, including inline tables and inline blocks.
    if (m_box.children_are_inline() && is<Layout::BlockContainer>(m_box)) {
        auto result = m_box.paint_box()->hit_test(transformed_position, type);
        if (result.paintable)
            return result;
    }

    // 4. the non-positioned floats.
    m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
        if (box.is_floating()) {
            result = box.paint_box()->hit_test(transformed_position, type);
            if (result.paintable)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // 3. the in-flow, non-inline-level, non-positioned descendants.
    if (!m_box.children_are_inline()) {
        m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
            if (!box.is_absolutely_positioned() && !box.is_floating()) {
                result = box.paint_box()->hit_test(transformed_position, type);
                if (result.paintable)
                    return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (result.paintable)
            return result;
    }

    // 2. the child stacking contexts with negative stack levels (most negative first).
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) < 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.paintable)
            return result;
    }

    // 1. the background and borders of the element forming the stacking context.
    if (m_box.paint_box()->absolute_border_box_rect().contains(transformed_position)) {
        return HitTestResult {
            .paintable = m_box.paintable(),
        };
    }

    return {};
}

void StackingContext::dump(int indent) const
{
    StringBuilder builder;
    for (int i = 0; i < indent; ++i)
        builder.append(' ');
    builder.appendff("SC for {} {} [children: {}] (z-index: ", m_box.debug_description(), m_box.paint_box()->absolute_rect(), m_children.size());
    if (m_box.computed_values().z_index().has_value())
        builder.appendff("{}", m_box.computed_values().z_index().value());
    else
        builder.append("auto");
    builder.append(')');

    auto affine_transform = combine_transformations_2d(m_box.computed_values().transformations());
    if (!affine_transform.is_identity()) {
        builder.appendff(", transform: {}", affine_transform);
    }
    dbgln("{}", builder.string_view());
    for (auto& child : m_children)
        child->dump(indent + 1);
}

}
