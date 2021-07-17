/*
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Iterator.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/Traits.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>

namespace AK {

template<typename T, size_t inline_capacity>
requires(!IsRvalueReference<T>) class TreeNode : public Weakable<TreeNode<T, inline_capacity>> {
private:
    Vector<NonnullOwnPtr<TreeNode<T, inline_capacity>>, inline_capacity> m_children;
    T m_value;

public:
    TreeNode()
        : m_children()
    {
    }

    TreeNode(TreeNode<T, inline_capacity> const& other)
        : m_value(other.m_value)
    {
        // We copy the tree recursively. This copies all the values.
        for (auto it = other.m_children.begin(); it != other.m_children.end(); it++) {
            // Make a new OwnPtr from the TreeNode
            NonnullOwnPtr<TreeNode<T, inline_capacity>> val = make<TreeNode<T, inline_capacity>>(*(*it));
            // Add it
            m_children.append(move(val));
        }
    }

    TreeNode(TreeNode<T, inline_capacity>&& other)
        : m_children(move(other.m_children))
        , m_value(move(other.m_value))
    {
    }

    T const& value() const
    {
        return m_value;
    }

    void set(T value)
    {
        m_value = value;
    }

    size_t num_children() const
    {
        return m_children.size();
    }

    size_t size() const
    {
        size_t size = 1; // Count ourselves

        // Add childrens recursively
        for (auto it = m_children.begin(); it != m_children.end(); it++) {
            size += (*it)->size();
        }

        return size;
    }

    size_t is_empty() const
    {
        return m_children.is_empty();
    }

    void clear()
    {
        for (auto it = m_children.begin(); it != m_children.end(); it++) {
            (*it)->clear();
        }
        m_children.clear();
    }

    TreeNode<T, inline_capacity>* add_child(T value)
    {
        NonnullOwnPtr<TreeNode<T, inline_capacity>> val = make<TreeNode<T, inline_capacity>>();
        val->set(value);
        auto ret = val.ptr();
        m_children.append(move(val));
        return ret;
    }

    void remove_at(size_t i) const
    {
        m_children.remove(i);
    }

    TreeNode<T, inline_capacity>* child_at(size_t i)
    {
        return m_children.at(i).ptr();
    }

    TreeNode<T, inline_capacity> const* child_at(size_t i) const
    {
        return m_children.at(i).ptr();
    }

    ~TreeNode()
    {
        clear();
    }

    template<typename TUnaryPredicate>
    Optional<size_t> find_first_index_if(TUnaryPredicate&& pred)
    {
        auto it = AK::find_if(m_children.begin(), m_children.end(), [&](auto& v) { return pred(v->value()); });
        if (it != m_children.end()) {
            return it.index();
        }
        return {};
    }

    Optional<size_t> find_first_index(T value)
    {
        return find_first_index_if([&](T v) { return Traits<T>::equals(value, v); });
    }

    template<typename TUnaryPredicate>
    TreeNode<T, inline_capacity>* find_if(TUnaryPredicate&& pred)
    {
        auto index = find_first_index_if(pred);
        if (index.has_value()) {
            return child_at(index.value());
        }
        return nullptr;
    }

    TreeNode<T, inline_capacity>* find(T value)
    {
        return find_if([&](T v) { return Traits<T>::equals(value, v); });
    }

    template<typename TUnaryPredicate>
    TreeNode<T, inline_capacity>* search_if(TUnaryPredicate&& pred)
    {
        // Check if we have the value
        if (pred(m_value)) {
            return this;
        }

        // Check if a child has the value
        for (auto it = m_children.begin(); it != m_children.end(); it++) {
            auto output = (*it)->search_if(pred);
            if (output != nullptr) {
                return output;
            }
        }

        return nullptr;
    }

    TreeNode<T, inline_capacity>* search(T value)
    {
        return search_if([&](T v) { return Traits<T>::equals(value, v); });
    }

    bool operator==(TreeNode<T, inline_capacity> const& other) const
    {
        if (m_value != other.m_value)
            return false;

        if (num_children() != other.num_children())
            return false;

        for (auto it = m_children.begin(), it2 = other.m_children.begin(); it != m_children.end() && it2 != other.m_children.end();) {
            if (*(*it) != *(*it2))
                return false;

            it++;
            it2++;
        }

        return true;
    }

    template<typename LessThan>
    void sort(LessThan less_than)
    {
        auto lt = [&](auto& a, auto& b) {
            return less_than(a->value(), b->value());
        };
        quick_sort(m_children.begin(), m_children.end(), move(lt));

        for (auto it = m_children.begin(); it != m_children.end(); it++) {
            (*it)->sort(less_than);
        }
    }

    using ValueType = T;
};

template<typename T, size_t inline_capacity = 0>
requires(!IsRvalueReference<T>) class Tree {
private:
    TreeNode<T, inline_capacity> m_root;

public:
    Tree()
        : m_root()
    {
    }

    Tree(Tree<T, inline_capacity> const& other)
        : m_root(other.m_root)
    {
    }

    Tree(Tree<T, inline_capacity>&& other)
        : m_root(move(other.m_root))
    {
    }

    TreeNode<T, inline_capacity>& root()
    {
        return m_root;
    }

    ~Tree()
    {
        root().clear();
    }

    bool operator==(Tree<T, inline_capacity> const& other) const
    {
        return other.m_root == m_root;
    }

    using ValueType = T;
};

}

using AK::Tree;
using AK::TreeNode;
