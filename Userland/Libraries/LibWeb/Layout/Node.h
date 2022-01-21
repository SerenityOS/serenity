/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibGfx/Rect.h>
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BoxModelMetrics.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/TreeNode.h>

namespace Web::Layout {

enum class LayoutMode {
    Default,
    AllPossibleLineBreaks,
    OnlyRequiredLineBreaks,
};

enum class PaintPhase {
    Background,
    Border,
    Foreground,
    FocusOutline,
    Overlay,
};

struct HitTestResult {
    RefPtr<Node> layout_node;
    int index_in_node { 0 };

    enum InternalPosition {
        None,
        Before,
        Inside,
        After,
    };
    InternalPosition internal_position { None };
};

enum class HitTestType {
    Exact,      // Exact matches only
    TextCursor, // Clicking past the right/bottom edge of text will still hit the text
};

class Node : public TreeNode<Node> {
public:
    virtual ~Node();

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const;

    bool is_anonymous() const { return !m_dom_node; }
    const DOM::Node* dom_node() const { return m_dom_node; }
    DOM::Node* dom_node() { return m_dom_node; }

    DOM::Document& document() { return m_document; }
    const DOM::Document& document() const { return m_document; }

    HTML::BrowsingContext const& browsing_context() const;
    HTML::BrowsingContext& browsing_context();

    const InitialContainingBlock& root() const;
    InitialContainingBlock& root();

    bool is_root_element() const;

    String class_name() const;

    bool has_style() const { return m_has_style; }

    virtual bool can_have_children() const { return true; }

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    bool is_inline_block() const;

    virtual bool wants_mouse_events() const { return false; }

    virtual void handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    virtual void handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    virtual void handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);
    virtual bool handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y);

    virtual void before_children_paint(PaintContext&, PaintPhase) {};
    virtual void paint(PaintContext&, PaintPhase) = 0;
    virtual void paint_fragment(PaintContext&, const LineBoxFragment&, PaintPhase) const { }
    virtual void after_children_paint(PaintContext&, PaintPhase) {};

    // These are used to optimize hot is<T> variants for some classes where dynamic_cast is too slow.
    virtual bool is_box() const { return false; }
    virtual bool is_block_container() const { return false; }
    virtual bool is_break_node() const { return false; }
    virtual bool is_text_node() const { return false; }
    virtual bool is_initial_containing_block_box() const { return false; }
    virtual bool is_svg_box() const { return false; }
    virtual bool is_svg_path_box() const { return false; }
    virtual bool is_label() const { return false; }

    template<typename T>
    bool fast_is() const = delete;

    bool is_floating() const;
    bool is_positioned() const;
    bool is_absolutely_positioned() const;
    bool is_fixed_position() const;

    bool is_flex_item() const { return m_is_flex_item; }
    void set_flex_item(bool b) { m_is_flex_item = b; }

    const BlockContainer* containing_block() const;
    BlockContainer* containing_block() { return const_cast<BlockContainer*>(const_cast<const Node*>(this)->containing_block()); }

    bool establishes_stacking_context() const;

    bool can_contain_boxes_with_position_absolute() const;

    const Gfx::Font& font() const;
    const CSS::ImmutableComputedValues& computed_values() const;

    NodeWithStyle* parent();
    const NodeWithStyle* parent() const;

    void inserted_into(Node&) { }
    void removed_from(Node&) { }
    void children_changed() { }

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible) { m_visible = visible; }

    virtual void set_needs_display();

    bool children_are_inline() const { return m_children_are_inline; }
    void set_children_are_inline(bool value) { m_children_are_inline = value; }

    Gfx::FloatPoint box_type_agnostic_position() const;

    enum class SelectionState {
        None,        // No selection
        Start,       // Selection starts in this Node
        End,         // Selection ends in this Node
        StartAndEnd, // Selection starts and ends in this Node
        Full,        // Selection starts before and ends after this Node
    };

    SelectionState selection_state() const { return m_selection_state; }
    void set_selection_state(SelectionState state) { m_selection_state = state; }

    template<typename Callback>
    void for_each_child_in_paint_order(Callback callback) const
    {
        for_each_child([&](auto& child) {
            if (is<Box>(child) && verify_cast<Box>(child).stacking_context())
                return;
            if (!child.is_positioned())
                callback(child);
        });
        for_each_child([&](auto& child) {
            if (is<Box>(child) && verify_cast<Box>(child).stacking_context())
                return;
            if (child.is_positioned())
                callback(child);
        });
    }

protected:
    Node(DOM::Document&, DOM::Node*);

private:
    friend class NodeWithStyle;

    NonnullRefPtr<DOM::Document> m_document;
    RefPtr<DOM::Node> m_dom_node;

    bool m_inline { false };
    bool m_has_style { false };
    bool m_visible { true };
    bool m_children_are_inline { false };
    SelectionState m_selection_state { SelectionState::None };

    bool m_is_flex_item { false };
};

class NodeWithStyle : public Node {
public:
    virtual ~NodeWithStyle() override { }

    const CSS::ImmutableComputedValues& computed_values() const { return static_cast<const CSS::ImmutableComputedValues&>(m_computed_values); }

    void apply_style(const CSS::StyleProperties&);

    const Gfx::Font& font() const { return *m_font; }
    float line_height() const { return m_line_height; }
    Vector<CSS::BackgroundLayerData> const& background_layers() const { return computed_values().background_layers(); }
    const CSS::ImageStyleValue* list_style_image() const { return m_list_style_image; }

    NonnullRefPtr<NodeWithStyle> create_anonymous_wrapper() const;

    bool has_definite_height() const { return m_has_definite_height; }
    bool has_definite_width() const { return m_has_definite_width; }

protected:
    NodeWithStyle(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    NodeWithStyle(DOM::Document&, DOM::Node*, CSS::ComputedValues);

private:
    CSS::ComputedValues m_computed_values;
    RefPtr<Gfx::Font> m_font;
    float m_line_height { 0 };
    RefPtr<CSS::ImageStyleValue> m_list_style_image;

    bool m_has_definite_height { false };
    bool m_has_definite_width { false };
};

class NodeWithStyleAndBoxModelMetrics : public NodeWithStyle {
public:
    BoxModelMetrics& box_model() { return m_box_model; }
    const BoxModelMetrics& box_model() const { return m_box_model; }

protected:
    NodeWithStyleAndBoxModelMetrics(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
        : NodeWithStyle(document, node, move(style))
    {
    }

    NodeWithStyleAndBoxModelMetrics(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
        : NodeWithStyle(document, node, move(computed_values))
    {
    }

private:
    BoxModelMetrics m_box_model;
};

inline const Gfx::Font& Node::font() const
{
    if (m_has_style)
        return static_cast<const NodeWithStyle*>(this)->font();
    return parent()->font();
}

inline const CSS::ImmutableComputedValues& Node::computed_values() const
{
    if (m_has_style)
        return static_cast<const NodeWithStyle*>(this)->computed_values();
    return parent()->computed_values();
}

inline const NodeWithStyle* Node::parent() const
{
    return static_cast<const NodeWithStyle*>(TreeNode<Node>::parent());
}

inline NodeWithStyle* Node::parent()
{
    return static_cast<NodeWithStyle*>(TreeNode<Node>::parent());
}

}
