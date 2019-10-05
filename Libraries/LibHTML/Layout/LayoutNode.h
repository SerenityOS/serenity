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

    BoxModelMetrics& box_model() { return m_style; }
    const BoxModelMetrics& box_model() const { return m_style; }

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

    const StyleProperties& style() const
    {
        if (m_style_properties)
            return *m_style_properties;
        return parent()->style();
    }

    void inserted_into(LayoutNode&) {}
    void removed_from(LayoutNode&) {}

    virtual void split_into_lines(LayoutBlock& container);

protected:
    explicit LayoutNode(const Node*, RefPtr<StyleProperties>);

private:
    const Node* m_node { nullptr };

    RefPtr<StyleProperties> m_style_properties;
    BoxModelMetrics m_style;
    Rect m_rect;
    bool m_inline { false };
};
