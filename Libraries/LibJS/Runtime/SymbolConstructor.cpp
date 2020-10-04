/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/SymbolConstructor.h>
#include <LibJS/Runtime/SymbolObject.h>

namespace JS {

SymbolConstructor::SymbolConstructor(GlobalObject& global_object)
    : NativeFunction("Symbol", *global_object.function_prototype())
{
}

void SymbolConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.symbol_prototype(), 0);
    define_property("length", Value(0), Attribute::Configurable);

    define_native_function("for", for_, 1, Attribute::Writable | Attribute::Configurable);
    define_native_function("keyFor", key_for, 1, Attribute::Writable | Attribute::Configurable);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    define_property(#SymbolName, global_object.vm().well_known_symbol_##snake_name(), 0);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
}

SymbolConstructor::~SymbolConstructor()
{
}

Value SymbolConstructor::call()
{
    if (!vm().argument_count())
        return js_symbol(heap(), "", false);
    return js_symbol(heap(), vm().argument(0).to_string(global_object()), false);
}

Value SymbolConstructor::construct(Function&)
{
    vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, "Symbol");
    return {};
}

JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::for_)
{
    String description;
    if (!vm.argument_count()) {
        description = "undefined";
    } else {
        description = vm.argument(0).to_string(global_object);
    }

    return global_object.vm().get_global_symbol(description);
}

JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::key_for)
{
    auto argument = vm.argument(0);
    if (!argument.is_symbol()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotASymbol, argument.to_string_without_side_effects());
        return {};
    }

    auto& symbol = argument.as_symbol();
    if (symbol.is_global())
        return js_string(vm, symbol.description());

    return js_undefined();
}

}
