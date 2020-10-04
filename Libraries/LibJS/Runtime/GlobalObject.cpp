/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/LogStream.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/ArrayIteratorPrototype.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/BigIntConstructor.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberPrototype.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/ProxyConstructor.h>
#include <LibJS/Runtime/ProxyPrototype.h>
#include <LibJS/Runtime/ReflectObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringIteratorPrototype.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/SymbolConstructor.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GlobalObject::GlobalObject()
    : Object(GlobalObjectTag::Tag)
    , m_console(make<Console>(*this))
{
}

void GlobalObject::initialize()
{
    ensure_shape_is_unique();

    // These are done first since other prototypes depend on their presence.
    m_empty_object_shape = heap().allocate<Shape>(*this, *this);
    m_object_prototype = heap().allocate_without_global_object<ObjectPrototype>(*this);
    m_function_prototype = heap().allocate_without_global_object<FunctionPrototype>(*this);

    static_cast<FunctionPrototype*>(m_function_prototype)->initialize(*this);
    static_cast<ObjectPrototype*>(m_object_prototype)->initialize(*this);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    if (!m_##snake_name##_prototype)                                          \
        m_##snake_name##_prototype = heap().allocate<PrototypeName>(*this, *this);
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    if (!m_##snake_name##_prototype)          \
        m_##snake_name##_prototype = heap().allocate<ClassName##Prototype>(*this, *this);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("gc", gc, 0, attr);
    define_native_function("isNaN", is_nan, 1, attr);
    define_native_function("isFinite", is_finite, 1, attr);
    define_native_function("parseFloat", parse_float, 1, attr);

    define_property("NaN", js_nan(), 0);
    define_property("Infinity", js_infinity(), 0);
    define_property("undefined", js_undefined(), 0);

    define_property("globalThis", this, attr);
    define_property("console", heap().allocate<ConsoleObject>(*this, *this), attr);
    define_property("Math", heap().allocate<MathObject>(*this, *this), attr);
    define_property("JSON", heap().allocate<JSONObject>(*this, *this), attr);
    define_property("Reflect", heap().allocate<ReflectObject>(*this, *this), attr);

    add_constructor("Array", m_array_constructor, *m_array_prototype);
    add_constructor("BigInt", m_bigint_constructor, *m_bigint_prototype);
    add_constructor("Boolean", m_boolean_constructor, *m_boolean_prototype);
    add_constructor("Date", m_date_constructor, *m_date_prototype);
    add_constructor("Error", m_error_constructor, *m_error_prototype);
    add_constructor("Function", m_function_constructor, *m_function_prototype);
    add_constructor("Number", m_number_constructor, *m_number_prototype);
    add_constructor("Object", m_object_constructor, *m_object_prototype);
    add_constructor("Proxy", m_proxy_constructor, *m_proxy_prototype);
    add_constructor("RegExp", m_regexp_constructor, *m_regexp_prototype);
    add_constructor("String", m_string_constructor, *m_string_prototype);
    add_constructor("Symbol", m_symbol_constructor, *m_symbol_prototype);

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

#define __JS_ENUMERATE(ClassName, snake_name) \
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::gc)
{
    dbg() << "Forced garbage collection requested!";
    vm.heap().collect_garbage();
    return js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_nan)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(number.is_nan());
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_finite)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(number.is_finite_number());
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::parse_float)
{
    if (vm.argument(0).is_number())
        return vm.argument(0);
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    for (size_t length = string.length(); length > 0; --length) {
        // This can't throw, so no exception check is fine.
        auto number = Value(js_string(vm, string.substring(0, length))).to_number(global_object);
        if (!number.is_nan())
            return number;
    }
    return js_nan();
}

}
