/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Format.h>
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

DownPointer::DownPointer(DownPointer&& other)
    : m_owner(other.m_owner)
    , m_pointer(other.pointer())
    , m_node(other.m_node ? move(other.m_node) : nullptr)
{
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
    serializer.read_storage(m_pointer);
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
        set_pointer(m_tree.request_new_block_index());
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
    for (auto& key : m_entries)
        len += sizeof(u32) + key.length();
    return len;
}

bool TreeNode::insert(Key const& key)
{
    dbgln_if(SQL_DEBUG, "[#{}] INSERT({})", pointer(), key.to_deprecated_string());
    if (!is_leaf())
        return node_for(key)->insert_in_leaf(key);
    return insert_in_leaf(key);
}

bool TreeNode::update_key_pointer(Key const& key)
{
    dbgln_if(SQL_DEBUG, "[#{}] UPDATE({}, {})", pointer(), key.to_deprecated_string(), key.pointer());
    if (!is_leaf())
        return node_for(key)->update_key_pointer(key);

    for (auto ix = 0u; ix < size(); ix++) {
        if (key == m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[#{}] {} == {}",
                pointer(), key.to_deprecated_string(), m_entries[ix].to_deprecated_string());
            if (m_entries[ix].pointer() != key.pointer()) {
                m_entries[ix].set_pointer(key.pointer());
                dump_if(SQL_DEBUG, "To WAL");
                tree().serializer().serialize_and_write<TreeNode>(*this);
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
                dbgln_if(SQL_DEBUG, "[#{}] duplicate key {}", pointer(), key.to_deprecated_string());
                return false;
            }
        }
    }

    dbgln_if(SQL_DEBUG, "[#{}] insert_in_leaf({})", pointer(), key.to_deprecated_string());
    just_insert(key, nullptr);
    return true;
}

u32 TreeNode::down_pointer(size_t ix) const
{
    return m_down[ix].pointer();
}

TreeNode* TreeNode::down_node(size_t ix)
{
    return m_down[ix].node();
}

TreeNode* TreeNode::node_for(Key const& key)
{
    dump_if(SQL_DEBUG, DeprecatedString::formatted("node_for(Key {})", key.to_deprecated_string()));
    if (is_leaf())
        return this;
    for (size_t ix = 0; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[{}] {} < {} v{}",
                pointer(), (DeprecatedString)key, (DeprecatedString)m_entries[ix], m_down[ix].pointer());
            return down_node(ix)->node_for(key);
        }
    }
    dbgln_if(SQL_DEBUG, "[#{}] {} >= {} v{}",
        pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[size() - 1], m_down[size()].pointer());
    return down_node(size())->node_for(key);
}

Optional<u32> TreeNode::get(Key& key)
{
    dump_if(SQL_DEBUG, DeprecatedString::formatted("get({})", key.to_deprecated_string()));
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            if (is_leaf()) {
                dbgln_if(SQL_DEBUG, "[#{}] {} < {} -> 0",
                    pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[ix]);
                return {};
            } else {
                dbgln_if(SQL_DEBUG, "[{}] {} < {} ({} -> {})",
                    pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[ix],
                    ix, m_down[ix].pointer());
                return down_node(ix)->get(key);
            }
        }
        if (key == m_entries[ix]) {
            dbgln_if(SQL_DEBUG, "[#{}] {} == {} -> {}",
                pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[ix],
                m_entries[ix].pointer());
            key.set_pointer(m_entries[ix].pointer());
            return m_entries[ix].pointer();
        }
    }
    if (m_entries.is_empty()) {
        dbgln_if(SQL_DEBUG, "[#{}] {} Empty node??", pointer(), key.to_deprecated_string());
        VERIFY_NOT_REACHED();
    }
    if (is_leaf()) {
        dbgln_if(SQL_DEBUG, "[#{}] {} > {} -> 0",
            pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[size() - 1]);
        return {};
    }
    dbgln_if(SQL_DEBUG, "[#{}] {} > {} ({} -> {})",
        pointer(), key.to_deprecated_string(), (DeprecatedString)m_entries[size() - 1],
        size(), m_down[size()].pointer());
    return down_node(size())->get(key);
}

void TreeNode::just_insert(Key const& key, TreeNode* right)
{
    dbgln_if(SQL_DEBUG, "[#{}] just_insert({}, right = {})",
        pointer(), (DeprecatedString)key, (right) ? right->pointer() : 0);
    dump_if(SQL_DEBUG, "Before");
    for (auto ix = 0u; ix < size(); ix++) {
        if (key < m_entries[ix]) {
            m_entries.insert(ix, key);
            VERIFY(is_leaf() == (right == nullptr));
            m_down.insert(ix + 1, DownPointer(this, right));
            if (length() > Block::DATA_SIZE) {
                split();
            } else {
                dump_if(SQL_DEBUG, "To WAL");
                tree().serializer().serialize_and_write(*this);
            }
            return;
        }
    }
    m_entries.append(key);
    m_down.empend(this, right);

    if (length() > Block::DATA_SIZE) {
        split();
    } else {
        dump_if(SQL_DEBUG, "To WAL");
        tree().serializer().serialize_and_write(*this);
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
        if (down.m_node != nullptr)
            down.m_node->m_up = new_node;
        new_node->m_entries.append(entry);
        new_node->m_down.append(move(down));
    }

    // Move the median key in the node one level up. Its right node will
    // be the new node:
    auto median = m_entries.take_last();

    dump_if(SQL_DEBUG, "Split Left To WAL");
    tree().serializer().serialize_and_write(*this);
    new_node->dump_if(SQL_DEBUG, "Split Right to WAL");
    tree().serializer().serialize_and_write(*new_node);

    m_up->just_insert(median, new_node);
}

void TreeNode::dump_if(int flag, DeprecatedString&& msg)
{
    if (!flag)
        return;
    StringBuilder builder;
    builder.appendff("[#{}] ", pointer());
    if (!msg.is_empty())
        builder.appendff("{}", msg);
    builder.append(": "sv);
    if (m_up)
        builder.appendff("[^{}] -> ", m_up->pointer());
    else
        builder.append("* -> "sv);
    for (size_t ix = 0; ix < m_entries.size(); ix++) {
        if (!is_leaf())
            builder.appendff("[v{}] ", m_down[ix].pointer());
        else
            VERIFY(m_down[ix].pointer() == 0);
        builder.appendff("'{}' ", (DeprecatedString)m_entries[ix]);
    }
    if (!is_leaf())
        builder.appendff("[v{}]", m_down[size()].pointer());
    else
        VERIFY(m_down[size()].pointer() == 0);
    builder.appendff(" (size {}", (int)size());
    if (is_leaf())
        builder.append(", leaf"sv);
    builder.append(')');
    dbgln(builder.to_deprecated_string());
}

void TreeNode::list_node(int indent)
{
    auto do_indent = [&]() {
        for (int i = 0; i < indent; ++i)
            warn(" ");
    };
    do_indent();
    warnln("--> #{}", pointer());
    for (auto ix = 0u; ix < size(); ix++) {
        if (!is_leaf())
            down_node(ix)->list_node(indent + 2);
        do_indent();
        warnln("{}", m_entries[ix].to_deprecated_string());
    }
    if (!is_leaf())
        down_node(size())->list_node(indent + 2);
}

}
