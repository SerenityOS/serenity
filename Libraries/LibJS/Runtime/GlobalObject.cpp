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
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberPrototype.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

template<typename ConstructorType>
void GlobalObject::add_constructor(const FlyString& property_name, ConstructorType*& constructor, Object& prototype)
{
    constructor = heap().allocate<ConstructorType>();
    prototype.put("constructor", constructor);
    put(property_name, constructor);
}

GlobalObject::GlobalObject()
    : Object(nullptr)
{
}

void GlobalObject::initialize()
{
    // These are done first since other prototypes depend on their presence.
    m_empty_object_shape = heap().allocate<Shape>();
    m_object_prototype = heap().allocate<ObjectPrototype>();
    m_function_prototype = heap().allocate<FunctionPrototype>();

    static_cast<FunctionPrototype*>(m_function_prototype)->initialize();
    static_cast<ObjectPrototype*>(m_object_prototype)->initialize();

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    if (!m_##snake_name##_prototype)                                          \
        m_##snake_name##_prototype = heap().allocate<PrototypeName>();
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

    put_native_function("gc", gc);
    put_native_function("isNaN", is_nan, 1);

    // FIXME: These are read-only in ES5
    put("NaN", js_nan());
    put("Infinity", js_infinity());
    put("undefined", js_undefined());

    put("globalThis", this);
    put("console", heap().allocate<ConsoleObject>());
    put("Math", heap().allocate<MathObject>());

    add_constructor("Array", m_array_constructor, *m_array_prototype);
    add_constructor("Boolean", m_boolean_constructor, *m_boolean_prototype);
    add_constructor("Date", m_date_constructor, *m_date_prototype);
    add_constructor("Error", m_error_constructor, *m_error_prototype);
    add_constructor("Function", m_function_constructor, *m_function_prototype);
    add_constructor("Number", m_number_constructor, *m_number_prototype);
    add_constructor("Object", m_object_constructor, *m_object_prototype);
    add_constructor("String", m_string_constructor, *m_string_prototype);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    add_constructor(#ClassName, m_##snake_name##_constructor, *m_##snake_name##_prototype);
    JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE
}

GlobalObject::~GlobalObject()
{
}

void GlobalObject::visit_children(Visitor& visitor)
{
    Object::visit_children(visitor);

    visitor.visit(m_empty_object_shape);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    visitor.visit(m_##snake_name##_constructor);
    JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE
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
