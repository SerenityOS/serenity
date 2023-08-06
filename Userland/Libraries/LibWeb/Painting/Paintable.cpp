/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/Paintable.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

void Paintable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_layout_node);
    if (m_containing_block.has_value())
        visitor.visit(m_containing_block.value());
}

Paintable::DispatchEventOfSameName Paintable::handle_mousedown(Badge<EventHandler>, CSSPixelPoint, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

Paintable::DispatchEventOfSameName Paintable::handle_mouseup(Badge<EventHandler>, CSSPixelPoint, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

Paintable::DispatchEventOfSameName Paintable::handle_mousemove(Badge<EventHandler>, CSSPixelPoint, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

bool Paintable::handle_mousewheel(Badge<EventHandler>, CSSPixelPoint, unsigned, unsigned, int, int)
{
    return false;
}

Optional<HitTestResult> Paintable::hit_test(CSSPixelPoint, HitTestType) const
{
    return {};
}

Paintable const* Paintable::first_child() const
{
    auto const* layout_child = m_layout_node->first_child();
    for (; layout_child && !layout_child->paintable(); layout_child = layout_child->next_sibling())
        ;
    return layout_child ? layout_child->paintable() : nullptr;
}

Paintable const* Paintable::next_sibling() const
{
    auto const* layout_node = m_layout_node->next_sibling();
    for (; layout_node && !layout_node->paintable(); layout_node = layout_node->next_sibling())
        ;
    return layout_node ? layout_node->paintable() : nullptr;
}

Paintable const* Paintable::last_child() const
{
    auto const* layout_child = m_layout_node->last_child();
    for (; layout_child && !layout_child->paintable(); layout_child = layout_child->previous_sibling())
        ;
    return layout_child ? layout_child->paintable() : nullptr;
}

Paintable const* Paintable::previous_sibling() const
{
    auto const* layout_node = m_layout_node->previous_sibling();
    for (; layout_node && !layout_node->paintable(); layout_node = layout_node->previous_sibling())
        ;
    return layout_node ? layout_node->paintable() : nullptr;
}

StackingContext const* Paintable::stacking_context_rooted_here() const
{
    if (!layout_node().is_box())
        return nullptr;
    return static_cast<PaintableBox const&>(*this).stacking_context();
}

}
