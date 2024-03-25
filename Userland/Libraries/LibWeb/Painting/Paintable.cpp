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
    auto& computed_values = layout_node.computed_values();
    if (layout_node.is_grid_item() && computed_values.z_index().has_value()) {
        // https://www.w3.org/TR/css-grid-2/#z-order
        // grid items with z_index should behave as if position were "relative"
        m_positioned = true;
    } else {
        m_positioned = computed_values.position() != CSS::Positioning::Static;
    }

    m_fixed_position = computed_values.position() == CSS::Positioning::Fixed;
    m_absolutely_positioned = computed_values.position() == CSS::Positioning::Absolute;
    m_floating = layout_node.is_floating();
    m_inline = layout_node.is_inline();
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
    auto const& computed_values = this->computed_values();
    return computed_values.visibility() == CSS::Visibility::Visible && computed_values.opacity() != 0;
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

JS::GCPtr<HTML::Navigable> Paintable::navigable() const
{
    return document().navigable();
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

TraversalDecision Paintable::hit_test(CSSPixelPoint, HitTestType, Function<TraversalDecision(HitTestResult)> const&) const
{
    return TraversalDecision::Continue;
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

void Paintable::set_needs_display() const
{
    auto* containing_block = this->containing_block();
    if (!containing_block)
        return;
    auto navigable = this->navigable();
    if (!navigable)
        return;

    if (is<Painting::InlinePaintable>(*this)) {
        auto const& fragments = static_cast<Painting::InlinePaintable const*>(this)->fragments();
        for (auto const& fragment : fragments)
            navigable->set_needs_display(fragment.absolute_rect());
    }

    if (!is<Painting::PaintableWithLines>(*containing_block))
        return;
    static_cast<Painting::PaintableWithLines const&>(*containing_block).for_each_fragment([&](auto& fragment) {
        navigable->set_needs_display(fragment.absolute_rect());
        return IterationDecision::Continue;
    });
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
        return inline_paintable.bounding_rect().location();
    }

    CSSPixelPoint position;
    if (auto const* block = containing_block(); block && is<Painting::PaintableWithLines>(*block)) {
        static_cast<Painting::PaintableWithLines const&>(*block).for_each_fragment([&](auto& fragment) {
            position = fragment.absolute_rect().location();
            return IterationDecision::Break;
        });
    }

    return position;
}

}
