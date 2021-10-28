/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>

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
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!this_object->is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Function");
    auto& function = static_cast<FunctionObject&>(*this_object);
    auto this_arg = vm.argument(0);
    auto arg_array = vm.argument(1);
    if (arg_array.is_nullish())
        return TRY(vm.call(function, this_arg));
    auto arguments = TRY(create_list_from_array_like(global_object, arg_array));
    return TRY(vm.call(function, this_arg, move(arguments)));
}

// 20.2.3.2 Function.prototype.bind ( thisArg, ...args ), https://tc39.es/ecma262/#sec-function.prototype.bind
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::bind)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!this_object->is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Function");
    auto& this_function = static_cast<FunctionObject&>(*this_object);
    auto bound_this_arg = vm.argument(0);

    Vector<Value> arguments;
    if (vm.argument_count() > 1) {
        arguments = vm.running_execution_context().arguments;
        arguments.remove(0);
    }

    return this_function.bind(bound_this_arg, move(arguments));
}

// 20.2.3.3 Function.prototype.call ( thisArg, ...args ), https://tc39.es/ecma262/#sec-function.prototype.call
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::call)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!this_object->is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Function");
    auto& function = static_cast<FunctionObject&>(*this_object);
    auto this_arg = vm.argument(0);
    MarkedValueList arguments(vm.heap());
    if (vm.argument_count() > 1) {
        for (size_t i = 1; i < vm.argument_count(); ++i)
            arguments.append(vm.argument(i));
    }
    return TRY(vm.call(function, this_arg, move(arguments)));
}

// 20.2.3.5 Function.prototype.toString ( ), https://tc39.es/ecma262/#sec-function.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::to_string)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!this_object->is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Function");
    String function_name;
    String function_parameters;
    String function_body;

    if (is<ECMAScriptFunctionObject>(this_object)) {
        auto& function = static_cast<ECMAScriptFunctionObject&>(*this_object);
        StringBuilder parameters_builder;
        auto first = true;
        for (auto& parameter : function.formal_parameters()) {
            // FIXME: Also stringify binding patterns.
            if (auto* name_ptr = parameter.binding.get_pointer<FlyString>()) {
                if (!first)
                    parameters_builder.append(", ");
                first = false;
                parameters_builder.append(*name_ptr);
                if (parameter.default_value) {
                    // FIXME: See note below
                    parameters_builder.append(" = TODO");
                }
            }
        }
        function_name = function.name();
        function_parameters = parameters_builder.build();
        // FIXME: ASTNodes should be able to dump themselves to source strings - something like this:
        // auto& body = static_cast<ECMAScriptFunctionObject*>(this_object)->body();
        // function_body = body.to_source();
        function_body = "  ???";
    } else {
        // This is "implementation-defined" - other engines don't include a name for
        // ProxyObject and BoundFunction, only NativeFunction - let's do the same here.
        if (is<NativeFunction>(this_object))
            function_name = static_cast<NativeFunction&>(*this_object).name();
        function_body = "  [native code]";
    }

    auto function_source = String::formatted(
        "function {}({}) {{\n{}\n}}",
        function_name.is_null() ? "" : function_name,
        function_parameters.is_null() ? "" : function_parameters,
        function_body);
    return js_string(vm, function_source);
}

// 20.2.3.6 Function.prototype [ @@hasInstance ] ( V ), https://tc39.es/ecma262/#sec-function.prototype-@@hasinstance
JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::symbol_has_instance)
{
    return TRY(ordinary_has_instance(global_object, vm.argument(0), vm.this_value(global_object)));
}

}
