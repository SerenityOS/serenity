/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class TypedArrayBase : public Object {
    JS_OBJECT(TypedArrayBase, Object);

public:
    enum class ContentType {
        BigInt,
        Number,
    };

    u32 array_length() const { return m_array_length; }
    u32 byte_length() const { return m_byte_length; }
    u32 byte_offset() const { return m_byte_offset; }
    ContentType content_type() const { return m_content_type; }
    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }

    void set_array_length(u32 length) { m_array_length = length; }
    void set_byte_length(u32 length) { m_byte_length = length; }
    void set_byte_offset(u32 offset) { m_byte_offset = offset; }
    void set_viewed_array_buffer(ArrayBuffer* array_buffer) { m_viewed_array_buffer = array_buffer; }

    virtual size_t element_size() const = 0;
    virtual String element_name() const = 0;

protected:
    explicit TypedArrayBase(Object& prototype)
        : Object(prototype)
    {
    }

    u32 m_array_length { 0 };
    u32 m_byte_length { 0 };
    u32 m_byte_offset { 0 };
    ContentType m_content_type { ContentType::Number };
    ArrayBuffer* m_viewed_array_buffer { nullptr };

private:
    virtual void visit_edges(Visitor&) override;
};

template<typename T>
class TypedArray : public TypedArrayBase {
    JS_OBJECT(TypedArray, TypedArrayBase);

    using UnderlyingBufferDataType = Conditional<IsSame<ClampedU8, T>, u8, T>;

public:
    // 10.4.5.11 IntegerIndexedElementSet ( O, index, value ), https://tc39.es/ecma262/#sec-integerindexedelementset
    // NOTE: In error cases, the function will return as if it succeeded.
    virtual bool put_by_index(u32 property_index, Value value) override
    {
        // FIXME: If O.[[ContentType]] is BigInt, let numValue be ? ToBigInt(value).
        //        Otherwise, let numValue be ? ToNumber(value).
        //        (set_value currently takes value by itself)

        if (!is_valid_integer_index(property_index))
            return true;

        auto offset = byte_offset();

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return as if it succeeded for now.
        Checked<size_t> indexed_position = property_index;
        indexed_position *= sizeof(UnderlyingBufferDataType);
        indexed_position += offset;
        if (indexed_position.has_overflow()) {
            dbgln("TypedArray::put_by_index: indexed_position overflowed, returning as if succeeded.");
            return true;
        }

        viewed_array_buffer()->template set_value<T>(indexed_position.value(), value, true, ArrayBuffer::Order::Unordered);

        return true;
    }

    // 10.4.5.10 IntegerIndexedElementGet ( O, index ), https://tc39.es/ecma262/#sec-integerindexedelementget
    virtual Value get_by_index(u32 property_index, AllowSideEffects = AllowSideEffects::Yes) const override
    {
        if (!is_valid_integer_index(property_index))
            return js_undefined();

        auto offset = byte_offset();

        // FIXME: Not exactly sure what we should do when overflow occurs.
        //        Just return as if it's an invalid index for now.
        Checked<size_t> indexed_position = property_index;
        indexed_position *= sizeof(UnderlyingBufferDataType);
        indexed_position += offset;
        if (indexed_position.has_overflow()) {
            dbgln("TypedArray::get_by_index: indexed_position overflowed, returning as if it's an invalid index.");
            return js_undefined();
        }

        return viewed_array_buffer()->template get_value<T>(indexed_position.value(), true, ArrayBuffer::Order::Unordered);
    }

    // 10.4.5.2 [[HasProperty]] ( P ), https://tc39.es/ecma262/#sec-integer-indexed-exotic-objects-hasproperty-p
    bool has_property(const PropertyName& name) const override
    {
        if (name.is_number()) {
            return is_valid_integer_index(name.as_number());
        }
        return Object::has_property(name);
    }

    Span<const UnderlyingBufferDataType> data() const
    {
        return { reinterpret_cast<const UnderlyingBufferDataType*>(m_viewed_array_buffer->buffer().data() + m_byte_offset), m_array_length };
    }
    Span<UnderlyingBufferDataType> data()
    {
        return { reinterpret_cast<UnderlyingBufferDataType*>(m_viewed_array_buffer->buffer().data() + m_byte_offset), m_array_length };
    }

    virtual size_t element_size() const override { return sizeof(UnderlyingBufferDataType); };

protected:
    TypedArray(u32 array_length, Object& prototype)
        : TypedArrayBase(prototype)
    {
        VERIFY(!Checked<u32>::multiplication_would_overflow(array_length, sizeof(UnderlyingBufferDataType)));
        m_viewed_array_buffer = ArrayBuffer::create(global_object(), array_length * sizeof(UnderlyingBufferDataType));
        if (array_length)
            VERIFY(!data().is_null());
        m_array_length = array_length;
        m_byte_length = m_viewed_array_buffer->byte_length();
    }

private:
    virtual bool is_typed_array() const final { return true; }

    // 10.4.5.9 IsValidIntegerIndex ( O, index ), https://tc39.es/ecma262/#sec-isvalidintegerindex
    bool is_valid_integer_index(u32 property_index) const
    {
        if (viewed_array_buffer()->is_detached())
            return false;

        // FIXME: If ! IsIntegralNumber(index) is false, return false.

        // FIXME: If index is -0𝔽, return false.

        if (property_index >= m_array_length /* FIXME: or less than 0 (index is currently unsigned) */)
            return false;

        return true;
    }
};

#define JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    class ClassName : public TypedArray<Type> {                                             \
        JS_OBJECT(ClassName, TypedArray);                                                   \
                                                                                            \
    public:                                                                                 \
        virtual ~ClassName();                                                               \
        static ClassName* create(GlobalObject&, u32 length);                                \
        ClassName(u32 length, Object& prototype);                                           \
        virtual String element_name() const override;                                       \
    };                                                                                      \
    class PrototypeName final : public Object {                                             \
        JS_OBJECT(PrototypeName, Object);                                                   \
                                                                                            \
    public:                                                                                 \
        PrototypeName(GlobalObject&);                                                       \
        virtual ~PrototypeName() override;                                                  \
    };                                                                                      \
    class ConstructorName final : public TypedArrayConstructor {                            \
        JS_OBJECT(ConstructorName, TypedArrayConstructor);                                  \
                                                                                            \
    public:                                                                                 \
        explicit ConstructorName(GlobalObject&);                                            \
        virtual void initialize(GlobalObject&) override;                                    \
        virtual ~ConstructorName() override;                                                \
                                                                                            \
        virtual Value call() override;                                                      \
        virtual Value construct(Function& new_target) override;                             \
                                                                                            \
    private:                                                                                \
        virtual bool has_constructor() const override { return true; }                      \
    };

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
