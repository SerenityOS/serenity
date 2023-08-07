/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/BTree.h>
#include <LibSQL/Meta.h>

namespace SQL {

ErrorOr<NonnullRefPtr<BTree>> BTree::create(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, Block::Index block_index)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) BTree(serializer, descriptor, unique, block_index));
}

ErrorOr<NonnullRefPtr<BTree>> BTree::create(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, Block::Index block_index)
{
    return create(serializer, descriptor, true, block_index);
}

BTree::BTree(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, Block::Index block_index)
    : Index(serializer, descriptor, unique, block_index)
    , m_root(nullptr)
{
}

BTreeIterator BTree::begin()
{
    if (!m_root)
        initialize_root();
    VERIFY(m_root);
    return BTreeIterator(m_root, -1);
}

BTreeIterator BTree::end()
{
    return BTreeIterator(nullptr, -1);
}

void BTree::initialize_root()
{
    if (block_index()) {
        if (serializer().has_block(block_index())) {
            serializer().read_storage(block_index());
            m_root = serializer().make_and_deserialize<TreeNode>(*this, block_index());
        } else {
            m_root = make<TreeNode>(*this, nullptr, block_index());
        }
    } else {
        set_block_index(request_new_block_index());
        m_root = make<TreeNode>(*this, nullptr, block_index());
        if (on_new_root)
            on_new_root();
    }
    m_root->dump_if(0, "initialize_root");
}

TreeNode* BTree::new_root()
{
    set_block_index(request_new_block_index());
    m_root = make<TreeNode>(*this, nullptr, m_root.leak_ptr(), block_index());
    serializer().serialize_and_write(*m_root.ptr());
    if (on_new_root)
        on_new_root();
    return m_root;
}

bool BTree::insert(Key const& key)
{
    if (!m_root)
        initialize_root();
    return m_root->insert(key);
}

bool BTree::update_key_pointer(Key const& key)
{
    if (!m_root)
        initialize_root();
    return m_root->update_key_pointer(key);
}

Optional<u32> BTree::get(Key& key)
{
    if (!m_root)
        initialize_root();
    return m_root->get(key);
}

BTreeIterator BTree::find(Key const& key)
{
    if (!m_root)
        initialize_root();
    for (auto node = m_root->node_for(key); node; node = node->up()) {
        for (auto ix = 0u; ix < node->size(); ix++) {
            auto match = (*node)[ix].match(key);
            if (match == 0)
                return BTreeIterator(node, (int)ix);
            else if (match > 0)
                return end();
        }
    }
    return end();
}

void BTree::list_tree()
{
    if (!m_root)
        initialize_root();
    m_root->list_node(0);
}

}
