/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
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
            return a->m_box.computed_values().z_index().value_or(0) < b->m_box.computed_values().z_index().value_or(0);
        });
    }
}

void StackingContext::paint(PaintContext& context, PaintPhase phase)
{
    if (!is<InitialContainingBlockBox>(m_box)) {
        m_box.paint(context, phase);
    } else {
        // NOTE: InitialContainingBlockBox::paint() merely calls StackingContext::paint()
        //       so we call its base class instead.
        downcast<InitialContainingBlockBox>(m_box).BlockBox::paint(context, phase);
    }
    for (auto* child : m_children) {
        child->paint(context, phase);
    }
}

HitTestResult StackingContext::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    HitTestResult result;
    if (!is<InitialContainingBlockBox>(m_box)) {
        result = m_box.hit_test(position, type);
    } else {
        // NOTE: InitialContainingBlockBox::hit_test() merely calls StackingContext::hit_test()
        //       so we call its base class instead.
        result = downcast<InitialContainingBlockBox>(m_box).BlockBox::hit_test(position, type);
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
