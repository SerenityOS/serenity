/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
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

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

static PlainDateTime* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainDateTime>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainDateTime");
        return {};
    }
    return static_cast<PlainDateTime*>(this_object);
}

// 5.3.3 get Temporal.PlainDateTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::calendar_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return dateTime.[[Calendar]].
    return Value(&date_time->calendar());
}

// 5.3.35 Temporal.PlainDateTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDateTime", "a primitive value");
    return {};
}

}
