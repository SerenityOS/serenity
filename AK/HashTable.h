/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/SinglyLinkedList.h>
#include <AK/StdLibExtras.h>
#include <AK/TemporaryChange.h>
#include <AK/Traits.h>

namespace AK {

template<typename T, typename>
class HashTable;

template<typename HashTableType, typename ElementType, typename BucketIteratorType>
class HashTableIterator {
public:
    bool operator!=(const HashTableIterator& other) const
    {
        if (m_is_end && other.m_is_end)
            return false;
        return &m_table != &other.m_table
            || m_is_end != other.m_is_end
            || m_bucket_index != other.m_bucket_index
            || m_bucket_iterator != other.m_bucket_iterator;
    }
    bool operator==(const HashTableIterator& other) const { return !(*this != other); }
    ElementType& operator*() { return *m_bucket_iterator; }
    ElementType* operator->() { return m_bucket_iterator.operator->(); }
    HashTableIterator& operator++()
    {
        skip_to_next();
        return *this;
    }

    void skip_to_next()
    {
        while (!m_is_end) {
            if (m_bucket_iterator.is_end()) {
                ++m_bucket_index;
                if (m_bucket_index >= m_table.capacity()) {
                    m_is_end = true;
                    return;
                }
                m_bucket_iterator = m_table.bucket(m_bucket_index).begin();
            } else {
                ++m_bucket_iterator;
            }
            if (!m_bucket_iterator.is_end())
                return;
        }
    }

private:
    friend HashTableType;

    explicit HashTableIterator(HashTableType& table, bool is_end, BucketIteratorType bucket_iterator = BucketIteratorType::universal_end(), int bucket_index = 0)
        : m_table(table)
        , m_bucket_index(bucket_index)
        , m_is_end(is_end)
        , m_bucket_iterator(bucket_iterator)
    {
        ASSERT(!table.m_clearing);
        ASSERT(!table.m_rehashing);
        if (!is_end && !m_table.is_empty() && !(m_bucket_iterator != BucketIteratorType::universal_end())) {
            m_bucket_iterator = m_table.bucket(0).begin();
            if (m_bucket_iterator.is_end())
                skip_to_next();
        }
    }

    HashTableType& m_table;
    int m_bucket_index { 0 };
    bool m_is_end { false };
    BucketIteratorType m_bucket_iterator;
};

template<typename T, typename TraitsForT>
class HashTable {
private:
    using Bucket = SinglyLinkedList<T>;

public:
    HashTable() {}
    HashTable(const HashTable& other)
    {
        ensure_capacity(other.size());
        for (auto& it : other)
            set(it);
    }
    HashTable& operator=(const HashTable& other)
    {
        if (this != &other) {
            clear();
            ensure_capacity(other.size());
            for (auto& it : other)
                set(it);
        }
        return *this;
    }
    HashTable(HashTable&& other)
        : m_buckets(other.m_buckets)
        , m_size(other.m_size)
        , m_capacity(other.m_capacity)
    {
        other.m_size = 0;
        other.m_capacity = 0;
        other.m_buckets = nullptr;
    }
    HashTable& operator=(HashTable&& other)
    {
        if (this != &other) {
            clear();
            m_buckets = other.m_buckets;
            m_size = other.m_size;
            m_capacity = other.m_capacity;
            other.m_size = 0;
            other.m_capacity = 0;
            other.m_buckets = nullptr;
        }
        return *this;
    }

    ~HashTable() { clear(); }
    bool is_empty() const { return !m_size; }
    int size() const { return m_size; }
    int capacity() const { return m_capacity; }

    void ensure_capacity(int capacity)
    {
        ASSERT(capacity >= size());
        rehash(capacity);
    }

    void set(const T&);
    void set(T&&);
    bool contains(const T&) const;
    void clear();

    using Iterator = HashTableIterator<HashTable, T, typename Bucket::Iterator>;
    friend Iterator;
    Iterator begin() { return Iterator(*this, is_empty()); }
    Iterator end() { return Iterator(*this, true); }

    using ConstIterator = HashTableIterator<const HashTable, const T, typename Bucket::ConstIterator>;
    friend ConstIterator;
    ConstIterator begin() const { return ConstIterator(*this, is_empty()); }
    ConstIterator end() const { return ConstIterator(*this, true); }

    template<typename Finder>
    Iterator find(unsigned hash, Finder finder)
    {
        if (is_empty())
            return end();
        int bucket_index;
        auto& bucket = lookup_with_hash(hash, &bucket_index);
        auto bucket_iterator = bucket.find(finder);
        if (bucket_iterator != bucket.end())
            return Iterator(*this, false, bucket_iterator, bucket_index);
        return end();
    }

    template<typename Finder>
    ConstIterator find(unsigned hash, Finder finder) const
    {
        if (is_empty())
            return end();
        int bucket_index;
        auto& bucket = lookup_with_hash(hash, &bucket_index);
        auto bucket_iterator = bucket.find(finder);
        if (bucket_iterator != bucket.end())
            return ConstIterator(*this, false, bucket_iterator, bucket_index);
        return end();
    }

    Iterator find(const T& value)
    {
        return find(TraitsForT::hash(value), [&](auto& other) { return TraitsForT::equals(value, other); });
    }

    ConstIterator find(const T& value) const
    {
        return find(TraitsForT::hash(value), [&](auto& other) { return TraitsForT::equals(value, other); });
    }

    void remove(const T& value)
    {
        auto it = find(value);
        if (it != end())
            remove(it);
    }

    void remove(Iterator);

private:
    Bucket& lookup(const T&, int* bucket_index = nullptr);
    const Bucket& lookup(const T&, int* bucket_index = nullptr) const;

    Bucket& lookup_with_hash(unsigned hash, int* bucket_index)
    {
        if (bucket_index)
            *bucket_index = hash % m_capacity;
        return m_buckets[hash % m_capacity];
    }

    const Bucket& lookup_with_hash(unsigned hash, int* bucket_index) const
    {
        if (bucket_index)
            *bucket_index = hash % m_capacity;
        return m_buckets[hash % m_capacity];
    }

    void rehash(int capacity);
    void insert(const T&);
    void insert(T&&);

    Bucket& bucket(int index) { return m_buckets[index]; }
    const Bucket& bucket(int index) const { return m_buckets[index]; }

    Bucket* m_buckets { nullptr };

    int m_size { 0 };
    int m_capacity { 0 };
    bool m_clearing { false };
    bool m_rehashing { false };
};

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::set(T&& value)
{
    if (!m_capacity)
        rehash(1);
    auto& bucket = lookup(value);
    for (auto& e : bucket) {
        if (TraitsForT::equals(e, value)) {
            e = move(value);
            return;
        }
    }
    if (size() >= capacity()) {
        rehash(size() + 1);
        insert(move(value));
    } else {
        bucket.append(move(value));
    }
    m_size++;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::set(const T& value)
{
    if (!m_capacity)
        rehash(1);
    auto& bucket = lookup(value);
    for (auto& e : bucket) {
        if (TraitsForT::equals(e, value)) {
            e = value;
            return;
        }
    }
    if (size() >= capacity()) {
        rehash(size() + 1);
        insert(value);
    } else {
        bucket.append(value);
    }
    m_size++;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::rehash(int new_capacity)
{
    TemporaryChange<bool> change(m_rehashing, true);
    new_capacity *= 2;
    auto* new_buckets = new Bucket[new_capacity];
    auto* old_buckets = m_buckets;
    int old_capacity = m_capacity;
    m_buckets = new_buckets;
    m_capacity = new_capacity;

    for (int i = 0; i < old_capacity; ++i) {
        for (auto& value : old_buckets[i]) {
            insert(move(value));
        }
    }

    delete[] old_buckets;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::clear()
{
    TemporaryChange<bool> change(m_clearing, true);
    if (m_buckets) {
        delete[] m_buckets;
        m_buckets = nullptr;
    }
    m_capacity = 0;
    m_size = 0;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::insert(T&& value)
{
    auto& bucket = lookup(value);
    bucket.append(move(value));
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::insert(const T& value)
{
    auto& bucket = lookup(value);
    bucket.append(value);
}

template<typename T, typename TraitsForT>
bool HashTable<T, TraitsForT>::contains(const T& value) const
{
    if (is_empty())
        return false;
    auto& bucket = lookup(value);
    for (auto& e : bucket) {
        if (TraitsForT::equals(e, value))
            return true;
    }
    return false;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::remove(Iterator it)
{
    ASSERT(!is_empty());
    m_buckets[it.m_bucket_index].remove(it.m_bucket_iterator);
    --m_size;
}

template<typename T, typename TraitsForT>
auto HashTable<T, TraitsForT>::lookup(const T& value, int* bucket_index) -> Bucket&
{
    unsigned hash = TraitsForT::hash(value);
    if (bucket_index)
        *bucket_index = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

template<typename T, typename TraitsForT>
auto HashTable<T, TraitsForT>::lookup(const T& value, int* bucket_index) const -> const Bucket&
{
    unsigned hash = TraitsForT::hash(value);
    if (bucket_index)
        *bucket_index = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

}

using AK::HashTable;
