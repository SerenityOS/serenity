/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/Runtime/SetPrototype.h>

namespace JS {

SetPrototype::SetPrototype(GlobalObject& global_object)
    : Set(*global_object.object_prototype())
{
}

void SetPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Set::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_property(vm.names.size, size_getter, {}, attr);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.Set), Attribute::Configurable);
}

SetPrototype::~SetPrototype()
{
}

JS_DEFINE_NATIVE_GETTER(SetPrototype::size_getter)
{
    auto* set = typed_this(vm, global_object);
    if (!set)
        return {};
    return Value(set->values().size());
}

}
