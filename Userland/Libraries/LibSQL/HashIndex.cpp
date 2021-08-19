/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/HashIndex.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Serializer.h>

namespace SQL {

HashDirectoryNode::HashDirectoryNode(HashIndex& index, u32 node_number, size_t offset)
    : IndexNode(index.node_pointer(node_number))
    , m_hash_index(index)
    , m_node_number(node_number)
    , m_offset(offset)
{
}

HashDirectoryNode::HashDirectoryNode(HashIndex& index, u32 pointer)
    : IndexNode(pointer)
    , m_hash_index(index)
{
}

void HashDirectoryNode::deserialize(Serializer& serializer)
{
    dbgln_if(SQL_DEBUG, "Deserializing Hash Directory Node");
    m_hash_index.m_global_depth = serializer.deserialize<u32>();
    auto size = serializer.deserialize<u32>();
    dbgln_if(SQL_DEBUG, "Global Depth {}, #Bucket pointers {}", m_hash_index.global_depth(), size);
    auto next_node = serializer.deserialize<u32>();
    if (next_node) {
        dbgln_if(SQL_DEBUG, "Next node {}", next_node);
        m_hash_index.m_nodes.append(next_node);
    } else {
        dbgln_if(SQL_DEBUG, "This is the last directory node");
        m_is_last = true;
    }
    for (auto ix = 0u; ix < size; ix++) {
        auto bucket_pointer = serializer.deserialize<u32>();
        auto local_depth = serializer.deserialize<u32>();
        dbgln_if(SQL_DEBUG, "--Index {} bucket pointer {} local depth {}", ix, bucket_pointer, local_depth);
        m_hash_index.append_bucket(ix, local_depth, bucket_pointer);
    }
}

void HashDirectoryNode::serialize(Serializer& serializer) const
{
    dbgln_if(SQL_DEBUG, "Serializing directory node #{}. Offset {}", m_node_number, m_offset);
    serializer.serialize<u32>((u32)m_hash_index.global_depth());
    serializer.serialize<u32>(number_of_pointers());
    dbgln_if(SQL_DEBUG, "Global depth {}, #bucket pointers {}", m_hash_index.global_depth(), number_of_pointers());

    u32 next_node;
    if (m_node_number < (m_hash_index.m_nodes.size() - 1)) {
        next_node = m_hash_index.m_nodes[m_node_number + 1];
        dbgln_if(SQL_DEBUG, "Next directory node pointer {}", next_node);
    } else {
        next_node = 0u;
        dbgln_if(SQL_DEBUG, "This is the last directory node");
    }

    serializer.serialize<u32>(next_node);
    for (auto ix = 0u; ix < number_of_pointers(); ix++) {
        auto& bucket = m_hash_index.m_buckets[m_offset + ix];
        dbgln_if(SQL_DEBUG, "Bucket index #{} pointer {} local depth {} size {}", ix, bucket->pointer(), bucket->local_depth(), bucket->size());
        serializer.serialize<u32>(bucket->pointer());
        serializer.serialize<u32>(bucket->local_depth());
    }
}

HashBucket::HashBucket(HashIndex& hash_index, u32 index, u32 local_depth, u32 pointer)
    : IndexNode(pointer)
    , m_hash_index(hash_index)
    , m_local_depth(local_depth)
    , m_index(index)
{
}

void HashBucket::serialize(Serializer& serializer) const
{
    dbgln_if(SQL_DEBUG, "Serializing bucket: pointer {}, index #{}, local depth {} size {}",
        pointer(), index(), local_depth(), size());
    serializer.serialize<u32>(local_depth());
    serializer.serialize<u32>(size());
    for (auto& key : m_entries) {
        serializer.serialize<Key>(key);
    }
}

void HashBucket::deserialize(Serializer& serializer)
{
    if (m_inflated || !pointer())
        return;
    dbgln_if(SQL_DEBUG, "Inflating Hash Bucket {}", pointer());
    m_local_depth = serializer.deserialize<u32>();
    dbgln_if(SQL_DEBUG, "Bucket Local Depth {}", m_local_depth);
    auto size = serializer.deserialize<u32>();
    dbgln_if(SQL_DEBUG, "Bucket has {} keys", size);
    for (auto ix = 0u; ix < size; ix++) {
        auto key = serializer.deserialize<Key>(m_hash_index.descriptor());
        dbgln_if(SQL_DEBUG, "Key {}: {}", ix, key.to_string());
        m_entries.append(key);
    }
    m_inflated = true;
}

size_t HashBucket::length() const
{
    size_t len = 2 * sizeof(u32);
    for (auto& key : m_entries) {
        len += key.length();
    }
    return len;
}

Optional<u32> HashBucket::get(Key& key)
{
    auto optional_index = find_key_in_bucket(key);
    if (optional_index.has_value()) {
        auto& k = m_entries[optional_index.value()];
        key.set_pointer(k.pointer());
        return k.pointer();
    }
    return {};
}

bool HashBucket::insert(Key const& key)
{
    if (!m_inflated)
        m_hash_index.serializer().deserialize_block_to(pointer(), *this);
    if (find_key_in_bucket(key).has_value()) {
        return false;
    }
    if ((length() + key.length()) > BLOCKSIZE) {
        dbgln_if(SQL_DEBUG, "Adding key {} would make length exceed block size", key.to_string());
        return false;
    }
    m_entries.append(key);
    m_hash_index.serializer().serialize_and_write(*this, pointer());
    return true;
}

Optional<size_t> HashBucket::find_key_in_bucket(Key const& key)
{
    for (auto ix = 0u; ix < size(); ix++) {
        auto& k = entries()[ix];
        if (k == key) {
            return ix;
        }
    }
    return {};
}

HashBucket const* HashBucket::next_bucket()
{
    for (auto ix = m_index + 1; ix < m_hash_index.size(); ix++) {
        auto bucket = m_hash_index.get_bucket_by_index(ix);
        m_hash_index.serializer().deserialize_block_to<HashBucket>(bucket->pointer(), *bucket);
        if (bucket->size())
            return bucket;
    }
    return nullptr;
}

HashBucket const* HashBucket::previous_bucket()
{
    for (auto ix = m_index - 1; ix > 0; ix--) {
        auto bucket = m_hash_index.get_bucket_by_index(ix);
        if (bucket->pointer())
            return bucket;
    }
    return nullptr;
}

Vector<Key> const& HashBucket::entries()
{
    if (!m_inflated)
        m_hash_index.serializer().deserialize_block_to(pointer(), *this);
    return m_entries;
}

Key const& HashBucket::operator[](size_t ix)
{
    if (!m_inflated)
        m_hash_index.serializer().deserialize_block_to(pointer(), *this);
    VERIFY(ix < size());
    return m_entries[ix];
}

Key const& HashBucket::operator[](size_t ix) const
{
    VERIFY(ix < m_entries.size());
    return m_entries[ix];
}

void HashBucket::list_bucket()
{
    warnln("Bucket #{} size {} local depth {} pointer {}{}",
        index(), size(), local_depth(), pointer(), (pointer() ? "" : " (VIRTUAL)"));
    for (auto& key : entries()) {
        warnln("  {} hash {}", key.to_string(), key.hash());
    }
}

HashIndex::HashIndex(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, u32 first_node)
    : Index(serializer, descriptor, true, first_node)
    , m_nodes()
    , m_buckets()
{
    if (!first_node) {
        set_pointer(new_record_pointer());
    }
    if (serializer.has_block(first_node)) {
        u32 pointer = first_node;
        do {
            VERIFY(serializer.has_block(pointer));
            auto node = serializer.deserialize_block<HashDirectoryNode>(pointer, *this, pointer);
            if (node.is_last())
                break;
            pointer = m_nodes.last(); // FIXME Ugly
        } while (pointer);
    } else {
        auto bucket = append_bucket(0u, 1u, new_record_pointer());
        bucket->m_inflated = true;
        serializer.serialize_and_write(*bucket, bucket->pointer());
        bucket = append_bucket(1u, 1u, new_record_pointer());
        bucket->m_inflated = true;
        serializer.serialize_and_write(*bucket, bucket->pointer());
        m_nodes.append(first_node);
        write_directory_to_write_ahead_log();
    }
}

HashBucket* HashIndex::get_bucket(u32 index)
{
    VERIFY(index < m_buckets.size());
    auto divisor = size() / 2;
    while (!m_buckets[index]->pointer()) {
        VERIFY(divisor > 1);
        index = index % divisor;
        divisor /= 2;
    }
    auto& bucket = m_buckets[index];
    return bucket;
}

HashBucket* HashIndex::get_bucket_for_insert(Key const& key)
{
    auto key_hash = key.hash();

    do {
        dbgln_if(SQL_DEBUG, "HashIndex::get_bucket_for_insert({}) bucket {} of {}", key.to_string(), key_hash % size(), size());
        auto bucket = get_bucket(key_hash % size());
        if (bucket->length() + key.length() < BLOCKSIZE) {
            return bucket;
        }
        dbgln_if(SQL_DEBUG, "Bucket is full (bucket size {}/length {} key length {}). Expanding directory", bucket->size(), bucket->length(), key.length());

        // We previously doubled the directory but the target bucket is
        // still at an older depth. Create new buckets at the current global
        // depth and allocate the contents of the existing buckets to the
        // newly created ones:
        while (bucket->local_depth() < global_depth()) {
            auto base_index = bucket->index();
            auto step = 1 << (global_depth() - bucket->local_depth());
            auto total_moved = 0;
            for (auto ix = base_index + step; ix < size(); ix += step) {
                auto& sub_bucket = m_buckets[ix];
                sub_bucket->set_local_depth(bucket->local_depth() + 1);
                auto moved = 0;
                for (auto entry_index = (int)bucket->m_entries.size() - 1; entry_index >= 0; entry_index--) {
                    if (bucket->m_entries[entry_index].hash() % size() == ix) {
                        if (!sub_bucket->pointer()) {
                            sub_bucket->set_pointer(new_record_pointer());
                        }
                        sub_bucket->insert(bucket->m_entries.take(entry_index));
                        moved++;
                    }
                }
                if (moved > 0) {
                    dbgln_if(SQL_DEBUG, "Moved {} entries from bucket #{} to #{}", moved, base_index, ix);
                    serializer().serialize_and_write(*sub_bucket, sub_bucket->pointer());
                }
                total_moved += moved;
            }
            if (total_moved)
                dbgln_if(SQL_DEBUG, "Redistributed {} entries from bucket #{}", total_moved, base_index);
            else
                dbgln_if(SQL_DEBUG, "Nothing redistributed from bucket #{}", base_index);
            bucket->set_local_depth(bucket->local_depth() + 1);
            serializer().serialize_and_write(*bucket, bucket->pointer());
            write_directory_to_write_ahead_log();

            auto bucket_after_redistribution = get_bucket(key_hash % size());
            if (bucket_after_redistribution->length() + key.length() < BLOCKSIZE)
                return bucket_after_redistribution;
        }
        expand();
    } while (true);
    VERIFY_NOT_REACHED();
}

void HashIndex::expand()
{
    auto sz = size();
    dbgln_if(SQL_DEBUG, "Expanding directory from {} to {} buckets", sz, 2 * sz);
    for (auto i = 0u; i < sz; i++) {
        auto bucket = get_bucket(i);
        bucket = append_bucket(sz + i, bucket->local_depth(), 0u);
        bucket->m_inflated = true;
    }
    m_global_depth++;
    write_directory_to_write_ahead_log();
}

void HashIndex::write_directory_to_write_ahead_log()
{
    auto num_nodes_required = (size() / HashDirectoryNode::max_pointers_in_node()) + 1;
    while (m_nodes.size() < num_nodes_required)
        m_nodes.append(new_record_pointer());

    size_t offset = 0u;
    size_t num_node = 0u;
    while (offset < size()) {
        HashDirectoryNode node(*this, num_node, offset);
        serializer().serialize_and_write(node, node.pointer());
        offset += node.number_of_pointers();
    }
}

HashBucket* HashIndex::append_bucket(u32 index, u32 local_depth, u32 pointer)
{
    m_buckets.append(make<HashBucket>(*this, index, local_depth, pointer));
    return m_buckets.last();
}

HashBucket* HashIndex::get_bucket_by_index(u32 index)
{
    if (index >= size())
        return nullptr;
    return m_buckets[index];
}

Optional<u32> HashIndex::get(Key& key)
{
    auto hash = key.hash();
    auto bucket_index = hash % size();
    dbgln_if(SQL_DEBUG, "HashIndex::get({}) bucket_index {}", key.to_string(), bucket_index);
    auto bucket = get_bucket(bucket_index);
    if constexpr (SQL_DEBUG)
        bucket->list_bucket();
    return bucket->get(key);
}

bool HashIndex::insert(Key const& key)
{
    dbgln_if(SQL_DEBUG, "HashIndex::insert({})", key.to_string());
    auto bucket = get_bucket_for_insert(key);
    bucket->insert(key);
    if constexpr (SQL_DEBUG)
        bucket->list_bucket();
    return true;
}

HashIndexIterator HashIndex::begin()
{
    return HashIndexIterator(get_bucket(0));
}

HashIndexIterator HashIndex::end()
{
    return HashIndexIterator::end();
}

HashIndexIterator HashIndex::find(Key const& key)
{
    auto hash = key.hash();
    auto bucket_index = hash % size();
    auto bucket = get_bucket(bucket_index);
    auto optional_index = bucket->find_key_in_bucket(key);
    if (!optional_index.has_value())
        return end();
    return HashIndexIterator(bucket, optional_index.value());
}

void HashIndex::list_hash()
{
    warnln("Number of buckets: {} (Global depth {})", size(), global_depth());
    warn("Directory pointer(s): ");
    for (auto ptr : m_nodes) {
        warn("{}, ", ptr);
    }
    warnln();

    bool first_bucket = true;
    for (auto& bucket : m_buckets) {
        if (first_bucket) {
            first_bucket = false;
        }
        bucket->list_bucket();
    }
}

HashIndexIterator::HashIndexIterator(HashBucket const* bucket, size_t index)
    : m_current(bucket)
    , m_index(index)
{
    VERIFY(!m_current || !index || (index < m_current->size()));
    while (m_current && (m_current->size() == 0)) {
        m_current = m_current->next_bucket();
        m_index = 0;
    }
}

HashIndexIterator HashIndexIterator::next()
{
    if (is_end())
        return *this;
    if (m_index < (m_current->size() - 1))
        return HashIndexIterator(m_current.ptr(), m_index + 1);
    return HashIndexIterator(m_current->next_bucket());
}

HashIndexIterator HashIndexIterator::previous()
{
    TODO();
}

bool HashIndexIterator::operator==(HashIndexIterator const& other) const
{
    if (is_end())
        return other.is_end();
    if (other.is_end())
        return false;
    VERIFY(&other.m_current->hash_index() == &m_current->hash_index());
    return (m_current.ptr() == other.m_current.ptr()) && (m_index == other.m_index);
}

bool HashIndexIterator::operator==(Key const& other) const
{
    if (is_end())
        return false;
    if (other.is_null())
        return false;
    return (**this).compare(other);
}

}
