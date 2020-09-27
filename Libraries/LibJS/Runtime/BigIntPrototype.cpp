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
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BigIntPrototype::BigIntPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void BigIntPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("toString", to_string, 0, attr);
    define_native_function("toLocaleString", to_locale_string, 0, attr);
    define_native_function("valueOf", value_of, 0, attr);

    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "BigInt"), Attribute::Configurable);
}

BigIntPrototype::~BigIntPrototype()
{
}

static BigIntObject* bigint_object_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_bigint_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "BigInt");
        return nullptr;
    }
    return static_cast<BigIntObject*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_string)
{
    auto* bigint_object = bigint_object_from(vm, global_object);
    if (!bigint_object)
        return {};
    return js_string(vm, bigint_object->bigint().big_integer().to_base10());
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_locale_string)
{
    return to_string(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::value_of)
{
    auto* bigint_object = bigint_object_from(vm, global_object);
    if (!bigint_object)
        return {};
    return bigint_object->value_of();
}

}
