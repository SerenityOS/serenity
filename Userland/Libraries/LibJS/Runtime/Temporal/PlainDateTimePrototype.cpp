/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDateTimePrototype.h>

namespace JS::Temporal {

// 5.3 Properties of the Temporal.PlainDateTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindatetime-prototype-object
PlainDateTimePrototype::PlainDateTimePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainDateTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 5.3.2 Temporal.PlainDateTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainDateTime"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 5.3.35 Temporal.PlainDateTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDateTime", "a primitive value");
    return {};
}

}
