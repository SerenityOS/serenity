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

Paintable::Paintable(Layout::Node const& layout_node)
    : m_layout_node(layout_node)
    , m_browsing_context(const_cast<HTML::BrowsingContext&>(layout_node.browsing_context()))
{
}

void Paintable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_dom_node);
    visitor.visit(m_layout_node);
    visitor.visit(m_browsing_context);
    if (m_containing_block.has_value())
        visitor.visit(m_containing_block.value());
}

void Paintable::set_dom_node(JS::GCPtr<DOM::Node> dom_node)
{
    m_dom_node = dom_node;
}

JS::GCPtr<DOM::Node> Paintable::dom_node()
{
    return m_dom_node;
}

JS::GCPtr<DOM::Node const> Paintable::dom_node() const
{
    return m_dom_node;
}

HTML::BrowsingContext const& Paintable::browsing_context() const
{
    return m_browsing_context;
}

HTML::BrowsingContext& Paintable::browsing_context()
{
    return m_browsing_context;
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

StackingContext const* Paintable::stacking_context_rooted_here() const
{
    if (!is<PaintableBox>(*this))
        return nullptr;
    return static_cast<PaintableBox const&>(*this).stacking_context();
}

}
