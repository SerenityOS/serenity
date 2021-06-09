/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/Runtime/SetIterator.h>
#include <LibJS/Runtime/SetPrototype.h>

namespace JS {

SetPrototype::SetPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void SetPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.add, add, 1, attr);
    define_native_function(vm.names.clear, clear, 0, attr);
    define_native_function(vm.names.delete_, delete_, 1, attr);
    define_native_function(vm.names.entries, entries, 0, attr);
    define_native_function(vm.names.forEach, for_each, 1, attr);
    define_native_function(vm.names.has, has, 1, attr);
    define_native_function(vm.names.values, values, 0, attr);

    define_native_property(vm.names.size, size_getter, {}, attr);

    define_property(vm.names.keys, get(vm.names.values), attr);
    define_property(vm.well_known_symbol_iterator(), get(vm.names.values), attr);
    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.Set), Attribute::Configurable);
}

SetPrototype::~SetPrototype()
{
}

Set* SetPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Set>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Set");
        return nullptr;
    }
    return static_cast<Set*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::add)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    auto value = vm.argument(0);
    if (value.is_negative_zero())
        value = Value(0);
    set->values().set(value, AK::HashSetExistingEntryBehavior::Keep);
    return set;
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::clear)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    set->values().clear();
    return js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::delete_)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    return Value(set->values().remove(vm.argument(0)));
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::entries)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};

    return SetIterator::create(global_object, *set, Object::PropertyKind::KeyAndValue);
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::for_each)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    if (!vm.argument(0).is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, vm.argument(0).to_string_without_side_effects());
        return {};
    }
    auto this_value = vm.this_value(global_object);
    for (auto& value : set->values()) {
        (void)vm.call(vm.argument(0).as_function(), vm.argument(1), value, value, this_value);
        if (vm.exception())
            return {};
    }
    return js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::has)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    auto& values = set->values();
    return Value(values.find(vm.argument(0)) != values.end());
}

JS_DEFINE_NATIVE_FUNCTION(SetPrototype::values)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};

    return SetIterator::create(global_object, *set, Object::PropertyKind::Value);
}

JS_DEFINE_NATIVE_GETTER(SetPrototype::size_getter)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    return Value(set->values().size());
}

}
