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
#include <LibWeb/Layout/BoxModelMetrics.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/RenderingContext.h>
#include <LibWeb/TreeNode.h>

namespace Web {

class Document;
class Element;
class LayoutBlock;
class LayoutDocument;
class LayoutNode;
class LayoutNodeWithStyle;
class LineBoxFragment;
class Node;

struct HitTestResult {
    RefPtr<LayoutNode> layout_node;
    int index_in_node { 0 };
};

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    virtual HitTestResult hit_test(const Gfx::Point&) const;

    bool is_anonymous() const { return !m_node; }
    const Node* node() const { return m_node; }

    Document& document();
    const Document& document() const;

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

    virtual const char* class_name() const { return "LayoutNode"; }
    virtual bool is_text() const { return false; }
    virtual bool is_block() const { return false; }
    virtual bool is_replaced() const { return false; }
    virtual bool is_widget() const { return false; }
    virtual bool is_image() const { return false; }
    virtual bool is_box() const { return false; }
    virtual bool is_table() const { return false; }
    virtual bool is_table_row() const { return false; }
    virtual bool is_table_cell() const { return false; }
    bool has_style() const { return m_has_style; }

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    virtual void layout();
    virtual void render(RenderingContext&);

    const LayoutBlock* containing_block() const;

    virtual LayoutNode& inline_wrapper() { return *this; }

    const StyleProperties& style() const;

    LayoutNodeWithStyle* parent();
    const LayoutNodeWithStyle* parent() const;

    void inserted_into(LayoutNode&) {}
    void removed_from(LayoutNode&) {}

    virtual void split_into_lines(LayoutBlock& container);

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible) { m_visible = visible; }

    virtual void set_needs_display();

    bool children_are_inline() const { return m_children_are_inline; }
    void set_children_are_inline(bool value) { m_children_are_inline = value; }

    template<typename U>
    const U* next_sibling_of_type() const;

    template<typename U>
    U* next_sibling_of_type();

    template<typename T>
    const T* first_child_of_type() const;

    template<typename T>
    T* first_child_of_type();

    template<typename T>
    const T* first_ancestor_of_type() const;

    template<typename T>
    T* first_ancestor_of_type();

    Gfx::FloatPoint box_type_agnostic_position() const;

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
    virtual ~LayoutNodeWithStyle() override {}

    const StyleProperties& style() const { return m_style; }
    void set_style(const StyleProperties& style) { m_style = style; }

protected:
    explicit LayoutNodeWithStyle(const Node* node, NonnullRefPtr<StyleProperties> style)
        : LayoutNode(node)
        , m_style(move(style))
    {
        m_has_style = true;
    }

private:
    NonnullRefPtr<StyleProperties> m_style;
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

inline const StyleProperties& LayoutNode::style() const
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

template<>
inline bool is<LayoutNodeWithStyle>(const LayoutNode& node)
{
    return node.has_style();
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

}
