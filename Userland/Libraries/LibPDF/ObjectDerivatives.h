/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/SourceLocation.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

class StringObject final : public Object {
public:
    StringObject(String string, bool is_binary)
        : m_string(move(string))
        , m_is_binary(is_binary)
    {
    }

    ~StringObject() override = default;

    [[nodiscard]] ALWAYS_INLINE String const& string() const { return m_string; }
    [[nodiscard]] ALWAYS_INLINE bool is_binary() const { return m_is_binary; }

    ALWAYS_INLINE bool is_string() const override { return true; }
    ALWAYS_INLINE const char* type_name() const override { return "string"; }
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

    [[nodiscard]] ALWAYS_INLINE FlyString const& name() const { return m_name; }

    ALWAYS_INLINE bool is_name() const override { return true; }
    ALWAYS_INLINE const char* type_name() const override { return "name"; }
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

    [[nodiscard]] ALWAYS_INLINE size_t size() const { return m_elements.size(); }
    [[nodiscard]] ALWAYS_INLINE Vector<Value> elements() const { return m_elements; }

    ALWAYS_INLINE auto begin() const { return m_elements.begin(); }
    ALWAYS_INLINE auto end() const { return m_elements.end(); }

    ALWAYS_INLINE Value const& operator[](size_t index) const { return at(index); }
    ALWAYS_INLINE Value const& at(size_t index) const { return m_elements[index]; }

    NonnullRefPtr<Object> get_object_at(Document*, size_t index) const;

#define DEFINE_INDEXER(class_name, snake_name) \
    NonnullRefPtr<class_name> get_##snake_name##_at(Document*, size_t index) const;
    ENUMERATE_OBJECT_TYPES(DEFINE_INDEXER)
#undef DEFINE_INDEXER

    ALWAYS_INLINE bool is_array() const override
    {
        return true;
    }
    ALWAYS_INLINE const char* type_name() const override { return "array"; }
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

    [[nodiscard]] ALWAYS_INLINE HashMap<FlyString, Value> const& map() const { return m_map; }

    template<typename... Args>
    bool contains(Args&&... keys) const { return (m_map.contains(keys) && ...); }

    ALWAYS_INLINE Optional<Value> get(FlyString const& key) const { return m_map.get(key); }

    Value get_value(FlyString const& key) const
    {
        auto value = get(key);
        VERIFY(value.has_value());
        return value.value();
    }

    NonnullRefPtr<Object> get_object(Document*, FlyString const& key) const;

#define DEFINE_GETTER(class_name, snake_name) \
    NonnullRefPtr<class_name> get_##snake_name(Document*, FlyString const& key) const;
    ENUMERATE_OBJECT_TYPES(DEFINE_GETTER)
#undef DEFINE_GETTER

    ALWAYS_INLINE bool is_dict() const override
    {
        return true;
    }
    ALWAYS_INLINE const char* type_name() const override { return "dict"; }
    String to_string(int indent) const override;

private:
    HashMap<FlyString, Value> m_map;
};

class StreamObject : public Object {
public:
    explicit StreamObject(NonnullRefPtr<DictObject> const& dict)
        : m_dict(dict)
    {
    }

    virtual ~StreamObject() override = default;

    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<DictObject> dict() const { return m_dict; }
    [[nodiscard]] virtual ReadonlyBytes bytes() const = 0;

    ALWAYS_INLINE bool is_stream() const override { return true; }
    ALWAYS_INLINE const char* type_name() const override { return "stream"; }
    String to_string(int indent) const override;

private:
    NonnullRefPtr<DictObject> m_dict;
};

class PlainTextStreamObject final : public StreamObject {
public:
    PlainTextStreamObject(NonnullRefPtr<DictObject> const& dict, ReadonlyBytes bytes)
        : StreamObject(dict)
        , m_bytes(bytes)
    {
    }

    virtual ~PlainTextStreamObject() override = default;

    [[nodiscard]] ALWAYS_INLINE virtual ReadonlyBytes bytes() const override { return m_bytes; }

private:
    ReadonlyBytes m_bytes;
};

class EncodedStreamObject final : public StreamObject {
public:
    EncodedStreamObject(NonnullRefPtr<DictObject> const& dict, ByteBuffer&& buffer)
        : StreamObject(dict)
        , m_buffer(buffer)
    {
    }

    virtual ~EncodedStreamObject() override = default;

    [[nodiscard]] ALWAYS_INLINE virtual ReadonlyBytes bytes() const override { return m_buffer.bytes(); }

private:
    ByteBuffer m_buffer;
};

class IndirectValue final : public Object {
public:
    IndirectValue(u32 index, u32 generation_index, Value const& value)
        : m_index(index)
        , m_value(value)
    {
        set_generation_index(generation_index);
    }

    ~IndirectValue() override = default;

    [[nodiscard]] ALWAYS_INLINE u32 index() const { return m_index; }
    [[nodiscard]] ALWAYS_INLINE Value const& value() const { return m_value; }

    ALWAYS_INLINE bool is_indirect_value() const override { return true; }
    ALWAYS_INLINE const char* type_name() const override { return "indirect_object"; }
    String to_string(int indent) const override;

private:
    u32 m_index;
    Value m_value;
};

}
