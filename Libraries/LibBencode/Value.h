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
#include <AK/NumericLimits.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

namespace Bencode {

class Dictionary;
class List;

class Value {
public:
    enum class Type {
        Invalid,
        Dictionary,
        List,
        String,
        Integer,
    };

    explicit Value(Type type);
    ~Value() { clear(); }

    Value(const Value&);
    Value(Value&&);

    Value& operator=(const Value&);
    Value& operator=(Value&&);

    Value(int);
    Value(i64);
    Value(const char*);
    Value(const String&);
    Value(const List&);
    Value(const Dictionary&);

    Value(List&&);
    Value(Dictionary&&);

    const Dictionary& as_dictionary() const
    {
        ASSERT(is_dictionary());
        return *m_value.as_dictionary;
    }

    const List& as_list() const
    {
        ASSERT(is_list());
        return *m_value.as_list;
    }

    String as_string() const
    {
        ASSERT(is_string());
        return *m_value.as_string;
    }

    int as_integer() const
    {
        ASSERT(is_integer());
        ASSERT(m_value.as_i64 >= NumericLimits<int>::min() && m_value.as_i64 <= NumericLimits<int>::max());
        return m_value.as_i64;
    }

    i64 as_i64() const
    {
        ASSERT(is_integer());
        return m_value.as_i64;
    }

    Type type() const
    {
        return m_type;
    }

    String type_name() const
    {
        switch (m_type) {
        case Type::Invalid:
            return "Invalid";
        case Type::Dictionary:
            return "Dictionary";
        case Type::List:
            return "List";
        case Type::String:
            return "String";
        case Type::Integer:
            return "Integer";
        }

        return "";
    }

    bool is_dictionary() const { return m_type == Type::Dictionary; }
    bool is_list() const { return m_type == Type::List; }
    bool is_string() const { return m_type == Type::String; }
    bool is_integer() const { return m_type == Type::Integer; }

    void serialize(StringBuilder&) const;
    String to_string() const;

private:
    void clear();
    void copy_from(const Value&);

    Type m_type { Type::Invalid };
    union {
        Dictionary* as_dictionary;
        List* as_list;
        StringImpl* as_string { nullptr };
        i64 as_i64;
    } m_value;
};

};
