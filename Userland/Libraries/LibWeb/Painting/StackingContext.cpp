/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

StackingContext::StackingContext(Box& box, StackingContext* parent)
    : m_box(box)
    , m_parent(parent)
{
    VERIFY(m_parent != this);
    if (m_parent) {
        m_parent->m_children.append(this);

        // FIXME: Don't sort on every append..
        quick_sort(m_parent->m_children, [](auto& a, auto& b) {
            auto a_z_index = a->m_box.computed_values().z_index().value_or(0);
            auto b_z_index = b->m_box.computed_values().z_index().value_or(0);
            if (a_z_index == b_z_index)
                return a->m_box.is_before(b->m_box);
            return a_z_index < b_z_index;
        });
    }
}

void StackingContext::paint_descendants(PaintContext& context, Node& box, StackingContextPaintPhase phase)
{
    if (phase == StackingContextPaintPhase::Foreground)
        box.before_children_paint(context, PaintPhase::Foreground);

    box.for_each_child([&](auto& child) {
        if (child.establishes_stacking_context())
            return;
        bool child_is_inline_or_replaced = child.is_inline() || is<ReplacedBox>(child);
        switch (phase) {
        case StackingContextPaintPhase::BackgroundAndBorders:
            if (!child_is_inline_or_replaced && !child.is_floating() && !child.is_positioned()) {
                child.paint(context, PaintPhase::Background);
                child.paint(context, PaintPhase::Border);
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::Floats:
            if (!child.is_positioned()) {
                if (child.is_floating()) {
                    child.paint(context, PaintPhase::Background);
                    child.paint(context, PaintPhase::Border);
                    paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
                }
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::BackgroundAndBordersForInlineLevelAndReplaced:
            if (!child.is_positioned()) {
                if (child_is_inline_or_replaced) {
                    child.paint(context, PaintPhase::Background);
                    child.paint(context, PaintPhase::Border);
                    paint_descendants(context, child, StackingContextPaintPhase::BackgroundAndBorders);
                }
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::Foreground:
            if (!child.is_positioned()) {
                child.paint(context, PaintPhase::Foreground);
                paint_descendants(context, child, phase);
            }
            break;
        case StackingContextPaintPhase::FocusAndOverlay:
            if (context.has_focus()) {
                child.paint(context, PaintPhase::FocusOutline);
            }
            child.paint(context, PaintPhase::Overlay);
            paint_descendants(context, child, phase);
            break;
        }
    });

    if (phase == StackingContextPaintPhase::Foreground)
        box.after_children_paint(context, PaintPhase::Foreground);
}

void StackingContext::paint_internal(PaintContext& context)
{
    // For a more elaborate description of the algorithm, see CSS 2.1 Appendix E
    // Draw the background and borders for the context root (steps 1, 2)
    m_box.paint(context, PaintPhase::Background);
    m_box.paint(context, PaintPhase::Border);
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
    m_box.paint(context, PaintPhase::Foreground);
    paint_descendants(context, m_box, StackingContextPaintPhase::Foreground);
    // Draw other positioned descendants (steps 8, 9)
    for (auto* child : m_children) {
        if (child->m_box.computed_values().z_index().has_value() && child->m_box.computed_values().z_index().value() < 0)
            continue;
        child->paint(context);
    }

    m_box.paint(context, PaintPhase::FocusOutline);
    m_box.paint(context, PaintPhase::Overlay);
    paint_descendants(context, m_box, StackingContextPaintPhase::FocusAndOverlay);
}

void StackingContext::paint(PaintContext& context)
{
    Gfx::PainterStateSaver saver(context.painter());
    if (m_box.is_fixed_position()) {
        context.painter().translate(context.scroll_offset());
    }

    auto opacity = m_box.computed_values().opacity();
    if (opacity == 0.0f)
        return;

    if (opacity < 1.0f) {
        auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, context.painter().target()->size());
        if (bitmap_or_error.is_error())
            return;
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(bitmap);
        PaintContext paint_context(painter, context.palette(), context.scroll_offset());
        paint_internal(paint_context);
        context.painter().blit(Gfx::IntPoint(m_box.absolute_position()), bitmap, Gfx::IntRect(m_box.absolute_rect()), opacity);
    } else {
        paint_internal(context);
    }
}

HitTestResult StackingContext::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    // NOTE: Hit testing basically happens in reverse painting order.
    // https://www.w3.org/TR/CSS22/visuren.html#z-index

    // 7. the child stacking contexts with positive stack levels (least positive first).
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) < 0)
            break;
        auto result = child.hit_test(position, type);
        if (result.layout_node)
            return result;
    }

    HitTestResult result;
    // 6. the child stacking contexts with stack level 0 and the positioned descendants with stack level 0.
    m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
        if (box.is_positioned() && !box.stacking_context()) {
            result = box.hit_test(position, type);
            if (result.layout_node)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (result.layout_node)
        return result;

    // 5. the in-flow, inline-level, non-positioned descendants, including inline tables and inline blocks.
    if (m_box.children_are_inline() && is<BlockContainer>(m_box)) {
        auto result = m_box.hit_test(position, type);
        if (result.layout_node)
            return result;
    }

    // 4. the non-positioned floats.
    m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
        if (box.is_floating()) {
            result = box.hit_test(position, type);
            if (result.layout_node)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // 3. the in-flow, non-inline-level, non-positioned descendants.
    if (!m_box.children_are_inline()) {
        m_box.for_each_in_subtree_of_type<Layout::Box>([&](Layout::Box const& box) {
            if (!box.is_absolutely_positioned() && !box.is_floating()) {
                result = box.hit_test(position, type);
                if (result.layout_node)
                    return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (result.layout_node)
            return result;
    }

    // 2. the child stacking contexts with negative stack levels (most negative first).
    for (ssize_t i = m_children.size() - 1; i >= 0; --i) {
        auto const& child = *m_children[i];
        if (child.m_box.computed_values().z_index().value_or(0) < 0)
            break;
        auto result = child.hit_test(position, type);
        if (result.layout_node)
            return result;
    }

    // 1. the background and borders of the element forming the stacking context.
    if (m_box.absolute_border_box_rect().contains(position.to_type<float>())) {
        return HitTestResult {
            .layout_node = m_box,
        };
    }

    return {};
}

void StackingContext::dump(int indent) const
{
    StringBuilder builder;
    for (int i = 0; i < indent; ++i)
        builder.append(' ');
    builder.appendff("SC for {} {} [children: {}] (z-index: ", m_box.debug_description(), m_box.absolute_rect(), m_children.size());
    if (m_box.computed_values().z_index().has_value())
        builder.appendff("{}", m_box.computed_values().z_index().value());
    else
        builder.append("auto");
    builder.append(')');
    dbgln("{}", builder.string_view());
    for (auto& child : m_children)
        child->dump(indent + 1);
}

}
