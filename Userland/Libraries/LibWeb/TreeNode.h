/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/NonnullRefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Weakable.h>
#include <LibWeb/Forward.h>

namespace Web {

template<typename T>
class TreeNode : public Weakable<T> {
public:
    void ref()
    {
        VERIFY(!m_in_removed_last_ref);
        if constexpr (!IsBaseOf<DOM::Node, T>) {
            // NOTE: DOM::Document is allowed to survive with 0 ref count, if one of its descendant nodes are alive.
            VERIFY(m_ref_count);
        }
        ++m_ref_count;
    }

    void unref()
    {
        VERIFY(!m_in_removed_last_ref);
        VERIFY(m_ref_count);
        if (!--m_ref_count) {
            if constexpr (IsBaseOf<DOM::Node, T>) {
                m_in_removed_last_ref = true;
                static_cast<T*>(this)->removed_last_ref();
            } else {
                delete static_cast<T*>(this);
            }
            return;
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

    size_t child_count() const
    {
        size_t count = 0;
        for (auto* child = first_child(); child; child = child->next_sibling())
            ++count;
        return count;
    }

    T* child_at_index(int index)
    {
        int count = 0;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (count == index)
                return child;
            ++count;
        }
        return nullptr;
    }

    const T* child_at_index(int index) const
    {
        return const_cast<TreeNode*>(this)->child_at_index(index);
    }

    // https://dom.spec.whatwg.org/#concept-tree-index
    size_t index() const
    {
        // The index of an object is its number of preceding siblings, or 0 if it has none.
        size_t index = 0;
        for (auto* node = previous_sibling(); node; node = node->previous_sibling())
            ++index;
        return index;
    }

    Optional<size_t> index_of_child(const T& search_child)
    {
        VERIFY(search_child.parent() == this);
        size_t index = 0;
        auto* child = first_child();
        VERIFY(child);

        do {
            if (child == &search_child)
                return index;
            index++;
        } while (child && (child = child->next_sibling()));
        return {};
    }

    template<typename ChildType>
    Optional<size_t> index_of_child(const T& search_child)
    {
        VERIFY(search_child.parent() == this);
        size_t index = 0;
        auto* child = first_child();
        VERIFY(child);

        do {
            if (!is<ChildType>(child))
                continue;
            if (child == &search_child)
                return index;
            index++;
        } while (child && (child = child->next_sibling()));
        return {};
    }

    bool is_ancestor_of(const TreeNode&) const;
    bool is_inclusive_ancestor_of(const TreeNode&) const;
    bool is_descendant_of(const TreeNode&) const;
    bool is_inclusive_descendant_of(const TreeNode&) const;

    bool is_following(TreeNode const&) const;

    void append_child(NonnullRefPtr<T> node);
    void prepend_child(NonnullRefPtr<T> node);
    void insert_before(NonnullRefPtr<T> node, RefPtr<T> child);
    void remove_child(NonnullRefPtr<T> node);

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

    T* next_in_pre_order(T const* stay_within)
    {
        if (first_child())
            return first_child();

        T* node = static_cast<T*>(this);
        T* next = nullptr;
        while (!(next = node->next_sibling())) {
            node = node->parent();
            if (!node || node == stay_within)
                return nullptr;
        }
        return next;
    }

    T const* next_in_pre_order() const
    {
        return const_cast<TreeNode*>(this)->next_in_pre_order();
    }

    T const* next_in_pre_order(T const* stay_within) const
    {
        return const_cast<TreeNode*>(this)->next_in_pre_order(stay_within);
    }

    T* previous_in_pre_order()
    {
        if (auto* node = previous_sibling()) {
            while (node->last_child())
                node = node->last_child();

            return node;
        }

        return parent();
    }

    T const* previous_in_pre_order() const
    {
        return const_cast<TreeNode*>(this)->previous_in_pre_order();
    }

    bool is_before(T const& other) const
    {
        if (this == &other)
            return false;
        for (auto* node = this; node; node = node->next_in_pre_order()) {
            if (node == &other)
                return true;
        }
        return false;
    }

    // https://dom.spec.whatwg.org/#concept-tree-preceding (Object A is 'typename U' and Object B is 'this')
    template<typename U>
    bool has_preceding_node_of_type_in_tree_order() const
    {
        for (auto* node = previous_in_pre_order(); node; node = node->previous_in_pre_order()) {
            if (is<U>(node))
                return true;
        }
        return false;
    }

    // https://dom.spec.whatwg.org/#concept-tree-following (Object A is 'typename U' and Object B is 'this')
    template<typename U>
    bool has_following_node_of_type_in_tree_order() const
    {
        for (auto* node = next_in_pre_order(); node; node = node->next_in_pre_order()) {
            if (is<U>(node))
                return true;
        }
        return false;
    }

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback) const
    {
        if (callback(static_cast<const T&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback)
    {
        if (callback(static_cast<T&>(*this)) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_inclusive_subtree_of_type(Callback callback)
    {
        if (is<U>(static_cast<const T&>(*this))) {
            if (callback(static_cast<U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_inclusive_subtree_of_type(Callback callback) const
    {
        if (is<U>(static_cast<const T&>(*this))) {
            if (callback(static_cast<const U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback)
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback)
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_inclusive_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    void for_each_child(Callback callback) const
    {
        return const_cast<TreeNode*>(this)->template for_each_child(move(callback));
    }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling())
            callback(*node);
    }

    template<typename U, typename Callback>
    void for_each_child_of_type(Callback callback)
    {
        for (auto* node = first_child(); node; node = node->next_sibling()) {
            if (is<U>(node))
                callback(verify_cast<U>(*node));
        }
    }

    template<typename U, typename Callback>
    void for_each_child_of_type(Callback callback) const
    {
        return const_cast<TreeNode*>(this)->template for_each_child_of_type<U>(move(callback));
    }

    template<typename U>
    const U* next_sibling_of_type() const
    {
        return const_cast<TreeNode*>(this)->template next_sibling_of_type<U>();
    }

    template<typename U>
    inline U* next_sibling_of_type()
    {
        for (auto* sibling = next_sibling(); sibling; sibling = sibling->next_sibling()) {
            if (is<U>(*sibling))
                return &verify_cast<U>(*sibling);
        }
        return nullptr;
    }

    template<typename U>
    const U* previous_sibling_of_type() const
    {
        return const_cast<TreeNode*>(this)->template previous_sibling_of_type<U>();
    }

    template<typename U>
    U* previous_sibling_of_type()
    {
        for (auto* sibling = previous_sibling(); sibling; sibling = sibling->previous_sibling()) {
            if (is<U>(*sibling))
                return &verify_cast<U>(*sibling);
        }
        return nullptr;
    }

    template<typename U>
    const U* first_child_of_type() const
    {
        return const_cast<TreeNode*>(this)->template first_child_of_type<U>();
    }

    template<typename U>
    const U* last_child_of_type() const
    {
        return const_cast<TreeNode*>(this)->template last_child_of_type<U>();
    }

    template<typename U>
    U* first_child_of_type()
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (is<U>(*child))
                return &verify_cast<U>(*child);
        }
        return nullptr;
    }

    template<typename U>
    U* last_child_of_type()
    {
        for (auto* child = last_child(); child; child = child->previous_sibling()) {
            if (is<U>(*child))
                return &verify_cast<U>(*child);
        }
        return nullptr;
    }

    template<typename U>
    bool has_child_of_type() const
    {
        return first_child_of_type<U>() != nullptr;
    }

    template<typename U>
    const U* first_ancestor_of_type() const
    {
        return const_cast<TreeNode*>(this)->template first_ancestor_of_type<U>();
    }

    template<typename U>
    U* first_ancestor_of_type()
    {
        for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
            if (is<U>(*ancestor))
                return &verify_cast<U>(*ancestor);
        }
        return nullptr;
    }

    bool is_parent_of(T const& other) const
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (&other == child)
                return true;
        }
        return false;
    }

    ~TreeNode()
    {
        VERIFY(!m_parent);
        T* next_child = nullptr;
        for (auto* child = m_first_child; child; child = next_child) {
            next_child = child->m_next_sibling;
            child->m_parent = nullptr;
            child->unref();
        }
    }

protected:
    TreeNode() = default;

    bool m_deletion_has_begun { false };
    bool m_in_removed_last_ref { false };

private:
    int m_ref_count { 1 };
    T* m_parent { nullptr };
    T* m_first_child { nullptr };
    T* m_last_child { nullptr };
    T* m_next_sibling { nullptr };
    T* m_previous_sibling { nullptr };
};

template<typename T>
inline void TreeNode<T>::remove_child(NonnullRefPtr<T> node)
{
    VERIFY(node->m_parent == this);

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

    node->unref();
}

template<typename T>
inline void TreeNode<T>::append_child(NonnullRefPtr<T> node)
{
    VERIFY(!node->m_parent);

    if (!static_cast<T*>(this)->is_child_allowed(*node))
        return;

    if (m_last_child)
        m_last_child->m_next_sibling = node.ptr();
    node->m_previous_sibling = m_last_child;
    node->m_parent = static_cast<T*>(this);
    m_last_child = node.ptr();
    if (!m_first_child)
        m_first_child = m_last_child;
    [[maybe_unused]] auto& rc = node.leak_ref();
}

template<typename T>
inline void TreeNode<T>::insert_before(NonnullRefPtr<T> node, RefPtr<T> child)
{
    if (!child)
        return append_child(move(node));

    VERIFY(!node->m_parent);
    VERIFY(child->parent() == this);

    node->m_previous_sibling = child->m_previous_sibling;
    node->m_next_sibling = child;

    if (child->m_previous_sibling)
        child->m_previous_sibling->m_next_sibling = node;

    if (m_first_child == child)
        m_first_child = node;

    child->m_previous_sibling = node;

    node->m_parent = static_cast<T*>(this);
    [[maybe_unused]] auto& rc = node.leak_ref();
}

template<typename T>
inline void TreeNode<T>::prepend_child(NonnullRefPtr<T> node)
{
    VERIFY(!node->m_parent);

    if (!static_cast<T*>(this)->is_child_allowed(*node))
        return;

    if (m_first_child)
        m_first_child->m_previous_sibling = node.ptr();
    node->m_next_sibling = m_first_child;
    node->m_parent = static_cast<T*>(this);
    m_first_child = node.ptr();
    if (!m_last_child)
        m_last_child = m_first_child;
    node->inserted_into(static_cast<T&>(*this));
    [[maybe_unused]] auto& rc = node.leak_ref();

    static_cast<T*>(this)->children_changed();
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

template<typename T>
inline bool TreeNode<T>::is_inclusive_ancestor_of(const TreeNode<T>& other) const
{
    return &other == this || is_ancestor_of(other);
}

template<typename T>
inline bool TreeNode<T>::is_descendant_of(const TreeNode<T>& other) const
{
    return other.is_ancestor_of(*this);
}

template<typename T>
inline bool TreeNode<T>::is_inclusive_descendant_of(const TreeNode<T>& other) const
{
    return other.is_inclusive_ancestor_of(*this);
}

// https://dom.spec.whatwg.org/#concept-tree-following
template<typename T>
inline bool TreeNode<T>::is_following(TreeNode<T> const& other) const
{
    // An object A is following an object B if A and B are in the same tree and A comes after B in tree order.
    for (auto* node = previous_in_pre_order(); node; node = node->previous_in_pre_order()) {
        if (node == &other)
            return true;
    }

    return false;
}

}
