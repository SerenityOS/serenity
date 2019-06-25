#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibHTML/Layout/LayoutStyle.h>
#include <LibHTML/TreeNode.h>
#include <SharedGraphics/Rect.h>

class Node;

class LayoutNode : public TreeNode<LayoutNode> {
public:
    virtual ~LayoutNode();

    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    LayoutStyle& style() { return m_style; }
    const LayoutStyle& style() const { return m_style; }

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

    virtual void layout();

protected:
    explicit LayoutNode(const Node*);

private:
    const Node* m_node { nullptr };

    LayoutStyle m_style;
    Rect m_rect;
};
