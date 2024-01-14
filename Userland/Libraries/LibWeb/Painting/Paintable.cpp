/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/Paintable.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Painting {

Paintable::Paintable(Layout::Node const& layout_node)
    : m_layout_node(layout_node)
    , m_browsing_context(const_cast<HTML::BrowsingContext&>(layout_node.browsing_context()))
{
}

Paintable::~Paintable()
{
}

void Paintable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    TreeNode::visit_edges(visitor);
    visitor.visit(m_dom_node);
    visitor.visit(m_layout_node);
    visitor.visit(m_browsing_context);
    if (m_containing_block.has_value())
        visitor.visit(m_containing_block.value());
}

bool Paintable::is_visible() const
{
    return computed_values().visibility() == CSS::Visibility::Visible && computed_values().opacity() != 0;
}

bool Paintable::is_positioned() const
{
    if (layout_node().is_grid_item() && computed_values().z_index().has_value()) {
        // https://www.w3.org/TR/css-grid-2/#z-order
        // grid items with z_index should behave as if position were "relative"
        return true;
    }
    return computed_values().position() != CSS::Positioning::Static;
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

StackingContext* Paintable::enclosing_stacking_context()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (auto* stacking_context = ancestor->stacking_context())
            return const_cast<StackingContext*>(stacking_context);
    }
    // We should always reach the viewport's stacking context.
    VERIFY_NOT_REACHED();
}

void Paintable::set_stacking_context(NonnullOwnPtr<StackingContext> stacking_context)
{
    m_stacking_context = move(stacking_context);
}

void Paintable::invalidate_stacking_context()
{
    m_stacking_context = nullptr;
}

PaintableBox const* Paintable::nearest_scrollable_ancestor_within_stacking_context() const
{
    auto* ancestor = parent();
    while (ancestor) {
        if (ancestor->stacking_context())
            return nullptr;
        if (ancestor->is_paintable_box() && static_cast<PaintableBox const*>(ancestor)->has_scrollable_overflow())
            return static_cast<PaintableBox const*>(ancestor);
        ancestor = ancestor->parent();
    }
    return nullptr;
}

CSSPixelPoint Paintable::box_type_agnostic_position() const
{
    if (is_paintable_box())
        return static_cast<PaintableBox const*>(this)->absolute_position();

    VERIFY(is_inline());
    if (is_inline_paintable()) {
        auto const& inline_paintable = static_cast<Painting::InlinePaintable const&>(*this);
        if (!inline_paintable.fragments().is_empty())
            return inline_paintable.fragments().first().absolute_rect().location();
        VERIFY_NOT_REACHED();
    }

    CSSPixelPoint position;
    if (auto const* block = containing_block(); block && block->paintable() && is<Painting::PaintableWithLines>(*block->paintable())) {
        static_cast<Painting::PaintableWithLines const&>(*block->paintable_box()).for_each_fragment([&](auto& fragment) {
            position = fragment.absolute_rect().location();
            return IterationDecision::Break;
        });
    }

    return position;
}

}
