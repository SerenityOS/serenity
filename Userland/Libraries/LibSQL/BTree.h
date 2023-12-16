/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibCore/EventReceiver.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Index.h>
#include <LibSQL/Key.h>

namespace SQL {

/**
 * The BTree class models a B-Tree index. It contains a collection of
 * Key objects organized in TreeNode objects. Keys can be inserted,
 * located, deleted, and the set can be traversed in sort order. All keys in
 * a tree have the same underlying structure. A BTree's TreeNodes and
 * the keys it includes are lazily loaded from the Heap when needed.
 *
 * The classes implementing the B-Tree functionality are BTree, TreeNode,
 * BTreeIterator, and DownPointer (a smart pointer-like helper class).
 */
class DownPointer {
public:
    explicit DownPointer(TreeNode*, Block::Index = 0);
    DownPointer(TreeNode*, TreeNode*);
    DownPointer(DownPointer&&);
    DownPointer(TreeNode*, DownPointer&);
    ~DownPointer() = default;
    [[nodiscard]] Block::Index block_index() const { return m_block_index; }
    TreeNode* node();

private:
    void deserialize(Serializer&);

    TreeNode* m_owner;
    Block::Index m_block_index { 0 };
    OwnPtr<TreeNode> m_node { nullptr };
    friend TreeNode;
};

class TreeNode : public IndexNode {
public:
    TreeNode(BTree&, Block::Index = 0);
    TreeNode(BTree&, TreeNode*, Block::Index = 0);
    TreeNode(BTree&, TreeNode*, TreeNode*, Block::Index = 0);
    ~TreeNode() override = default;

    [[nodiscard]] BTree& tree() const { return m_tree; }
    [[nodiscard]] TreeNode* up() const { return m_up; }
    [[nodiscard]] size_t size() const { return m_entries.size(); }
    [[nodiscard]] size_t length() const;
    [[nodiscard]] Vector<Key> entries() const { return m_entries; }
    [[nodiscard]] Block::Index down_pointer(size_t) const;
    [[nodiscard]] TreeNode* down_node(size_t);
    [[nodiscard]] bool is_leaf() const { return m_is_leaf; }

    Key const& operator[](size_t index) const { return m_entries[index]; }
    bool insert(Key const&);
    bool update_key_pointer(Key const&);
    TreeNode* node_for(Key const&);
    Optional<u32> get(Key&);
    void deserialize(Serializer&);
    void serialize(Serializer&) const;

private:
    TreeNode(BTree&, TreeNode*, DownPointer&, u32 = 0);
    void dump_if(int, ByteString&& = "");
    bool insert_in_leaf(Key const&);
    void just_insert(Key const&, TreeNode* = nullptr);
    void split();
    void list_node(int);

    BTree& m_tree;
    TreeNode* m_up;
    Vector<Key> m_entries;
    bool m_is_leaf { true };
    Vector<DownPointer> m_down;

    friend BTree;
    friend BTreeIterator;
};

class BTree : public Index {
public:
    static ErrorOr<NonnullRefPtr<BTree>> create(Serializer&, NonnullRefPtr<TupleDescriptor> const&, bool unique, Block::Index);
    static ErrorOr<NonnullRefPtr<BTree>> create(Serializer&, NonnullRefPtr<TupleDescriptor> const&, Block::Index);

    Block::Index root() const { return m_root ? m_root->block_index() : 0; }
    bool insert(Key const&);
    bool update_key_pointer(Key const&);
    Optional<u32> get(Key&);
    BTreeIterator find(Key const& key);
    BTreeIterator begin();
    static BTreeIterator end();
    void list_tree();

    Function<void(void)> on_new_root;

private:
    BTree(Serializer&, NonnullRefPtr<TupleDescriptor> const&, bool unique, Block::Index);
    void initialize_root();
    TreeNode* new_root();
    OwnPtr<TreeNode> m_root { nullptr };

    friend BTreeIterator;
    friend DownPointer;
    friend TreeNode;
};

class BTreeIterator {
public:
    [[nodiscard]] bool is_end() const { return m_where == Where::End; }
    [[nodiscard]] size_t index() const { return m_index; }
    bool update(Key const&);

    bool operator==(BTreeIterator const& other) const { return cmp(other) == 0; }
    bool operator!=(BTreeIterator const& other) const { return cmp(other) != 0; }
    bool operator<(BTreeIterator const& other) const { return cmp(other) < 0; }
    bool operator>(BTreeIterator const& other) const { return cmp(other) > 0; }
    bool operator<=(BTreeIterator const& other) const { return cmp(other) <= 0; }
    bool operator>=(BTreeIterator const& other) const { return cmp(other) >= 0; }
    bool operator==(Key const& other) const { return cmp(other) == 0; }
    bool operator!=(Key const& other) const { return cmp(other) != 0; }
    bool operator<(Key const& other) const { return cmp(other) < 0; }
    bool operator>(Key const& other) const { return cmp(other) > 0; }
    bool operator<=(Key const& other) const { return cmp(other) <= 0; }
    bool operator>=(Key const& other) const { return cmp(other) >= 0; }

    BTreeIterator operator++()
    {
        *this = next();
        return *this;
    }

    BTreeIterator operator++(int)
    {
        *this = next();
        return *this;
    }

    BTreeIterator operator--()
    {
        *this = previous();
        return *this;
    }

    BTreeIterator const operator--(int)
    {
        *this = previous();
        return *this;
    }

    Key const& operator*() const
    {
        VERIFY(!is_end());
        return (*m_current)[m_index];
    }

    Key const& operator->() const
    {
        VERIFY(!is_end());
        return (*m_current)[m_index];
    }

    BTreeIterator& operator=(BTreeIterator const&);
    BTreeIterator(BTreeIterator const&) = default;

private:
    BTreeIterator(TreeNode*, int index);
    static BTreeIterator end() { return BTreeIterator(nullptr, -1); }

    [[nodiscard]] int cmp(BTreeIterator const&) const;
    [[nodiscard]] int cmp(Key const&) const;
    [[nodiscard]] BTreeIterator next() const;
    [[nodiscard]] BTreeIterator previous() const;
    [[nodiscard]] Key key() const;

    enum class Where {
        Valid,
        End
    };

    Where m_where { Where::Valid };
    TreeNode* m_current { nullptr };
    int m_index { -1 };

    friend BTree;
};

}
