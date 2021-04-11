/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ErrorPrototype::ErrorPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ErrorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_property(vm.names.name, js_string(vm, "Error"), attr);
    define_property(vm.names.message, js_string(vm, ""), attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
}

// 20.5.3.4 Error.prototype.toString, https://tc39.es/ecma262/#sec-error.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ErrorPrototype::to_string)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, this_value.to_string_without_side_effects());
        return {};
    }
    auto& this_object = this_value.as_object();

    String name = "Error";
    auto name_property = this_object.get(vm.names.name).value_or(js_undefined());
    if (vm.exception())
        return {};
    if (!name_property.is_undefined()) {
        name = name_property.to_string(global_object);
        if (vm.exception())
            return {};
    }

    String message = "";
    auto message_property = this_object.get(vm.names.message).value_or(js_undefined());
    if (vm.exception())
        return {};
    if (!message_property.is_undefined()) {
        message = message_property.to_string(global_object);
        if (vm.exception())
            return {};
    }

    if (name.is_empty())
        return js_string(vm, message);
    if (message.is_empty())
        return js_string(vm, name);
    return js_string(vm, String::formatted("{}: {}", name, message));
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    PrototypeName::PrototypeName(GlobalObject& global_object)                            \
        : Object(*global_object.error_prototype())                                       \
    {                                                                                    \
    }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
