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
    virtual ~Paintable();

    [[nodiscard]] bool is_visible() const;
    [[nodiscard]] bool is_positioned() const;
    [[nodiscard]] bool is_fixed_position() const { return layout_node().is_fixed_position(); }
    [[nodiscard]] bool is_absolutely_positioned() const { return layout_node().is_absolutely_positioned(); }
    [[nodiscard]] bool is_floating() const { return layout_node().is_floating(); }
    [[nodiscard]] bool is_inline() const { return layout_node().is_inline(); }
    [[nodiscard]] CSS::Display display() const { return layout_node().display(); }

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

    StackingContext* stacking_context() { return m_stacking_context; }
    StackingContext const* stacking_context() const { return m_stacking_context; }
    void set_stacking_context(NonnullOwnPtr<StackingContext>);
    StackingContext* enclosing_stacking_context();

    void invalidate_stacking_context();

    virtual void before_paint(PaintContext&, PaintPhase) const { }
    virtual void after_paint(PaintContext&, PaintPhase) const { }

    virtual void paint(PaintContext&, PaintPhase) const { }

    virtual void before_children_paint(PaintContext&, PaintPhase) const { }
    virtual void after_children_paint(PaintContext&, PaintPhase) const { }

    virtual void apply_scroll_offset(PaintContext&, PaintPhase) const { }
    virtual void reset_scroll_offset(PaintContext&, PaintPhase) const { }

    virtual void apply_clip_overflow_rect(PaintContext&, PaintPhase) const { }
    virtual void clear_clip_overflow_rect(PaintContext&, PaintPhase) const { }

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const;

    virtual bool wants_mouse_events() const { return false; }

    virtual bool forms_unconnected_subtree() const { return false; }

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

    [[nodiscard]] HTML::BrowsingContext const& browsing_context() const;
    [[nodiscard]] HTML::BrowsingContext& browsing_context();

    void set_needs_display() const { const_cast<Layout::Node&>(*m_layout_node).set_needs_display(); }

    Layout::Box const* containing_block() const
    {
        if (!m_containing_block.has_value())
            m_containing_block = m_layout_node->containing_block();
        return *m_containing_block;
    }

    template<typename T>
    bool fast_is() const = delete;

    [[nodiscard]] virtual bool is_paintable_box() const { return false; }
    [[nodiscard]] virtual bool is_paintable_with_lines() const { return false; }
    [[nodiscard]] virtual bool is_inline_paintable() const { return false; }

    DOM::Document const& document() const { return layout_node().document(); }
    DOM::Document& document() { return layout_node().document(); }

    PaintableBox const* nearest_scrollable_ancestor_within_stacking_context() const;

    CSSPixelPoint box_type_agnostic_position() const;

protected:
    explicit Paintable(Layout::Node const&);

    virtual void visit_edges(Cell::Visitor&) override;

private:
    JS::GCPtr<DOM::Node> m_dom_node;
    JS::NonnullGCPtr<Layout::Node const> m_layout_node;
    JS::NonnullGCPtr<HTML::BrowsingContext> m_browsing_context;
    Optional<JS::GCPtr<Layout::Box const>> mutable m_containing_block;

    OwnPtr<StackingContext> m_stacking_context;
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
inline bool Paintable::fast_is<PaintableBox>() const { return is_paintable_box(); }

template<>
inline bool Paintable::fast_is<PaintableWithLines>() const { return is_paintable_with_lines(); }

}
