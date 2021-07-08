/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/TimeZonePrototype.h>

namespace JS::Temporal {

// 11.4 Properties of the Temporal.TimeZone Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-timezone-prototype-object
TimeZonePrototype::TimeZonePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void TimeZonePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_accessor(vm.names.id, id_getter, {}, Attribute::Configurable);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);

    // 11.4.2 Temporal.TimeZone.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.TimeZone"), Attribute::Configurable);
}

static TimeZone* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<TimeZone>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.TimeZone");
        return {};
    }
    return static_cast<TimeZone*>(this_object);
}

// 11.4.3 get Temporal.TimeZone.prototype.id, https://tc39.es/proposal-temporal/#sec-get-temporal.timezone.prototype.id
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::id_getter)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Return ? ToString(timeZone).
    return js_string(vm, time_zone.to_string(global_object));
}

// 11.4.11 Temporal.TimeZone.prototype.toString ( ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::to_string)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return timeZone.[[Identifier]].
    return js_string(vm, time_zone->identifier());
}

// 11.4.12 Temporal.TimeZone.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::to_json)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Return ? ToString(timeZone).
    return js_string(vm, time_zone.to_string(global_object));
}

}
