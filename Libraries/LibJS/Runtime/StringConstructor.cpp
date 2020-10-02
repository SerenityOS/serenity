/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringObject.h>

namespace JS {

StringConstructor::StringConstructor(GlobalObject& global_object)
    : NativeFunction("String", *global_object.function_prototype())
{
}

void StringConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.string_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("raw", raw, 1, attr);
    define_native_function("fromCharCode", from_char_code, 1, attr);
}

StringConstructor::~StringConstructor()
{
}

Value StringConstructor::call()
{
    if (!vm().argument_count())
        return js_string(heap(), "");
    if (vm().argument(0).is_symbol())
        return js_string(heap(), vm().argument(0).as_symbol().to_string());
    auto* string = vm().argument(0).to_primitive_string(global_object());
    if (vm().exception())
        return {};
    return string;
}

Value StringConstructor::construct(Function&)
{
    PrimitiveString* primitive_string = nullptr;
    if (!vm().argument_count())
        primitive_string = js_string(vm(), "");
    else
        primitive_string = vm().argument(0).to_primitive_string(global_object());
    if (!primitive_string)
        return {};
    return StringObject::create(global_object(), *primitive_string);
}

JS_DEFINE_NATIVE_FUNCTION(StringConstructor::raw)
{
    auto* template_object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    auto raw = template_object->get("raw");
    if (vm.exception())
        return {};
    if (raw.is_empty() || raw.is_nullish()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::StringRawCannotConvert, raw.is_null() ? "null" : "undefined");
        return {};
    }
    if (!raw.is_array())
        return js_string(vm, "");

    auto* array = static_cast<Array*>(raw.to_object(global_object));
    auto& raw_array_elements = array->indexed_properties();
    StringBuilder builder;

    for (size_t i = 0; i < raw_array_elements.array_like_size(); ++i) {
        auto result = raw_array_elements.get(array, i);
        if (vm.exception())
            return {};
        if (!result.has_value())
            continue;
        builder.append(result.value().value.to_string(global_object));
        if (vm.exception())
            return {};
        if (i + 1 < vm.argument_count() && i < raw_array_elements.array_like_size() - 1) {
            builder.append(vm.argument(i + 1).to_string(global_object));
            if (vm.exception())
                return {};
        }
    }

    return js_string(vm, builder.build());
}

JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_char_code)
{
    StringBuilder builder;
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto char_code = vm.argument(i).to_i32(global_object);
        if (vm.exception())
            return {};
        auto truncated = char_code & 0xffff;
        // FIXME: We need an Utf16View :^)
        builder.append(Utf32View((u32*)&truncated, 1));
    }
    return js_string(vm, builder.build());
}

}
