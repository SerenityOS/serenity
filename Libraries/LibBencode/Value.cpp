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

#include <AK/StringBuilder.h>
#include <LibBencode/Dictionary.h>
#include <LibBencode/List.h>
#include <LibBencode/Value.h>

namespace Bencode {

Value::Value(Type type)
    : m_type(type)
{
}

Value::Value(const Value& other)
{
    copy_from(other);
}

Value& Value::operator=(const Value& other)
{
    if (this != &other) {
        clear();
        copy_from(other);
    }
    return *this;
}

Value::Value(Value&& other)
{
    m_type = exchange(other.m_type, Type::Invalid);
    m_value.as_i64 = exchange(other.m_value.as_i64, 0);
}

Value& Value::operator=(Value&& other)
{
    if (this != &other) {
        clear();
        m_type = exchange(other.m_type, Type::Invalid);
        m_value.as_i64 = exchange(other.m_value.as_i64, 0);
    }
    return *this;
}

Value::Value(int value)
    : m_type(Type::Integer)
{
    m_value.as_i64 = value;
}

Value::Value(i64 value)
    : m_type(Type::Integer)
{
    m_value.as_i64 = value;
}

Value::Value(const char* cstring)
    : Value(String(cstring))
{
}

Value::Value(const String& value)
    : m_type(Type::String)
{
    m_value.as_string = const_cast<StringImpl*>(value.impl());
    m_value.as_string->ref();
}

Value::Value(const Dictionary& value)
    : m_type(Type::Dictionary)
{
    m_value.as_dictionary = new Dictionary(value);
}

Value::Value(const List& value)
    : m_type(Type::List)
{
    m_value.as_list = new List(value);
}

Value::Value(Dictionary&& value)
    : m_type(Type::Dictionary)
{
    m_value.as_dictionary = new Dictionary(move(value));
}

Value::Value(List&& value)
    : m_type(Type::List)
{
    m_value.as_list = new List(move(value));
}

void Value::serialize(StringBuilder& builder) const
{
    switch (m_type) {
    case Type::Dictionary:
        m_value.as_dictionary->serialize(builder);
        break;
    case Type::List:
        m_value.as_list->serialize(builder);
        break;
    case Type::String:
        builder.appendf("%d:", m_value.as_string->length());
        builder.append(String(m_value.as_string));
        break;
    case Type::Integer:
        builder.appendf("i%llde", m_value.as_i64);
        break;
    case Type::Invalid:
        ASSERT_NOT_REACHED();
        break;
    }
}

String Value::to_string() const
{
    StringBuilder builder;
    serialize(builder);
    return builder.build();
}

void Value::clear()
{
    switch (m_type) {
    case Type::Dictionary:
        delete m_value.as_dictionary;
        break;
    case Type::List:
        delete m_value.as_list;
        break;
    case Type::String:
        m_value.as_string->unref();
        break;
    default:
        break;
    }
    m_type = Type::Invalid;
    m_value.as_i64 = 0;
}

void Value::copy_from(const Value& other)
{
    m_type = other.m_type;
    switch (m_type) {
    case Type::Dictionary:
        m_value.as_dictionary = new Dictionary(*other.m_value.as_dictionary);
        break;
    case Type::List:
        m_value.as_list = new List(*other.m_value.as_list);
        break;
    case Type::String:
        ASSERT(!m_value.as_string);
        m_value.as_string = other.m_value.as_string;
        m_value.as_string->ref();
        break;
    case Type::Integer:
        m_value.as_i64 = other.m_value.as_i64;
        break;
    case Type::Invalid:
        ASSERT_NOT_REACHED();
        break;
    }
}

};
