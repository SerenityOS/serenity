/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Index.h>
#include <LibSQL/Key.h>

namespace SQL {

/**
 * The HashIndex class is a straightforward implementation of a persisted
 * extendible hash table (see
 * https://en.wikipedia.org/wiki/Extendible_hashing).
 */

class HashBucket : public IndexNode
    , public Weakable<HashBucket> {
public:
    HashBucket(HashIndex&, u32 index, u32 local_depth, u32 pointer);
    ~HashBucket() override = default;
    Optional<u32> get(Key&);
    bool insert(Key const&);
    Vector<Key> const& entries();
    Key const& operator[](size_t);
    Key const& operator[](size_t ix) const;
    [[nodiscard]] u32 local_depth() const { return m_local_depth; }
    [[nodiscard]] u32 size() { return entries().size(); }
    [[nodiscard]] size_t length() const;
    [[nodiscard]] u32 size() const { return m_entries.size(); }
    [[nodiscard]] u32 index() const { return m_index; }
    void serialize(Serializer&) const;
    void deserialize(Serializer&);
    [[nodiscard]] HashIndex const& hash_index() const { return m_hash_index; }
    [[nodiscard]] HashBucket const* next_bucket();
    [[nodiscard]] HashBucket const* previous_bucket();
    void list_bucket();

private:
    Optional<size_t> find_key_in_bucket(Key const&);
    void set_index(u32 index) { m_index = index; }
    void set_local_depth(u32 depth) { m_local_depth = depth; }

    HashIndex& m_hash_index;
    u32 m_local_depth { 1 };
    u32 m_index { 0 };
    Vector<Key> m_entries;
    bool m_inflated { false };

    friend HashIndex;
};

class HashIndex : public Index {
    C_OBJECT(HashIndex);

public:
    ~HashIndex() override = default;

    Optional<u32> get(Key&);
    bool insert(Key const&);
    bool insert(Key const&& entry) { return insert(entry); }
    HashIndexIterator find(Key const&);
    HashIndexIterator begin();
    static HashIndexIterator end();

    [[nodiscard]] u32 global_depth() const { return m_global_depth; }
    [[nodiscard]] u32 size() const { return 1 << m_global_depth; }
    [[nodiscard]] HashBucket* get_bucket(u32);
    [[nodiscard]] u32 node_pointer(u32 node_number) const { return m_nodes[node_number]; }
    [[nodiscard]] u32 first_node_pointer() const { return m_nodes[0]; }
    [[nodiscard]] size_t nodes() const { return m_nodes.size(); }
    void list_hash();

private:
    HashIndex(Serializer&, NonnullRefPtr<TupleDescriptor> const&, u32);

    void expand();
    void write_directory_to_write_ahead_log();
    HashBucket* append_bucket(u32 index, u32 local_depth, u32 pointer);
    HashBucket* get_bucket_for_insert(Key const&);
    [[nodiscard]] HashBucket* get_bucket_by_index(u32 index);

    u32 m_global_depth { 1 };
    Vector<u32> m_nodes;
    Vector<OwnPtr<HashBucket>> m_buckets;

    friend HashBucket;
    friend HashDirectoryNode;
};

class HashDirectoryNode : public IndexNode {
public:
    HashDirectoryNode(HashIndex&, u32, size_t);
    HashDirectoryNode(HashIndex&, u32);
    HashDirectoryNode(HashDirectoryNode const& other) = default;
    void deserialize(Serializer&);
    void serialize(Serializer&) const;
    [[nodiscard]] u32 number_of_pointers() const { return min(max_pointers_in_node(), m_hash_index.size() - m_offset); }
    [[nodiscard]] bool is_last() const { return m_is_last; }
    static constexpr size_t max_pointers_in_node() { return (BLOCKSIZE - 3 * sizeof(u32)) / (2 * sizeof(u32)); }

private:
    HashIndex& m_hash_index;
    size_t m_node_number { 0 };
    size_t m_offset { 0 };
    bool m_is_last { false };
};

class HashIndexIterator {
public:
    [[nodiscard]] bool is_end() const { return !m_current; }

    bool operator==(HashIndexIterator const& other) const;
    bool operator!=(HashIndexIterator const& other) const { return !(*this == other); }
    bool operator==(Key const& other) const;
    bool operator!=(Key const& other) const { return !(*this == other); }

    HashIndexIterator operator++()
    {
        *this = next();
        return *this;
    }

    HashIndexIterator operator++(int)
    {
        *this = next();
        return *this;
    }

    HashIndexIterator operator--()
    {
        *this = previous();
        return *this;
    }

    HashIndexIterator const operator--(int)
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

    HashIndexIterator& operator=(HashIndexIterator const&) = default;
    HashIndexIterator(HashIndexIterator const&) = default;

private:
    HashIndexIterator() = default;
    explicit HashIndexIterator(HashBucket const*, size_t key_index = 0);
    static HashIndexIterator end() { return HashIndexIterator(); }

    [[nodiscard]] HashIndexIterator next();
    [[nodiscard]] HashIndexIterator previous();
    [[nodiscard]] Key key() const { return **this; }

    WeakPtr<HashBucket> m_current;
    size_t m_index { 0 };

    friend HashIndex;
};

}
