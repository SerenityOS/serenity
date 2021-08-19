/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Serializer.h>

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
    , m_node(adopt_own_if_nonnull(node))
{
}

DownPointer::DownPointer(TreeNode* owner, DownPointer& down)
    : m_owner(owner)
    , m_pointer(down.m_pointer)
    , m_node(move(down.m_node))
{
}

DownPointer::DownPointer(DownPointer const& other)
    : m_owner(other.m_owner)
    , m_pointer(other.pointer())
{
    if (other.m_node)
        // FIXME This is gross. We modify the other object which we promised
        // to be const. However, this particular constructor is needed
        // when we take DownPointers from the Vector they live in when
        // we split a node. The original object is going to go away, so
        // there is no harm done. However, it's yucky. If anybody has
        // a better idea...
        m_node = move(const_cast<DownPointer&>(other).m_node);
    else
        m_node = nullptr;
}

TreeNode* DownPointer::node()
{
    if (!m_node)
        deserialize(m_owner->tree().serializer());
    return m_node;
}

void DownPointer::deserialize(Serializer& serializer)
{
    if (m_node || !m_pointer)
        return;
    serializer.get_block(m_pointer);
    m_node = serializer.make_and_deserialize<TreeNode>(m_owner->tree(), m_owner, m_pointer);
}

TreeNode::TreeNode(BTree& tree, u32 pointer)
    : IndexNode(pointer)
    , m_tree(tree)
    , m_up(nullptr)
    , m_entries()
    , m_down()
{
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, u32 pointer)
    : IndexNode(pointer)
    , m_tree(tree)
    , m_up(up)
    , m_entries()
    , m_down()
{
    m_down.append(DownPointer(this, nullptr));
    m_is_leaf = true;
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, DownPointer& left, u32 pointer)
    : IndexNode(pointer)
    , m_tree(tree)
    , m_up(up)
    , m_entries()
    , m_down()
{
    if (left.m_node != nullptr)
        left.m_node->m_up = this;
    m_down.append(DownPointer(this, left));
    m_is_leaf = left.pointer() == 0;
    if (!pointer)
        set_pointer(m_tree.new_record_pointer());
}

TreeNode::TreeNode(BTree& tree, TreeNode* up, TreeNode* left, u32 pointer)
    : IndexNode(pointer)
    , m_tree(tree)
    , m_up(up)
    , m_entries()
    , m_down()
{
    m_down.append(DownPointer(this, left));
    m_is_leaf = left->pointer() == 0;
}

void TreeNode::deserialize(Serializer& serializer)
{
    auto nodes = serializer.deserialize<u32>();
    dbgln_if(SQL_DEBUG, "Deserializing node. Size {}", nodes);
    if (nodes > 0) {
        for (u32 i = 0; i < nodes; i++) {
            auto left = serializer.deserialize<u32>();
            dbgln_if(SQL_DEBUG, "Down[{}] {}", i, left);
            if (!m_down.is_empty())
                VERIFY((left == 0) == m_is_leaf);
            else
                m_is_leaf = (left == 0);
            m_entries.append(serializer.deserialize<Key>(m_tree.descriptor()));
            m_down.empend(this, left);
        }
        auto right = serializer.deserialize<u32>();
        dbgln_if(SQL_DEBUG, "Right {}", right);
        VERIFY((right == 0) == m_is_leaf);
        m_down.empend(this, right);
    }
}

void TreeNode::serialize(Serializer& serializer) const
{
    u32 sz = size();
    serializer.serialize<u32>(sz);
    if (sz > 0) {
        for (auto ix = 0u; ix < size(); ix++) {
            auto& entry = m_entries[ix];
            dbgln_if(SQL_DEBUG, "Serializing Left[{}] = {}", ix, m_down[ix].pointer());
            serializer.serialize<u32>(is_leaf() ? 0u : m_down[ix].pointer());
            serializer.serialize<Key>(entry);
        }
        dbgln_if(SQL_DEBUG, "Serializing Right = {}", m_down[size()].pointer());
        serializer.serialize<u32>(is_leaf() ? 0u : m_down[size()].pointer());
    }
}

size_t TreeNode::length() const
{
    if (!size())
        return 0;
    size_t len = sizeof(u32);
    for (auto& key : m_entries) {
        len += sizeof(u32) + key.length();
    }
    return len;
}

bool TreeNode::insert(Key const& key)
{
    dbgln_if(SQL_DEBUG, "[#{}] INSERT({})", pointer(), key.to_string());
    if (!is_leaf())
        return node_for(key)->insert_in_leaf(key);
    return insert_in_leaf(key);
}

bool TreeNode::update_key_pointer(Key const& key)
{
    dbgln_if(SQL_DEBUG, "[#{}] UPDATE({}, {})", pointer(), key.to_string(), key.pointer());
    if (!is_leaf())
        return node_for(key)->update_key_pointer(key);

    for (auto ix = 0u; ix < size(); ix++) {
        if (key == m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[#{}] {} == {}",
                pointer(), key.to_string(), m_entries[ix].to_string());
            if (m_entries[ix].pointer() != key.pointer()) {
                m_entries[ix].set_pointer(key.pointer());
                dump_if(SQL_DEBUG, "To WAL");
                tree().serializer().serialize_and_write<TreeNode>(*this, pointer());
            }
            return true;
        }
    }
    return false;
}

bool TreeNode::insert_in_leaf(Key const& key)
{
    VERIFY(is_leaf());
    if (!m_tree.duplicates_allowed()) {
        for (auto& entry : m_entries) {
            if (key == entry) {
                dbgln_if(SQL_DEBUG, "[#{}] duplicate key {}", pointer(), key.to_string());
                return false;
            }
        }
    }

    dbgln_if(SQL_DEBUG, "[#{}] insert_in_leaf({})", pointer(), key.to_string());
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
    dump_if(SQL_DEBUG, String::formatted("node_for(Key {})", key.to_string()));
    if (is_leaf())
        return this;
    for (size_t ix = 0; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[{}] {} < {} v{}",
                pointer(), (String)key, (String)m_entries[ix], m_down[ix].pointer());
            return down_node(ix)->node_for(key);
        }
    }
    dbgln_if(SQL_DEBUG, "[#{}] {} >= {} v{}",
        pointer(), key.to_string(), (String)m_entries[size() - 1], m_down[size()].pointer());
    return down_node(size())->node_for(key);
}

Optional<u32> TreeNode::get(Key& key)
{
    dump_if(SQL_DEBUG, String::formatted("get({})", key.to_string()));
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            if (is_leaf()) {
                dbgln_if(SQL_DEBUG, "[#{}] {} < {} -> 0",
                    pointer(), key.to_string(), (String)m_entries[ix]);
                return {};
            } else {
                dbgln_if(SQL_DEBUG, "[{}] {} < {} ({} -> {})",
                    pointer(), key.to_string(), (String)m_entries[ix],
                    ix, m_down[ix].pointer());
                return down_node(ix)->get(key);
            }
        }
        if (key == m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[#{}] {} == {} -> {}",
                pointer(), key.to_string(), (String)m_entries[ix],
                m_entries[ix].pointer());
            key.set_pointer(m_entries[ix].pointer());
            return m_entries[ix].pointer();
        }
    }
    if (m_entries.is_empty()) {
        dbgln_if(SQL_DEBUG, "[#{}] {} Empty node??", pointer(), key.to_string());
        VERIFY_NOT_REACHED();
    }
    if (is_leaf()) {
        dbgln_if(SQL_DEBUG, "[#{}] {} > {} -> 0",
            pointer(), key.to_string(), (String)m_entries[size() - 1]);
        return {};
    }
    dbgln_if(SQL_DEBUG, "[#{}] {} > {} ({} -> {})",
        pointer(), key.to_string(), (String)m_entries[size() - 1],
        size(), m_down[size()].pointer());
    return down_node(size())->get(key);
}

void TreeNode::just_insert(Key const& key, TreeNode* right)
{
    dbgln_if(SQL_DEBUG, "[#{}] just_insert({}, right = {})",
        pointer(), (String)key, (right) ? right->pointer() : 0);
    dump_if(SQL_DEBUG, "Before");
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            m_entries.insert(ix, key);
            VERIFY(is_leaf() == (right == nullptr));
            m_down.insert(ix + 1, DownPointer(this, right));
            if (length() > BLOCKSIZE) {
                split();
            } else {
                dump_if(SQL_DEBUG, "To WAL");
                tree().serializer().serialize_and_write(*this, pointer());
            }
            return;
        }
    }
    m_entries.append(key);
    m_down.empend(this, right);

    if (length() > BLOCKSIZE) {
        split();
    } else {
        dump_if(SQL_DEBUG, "To WAL");
        tree().serializer().serialize_and_write(*this, pointer());
    }
}

void TreeNode::split()
{
    dump_if(SQL_DEBUG, "Splitting node");
    if (!m_up)
        // Make new m_up. This is the new root node.
        m_up = m_tree.new_root();

    // Take the left pointer for the new node:
    auto median_index = size() / 2;
    if (!(size() % 2))
        ++median_index;
    DownPointer left = m_down.take(median_index);

    // Create the new right node:
    auto* new_node = new TreeNode(tree(), m_up, left);

    // Move the rightmost keys from this node to the new right node:
    while (m_entries.size() > median_index) {
        auto entry = m_entries.take(median_index);
        auto down = m_down.take(median_index);

        // Reparent to new right node:
        if (down.m_node != nullptr) {
            down.m_node->m_up = new_node;
        }
        new_node->m_entries.append(entry);
        new_node->m_down.append(down);
    }

    // Move the median key in the node one level up. Its right node will
    // be the new node:
    auto median = m_entries.take_last();

    dump_if(SQL_DEBUG, "Split Left To WAL");
    tree().serializer().serialize_and_write(*this, pointer());
    new_node->dump_if(SQL_DEBUG, "Split Right to WAL");
    tree().serializer().serialize_and_write(*new_node, pointer());

    m_up->just_insert(median, new_node);
}

void TreeNode::dump_if(int flag, String&& msg)
{
    if (!flag)
        return;
    StringBuilder builder;
    builder.appendff("[#{}] ", pointer());
    if (!msg.is_empty())
        builder.appendff("{}", msg);
    builder.append(": ");
    if (m_up)
        builder.appendff("[^{}] -> ", m_up->pointer());
    else
        builder.append("* -> ");
    for (size_t ix = 0; ix < m_entries.size(); ix++) {
        if (!is_leaf())
            builder.appendff("[v{}] ", m_down[ix].pointer());
        else
            VERIFY(m_down[ix].pointer() == 0);
        builder.appendff("'{}' ", (String)m_entries[ix]);
    }
    if (!is_leaf()) {
        builder.appendff("[v{}]", m_down[size()].pointer());
    } else {
        VERIFY(m_down[size()].pointer() == 0);
    }
    builder.appendff(" (size {}", (int)size());
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
            warn(" ");
        }
    };
    do_indent();
    warnln("--> #{}", pointer());
    for (auto ix = 0u; ix < size(); ix++) {
        if (!is_leaf()) {
            down_node(ix)->list_node(indent + 2);
        }
        do_indent();
        warnln("{}", m_entries[ix].to_string());
    }
    if (!is_leaf()) {
        down_node(size())->list_node(indent + 2);
    }
}

}
