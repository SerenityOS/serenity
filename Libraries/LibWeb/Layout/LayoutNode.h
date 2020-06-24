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
#include <AK/Vector.h>
#include <LibGfx/FloatRect.h>
#include <LibGfx/Rect.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BoxModelMetrics.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Layout/LayoutStyle.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/TreeNode.h>

namespace Web {

template<typename T>
inline bool is(const LayoutNode&)
{
    return false;
}

template<typename T>
inline bool is(const LayoutNode* node)
{
    return !node || is<T>(*node);
}

template<>
inline bool is<LayoutNode>(const LayoutNode&)
{
    return true;
}

template<typename T>
inline const T& to(const LayoutNode& node)
{
    ASSERT(is<T>(node));
    return static_cast<const T&>(node);
}

template<typename T>
inline T* to(LayoutNode* node)
{
    ASSERT(is<T>(node));
    return static_cast<T*>(node);
}

template<typename T>
inline const T* to(const LayoutNode* node)
{
    ASSERT(is<T>(node));
    return static_cast<const T*>(node);
}

template<typename T>
inline T& to(LayoutNode& node)
{
    ASSERT(is<T>(node));
    return static_cast<T&>(node);
}

struct HitTestResult {
    RefPtr<LayoutNode> layout_node;
    int index_in_node { 0 };
};

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    virtual HitTestResult hit_test(const Gfx::IntPoint&) const;

    bool is_anonymous() const { return !m_node; }
    const Node* node() const { return m_node; }
    Node* node() { return const_cast<Node*>(m_node); }

    Document& document();
    const Document& document() const;

    const Frame& frame() const;
    Frame& frame();

    const LayoutDocument& root() const;
    LayoutDocument& root();

    template<typename Callback>
    inline void for_each_child(Callback callback) const
    {
        for (auto* node = first_child(); node; node = node->next_sibling())
            callback(*node);
    }

    template<typename Callback>
    inline void for_each_child(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling())
            callback(*node);
    }

    template<typename T, typename Callback>
    inline void for_each_child_of_type(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling()) {
            if (!is<T>(node))
                continue;
            callback(to<T>(*node));
        }
    }

    template<typename T, typename Callback>
    inline void for_each_child_of_type(Callback callback) const
    {
        for (auto* node = first_child(); node; node = node->next_sibling()) {
            if (!is<T>(node))
                continue;
            callback(to<T>(*node));
        }
    }

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
    bool has_style() const { return m_has_style; }

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    bool is_inline_block() const { return is_inline() && is_block(); }

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
        Overlay,
    };
    virtual void paint(PaintContext&, PaintPhase);

    bool is_absolutely_positioned() const;
    bool is_fixed_position() const;

    const LayoutBlock* containing_block() const;

    bool can_contain_boxes_with_position_absolute() const;

    virtual LayoutNode& inline_wrapper() { return *this; }

    const StyleProperties& specified_style() const;
    const ImmutableLayoutStyle& style() const;
    CSS::Position position() const;
    CSS::TextAlign text_align() const;

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

    template<typename U>
    const U* next_sibling_of_type() const;

    template<typename U>
    U* next_sibling_of_type();

    template<typename U>
    const U* previous_sibling_of_type() const;

    template<typename U>
    U* previous_sibling_of_type();

    template<typename T>
    const T* first_child_of_type() const;

    template<typename T>
    T* first_child_of_type();

    template<typename T>
    const T* first_ancestor_of_type() const;

    template<typename T>
    T* first_ancestor_of_type();

    Gfx::FloatPoint box_type_agnostic_position() const;

    float font_size() const;

protected:
    explicit LayoutNode(const Node*);

private:
    friend class LayoutNodeWithStyle;

    const Node* m_node { nullptr };

    bool m_inline { false };
    bool m_has_style { false };
    bool m_visible { true };
    bool m_children_are_inline { false };
};

class LayoutNodeWithStyle : public LayoutNode {
public:
    virtual ~LayoutNodeWithStyle() override { }

    const StyleProperties& specified_style() const { return m_specified_style; }
    void set_specified_style(const StyleProperties& style) { m_specified_style = style; }

    const ImmutableLayoutStyle& style() const { return static_cast<const ImmutableLayoutStyle&>(m_style); }

    CSS::Position position() const { return m_position; }
    CSS::TextAlign text_align() const { return m_text_align; }

protected:
    explicit LayoutNodeWithStyle(const Node*, NonnullRefPtr<StyleProperties>);

private:
    void apply_style(const StyleProperties&);

    LayoutStyle m_style;

    NonnullRefPtr<StyleProperties> m_specified_style;
    CSS::Position m_position;
    CSS::TextAlign m_text_align;
};

class LayoutNodeWithStyleAndBoxModelMetrics : public LayoutNodeWithStyle {
public:
    BoxModelMetrics& box_model() { return m_box_model; }
    const BoxModelMetrics& box_model() const { return m_box_model; }

protected:
    LayoutNodeWithStyleAndBoxModelMetrics(const Node* node, NonnullRefPtr<StyleProperties> style)
        : LayoutNodeWithStyle(node, move(style))
    {
    }

private:
    BoxModelMetrics m_box_model;
};

inline const StyleProperties& LayoutNode::specified_style() const
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

inline CSS::Position LayoutNode::position() const
{
    if (m_has_style)
        return static_cast<const LayoutNodeWithStyle*>(this)->position();
    return parent()->position();
}

inline CSS::TextAlign LayoutNode::text_align() const
{
    if (m_has_style)
        return static_cast<const LayoutNodeWithStyle*>(this)->text_align();
    return parent()->text_align();
}

inline const LayoutNodeWithStyle* LayoutNode::parent() const
{
    return static_cast<const LayoutNodeWithStyle*>(TreeNode<LayoutNode>::parent());
}

inline LayoutNodeWithStyle* LayoutNode::parent()
{
    return static_cast<LayoutNodeWithStyle*>(TreeNode<LayoutNode>::parent());
}

template<typename T>
inline const T* LayoutNode::next_sibling_of_type() const
{
    for (auto* sibling = next_sibling(); sibling; sibling = sibling->next_sibling()) {
        if (is<T>(*sibling))
            return &to<T>(*sibling);
    }
    return nullptr;
}

template<typename T>
inline T* LayoutNode::next_sibling_of_type()
{
    for (auto* sibling = next_sibling(); sibling; sibling = sibling->next_sibling()) {
        if (is<T>(*sibling))
            return &to<T>(*sibling);
    }
    return nullptr;
}

template<typename T>
inline const T* LayoutNode::previous_sibling_of_type() const
{
    for (auto* sibling = previous_sibling(); sibling; sibling = sibling->previous_sibling()) {
        if (is<T>(*sibling))
            return &to<T>(*sibling);
    }
    return nullptr;
}

template<typename T>
inline T* LayoutNode::previous_sibling_of_type()
{
    for (auto* sibling = previous_sibling(); sibling; sibling = sibling->previous_sibling()) {
        if (is<T>(*sibling))
            return &to<T>(*sibling);
    }
    return nullptr;
}

template<typename T>
inline const T* LayoutNode::first_child_of_type() const
{
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<T>(*child))
            return &to<T>(*child);
    }
    return nullptr;
}

template<typename T>
inline T* LayoutNode::first_child_of_type()
{
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<T>(*child))
            return &to<T>(*child);
    }
    return nullptr;
}

template<typename T>
inline const T* LayoutNode::first_ancestor_of_type() const
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (is<T>(*ancestor))
            return &to<T>(*ancestor);
    }
    return nullptr;
}

template<typename T>
inline T* LayoutNode::first_ancestor_of_type()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (is<T>(*ancestor))
            return &to<T>(*ancestor);
    }
    return nullptr;
}

template<>
inline bool is<LayoutNodeWithStyle>(const LayoutNode& node)
{
    return node.has_style();
}

}
