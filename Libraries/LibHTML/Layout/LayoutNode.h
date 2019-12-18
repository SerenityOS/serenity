#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibDraw/FloatRect.h>
#include <LibDraw/Rect.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/Layout/BoxModelMetrics.h>
#include <LibHTML/Layout/LayoutPosition.h>
#include <LibHTML/RenderingContext.h>
#include <LibHTML/TreeNode.h>

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

    virtual HitTestResult hit_test(const Point&) const;

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

    FloatPoint box_type_agnostic_position() const;

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
