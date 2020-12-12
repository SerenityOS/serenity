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

class List {
public:
    List() {}
    ~List() {}

    List(const List& other)
        : m_values(other.m_values)
    {
    }

    List(List&& other)
        : m_values(move(other.m_values))
    {
    }

    List& operator=(const List& other)
    {
        if (this != &other)
            m_values = other.m_values;
        return *this;
    }

    List& operator=(List&& other)
    {
        if (this != &other)
            m_values = move(other.m_values);
        return *this;
    }

    int size() const { return m_values.size(); }
    bool is_empty() const { return m_values.is_empty(); }

    const Value& at(int index) const { return m_values.at(index); }
    const Value& operator[](int index) const { return at(index); }

    void clear() { m_values.clear(); }
    void append(Value value) { m_values.append(move(value)); }

    const Vector<Value>& values() const { return m_values; }

    void ensure_capacity(int capacity) { m_values.ensure_capacity(capacity); }

    void serialize(StringBuilder&) const;
    String to_string() const;

private:
    Vector<Value> m_values;
};

};
