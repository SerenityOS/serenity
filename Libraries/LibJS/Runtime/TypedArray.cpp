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

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                               \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length)                                                \
    {                                                                                                                    \
        return global_object.heap().allocate<ClassName>(global_object, length, *global_object.snake_name##_prototype()); \
    }                                                                                                                    \
                                                                                                                         \
    ClassName::ClassName(u32 length, Object& prototype)                                                                  \
        : TypedArray(length, prototype)                                                                                  \
    {                                                                                                                    \
    }                                                                                                                    \
    ClassName::~ClassName() { }                                                                                          \
                                                                                                                         \
    PrototypeName::PrototypeName(GlobalObject& global_object)                                                            \
        : Object(*global_object.typed_array_prototype())                                                                 \
    {                                                                                                                    \
    }                                                                                                                    \
    PrototypeName::~PrototypeName() { }                                                                                  \
                                                                                                                         \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                                        \
        : TypedArrayConstructor(vm().names.ClassName, *global_object.typed_array_constructor())                          \
    {                                                                                                                    \
    }                                                                                                                    \
    ConstructorName::~ConstructorName() { }                                                                              \
    void ConstructorName::initialize(GlobalObject& global_object)                                                        \
    {                                                                                                                    \
        auto& vm = this->vm();                                                                                           \
        NativeFunction::initialize(global_object);                                                                       \
        define_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                  \
        define_property(vm.names.length, Value(1), Attribute::Configurable);                                             \
        define_property(vm.names.BYTES_PER_ELEMENT, Value((i32)sizeof(Type)), 0);                                        \
    }                                                                                                                    \
    Value ConstructorName::call()                                                                                        \
    {                                                                                                                    \
        auto& vm = this->vm();                                                                                           \
        vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ClassName);            \
        return {};                                                                                                       \
    }                                                                                                                    \
    Value ConstructorName::construct(Function&)                                                                          \
    {                                                                                                                    \
        auto& vm = this->vm();                                                                                           \
        if (vm.argument_count() == 0)                                                                                    \
            return ClassName::create(global_object(), 0);                                                                \
                                                                                                                         \
        if (vm.argument(0).is_object()) {                                                                                \
            /* FIXME: Initialize from TypedArray, ArrayBuffer, Iterable or Array-like object */                          \
            TODO();                                                                                                      \
        }                                                                                                                \
        auto array_length = vm.argument(0).to_index(global_object());                                                    \
        if (vm.exception()) {                                                                                            \
            /* Re-throw more specific RangeError */                                                                      \
            vm.clear_exception();                                                                                        \
            vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "typed array");                    \
            return {};                                                                                                   \
        }                                                                                                                \
        auto* array = ClassName::create(global_object(), array_length);                                                  \
        return array;                                                                                                    \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
