/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ErrorConstructor::ErrorConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Error.as_string(), *global_object.function_prototype())
{
}

void ErrorConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 20.5.2.1 Error.prototype, https://tc39.es/ecma262/#sec-error.prototype
    define_direct_property(vm.names.prototype, global_object.error_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 20.5.1.1 Error ( message [ , options ] ), https://tc39.es/ecma262/#sec-error-message
ThrowCompletionOr<Value> ErrorConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object; else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 20.5.1.1 Error ( message [ , options ] ), https://tc39.es/ecma262/#sec-error-message
ThrowCompletionOr<Object*> ErrorConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto message = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let O be ? OrdinaryCreateFromConstructor(newTarget, "%Error.prototype%", « [[ErrorData]] »).
    auto* error = TRY(ordinary_create_from_constructor<Error>(global_object, new_target, &GlobalObject::error_prototype));

    // 3. If message is not undefined, then
    if (!message.is_undefined()) {
        // a. Let msg be ? ToString(message).
        auto msg = TRY(message.to_string(global_object));

        // b. Perform ! CreateNonEnumerableDataPropertyOrThrow(O, "message", msg).
        MUST(error->create_non_enumerable_data_property_or_throw(vm.names.message, js_string(vm, move(msg))));
    }

    // 4. Perform ? InstallErrorCause(O, options).
    TRY(error->install_error_cause(options));

    // 5. Return O.
    return error;
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)                                                  \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                                                         \
        : NativeFunction(*static_cast<Object*>(global_object.error_constructor()))                                                        \
    {                                                                                                                                     \
    }                                                                                                                                     \
                                                                                                                                          \
    void ConstructorName::initialize(GlobalObject& global_object)                                                                         \
    {                                                                                                                                     \
        auto& vm = this->vm();                                                                                                            \
        NativeFunction::initialize(global_object);                                                                                        \
                                                                                                                                          \
        /* 20.5.6.2.1 NativeError.prototype, https://tc39.es/ecma262/#sec-nativeerror.prototype */                                        \
        define_direct_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);                                            \
                                                                                                                                          \
        define_direct_property(vm.names.length, Value(1), Attribute::Configurable);                                                       \
    }                                                                                                                                     \
                                                                                                                                          \
    ConstructorName::~ConstructorName() { }                                                                                               \
                                                                                                                                          \
    /* 20.5.6.1.1 NativeError ( message [ , options ] ), https://tc39.es/ecma262/#sec-nativeerror */                                      \
    ThrowCompletionOr<Value> ConstructorName::call()                                                                                      \
    {                                                                                                                                     \
        /* 1. If NewTarget is undefined, let newTarget be the active function object; else let newTarget be NewTarget. */                 \
        return TRY(construct(*this));                                                                                                     \
    }                                                                                                                                     \
                                                                                                                                          \
    /* 20.5.6.1.1 NativeError ( message [ , options ] ), https://tc39.es/ecma262/#sec-nativeerror */                                      \
    ThrowCompletionOr<Object*> ConstructorName::construct(FunctionObject& new_target)                                                     \
    {                                                                                                                                     \
        auto& vm = this->vm();                                                                                                            \
        auto& global_object = this->global_object();                                                                                      \
                                                                                                                                          \
        auto message = vm.argument(0);                                                                                                    \
        auto options = vm.argument(1);                                                                                                    \
                                                                                                                                          \
        /* 2. Let O be ? OrdinaryCreateFromConstructor(newTarget, "%NativeError.prototype%", « [[ErrorData]] »). */                     \
        auto* error = TRY(ordinary_create_from_constructor<ClassName>(global_object, new_target, &GlobalObject::snake_name##_prototype)); \
                                                                                                                                          \
        /* 3. If message is not undefined, then */                                                                                        \
        if (!message.is_undefined()) {                                                                                                    \
            /* a. Let msg be ? ToString(message). */                                                                                      \
            auto msg = TRY(message.to_string(global_object));                                                                             \
                                                                                                                                          \
            /* b. Perform ! CreateNonEnumerableDataPropertyOrThrow(O, "message", msg). */                                                 \
            MUST(error->create_non_enumerable_data_property_or_throw(vm.names.message, js_string(vm, move(msg))));                        \
        }                                                                                                                                 \
                                                                                                                                          \
        /* 4. Perform ? InstallErrorCause(O, options). */                                                                                 \
        TRY(error->install_error_cause(options));                                                                                         \
                                                                                                                                          \
        /* 5. Return O. */                                                                                                                \
        return error;                                                                                                                     \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
