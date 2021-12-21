/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Completion.h>
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
    error->define_direct_property(vm.names.message, js_string(vm, message), attr);
    return error;
}

Error::Error(Object& prototype)
    : Object(prototype)
{
}

// 20.5.8.1 InstallErrorCause ( O, options ), https://tc39.es/ecma262/#sec-installerrorcause
ThrowCompletionOr<void> Error::install_error_cause(Value options)
{
    auto& vm = this->vm();

    // 1. If Type(options) is Object and ? HasProperty(options, "cause") is true, then
    if (options.is_object() && TRY(options.as_object().has_property(vm.names.cause))) {
        // a. Let cause be ? Get(options, "cause").
        auto cause = TRY(options.as_object().get(vm.names.cause));

        // b. Perform ! CreateNonEnumerableDataPropertyOrThrow(O, "cause", cause).
        MUST(create_non_enumerable_data_property_or_throw(vm.names.cause, cause));
    }

    // 2. Return NormalCompletion(undefined).
    return {};
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
        error->define_direct_property(vm.names.message, js_string(vm, message), attr);                           \
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
