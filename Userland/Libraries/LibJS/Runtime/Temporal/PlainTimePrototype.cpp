/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainTimePrototype.h>

namespace JS::Temporal {

// 4.3 Properties of the Temporal.PlainTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaintime-prototype-object
PlainTimePrototype::PlainTimePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 4.3.2 Temporal.PlainTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainTime"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 4.3.23 Temporal.PlainTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainTime", "a primitive value");
    return {};
}

}
