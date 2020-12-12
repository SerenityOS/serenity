/*
 * Copyright (c) 2020, The SerenityOS developers.
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
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibBencode/Value.h>

namespace Bencode {

class Dictionary {
public:
    Dictionary() {}
    ~Dictionary() {}

    Dictionary(const Dictionary& other)
        : m_members(other.m_members)
    {
    }

    Dictionary(Dictionary&& other)
        : m_members(move(other.m_members))
    {
    }

    Dictionary& operator=(const Dictionary& other)
    {
        if (this != &other) {
            m_members = other.m_members;
        }
        return *this;
    }

    Dictionary& operator=(Dictionary&& other)
    {
        if (this != &other) {
            m_members = move(other.m_members);
        }
        return *this;
    }

    int size() const { return m_members.size(); }
    bool is_empty() const { return m_members.is_empty(); }

    const Value get(const String& key) const
    {
        auto* value = get_ptr(key);
        return value ? *value : Value(Value::Type::Invalid);
    }

    const Value& get_or(const String& key, const Value& alternative) const
    {
        auto* value = get_ptr(key);
        return value ? *value : alternative;
    }

    const Value* get_ptr(const String& key) const
    {
        auto it = m_members.find(key);
        if (it == m_members.end())
            return nullptr;
        return &(*it).value;
    }

    bool has(const String& key) const
    {
        return m_members.contains(key);
    }

    void set(const String& key, Value value)
    {
        m_members.set(key, move(value));
    }

    template<typename Callback>
    void for_each_member(Callback callback) const
    {
        for (auto& pair : m_members)
            callback(pair.key, pair.value);
    }

    void serialize(StringBuilder&) const;
    String to_string() const;

private:
    HashMap<String, Value> m_members;
};

};
