/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/SourceLocation.h>
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

    virtual const char* type_name() const = 0;
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

    [[nodiscard]] ALWAYS_INLINE const FlyString& name() const { return m_name; }

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

    ALWAYS_INLINE const Value& operator[](size_t index) const { return at(index); }
    ALWAYS_INLINE const Value& at(size_t index) const { return m_elements[index]; }

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

    [[nodiscard]] ALWAYS_INLINE const HashMap<FlyString, Value>& map() const { return m_map; }

    ALWAYS_INLINE bool contains(const FlyString& key) const { return m_map.contains(key); }

    ALWAYS_INLINE Optional<Value> get(const FlyString& key) const { return m_map.get(key); }

    Value get_value(const FlyString& key) const { return get(key).value(); }

    NonnullRefPtr<Object> get_object(Document*, const FlyString& key) const;

#define DEFINE_GETTER(class_name, snake_name) \
    NonnullRefPtr<class_name> get_##snake_name(Document*, const FlyString& key) const;
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
    explicit StreamObject(const NonnullRefPtr<DictObject>& dict)
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
    PlainTextStreamObject(const NonnullRefPtr<DictObject>& dict, const ReadonlyBytes& bytes)
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
    EncodedStreamObject(const NonnullRefPtr<DictObject>& dict, ByteBuffer&& buffer)
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
    ALWAYS_INLINE const char* type_name() const override { return "indirect_object"; }
    String to_string(int indent) const override;

private:
    u32 m_index;
    Value m_value;
};

template<IsObject To, IsObject From>
[[nodiscard]] ALWAYS_INLINE static NonnullRefPtr<To> object_cast(NonnullRefPtr<From> obj
#ifdef PDF_DEBUG
    ,
    SourceLocation loc = SourceLocation::current()
#endif
)
{
#ifdef PDF_DEBUG
#    define ENUMERATE_TYPES(class_name, snake_name)                                                \
        if constexpr (IsSame<To, class_name>) {                                                    \
            if (!obj->is_##snake_name()) {                                                         \
                dbgln("{} invalid cast from type {} to type " #snake_name, loc, obj->type_name()); \
            }                                                                                      \
        }
    ENUMERATE_OBJECT_TYPES(ENUMERATE_TYPES)
#    undef ENUMERATE_TYPES
#endif

    return static_cast<NonnullRefPtr<To>>(obj);
}

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
