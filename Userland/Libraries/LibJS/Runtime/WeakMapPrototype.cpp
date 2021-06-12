/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/Runtime/WeakMapPrototype.h>

namespace JS {

WeakMapPrototype::WeakMapPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void WeakMapPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.WeakMap), Attribute::Configurable);
}

WeakMapPrototype::~WeakMapPrototype()
{
}

WeakMap* WeakMapPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<WeakMap>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "WeakMap");
        return nullptr;
    }
    return static_cast<WeakMap*>(this_object);
}

}
