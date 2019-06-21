#pragma once

#include <LibHTML/DOM/Node.h>

class ParentNode : public Node {
public:
    void append_child(NonnullRefPtr<Node>);

    Node* first_child() { return m_first_child; }
    Node* last_child() { return m_last_child; }
    const Node* first_child() const { return m_first_child; }
    const Node* last_child() const { return m_last_child; }

    template<typename F> void for_each_child(F) const;
    template<typename F> void for_each_child(F);

protected:
    explicit ParentNode(NodeType type)
        : Node(type)
    {
    }

private:
    Node* m_first_child { nullptr };
    Node* m_last_child { nullptr };
};

template<typename Callback>
inline void ParentNode::for_each_child(Callback callback) const
{
    for (auto* node = first_child(); node; node = node->next_sibling())
        callback(*node);
}

template<typename Callback>
inline void ParentNode::for_each_child(Callback callback)
{
    for (auto* node = first_child(); node; node = node->next_sibling())
        callback(*node);
}
