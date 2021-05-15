/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Error* Error::create(GlobalObject& global_object, const String& message)
{
    auto& vm = global_object.vm();
    auto* error = global_object.heap().allocate<Error>(global_object, *global_object.error_prototype());
    if (!message.is_null())
        error->define_property(vm.names.message, js_string(vm, message));
    return error;
}

Error::Error(Object& prototype)
    : Object(prototype)
{
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)                                \
    ClassName* ClassName::create(GlobalObject& global_object, const String& message)                                    \
    {                                                                                                                   \
        auto& vm = global_object.vm();                                                                                  \
        auto* error = global_object.heap().allocate<ClassName>(global_object, *global_object.snake_name##_prototype()); \
        if (!message.is_null())                                                                                         \
            error->define_property(vm.names.message, js_string(vm, message));                                           \
        return error;                                                                                                   \
    }                                                                                                                   \
                                                                                                                        \
    ClassName::ClassName(Object& prototype)                                                                             \
        : Error(prototype)                                                                                              \
    {                                                                                                                   \
    }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
