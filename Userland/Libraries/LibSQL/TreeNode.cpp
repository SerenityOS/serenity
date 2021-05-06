/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/StringBuilder.h>

#include <LibSQL/BTree.h>
#include <LibSQL/Key.h>
#include <LibSQL/Serialize.h>

namespace SQL {

DownPointer::DownPointer(TreeNode* owner, u32 pointer)
    : m_owner(owner)
    , m_pointer(pointer)
    , m_node(nullptr)
{
}

DownPointer::DownPointer(TreeNode* owner, TreeNode* node)
    : m_owner(owner)
    , m_pointer((node) ? node->pointer() : 0)
    , m_node(node)
{
}

DownPointer::DownPointer(TreeNode* owner, DownPointer& down)
    : m_owner(owner)
    , m_pointer(down.m_pointer)
    , m_node(down.m_node.leak_ptr())
{
}

DownPointer::DownPointer(DownPointer const& other)
    : m_owner(other.m_owner)
    , m_pointer(other.pointer())
{
    if (other.m_node) {
        m_node = const_cast<DownPointer&>(other).m_node.release_nonnull<TreeNode>();
    } else {
        m_node = nullptr;
    }
}

TreeNode * DownPointer::node()
{
    if (!m_node) {
        inflate();
    }
    return m_node;
}

void DownPointer::inflate()
{
    if (m_node || !m_pointer) {
        return;
    }
    auto buffer = m_owner->tree()->read_block(m_pointer);
    size_t offset = 0;
    m_node = make<TreeNode>(m_owner->tree(), m_owner, m_pointer, buffer, offset);
}


TreeNode::TreeNode(BTree& tree, TreeNode* up, u32 pointer)
    : m_tree(tree)
    , m_up(up)
    , m_pointer(pointer)
    , m_entries()
    , m_down()
{
    m_down.append(DownPointer(this, nullptr));
    m_is_leaf = true;
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, DownPointer& left, u32 pointer)
    : m_tree(tree)
    , m_up(up)
    , m_pointer(pointer)
    , m_entries()
    , m_down()
{
    if (left.m_node != nullptr) {
        left.m_node->m_up = this;
    }
    m_down.append(DownPointer(this, left));
    m_is_leaf = left.pointer() == 0;
    if (!m_pointer) {
        m_pointer = m_tree->new_record_pointer();
    }
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, TreeNode* left, u32 pointer)
    : m_tree(tree)
    , m_up(up)
    , m_pointer(pointer)
    , m_entries()
    , m_down()
{
    m_down.append(DownPointer(this, left));
    m_is_leaf = left->pointer() == 0;
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, u32 pointer, ByteBuffer& buffer, size_t& at_offset)
    : m_tree(tree)
    , m_up(up)
    , m_pointer(pointer)
    , m_entries()
    , m_down()
{
    u32 nodes;
    deserialize_from<u32>(buffer, at_offset, nodes);
    dbgln_if(SERIALIZE_DEBUG, "Deserializing node. Size {}", nodes);
    if (nodes > 0) {
        for (u32 i = 0; i < nodes; i++) {
            u32 left;
            deserialize_from<u32>(buffer, at_offset, left);
            dbgln_if(SERIALIZE_DEBUG, "Down[{}] {}", i, left);
            if (!m_down.is_empty()) {
                VERIFY((left == 0) == m_is_leaf);
            } else {
                m_is_leaf = (left == 0);
            }
            m_entries.append(Key(m_tree->index_def(), buffer, at_offset));
            m_down.empend(this, left);
        }
        u32 right;
        deserialize_from<u32>(buffer, at_offset, right);
        dbgln_if(SERIALIZE_DEBUG, "Right {}", right);
        VERIFY((right == 0) == m_is_leaf);
        m_down.empend(this, right);
    }
}

bool TreeNode::insert(Key const& key)
{
    dbgln_if(BTREE_DEBUG, "[#{}] INSERT({})", m_pointer, (String) key);
    if (!is_leaf()) {
        return node_for(key)->insert_in_leaf(key);
    }
    return insert_in_leaf(key);
}

bool TreeNode::insert_in_leaf(Key const& key)
{
    VERIFY(is_leaf());
    if (!m_tree->duplicates_allowed()) {
        for (auto& entry : m_entries) {
            if (key == entry) {
                dbgln_if(BTREE_DEBUG, "[#{}] duplicate key {}", m_pointer, (String) key);
                return false;
            }
        }
    }

    dbgln_if(BTREE_DEBUG, "[#{}] insert_in_leaf({})", m_pointer, (String) key);
    just_insert(key, nullptr);
    return true;
}

Key const& TreeNode::operator[](size_t ix) const
{
    VERIFY(ix < size());
    return m_entries[ix];
}

u32 TreeNode::down_pointer(size_t ix) const
{
    VERIFY(ix < m_down.size());
    return m_down[ix].pointer();
}

TreeNode* TreeNode::down_node(size_t ix)
{
    VERIFY(ix < m_down.size());
    return m_down[ix].node();
}

TreeNode* TreeNode::node_for(Key const& key)
{
    dump_if(BTREE_DEBUG, String::formatted("node_for(Key {})", (String) key));
    if (is_leaf()) {
        return this;
    }
    // Heuristic hack: if we just created a new root node because the original
    // root had to split, there will just be a left down node. We will have to
    // follow that because it's all we have...
    for (size_t ix = 0; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            dbgln_if(BTREE_DEBUG, "[{}] {} < {} v{}",
                m_pointer, (String) key, (String) m_entries[ix],m_down[ix].pointer());
            return down_node(ix)->node_for(key);
        }
    }
    dbgln_if(BTREE_DEBUG, "[#{}] {} >= {} v{}",
        m_pointer, (String) key, (String) m_entries[size() - 1],m_down[size()].pointer());
    return down_node(size())->node_for(key);
}

Optional<u32> TreeNode::get(Key& key)
{
    dump_if(BTREE_DEBUG, String::formatted("get({})", (String) key));
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            if (is_leaf()) {
                dbgln_if(BTREE_DEBUG, "[#{}] {} < {} -> 0",
                    m_pointer, (String) key, (String) m_entries[ix]);
                return {};
            } else {
                dbgln_if(BTREE_DEBUG, "[{}] {} < {} ({} -> {})",
                    m_pointer, (String) key, (String) m_entries[ix],
                    ix, m_down[ix].pointer());
                return down_node(ix)->get(key);
            }
        }
        if (key == m_entries[ix]) {
            dbgln_if(BTREE_DEBUG, "[#{}] {} == {} -> {}",
                m_pointer, (String) key, (String) m_entries[ix],
                m_entries[ix].pointer());
            key.pointer(m_entries[ix].pointer());
            return m_entries[ix].pointer();
        }
    }
    if (m_entries.is_empty()) {
        dbgln_if(BTREE_DEBUG, "[#{}] {} Empty node??", m_pointer, (String) key);
        VERIFY_NOT_REACHED();
    }
    if (is_leaf()) {
        dbgln_if(BTREE_DEBUG, "[#{}] {} > {} -> 0",
            m_pointer, (String) key, (String) m_entries[size() - 1]);
        return {};
    }
    dbgln_if(BTREE_DEBUG, "[#{}] {} > {} ({} -> {})",
        m_pointer, (String) key, (String) m_entries[size() - 1],
        size(), m_down[size()].pointer());
    return down_node(size())->get(key);
}

void TreeNode::serialize(ByteBuffer& buffer) const
{
    u32 sz = size();
    serialize_to<u32>(buffer, sz);
    if (sz > 0) {
        for (auto ix = 0u; ix < size(); ix++) {
            auto& entry = m_entries[ix];
            dbgln_if(SERIALIZE_DEBUG, "Serializing Left[{}] = {}", ix, m_down[ix].pointer());
            serialize_to<u32>(buffer, is_leaf() ? 0u : m_down[ix].pointer());
            entry.serialize(buffer);
        }
        dbgln_if(SERIALIZE_DEBUG, "Serializing Right = {}", m_down[size()].pointer());
        serialize_to<u32>(buffer, is_leaf() ? 0u : m_down[size()].pointer());
    }
}

void TreeNode::just_insert(Key const& key, TreeNode *right)
{
    dbgln_if(BTREE_DEBUG, "[#{}] just_insert({}, right = {})",
        m_pointer, (String)key, (right) ? right->m_pointer : 0);
    dump_if(BTREE_DEBUG, "Before");
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            m_entries.insert(ix, key);
            VERIFY(is_leaf() == (right == nullptr));
            m_down.insert(ix + 1, DownPointer(this, right));
            if (size() > max_entries()) {
                split();
            } else {
                dump_if(BTREE_DEBUG, "To WAL");
                tree()->add_to_write_ahead_log(*this);
            }
            return;
        }
    }
    m_entries.append(key);
    m_down.empend(this, right);

    if (size() > max_entries()) {
        split();
    } else {
        dump_if(BTREE_DEBUG, "To WAL");
        tree()->add_to_write_ahead_log(*this);
    }
}

void TreeNode::split()
{
    dump_if(BTREE_DEBUG, "Splitting node");
    if (!m_up) {
        // Make new m_up. This is the new root node.
        m_up = m_tree->new_root();
    }

    // Take the left pointer for the new node:
    DownPointer left = m_down.take(max_entries()/2 + 1);

    // Create the new right node:
    auto* new_node = new TreeNode(tree(), m_up, left);

    // Move the rightmost keys from this node to the new right node:
    while (m_entries.size() > max_entries()/2+1) {
        auto entry = m_entries.take(max_entries()/2+1);
        auto down = m_down.take(max_entries()/2+1);

        // Reparent to new right node:
        if (down.m_node != nullptr) {
            down.m_node->m_up = new_node;
        }
        new_node->m_entries.append(entry);
        new_node->m_down.append(down);
    }

    // The median key in the node one level up. Its right node will
    // be the new node:
    auto median = m_entries.take_last();

    dump_if(BTREE_DEBUG, "Split Left To WAL");
    tree()->add_to_write_ahead_log(*this);
    new_node->dump_if(BTREE_DEBUG, "Split Right to WAL");
    tree()->add_to_write_ahead_log(*new_node);

    m_up->just_insert(median, new_node);
}

void TreeNode::dump_if(int flag, String &&msg) {
    if (!flag)
        return;
    StringBuilder builder;
    builder.appendff("[#{}] ", pointer());
    if (!msg.is_empty()) {
        builder.appendff("{}", msg);
    }
    builder.append(": ");
    if (m_up) {
        builder.appendff("[^{}] -> ", m_up->pointer());
    } else {
        builder.append("* -> ");
    }
    for (size_t ix = 0; ix < m_entries.size(); ix++) {
        if (!is_leaf()) {
            builder.appendff("[v{}] ", m_down[ix].pointer());
        } else {
            VERIFY(m_down[ix].pointer() == 0);
        }
        builder.appendff("'{}' ", (String) m_entries[ix]);
    }
    if (!is_leaf()) {
        builder.appendff("[v{}]", m_down[size()].pointer());
    } else {
        VERIFY(m_down[size()].pointer() == 0);
    }
    builder.appendff(" (size {}", (int) size());
    if (is_leaf()) {
        builder.append(", leaf");
    }
    builder.append(")");
    dbgln(builder.build());
}

void TreeNode::list_node(int indent)
{
    auto do_indent = [&]() {
        for (int i = 0; i < indent; ++i) {
            fprintf(stderr," ");
        }
    };
    do_indent();
    fprintf(stderr,"--> #%d\n", m_pointer);
    for (auto ix = 0u; ix < size(); ix++) {
        if (!is_leaf()) {
            down_node(ix)->list_node(indent + 2);
        }
        do_indent();
        fprintf(stderr, "%s\n", ((String) m_entries[ix]).characters());
    }
    if (!is_leaf()) {
        down_node(size())->list_node(indent + 2);
    }
}

}
