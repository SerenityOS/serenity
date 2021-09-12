/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

SymbolPrototype::SymbolPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void SymbolPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_accessor(vm.names.description, description_getter, {}, Attribute::Configurable);
    define_native_function(*vm.well_known_symbol_to_primitive(), symbol_to_primitive, 1, Attribute::Configurable);

    // 20.4.3.6 Symbol.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-symbol.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Symbol"), Attribute::Configurable);
}

SymbolPrototype::~SymbolPrototype()
{
}

// thisSymbolValue ( value ), https://tc39.es/ecma262/#thissymbolvalue
static Value this_symbol_value(GlobalObject& global_object, Value value)
{
    if (value.is_symbol())
        return value;
    if (value.is_object() && is<SymbolObject>(value.as_object()))
        return static_cast<SymbolObject&>(value.as_object()).value_of();
    auto& vm = global_object.vm();
    vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Symbol");
    return {};
}

// 20.4.3.2 get Symbol.prototype.description, https://tc39.es/ecma262/#sec-symbol.prototype.description
JS_DEFINE_NATIVE_GETTER(SymbolPrototype::description_getter)
{
    auto symbol_value = this_symbol_value(global_object, vm.this_value(global_object));
    if (vm.exception())
        return {};
    auto& description = symbol_value.as_symbol().raw_description();
    if (!description.has_value())
        return js_undefined();
    return js_string(vm, *description);
}

// 20.4.3.3 Symbol.prototype.toString ( ), https://tc39.es/ecma262/#sec-symbol.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(SymbolPrototype::to_string)
{
    auto symbol_value = this_symbol_value(global_object, vm.this_value(global_object));
    if (vm.exception())
        return {};
    return js_string(vm, symbol_value.as_symbol().to_string());
}

// 20.4.3.4 Symbol.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-symbol.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(SymbolPrototype::value_of)
{
    return this_symbol_value(global_object, vm.this_value(global_object));
}

// 20.4.3.5 Symbol.prototype [ @@toPrimitive ] ( hint ), https://tc39.es/ecma262/#sec-symbol.prototype-@@toprimitive
JS_DEFINE_NATIVE_FUNCTION(SymbolPrototype::symbol_to_primitive)
{
    // The hint argument is ignored.
    return this_symbol_value(global_object, vm.this_value(global_object));
}

}
