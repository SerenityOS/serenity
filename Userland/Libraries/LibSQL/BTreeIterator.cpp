/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/BTree.h>

namespace SQL {

BTreeIterator::BTreeIterator(TreeNode* node, int index)
    : m_current(node)
    , m_index(index)
{
    if (!node) {
        m_where = Where::End;
    } else {
        if (index < 0) {
            while (!node->is_leaf() && (node->size() != 0)) {
                node = node->down_node(0);
            }
            if (node->size() == 0) {
                m_where = Where::End;
                m_current = nullptr;
                m_index = -1;
            } else {
                m_where = Where::Valid;
                m_current = node;
                m_index = 0;
            }
        } else {
            VERIFY(m_index < (int)m_current->size());
        }
    }
}

int BTreeIterator::cmp(BTreeIterator const& other) const
{
    if (is_end())
        return (other.is_end()) ? 0 : 1;
    if (other.is_end())
        return -1;
    VERIFY(&other.m_current->tree() == &m_current->tree());
    VERIFY((m_current->size() > 0) && (other.m_current->size() > 0));
    if (&m_current != &other.m_current)
        return (*m_current)[m_current->size() - 1].compare((*(other.m_current))[0]);
    return (*m_current)[m_index].compare((*(other.m_current))[other.m_index]);
}

int BTreeIterator::cmp(Key const& other) const
{
    if (is_end())
        return 1;
    if (other.is_null())
        return -1;
    return key().compare(other);
}

BTreeIterator BTreeIterator::next() const
{
    if (is_end())
        return end();

    auto ix = m_index;
    auto node = m_current;
    if (ix < (int)(node->size() - 1)) {
        if (node->is_leaf()) {
            // We're in the middle of a leaf node. Next entry is
            // is the next entry of the node:
            return BTreeIterator(node, ix + 1);
        } else {
            /*
             * We're in the middle of a non-leaf node. The iterator's
             * next value is all the way down to the right, first entry.
             *
             *                  |
             *                  +--+--+--+--+
             *                  |  |##|  |  |
             *                  +--+--+--+--+
             *                 /   |  |  |   \
             *                        |
             *               +--+--+--+--+
             *               |  |  |  |  |
             *               +--+--+--+--+
             *              /
             * +--+--+--+--+
             * |++|  |  |  |
             * +--+--+--+--+
             */
            ix++;
            while (!node->is_leaf()) {
                node = node->down_node(ix);
                ix = 0;
            }
        }
        VERIFY(node->is_leaf() && (ix < (int)node->size()));
        return BTreeIterator(node, ix);
    }

    if (node->is_leaf()) {
        // We currently at the last entry of a leaf node. We need to check
        // one or more levels up until we end up in the "middle" of a node.
        // If one level up we're still at the end of the node, we need
        // to keep going up until we hit the root node. If we're at the
        // end of the root node, we reached the end of the btree.
        for (auto up = node->up(); up; up = node->up()) {
            for (size_t i = 0; i < up->size(); i++) {
                // One level up, try to find the entry with the current
                // node's pointer as the left pointer:
                if (up->down_pointer(i) == node->block_index())
                    // Found it. This is the iterator's next value:
                    return BTreeIterator(up, (int)i);
            }
            // We didn't find the m_current's pointer as a left node. So
            // it must be the right node all the way at the end and we need
            // to go one more level up:
            node = up;
        }
        // We reached the root node and we're still at the end of the node.
        // That means we're at the end of the btree.
        return end();
    }

    // If we're at the end of a non-leaf node, we need to follow the
    // right pointer down until we find a leaf:
    TreeNode* down;
    for (down = node->down_node(node->size()); !down->is_leaf(); down = down->down_node(0))
        ;
    return BTreeIterator(down, 0);
}

// FIXME Reverse iterating doesn't quite work; we don't recognize the
// end (which is really the beginning) of the tree.
BTreeIterator BTreeIterator::previous() const
{
    if (is_end())
        return end();

    auto node = m_current;
    auto ix = m_index;
    if (ix > 0) {
        if (node->is_leaf()) {
            // We're in the middle of a leaf node. Previous entry is
            // is the previous entry of the node:
            return BTreeIterator(node, ix - 1);
        } else {
            /*
             * We're in the middle of a non-leaf node. The iterator's
             * previous value is all the way down to the left, last entry.
             *
             *                  |
             *                  +--+--+--+--+
             *                  |  |  |##|  |
             *                  +--+--+--+--+
             *                 /   |  |  |   \
             *                        |
             *               +--+--+--+--+
             *               |  |  |  |  |
             *               +--+--+--+--+
             *                            \
             *                 +--+--+--+--+
             *                 |  |  |  |++|
             *                 +--+--+--+--+
             */
            while (!node->is_leaf()) {
                node = node->down_node(ix);
                ix = (int)node->size();
            }
        }
        VERIFY(node->is_leaf() && (ix <= (int)node->size()));
        return BTreeIterator(node, ix);
    }

    if (node->is_leaf()) {
        // We currently at the first entry of a leaf node. We need to check one
        // or more levels up until we end up in the "middle" of a node.
        // If one level up we're still at the start of the node, we need
        // to keep going up until we hit the root node. If we're at the
        // start of the root node, we reached the start of the btree.
        auto stash_current = node;
        for (auto up = node->up(); up; up = node->up()) {
            for (size_t i = up->size(); i > 0; i--) {
                // One level up, try to find the entry with the current
                // node's pointer as the right pointer:
                if (up->down_pointer(i) == node->block_index()) {
                    // Found it. This is the iterator's next value:
                    node = up;
                    ix = (int)i - 1;
                    return BTreeIterator(node, ix);
                }
            }
            // We didn't find the m_current's pointer as a right node. So
            // it must be the left node all the way at the start and we need
            // to go one more level up:
            node = up;
        }
        // We reached the root node and we're still at the start of the node.
        // That means we're at the start of the btree.
        return BTreeIterator(stash_current, 0);
    }

    // If we're at the start of a non-leaf node, we need to follow the
    // left pointer down until we find a leaf:
    TreeNode* down = node->down_node(0);
    while (!down->is_leaf())
        down = down->down_node(down->size());
    return BTreeIterator(down, down->size() - 1);
}

Key BTreeIterator::key() const
{
    if (is_end())
        return {};
    return (*m_current)[m_index];
}

bool BTreeIterator::update(Key const& new_value)
{
    if (is_end())
        return false;
    if ((cmp(new_value) == 0) && (key().block_index() == new_value.block_index()))
        return true;
    auto previous_iter = previous();
    auto next_iter = next();
    if (!m_current->tree().duplicates_allowed() && ((previous_iter == new_value) || (next_iter == new_value))) {
        return false;
    }
    if ((previous_iter > new_value) || (next_iter < new_value))
        return false;

    // We are friend of BTree and TreeNode. Don't know how I feel about that.
    m_current->m_entries[m_index] = new_value;
    m_current->tree().serializer().serialize_and_write(*m_current);
    return true;
}

BTreeIterator& BTreeIterator::operator=(BTreeIterator const& other)
{
    if (&other != this) {
        m_current = other.m_current;
        m_index = other.m_index;
        m_where = other.m_where;
    }
    return *this;
}

}
