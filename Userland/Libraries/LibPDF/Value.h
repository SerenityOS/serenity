/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Object.h>

namespace PDF {

class Value {
public:
    // We store refs as u32, with 18 bits for the index and 14 bits for the
    // generation index. The generation index is stored in the higher bits.
    // This may need to be rethought later, as the max generation index is
    // 2^16 and the max for the object index is probably 2^32 (I don't know
    // exactly)
    static constexpr auto max_ref_index = (1 << 19) - 1;            // 2 ^ 19 - 1
    static constexpr auto max_ref_generation_index = (1 << 15) - 1; // 2 ^ 15 - 1

    Value()
        : m_type(Type::Empty)
    {
    }

    struct NullTag {
    };

    Value(NullTag)
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
    Value(RefPtr<T> obj)
        : m_type(obj ? Type::Object : Type::Empty)
    {
        if (obj) {
            m_as_object = obj;
        }
    }

    template<IsObject T>
    Value(NonnullRefPtr<T> obj)
        : m_type(Type::Object)
    {
        m_as_object = obj;
    }

    Value(Value const& other)
    {
        *this = other;
    }

    ~Value() = default;

    Value& operator=(Value const& other);

    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return m_type == Type::Empty; }
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

    template<typename T>
    [[nodiscard]] ALWAYS_INLINE bool is_int_type() const
    {
        if (!is_int())
            return false;
        auto as_int = static_cast<T>(m_as_int);
        return as_int >= NumericLimits<T>::min() && as_int <= NumericLimits<T>::max();
    }

    template<typename T>
    [[nodiscard]] ALWAYS_INLINE T as_int_type() const
    {
        VERIFY(is_int_type<T>());
        return static_cast<T>(m_as_int);
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

    [[nodiscard]] ALWAYS_INLINE explicit operator bool() const { return !is_empty(); }

    [[nodiscard]] String to_string(int indent = 0) const;

private:
    enum class Type {
        Empty,
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
    };

    RefPtr<Object> m_as_object;
    Type m_type;
};

}

namespace AK {

template<>
struct Formatter<PDF::Value> : Formatter<StringView> {
    void format(FormatBuilder& builder, PDF::Value const& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

}
