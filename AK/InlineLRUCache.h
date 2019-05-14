#pragma once

#include <AK/HashMap.h>
#include <AK/InlineLinkedList.h>

namespace AK {

template<typename K, typename V>
class InlineLRUCache {
public:
    ~InlineLRUCache()
    {
        while (size())
            remove_last();
    }

    size_t size() const { return m_map.size(); }
    size_t capacity() const { return m_capacity; }

    void set_capacity(size_t capacity)
    {
        if (capacity == m_capacity)
            return;
        m_capacity = capacity;
        while (size() >= capacity)
            remove_last();
    }

    V* get(const K& key)
    {
        auto it = m_map.find(key);
        if (it == m_map.end())
            return nullptr;
        V* entry = (*it).value;
        m_entries.remove(entry);
        m_entries.prepend(entry);
        return entry;
    }

    void put(K&& key, V&& value)
    {
        auto it = m_map.find(key);
        if (it != m_map.end())
            return;
        V* new_entry = new V(move(value));
        m_entries.prepend(new_entry);
        m_map.set(key, new_entry);

        while (size() > capacity())
            remove_last();
    }

private:
    void remove_last()
    {
        V* entry = m_entries.tail();
        ASSERT(entry);
        m_entries.remove(entry);
        m_map.remove(entry->m_key);
        delete entry;
    }

    InlineLinkedList<V> m_entries;
    HashMap<K, V*> m_map;
    size_t m_capacity { 16 };
};

}

using AK::InlineLRUCache;
