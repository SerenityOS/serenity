/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace PDF {

class Object;

class Value {
public:
    // We store refs as u32, with 18 bits for the index and 14 bits for the
    // generation index. The generation index is stored in the higher bits.
    // This may need to be rethought later, as the max generation index is
    // 2^16 and the max for the object index is probably 2^32 (I don't know
    // exactly)
    static constexpr auto max_ref_index = (1 << 19) - 1;            // 2 ^ 18 - 1
    static constexpr auto max_ref_generation_index = (1 << 15) - 1; // 2 ^ 14 - 1

    Value()
        : m_type(Type::Null)
    {
    }

    Value(bool b)
        : m_type(Type::Bool)
    {
        m_as_bool = b;
    }

    Value(int i)
        : m_type(Type::Int)
    {
        m_as_int = i;
    }

    Value(float f)
        : m_type(Type::Float)
    {
        m_as_float = f;
    }

    Value(u32 index, u32 generation_index)
        : m_type(Type::Ref)
    {
        VERIFY(index < max_ref_index);
        VERIFY(generation_index < max_ref_generation_index);
        m_as_ref = (generation_index << 14) | index;
    }

    template<IsObject T>
    Value(NonnullRefPtr<T> obj)
        : m_type(Type::Object)
    {
        obj->ref();
        m_as_object = obj;
    }

    Value(const Value& other)
    {
        *this = other;
    }

    ~Value();

    Value& operator=(const Value& other);

    [[nodiscard]] ALWAYS_INLINE bool is_null() const { return m_type == Type::Null; }
    [[nodiscard]] ALWAYS_INLINE bool is_bool() const { return m_type == Type::Bool; }
    [[nodiscard]] ALWAYS_INLINE bool is_int() const { return m_type == Type::Int; }
    [[nodiscard]] ALWAYS_INLINE bool is_float() const { return m_type == Type::Float; }
    [[nodiscard]] ALWAYS_INLINE bool is_number() const { return is_int() || is_float(); }
    [[nodiscard]] ALWAYS_INLINE bool is_ref() const { return m_type == Type::Ref; }
    [[nodiscard]] ALWAYS_INLINE bool is_object() const { return m_type == Type::Object; }

    [[nodiscard]] ALWAYS_INLINE bool as_bool() const
    {
        VERIFY(is_bool());
        return m_as_bool;
    }

    [[nodiscard]] ALWAYS_INLINE int as_int() const
    {
        VERIFY(is_int());
        return m_as_int;
    }

    [[nodiscard]] ALWAYS_INLINE int to_int() const
    {
        if (is_int())
            return as_int();
        return static_cast<int>(as_float());
    }

    [[nodiscard]] ALWAYS_INLINE float as_float() const
    {
        VERIFY(is_float());
        return m_as_float;
    }

    [[nodiscard]] ALWAYS_INLINE float to_float() const
    {
        if (is_float())
            return as_float();
        return static_cast<float>(as_int());
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_index() const
    {
        VERIFY(is_ref());
        return m_as_ref & 0x3ffff;
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_generation_index() const
    {
        VERIFY(is_ref());
        return m_as_ref >> 18;
    }

    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<Object> as_object() const { return *m_as_object; }

    [[nodiscard]] ALWAYS_INLINE explicit operator bool() const { return !is_null(); }

    [[nodiscard]] String to_string(int indent = 0) const;

private:
    enum class Type {
        Null,
        Bool,
        Int,
        Float,
        Ref,
        Object,
    };

    union {
        bool m_as_bool;
        int m_as_int;
        u32 m_as_ref;
        float m_as_float;
        Object* m_as_object;
    };

    Type m_type;
};

}

namespace AK {

template<>
struct Formatter<PDF::Value> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::Value& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

}
