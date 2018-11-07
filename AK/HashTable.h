#pragma once

#include "Assertions.h"
#include "DoublyLinkedList.h"
#include "Traits.h"
#include "StdLib.h"
#include "kstdio.h"

//#define HASHTABLE_DEBUG

namespace AK {

template<typename T, typename = Traits<T>> class HashTable;

template<typename T, typename TraitsForT>
class HashTable {
private:
    struct Bucket {
        DoublyLinkedList<T> chain;
    };

public:
    HashTable() { }
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
    bool isEmpty() const { return !m_size; }
    unsigned size() const { return m_size; }
    unsigned capacity() const { return m_capacity; }

    void set(const T&);
    void set(T&&);
    bool contains(const T&) const;
    void clear();

    void dump() const;

    class Iterator {
    public:
        bool operator!=(const Iterator& other) const
        {
            if (m_isEnd && other.m_isEnd)
                return false;
            return &m_table != &other.m_table
                || m_isEnd != other.m_isEnd
                || m_bucketIndex != other.m_bucketIndex
                || m_bucketIterator != other.m_bucketIterator;
        }
        bool operator==(const Iterator& other) const { return !(*this != other); }
        T& operator*()
        {
#ifdef HASHTABLE_DEBUG
            kprintf("retrieve { bucketIndex: %u, isEnd: %u }\n", m_bucketIndex, m_isEnd);
#endif
            return *m_bucketIterator;
        }
        Iterator& operator++()
        {
            skipToNext();
            return *this;
        }

        void skipToNext()
        {
#ifdef HASHTABLE_DEBUG
            unsigned pass = 0;
#endif
            while (!m_isEnd) {
#ifdef HASHTABLE_DEBUG
                ++pass;
                kprintf("skipToNext pass %u, m_bucketIndex=%u\n", pass, m_bucketIndex);
#endif
                if (m_bucketIterator.isEnd()) {
                    ++m_bucketIndex;
                    if (m_bucketIndex >= m_table.capacity()) {
                        m_isEnd = true;
                        return;
                    }
                    m_bucketIterator = m_table.m_buckets[m_bucketIndex].chain.begin();
                } else {
                    ++m_bucketIterator;
                }
                if (!m_bucketIterator.isEnd())
                    return;
            }
        }
    private:
        friend class HashTable;
        explicit Iterator(HashTable& table, bool isEnd, typename DoublyLinkedList<T>::Iterator bucketIterator = DoublyLinkedList<T>::Iterator::universalEnd(), unsigned bucketIndex = 0)
            : m_table(table)
            , m_bucketIndex(bucketIndex)
            , m_isEnd(isEnd)
            , m_bucketIterator(bucketIterator)
        {
            if (!isEnd && !m_table.isEmpty() && !(m_bucketIterator != DoublyLinkedList<T>::Iterator::universalEnd())) {
#ifdef HASHTABLE_DEBUG
                kprintf("bucket iterator init!\n");
#endif
                m_bucketIterator = m_table.m_buckets[0].chain.begin();
                if (m_bucketIterator.isEnd())
                    skipToNext();
            }
        }

        HashTable& m_table;
        unsigned m_bucketIndex { 0 };
        bool m_isEnd { false };
        typename DoublyLinkedList<T>::Iterator m_bucketIterator;
    };

    Iterator begin() { return Iterator(*this, isEmpty()); }
    Iterator end() { return Iterator(*this, true); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) const
        {
            if (m_isEnd && other.m_isEnd)
                return false;
            return &m_table != &other.m_table
                || m_isEnd != other.m_isEnd
                || m_bucketIndex != other.m_bucketIndex
                || m_bucketIterator != other.m_bucketIterator;
        }
        bool operator==(const ConstIterator& other) const { return !(*this != other); }
        const T& operator*() const
        {
#ifdef HASHTABLE_DEBUG
            kprintf("retrieve { bucketIndex: %u, isEnd: %u }\n", m_bucketIndex, m_isEnd);
#endif
            return *m_bucketIterator;
        }
        ConstIterator& operator++()
        {
            skipToNext();
            return *this;
        }

        void skipToNext()
        {
#ifdef HASHTABLE_DEBUG
            unsigned pass = 0;
#endif
            while (!m_isEnd) {
#ifdef HASHTABLE_DEBUG
                ++pass;
                kprintf("skipToNext pass %u, m_bucketIndex=%u\n", pass, m_bucketIndex);
#endif
                if (m_bucketIterator.isEnd()) {
                    ++m_bucketIndex;
                    if (m_bucketIndex >= m_table.capacity()) {
                        m_isEnd = true;
                        return;
                    }
                    const DoublyLinkedList<T>& chain = m_table.m_buckets[m_bucketIndex].chain;
                    m_bucketIterator = chain.begin();
                } else {
                    ++m_bucketIterator;
                }
                if (!m_bucketIterator.isEnd())
                    return;
            }
        }
    private:
        friend class HashTable;
        ConstIterator(const HashTable& table, bool isEnd, typename DoublyLinkedList<T>::ConstIterator bucketIterator = DoublyLinkedList<T>::ConstIterator::universalEnd(), unsigned bucketIndex = 0)
            : m_table(table)
            , m_bucketIndex(bucketIndex)
            , m_isEnd(isEnd)
            , m_bucketIterator(bucketIterator)
        {
            if (!isEnd && !m_table.isEmpty() && !(m_bucketIterator != DoublyLinkedList<T>::ConstIterator::universalEnd())) {
#ifdef HASHTABLE_DEBUG
                kprintf("const bucket iterator init!\n");
#endif
                const DoublyLinkedList<T>& chain = m_table.m_buckets[0].chain;
                m_bucketIterator = chain.begin();
                if (m_bucketIterator.isEnd())
                    skipToNext();
            }
        }

        const HashTable& m_table;
        unsigned m_bucketIndex { 0 };
        bool m_isEnd { false };
        typename DoublyLinkedList<T>::ConstIterator m_bucketIterator;
    };

    ConstIterator begin() const { return ConstIterator(*this, isEmpty()); }
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
    Bucket& lookup(const T&, unsigned* bucketIndex = nullptr);
    const Bucket& lookup(const T&, unsigned* bucketIndex = nullptr) const;
    void rehash(unsigned capacity);
    void insert(const T&);
    void insert(T&&);

    Bucket* m_buckets { nullptr };

    unsigned m_size { 0 };
    unsigned m_capacity { 0 };
};

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::set(T&& value)
{
    if (!m_capacity)
        rehash(1);
    auto& bucket = lookup(value);
    for (auto& e : bucket.chain) {
        if (e == value)
            return;
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
        if (e == value)
            return;
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
void HashTable<T, TraitsForT>::rehash(unsigned newCapacity)
{
    newCapacity *= 2;
#ifdef HASHTABLE_DEBUG
    kprintf("rehash to %u buckets\n", newCapacity);
#endif
    auto* newBuckets = new Bucket[newCapacity];
    auto* oldBuckets = m_buckets;
    unsigned oldCapacity = m_capacity;
    m_buckets = newBuckets;
    m_capacity = newCapacity;

#ifdef HASHTABLE_DEBUG
    kprintf("reinsert %u buckets\n", oldCapacity);
#endif
    for (unsigned i = 0; i < oldCapacity; ++i) {
        for (auto& value : oldBuckets[i].chain) {
            insert(move(value));
        }
    }

    delete [] oldBuckets;
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::clear()
{
    delete [] m_buckets;
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
    if (isEmpty())
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
    if (isEmpty())
        return end();
    unsigned bucketIndex;
    auto& bucket = lookup(value, &bucketIndex);
    auto bucketIterator = bucket.chain.find(value);
    if (bucketIterator != bucket.chain.end())
        return Iterator(*this, false, bucketIterator, bucketIndex);
    return end();
}

template<typename T, typename TraitsForT>
auto HashTable<T, TraitsForT>::find(const T& value) const -> ConstIterator
{
    if (isEmpty())
        return end();
    unsigned bucketIndex;
    auto& bucket = lookup(value, &bucketIndex);
    auto bucketIterator = bucket.chain.find(value);
    if (bucketIterator != bucket.chain.end())
        return ConstIterator(*this, false, bucketIterator, bucketIndex);
    return end();
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::remove(Iterator it)
{
    ASSERT(!isEmpty());
    m_buckets[it.m_bucketIndex].chain.remove(it.m_bucketIterator);
    --m_size;
}

template<typename T, typename TraitsForT>
typename HashTable<T, TraitsForT>::Bucket& HashTable<T, TraitsForT>::lookup(const T& value, unsigned* bucketIndex)
{
    unsigned hash = TraitsForT::hash(value);
#ifdef HASHTABLE_DEBUG
    kprintf("hash for ");
    TraitsForT::dump(value);
    kprintf(" is %u\n", hash);
#endif
    if (bucketIndex)
        *bucketIndex = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

template<typename T, typename TraitsForT>
const typename HashTable<T, TraitsForT>::Bucket& HashTable<T, TraitsForT>::lookup(const T& value, unsigned* bucketIndex) const
{
    unsigned hash = TraitsForT::hash(value);
#ifdef HASHTABLE_DEBUG
    kprintf("hash for ");
    TraitsForT::dump(value);
    kprintf(" is %u\n", hash);
#endif
    if (bucketIndex)
        *bucketIndex = hash % m_capacity;
    return m_buckets[hash % m_capacity];
}

template<typename T, typename TraitsForT>
void HashTable<T, TraitsForT>::dump() const
{
    kprintf("HashTable{%p} m_size=%u, m_capacity=%u, m_buckets=%p\n", this, m_size, m_capacity, m_buckets);
    for (unsigned i = 0; i < m_capacity; ++i) {
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

