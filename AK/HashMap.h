#pragma once

#include "HashTable.h"
#include "StdLibExtras.h"
#include "kstdio.h"

namespace AK {

template<typename K, typename V>
class HashMap {
private:
    struct Entry {
        K key;
        V value;

        bool operator==(const Entry& other)
        {
            return key == other.key;
        }
    };

    struct EntryTraits {
        static unsigned hash(const Entry& entry) { return Traits<K>::hash(entry.key); }
        static void dump(const Entry& entry)
        {
            kprintf("key=");
            Traits<K>::dump(entry.key);
            kprintf(" value=");
            Traits<V>::dump(entry.value);
        }
    };

public:
    HashMap() { }

    HashMap(HashMap&& other)
        : m_table(move(other.m_table))
    {
    }

    HashMap& operator=(HashMap&& other)
    {
        if (this != &other) {
            m_table = move(other.m_table);
        }
        return *this;
    }

    bool isEmpty() const { return m_table.isEmpty(); }
    unsigned size() const { return m_table.size(); }
    unsigned capacity() const { return m_table.capacity(); }
    void clear() { m_table.clear(); }

    void set(const K&, const V&);
    void set(const K&, V&&);
    void remove(const K&);
    void removeOneRandomly() { m_table.remove(m_table.begin()); }

    typedef HashTable<Entry, EntryTraits> HashTableType;
    typedef typename HashTableType::Iterator IteratorType;
    typedef typename HashTableType::ConstIterator ConstIteratorType;

    IteratorType begin() { return m_table.begin(); }
    IteratorType end() { return m_table.end(); }
    IteratorType find(const K&);

    ConstIteratorType begin() const { return m_table.begin(); }
    ConstIteratorType end() const { return m_table.end(); }
    ConstIteratorType find(const K&) const;

    void dump() const { m_table.dump(); }

private:
    HashTable<Entry, EntryTraits> m_table;
};

template<typename K, typename V>
void HashMap<K, V>::set(const K& key, V&& value)
{
    m_table.set(Entry{key, move(value)});
}

template<typename K, typename V>
void HashMap<K, V>::set(const K& key, const V& value)
{
    m_table.set(Entry{key, value});
}

template<typename K, typename V>
void HashMap<K, V>::remove(const K& key)
{
    Entry dummy { key, V() };
    m_table.remove(dummy);
}

template<typename K, typename V>
auto HashMap<K, V>::find(const K& key) -> IteratorType
{
    Entry dummy { key, V() };
    return m_table.find(dummy);
}

template<typename K, typename V>
auto HashMap<K, V>::find(const K& key) const -> ConstIteratorType
{
    Entry dummy { key, V() };
    return m_table.find(dummy);
}

}

using AK::HashMap;

