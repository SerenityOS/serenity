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

    template<typename T>
    Value(RefPtr<T> obj) requires(IsBaseOf<Object, T>)
        : m_type(Type::Object)
    {
        VERIFY(obj);
        obj->ref();
        m_as_object = obj;
    }

    template<typename T>
    Value(NonnullRefPtr<T> obj) requires(IsBaseOf<Object, T>)
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

    [[nodiscard]] ALWAYS_INLINE float as_float() const
    {
        VERIFY(is_float());
        return m_as_float;
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
        Object,
    };

    union {
        bool m_as_bool;
        int m_as_int;
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
