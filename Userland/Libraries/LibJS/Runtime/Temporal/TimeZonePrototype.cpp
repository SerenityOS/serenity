/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
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
    define_native_function(vm.names.getOffsetNanosecondsFor, get_offset_nanoseconds_for, 1, attr);
    define_native_function(vm.names.getOffsetStringFor, get_offset_string_for, 1, attr);
    define_native_function(vm.names.getPlainDateTimeFor, get_plain_date_time_for, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);

    // 11.4.2 Temporal.TimeZone.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.TimeZone"), Attribute::Configurable);
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

// 11.4.4 Temporal.TimeZone.prototype.getOffsetNanosecondsFor ( instant ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getoffsetnanosecondsfor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_offset_nanoseconds_for)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set instant to ? ToTemporalInstant(instant).
    auto* instant = to_temporal_instant(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. If timeZone.[[OffsetNanoseconds]] is not undefined, return timeZone.[[OffsetNanoseconds]].
    if (time_zone->offset_nanoseconds().has_value())
        return Value(*time_zone->offset_nanoseconds());

    // 5. Return ! GetIANATimeZoneOffsetNanoseconds(instant.[[Nanoseconds]], timeZone.[[Identifier]]).
    return Value((double)get_iana_time_zone_offset_nanoseconds(instant->nanoseconds(), time_zone->identifier()));
}

// 11.4.5 Temporal.TimeZone.prototype.getOffsetStringFor ( instant ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getoffsetstringfor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_offset_string_for)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set instant to ? ToTemporalInstant(instant).
    auto* instant = to_temporal_instant(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Return ? BuiltinTimeZoneGetOffsetStringFor(timeZone, instant).
    auto offset_string = builtin_time_zone_get_offset_string_for(global_object, time_zone, *instant);
    if (vm.exception())
        return {};
    return js_string(vm, move(*offset_string));
}

// 11.4.6 Temporal.TimeZone.prototype.getPlainDateTimeFor ( instant [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getplaindatetimefor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_plain_date_time_for)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Set instant to ? ToTemporalInstant(instant).
    auto* instant = to_temporal_instant(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 3. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = to_temporal_calendar_with_iso_default(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 4. Return ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    return builtin_time_zone_get_plain_date_time_for(global_object, time_zone, *instant, *calendar);
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
