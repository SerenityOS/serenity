/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormatPrototype.h>

namespace JS::Intl {

// 1.4 Properties of the Intl.DurationFormat Prototype Object, https://tc39.es/proposal-intl-duration-format/#sec-properties-of-intl-durationformat-prototype-object
DurationFormatPrototype::DurationFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DurationFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 1.4.2 Intl.DurationFormat.prototype [ @@toStringTag ], https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DurationFormat"), Attribute::Configurable);
}

}
