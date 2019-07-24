#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/TreeNode.h>

class Node;

enum class Display {
    None,
    Block,
    Inline,
};

class StyledNode : public TreeNode<StyledNode> {
public:
    static NonnullRefPtr<StyledNode> create(const Node& node)
    {
        return adopt(*new StyledNode(&node));
    }
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

    template<typename Callback>
    inline void for_each_property(Callback callback) const
    {
        for (auto& it : m_property_values)
            callback(it.key, *it.value);
    }

    void set_property(const String& name, NonnullRefPtr<StyleValue> value)
    {
        m_property_values.set(name, move(value));
    }

    Optional<NonnullRefPtr<StyleValue>> property(const String& name) const
    {
        auto it = m_property_values.find(name);
        if (it == m_property_values.end())
            return {};
        return it->value;
    }

    Display display() const;

protected:
    explicit StyledNode(const Node*);

private:
    const Node* m_node { nullptr };
    HashMap<String, NonnullRefPtr<StyleValue>> m_property_values;
};
