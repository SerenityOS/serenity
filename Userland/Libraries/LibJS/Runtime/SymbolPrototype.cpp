/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/Value.h>
#include <string.h>

namespace JS {

SymbolPrototype::SymbolPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void SymbolPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_native_property(vm.names.description, description_getter, {}, Attribute::Configurable);
    define_native_function(vm.names.toString, to_string, 0, Attribute::Writable | Attribute::Configurable);
    define_native_function(vm.names.valueOf, value_of, 0, Attribute::Writable | Attribute::Configurable);

    define_property(global_object.vm().well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Symbol"), Attribute::Configurable);
}

SymbolPrototype::~SymbolPrototype()
{
}

static SymbolObject* typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!is<SymbolObject>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Symbol");
        return nullptr;
    }
    return static_cast<SymbolObject*>(this_object);
}

JS_DEFINE_NATIVE_GETTER(SymbolPrototype::description_getter)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return js_string(vm, this_object->description());
}

JS_DEFINE_NATIVE_FUNCTION(SymbolPrototype::to_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    auto string = this_object->primitive_symbol().to_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(SymbolPrototype::value_of)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return this_object->value_of();
}
}
