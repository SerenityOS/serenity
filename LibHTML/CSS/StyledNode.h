#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/AKString.h>
#include <LibHTML/TreeNode.h>
#include <LibHTML/CSS/StyleValue.h>

class Node;

class StyledNode : public TreeNode<StyledNode> {
public:
    ~StyledNode();

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

protected:
    explicit StyledNode(const Node*);

private:
    const Node* m_node { nullptr };
    HashMap<String, NonnullRefPtr<StyleValue>> m_property_values;
};
