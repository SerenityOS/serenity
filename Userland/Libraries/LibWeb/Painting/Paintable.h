/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/LineBox.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Painting {

enum class PaintPhase {
    Background,
    Border,
    Foreground,
    FocusOutline,
    Overlay,
};

struct HitTestResult {
    NonnullRefPtr<Painting::Paintable> paintable;
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

class Paintable : public RefCounted<Paintable> {
    AK_MAKE_NONMOVABLE(Paintable);
    AK_MAKE_NONCOPYABLE(Paintable);

public:
    virtual ~Paintable() = default;

    Paintable const* first_child() const;
    Paintable const* next_sibling() const;

    template<typename U, typename Callback>
    IterationDecision for_each_in_inclusive_subtree_of_type(Callback callback) const
    {
        if (is<U>(*this)) {
            if (callback(static_cast<const U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    virtual void paint(PaintContext&, PaintPhase) const { }
    virtual void before_children_paint(PaintContext&, PaintPhase) const { }
    virtual void after_children_paint(PaintContext&, PaintPhase) const { }

    virtual Optional<HitTestResult> hit_test(Gfx::FloatPoint const&, HitTestType) const;

    virtual bool wants_mouse_events() const { return false; }

    enum class DispatchEventOfSameName {
        Yes,
        No,
    };
    // When these methods return true, the DOM event with the same name will be
    // dispatch at the mouse_event_target if it returns a valid DOM::Node, or
    // the layout node's associated DOM node if it doesn't.
    virtual DispatchEventOfSameName handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers);
    virtual DispatchEventOfSameName handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers);
    virtual DispatchEventOfSameName handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const&, unsigned buttons, unsigned modifiers);
    virtual DOM::Node* mouse_event_target() const { return nullptr; }

    virtual bool handle_mousewheel(Badge<EventHandler>, Gfx::IntPoint const&, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y);

    Layout::Node const& layout_node() const { return m_layout_node; }
    Layout::Node& layout_node() { return const_cast<Layout::Node&>(m_layout_node); }

    DOM::Node* dom_node() { return layout_node().dom_node(); }
    DOM::Node const* dom_node() const { return layout_node().dom_node(); }

    auto const& computed_values() const { return m_layout_node.computed_values(); }

    HTML::BrowsingContext const& browsing_context() const { return m_layout_node.browsing_context(); }
    HTML::BrowsingContext& browsing_context() { return layout_node().browsing_context(); }

    void set_needs_display() const { const_cast<Layout::Node&>(m_layout_node).set_needs_display(); }

    Layout::BlockContainer const* containing_block() const
    {
        if (!m_containing_block.has_value())
            m_containing_block = const_cast<Layout::Node&>(m_layout_node).containing_block();
        return *m_containing_block;
    }

    template<typename T>
    bool fast_is() const = delete;

protected:
    explicit Paintable(Layout::Node const& layout_node)
        : m_layout_node(layout_node)
    {
    }

private:
    Layout::Node const& m_layout_node;
    Optional<Layout::BlockContainer*> mutable m_containing_block;
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
inline bool Paintable::fast_is<PaintableBox>() const { return m_layout_node.is_box(); }

}
