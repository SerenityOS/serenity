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
#include <LibPDF/Value.h>

namespace PDF {

class Object : public RefCounted<Object> {
public:
    virtual ~Object() = default;

    [[nodiscard]] ALWAYS_INLINE int generation_index() const { return m_generation_index; }
    ALWAYS_INLINE void set_generation_index(int generation_index) { m_generation_index = generation_index; }

    virtual bool is_string() const { return false; }
    virtual bool is_name() const { return false; }
    virtual bool is_array() const { return false; }
    virtual bool is_dict() const { return false; }
    virtual bool is_stream() const { return false; }
    virtual bool is_indirect_object() const { return false; }
    virtual bool is_indirect_object_ref() const { return false; }

    virtual String to_string(int indent = 0) const = 0;

private:
    int m_generation_index { 0 };
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

    bool is_string() const override { return true; }
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

    bool is_name() const override { return true; }
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

    bool is_array() const override { return true; }
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

    bool is_dict() const override { return true; }
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

    bool is_stream() const override { return true; }
    String to_string(int indent) const override;

private:
    NonnullRefPtr<DictObject> m_dict;
    ReadonlyBytes m_bytes;
};

class IndirectObject final : public Object {
public:
    IndirectObject(int index, int generation_index, const NonnullRefPtr<Object>& object)
        : m_index(index)
        , m_object(object)
    {
        set_generation_index(generation_index);
    }

    ~IndirectObject() override = default;

    [[nodiscard]] ALWAYS_INLINE int index() const { return m_index; }
    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<Object> object() const { return m_object; }

    bool is_indirect_object() const override { return true; }
    String to_string(int indent) const override;

private:
    int m_index;
    NonnullRefPtr<Object> m_object;
};

class IndirectObjectRef final : public Object {
public:
    IndirectObjectRef(int index, int generation_index)
        : m_index(index)
    {
        set_generation_index(generation_index);
    }

    ~IndirectObjectRef() override = default;

    [[nodiscard]] ALWAYS_INLINE int index() const { return m_index; }

    bool is_indirect_object_ref() const override { return true; }
    String to_string(int indent) const override;

private:
    int m_index;
};

template<typename T>
concept IsObject = IsBaseOf<Object, T>;

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
