#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibDraw/Rect.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/Layout/ComputedStyle.h>
#include <LibHTML/RenderingContext.h>
#include <LibHTML/TreeNode.h>

class Node;
class LayoutBlock;

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    ComputedStyle& style() { return m_style; }
    const ComputedStyle& style() const { return m_style; }

    bool is_anonymous() const { return !m_node; }
    const Node* node() const { return m_node; }

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
    virtual bool is_inline() const { return false; }

    virtual void layout();
    virtual void render(RenderingContext&);

    const LayoutBlock* containing_block() const;

    virtual LayoutNode& inline_wrapper() { return *this; }

    const StyleProperties& style_properties() const { return m_style_properties; }

protected:
    explicit LayoutNode(const Node*, StyleProperties&&);

private:
    const Node* m_node { nullptr };

    StyleProperties m_style_properties;
    ComputedStyle m_style;
    Rect m_rect;
};
