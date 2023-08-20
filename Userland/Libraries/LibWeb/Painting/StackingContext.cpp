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
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/TableBordersPainting.h>

namespace Web::Painting {

static void paint_node(Paintable const& paintable, PaintContext& context, PaintPhase phase)
{
    paintable.paint(context, phase);
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

void StackingContext::paint_node_as_stacking_context(Paintable const& paintable, PaintContext& context) const
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

void StackingContext::paint_descendants(PaintContext& context, Paintable const& paintable, StackingContextPaintPhase phase) const
{
    paintable.before_children_paint(context, to_paint_phase(phase));
    paintable.apply_clip_overflow_rect(context, to_paint_phase(phase));

    paintable.for_each_child([this, &context, phase](auto& child) {
        auto* stacking_context = child.stacking_context_rooted_here();

        if (child.is_positioned()) {
            // If `child` is positioned with a z-index of `0` or `auto`, skip over it.
            auto const& z_index = child.computed_values().z_index();
            if (!z_index.has_value() || z_index.value() == 0)
                return;

            // Skip positioned children with stacking contexts, these are handled in paint_internal().
            if (stacking_context)
                return;
        }

        if (stacking_context) {
            // FIXME: This may not be fully correct with respect to the paint phases.
            if (phase == StackingContextPaintPhase::Foreground)
                paint_child(context, *stacking_context);
            // Note: Don't further recuse into descendants as paint_child() will do that.
            return;
        }

        // NOTE: Grid specification https://www.w3.org/TR/css-grid-2/#z-order says that grid items should be treated
        //       the same way as CSS2 defines for inline-blocks:
        //       "For each one of these, treat the element as if it created a new stacking context, but any positioned
        //       descendants and descendants which actually create a new stacking context should be considered part of
        //       the parent stacking context, not this new one."
        auto should_be_treated_as_stacking_context = child.layout_node().is_grid_item();
        if (should_be_treated_as_stacking_context) {
            // FIXME: This may not be fully correct with respect to the paint phases.
            if (phase == StackingContextPaintPhase::Foreground)
                paint_node_as_stacking_context(child, context);
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
}

void StackingContext::paint_child(PaintContext& context, StackingContext const& child) const
{
    auto parent_paintable = child.paintable_box().parent();
    if (parent_paintable)
        parent_paintable->before_children_paint(context, PaintPhase::Foreground);
    auto containing_block = child.paintable_box().containing_block();
    auto* containing_block_paintable = containing_block ? containing_block->paintable() : nullptr;
    if (containing_block_paintable)
        containing_block_paintable->apply_clip_overflow_rect(context, PaintPhase::Foreground);

    child.paint(context);

    if (parent_paintable)
        parent_paintable->after_children_paint(context, PaintPhase::Foreground);
    if (containing_block_paintable)
        containing_block_paintable->clear_clip_overflow_rect(context, PaintPhase::Foreground);
}

void StackingContext::paint_internal(PaintContext& context) const
{
    // For a more elaborate description of the algorithm, see CSS 2.1 Appendix E
    // Draw the background and borders for the context root (steps 1, 2)
    paint_node(paintable_box(), context, PaintPhase::Background);
    paint_node(paintable_box(), context, PaintPhase::Border);

    // Stacking contexts formed by positioned descendants with negative z-indices (excluding 0) in z-index order
    // (most negative first) then tree order. (step 3)
    for (auto* child : m_children) {
        if (!child->paintable_box().is_positioned())
            continue;
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
    paintable_box().for_each_in_subtree([this, &context](Paintable const& paintable) {
        auto const& z_index = paintable.computed_values().z_index();

        if (!paintable.is_positioned() || (z_index.has_value() && z_index.value() != 0)) {
            return paintable.stacking_context_rooted_here()
                ? TraversalDecision::SkipChildrenAndContinue
                : TraversalDecision::Continue;
        }

        // At this point, `paintable_box` is a positioned descendant with z-index: auto.
        // FIXME: This is basically duplicating logic found elsewhere in this same function. Find a way to make this more elegant.
        auto* parent_paintable = paintable.parent();
        if (parent_paintable)
            parent_paintable->before_children_paint(context, PaintPhase::Foreground);
        auto containing_block = paintable.containing_block();
        auto* containing_block_paintable = containing_block ? containing_block->paintable() : nullptr;
        if (containing_block_paintable)
            containing_block_paintable->apply_clip_overflow_rect(context, PaintPhase::Foreground);
        if (auto* child = paintable.stacking_context_rooted_here()) {
            paint_child(context, *child);
            return TraversalDecision::SkipChildrenAndContinue;
        } else {
            paint_node_as_stacking_context(paintable, context);
        }
        if (parent_paintable)
            parent_paintable->after_children_paint(context, PaintPhase::Foreground);
        if (containing_block_paintable)
            containing_block_paintable->clear_clip_overflow_rect(context, PaintPhase::Foreground);

        return TraversalDecision::Continue;
    });

    // Stacking contexts formed by positioned descendants with z-indices greater than or equal to 1 in z-index order
    // (smallest first) then tree order. (Step 9)
    for (auto* child : m_children) {
        if (!child->paintable_box().is_positioned())
            continue;
        if (child->paintable_box().computed_values().z_index().has_value() && child->paintable_box().computed_values().z_index().value() >= 1)
            paint_child(context, *child);
    }

    paint_node(paintable_box(), context, PaintPhase::Outline);
    paint_node(paintable_box(), context, PaintPhase::Overlay);
    paint_descendants(context, paintable_box(), StackingContextPaintPhase::FocusAndOverlay);
}

Gfx::FloatMatrix4x4 StackingContext::get_transformation_matrix(CSS::Transformation const& transformation) const
{
    auto count = transformation.values.size();
    auto value = [this, transformation](size_t index, CSS::Length const& reference_length = CSS::Length::make_px(0)) -> float {
        return transformation.values[index].visit(
            [this, reference_length](CSS::LengthPercentage const& value) -> double {
                return value.resolved(paintable_box().layout_node(), reference_length).to_px(paintable_box().layout_box()).to_float();
            },
            [this](CSS::AngleOrCalculated const& value) {
                return value.resolved(paintable_box().layout_node()).to_degrees() * M_DEG2RAD;
            },
            [](double value) {
                return value;
            });
    };

    auto reference_box = paintable_box().absolute_rect();
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
    case CSS::TransformFunction::Translate3d:
        return Gfx::FloatMatrix4x4(1, 0, 0, value(0, width),
            0, 1, 0, value(1, height),
            0, 0, 1, value(2),
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
    case CSS::TransformFunction::Skew:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        if (count == 2)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                tanf(value(1)), 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::SkewX:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, tanf(value(0)), 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        break;
    case CSS::TransformFunction::SkewY:
        if (count == 1)
            return Gfx::FloatMatrix4x4(1, 0, 0, 0,
                tanf(value(0)), 1, 0, 0,
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
Gfx::AffineTransform StackingContext::affine_transform_matrix() const
{
    auto* m = m_transform.elements();
    return Gfx::AffineTransform(m[0][0], m[1][0], m[0][1], m[1][1], m[0][3], m[1][3]);
}

void StackingContext::paint(PaintContext& context) const
{
    Gfx::PainterStateSaver saver(context.painter());
    if (paintable_box().is_fixed_position()) {
        context.painter().translate(-context.painter().translation());
    }

    auto opacity = paintable_box().computed_values().opacity();
    if (opacity == 0.0f)
        return;

    auto affine_transform = affine_transform_matrix();
    auto translation = context.rounded_device_point(affine_transform.translation().to_type<CSSPixels>()).to_type<int>().to_type<float>();
    affine_transform.set_translation(translation);

    if (opacity < 1.0f || !affine_transform.is_identity_or_translation()) {
        auto transform_origin = this->transform_origin();
        auto source_rect = context.enclosing_device_rect(paintable_box().absolute_paint_rect()).to_type<int>().to_type<float>().translated(-transform_origin);
        auto transformed_destination_rect = affine_transform.map(source_rect).translated(transform_origin);
        auto destination_rect = transformed_destination_rect.to_rounded<int>();

        // FIXME: We should find a way to scale the paintable, rather than paint into a separate bitmap,
        // then scale it. This snippet now copies the background at the destination, then scales it down/up
        // to the size of the source (which could add some artefacts, though just scaling the bitmap already does that).
        // We need to copy the background at the destination because a bunch of our rendering effects now rely on
        // being able to sample the painter (see border radii, shadows, filters, etc).
        CSSPixelPoint destination_clipped_fixup {};
        auto try_get_scaled_destination_bitmap = [&]() -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
            Gfx::IntRect actual_destination_rect;
            auto bitmap = TRY(context.painter().get_region_bitmap(destination_rect, Gfx::BitmapFormat::BGRA8888, actual_destination_rect));
            // get_region_bitmap() may clip to a smaller region if the requested rect goes outside the painter, so we need to account for that.
            destination_clipped_fixup = CSSPixelPoint { destination_rect.location() - actual_destination_rect.location() };
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
        painter.translate(context.rounded_device_point(-paintable_box().absolute_paint_rect().location() + destination_clipped_fixup).to_type<int>());
        auto paint_context = context.clone(painter);
        paint_internal(paint_context);

        if (destination_rect.size() == bitmap->size()) {
            context.painter().blit(destination_rect.location(), *bitmap, bitmap->rect(), opacity);
        } else {
            auto scaling_mode = CSS::to_gfx_scaling_mode(paintable_box().computed_values().image_rendering(), bitmap->rect(), destination_rect);
            context.painter().draw_scaled_bitmap(destination_rect, *bitmap, bitmap->rect(), opacity, scaling_mode);
        }
    } else {
        Gfx::PainterStateSaver saver(context.painter());
        context.painter().translate(affine_transform.translation().to_rounded<int>());
        paint_internal(context);
    }
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
        auto scroll_offset = paintable_box().document().browsing_context()->viewport_scroll_offset();
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
