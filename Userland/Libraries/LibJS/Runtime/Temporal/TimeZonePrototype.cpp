/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/TimeZonePrototype.h>

namespace JS::Temporal {

// 11.4 Properties of the Temporal.TimeZone Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-timezone-prototype-object
TimeZonePrototype::TimeZonePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
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
    define_native_function(vm.names.getInstantFor, get_instant_for, 1, attr);
    define_native_function(vm.names.getPossibleInstantsFor, get_possible_instants_for, 1, attr);
    define_native_function(vm.names.getNextTransition, get_next_transition, 1, attr);
    define_native_function(vm.names.getPreviousTransition, get_previous_transition, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);

    // 11.4.2 Temporal.TimeZone.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.TimeZone"), Attribute::Configurable);
}

// 11.4.3 get Temporal.TimeZone.prototype.id, https://tc39.es/proposal-temporal/#sec-get-temporal.timezone.prototype.id
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::id_getter)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Return ? ToString(timeZone).
    return js_string(vm, TRY(time_zone.to_string(global_object)));
}

// 11.4.4 Temporal.TimeZone.prototype.getOffsetNanosecondsFor ( instant ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getoffsetnanosecondsfor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_offset_nanoseconds_for)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set instant to ? ToTemporalInstant(instant).
    auto* instant = TRY(to_temporal_instant(global_object, vm.argument(0)));

    // 4. If timeZone.[[OffsetNanoseconds]] is not undefined, return 𝔽(timeZone.[[OffsetNanoseconds]]).
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
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set instant to ? ToTemporalInstant(instant).
    auto* instant = TRY(to_temporal_instant(global_object, vm.argument(0)));

    // 4. Return ? BuiltinTimeZoneGetOffsetStringFor(timeZone, instant).
    auto offset_string = TRY(builtin_time_zone_get_offset_string_for(global_object, time_zone, *instant));
    return js_string(vm, move(offset_string));
}

// 11.4.6 Temporal.TimeZone.prototype.getPlainDateTimeFor ( instant [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getplaindatetimefor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_plain_date_time_for)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Set instant to ? ToTemporalInstant(instant).
    auto* instant = TRY(to_temporal_instant(global_object, vm.argument(0)));

    // 3. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, vm.argument(1)));

    // 4. Return ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    return TRY(builtin_time_zone_get_plain_date_time_for(global_object, time_zone, *instant, *calendar));
}

// 11.4.7 Temporal.TimeZone.prototype.getInstantFor ( dateTime [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getinstantfor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_instant_for)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set dateTime to ? ToTemporalDateTime(dateTime).
    auto* date_time = TRY(to_temporal_date_time(global_object, vm.argument(0)));

    // 4. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 5. Let disambiguation be ? ToTemporalDisambiguation(options).
    auto disambiguation = TRY(to_temporal_disambiguation(global_object, *options));

    // 6. Return ? BuiltinTimeZoneGetInstantFor(timeZone, dateTime, disambiguation).
    return TRY(builtin_time_zone_get_instant_for(global_object, time_zone, *date_time, disambiguation));
}

// 11.4.8 Temporal.TimeZone.prototype.getPossibleInstantsFor ( dateTime ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getpossibleinstantsfor
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_possible_instants_for)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimezone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set dateTime to ? ToTemporalDateTime(dateTime).
    auto* date_time = TRY(to_temporal_date_time(global_object, vm.argument(0)));

    // 4. If timeZone.[[OffsetNanoseconds]] is not undefined, then
    if (time_zone->offset_nanoseconds().has_value()) {
        // a. Let epochNanoseconds be ! GetEpochFromISOParts(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]]).
        auto* epoch_nanoseconds = get_epoch_from_iso_parts(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond());

        // b. Let instant be ! CreateTemporalInstant(ℤ(epochNanoseconds − timeZone.[[OffsetNanoseconds]])).
        auto* instant = MUST(create_temporal_instant(global_object, *js_bigint(vm, epoch_nanoseconds->big_integer().minus(Crypto::SignedBigInteger::create_from(*time_zone->offset_nanoseconds())))));

        // c. Return ! CreateArrayFromList(« instant »).
        return Array::create_from(global_object, { instant });
    }

    // 5. Let possibleEpochNanoseconds be ? GetIANATimeZoneEpochValue(timeZone.[[Identifier]], dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]]).
    auto possible_epoch_nanoseconds = get_iana_time_zone_epoch_value(global_object, time_zone->identifier(), date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond());

    // 6. Let possibleInstants be a new empty List.
    auto possible_instants = MarkedValueList { vm.heap() };

    // 7. For each value epochNanoseconds in possibleEpochNanoseconds, do
    for (auto& epoch_nanoseconds : possible_epoch_nanoseconds) {
        // a. Let instant be ! CreateTemporalInstant(epochNanoseconds).
        auto* instant = MUST(create_temporal_instant(global_object, epoch_nanoseconds.as_bigint()));

        // b. Append instant to possibleInstants.
        possible_instants.append(instant);
    }

    // 8. Return ! CreateArrayFromList(possibleInstants).
    return Array::create_from(global_object, possible_instants);
}

// 11.4.9 Temporal.TimeZone.prototype.getNextTransition ( startingPoint ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getnexttransition
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_next_transition)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set startingPoint to ? ToTemporalInstant(startingPoint).
    auto* starting_point = TRY(to_temporal_instant(global_object, vm.argument(0)));

    // 4. If timeZone.[[OffsetNanoseconds]] is not undefined, return null.
    if (!time_zone->offset_nanoseconds().has_value())
        return js_null();

    // 5. Let transition be ? GetIANATimeZoneNextTransition(startingPoint.[[Nanoseconds]], timeZone.[[Identifier]]).
    auto* transition = get_iana_time_zone_next_transition(global_object, starting_point->nanoseconds(), time_zone->identifier());

    // 6. If transition is null, return null.
    if (!transition)
        return js_null();

    // 7. Return ! CreateTemporalInstant(transition).
    return MUST(create_temporal_instant(global_object, *transition));
}

// 11.4.10 Temporal.TimeZone.prototype.getPreviousTransition ( startingPoint ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.getprevioustransition
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::get_previous_transition)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Set startingPoint to ? ToTemporalInstant(startingPoint).
    auto* starting_point = TRY(to_temporal_instant(global_object, vm.argument(0)));

    // 4. If timeZone.[[OffsetNanoseconds]] is not undefined, return null.
    if (!time_zone->offset_nanoseconds().has_value())
        return js_null();

    // 5. Let transition be ? GetIANATimeZonePreviousTransition(startingPoint.[[Nanoseconds]], timeZone.[[Identifier]]).
    auto* transition = get_iana_time_zone_previous_transition(global_object, starting_point->nanoseconds(), time_zone->identifier());

    // 6. If transition is null, return null.
    if (!transition)
        return js_null();

    // 7. Return ! CreateTemporalInstant(transition).
    return MUST(create_temporal_instant(global_object, *transition));
}

// 11.4.11 Temporal.TimeZone.prototype.toString ( ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::to_string)
{
    // 1. Let timeZone be the this value.
    // 2. Perform ? RequireInternalSlot(timeZone, [[InitializedTemporalTimeZone]]).
    auto* time_zone = TRY(typed_this_object(global_object));

    // 3. Return timeZone.[[Identifier]].
    return js_string(vm, time_zone->identifier());
}

// 11.4.12 Temporal.TimeZone.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(TimeZonePrototype::to_json)
{
    // 1. Let timeZone be the this value.
    auto time_zone = vm.this_value(global_object);

    // 2. Return ? ToString(timeZone).
    return js_string(vm, TRY(time_zone.to_string(global_object)));
}

}
