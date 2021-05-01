/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Value.h>

namespace PDF {

class Object : public RefCounted<Object> {
public:
    virtual ~Object() = default;

    [[nodiscard]] ALWAYS_INLINE u32 generation_index() const { return m_generation_index; }
    ALWAYS_INLINE void set_generation_index(u32 generation_index) { m_generation_index = generation_index; }

#define DEFINE_ID(_, name) \
    virtual bool is_##name() const { return false; }
    ENUMERATE_OBJECT_TYPES(DEFINE_ID)
#undef DEFINE_ID

    virtual String to_string(int indent) const = 0;

private:
    u32 m_generation_index { 0 };
};

class StringObject final : public Object {
public:
    StringObject(String string, bool is_binary)
        : m_string(move(string))
        , m_is_binary(is_binary)
    {
    }

    ~StringObject() override = default;

    [[nodiscard]] ALWAYS_INLINE const String& string() const { return m_string; }
    [[nodiscard]] ALWAYS_INLINE bool is_binary() const { return m_is_binary; }

    ALWAYS_INLINE bool is_string() const override { return true; }
    String to_string(int indent) const override;

private:
    String m_string;
    bool m_is_binary;
};

class NameObject final : public Object {
public:
    explicit NameObject(FlyString name)
        : m_name(move(name))
    {
    }

    ~NameObject() override = default;

    [[nodiscard]] ALWAYS_INLINE FlyString name() const { return m_name; }

    ALWAYS_INLINE bool is_name() const override { return true; }
    String to_string(int indent) const override;

private:
    FlyString m_name;
};

class ArrayObject final : public Object {
public:
    explicit ArrayObject(Vector<Value> elements)
        : m_elements(move(elements))
    {
    }

    ~ArrayObject() override = default;

    [[nodiscard]] ALWAYS_INLINE Vector<Value> elements() const { return m_elements; }

    ALWAYS_INLINE bool is_array() const override { return true; }
    String to_string(int indent) const override;

private:
    Vector<Value> m_elements;
};

class DictObject final : public Object {
public:
    explicit DictObject(HashMap<FlyString, Value> map)
        : m_map(move(map))
    {
    }

    ~DictObject() override = default;

    [[nodiscard]] ALWAYS_INLINE HashMap<FlyString, Value> map() const { return m_map; }

    ALWAYS_INLINE bool is_dict() const override { return true; }
    String to_string(int indent) const override;

private:
    HashMap<FlyString, Value> m_map;
};

class StreamObject final : public Object {
public:
    StreamObject(const NonnullRefPtr<DictObject>& dict, const ReadonlyBytes& bytes)
        : m_dict(dict)
        , m_bytes(bytes)
    {
    }

    ~StreamObject() override = default;

    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<DictObject> dict() const { return m_dict; }
    [[nodiscard]] ALWAYS_INLINE const ReadonlyBytes& bytes() const { return m_bytes; }

    ALWAYS_INLINE bool is_stream() const override { return true; }
    String to_string(int indent) const override;

private:
    NonnullRefPtr<DictObject> m_dict;
    ReadonlyBytes m_bytes;
};

class IndirectValue final : public Object {
public:
    IndirectValue(u32 index, u32 generation_index, const Value& value)
        : m_index(index)
        , m_value(value)
    {
        set_generation_index(generation_index);
    }

    ~IndirectValue() override = default;

    [[nodiscard]] ALWAYS_INLINE u32 index() const { return m_index; }
    [[nodiscard]] ALWAYS_INLINE const Value& value() const { return m_value; }

    ALWAYS_INLINE bool is_indirect_value() const override { return true; }
    String to_string(int indent) const override;

private:
    u32 m_index;
    Value m_value;
};

class IndirectValueRef final : public Object {
public:
    IndirectValueRef(u32 index, u32 generation_index)
        : m_index(index)
    {
        set_generation_index(generation_index);
    }

    ~IndirectValueRef() override = default;

    [[nodiscard]] ALWAYS_INLINE u32 index() const { return m_index; }

    ALWAYS_INLINE bool is_indirect_value_ref() const override { return true; }
    String to_string(int indent) const override;

private:
    u32 m_index;
};

}

namespace AK {

template<PDF::IsObject T>
struct Formatter<T> : Formatter<StringView> {
    void format(FormatBuilder& builder, const T& object)
    {
        Formatter<StringView>::format(builder, object.to_string(0));
    }
};

template<PDF::IsObject T>
struct Formatter<NonnullRefPtr<T>> : Formatter<T> {
    void format(FormatBuilder& builder, const NonnullRefPtr<T>& object)
    {
        Formatter<T>::format(builder, *object);
    }
};

}
