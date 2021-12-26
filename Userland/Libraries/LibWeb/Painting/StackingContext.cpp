/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
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
        // FIXME: Apparently this also breaks tree order inside layers
        quick_sort(m_parent->m_children, [](auto& a, auto& b) {
            return a->m_box.computed_values().z_index().value_or(0) < b->m_box.computed_values().z_index().value_or(0);
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
        switch (phase) {
        case StackingContextPaintPhase::BackgroundAndBorders:
            if (!child.is_floating() && !child.is_positioned()) {
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
    auto opacity = m_box.computed_values().opacity();
    if (opacity.has_value() && opacity.value() == 0.0f)
        return;

    if (opacity.has_value() && opacity.value() != 1.0f) {
        auto bitmap = context.painter().target();
        auto new_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, bitmap->size());
        if (!new_bitmap)
            return;
        Gfx::Painter painter(*new_bitmap);
        PaintContext paint_context(painter, context.palette(), context.scroll_offset());
        paint_internal(paint_context);
        context.painter().blit(Gfx::IntPoint(m_box.absolute_position()), *new_bitmap, Gfx::IntRect(m_box.absolute_rect()), opacity.value());
    } else {
        paint_internal(context);
    }
}

HitTestResult StackingContext::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    HitTestResult result;
    if (!is<InitialContainingBlock>(m_box)) {
        result = m_box.hit_test(position, type);
    } else {
        // NOTE: InitialContainingBlock::hit_test() merely calls StackingContext::hit_test()
        //       so we call its base class instead.
        result = verify_cast<InitialContainingBlock>(m_box).BlockContainer::hit_test(position, type);
    }

    int z_index = m_box.computed_values().z_index().value_or(0);

    for (auto* child : m_children) {
        int child_z_index = child->m_box.computed_values().z_index().value_or(0);
        if (result.layout_node && (child_z_index < z_index))
            continue;

        auto result_here = child->hit_test(position, type);
        if (result_here.layout_node)
            result = result_here;
    }
    return result;
}

void StackingContext::dump(int indent) const
{
    StringBuilder builder;
    for (int i = 0; i < indent; ++i)
        builder.append(' ');
    builder.appendff("SC for {}({}) {} [children: {}]", m_box.class_name(), m_box.dom_node() ? m_box.dom_node()->node_name().characters() : "(anonymous)", m_box.absolute_rect().to_string().characters(), m_children.size());
    dbgln("{}", builder.string_view());
    for (auto& child : m_children)
        child->dump(indent + 1);
}

}
