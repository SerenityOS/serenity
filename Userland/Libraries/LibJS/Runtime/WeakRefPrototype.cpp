/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/WeakRefPrototype.h>

namespace JS {

WeakRefPrototype::WeakRefPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void WeakRefPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.WeakRef), Attribute::Configurable);
}

WeakRefPrototype::~WeakRefPrototype()
{
}

}
