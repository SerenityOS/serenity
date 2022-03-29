/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Contrib/Test262/IsHTMLDDA.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS::Test262 {

IsHTMLDDA::IsHTMLDDA(JS::GlobalObject& global_object)
    // NativeFunction without prototype is currently not possible (only due to the lack of a ctor that supports it)
    : NativeFunction("IsHTMLDDA", *global_object.function_prototype())
{
}

ThrowCompletionOr<Value> IsHTMLDDA::call()
{
    auto& vm = this->vm();
    if (vm.argument_count() == 0)
        return js_null();
    if (vm.argument(0).is_string() && vm.argument(0).as_string().string().is_empty())
        return js_null();
    // Not sure if this really matters, INTERPRETING.md simply says:
    // * IsHTMLDDA - (present only in implementations that can provide it) an object that:
    //   a. has an [[IsHTMLDDA]] internal slot, and
    //   b. when called with no arguments or with the first argument "" (an empty string) returns null.
    return js_undefined();
}

ThrowCompletionOr<Object*> IsHTMLDDA::construct(FunctionObject&)
{
    // Not sure if we need to support construction, but ¯\_(ツ)_/¯
    auto& vm = this->vm();
    auto& global_object = this->global_object();
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, "IsHTMLDDA");
}

}
