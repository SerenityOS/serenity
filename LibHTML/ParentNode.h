#pragma once

#include <LibHTML/Node.h>

class ParentNode : public Node {
public:
    void append_child(Retained<Node>);

    Node* first_child() { return m_first_child; }
    Node* last_child() { return m_last_child; }

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

template<typename F>
inline void ParentNode::for_each_child(F func)
{
    for (auto* node = first_child(); node; node = node->next_sibling()) {
        func(*node);
    }
}

