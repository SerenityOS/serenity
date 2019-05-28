#pragma once

#include "Assertions.h"
#include "DoublyLinkedList.h"
#include "StdLibExtras.h"
#include "Traits.h"
#include "kstdio.h"

//#define HASHTABLE_DEBUG

namespace AK {

template<typename T, typename = Traits<T>>
class HashTable;

template<typename T, typename TraitsForT>
class HashTable {
private:
    struct Bucket {
        DoublyLinkedList<T> chain;
    };

public:
    HashTable() {}
    explicit HashTable(HashTable&& other)
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

    void dump() const;

    class Iterator {
    public:
        bool operator!=(const Iterator& other) const
        {
            if (m_is_end && other.m_is_end)
                return false;
            return &m_table != &other.m_table
                || m_is_end != other.m_is_end
                || m_bucket_index != other.m_bucket_index
                || m_bucket_iterator != other.m_bucket_iterator;
        }
        bool operator==(const Iterator& other) const { return !(*this != other); }
        T& operator*()
        {
#ifdef HASHTABLE_DEBUG
            kprintf("retrieve { bucket_index: %u, is_end: %u }\n", m_bucket_index, m_is_end);
#endif
            return *m_bucket_iterator;
        }
        T* operator->() { return m_bucket_iterator.operator->(); }
        Iterator& operator++()
        {
            skip_to_next();
            return *this;
        }

        void skip_to_next()
        {
#ifdef HASHTABLE_DEBUG
            unsigned pass = 0;
#endif
            while (!m_is_end) {
#ifdef HASHTABLE_DEBUG
                ++pass;
                kprintf("skip_to_next pass %u, m_bucket_index=%u\n", pass, m_bucket_index);
#endif
                if (m_bucket_iterator.is_end()) {
                    ++m_bucket_index;
                    if (m_bucket_index >= m_table.capacity()) {
                        m_is_end = true;
                        return;
                    }
                    m_bucket_iterator = m_table.m_buckets[m_bucket_index].chain.begin();
                } else {
                    ++m_bucket_iterator;
                }
                if (!m_bucket_iterator.is_end())
                    return;
            }
        }

    private:
        friend class HashTable;
        explicit Iterator(HashTable& table, bool is_end, typename DoublyLinkedList<T>::Iterator bucket_iterator = DoublyLinkedList<T>::Iterator::universal_end(), int bucket_index = 0)
            : m_table(table)
            , m_bucket_index(bucket_index)
            , m_is_end(is_end)
            , m_bucket_iterator(bucket_iterator)
        {
            if (!is_end && !m_table.is_empty() && !(m_bucket_iterator != DoublyLinkedList<T>::Iterator::universal_end())) {
#ifdef HASHTABLE_DEBUG
                kprintf("bucket iterator init!\n");
#endif
                m_bucket_iterator = m_table.m_buckets[0].chain.begin();
                if (m_bucket_iterator.is_end())
                    skip_to_next();
            }
        }

        HashTable& m_table;
        int m_bucket_index { 0 };
        bool m_is_end { false };
        typename DoublyLinkedList<T>::Iterator m_bucket_iterator;
    };

    Iterator begin() { return Iterator(*this, is_empty()); }
    Iterator end() { return Iterator(*this, true); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) const
        {
            if (m_is_end && other.m_is_end)
                return false;
            return &m_table != &other.m_table
                || m_is_end != other.m_is_end
                || m_bucket_index != other.m_bucket_index
                || m_bucket_iterator != other.m_bucket_iterator;
        }
        bool operator==(const ConstIterator& other) const { return !(*this != other); }
        const T& operator*() const
        {
#ifdef HASHTABLE_DEBUG
            kprintf("retrieve { bucket_index: %u, is_end: %u }\n", m_bucket_index, m_is_end);
#endif
            return *m_bucket_iterator;
        }
        const T* operator->() const { return m_bucket_iterator.operator->(); }
        ConstIterator& operator++()
        {
            skip_to_next();
            return *this;
        }

        void skip_to_next()
        {
#ifdef HASHTABLE_DEBUG
            unsigned pass = 0;
#endif
            while (!m_is_end) {
#ifdef HASHTABLE_DEBUG
                ++pass;
                kprintf("skip_to_next pass %u, m_bucket_index=%u\n", pass, m_bucket_index);
#endif
                if (m_bucket_iterator.is_end()) {
                    ++m_bucket_index;
                    if (m_bucket_index >= m_table.capacity()) {
                        m_is_end = true;
                        return;
                    }
                    const DoublyLinkedList<T>& chain = m_table.m_buckets[m_bucket_index].chain;
                    m_bucket_iterator = chain.begin();
                } else {
                    ++m_bucket_iterator;
                }
                if (!m_bucket_iterator.is_end())
                    return;
            }
        }

    private:
        friend class HashTable;
        ConstIterator(const HashTable& table, bool is_end, typename DoublyLinkedList<T>::ConstIterator bucket_iterator = DoublyLinkedList<T>::ConstIterator::universal_end(), int bucket_index = 0)
            : m_table(table)
            , m_bucket_index(bucket_index)
            , m_is_end(is_end)
            , m_bucket_iterator(bucket_iterator)
        {
            if (!is_end && !m_table.is_empty() && !(m_bucket_iterator != DoublyLinkedList<T>::ConstIterator::universal_end())) {
#ifdef HASHTABLE_DEBUG
                kprintf("const bucket iterator init!\n");
#endif
                const DoublyLinkedList<T>& chain = m_table.m_buckets[0].chain;
                m_bucket_iterator = chain.begin();
                if (m_bucket_iterator.is_end())
                    skip_to_next();
            }
        }

        const HashTable& m_table;
        int m_bucket_index { 0 };
        bool m_is_end { false };
        typename DoublyLinkedList<T>::ConstIterator m_bucket_iterator;
    };

    ConstIterator begin() const { return ConstIterator(*this, is_empty()); }
    ConstIterator end() const { return ConstIterator(*this, true); }

    Iterator find(const T&);
    ConstIterator find(const T&) const;

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
    void rehash(int capacity);
    void insert(const T&);
    void insert(T&&);

    Bucket* m_buckets { nullptr };

    int m_size { 0 };
    int m_capacity { 0 };
};

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::set(T&& value)
{
    if (!m_capacity)
        rehash(1);
    auto& bucket = lookup(value);
    for (auto& e : bucket.chain) {
        if (e == value) {
            e = move(value);
            return;
        }
    }
    if (size() >= capacity()) {
        rehash(size() + 1);
        insert(move(value));
    } else {
        bucket.chain.append(move(value));
    }
    m_size++;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::set(const T& value)
{
    if (!m_capacity)
        rehash(1);
    auto& bucket = lookup(value);
    for (auto& e : bucket.chain) {
        if (e == value) {
            e = move(value);
            return;
        }
    }
    if (size() >= capacity()) {
        rehash(size() + 1);
        insert(value);
    } else {
        bucket.chain.append(value);
    }
    m_size++;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::rehash(int new_capacity)
{
    new_capacity *= 2;
#ifdef HASHTABLE_DEBUG
    kprintf("rehash to %u buckets\n", new_capacity);
#endif
    auto* new_buckets = new Bucket[new_capacity];
    auto* old_buckets = m_buckets;
    int old_capacity = m_capacity;
    m_buckets = new_buckets;
    m_capacity = new_capacity;

#ifdef HASHTABLE_DEBUG
    kprintf("reinsert %u buckets\n", old_capacity);
#endif
    for (int i = 0; i < old_capacity; ++i) {
        for (auto& value : old_buckets[i].chain) {
            insert(move(value));
        }
    }

    delete[] old_buckets;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::clear()
{
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
    bucket.chain.append(move(value));
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::insert(const T& value)
{
    auto& bucket = lookup(value);
    bucket.chain.append(value);
}

template<typename T, typename TraitsForT>
bool HashTable<T, TraitsForT>::contains(const T& value) const
{
    if (is_empty())
        return false;
    auto& bucket = lookup(value);
    for (auto& e : bucket.chain) {
        if (e == value)
            return true;
    }
    return false;
}

template<typename T, typename TraitsForT>
auto HashTable<T, TraitsForT>::find(const T& value) -> Iterator
{
    if (is_empty())
        return end();
    int bucket_index;
    auto& bucket = lookup(value, &bucket_index);
    auto bucket_iterator = bucket.chain.find(value);
    if (bucket_iterator != bucket.chain.end())
        return Iterator(*this, false, bucket_iterator, bucket_index);
    return end();
}

template<typename T, typename TraitsForT>
auto HashTable<T, TraitsForT>::find(const T& value) const -> ConstIterator
{
    if (is_empty())
        return end();
    int bucket_index;
    auto& bucket = lookup(value, &bucket_index);
    auto bucket_iterator = bucket.chain.find(value);
    if (bucket_iterator != bucket.chain.end())
        return ConstIterator(*this, false, bucket_iterator, bucket_index);
    return end();
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::remove(Iterator it)
{
    ASSERT(!is_empty());
    m_buckets[it.m_bucket_index].chain.remove(it.m_bucket_iterator);
    --m_size;
}

template<typename T, typename TraitsForT>
typename HashTable<T, TraitsForT>::Bucket& HashTable<T, TraitsForT>::lookup(const T& value, int* bucket_index)
{
    unsigned hash = TraitsForT::hash(value);
#ifdef HASHTABLE_DEBUG
    kprintf("hash for ");
    TraitsForT::dump(value);
    kprintf(" is %u\n", hash);
#endif
    if (bucket_index)
        *bucket_index = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

template<typename T, typename TraitsForT>
const typename HashTable<T, TraitsForT>::Bucket& HashTable<T, TraitsForT>::lookup(const T& value, int* bucket_index) const
{
    unsigned hash = TraitsForT::hash(value);
#ifdef HASHTABLE_DEBUG
    kprintf("hash for ");
    TraitsForT::dump(value);
    kprintf(" is %u\n", hash);
#endif
    if (bucket_index)
        *bucket_index = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::dump() const
{
    kprintf("HashTable{%p} m_size=%u, m_capacity=%u, m_buckets=%p\n", this, m_size, m_capacity, m_buckets);
    for (int i = 0; i < m_capacity; ++i) {
        auto& bucket = m_buckets[i];
        kprintf("Bucket %u\n", i);
        for (auto& e : bucket.chain) {
            kprintf("  > ");
            TraitsForT::dump(e);
            kprintf("\n");
        }
    }
}

}

using AK::HashTable;
