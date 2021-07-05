/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Error* Error::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Error>(global_object, *global_object.error_prototype());
}

Error* Error::create(GlobalObject& global_object, String const& message)
{
    auto& vm = global_object.vm();
    auto* error = Error::create(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    error->define_property(vm.names.message, js_string(vm, message), attr);
    return error;
}

Error::Error(Object& prototype)
    : Object(prototype)
{
}

// 20.5.8.1 InstallErrorCause ( O, options ), https://tc39.es/proposal-error-cause/#sec-errorobjects-install-error-cause
void Error::install_error_cause(Value options)
{
    auto& vm = this->vm();
    if (!options.is_object())
        return;
    auto& options_object = options.as_object();
    if (!options_object.has_property(vm.names.cause))
        return;
    auto cause = options_object.get(vm.names.cause);
    if (vm.exception())
        return;
    create_non_enumerable_data_property_or_throw(vm.names.cause, cause);
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)                         \
    ClassName* ClassName::create(GlobalObject& global_object)                                                    \
    {                                                                                                            \
        return global_object.heap().allocate<ClassName>(global_object, *global_object.snake_name##_prototype()); \
    }                                                                                                            \
                                                                                                                 \
    ClassName* ClassName::create(GlobalObject& global_object, String const& message)                             \
    {                                                                                                            \
        auto& vm = global_object.vm();                                                                           \
        auto* error = ClassName::create(global_object);                                                          \
        u8 attr = Attribute::Writable | Attribute::Configurable;                                                 \
        error->define_property(vm.names.message, js_string(vm, message), attr);                                  \
        return error;                                                                                            \
    }                                                                                                            \
                                                                                                                 \
    ClassName::ClassName(Object& prototype)                                                                      \
        : Error(prototype)                                                                                       \
    {                                                                                                            \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
