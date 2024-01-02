/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/SVGPaintable.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/TableBordersPainting.h>
#include <LibWeb/SVG/SVGMaskElement.h>

namespace Web::Painting {

static void paint_node(Paintable const& paintable, PaintContext& context, PaintPhase phase)
{
    paintable.before_paint(context, phase);
    paintable.paint(context, phase);
    paintable.after_paint(context, phase);
}

StackingContext::StackingContext(PaintableBox& paintable_box, StackingContext* parent, size_t index_in_tree_order)
    : m_paintable_box(paintable_box)
    , m_transform(combine_transformations(paintable_box.computed_values().transformations()))
    , m_transform_origin(compute_transform_origin())
    , m_parent(parent)
    , m_index_in_tree_order(index_in_tree_order)
{
    VERIFY(m_parent != this);
    if (m_parent)
        m_parent->m_children.append(this);
}

void StackingContext::sort()
{
    quick_sort(m_children, [](auto& a, auto& b) {
        auto a_z_index = a->paintable_box().computed_values().z_index().value_or(0);
        auto b_z_index = b->paintable_box().computed_values().z_index().value_or(0);
        if (a_z_index == b_z_index)
            return a->m_index_in_tree_order < b->m_index_in_tree_order;
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

void StackingContext::paint_node_as_stacking_context(Paintable const& paintable, PaintContext& context)
{
    paint_node(paintable, context, PaintPhase::Background);
    paint_node(paintable, context, PaintPhase::Border);
    paint_descendants(context, paintable, StackingContextPaintPhase::BackgroundAndBorders);
    paint_descendants(context, paintable, StackingContextPaintPhase::Floats);
    paint_descendants(context, paintable, StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced);
    paint_node(paintable, context, PaintPhase::Foreground);
    paint_descendants(context, paintable, StackingContextPaintPhase::Foreground);
    paint_node(paintable, context, PaintPhase::Outline);
    paint_node(paintable, context, PaintPhase::Overlay);
    paint_descendants(context, paintable, StackingContextPaintPhase::FocusAndOverlay);
}

void StackingContext::paint_descendants(PaintContext& context, Paintable const& paintable, StackingContextPaintPhase phase)
{
    paintable.apply_scroll_offset(context, to_paint_phase(phase));
    paintable.before_children_paint(context, to_paint_phase(phase));
    paintable.apply_clip_overflow_rect(context, to_paint_phase(phase));

    paintable.for_each_child([&context, phase](auto& child) {
        auto* stacking_context = child.stacking_context_rooted_here();
        auto const& z_index = child.computed_values().z_index();

        // NOTE: Grid specification https://www.w3.org/TR/css-grid-2/#z-order says that grid items should be treated
        //       the same way as CSS2 defines for inline-blocks:
        //       "For each one of these, treat the element as if it created a new stacking context, but any positioned
        //       descendants and descendants which actually create a new stacking context should be considered part of
        //       the parent stacking context, not this new one."
        auto should_be_treated_as_stacking_context = child.layout_node().is_grid_item() && !z_index.has_value();
        if (should_be_treated_as_stacking_context) {
            // FIXME: This may not be fully correct with respect to the paint phases.
            if (phase == StackingContextPaintPhase::Foreground)
                paint_node_as_stacking_context(child, context);
            return;
        }

        if (stacking_context && z_index.has_value())
            return;
        if (child.is_positioned() && !z_index.has_value())
            return;

        if (stacking_context) {
            // FIXME: This may not be fully correct with respect to the paint phases.
            if (phase == StackingContextPaintPhase::Foreground) {
                paint_child(context, *stacking_context);
            }
            // Note: Don't further recurse into descendants as paint_child() will do that.
            return;
        }

        bool child_is_inline_or_replaced = child.is_inline() || is<Layout::ReplacedBox>(child);
        switch (phase) {
        case StackingContextPaintPhase::BackgroundAndBorders:
            if (!child_is_inline_or_replaced && !child.is_floating()) {
                paint_node(child, context, PaintPhase::Background);
                bool is_table_with_collapsed_borders = child.display().is_table_inside() && child.computed_values().border_collapse() == CSS::BorderCollapse::Collapse;
                if (!child.display().is_table_cell() && !is_table_with_collapsed_borders)
                    paint_node(child, context, PaintPhase::Border);
                paint_descendants(context, child, phase);
                if (child.display().is_table_inside() || child.computed_values().border_collapse() == CSS::BorderCollapse::Collapse) {
                    paint_table_borders(context, verify_cast<PaintableBox>(child));
                }
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
                if (child.display().is_table_inside() && child.computed_values().border_collapse() == CSS::BorderCollapse::Separate)
                    paint_table_borders(context, verify_cast<PaintableBox>(child));
                paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
            }
            paint_descendants(context, child, phase);
            break;
        case StackingContextPaintPhase::Foreground:
            paint_node(child, context, PaintPhase::Foreground);
            paint_descendants(context, child, phase);
            break;
        case StackingContextPaintPhase::FocusAndOverlay:
            paint_node(child, context, PaintPhase::Outline);
            paint_node(child, context, PaintPhase::Overlay);
            paint_descendants(context, child, phase);
            break;
        }
    });

    paintable.clear_clip_overflow_rect(context, to_paint_phase(phase));
    paintable.after_children_paint(context, to_paint_phase(phase));
    paintable.reset_scroll_offset(context, to_paint_phase(phase));
}

void StackingContext::paint_child(PaintContext& context, StackingContext const& child)
{
    auto parent_paintable = child.paintable_box().parent();
    if (parent_paintable)
        parent_paintable->before_children_paint(context, PaintPhase::Foreground);

    PaintableBox const* nearest_scrollable_ancestor = child.paintable_box().nearest_scrollable_ancestor_within_stacking_context();

    if (nearest_scrollable_ancestor)
        nearest_scrollable_ancestor->apply_scroll_offset(context, PaintPhase::Foreground);

    child.paint(context);

    if (nearest_scrollable_ancestor)
        nearest_scrollable_ancestor->reset_scroll_offset(context, PaintPhase::Foreground);

    if (parent_paintable)
        parent_paintable->after_children_paint(context, PaintPhase::Foreground);
}

void StackingContext::paint_internal(PaintContext& context) const
{
    // For a more elaborate description of the algorithm, see CSS 2.1 Appendix E
    // Draw the background and borders for the context root (steps 1, 2)
    paint_node(paintable_box(), context, PaintPhase::Background);
    paint_node(paintable_box(), context, PaintPhase::Border);

    // Stacking contexts formed by positioned descendants with negative z-indices (excluding 0) in z-index order
    // (most negative first) then tree order. (step 3)
    // NOTE: This doesn't check if a descendant is positioned as modern CSS allows for alternative methods to establish stacking contexts.
    for (auto* child : m_children) {
        if (child->paintable_box().computed_values().z_index().has_value() && child->paintable_box().computed_values().z_index().value() < 0)
            paint_child(context, *child);
    }

    // Draw the background and borders for block-level children (step 4)
    paint_descendants(context, paintable_box(), StackingContextPaintPhase::BackgroundAndBorders);
    // Draw the non-positioned floats (step 5)
    paint_descendants(context, paintable_box(), StackingContextPaintPhase::Floats);
    // Draw inline content, replaced content, etc. (steps 6, 7)
    paint_descendants(context, paintable_box(), StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced);
    paint_node(paintable_box(), context, PaintPhase::Foreground);
    paint_descendants(context, paintable_box(), StackingContextPaintPhase::Foreground);

    // Draw positioned descendants with z-index `0` or `auto` in tree order. (step 8)
    // FIXME: There's more to this step that we have yet to understand and implement.
    paintable_box().for_each_in_subtree([&context](Paintable const& paintable) {
        auto const& z_index = paintable.computed_values().z_index();

        if (!paintable.is_positioned() || (z_index.has_value() && z_index.value() != 0)) {
            return paintable.stacking_context_rooted_here()
                ? TraversalDecision::SkipChildrenAndContinue
                : TraversalDecision::Continue;
        }

        // Apply scroll offset of nearest scrollable ancestor before painting the positioned descendant.
        PaintableBox const* nearest_scrollable_ancestor = nullptr;
        if (paintable.is_paintable_box())
            nearest_scrollable_ancestor = static_cast<PaintableBox const&>(paintable).nearest_scrollable_ancestor_within_stacking_context();
        if (nearest_scrollable_ancestor)
            nearest_scrollable_ancestor->apply_scroll_offset(context, PaintPhase::Foreground);

        // At this point, `paintable_box` is a positioned descendant with z-index: auto.
        // FIXME: This is basically duplicating logic found elsewhere in this same function. Find a way to make this more elegant.
        auto exit_decision = TraversalDecision::Continue;
        auto* parent_paintable = paintable.parent();
        if (parent_paintable)
            parent_paintable->before_children_paint(context, PaintPhase::Foreground);
        auto containing_block = paintable.containing_block();
        auto* containing_block_paintable = containing_block ? containing_block->paintable() : nullptr;
        if (containing_block_paintable)
            containing_block_paintable->apply_clip_overflow_rect(context, PaintPhase::Foreground);
        if (auto* child = paintable.stacking_context_rooted_here()) {
            paint_child(context, *child);
            exit_decision = TraversalDecision::SkipChildrenAndContinue;
        } else {
            paint_node_as_stacking_context(paintable, context);
        }
        if (parent_paintable)
            parent_paintable->after_children_paint(context, PaintPhase::Foreground);
        if (containing_block_paintable)
            containing_block_paintable->clear_clip_overflow_rect(context, PaintPhase::Foreground);

        if (nearest_scrollable_ancestor)
            nearest_scrollable_ancestor->reset_scroll_offset(context, PaintPhase::Foreground);

        return exit_decision;
    });

    // Stacking contexts formed by positioned descendants with z-indices greater than or equal to 1 in z-index order
    // (smallest first) then tree order. (Step 9)
    // NOTE: This doesn't check if a descendant is positioned as modern CSS allows for alternative methods to establish stacking contexts.
    for (auto* child : m_children) {
        PaintableBox const* nearest_scrollable_ancestor = child->paintable_box().nearest_scrollable_ancestor_within_stacking_context();

        if (nearest_scrollable_ancestor)
            nearest_scrollable_ancestor->apply_scroll_offset(context, PaintPhase::Foreground);

        auto containing_block = child->paintable_box().containing_block();
        auto const* containing_block_paintable = containing_block ? containing_block->paintable() : nullptr;
        if (containing_block_paintable)
            containing_block_paintable->apply_clip_overflow_rect(context, PaintPhase::Foreground);
        if (child->paintable_box().computed_values().z_index().has_value() && child->paintable_box().computed_values().z_index().value() >= 1)
            paint_child(context, *child);
        if (containing_block_paintable)
            containing_block_paintable->clear_clip_overflow_rect(context, PaintPhase::Foreground);

        if (nearest_scrollable_ancestor)
            nearest_scrollable_ancestor->reset_scroll_offset(context, PaintPhase::Foreground);
    }

    paint_node(paintable_box(), context, PaintPhase::Outline);

    if (context.should_paint_overlay()) {
        paint_node(paintable_box(), context, PaintPhase::Overlay);
        paint_descendants(context, paintable_box(), StackingContextPaintPhase::FocusAndOverlay);
    }
}

Gfx::FloatMatrix4x4 StackingContext::combine_transformations(Vector<CSS::Transformation> const& transformations) const
{
    auto matrix = Gfx::FloatMatrix4x4::identity();

    for (auto const& transform : transformations)
        matrix = matrix * transform.to_matrix(paintable_box());

    return matrix;
}

// FIXME: This extracts the affine 2D part of the full transformation matrix.
//  Use the whole matrix when we get better transformation support in LibGfx or use LibGL for drawing the bitmap
Gfx::AffineTransform StackingContext::affine_transform_matrix() const
{
    return Gfx::extract_2d_affine_transform(m_transform);
}

static Gfx::FloatMatrix4x4 matrix_with_scaled_translation(Gfx::FloatMatrix4x4 matrix, float scale)
{
    auto* m = matrix.elements();
    m[0][3] *= scale;
    m[1][3] *= scale;
    m[2][3] *= scale;
    return matrix;
}

void StackingContext::paint(PaintContext& context) const
{
    auto opacity = paintable_box().computed_values().opacity();
    if (opacity == 0.0f)
        return;

    RecordingPainterStateSaver saver(context.recording_painter());

    auto to_device_pixels_scale = float(context.device_pixels_per_css_pixel());
    RecordingPainter::PushStackingContextParams push_stacking_context_params {
        .opacity = opacity,
        .is_fixed_position = paintable_box().is_fixed_position(),
        .source_paintable_rect = context.enclosing_device_rect(paintable_box().absolute_paint_rect()).to_type<int>(),
        .image_rendering = paintable_box().computed_values().image_rendering(),
        .transform = {
            .origin = transform_origin().scaled(to_device_pixels_scale),
            .matrix = matrix_with_scaled_translation(transform_matrix(), to_device_pixels_scale),
        },
    };

    if (auto masking_area = paintable_box().get_masking_area(); masking_area.has_value()) {
        if (masking_area->is_empty())
            return;
        auto mask_bitmap = paintable_box().calculate_mask(context, *masking_area);
        if (mask_bitmap) {
            auto source_paintable_rect = context.enclosing_device_rect(*masking_area).to_type<int>();
            push_stacking_context_params.source_paintable_rect = source_paintable_rect;
            push_stacking_context_params.mask = StackingContextMask {
                .mask_bitmap = mask_bitmap.release_nonnull(),
                .mask_kind = *paintable_box().get_mask_type()
            };
        }
    }

    context.recording_painter().push_stacking_context(push_stacking_context_params);
    paint_internal(context);
    context.recording_painter().pop_stacking_context();
}

Gfx::FloatPoint StackingContext::compute_transform_origin() const
{
    auto style_value = paintable_box().computed_values().transform_origin();
    // FIXME: respect transform-box property
    auto reference_box = paintable_box().absolute_border_box_rect();
    auto x = reference_box.left() + style_value.x.to_px(paintable_box().layout_node(), reference_box.width());
    auto y = reference_box.top() + style_value.y.to_px(paintable_box().layout_node(), reference_box.height());
    return { x.to_float(), y.to_float() };
}

template<typename U, typename Callback>
static TraversalDecision for_each_in_inclusive_subtree_of_type_within_same_stacking_context_in_reverse(Paintable const& paintable, Callback callback)
{
    if (paintable.stacking_context_rooted_here()) {
        // Note: Include the stacking context (so we can hit test it), but don't recurse into it.
        if (auto decision = callback(static_cast<U const&>(paintable)); decision != TraversalDecision::Continue)
            return decision;
        return TraversalDecision::SkipChildrenAndContinue;
    }
    for (auto* child = paintable.last_child(); child; child = child->previous_sibling()) {
        if (for_each_in_inclusive_subtree_of_type_within_same_stacking_context_in_reverse<U>(*child, callback) == TraversalDecision::Break)
            return TraversalDecision::Break;
    }
    if (is<U>(paintable)) {
        if (auto decision = callback(static_cast<U const&>(paintable)); decision != TraversalDecision::Continue)
            return decision;
    }
    return TraversalDecision::Continue;
}

template<typename U, typename Callback>
static TraversalDecision for_each_in_subtree_of_type_within_same_stacking_context_in_reverse(Paintable const& paintable, Callback callback)
{
    for (auto* child = paintable.last_child(); child; child = child->previous_sibling()) {
        if (for_each_in_inclusive_subtree_of_type_within_same_stacking_context_in_reverse<U>(*child, callback) == TraversalDecision::Break)
            return TraversalDecision::Break;
    }
    return TraversalDecision::Continue;
}

Optional<HitTestResult> StackingContext::hit_test(CSSPixelPoint position, HitTestType type) const
{
    if (!paintable_box().is_visible())
        return {};

    auto transform_origin = this->transform_origin().to_type<CSSPixels>();
    // NOTE: This CSSPixels -> Float -> CSSPixels conversion is because we can't AffineTransform::map() a CSSPixelPoint.
    Gfx::FloatPoint offset_position {
        (position.x() - transform_origin.x()).to_float(),
        (position.y() - transform_origin.y()).to_float()
    };
    auto transformed_position = affine_transform_matrix().inverse().value_or({}).map(offset_position).to_type<CSSPixels>() + transform_origin;

    if (paintable_box().is_fixed_position()) {
        auto scroll_offset = paintable_box().document().navigable()->viewport_scroll_offset();
        transformed_position.translate_by(-scroll_offset);
    }

    // FIXME: Support more overflow variations.
    if (paintable_box().computed_values().overflow_x() == CSS::Overflow::Hidden && paintable_box().computed_values().overflow_y() == CSS::Overflow::Hidden) {
        if (!paintable_box().absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
            return {};
    }

    // NOTE: Hit testing basically happens in reverse painting order.
    // https://www.w3.org/TR/CSS22/visuren.html#z-index

    // 7. the child stacking contexts with positive stack levels (least positive first).
    // NOTE: Hit testing follows reverse painting order, that's why the conditions here are reversed.
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.paintable_box().computed_values().z_index().value_or(0) <= 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 6. the child stacking contexts with stack level 0 and the positioned descendants with stack level 0.
    Optional<HitTestResult> result;
    for_each_in_subtree_of_type_within_same_stacking_context_in_reverse<PaintableBox>(paintable_box(), [&](PaintableBox const& paintable_box) {
        // FIXME: Support more overflow variations.
        if (paintable_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paintable_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
            if (!paintable_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                return TraversalDecision::SkipChildrenAndContinue;
        }

        auto const& z_index = paintable_box.computed_values().z_index();
        if (z_index.value_or(0) == 0 && paintable_box.is_positioned() && !paintable_box.stacking_context()) {
            auto candidate = paintable_box.hit_test(transformed_position, type);
            if (candidate.has_value() && candidate->paintable->visible_for_hit_testing()) {
                result = move(candidate);
                return TraversalDecision::Break;
            }
        }

        if (paintable_box.stacking_context()) {
            if (z_index.value_or(0) == 0) {
                auto candidate = paintable_box.stacking_context()->hit_test(transformed_position, type);
                if (candidate.has_value() && candidate->paintable->visible_for_hit_testing()) {
                    result = move(candidate);
                    return TraversalDecision::Break;
                }
            }
        }

        return TraversalDecision::Continue;
    });
    if (result.has_value())
        return result;

    // 5. the in-flow, inline-level, non-positioned descendants, including inline tables and inline blocks.
    if (paintable_box().layout_box().children_are_inline() && is<Layout::BlockContainer>(paintable_box().layout_box())) {
        auto result = paintable_box().hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 4. the non-positioned floats.
    for_each_in_subtree_of_type_within_same_stacking_context_in_reverse<PaintableBox>(paintable_box(), [&](PaintableBox const& paintable_box) {
        // FIXME: Support more overflow variations.
        if (paintable_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paintable_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
            if (!paintable_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                return TraversalDecision::SkipChildrenAndContinue;
        }

        if (paintable_box.is_floating()) {
            if (auto candidate = paintable_box.hit_test(transformed_position, type); candidate.has_value()) {
                result = move(candidate);
                return TraversalDecision::Break;
            }
        }
        return TraversalDecision::Continue;
    });
    if (result.has_value() && result->paintable->visible_for_hit_testing())
        return result;

    // 3. the in-flow, non-inline-level, non-positioned descendants.
    if (!paintable_box().layout_box().children_are_inline()) {
        for_each_in_subtree_of_type_within_same_stacking_context_in_reverse<PaintableBox>(paintable_box(), [&](PaintableBox const& paintable_box) {
            // FIXME: Support more overflow variations.
            if (paintable_box.computed_values().overflow_x() == CSS::Overflow::Hidden && paintable_box.computed_values().overflow_y() == CSS::Overflow::Hidden) {
                if (!paintable_box.absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y()))
                    return TraversalDecision::SkipChildrenAndContinue;
            }

            if (!paintable_box.is_absolutely_positioned() && !paintable_box.is_floating()) {
                if (auto candidate = paintable_box.hit_test(transformed_position, type); candidate.has_value()) {
                    result = move(candidate);
                    return TraversalDecision::Break;
                }
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
        if (child.paintable_box().computed_values().z_index().value_or(0) >= 0)
            break;
        auto result = child.hit_test(transformed_position, type);
        if (result.has_value() && result->paintable->visible_for_hit_testing())
            return result;
    }

    // 1. the background and borders of the element forming the stacking context.
    if (paintable_box().absolute_border_box_rect().contains(transformed_position.x(), transformed_position.y())) {
        return HitTestResult {
            .paintable = const_cast<PaintableBox&>(paintable_box()),
        };
    }

    return {};
}

void StackingContext::dump(int indent) const
{
    StringBuilder builder;
    for (int i = 0; i < indent; ++i)
        builder.append(' ');
    builder.appendff("SC for {} {} [children: {}] (z-index: ", paintable_box().layout_box().debug_description(), paintable_box().absolute_rect(), m_children.size());
    if (paintable_box().computed_values().z_index().has_value())
        builder.appendff("{}", paintable_box().computed_values().z_index().value());
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
