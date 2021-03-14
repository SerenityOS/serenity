/*
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

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ScriptFunction.h>

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
    define_native_function(vm.well_known_symbol_has_instance(), symbol_has_instance, 1, 0);
    define_property(vm.names.length, Value(0), Attribute::Configurable);
    define_property(vm.names.name, js_string(heap(), ""), Attribute::Configurable);
}

FunctionPrototype::~FunctionPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::apply)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Function");
        return {};
    }
    auto& function = static_cast<Function&>(*this_object);
    auto this_arg = vm.argument(0);
    auto arg_array = vm.argument(1);
    if (arg_array.is_nullish())
        return vm.call(function, this_arg);
    if (!arg_array.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::FunctionArgsNotObject);
        return {};
    }
    auto length = length_of_array_like(global_object, arg_array.as_object());
    if (vm.exception())
        return {};
    MarkedValueList arguments(vm.heap());
    for (size_t i = 0; i < length; ++i) {
        auto element = arg_array.as_object().get(i);
        if (vm.exception())
            return {};
        arguments.append(element.value_or(js_undefined()));
    }
    return vm.call(function, this_arg, move(arguments));
}

JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::bind)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Function");
        return {};
    }
    auto& this_function = static_cast<Function&>(*this_object);
    auto bound_this_arg = vm.argument(0);

    Vector<Value> arguments;
    if (vm.argument_count() > 1) {
        arguments = vm.call_frame().arguments;
        arguments.remove(0);
    }

    return this_function.bind(bound_this_arg, move(arguments));
}

JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::call)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Function");
        return {};
    }
    auto& function = static_cast<Function&>(*this_object);
    auto this_arg = vm.argument(0);
    MarkedValueList arguments(vm.heap());
    if (vm.argument_count() > 1) {
        for (size_t i = 1; i < vm.argument_count(); ++i)
            arguments.append(vm.argument(i));
    }
    return vm.call(function, this_arg, move(arguments));
}

JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::to_string)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Function");
        return {};
    }
    String function_name;
    String function_parameters;
    String function_body;

    if (is<ScriptFunction>(this_object)) {
        auto& script_function = static_cast<ScriptFunction&>(*this_object);
        StringBuilder parameters_builder;
        auto first = true;
        for (auto& parameter : script_function.parameters()) {
            if (!first)
                parameters_builder.append(", ");
            first = false;
            parameters_builder.append(parameter.name);
            if (parameter.default_value) {
                // FIXME: See note below
                parameters_builder.append(" = TODO");
            }
        }
        function_name = script_function.name();
        function_parameters = parameters_builder.build();
        // FIXME: ASTNodes should be able to dump themselves to source strings - something like this:
        // auto& body = static_cast<ScriptFunction*>(this_object)->body();
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

JS_DEFINE_NATIVE_FUNCTION(FunctionPrototype::symbol_has_instance)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Function");
        return {};
    }
    return ordinary_has_instance(global_object, vm.argument(0), this_object);
}

}
