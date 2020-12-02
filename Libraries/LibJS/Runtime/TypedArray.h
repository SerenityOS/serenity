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

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

template<typename T>
class TypedArray : public Object {
    JS_OBJECT(TypedArray, Object);

public:
    virtual ~TypedArray() override
    {
        ASSERT(m_data);
        free(m_data);
        m_data = nullptr;
    }

    i32 length() const { return m_length; }

    virtual bool put_by_index(u32 property_index, Value value) override
    {
        if (property_index >= m_length)
            return Base::put_by_index(property_index, value);

        if constexpr (sizeof(T) < 4) {
            auto number = value.to_i32(global_object());
            if (vm().exception())
                return {};
            m_data[property_index] = number;
        } else if constexpr (sizeof(T) == 4) {
            auto number = value.to_double(global_object());
            if (vm().exception())
                return {};
            m_data[property_index] = number;
        } else {
            static_assert(DependentFalse<T>, "TypedArray::put_by_index with unhandled type size");
        }
        return true;
    }

    virtual Value get_by_index(u32 property_index) const override
    {
        if (property_index >= m_length)
            return Base::get_by_index(property_index);

        if constexpr (sizeof(T) < 4) {
            return Value((i32)m_data[property_index]);
        } else if constexpr (sizeof(T) == 4) {
            auto value = m_data[property_index];
            if constexpr (NumericLimits<T>::is_signed()) {
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

    T* data() { return m_data; }
    const T* data() const { return m_data; }

protected:
    TypedArray(u32 length, Object& prototype)
        : Object(prototype)
        , m_length(length)
    {
        auto& vm = this->vm();
        // FIXME: This belongs to TypedArray.prototype
        define_native_property(vm.names.length, length_getter, nullptr);
        m_data = (T*)calloc(m_length, sizeof(T));
    }

private:
    virtual bool is_typed_array() const final { return true; }

    JS_DECLARE_NATIVE_GETTER(length_getter);

    T* m_data { nullptr };
    u32 m_length { 0 };
};

template<typename T>
inline JS_DEFINE_NATIVE_GETTER(TypedArray<T>::length_getter)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "TypedArray");
        return {};
    }
    return Value(static_cast<const TypedArray*>(this_object)->length());
}

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
        virtual void initialize(GlobalObject&) override;                                    \
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

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DECLARE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
