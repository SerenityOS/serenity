/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FinalizationRegistryPrototype.h>

namespace JS {

FinalizationRegistryPrototype::FinalizationRegistryPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void FinalizationRegistryPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // 26.2.3.4 FinalizationRegistry.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-finalization-registry.prototype-@@tostringtag
    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.FinalizationRegistry.as_string()), Attribute::Configurable);
}

FinalizationRegistryPrototype::~FinalizationRegistryPrototype()
{
}

FinalizationRegistry* FinalizationRegistryPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!is<FinalizationRegistry>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "FinalizationRegistry");
        return nullptr;
    }
    return static_cast<FinalizationRegistry*>(this_object);
}

}
