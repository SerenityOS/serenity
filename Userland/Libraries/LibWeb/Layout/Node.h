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
    // Normal layout.
    // - We use the containing block's used width.
    // - Content flows into the available space, line breaks inserted where necessary.
    Normal,

    // MinContent layout is used for discovering the min-content intrinsic size of a box.
    // - We act as if the containing block has 0 used width.
    // - Every line-breaking opportunity is taken.
    MinContent,

    // MaxContent layout is used for discovering the max-content intrinsic size of a box.
    // - We act as if the containing block has infinite used width.
    // - Only forced line-breaking opportunities are taken.
    MaxContent,
};

class Node : public TreeNode<Node> {
public:
    virtual ~Node();

    bool is_anonymous() const { return !m_dom_node; }
    const DOM::Node* dom_node() const { return m_dom_node; }
    DOM::Node* dom_node() { return m_dom_node; }

    Painting::Paintable* paintable() { return m_paintable; }
    Painting::Paintable const* paintable() const { return m_paintable; }
    void set_paintable(RefPtr<Painting::Paintable>);

    virtual RefPtr<Painting::Paintable> create_paintable() const;

    DOM::Document& document() { return m_document; }
    const DOM::Document& document() const { return m_document; }

    HTML::BrowsingContext const& browsing_context() const;
    HTML::BrowsingContext& browsing_context();

    InitialContainingBlock const& root() const;
    InitialContainingBlock& root();

    bool is_root_element() const;

    String class_name() const;
    String debug_description() const;

    bool has_style() const { return m_has_style; }

    virtual bool can_have_children() const { return true; }

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    bool is_inline_block() const;

    bool is_out_of_flow(FormattingContext const&) const;

    // These are used to optimize hot is<T> variants for some classes where dynamic_cast is too slow.
    virtual bool is_box() const { return false; }
    virtual bool is_block_container() const { return false; }
    virtual bool is_break_node() const { return false; }
    virtual bool is_text_node() const { return false; }
    virtual bool is_initial_containing_block_box() const { return false; }
    virtual bool is_svg_box() const { return false; }
    virtual bool is_svg_geometry_box() const { return false; }
    virtual bool is_label() const { return false; }
    virtual bool is_replaced_box() const { return false; }
    virtual bool is_list_item_marker_box() const { return false; }

    template<typename T>
    bool fast_is() const = delete;

    bool is_floating() const;
    bool is_positioned() const;
    bool is_absolutely_positioned() const;
    bool is_fixed_position() const;

    bool is_flex_item() const { return m_is_flex_item; }
    void set_flex_item(bool b) { m_is_flex_item = b; }

    BlockContainer const* containing_block() const;
    BlockContainer* containing_block() { return const_cast<BlockContainer*>(const_cast<Node const*>(this)->containing_block()); }

    bool establishes_stacking_context() const;

    bool can_contain_boxes_with_position_absolute() const;

    Gfx::Font const& font() const;
    const CSS::ImmutableComputedValues& computed_values() const;
    float line_height() const;

    NodeWithStyle* parent();
    NodeWithStyle const* parent() const;

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

protected:
    Node(DOM::Document&, DOM::Node*);

private:
    friend class NodeWithStyle;

    NonnullRefPtr<DOM::Document> m_document;
    RefPtr<DOM::Node> m_dom_node;
    RefPtr<Painting::Paintable> m_paintable;

    bool m_inline { false };
    bool m_has_style { false };
    bool m_visible { true };
    bool m_children_are_inline { false };
    SelectionState m_selection_state { SelectionState::None };

    bool m_is_flex_item { false };
};

class NodeWithStyle : public Node {
public:
    virtual ~NodeWithStyle() override = default;

    const CSS::ImmutableComputedValues& computed_values() const { return static_cast<const CSS::ImmutableComputedValues&>(m_computed_values); }

    void apply_style(const CSS::StyleProperties&);

    Gfx::Font const& font() const { return *m_font; }
    float line_height() const { return m_line_height; }
    Vector<CSS::BackgroundLayerData> const& background_layers() const { return computed_values().background_layers(); }
    const CSS::ImageStyleValue* list_style_image() const { return m_list_style_image; }

    NonnullRefPtr<NodeWithStyle> create_anonymous_wrapper() const;

    bool has_definite_height() const { return m_has_definite_height; }
    bool has_definite_width() const { return m_has_definite_width; }

    void set_has_definite_height(bool b) { m_has_definite_height = b; }
    void set_has_definite_width(bool b) { m_has_definite_width = b; }

    void did_insert_into_layout_tree(CSS::StyleProperties const&);

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
    BoxModelMetrics const& box_model() const { return m_box_model; }

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

inline Gfx::Font const& Node::font() const
{
    if (m_has_style)
        return static_cast<NodeWithStyle const*>(this)->font();
    return parent()->font();
}

inline const CSS::ImmutableComputedValues& Node::computed_values() const
{
    if (m_has_style)
        return static_cast<NodeWithStyle const*>(this)->computed_values();
    return parent()->computed_values();
}

inline float Node::line_height() const
{
    if (m_has_style)
        return static_cast<NodeWithStyle const*>(this)->line_height();
    return parent()->line_height();
}

inline NodeWithStyle const* Node::parent() const
{
    return static_cast<NodeWithStyle const*>(TreeNode<Node>::parent());
}

inline NodeWithStyle* Node::parent()
{
    return static_cast<NodeWithStyle*>(TreeNode<Node>::parent());
}

}
