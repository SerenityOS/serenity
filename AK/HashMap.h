#pragma once

#include <AK/HashTable.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <AK/kstdio.h>

namespace AK {

template<typename K, typename V>
class HashMap {
private:
    struct Entry {
        K key;
        V value;

        bool operator==(const Entry& other) const
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
    HashMap() {}

    bool is_empty() const { return m_table.is_empty(); }
    int size() const { return m_table.size(); }
    int capacity() const { return m_table.capacity(); }
    void clear() { m_table.clear(); }

    void set(const K& key, const V& value) { m_table.set({ key, value }); }
    void set(const K& key, V&& value) { m_table.set({ key, move(value) }); }
    void remove(const K& key) { m_table.remove({ key, V() }); }
    void remove_one_randomly() { m_table.remove(m_table.begin()); }

    typedef HashTable<Entry, EntryTraits> HashTableType;
    typedef typename HashTableType::Iterator IteratorType;
    typedef typename HashTableType::ConstIterator ConstIteratorType;

    IteratorType begin() { return m_table.begin(); }
    IteratorType end() { return m_table.end(); }
    IteratorType find(const K& key) { return m_table.find({ key, V() }); }

    ConstIteratorType begin() const { return m_table.begin(); }
    ConstIteratorType end() const { return m_table.end(); }
    ConstIteratorType find(const K& key) const { return m_table.find({ key, V() }); }

    void ensure_capacity(int capacity) { m_table.ensure_capacity(capacity); }

    void dump() const { m_table.dump(); }

    V get(const K& key) const
    {
        auto it = find(key);
        if (it == end())
            return V();
        return (*it).value;
    }

    bool contains(const K& key) const
    {
        return find(key) != end();
    }

    void remove(IteratorType it)
    {
        m_table.remove(it);
    }

    V& ensure(const K& key)
    {
        auto it = find(key);
        if (it == end())
            set(key, V());
        return find(key)->value;
    }

    Vector<K> keys() const
    {
        Vector<K> list;
        list.ensure_capacity(size());
        for (auto& it : *this)
            list.unchecked_append(it.key);
        return list;
    }

private:
    HashTableType m_table;
};

}

using AK::HashMap;
