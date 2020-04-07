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

#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GlobalObject::GlobalObject()
{
    put_native_function("gc", gc);
    put_native_function("isNaN", is_nan, 1);

    // FIXME: These are read-only in ES5
    put("NaN", js_nan());
    put("Infinity", js_infinity());
    put("undefined", js_undefined());

    put("console", heap().allocate<ConsoleObject>());
    put("Date", heap().allocate<DateConstructor>());
    put("Error", heap().allocate<ErrorConstructor>());
    put("Function", heap().allocate<FunctionConstructor>());
    put("Math", heap().allocate<MathObject>());
    put("Object", heap().allocate<ObjectConstructor>());
    put("Array", heap().allocate<ArrayConstructor>());
    put("Boolean", heap().allocate<BooleanConstructor>());
    put("Number", heap().allocate<NumberConstructor>());
}

GlobalObject::~GlobalObject()
{
}

Value GlobalObject::gc(Interpreter& interpreter)
{
    dbg() << "Forced garbage collection requested!";
    interpreter.heap().collect_garbage();
    return js_undefined();
}

Value GlobalObject::is_nan(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 1)
        return js_undefined();
    return Value(interpreter.argument(0).to_number().is_nan());
}

}
