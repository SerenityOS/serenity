/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/HashIndex.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Serialize.h>

namespace SQL {

HashDirectoryNode::HashDirectoryNode(HashIndex& index, u32 node_number, size_t offset)
    : IndexNode(index.node_pointer(node_number))
    , m_hash_index(index)
    , m_node_number(node_number)
    , m_offset(offset)
{
}

HashDirectoryNode::HashDirectoryNode(HashIndex& index, u32 pointer, ByteBuffer& buffer)
    : IndexNode(pointer)
    , m_hash_index(index)
{
    dbgln_if(SQL_DEBUG, "Deserializing Hash Directory Node");
    size_t offset = 0;
    deserialize_from<u32>(buffer, offset, index.m_global_depth);
    u32 size;
    deserialize_from<u32>(buffer, offset, size);
    dbgln_if(SQL_DEBUG, "Global Depth {}, #Bucket pointers {}", index.global_depth(), size);
    u32 next_node;
    deserialize_from<u32>(buffer, offset, next_node);
    if (next_node) {
        dbgln_if(SQL_DEBUG, "Next node {}", next_node);
        m_hash_index.m_nodes.append(next_node);
    } else {
        dbgln_if(SQL_DEBUG, "This is the last directory node");
        m_is_last = true;
    }
    for (auto ix = 0u; ix < size; ix++) {
        u32 bucket_pointer;
        deserialize_from(buffer, offset, bucket_pointer);
        u32 local_depth;
        deserialize_from(buffer, offset, local_depth);
        dbgln_if(SQL_DEBUG, "Bucket pointer {} local depth {}", bucket_pointer, local_depth);
        index.append_bucket(ix, local_depth, bucket_pointer);
    }
}

void HashDirectoryNode::serialize(ByteBuffer& buffer) const
{
    dbgln_if(SQL_DEBUG, "Serializing directory node #{}. Offset {}", m_node_number, m_offset);
    serialize_to(buffer, m_hash_index.global_depth());
    serialize_to(buffer, number_of_pointers());
    dbgln_if(SQL_DEBUG, "Global depth {}, #bucket pointers {}", m_hash_index.global_depth(), number_of_pointers());

    u32 next_node;
    if (m_node_number < (m_hash_index.m_nodes.size() - 1)) {
        next_node = m_hash_index.m_nodes[m_node_number + 1];
        dbgln_if(SQL_DEBUG, "Next directory node pointer {}", next_node);
    } else {
        next_node = 0u;
        dbgln_if(SQL_DEBUG, "This is the last directory node");
    }

    serialize_to(buffer, next_node);
    for (auto ix = 0u; ix < number_of_pointers(); ix++) {
        auto& bucket = m_hash_index.m_buckets[m_offset + ix];
        dbgln_if(SQL_DEBUG, "Bucket pointer {} local depth {}", bucket->pointer(), bucket->local_depth());
        serialize_to(buffer, bucket->pointer());
        serialize_to(buffer, bucket->local_depth());
    }
}

HashBucket::HashBucket(HashIndex& hash_index, u32 index, u32 local_depth, u32 pointer)
    : IndexNode(pointer)
    , m_hash_index(hash_index)
    , m_local_depth(local_depth)
    , m_index(index)
{
}

void HashBucket::serialize(ByteBuffer& buffer) const
{
    dbgln_if(SQL_DEBUG, "Serializing bucket: pointer {}, index #{}, local depth {} size {}",
        pointer(), index(), local_depth(), size());
    dbgln_if(SQL_DEBUG, "key_length: {} max_entries: {}", m_hash_index.descriptor()->data_length(), max_entries_in_bucket());
    serialize_to(buffer, local_depth());
    serialize_to(buffer, size());
    dbgln_if(SQL_DEBUG, "buffer size after prolog {}", buffer.size());
    for (auto& key : m_entries) {
        key.serialize(buffer);
        dbgln_if(SQL_DEBUG, "Key {} buffer size {}", key.to_string(), buffer.size());
    }
}

void HashBucket::inflate()
{
    if (m_inflated || !pointer())
        return;
    dbgln_if(SQL_DEBUG, "Inflating Hash Bucket {}", pointer());
    auto buffer = m_hash_index.read_block(pointer());
    size_t offset = 0;
    deserialize_from(buffer, offset, m_local_depth);
    dbgln_if(SQL_DEBUG, "Bucket Local Depth {}", m_local_depth);
    u32 size;
    deserialize_from(buffer, offset, size);
    dbgln_if(SQL_DEBUG, "Bucket has {} keys", size);
    for (auto ix = 0u; ix < size; ix++) {
        Key key(m_hash_index.descriptor(), buffer, offset);
        dbgln_if(SQL_DEBUG, "Key {}: {}", ix, key.to_string());
        m_entries.append(key);
    }
    m_inflated = true;
}

size_t HashBucket::max_entries_in_bucket() const
{
    auto key_size = m_hash_index.descriptor()->data_length() + sizeof(u32);
    return (BLOCKSIZE - 2 * sizeof(u32)) / key_size;
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
    inflate();
    if (find_key_in_bucket(key).has_value()) {
        return false;
    }
    if (size() >= max_entries_in_bucket()) {
        return false;
    }
    m_entries.append(key);
    m_hash_index.add_to_write_ahead_log(this);
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
        bucket->inflate();
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

Key const& HashBucket::operator[](size_t ix)
{
    inflate();
    VERIFY(ix < size());
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

HashIndex::HashIndex(Heap& heap, NonnullRefPtr<TupleDescriptor> const& descriptor, u32 first_node)
    : Index(heap, descriptor, true, first_node)
    , m_nodes()
    , m_buckets()
{
    if (!first_node) {
        set_pointer(new_record_pointer());
    }
    if (this->heap().has_block(first_node)) {
        u32 pointer = first_node;
        do {
            VERIFY(this->heap().has_block(pointer));
            auto buffer = read_block(pointer);
            auto node = HashDirectoryNode(*this, pointer, buffer);
            if (node.is_last())
                break;
            pointer = m_nodes.last(); // FIXME Ugly
        } while (pointer);
    } else {
        auto bucket = append_bucket(0u, 1u, new_record_pointer());
        bucket->m_inflated = true;
        add_to_write_ahead_log(bucket);
        bucket = append_bucket(1u, 1u, new_record_pointer());
        bucket->m_inflated = true;
        add_to_write_ahead_log(bucket);
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
        auto bucket = get_bucket(key_hash % size());
        if (bucket->size() < bucket->max_entries_in_bucket()) {
            return bucket;
        }

        // We previously doubled the directory but the target bucket is
        // still at an older depth. Create new buckets at the current global
        // depth and allocate the contents of the existing buckets to the
        // newly created ones:
        while (bucket->local_depth() < global_depth()) {
            auto base_index = bucket->index();
            auto step = 1 << (global_depth() - bucket->local_depth());
            for (auto ix = base_index + step; ix < size(); ix += step) {
                auto& sub_bucket = m_buckets[ix];
                sub_bucket->set_local_depth(bucket->local_depth() + 1);
                for (auto entry_index = (int)bucket->m_entries.size() - 1; entry_index >= 0; entry_index--) {
                    if (bucket->m_entries[entry_index].hash() % size() == ix) {
                        if (!sub_bucket->pointer()) {
                            sub_bucket->set_pointer(new_record_pointer());
                        }
                        sub_bucket->insert(bucket->m_entries.take(entry_index));
                    }
                }
                if (m_buckets[ix]->pointer())
                    add_to_write_ahead_log(m_buckets[ix]);
            }
            bucket->set_local_depth(bucket->local_depth() + 1);
            add_to_write_ahead_log(bucket);
            write_directory_to_write_ahead_log();

            auto bucket_after_redistribution = get_bucket(key_hash % size());
            if (bucket_after_redistribution->size() < bucket_after_redistribution->max_entries_in_bucket()) {
                return bucket_after_redistribution;
            }
        }
        expand();
    } while (true);
}

void HashIndex::expand()
{
    auto sz = size();
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
        add_to_write_ahead_log(node.as_index_node());
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
    auto bucket = get_bucket(bucket_index);
    return bucket->get(key);
}

bool HashIndex::insert(Key const& key)
{
    auto bucket = get_bucket_for_insert(key);
    bucket->insert(key);
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
            warnln("Max. keys in bucket {}", bucket->max_entries_in_bucket());
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
