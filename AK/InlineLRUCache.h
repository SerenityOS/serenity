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
