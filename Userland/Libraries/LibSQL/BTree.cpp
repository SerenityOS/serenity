/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>

#include <LibSQL/BTree.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>

namespace SQL {

BTree::BTree(Heap &heap, IndexDef &index, u32 pointer)
    : m_heap(heap)
    , m_index(index)
    , m_root(nullptr)
    , m_pointer(pointer)
{
}

NonnullRefPtr<IndexDef> BTree::index_def() const
{
    return m_index;
}

BTreeIterator BTree::begin()
{
    if (!m_root) {
        initialize_root();
    }
    VERIFY(m_root);
    return BTreeIterator(m_root, -1);
}

BTreeIterator BTree::end()
{
    return BTreeIterator(nullptr, -1);
}

void BTree::initialize_root()
{
    if (m_pointer) {
        if (m_pointer < m_heap.size()) {
            auto buffer = read_block(m_pointer);
            size_t offset = 0;
            m_root = make<TreeNode>(*this, nullptr, m_pointer, buffer, offset);
        } else {
            m_root = make<TreeNode>(*this, nullptr, m_pointer);
        }
    } else {
        m_pointer = new_record_pointer();
        m_root = make<TreeNode>(*this, nullptr, m_pointer);
    }
    if (on_new_root) {
        on_new_root();
    }
}

TreeNode* BTree::new_root() {
    m_pointer = new_record_pointer();
    m_root = make<TreeNode>(*this, nullptr, m_root.leak_ptr(), m_pointer);
    add_to_write_ahead_log(*m_root);
    if (on_new_root) {
        on_new_root();
    }
    return m_root;
}

bool BTree::insert(Key const& key) {
    if (!m_root) {
        initialize_root();
    }
    VERIFY(m_root);
    return m_root->insert(key);
}

Optional<u32> BTree::get(Key& key)
{
    if (!m_root) {
        initialize_root();
    }
    VERIFY(m_root);
    return m_root->get(key);
}

BTreeIterator BTree::find(Key const& key)
{
    if (!m_root) {
        initialize_root();
    }
    VERIFY(m_root);
    for (auto node = m_root->node_for(key); node; node = node->up()) {
        for (auto ix = 0u; ix < node->size(); ix++) {
            auto match = (*node)[ix].match(key);
            if (match == 0) {
                return BTreeIterator(node, (int) ix);
            } else if (match > 0) {
                return end();
            }
        }
    }
    return end();
}

BTreeIterator BTree::find(Key&& key)
{
    if (!m_root) {
        initialize_root();
    }
    return find(key);
}

u32 BTree::new_record_pointer()
{
    return m_heap.new_record_pointer();
}

ByteBuffer BTree::read_block(u32 block)
{
    auto ret = m_heap.read_block(block);
    if (ret.is_error()) {
        warnln("Error reading block {}: {}", block, ret.error());
        VERIFY_NOT_REACHED();
    }
    return ret.value();
}

bool BTree::duplicates_allowed() const
{
    return m_index->unique();
}

void BTree::add_to_write_ahead_log(TreeNode& node)
{
    ByteBuffer buffer;
    node.serialize(buffer);
    m_heap.add_to_wal(node.pointer(), buffer);
}

void BTree::list_tree()
{
    if (!m_root) {
        initialize_root();
    }
    m_root->list_node(0);
}

}
