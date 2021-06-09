/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/Runtime/WeakSetPrototype.h>

namespace JS {

WeakSetPrototype::WeakSetPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void WeakSetPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.add, add, 1, attr);
    define_native_function(vm.names.delete_, delete_, 1, attr);
    define_native_function(vm.names.has, has, 1, attr);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.WeakSet), Attribute::Configurable);
}

WeakSetPrototype::~WeakSetPrototype()
{
}

WeakSet* WeakSetPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<WeakSet>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "WeakSet");
        return nullptr;
    }
    return static_cast<WeakSet*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(WeakSetPrototype::add)
{
    auto* weak_set = typed_this(vm, global_object);
    if (!weak_set)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, value.to_string_without_side_effects());
        return {};
    }
    weak_set->values().set(&value.as_object(), AK::HashSetExistingEntryBehavior::Keep);
    return weak_set;
}

JS_DEFINE_NATIVE_FUNCTION(WeakSetPrototype::delete_)
{
    auto* weak_set = typed_this(vm, global_object);
    if (!weak_set)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object())
        return Value(false);
    return Value(weak_set->values().remove(&value.as_object()));
}

JS_DEFINE_NATIVE_FUNCTION(WeakSetPrototype::has)
{
    auto* weak_set = typed_this(vm, global_object);
    if (!weak_set)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object())
        return Value(false);
    auto& values = weak_set->values();
    return Value(values.find(&value.as_object()) != values.end());
}

}
