/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 8 Temporal.Instant Objects, https://tc39.es/proposal-temporal/#sec-temporal-instant-objects
Instant::Instant(BigInt const& nanoseconds, Object& prototype)
    : Object(prototype)
    , m_nanoseconds(nanoseconds)
{
}

void Instant::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_nanoseconds);
}

// 8.5.1 IsValidEpochNanoseconds ( epochNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidepochnanoseconds
bool is_valid_epoch_nanoseconds(BigInt const& epoch_nanoseconds)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.

    // 2. If epochNanoseconds < −86400ℤ × 10^17ℤ or epochNanoseconds > 86400ℤ × 10^17ℤ, then
    if (epoch_nanoseconds.big_integer() < INSTANT_NANOSECONDS_MIN || epoch_nanoseconds.big_integer() > INSTANT_NANOSECONDS_MAX) {
        // a. Return false.
        return false;
    }

    // 3. Return true.
    return true;
}

// 8.5.2 CreateTemporalInstant ( epochNanoseconds [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalinstant
ThrowCompletionOr<Instant*> create_temporal_instant(GlobalObject& global_object, BigInt const& epoch_nanoseconds, FunctionObject const* new_target)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 3. If newTarget is not present, set it to %Temporal.Instant%.
    if (!new_target)
        new_target = global_object.temporal_instant_constructor();

    // 4. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Instant.prototype%", « [[InitializedTemporalInstant]], [[Nanoseconds]] »).
    // 5. Set object.[[Nanoseconds]] to epochNanoseconds.
    auto* object = TRY(ordinary_create_from_constructor<Instant>(global_object, *new_target, &GlobalObject::temporal_instant_prototype, epoch_nanoseconds));

    // 6. Return object.
    return object;
}

// 8.5.3 ToTemporalInstant ( item ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalinstant
ThrowCompletionOr<Instant*> to_temporal_instant(GlobalObject& global_object, Value item)
{
    // 1. If Type(item) is Object, then
    if (item.is_object()) {
        // a. If item has an [[InitializedTemporalInstant]] internal slot, then
        if (is<Instant>(item.as_object())) {
            // i. Return item.
            return &static_cast<Instant&>(item.as_object());
        }

        // b. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(item.as_object())) {
            auto& zoned_date_time = static_cast<ZonedDateTime&>(item.as_object());

            // i. Return ! CreateTemporalInstant(item.[[Nanoseconds]]).
            return create_temporal_instant(global_object, zoned_date_time.nanoseconds());
        }
    }

    // 2. Let string be ? ToString(item).
    auto string = TRY(item.to_string(global_object));

    // 3. Let epochNanoseconds be ? ParseTemporalInstant(string).
    auto* epoch_nanoseconds = TRY(parse_temporal_instant(global_object, string));

    // 4. Return ! CreateTemporalInstant(ℤ(epochNanoseconds)).
    return create_temporal_instant(global_object, *epoch_nanoseconds);
}

// 8.5.4 ParseTemporalInstant ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalinstant
ThrowCompletionOr<BigInt*> parse_temporal_instant(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let result be ? ParseTemporalInstantString(isoString).
    auto result = TRY(parse_temporal_instant_string(global_object, iso_string));

    // 3. Let offsetString be result.[[TimeZoneOffsetString]].
    auto& offset_string = result.time_zone_offset;

    // 4. Assert: offsetString is not undefined.
    VERIFY(offset_string.has_value());

    // 5. Let utc be ! GetEpochFromISOParts(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    auto* utc = get_epoch_from_iso_parts(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond);

    // 6. If utc < −8.64 × 10^21 or utc > 8.64 × 10^21, then
    if (utc->big_integer() < INSTANT_NANOSECONDS_MIN || utc->big_integer() > INSTANT_NANOSECONDS_MAX) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
    }

    // 7. Let offsetNanoseconds be ? ParseTimeZoneOffsetString(offsetString).
    auto offset_nanoseconds = TRY(parse_time_zone_offset_string(global_object, *offset_string));

    // 8. Return utc − offsetNanoseconds.
    return js_bigint(vm, utc->big_integer().minus(Crypto::SignedBigInteger::create_from(offset_nanoseconds)));
}

// 8.5.5 CompareEpochNanoseconds ( epochNanosecondsOne, epochNanosecondsTwo ), https://tc39.es/proposal-temporal/#sec-temporal-compareepochnanoseconds
i32 compare_epoch_nanoseconds(BigInt const& epoch_nanoseconds_one, BigInt const& epoch_nanoseconds_two)
{
    // 1. If epochNanosecondsOne > epochNanosecondsTwo, return 1.
    if (epoch_nanoseconds_one.big_integer() > epoch_nanoseconds_two.big_integer())
        return 1;

    // 2. If epochNanosecondsOne < epochNanosecondsTwo, return -1.
    if (epoch_nanoseconds_one.big_integer() < epoch_nanoseconds_two.big_integer())
        return -1;

    // 3. Return 0.
    return 0;
}

// 8.5.6 AddInstant ( epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-addinstant
ThrowCompletionOr<BigInt*> add_instant(GlobalObject& global_object, BigInt const& epoch_nanoseconds, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds)
{
    auto& vm = global_object.vm();

    // 1. Assert: hours, minutes, seconds, milliseconds, microseconds, and nanoseconds are integer Number values.
    VERIFY(hours == trunc(hours) && minutes == trunc(minutes) && seconds == trunc(seconds) && milliseconds == trunc(milliseconds) && microseconds == trunc(microseconds) && nanoseconds == trunc(nanoseconds));

    // 2. Let result be epochNanoseconds + ℤ(nanoseconds) + ℤ(microseconds) × 1000ℤ + ℤ(milliseconds) × 10^6ℤ + ℤ(seconds) × 10^9ℤ + ℤ(minutes) × 60ℤ × 10^9ℤ + ℤ(hours) × 3600ℤ × 10^9ℤ.
    // FIXME: Pretty sure i64's are not sufficient for the extreme cases.
    auto* result = js_bigint(vm,
        epoch_nanoseconds.big_integer()
            .plus(Crypto::SignedBigInteger::create_from((i64)nanoseconds))
            .plus(Crypto::SignedBigInteger::create_from((i64)microseconds).multiplied_by(Crypto::SignedBigInteger { 1'000 }))
            .plus(Crypto::SignedBigInteger::create_from((i64)milliseconds).multiplied_by(Crypto::SignedBigInteger { 1'000'000 }))
            .plus(Crypto::SignedBigInteger::create_from((i64)seconds).multiplied_by(Crypto::SignedBigInteger { 1'000'000'000 }))
            .plus(Crypto::SignedBigInteger::create_from((i64)minutes).multiplied_by(Crypto::SignedBigInteger { 60 }).multiplied_by(Crypto::SignedBigInteger { 1'000'000'000 }))
            .plus(Crypto::SignedBigInteger::create_from((i64)hours).multiplied_by(Crypto::SignedBigInteger { 3600 }).multiplied_by(Crypto::SignedBigInteger { 1'000'000'000 })));

    // If ! IsValidEpochNanoseconds(result) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*result))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);

    // 4. Return result.
    return result;
}

// 8.5.7 DifferenceInstant ( ns1, ns2, roundingIncrement, smallestUnit, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-differenceinstant
BigInt* difference_instant(GlobalObject& global_object, BigInt const& nanoseconds1, BigInt const& nanoseconds2, u64 rounding_increment, StringView smallest_unit, StringView rounding_mode)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(ns1) is BigInt.
    // 2. Assert: Type(ns2) is BigInt.

    // 3. Return ! RoundTemporalInstant(ns2 − ns1, roundingIncrement, smallestUnit, roundingMode).
    return round_temporal_instant(global_object, *js_bigint(vm, nanoseconds2.big_integer().minus(nanoseconds1.big_integer())), rounding_increment, smallest_unit, rounding_mode);
}

// 8.5.8 RoundTemporalInstant ( ns, increment, unit, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundtemporalinstant
BigInt* round_temporal_instant(GlobalObject& global_object, BigInt const& nanoseconds, u64 increment, StringView unit, StringView rounding_mode)
{
    // 1. Assert: Type(ns) is BigInt.

    u64 increment_nanoseconds;
    // 2. If unit is "hour", then
    if (unit == "hour"sv) {
        // a. Let incrementNs be increment × 3.6 × 10^12.
        increment_nanoseconds = increment * 3600000000000;
    }
    // 3. Else if unit is "minute", then
    else if (unit == "minute"sv) {
        // a. Let incrementNs be increment × 6 × 10^10.
        increment_nanoseconds = increment * 60000000000;
    }
    // 4. Else if unit is "second", then
    else if (unit == "second"sv) {
        // a. Let incrementNs be increment × 10^9.
        increment_nanoseconds = increment * 1000000000;
    }
    // 5. Else if unit is "millisecond", then
    else if (unit == "millisecond"sv) {
        // a. Let incrementNs be increment × 10^6.
        increment_nanoseconds = increment * 1000000;
    }
    // 6. Else if unit is "microsecond", then
    else if (unit == "microsecond"sv) {
        // a. Let incrementNs be increment × 10^3.
        increment_nanoseconds = increment * 1000;
    }
    // 7. Else,
    else {
        // a. Assert: unit is "nanosecond".
        VERIFY(unit == "nanosecond"sv);

        // b. Let incrementNs be increment.
        increment_nanoseconds = increment;
    }

    // 8. Return ! RoundNumberToIncrement(ℝ(ns), incrementNs, roundingMode).
    return round_number_to_increment(global_object, nanoseconds, increment_nanoseconds, rounding_mode);
}

// 8.5.9 TemporalInstantToString ( instant, timeZone, precision ), https://tc39.es/proposal-temporal/#sec-temporal-temporalinstanttostring
ThrowCompletionOr<String> temporal_instant_to_string(GlobalObject& global_object, Instant& instant, Value time_zone, Variant<StringView, u8> const& precision)
{
    // 1. Assert: Type(instant) is Object.
    // 2. Assert: instant has an [[InitializedTemporalInstant]] internal slot.

    // 3. Let outputTimeZone be timeZone.
    auto output_time_zone = time_zone;

    // 4. If outputTimeZone is undefined, then
    if (output_time_zone.is_undefined()) {
        // a. Set outputTimeZone to ! CreateTemporalTimeZone("UTC").
        output_time_zone = MUST(create_temporal_time_zone(global_object, "UTC"sv));
    }

    // 5. Let isoCalendar be ! GetISO8601Calendar().
    auto* iso_calendar = get_iso8601_calendar(global_object);

    // 6. Let dateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(outputTimeZone, instant, isoCalendar).
    auto* date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, output_time_zone, instant, *iso_calendar));

    // 7. Let dateTimeString be ? TemporalDateTimeToString(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], undefined, precision, "never").
    auto date_time_string = TRY(temporal_date_time_to_string(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond(), js_undefined(), precision, "never"sv));

    String time_zone_string;

    // 8. If timeZone is undefined, then
    if (time_zone.is_undefined()) {
        // a. Let timeZoneString be "Z".
        time_zone_string = "Z"sv;
    }
    // 9. Else,
    else {
        // a. Let offsetNs be ? GetOffsetNanosecondsFor(timeZone, instant).
        auto offset_ns = TRY(get_offset_nanoseconds_for(global_object, time_zone, instant));

        // b. Let timeZoneString be ! FormatISOTimeZoneOffsetString(offsetNs).
        time_zone_string = format_iso_time_zone_offset_string(offset_ns);
    }

    // 10. Return the string-concatenation of dateTimeString and timeZoneString.
    return String::formatted("{}{}", date_time_string, time_zone_string);
}

}
