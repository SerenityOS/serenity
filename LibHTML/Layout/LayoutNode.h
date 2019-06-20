#pragma once

#include <AK/Retained.h>
#include <AK/Vector.h>
#include <LibHTML/Layout/LayoutStyle.h>
#include <SharedGraphics/Rect.h>

class Node;

class LayoutNode {
public:
    virtual ~LayoutNode();

    void retain();
    void release();
    int retain_count() const { return m_retain_count; }

    const Rect& rect() const { return m_rect; }
    Rect& rect() { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    LayoutStyle& style() { return m_style; }
    const LayoutStyle& style() const { return m_style; }

    bool is_anonymous() const { return !m_node; }
    const Node* node() const { return m_node; }

    const LayoutNode* parent_layout_node() const { return m_parent_node; }

    LayoutNode* next_sibling() { return m_next_sibling; }
    LayoutNode* previous_sibling() { return m_previous_sibling; }
    LayoutNode* first_child() { return m_first_child; }
    LayoutNode* last_child() { return m_last_child; }
    const LayoutNode* next_sibling() const { return m_next_sibling; }
    const LayoutNode* previous_sibling() const { return m_previous_sibling; }
    const LayoutNode* first_child() const { return m_first_child; }
    const LayoutNode* last_child() const { return m_last_child; }

    bool has_children() const { return m_first_child; }

    void append_child(Retained<LayoutNode>);

    void set_next_sibling(LayoutNode* node) { m_next_sibling = node; }
    void set_previous_sibling(LayoutNode* node) { m_previous_sibling = node; }

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
    int m_retain_count { 1 };
    const Node* m_node { nullptr };
    LayoutNode* m_parent_node { nullptr };
    LayoutNode* m_first_child { nullptr };
    LayoutNode* m_last_child { nullptr };
    LayoutNode* m_next_sibling { nullptr };
    LayoutNode* m_previous_sibling { nullptr };
    LayoutStyle m_style;
    Rect m_rect;
};
