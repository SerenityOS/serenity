/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    u32 array_length() const { return m_array_length; }
    u32 byte_length() const { return m_byte_length; }
    u32 byte_offset() const { return m_byte_offset; }
    ArrayBuffer* viewed_array_buffer() const { return m_viewed_array_buffer; }

    void set_array_length(u32 length) { m_array_length = length; }
    void set_byte_length(u32 length) { m_byte_length = length; }
    void set_byte_offset(u32 offset) { m_byte_offset = offset; }
    void set_viewed_array_buffer(ArrayBuffer* array_buffer) { m_viewed_array_buffer = array_buffer; }

    virtual size_t element_size() const = 0;

protected:
    explicit TypedArrayBase(Object& prototype)
        : Object(prototype)
    {
    }

    u32 m_array_length { 0 };
    u32 m_byte_length { 0 };
    u32 m_byte_offset { 0 };
    ArrayBuffer* m_viewed_array_buffer { nullptr };

private:
    virtual void visit_edges(Visitor&) override;
};

template<typename T>
class TypedArray : public TypedArrayBase {
    JS_OBJECT(TypedArray, TypedArrayBase);

public:
    virtual bool put_by_index(u32 property_index, Value value) override
    {
        property_index += m_byte_offset / sizeof(T);
        if (property_index >= m_array_length)
            return Base::put_by_index(property_index, value);

        if constexpr (sizeof(T) < 4) {
            auto number = value.to_i32(global_object());
            if (vm().exception())
                return {};
            data()[property_index] = number;
        } else if constexpr (sizeof(T) == 4 || sizeof(T) == 8) {
            auto number = value.to_double(global_object());
            if (vm().exception())
                return {};
            data()[property_index] = number;
        } else {
            static_assert(DependentFalse<T>, "TypedArray::put_by_index with unhandled type size");
        }
        return true;
    }

    virtual Value get_by_index(u32 property_index) const override
    {
        property_index += m_byte_offset / sizeof(T);
        if (property_index >= m_array_length)
            return Base::get_by_index(property_index);

        if constexpr (sizeof(T) < 4) {
            return Value((i32)data()[property_index]);
        } else if constexpr (sizeof(T) == 4 || sizeof(T) == 8) {
            auto value = data()[property_index];
            if constexpr (IsFloatingPoint<T>::value) {
                return Value((double)value);
            } else if constexpr (NumericLimits<T>::is_signed()) {
                if (value > NumericLimits<i32>::max() || value < NumericLimits<i32>::min())
                    return Value((double)value);
            } else {
                if (value > NumericLimits<i32>::max())
                    return Value((double)value);
            }
            return Value((i32)value);
        } else {
            static_assert(DependentFalse<T>, "TypedArray::get_by_index with unhandled type size");
        }
    }

    Span<const T> data() const
    {
        return { reinterpret_cast<const T*>(m_viewed_array_buffer->buffer().data()), m_array_length };
    }
    Span<T> data()
    {
        return { reinterpret_cast<T*>(m_viewed_array_buffer->buffer().data()), m_array_length };
    }

    virtual size_t element_size() const override { return sizeof(T); };

protected:
    TypedArray(u32 array_length, Object& prototype)
        : TypedArrayBase(prototype)
    {
        VERIFY(!Checked<u32>::multiplication_would_overflow(array_length, sizeof(T)));
        m_viewed_array_buffer = ArrayBuffer::create(global_object(), array_length * sizeof(T));
        if (array_length)
            VERIFY(!data().is_null());
        m_array_length = array_length;
        m_byte_length = m_viewed_array_buffer->byte_length();
    }

private:
    virtual bool is_typed_array() const final { return true; }
};

#define JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    class ClassName : public TypedArray<Type> {                                             \
        JS_OBJECT(ClassName, TypedArray);                                                   \
                                                                                            \
    public:                                                                                 \
        virtual ~ClassName();                                                               \
        static ClassName* create(GlobalObject&, u32 length);                                \
        ClassName(u32 length, Object& prototype);                                           \
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
