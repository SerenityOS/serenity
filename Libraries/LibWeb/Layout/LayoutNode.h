/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibGfx/Rect.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BoxModelMetrics.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Layout/LayoutStyle.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/TreeNode.h>

namespace Web {

struct HitTestResult {
    RefPtr<LayoutNode> layout_node;
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

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const;

    bool is_anonymous() const { return !m_node; }
    const DOM::Node* node() const { return m_node; }
    DOM::Node* node() { return m_node; }

    DOM::Document& document() { return m_document; }
    const DOM::Document& document() const { return m_document; }

    const Frame& frame() const;
    Frame& frame();

    const LayoutDocument& root() const;
    LayoutDocument& root();

    virtual const char* class_name() const = 0;
    virtual bool is_root() const { return false; }
    virtual bool is_text() const { return false; }
    virtual bool is_block() const { return false; }
    virtual bool is_replaced() const { return false; }
    virtual bool is_widget() const { return false; }
    virtual bool is_frame() const { return false; }
    virtual bool is_image() const { return false; }
    virtual bool is_canvas() const { return false; }
    virtual bool is_box() const { return false; }
    virtual bool is_table() const { return false; }
    virtual bool is_table_row() const { return false; }
    virtual bool is_table_cell() const { return false; }
    virtual bool is_table_row_group() const { return false; }
    virtual bool is_break() const { return false; }
    virtual bool is_check_box() const { return false; }
    virtual bool is_button() const { return false; }
    bool has_style() const { return m_has_style; }

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    bool is_inline_block() const { return is_inline() && is_block(); }

    virtual bool wants_mouse_events() const { return false; }

    virtual void handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    virtual void handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    virtual void handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);

    enum class LayoutMode {
        Default,
        AllPossibleLineBreaks,
        OnlyRequiredLineBreaks,
    };

    virtual void layout(LayoutMode);

    enum class PaintPhase {
        Background,
        Border,
        Foreground,
        FocusOutline,
        Overlay,
    };

    virtual void before_children_paint(PaintContext&, PaintPhase) {};
    virtual void paint(PaintContext&, PaintPhase);
    virtual void after_children_paint(PaintContext&, PaintPhase) {};

    bool is_floating() const;
    bool is_absolutely_positioned() const;
    bool is_fixed_position() const;

    const LayoutBlock* containing_block() const;

    bool can_contain_boxes_with_position_absolute() const;

    virtual LayoutNode& inline_wrapper() { return *this; }

    const CSS::StyleProperties& specified_style() const;
    const ImmutableLayoutStyle& style() const;

    LayoutNodeWithStyle* parent();
    const LayoutNodeWithStyle* parent() const;

    void inserted_into(LayoutNode&) { }
    void removed_from(LayoutNode&) { }
    void children_changed() { }

    virtual void split_into_lines(LayoutBlock& container, LayoutMode);

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible) { m_visible = visible; }

    virtual void set_needs_display();

    bool children_are_inline() const { return m_children_are_inline; }
    void set_children_are_inline(bool value) { m_children_are_inline = value; }

    Gfx::FloatPoint box_type_agnostic_position() const;

    float font_size() const;

    enum class SelectionState {
        None,        // No selection
        Start,       // Selection starts in this LayoutNode
        End,         // Selection ends in this LayoutNode
        StartAndEnd, // Selection starts and ends in this LayoutNode
        Full,        // Selection starts before and ends after this LayoutNode
    };

    SelectionState selection_state() const { return m_selection_state; }
    void set_selection_state(SelectionState state) { m_selection_state = state; }

protected:
    LayoutNode(DOM::Document&, DOM::Node*);

private:
    friend class LayoutNodeWithStyle;

    DOM::Document& m_document;
    DOM::Node* m_node { nullptr };

    bool m_inline { false };
    bool m_has_style { false };
    bool m_visible { true };
    bool m_children_are_inline { false };
    SelectionState m_selection_state { SelectionState::None };
};

class LayoutNodeWithStyle : public LayoutNode {
public:
    virtual ~LayoutNodeWithStyle() override { }

    const CSS::StyleProperties& specified_style() const { return m_specified_style; }
    void set_specified_style(const CSS::StyleProperties& style) { m_specified_style = style; }

    const ImmutableLayoutStyle& style() const { return static_cast<const ImmutableLayoutStyle&>(m_style); }

    void apply_style(const CSS::StyleProperties&);

protected:
    LayoutNodeWithStyle(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);

private:
    LayoutStyle m_style;

    NonnullRefPtr<CSS::StyleProperties> m_specified_style;
    CSS::Position m_position;
};

class LayoutNodeWithStyleAndBoxModelMetrics : public LayoutNodeWithStyle {
public:
    BoxModelMetrics& box_model() { return m_box_model; }
    const BoxModelMetrics& box_model() const { return m_box_model; }

protected:
    LayoutNodeWithStyleAndBoxModelMetrics(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
        : LayoutNodeWithStyle(document, node, move(style))
    {
    }

private:
    BoxModelMetrics m_box_model;
};

inline const CSS::StyleProperties& LayoutNode::specified_style() const
{
    if (m_has_style)
        return static_cast<const LayoutNodeWithStyle*>(this)->specified_style();
    return parent()->specified_style();
}

inline const ImmutableLayoutStyle& LayoutNode::style() const
{
    if (m_has_style)
        return static_cast<const LayoutNodeWithStyle*>(this)->style();
    return parent()->style();
}

inline const LayoutNodeWithStyle* LayoutNode::parent() const
{
    return static_cast<const LayoutNodeWithStyle*>(TreeNode<LayoutNode>::parent());
}

inline LayoutNodeWithStyle* LayoutNode::parent()
{
    return static_cast<LayoutNodeWithStyle*>(TreeNode<LayoutNode>::parent());
}

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutNodeWithStyle)
static bool is_type(const Web::LayoutNode& node) { return node.has_style(); }
AK_END_TYPE_TRAITS()
