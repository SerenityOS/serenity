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
#include <LibJS/Interpreter.h>
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

Value StringConstructor::call(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return js_string(interpreter, "");
    if (interpreter.argument(0).is_symbol())
        return js_string(interpreter, interpreter.argument(0).as_symbol().to_string());
    auto* string = interpreter.argument(0).to_primitive_string(interpreter);
    if (interpreter.exception())
        return {};
    return string;
}

Value StringConstructor::construct(Interpreter& interpreter, Function&)
{
    PrimitiveString* primitive_string = nullptr;
    if (!interpreter.argument_count())
        primitive_string = js_string(interpreter, "");
    else
        primitive_string = interpreter.argument(0).to_primitive_string(interpreter);
    if (!primitive_string)
        return {};
    return StringObject::create(global_object(), *primitive_string);
}

JS_DEFINE_NATIVE_FUNCTION(StringConstructor::raw)
{
    auto* template_object = interpreter.argument(0).to_object(interpreter, global_object);
    if (interpreter.exception())
        return {};

    auto raw = template_object->get("raw");
    if (interpreter.exception())
        return {};
    if (raw.is_empty() || raw.is_undefined() || raw.is_null()) {
        interpreter.throw_exception<TypeError>(ErrorType::StringRawCannotConvert, raw.is_null() ? "null" : "undefined");
        return {};
    }
    if (!raw.is_array())
        return js_string(interpreter, "");

    auto* array = static_cast<Array*>(raw.to_object(interpreter, global_object));
    auto& raw_array_elements = array->indexed_properties();
    StringBuilder builder;

    for (size_t i = 0; i < raw_array_elements.array_like_size(); ++i) {
        auto result = raw_array_elements.get(array, i);
        if (interpreter.exception())
            return {};
        if (!result.has_value())
            continue;
        builder.append(result.value().value.to_string(interpreter));
        if (interpreter.exception())
            return {};
        if (i + 1 < interpreter.argument_count() && i < raw_array_elements.array_like_size() - 1) {
            builder.append(interpreter.argument(i + 1).to_string(interpreter));
            if (interpreter.exception())
                return {};
        }
    }

    return js_string(interpreter, builder.build());
}

JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_char_code)
{
    StringBuilder builder;
    for (size_t i = 0; i < interpreter.argument_count(); ++i) {
        auto char_code = interpreter.argument(i).to_i32(interpreter);
        if (interpreter.exception())
            return {};
        auto truncated = char_code & 0xffff;
        // FIXME: We need an Utf16View :^)
        builder.append(Utf32View((u32*)&truncated, 1));
    }
    return js_string(interpreter, builder.build());
}

}
