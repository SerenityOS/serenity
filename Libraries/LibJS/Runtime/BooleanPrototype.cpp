/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
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
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BooleanPrototype::BooleanPrototype(GlobalObject& global_object)
    : BooleanObject(false, *global_object.object_prototype())
{
}

void BooleanPrototype::initialize(GlobalObject& global_object)
{
    BooleanObject::initialize(global_object);
    define_native_function("toString", to_string, 0, Attribute::Writable | Attribute::Configurable);
    define_native_function("valueOf", value_of, 0, Attribute::Writable | Attribute::Configurable);
}

BooleanPrototype::~BooleanPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(BooleanPrototype::to_string)
{
    auto this_object = vm.this_value(global_object);
    if (this_object.is_boolean()) {
        return js_string(vm, this_object.as_bool() ? "true" : "false");
    }
    if (!this_object.is_object() || !this_object.as_object().is_boolean_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Boolean");
        return {};
    }

    bool bool_value = static_cast<BooleanObject&>(this_object.as_object()).value_of().as_bool();
    return js_string(vm, bool_value ? "true" : "false");
}

JS_DEFINE_NATIVE_FUNCTION(BooleanPrototype::value_of)
{
    auto this_object = vm.this_value(global_object);
    if (this_object.is_boolean()) {
        return this_object;
    }
    if (!this_object.is_object() || !this_object.as_object().is_boolean_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Boolean");
        return {};
    }

    return static_cast<BooleanObject&>(this_object.as_object()).value_of();
}
}
