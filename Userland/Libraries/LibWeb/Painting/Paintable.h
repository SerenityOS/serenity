/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/LineBox.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Painting {

enum class TraversalDecision {
    Continue,
    SkipChildrenAndContinue,
    Break,
};

enum class PaintPhase {
    Background,
    Border,
    Foreground,
    Outline,
    Overlay,
};

struct HitTestResult {
    JS::Handle<Paintable> paintable;
    int index_in_node { 0 };

    enum InternalPosition {
        None,
        Before,
        Inside,
        After,
    };
    InternalPosition internal_position { None };

    DOM::Node* dom_node();
    DOM::Node const* dom_node() const;
};

enum class HitTestType {
    Exact,      // Exact matches only
    TextCursor, // Clicking past the right/bottom edge of text will still hit the text
};

class Paintable
    : public JS::Cell
    , public TreeNode<Paintable> {
    JS_CELL(Paintable, Cell);

public:
    virtual ~Paintable() = default;

    template<typename U, typename Callback>
    TraversalDecision for_each_in_inclusive_subtree_of_type(Callback callback) const
    {
        if (is<U>(*this)) {
            if (auto decision = callback(static_cast<U const&>(*this)); decision != TraversalDecision::Continue)
                return decision;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == TraversalDecision::Break)
                return TraversalDecision::Break;
        }
        return TraversalDecision::Continue;
    }

    template<typename U, typename Callback>
    TraversalDecision for_each_in_subtree_of_type(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == TraversalDecision::Break)
                return TraversalDecision::Break;
        }
        return TraversalDecision::Continue;
    }

    template<typename Callback>
    TraversalDecision for_each_in_inclusive_subtree(Callback callback) const
    {
        if (auto decision = callback(*this); decision != TraversalDecision::Continue)
            return decision;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == TraversalDecision::Break)
                return TraversalDecision::Break;
        }
        return TraversalDecision::Continue;
    }

    template<typename Callback>
    TraversalDecision for_each_in_subtree(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == TraversalDecision::Break)
                return TraversalDecision::Break;
        }
        return TraversalDecision::Continue;
    }

    virtual void paint(PaintContext&, PaintPhase) const { }

    virtual void before_children_paint(PaintContext&, PaintPhase) const { }
    virtual void after_children_paint(PaintContext&, PaintPhase) const { }

    virtual void apply_clip_overflow_rect(PaintContext&, PaintPhase) const { }
    virtual void clear_clip_overflow_rect(PaintContext&, PaintPhase) const { }

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const;

    virtual bool wants_mouse_events() const { return false; }

    enum class DispatchEventOfSameName {
        Yes,
        No,
    };
    // When these methods return true, the DOM event with the same name will be
    // dispatch at the mouse_event_target if it returns a valid DOM::Node, or
    // the layout node's associated DOM node if it doesn't.
    virtual DispatchEventOfSameName handle_mousedown(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers);
    virtual DispatchEventOfSameName handle_mouseup(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers);
    virtual DispatchEventOfSameName handle_mousemove(Badge<EventHandler>, CSSPixelPoint, unsigned buttons, unsigned modifiers);
    virtual DOM::Node* mouse_event_target() const { return nullptr; }

    virtual bool handle_mousewheel(Badge<EventHandler>, CSSPixelPoint, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y);

    Layout::Node const& layout_node() const { return m_layout_node; }
    Layout::Node& layout_node() { return const_cast<Layout::Node&>(*m_layout_node); }

    [[nodiscard]] JS::GCPtr<DOM::Node> dom_node();
    [[nodiscard]] JS::GCPtr<DOM::Node const> dom_node() const;
    void set_dom_node(JS::GCPtr<DOM::Node>);

    auto const& computed_values() const { return m_layout_node->computed_values(); }

    bool visible_for_hit_testing() const { return computed_values().pointer_events() != CSS::PointerEvents::None; }

    HTML::BrowsingContext const& browsing_context() const { return m_layout_node->browsing_context(); }
    HTML::BrowsingContext& browsing_context() { return layout_node().browsing_context(); }

    void set_needs_display() const { const_cast<Layout::Node&>(*m_layout_node).set_needs_display(); }

    Layout::Box const* containing_block() const
    {
        if (!m_containing_block.has_value())
            m_containing_block = m_layout_node->containing_block();
        return *m_containing_block;
    }

    template<typename T>
    bool fast_is() const = delete;

    StackingContext const* stacking_context_rooted_here() const;

protected:
    explicit Paintable(Layout::Node const& layout_node)
        : m_layout_node(layout_node)
    {
    }

    virtual void visit_edges(Cell::Visitor&) override;

private:
    JS::GCPtr<DOM::Node> m_dom_node;
    JS::NonnullGCPtr<Layout::Node const> m_layout_node;
    Optional<JS::GCPtr<Layout::Box const>> mutable m_containing_block;
};

inline DOM::Node* HitTestResult::dom_node()
{
    return paintable->dom_node();
}

inline DOM::Node const* HitTestResult::dom_node() const
{
    return paintable->dom_node();
}

template<>
inline bool Paintable::fast_is<PaintableBox>() const { return m_layout_node->is_box(); }

}
