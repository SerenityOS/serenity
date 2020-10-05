/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <AK/FlyString.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class StringOrSymbol {
public:
    static StringOrSymbol from_value(GlobalObject& global_object, Value value)
    {
        if (value.is_symbol())
            return &value.as_symbol();
        if (!value.is_empty())
            return value.to_string(global_object);
        return {};
    }

    StringOrSymbol() = default;

    StringOrSymbol(const char* chars)
        : m_ptr(StringImpl::create(chars).leak_ref())
    {
    }

    StringOrSymbol(const String& string)
        : m_ptr(string.impl())
    {
        ASSERT(!string.is_null());
        as_string_impl().ref();
    }

    StringOrSymbol(const FlyString& string)
        : m_ptr(string.impl())
    {
        ASSERT(!string.is_null());
        as_string_impl().ref();
    }

    ~StringOrSymbol()
    {
        if (is_string())
            as_string_impl().unref();
    }

    StringOrSymbol(const Symbol* symbol)
        : m_ptr(symbol)
    {
        set_symbol_flag();
    }

    StringOrSymbol(const StringOrSymbol& other)
    {
        m_ptr = other.m_ptr;
        if (is_string())
            as_string_impl().ref();
    }

    ALWAYS_INLINE bool is_valid() const { return m_ptr != nullptr; }
    ALWAYS_INLINE bool is_symbol() const { return is_valid() && (bits() & 1ul); }
    ALWAYS_INLINE bool is_string() const { return is_valid() && !(bits() & 1ul); }

    ALWAYS_INLINE String as_string() const
    {
        ASSERT(is_string());
        return as_string_impl();
    }

    ALWAYS_INLINE const Symbol* as_symbol() const
    {
        ASSERT(is_symbol());
        return reinterpret_cast<const Symbol*>(bits() & ~1ul);
    }

    String to_display_string() const
    {
        if (is_string())
            return as_string();
        if (is_symbol())
            return as_symbol()->to_string();
        ASSERT_NOT_REACHED();
    }

    Value to_value(VM& vm) const
    {
        if (is_string())
            return js_string(vm, as_string());
        if (is_symbol())
            return const_cast<Symbol*>(as_symbol());
        return {};
    }

    void visit_children(Cell::Visitor& visitor)
    {
        if (is_symbol())
            visitor.visit(const_cast<Symbol*>(as_symbol()));
    }

    ALWAYS_INLINE bool operator==(const StringOrSymbol& other) const
    {
        if (is_string())
            return other.is_string() && as_string_impl() == other.as_string_impl();
        if (is_symbol())
            return other.is_symbol() && as_symbol() == other.as_symbol();
        return true;
    }

    StringOrSymbol& operator=(const StringOrSymbol& other)
    {
        if (this == &other)
            return *this;
        m_ptr = other.m_ptr;
        if (is_string())
            as_string_impl().ref();
        return *this;
    }

    unsigned hash() const
    {
        if (is_string())
            return as_string_impl().hash();
        return ptr_hash(as_symbol());
    }

private:
    ALWAYS_INLINE u64 bits() const
    {
        return reinterpret_cast<uintptr_t>(m_ptr);
    }

    ALWAYS_INLINE void set_symbol_flag()
    {
        m_ptr = reinterpret_cast<const void*>(bits() | 1ul);
    }

    ALWAYS_INLINE const StringImpl& as_string_impl() const
    {
        ASSERT(is_string());
        return *reinterpret_cast<const StringImpl*>(m_ptr);
    }

    const void* m_ptr { nullptr };
};

}

template<>
struct AK::Traits<JS::StringOrSymbol> : public GenericTraits<JS::StringOrSymbol> {
    static unsigned hash(const JS::StringOrSymbol& key)
    {
        return key.hash();
    }
};
