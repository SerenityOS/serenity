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

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

StringPrototype::StringPrototype()
{
    put_native_property(
        "length", [](Object* this_object) {
            ASSERT(this_object);
            ASSERT(this_object->is_string_object());
            return Value((i32) static_cast<const StringObject*>(this_object)->primitive_string()->string().length());
        },
        nullptr);
    put_native_function("charAt", [](Object* this_object, const Vector<Value>& arguments) -> Value {
        ASSERT(this_object);
        i32 index = 0;
        if (!arguments.is_empty())
            index = arguments[0].to_i32();
        ASSERT(this_object->is_string_object());
        auto underlying_string = static_cast<const StringObject*>(this_object)->primitive_string()->string();
        if (index < 0 || index >= static_cast<i32>(underlying_string.length()))
            return js_string(this_object->heap(), String::empty());
        return js_string(this_object->heap(), underlying_string.substring(index, 1));
    });
    put_native_function("repeat", [](Object* this_object, const Vector<Value>& arguments) -> Value {
        ASSERT(this_object->is_string_object());
        if (arguments.is_empty())
            return js_string(this_object->heap(), String::empty());
        i32 count = 0;
        count = arguments[0].to_i32();
        if (count < 0) {
            // FIXME: throw RangeError
            return js_undefined();
        }
        auto* string_object = static_cast<StringObject*>(this_object);
        StringBuilder builder;
        for (i32 i = 0; i < count; ++i)
            builder.append(string_object->primitive_string()->string());
        return js_string(this_object->heap(), builder.to_string());
    });
}

StringPrototype::~StringPrototype()
{
}

}
