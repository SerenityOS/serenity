/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashFunctions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

enum class HashSetResult {
    InsertedNewEntry,
    ReplacedExistingEntry
};

template<typename HashTableType, typename T, typename BucketType>
class HashTableIterator {
    friend HashTableType;

public:
    bool operator==(const HashTableIterator& other) const { return m_bucket == other.m_bucket; }
    bool operator!=(const HashTableIterator& other) const { return m_bucket != other.m_bucket; }
    T& operator*() { return *m_bucket->slot(); }
    T* operator->() { return m_bucket->slot(); }
    void operator++() { skip_to_next(); }

private:
    void skip_to_next()
    {
        if (!m_bucket)
            return;
        do {
            ++m_bucket;
            if (m_bucket->used)
                return;
        } while (!m_bucket->end);
        if (m_bucket->end)
            m_bucket = nullptr;
    }

    explicit HashTableIterator(BucketType* bucket)
        : m_bucket(bucket)
    {
    }

    BucketType* m_bucket { nullptr };
};

template<typename T, typename TraitsForT>
class HashTable {
    static constexpr size_t load_factor_in_percent = 60;

    struct Bucket {
        bool used;
        bool deleted;
        bool end;
        alignas(T) u8 storage[sizeof(T)];

        T* slot() { return reinterpret_cast<T*>(storage); }
        const T* slot() const { return reinterpret_cast<const T*>(storage); }
    };

public:
    HashTable() = default;
    explicit HashTable(size_t capacity) { rehash(capacity); }

    ~HashTable()
    {
        if (!m_buckets)
            return;

        for (size_t i = 0; i < m_capacity; ++i) {
            if (m_buckets[i].used)
                m_buckets[i].slot()->~T();
        }

        kfree(m_buckets);
    }

    HashTable(const HashTable& other)
    {
        rehash(other.capacity());
        for (auto& it : other)
            set(it);
    }

    HashTable& operator=(const HashTable& other)
    {
        HashTable temporary(other);
        swap(*this, temporary);
        return *this;
    }

    HashTable(HashTable&& other) noexcept
        : m_buckets(other.m_buckets)
        , m_size(other.m_size)
        , m_capacity(other.m_capacity)
        , m_deleted_count(other.m_deleted_count)
    {
        other.m_size = 0;
        other.m_capacity = 0;
        other.m_deleted_count = 0;
        other.m_buckets = nullptr;
    }

    HashTable& operator=(HashTable&& other) noexcept
    {
        HashTable temporary { move(other) };
        swap(*this, temporary);
        return *this;
    }

    friend void swap(HashTable& a, HashTable& b) noexcept
    {
        swap(a.m_buckets, b.m_buckets);
        swap(a.m_size, b.m_size);
        swap(a.m_capacity, b.m_capacity);
        swap(a.m_deleted_count, b.m_deleted_count);
    }

    [[nodiscard]] bool is_empty() const { return !m_size; }
    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] size_t capacity() const { return m_capacity; }

    template<typename U, size_t N>
    void set_from(U (&from_array)[N])
    {
        for (size_t i = 0; i < N; ++i) {
            set(from_array[i]);
        }
    }

    void ensure_capacity(size_t capacity)
    {
        VERIFY(capacity >= size());
        rehash(capacity * 2);
    }

    bool contains(const T& value) const
    {
        return find(value) != end();
    }

    using Iterator = HashTableIterator<HashTable, T, Bucket>;

    Iterator begin()
    {
        for (size_t i = 0; i < m_capacity; ++i) {
            if (m_buckets[i].used)
                return Iterator(&m_buckets[i]);
        }
        return end();
    }

    Iterator end()
    {
        return Iterator(nullptr);
    }

    using ConstIterator = HashTableIterator<const HashTable, const T, const Bucket>;

    ConstIterator begin() const
    {
        for (size_t i = 0; i < m_capacity; ++i) {
            if (m_buckets[i].used)
                return ConstIterator(&m_buckets[i]);
        }
        return end();
    }

    ConstIterator end() const
    {
        return ConstIterator(nullptr);
    }

    void clear()
    {
        *this = HashTable();
    }

    template<typename U = T>
    HashSetResult set(U&& value)
    {
        auto& bucket = lookup_for_writing(value);
        if (bucket.used) {
            (*bucket.slot()) = forward<U>(value);
            return HashSetResult::ReplacedExistingEntry;
        }

        new (bucket.slot()) T(forward<U>(value));
        bucket.used = true;
        if (bucket.deleted) {
            bucket.deleted = false;
            --m_deleted_count;
        }
        ++m_size;
        return HashSetResult::InsertedNewEntry;
    }

    template<typename Finder>
    Iterator find(unsigned hash, Finder finder)
    {
        return Iterator(lookup_with_hash(hash, move(finder)));
    }

    Iterator find(const T& value)
    {
        return find(TraitsForT::hash(value), [&](auto& other) { return TraitsForT::equals(value, other); });
    }

    template<typename Finder>
    ConstIterator find(unsigned hash, Finder finder) const
    {
        return ConstIterator(lookup_with_hash(hash, move(finder)));
    }

    ConstIterator find(const T& value) const
    {
        return find(TraitsForT::hash(value), [&](auto& other) { return TraitsForT::equals(value, other); });
    }

    bool remove(const T& value)
    {
        auto it = find(value);
        if (it != end()) {
            remove(it);
            return true;
        }
        return false;
    }

    void remove(Iterator iterator)
    {
        VERIFY(iterator.m_bucket);
        auto& bucket = *iterator.m_bucket;
        VERIFY(bucket.used);
        VERIFY(!bucket.end);
        VERIFY(!bucket.deleted);
        bucket.slot()->~T();
        bucket.used = false;
        bucket.deleted = true;
        --m_size;
        ++m_deleted_count;
    }

private:
    void insert_during_rehash(T&& value)
    {
        auto& bucket = lookup_for_writing(value);
        new (bucket.slot()) T(move(value));
        bucket.used = true;
    }

    void rehash(size_t new_capacity)
    {
        new_capacity = max(new_capacity, static_cast<size_t>(4));
        new_capacity = kmalloc_good_size(new_capacity * sizeof(Bucket)) / sizeof(Bucket);

        auto* old_buckets = m_buckets;
        auto old_capacity = m_capacity;

        m_buckets = (Bucket*)kmalloc(sizeof(Bucket) * (new_capacity + 1));
        __builtin_memset(m_buckets, 0, sizeof(Bucket) * (new_capacity + 1));
        m_capacity = new_capacity;
        m_deleted_count = 0;

        m_buckets[m_capacity].end = true;

        if (!old_buckets)
            return;

        for (size_t i = 0; i < old_capacity; ++i) {
            auto& old_bucket = old_buckets[i];
            if (old_bucket.used) {
                insert_during_rehash(move(*old_bucket.slot()));
                old_bucket.slot()->~T();
            }
        }

        kfree(old_buckets);
    }

    template<typename Finder>
    Bucket* lookup_with_hash(unsigned hash, Finder finder) const
    {
        if (is_empty())
            return nullptr;

        for (;;) {
            auto& bucket = m_buckets[hash % m_capacity];

            if (bucket.used && finder(*bucket.slot()))
                return &bucket;

            if (!bucket.used && !bucket.deleted)
                return nullptr;

            hash = double_hash(hash);
        }
    }

    const Bucket* lookup_for_reading(const T& value) const
    {
        return lookup_with_hash(TraitsForT::hash(value), [&value](auto& entry) { return TraitsForT::equals(entry, value); });
    }

    Bucket& lookup_for_writing(const T& value)
    {
        if (should_grow())
            rehash(capacity() * 2);

        auto hash = TraitsForT::hash(value);
        Bucket* first_empty_bucket = nullptr;
        for (;;) {
            auto& bucket = m_buckets[hash % m_capacity];

            if (bucket.used && TraitsForT::equals(*bucket.slot(), value))
                return bucket;

            if (!bucket.used) {
                if (!first_empty_bucket)
                    first_empty_bucket = &bucket;

                if (!bucket.deleted)
                    return *const_cast<Bucket*>(first_empty_bucket);
            }

            hash = double_hash(hash);
        }
    }

    [[nodiscard]] size_t used_bucket_count() const { return m_size + m_deleted_count; }
    [[nodiscard]] bool should_grow() const { return ((used_bucket_count() + 1) * 100) >= (m_capacity * load_factor_in_percent); }

    Bucket* m_buckets { nullptr };
    size_t m_size { 0 };
    size_t m_capacity { 0 };
    size_t m_deleted_count { 0 };
};

}

using AK::HashTable;
