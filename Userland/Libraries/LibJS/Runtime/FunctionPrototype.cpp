/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ShadowRealm.h>

namespace JS {

FunctionPrototype::FunctionPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void FunctionPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.apply, apply, 2, attr);
    define_native_function(vm.names.bind, bind, 1, attr);
    define_native_function(vm.names.call, call, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(*vm.well_known_symbol_has_instance(), symbol_has_instance, 1, 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
    define_direct_property(vm.names.name, js_string(heap(), ""), Attribute::Configurable);
}

FunctionPrototype::~FunctionPrototype()
{
}

// 20.2.3.1 Function.prototype.apply ( thisArg, argArray ), https://tc39.es/ecma262/#sec-function.prototype.apply
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::apply)
{
    // 1. Let func be the this value.
    auto function_value = vm.this_value(global_object);

    // 2. If IsCallable(func) is false, throw a TypeError exception.
    if (!function_value.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, function_value.to_string_without_side_effects());

    auto& function = static_cast<FunctionObject&>(function_value.as_object());

    auto this_arg = vm.argument(0);
    auto arg_array = vm.argument(1);

    // 3. If argArray is undefined or null, then
    if (arg_array.is_nullish()) {
        // FIXME: a. Perform PrepareForTailCall().

        // b. Return ? Call(func, thisArg).
        return TRY(JS::call(global_object, function, this_arg));
    }

    // 4. Let argList be ? CreateListFromArrayLike(argArray).
    auto arguments = TRY(create_list_from_array_like(global_object, arg_array));

    // FIXME: 5. Perform PrepareForTailCall().

    // 6. Return ? Call(func, thisArg, argList).
    return TRY(JS::call(global_object, function, this_arg, move(arguments)));
}

// 20.2.3.2 Function.prototype.bind ( thisArg, ...args ), https://tc39.es/ecma262/#sec-function.prototype.bind
// 3.1.2.1 Function.prototype.bind ( thisArg, ...args ), https://tc39.es/proposal-shadowrealm/#sec-function.prototype.bind
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::bind)
{
    auto this_argument = vm.argument(0);

    // 1. Let Target be the this value.
    auto target_value = vm.this_value(global_object);

    // 2. If IsCallable(Target) is false, throw a TypeError exception.
    if (!target_value.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, target_value.to_string_without_side_effects());

    auto& target = static_cast<FunctionObject&>(target_value.as_object());

    Vector<Value> arguments;
    if (vm.argument_count() > 1) {
        arguments = vm.running_execution_context().arguments;
        arguments.remove(0);
    }

    // 3. Let F be ? BoundFunctionCreate(Target, thisArg, args).
    auto* function = TRY(BoundFunction::create(global_object, target, this_argument, move(arguments)));

    // 4. Let argCount be the number of elements in args.
    auto arg_count = vm.argument_count() - 1;

    // 5. Perform ? CopyNameAndLength(F, Target, "bound", argCount).
    TRY(copy_name_and_length(global_object, *function, target, "bound"sv, arg_count));

    // 6. Return F.
    return function;
}

// 20.2.3.3 Function.prototype.call ( thisArg, ...args ), https://tc39.es/ecma262/#sec-function.prototype.call
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::call)
{
    // 1. Let func be the this value.
    auto function_value = vm.this_value(global_object);

    // 2. If IsCallable(func) is false, throw a TypeError exception.
    if (!function_value.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, function_value.to_string_without_side_effects());

    auto& function = static_cast<FunctionObject&>(function_value.as_object());

    // FIXME: 3. Perform PrepareForTailCall().

    auto this_arg = vm.argument(0);
    MarkedVector<Value> arguments(vm.heap());
    if (vm.argument_count() > 1) {
        for (size_t i = 1; i < vm.argument_count(); ++i)
            arguments.append(vm.argument(i));
    }

    // 4. Return ? Call(func, thisArg, args).
    return TRY(JS::call(global_object, function, this_arg, move(arguments)));
}

// 20.2.3.5 Function.prototype.toString ( ), https://tc39.es/ecma262/#sec-function.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::to_string)
{
    // 1. Let func be the this value.
    auto function_value = vm.this_value(global_object);

    // If func is not a function, let's bail out early. The order of this step is not observable.
    if (!function_value.is_function()) {
        // 5. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Function");
    }

    auto& function = function_value.as_function();

    // 2. If Type(func) is Object and func has a [[SourceText]] internal slot and func.[[SourceText]] is a sequence of Unicode code points and ! HostHasSourceTextAvailable(func) is true, then
    if (is<ECMAScriptFunctionObject>(function)) {
        // a. Return ! CodePointsToString(func.[[SourceText]]).
        return js_string(vm, static_cast<ECMAScriptFunctionObject&>(function).source_text());
    }

    // 3. If func is a built-in function object, return an implementation-defined String source code representation of func. The representation must have the syntax of a NativeFunction. Additionally, if func has an [[InitialName]] internal slot and func.[[InitialName]] is a String, the portion of the returned String that would be matched by NativeFunctionAccessor[opt] PropertyName must be the value of func.[[InitialName]].
    if (is<NativeFunction>(function))
        return js_string(vm, String::formatted("function {}() {{ [native code] }}", static_cast<NativeFunction&>(function).name()));

    // 4. If Type(func) is Object and IsCallable(func) is true, return an implementation-defined String source code representation of func. The representation must have the syntax of a NativeFunction.
    // NOTE: ProxyObject, BoundFunction, WrappedFunction
    return js_string(vm, "function () { [native code] }");
}

// 20.2.3.6 Function.prototype [ @@hasInstance ] ( V ), https://tc39.es/ecma262/#sec-function.prototype-@@hasinstance
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::symbol_has_instance)
{
    return TRY(ordinary_has_instance(global_object, vm.argument(0), vm.this_value(global_object)));
}

}
