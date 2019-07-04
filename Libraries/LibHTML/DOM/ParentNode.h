#pragma once

#include <LibHTML/DOM/Node.h>

class ParentNode : public Node {
public:
    template<typename F> void for_each_child(F) const;
    template<typename F> void for_each_child(F);

protected:
    explicit ParentNode(NodeType type)
        : Node(type)
    {
    }
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
