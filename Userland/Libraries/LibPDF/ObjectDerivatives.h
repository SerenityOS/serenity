/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/SourceLocation.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Object.h>
#include <LibPDF/Value.h>

namespace PDF {

class StringObject final : public Object {
public:
    StringObject(ByteString string, bool is_binary)
        : m_string(move(string))
        , m_is_binary(is_binary)
    {
    }

    ~StringObject() override = default;

    [[nodiscard]] ALWAYS_INLINE ByteString const& string() const { return m_string; }
    [[nodiscard]] ALWAYS_INLINE bool is_binary() const { return m_is_binary; }
    void set_string(ByteString string) { m_string = move(string); }

    char const* type_name() const override { return "string"; }
    ByteString to_byte_string(int indent) const override;

protected:
    bool is_string() const override { return true; }

private:
    ByteString m_string;
    bool m_is_binary;
};

class NameObject final : public Object {
public:
    explicit NameObject(DeprecatedFlyString name)
        : m_name(move(name))
    {
    }

    ~NameObject() override = default;

    [[nodiscard]] ALWAYS_INLINE DeprecatedFlyString const& name() const { return m_name; }

    char const* type_name() const override { return "name"; }
    ByteString to_byte_string(int indent) const override;

protected:
    bool is_name() const override { return true; }

private:
    DeprecatedFlyString m_name;
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
    [[nodiscard]] Vector<float> float_elements() const;

    ALWAYS_INLINE auto begin() const { return m_elements.begin(); }
    ALWAYS_INLINE auto end() const { return m_elements.end(); }

    ALWAYS_INLINE Value const& operator[](size_t index) const { return at(index); }
    ALWAYS_INLINE Value const& at(size_t index) const { return m_elements[index]; }

    PDFErrorOr<NonnullRefPtr<Object>> get_object_at(Document* document, size_t index) const;
    NonnullRefPtr<Object> get_object_at(size_t index) const { return at(index).get<NonnullRefPtr<Object>>(); }

#define DEFINE_INDEXER(class_name, snake_name)                                                  \
    PDFErrorOr<NonnullRefPtr<class_name>> get_##snake_name##_at(Document*, size_t index) const; \
    NonnullRefPtr<class_name> get_##snake_name##_at(size_t index) const;
    ENUMERATE_OBJECT_TYPES(DEFINE_INDEXER)
#undef DEFINE_INDEXER

    char const* type_name() const override
    {
        return "array";
    }
    ByteString to_byte_string(int indent) const override;

protected:
    bool is_array() const override { return true; }

private:
    Vector<Value> m_elements;
};

class DictObject final : public Object {
public:
    explicit DictObject(HashMap<DeprecatedFlyString, Value> map)
        : m_map(move(map))
    {
    }

    ~DictObject() override = default;

    [[nodiscard]] ALWAYS_INLINE HashMap<DeprecatedFlyString, Value> const& map() const { return m_map; }

    template<typename... Args>
    bool contains(Args&&... keys) const { return (m_map.contains(keys) && ...); }

    template<typename... Args>
    bool contains_any_of(Args&&... keys) const { return (m_map.contains(keys) || ...); }

    ALWAYS_INLINE Optional<Value> get(DeprecatedFlyString const& key) const { return m_map.get(key).copy(); }

    Value get_value(DeprecatedFlyString const& key) const
    {
        auto value = get(key);
        VERIFY(value.has_value());
        return value.value();
    }

    PDFErrorOr<NonnullRefPtr<Object>> get_object(Document*, DeprecatedFlyString const& key) const;

#define DEFINE_GETTER(class_name, snake_name)                                                                \
    PDFErrorOr<NonnullRefPtr<class_name>> get_##snake_name(Document*, DeprecatedFlyString const& key) const; \
    NonnullRefPtr<class_name> get_##snake_name(DeprecatedFlyString const& key) const;
    ENUMERATE_OBJECT_TYPES(DEFINE_GETTER)
#undef DEFINE_GETTER

    char const* type_name() const override
    {
        return "dict";
    }
    ByteString to_byte_string(int indent) const override;

protected:
    bool is_dict() const override { return true; }

private:
    HashMap<DeprecatedFlyString, Value> m_map;
};

class StreamObject : public Object {
public:
    explicit StreamObject(NonnullRefPtr<DictObject> const& dict, ByteBuffer const& bytes)
        : m_dict(dict)
        , m_buffer(bytes)
    {
    }

    virtual ~StreamObject() override = default;

    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<DictObject> dict() const { return m_dict; }
    [[nodiscard]] ReadonlyBytes bytes() const { return m_buffer.bytes(); }
    [[nodiscard]] ByteBuffer& buffer() { return m_buffer; }

    char const* type_name() const override { return "stream"; }
    ByteString to_byte_string(int indent) const override;

private:
    bool is_stream() const override { return true; }

    NonnullRefPtr<DictObject> m_dict;
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

    char const* type_name() const override { return "indirect_object"; }
    ByteString to_byte_string(int indent) const override;

protected:
    bool is_indirect_value() const override { return true; }

private:
    u32 m_index;
    Value m_value;
};

template<IsValueType T>
UnwrappedValueType<T> cast_to(Value const& value)
{
    if constexpr (IsSame<T, bool>)
        return value.get<bool>();
    else if constexpr (IsSame<T, int>)
        return value.get<int>();
    else if constexpr (IsSame<T, float>)
        return value.get<float>();
    else if constexpr (IsSame<T, Object>)
        return value.get<NonnullRefPtr<Object>>();
    else if constexpr (IsObject<T>)
        return value.get<NonnullRefPtr<Object>>()->cast<T>();
    VERIFY_NOT_REACHED();
}

}
