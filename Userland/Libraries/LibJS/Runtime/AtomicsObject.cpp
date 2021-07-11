/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AtomicsObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AtomicsObject::AtomicsObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void AtomicsObject::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 25.4.15 Atomics [ @@toStringTag ], https://tc39.es/ecma262/#sec-atomics-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Atomics"), Attribute::Configurable);
}

}
