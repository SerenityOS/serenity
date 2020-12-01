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

namespace JS {

#define JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, Type)                               \
    ClassName::~ClassName() { }                                                                                          \
    ClassName* ClassName::create(GlobalObject& global_object, u32 length)                                                \
    {                                                                                                                    \
        return global_object.heap().allocate<ClassName>(global_object, length, *global_object.snake_name##_prototype()); \
    }                                                                                                                    \
                                                                                                                         \
    ClassName::ClassName(u32 length, Object& prototype)                                                                  \
        : TypedArray(length, prototype)                                                                                  \
    {                                                                                                                    \
    }                                                                                                                    \
                                                                                                                         \
    PrototypeName::PrototypeName(GlobalObject& global_object)                                                            \
        : Object(*global_object.object_prototype())                                                                      \
    {                                                                                                                    \
    }                                                                                                                    \
    void PrototypeName::initialize(GlobalObject& global_object)                                                          \
    {                                                                                                                    \
        auto& vm = this->vm();                                                                                           \
        Object::initialize(global_object);                                                                               \
        define_property(vm.names.length, Value(0), Attribute::Configurable);                                             \
    }                                                                                                                    \
    PrototypeName::~PrototypeName() { }                                                                                  \
                                                                                                                         \
    ConstructorName::~ConstructorName() { }                                                                              \
    Value ConstructorName::construct(Function&) { return call(); }                                                       \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                                        \
        : NativeFunction(vm().names.ClassName, *global_object.function_prototype())                                      \
    {                                                                                                                    \
    }                                                                                                                    \
    void ConstructorName::initialize(GlobalObject& global_object)                                                        \
    {                                                                                                                    \
        auto& vm = this->vm();                                                                                           \
        NativeFunction::initialize(global_object);                                                                       \
        define_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                  \
        define_property(vm.names.length, Value(1), Attribute::Configurable);                                             \
    }                                                                                                                    \
    Value ConstructorName::call()                                                                                        \
    {                                                                                                                    \
        if (vm().argument_count() <= 0)                                                                                  \
            return ClassName::create(global_object(), 0);                                                                \
                                                                                                                         \
        if (vm().argument_count() == 1 && vm().argument(0).is_number()) {                                                \
            auto array_length_value = vm().argument(0);                                                                  \
            if (!array_length_value.is_integer() || array_length_value.as_i32() < 0) {                                   \
                vm().throw_exception<TypeError>(global_object(), ErrorType::ArrayInvalidLength);                         \
                return {};                                                                                               \
            }                                                                                                            \
            auto* array = ClassName::create(global_object(), array_length_value.as_i32());                               \
            return array;                                                                                                \
        }                                                                                                                \
        auto* array = ClassName::create(global_object(), vm().argument_count());                                         \
        for (size_t i = 0; i < vm().argument_count(); ++i)                                                               \
            array->put_by_index(i, vm().argument(i));                                                                    \
        return array;                                                                                                    \
    }

#undef __JS_ENUMERATE
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    JS_DEFINE_TYPED_ARRAY(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType);
JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

}
