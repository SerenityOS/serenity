#pragma once

#include <AK/Assertions.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Weakable.h>

template<typename T>
class TreeNode : public Weakable<T> {
public:
    void ref()
    {
        ASSERT(m_ref_count);
        ++m_ref_count;
    }

    void deref()
    {
        ASSERT(m_ref_count);
        if (!--m_ref_count) {
            if (m_next_sibling)
                m_next_sibling->m_previous_sibling = m_previous_sibling;
            if (m_previous_sibling)
                m_previous_sibling->m_next_sibling = m_next_sibling;
            T* next_child;
            for (auto* child = m_first_child; child; child = next_child) {
                next_child = child->m_next_sibling;
                child->m_parent = nullptr;
                child->deref();
            }
            delete static_cast<T*>(this);
        }
    }
    int ref_count() const { return m_ref_count; }

    T* parent() { return m_parent; }
    const T* parent() const { return m_parent; }

    bool has_children() const { return m_first_child; }
    T* next_sibling() { return m_next_sibling; }
    T* previous_sibling() { return m_previous_sibling; }
    T* first_child() { return m_first_child; }
    T* last_child() { return m_last_child; }
    const T* next_sibling() const { return m_next_sibling; }
    const T* previous_sibling() const { return m_previous_sibling; }
    const T* first_child() const { return m_first_child; }
    const T* last_child() const { return m_last_child; }

    bool is_ancestor_of(const TreeNode&) const;

    void prepend_child(NonnullRefPtr<T> node, bool call_inserted_into = true);
    void append_child(NonnullRefPtr<T> node, bool call_inserted_into = true);
    NonnullRefPtr<T> remove_child(NonnullRefPtr<T> node, bool call_removed_from = true);
    void donate_all_children_to(T& node);

    bool is_child_allowed(const T&) const { return true; }

    T* next_in_pre_order()
    {
        if (first_child())
            return first_child();
        T* node;
        if (!(node = next_sibling())) {
            node = parent();
            while (node && !node->next_sibling())
                node = node->parent();
            if (node)
                node = node->next_sibling();
        }
        return node;
    }

    const T* next_in_pre_order() const
    {
        return const_cast<TreeNode*>(this)->next_in_pre_order();
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback) const
    {
        if (callback(static_cast<const T&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback)
    {
        if (callback(static_cast<T&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

protected:
    TreeNode() {}

private:
    int m_ref_count { 1 };
    T* m_parent { nullptr };
    T* m_first_child { nullptr };
    T* m_last_child { nullptr };
    T* m_next_sibling { nullptr };
    T* m_previous_sibling { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> TreeNode<T>::remove_child(NonnullRefPtr<T> node, bool call_removed_from)
{
    ASSERT(node->m_parent == this);

    if (m_first_child == node)
        m_first_child = node->m_next_sibling;

    if (m_last_child == node)
        m_last_child = node->m_previous_sibling;

    if (node->m_next_sibling)
        node->m_next_sibling->m_previous_sibling = node->m_previous_sibling;

    if (node->m_previous_sibling)
        node->m_previous_sibling->m_next_sibling = node->m_next_sibling;

    node->m_next_sibling = nullptr;
    node->m_previous_sibling = nullptr;
    node->m_parent = nullptr;

    if (call_removed_from)
        node->removed_from(static_cast<T&>(*this));

    node->deref();

    return node;
}

template<typename T>
inline void TreeNode<T>::append_child(NonnullRefPtr<T> node, bool call_inserted_into)
{
    ASSERT(!node->m_parent);

    if (!static_cast<T*>(this)->is_child_allowed(*node))
        return;

    if (m_last_child)
        m_last_child->m_next_sibling = node.ptr();
    node->m_previous_sibling = m_last_child;
    node->m_parent = static_cast<T*>(this);
    m_last_child = node.ptr();
    if (!m_first_child)
        m_first_child = m_last_child;
    if (call_inserted_into)
        node->inserted_into(static_cast<T&>(*this));
    (void)node.leak_ref();
}

template<typename T>
inline void TreeNode<T>::prepend_child(NonnullRefPtr<T> node, bool call_inserted_into)
{
    ASSERT(!node->m_parent);

    if (!static_cast<T*>(this)->is_child_allowed(*node))
        return;

    if (m_first_child)
        m_first_child->m_previous_sibling = node.ptr();
    node->m_next_sibling = m_first_child;
    node->m_parent = static_cast<T*>(this);
    m_first_child = node.ptr();
    if (!m_last_child)
        m_last_child = m_first_child;
    if (call_inserted_into)
        node->inserted_into(static_cast<T&>(*this));
    (void)node.leak_ref();
}

template<typename T>
inline void TreeNode<T>::donate_all_children_to(T& node)
{
    for (T* child = m_first_child; child != nullptr;) {
        T* next_child = child->m_next_sibling;

        child->m_parent = nullptr;
        child->m_next_sibling = nullptr;
        child->m_previous_sibling = nullptr;

        node.append_child(adopt(*child));
        child = next_child;
    }

    m_first_child = nullptr;
    m_last_child = nullptr;
}

template<typename T>
inline bool TreeNode<T>::is_ancestor_of(const TreeNode<T>& other) const
{
    for (auto* ancestor = other.parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor == this)
            return true;
    }
    return false;
}
