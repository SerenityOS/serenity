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
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimePrototype.h>

namespace JS::Temporal {

// 6.3 Properties of the Temporal.ZonedDateTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-zoneddatetime-prototype-object
ZonedDateTimePrototype::ZonedDateTimePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ZonedDateTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 6.3.2 Temporal.ZonedDateTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.ZonedDateTime"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.timeZone, time_zone_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
}

static ZonedDateTime* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<ZonedDateTime>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.ZonedDateTime");
        return {};
    }
    return static_cast<ZonedDateTime*>(this_object);
}

// 6.3.3 get Temporal.ZonedDateTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimePrototype::calendar_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return zonedDateTime.[[Calendar]].
    return Value(&zoned_date_time->calendar());
}

// 6.3.4 get Temporal.ZonedDateTime.prototype.timeZone, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.timezone
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimePrototype::time_zone_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return zonedDateTime.[[TimeZone]].
    return Value(&zoned_date_time->time_zone());
}

// 6.3.5 get Temporal.ZonedDateTime.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.year
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimePrototype::year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = create_temporal_instant(global_object, zoned_date_time->nanoseconds());

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar);
    if (vm.exception())
        return {};

    // 7. Return ? CalendarYear(calendar, temporalDateTime).
    return Value(calendar_year(global_object, calendar, *temporal_date_time));
}

// 6.3.6 get Temporal.ZonedDateTime.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.month
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimePrototype::month_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = create_temporal_instant(global_object, zoned_date_time->nanoseconds());

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar);
    if (vm.exception())
        return {};

    // 7. Return ? CalendarMonth(calendar, temporalDateTime).
    return Value(calendar_month(global_object, calendar, *temporal_date_time));
}

}
