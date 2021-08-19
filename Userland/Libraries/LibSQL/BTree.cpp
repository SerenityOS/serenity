/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Meta.h>

namespace SQL {

BTree::BTree(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, u32 pointer)
    : Index(serializer, descriptor, unique, pointer)
    , m_root(nullptr)
{
}

BTree::BTree(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, u32 pointer)
    : BTree(serializer, descriptor, true, pointer)
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
    if (pointer()) {
        if (serializer().has_block(pointer())) {
            serializer().get_block(pointer());
            m_root = serializer().make_and_deserialize<TreeNode>(*this, pointer());
        } else {
            m_root = make<TreeNode>(*this, nullptr, pointer());
        }
    } else {
        set_pointer(new_record_pointer());
        m_root = make<TreeNode>(*this, nullptr, pointer());
        if (on_new_root)
            on_new_root();
    }
    m_root->dump_if(0, "initialize_root");
}

TreeNode* BTree::new_root()
{
    set_pointer(new_record_pointer());
    m_root = make<TreeNode>(*this, nullptr, m_root.leak_ptr(), pointer());
    serializer().serialize_and_write(*m_root.ptr(), m_root->pointer());
    if (on_new_root)
        on_new_root();
    return m_root;
}

bool BTree::insert(Key const& key)
{
    if (!m_root)
        initialize_root();
    VERIFY(m_root);
    return m_root->insert(key);
}

bool BTree::update_key_pointer(Key const& key)
{
    if (!m_root)
        initialize_root();
    VERIFY(m_root);
    return m_root->update_key_pointer(key);
}

Optional<u32> BTree::get(Key& key)
{
    if (!m_root)
        initialize_root();
    VERIFY(m_root);
    return m_root->get(key);
}

BTreeIterator BTree::find(Key const& key)
{
    if (!m_root)
        initialize_root();
    VERIFY(m_root);
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
