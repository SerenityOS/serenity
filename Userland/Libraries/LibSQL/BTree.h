/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Object.h>
#include <LibSQL/StorageForward.h>
#include <LibSQL/Key.h>

namespace SQL {

#ifndef BTREE_DEBUG
#define BTREE_DEBUG 0
#endif

#ifndef SERIALIZE_DEBUG
#define SERIALIZE_DEBUG 0
#endif

class DownPointer {
public:
    explicit DownPointer(TreeNode*, u32 = 0);
    DownPointer(TreeNode*, TreeNode*);
    DownPointer(DownPointer const&);
    DownPointer(TreeNode*, DownPointer&);
    ~DownPointer() = default;
    [[nodiscard]] u32 pointer() const { return m_pointer; }
    TreeNode * node();
private:
    void inflate();

    TreeNode* m_owner;
    u32 m_pointer { 0 };
    OwnPtr<TreeNode> m_node { nullptr };
    friend TreeNode;
};

class TreeNode {
public:
    TreeNode(BTree&, TreeNode*, u32 = 0);
    TreeNode(BTree&, TreeNode*, TreeNode*, u32 = 0);
    TreeNode(BTree&, TreeNode*, u32 pointer, ByteBuffer &, size_t &);
    ~TreeNode() = default;

    [[nodiscard]] NonnullRefPtr<BTree> tree() const { return m_tree; }
    [[nodiscard]] TreeNode* up() const { return m_up; }
    [[nodiscard]] u32 pointer() const { return m_pointer; }
    [[nodiscard]] size_t size() const { return m_entries.size(); }
    [[nodiscard]] Vector<Key> entries() const { return m_entries; }
    [[nodiscard]] u32 down_pointer(size_t) const;
    [[nodiscard]] TreeNode* down_node(size_t);
    [[nodiscard]] bool is_leaf() const { return m_is_leaf; }
    [[nodiscard]] static size_t max_entries() { return 4; } // FIXME
    Key const& operator [](size_t) const;

    bool insert(Key const&);

    bool insert(Key const &&entry) {
        return insert(entry);
    }

    TreeNode* node_for(Key const&);
    Optional<u32> get(Key&);
    void serialize(ByteBuffer &) const;

private:
    TreeNode(BTree&, TreeNode*, DownPointer&, u32 = 0);
    void dump_if(int, String&& = "");
    bool insert_in_leaf(Key const&);
    void just_insert(Key const&, TreeNode* = nullptr);
    void split();
    void list_node(int);

    NonnullRefPtr<BTree> m_tree;
    TreeNode* m_up;
    u32 m_pointer;
    Vector<Key> m_entries;
    bool m_is_leaf { true };
    Vector<DownPointer> m_down;

    friend BTree;
    friend BTreeIterator;
};

class BTree : public Core::Object {
    C_OBJECT(BTree);
public:
    BTree(Heap &heap, IndexDef &index, u32 pointer);
    ~BTree() override = default;

    NonnullRefPtr<IndexDef> index_def() const;
    u32 root() const { return (m_root) ? m_root->pointer() : 0; }
    bool insert(Key const&);
    Optional<u32> get(Key&);
    BTreeIterator find(Key const& key);
    BTreeIterator find(Key&& key);
    [[nodiscard]] bool duplicates_allowed() const;
    BTreeIterator begin();
    static BTreeIterator end();
    void list_tree();

    Function<void(void)> on_new_root;

private:
    void initialize_root();
    TreeNode* new_root();
    u32 new_record_pointer();
    ByteBuffer read_block(u32);
    void add_to_write_ahead_log(TreeNode &);

    Heap& m_heap;
    NonnullRefPtr<IndexDef> m_index;
    OwnPtr<TreeNode> m_root { nullptr };
    u32 m_pointer { 0 };

    friend BTreeIterator;
    friend DownPointer;
    friend TreeNode;
};

class BTreeIterator {
public:
    [[nodiscard]] bool is_end() const;
    [[nodiscard]] size_t index() const;
    bool update(Key const &);

    bool operator==(BTreeIterator const&) const;
    bool operator!=(BTreeIterator const&) const;
    bool operator<(BTreeIterator const&) const;
    bool operator>(BTreeIterator const&) const;
    bool operator<=(BTreeIterator const&) const;
    bool operator>=(BTreeIterator const&) const;
    bool operator==(Key const&) const;
    bool operator!=(Key const&) const;
    bool operator<(Key const&) const;
    bool operator>(Key const&) const;
    bool operator<=(Key const&) const;
    bool operator>=(Key const&) const;

    BTreeIterator operator++();
    BTreeIterator operator++(int);
    BTreeIterator operator--();
    BTreeIterator operator--(int);
    Key const& operator*() const;
    Key const& operator->() const;

    BTreeIterator& operator=(BTreeIterator const& );
    BTreeIterator(BTreeIterator const&) = default;

private:
    BTreeIterator(TreeNode *, int index);
    static BTreeIterator end();

    [[nodiscard]] int cmp(BTreeIterator const&) const;
    [[nodiscard]] int cmp(Key const&) const;
    [[nodiscard]] BTreeIterator next() const;
    [[nodiscard]] BTreeIterator previous() const;
    [[nodiscard]] Key key() const;
    enum Where { Valid, End };

    Where m_where { Valid };
    TreeNode* m_current { nullptr };
    int m_index { -1 };

    friend BTree;
};

}
