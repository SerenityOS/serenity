#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibDraw/Rect.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/Layout/BoxModelMetrics.h>
#include <LibHTML/RenderingContext.h>
#include <LibHTML/TreeNode.h>

class Document;
class Element;
class LayoutBlock;
class LayoutNode;
class LayoutNodeWithStyle;
class LineBoxFragment;
class Node;

struct HitTestResult {
    RefPtr<LayoutNode> layout_node;
};

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    BoxModelMetrics& box_model() { return m_box_metrics; }
    const BoxModelMetrics& box_model() const { return m_box_metrics; }

    virtual HitTestResult hit_test(const Point&) const;

    bool is_anonymous() const { return !m_node; }
    const Node* node() const { return m_node; }

    const Document& document() const;

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

    bool is_inline() const { return m_inline; }
    void set_inline(bool b) { m_inline = b; }

    virtual void layout();
    virtual void render(RenderingContext&);

    const LayoutBlock* containing_block() const;

    virtual LayoutNode& inline_wrapper() { return *this; }

    const StyleProperties& style() const;

    const LayoutNodeWithStyle* parent() const;

    void inserted_into(LayoutNode&) {}
    void removed_from(LayoutNode&) {}

    virtual void split_into_lines(LayoutBlock& container);

protected:
    explicit LayoutNode(const Node*);

private:
    friend class LayoutNodeWithStyle;

    const Node* m_node { nullptr };

    BoxModelMetrics m_box_metrics;
    Rect m_rect;
    bool m_inline { false };
    bool m_has_style { false };
};

class LayoutNodeWithStyle : public LayoutNode {
public:
    virtual ~LayoutNodeWithStyle() override {}

    const StyleProperties& style() const { return m_style; }

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
