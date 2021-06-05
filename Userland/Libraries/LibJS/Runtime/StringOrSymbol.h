/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class StringOrSymbol {
public:
    StringOrSymbol() = default;

    StringOrSymbol(const char* chars)
        : m_ptr(StringImpl::create(chars).leak_ref())
    {
    }

    StringOrSymbol(const String& string)
        : m_ptr(string.impl())
    {
        VERIFY(!string.is_null());
        as_string_impl().ref();
    }

    StringOrSymbol(const FlyString& string)
        : m_ptr(string.impl())
    {
        VERIFY(!string.is_null());
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

    StringOrSymbol(StringOrSymbol&& other)
    {
        m_ptr = exchange(other.m_ptr, nullptr);
    }

    ALWAYS_INLINE bool is_valid() const { return m_ptr != nullptr; }
    ALWAYS_INLINE bool is_symbol() const { return is_valid() && (bits() & 1ul); }
    ALWAYS_INLINE bool is_string() const { return is_valid() && !(bits() & 1ul); }

    ALWAYS_INLINE String as_string() const
    {
        VERIFY(is_string());
        return as_string_impl();
    }

    ALWAYS_INLINE const Symbol* as_symbol() const
    {
        VERIFY(is_symbol());
        return reinterpret_cast<const Symbol*>(bits() & ~1ul);
    }

    String to_display_string() const
    {
        if (is_string())
            return as_string();
        if (is_symbol())
            return as_symbol()->to_string();
        VERIFY_NOT_REACHED();
    }

    Value to_value(VM& vm) const
    {
        if (is_string())
            return js_string(vm, as_string());
        if (is_symbol())
            return const_cast<Symbol*>(as_symbol());
        return {};
    }

    void visit_edges(Cell::Visitor& visitor)
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

    StringOrSymbol& operator=(StringOrSymbol&& other)
    {
        if (this != &other)
            m_ptr = exchange(other.m_ptr, nullptr);
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
        VERIFY(is_string());
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
