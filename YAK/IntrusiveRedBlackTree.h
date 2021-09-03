/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/RedBlackTree.h>

namespace YAK {

template<Integral K>
class IntrusiveRedBlackTreeNode;

template<Integral K, typename V, IntrusiveRedBlackTreeNode<K> V::*member>
class IntrusiveRedBlackTree final : public BaseRedBlackTree<K> {

public:
    IntrusiveRedBlackTree() = default;
    virtual ~IntrusiveRedBlackTree() override
    {
        clear();
    }

    using BaseTree = BaseRedBlackTree<K>;
    using TreeNode = IntrusiveRedBlackTreeNode<K>;

    V* find(K key)
    {
        auto* node = static_cast<TreeNode*>(BaseTree::find(this->m_root, key));
        if (!node)
            return nullptr;
        return node_to_value(*node);
    }

    V* find_largest_not_above(K key)
    {
        auto* node = static_cast<TreeNode*>(BaseTree::find_largest_not_above(this->m_root, key));
        if (!node)
            return nullptr;
        return node_to_value(*node);
    }

    void insert(V& value)
    {
        auto& node = value.*member;
        BaseTree::insert(&node);
        node.m_in_tree = true;
    }

    template<typename ElementType>
    class BaseIterator {
    public:
        BaseIterator() = default;
        bool operator!=(const BaseIterator& other) const { return m_node != other.m_node; }
        BaseIterator& operator++()
        {
            if (!m_node)
                return *this;
            m_prev = m_node;
            // the complexity is O(logn) for each successor call, but the total complexity for all elements comes out to O(n), meaning the amortized cost for a single call is O(1)
            m_node = static_cast<TreeNode*>(BaseTree::successor(m_node));
            return *this;
        }
        BaseIterator& operator--()
        {
            if (!m_prev)
                return *this;
            m_node = m_prev;
            m_prev = static_cast<TreeNode*>(BaseTree::predecessor(m_prev));
            return *this;
        }
        ElementType& operator*()
        {
            VERIFY(m_node);
            return *node_to_value(*m_node);
        }
        ElementType* operator->()
        {
            VERIFY(m_node);
            return node_to_value(*m_node);
        }
        [[nodiscard]] bool is_end() const { return !m_node; }
        [[nodiscard]] bool is_begin() const { return !m_prev; }
        [[nodiscard]] auto key() const { return m_node->key; }

    private:
        friend class IntrusiveRedBlackTree;
        explicit BaseIterator(TreeNode* node, TreeNode* prev = nullptr)
            : m_node(node)
            , m_prev(prev)
        {
        }
        TreeNode* m_node { nullptr };
        TreeNode* m_prev { nullptr };
    };

    using Iterator = BaseIterator<V>;
    Iterator begin() { return Iterator(static_cast<TreeNode*>(this->m_minimum)); }
    Iterator end() { return {}; }
    Iterator begin_from(K key) { return Iterator(static_cast<TreeNode*>(BaseTree::find(this->m_root, key))); }

    using ConstIterator = BaseIterator<const V>;
    ConstIterator begin() const { return ConstIterator(static_cast<TreeNode*>(this->m_minimum)); }
    ConstIterator end() const { return {}; }
    ConstIterator begin_from(K key) const { return ConstIterator(static_cast<TreeNode*>(BaseTree::find(this->m_rootF, key))); }

    bool remove(K key)
    {
        auto* node = static_cast<TreeNode*>(BaseTree::find(this->m_root, key));
        if (!node)
            return false;

        BaseTree::remove(node);

        node->right_child = nullptr;
        node->left_child = nullptr;
        node->m_in_tree = false;

        return true;
    }

    void clear()
    {
        clear_nodes(static_cast<TreeNode*>(this->m_root));
        this->m_root = nullptr;
        this->m_minimum = nullptr;
        this->m_size = 0;
    }

private:
    static void clear_nodes(TreeNode* node)
    {
        if (!node)
            return;
        clear_nodes(static_cast<TreeNode*>(node->right_child));
        node->right_child = nullptr;
        clear_nodes(static_cast<TreeNode*>(node->left_child));
        node->left_child = nullptr;
        node->m_in_tree = false;
    }

    static V* node_to_value(TreeNode& node)
    {
        return bit_cast<V*>(bit_cast<u8*>(&node) - bit_cast<u8*>(member));
    }
};

template<Integral K>
class IntrusiveRedBlackTreeNode : public BaseRedBlackTree<K>::Node {
public:
    IntrusiveRedBlackTreeNode(K key)
        : BaseRedBlackTree<K>::Node(key)
    {
    }

    ~IntrusiveRedBlackTreeNode()
    {
        VERIFY(!is_in_tree());
    }

    [[nodiscard]] bool is_in_tree() const
    {
        return m_in_tree;
    }

private:
    template<Integral TK, typename V, IntrusiveRedBlackTreeNode<TK> V::*member>
    friend class IntrusiveRedBlackTree;
    bool m_in_tree { false };
};

}

using YAK::IntrusiveRedBlackTree;
using YAK::IntrusiveRedBlackTreeNode;
