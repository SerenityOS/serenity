/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/NonnullRefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Weakable.h>

namespace Web {

template<typename T>
class TreeNode : public Weakable<T> {
public:
    void ref()
    {
        ASSERT(m_ref_count);
        ++m_ref_count;
    }

    void unref()
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
                child->unref();
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

    int child_count() const
    {
        int count = 0;
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

    bool is_ancestor_of(const TreeNode&) const;

    void prepend_child(NonnullRefPtr<T> node);
    void append_child(NonnullRefPtr<T> node, bool notify = true);
    void insert_before(NonnullRefPtr<T> node, RefPtr<T> child, bool notify = true);
    NonnullRefPtr<T> remove_child(NonnullRefPtr<T> node);
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

    bool is_before(const T& other) const
    {
        if (this == &other)
            return false;
        for (auto* node = this; node; node = node->next_in_pre_order()) {
            if (node == &other)
                return true;
        }
        return false;
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

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback)
    {
        if (is<U>(static_cast<const T&>(*this))) {
            if (callback(static_cast<U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_subtree_of_type<U>(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename U, typename Callback>
    IterationDecision for_each_in_subtree_of_type(Callback callback) const
    {
        if (is<U>(static_cast<const T&>(*this))) {
            if (callback(static_cast<const U&>(*this)) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (child->template for_each_in_subtree_of_type<U>(callback) == IterationDecision::Break)
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
                callback(downcast<U>(*node));
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
                return &downcast<U>(*sibling);
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
                return &downcast<U>(*sibling);
        }
        return nullptr;
    }

    template<typename U>
    const U* first_child_of_type() const
    {
        return const_cast<TreeNode*>(this)->template first_child_of_type<U>();
    }

    template<typename U>
    U* first_child_of_type()
    {
        for (auto* child = first_child(); child; child = child->next_sibling()) {
            if (is<U>(*child))
                return &downcast<U>(*child);
        }
        return nullptr;
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
                return &downcast<U>(*ancestor);
        }
        return nullptr;
    }

protected:
    TreeNode() { }

private:
    int m_ref_count { 1 };
    T* m_parent { nullptr };
    T* m_first_child { nullptr };
    T* m_last_child { nullptr };
    T* m_next_sibling { nullptr };
    T* m_previous_sibling { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> TreeNode<T>::remove_child(NonnullRefPtr<T> node)
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

    node->removed_from(static_cast<T&>(*this));

    node->unref();

    static_cast<T*>(this)->children_changed();

    return node;
}

template<typename T>
inline void TreeNode<T>::append_child(NonnullRefPtr<T> node, bool notify)
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
    if (notify)
        node->inserted_into(static_cast<T&>(*this));
    (void)node.leak_ref();

    if (notify)
        static_cast<T*>(this)->children_changed();
}

template<typename T>
inline void TreeNode<T>::insert_before(NonnullRefPtr<T> node, RefPtr<T> child, bool notify)
{
    if (!child)
        return append_child(move(node), notify);

    ASSERT(!node->m_parent);
    ASSERT(child->parent() == this);

    if (!static_cast<T*>(this)->is_child_allowed(*node))
        return;

    node->m_previous_sibling = child->m_previous_sibling;
    node->m_next_sibling = child;

    if (m_first_child == child)
        m_first_child = node;

    node->m_parent = static_cast<T*>(this);
    if (notify)
        node->inserted_into(static_cast<T&>(*this));
    (void)node.leak_ref();

    if (notify)
        static_cast<T*>(this)->children_changed();
}

template<typename T>
inline void TreeNode<T>::prepend_child(NonnullRefPtr<T> node)
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
    node->inserted_into(static_cast<T&>(*this));
    (void)node.leak_ref();

    static_cast<T*>(this)->children_changed();
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

}
