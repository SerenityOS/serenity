/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/Checked.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

static void initialize_typed_array_from_array_buffer(GlobalObject& global_object, TypedArrayBase& typed_array, ArrayBuffer& array_buffer, Value byte_offset, Value length)
{
    // 22.2.5.1.3 InitializeTypedArrayFromArrayBuffer, https://tc39.es/ecma262/#sec-initializetypedarrayfromarraybuffer

    auto& vm = global_object.vm();
    auto element_size = typed_array.element_size();
    auto offset = byte_offset.to_index(global_object);
    if (vm.exception())
        return;
    if (offset % element_size != 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayInvalidByteOffset, typed_array.class_name(), element_size, offset);
        return;
    }
    size_t new_length { 0 };
    if (!length.is_undefined()) {
        new_length = length.to_index(global_object);
        if (vm.exception())
            return;
    }
    // FIXME: 8. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    auto buffer_byte_length = array_buffer.byte_length();
    size_t new_byte_length;
    if (length.is_undefined()) {
        if (buffer_byte_length % element_size != 0) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayInvalidBufferLength, typed_array.class_name(), element_size, buffer_byte_length);
            return;
        }
        if (offset > buffer_byte_length) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffset, offset, buffer_byte_length);
            return;
        }
        new_byte_length = buffer_byte_length - offset;
    } else {
        new_byte_length = new_length * element_size;
        if (offset + new_byte_length > buffer_byte_length) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TypedArrayOutOfRangeByteOffsetOrLength, offset, offset + new_byte_length, buffer_byte_length);
            return;
        }
    }
    typed_array.set_viewed_array_buffer(&array_buffer);
    typed_array.set_byte_length(new_byte_length);
    typed_array.set_byte_offset(offset);
    typed_array.set_array_length(new_byte_length / element_size);
}

void TypedArrayBase::visit_edges(Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                                             \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length)                                                              \
    {                                                                                                                                  \
        return global_object.heap().allocate<ClassName>(global_object, length, *global_object.snake_name##_prototype());               \
    }                                                                                                                                  \
                                                                                                                                       \
    ClassName::ClassName(u32 length, Object& prototype)                                                                                \
        : TypedArray(length, prototype)                                                                                                \
    {                                                                                                                                  \
    }                                                                                                                                  \
    ClassName::~ClassName() { }                                                                                                        \
                                                                                                                                       \
    PrototypeName::PrototypeName(GlobalObject& global_object)                                                                          \
        : Object(*global_object.typed_array_prototype())                                                                               \
    {                                                                                                                                  \
    }                                                                                                                                  \
    PrototypeName::~PrototypeName() { }                                                                                                \
                                                                                                                                       \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                                                      \
        : TypedArrayConstructor(vm().names.ClassName, *global_object.typed_array_constructor())                                        \
    {                                                                                                                                  \
    }                                                                                                                                  \
    ConstructorName::~ConstructorName() { }                                                                                            \
    void ConstructorName::initialize(GlobalObject& global_object)                                                                      \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        NativeFunction::initialize(global_object);                                                                                     \
        define_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                                \
        define_property(vm.names.length, Value(1), Attribute::Configurable);                                                           \
        define_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                                      \
    }                                                                                                                                  \
    Value ConstructorName::call()                                                                                                      \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ClassName);                          \
        return {};                                                                                                                     \
    }                                                                                                                                  \
    Value ConstructorName::construct(Function&)                                                                                        \
    {                                                                                                                                  \
        auto& vm = this->vm();                                                                                                         \
        if (vm.argument_count() == 0)                                                                                                  \
            return ClassName::create(global_object(), 0);                                                                              \
                                                                                                                                       \
        auto first_argument = vm.argument(0);                                                                                          \
        if (first_argument.is_object()) {                                                                                              \
            auto* typed_array = ClassName::create(global_object(), 0);                                                                 \
            if (first_argument.as_object().is_typed_array()) {                                                                         \
                /* FIXME: Initialize from TypedArray */                                                                                \
                TODO();                                                                                                                \
            } else if (is<ArrayBuffer>(first_argument.as_object())) {                                                                  \
                auto& array_buffer = static_cast<ArrayBuffer&>(first_argument.as_object());                                            \
                initialize_typed_array_from_array_buffer(global_object(), *typed_array, array_buffer, vm.argument(1), vm.argument(2)); \
                if (vm.exception())                                                                                                    \
                    return {};                                                                                                         \
            } else {                                                                                                                   \
                /* FIXME: Initialize from Iterator or Array-like object */                                                             \
                TODO();                                                                                                                \
            }                                                                                                                          \
            return typed_array;                                                                                                        \
        }                                                                                                                              \
                                                                                                                                       \
        auto array_length = first_argument.to_index(global_object());                                                                  \
        if (vm.exception()) {                                                                                                          \
            /* Re-throw more specific RangeError */                                                                                    \
            vm.clear_exception();                                                                                                      \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        if (array_length > NumericLimits<i32>::max()) {                                                                                \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        /* FIXME: What is the best/correct behavior here? */                                                                           \
        if (Checked<u32>::multiplication_would_overflow(array_length, sizeof(Type))) {                                                 \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                                  \
            return {};                                                                                                                 \
        }                                                                                                                              \
        return ClassName::create(global_object(), array_length);                                                                       \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
