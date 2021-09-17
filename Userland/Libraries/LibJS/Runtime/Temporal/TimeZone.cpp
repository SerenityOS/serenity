/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateTimeLexer.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/TimeZoneConstructor.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 11 Temporal.TimeZone Objects, https://tc39.es/proposal-temporal/#sec-temporal-timezone-objects
TimeZone::TimeZone(String identifier, Object& prototype)
    : Object(prototype)
    , m_identifier(move(identifier))
{
}

// 11.1.1 IsValidTimeZoneName ( timeZone ), https://tc39.es/proposal-temporal/#sec-isvalidtimezonename
// NOTE: This is the minimum implementation of IsValidTimeZoneName, supporting only the "UTC" time zone.
bool is_valid_time_zone_name(String const& time_zone)
{
    // 1. Assert: Type(timeZone) is String.

    // 2. Let tzText be ! StringToCodePoints(timeZone).
    // 3. Let tzUpperText be the result of toUppercase(tzText), according to the Unicode Default Case Conversion algorithm.
    // 4. Let tzUpper be ! CodePointsToString(tzUpperText).
    auto tz_upper = time_zone.to_uppercase();

    // 5. If tzUpper and "UTC" are the same sequence of code points, return true.
    if (tz_upper == "UTC")
        return true;

    // 6. Return false.
    return false;
}

// 11.1.2 CanonicalizeTimeZoneName ( timeZone ), https://tc39.es/proposal-temporal/#sec-canonicalizetimezonename
// NOTE: This is the minimum implementation of CanonicalizeTimeZoneName, supporting only the "UTC" time zone.
String canonicalize_time_zone_name(String const& time_zone)
{
    // 1. Assert: Type(timeZone) is String.

    // 2. Assert: ! IsValidTimeZoneName(timeZone) is true.
    VERIFY(is_valid_time_zone_name(time_zone));

    // 3. Return "UTC".
    return "UTC";
}

// 11.1.3 DefaultTimeZone ( ), https://tc39.es/proposal-temporal/#sec-defaulttimezone
// NOTE: This is the minimum implementation of DefaultTimeZone, supporting only the "UTC" time zone.
String default_time_zone()
{
    // 1. Return "UTC".
    return "UTC";
}

// 11.6.1 ParseTemporalTimeZone ( string ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimezone
ThrowCompletionOr<String> parse_temporal_time_zone(GlobalObject& global_object, String const& string)
{
    // 1. Assert: Type(string) is String.

    // 2. Let result be ? ParseTemporalTimeZoneString(string).
    auto result = TRY(parse_temporal_time_zone_string(global_object, string));

    // 3. If result.[[Z]] is not undefined, return "UTC".
    if (result.z)
        return String { "UTC" };

    // 4. Return result.[[Name]].
    return *result.name;
}

// 11.6.2 CreateTemporalTimeZone ( identifier [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaltimezone
ThrowCompletionOr<TimeZone*> create_temporal_time_zone(GlobalObject& global_object, String const& identifier, FunctionObject const* new_target)
{
    // 1. If newTarget is not present, set it to %Temporal.TimeZone%.
    if (!new_target)
        new_target = global_object.temporal_time_zone_constructor();

    // 2. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.TimeZone.prototype%", « [[InitializedTemporalTimeZone]], [[Identifier]], [[OffsetNanoseconds]] »).
    // 3. Set object.[[Identifier]] to identifier.
    auto* object = TRY(ordinary_create_from_constructor<TimeZone>(global_object, *new_target, &GlobalObject::temporal_time_zone_prototype, identifier));

    // 4. If identifier satisfies the syntax of a TimeZoneNumericUTCOffset (see 13.33), then
    if (is_valid_time_zone_numeric_utc_offset_syntax(identifier)) {
        // a. Set object.[[OffsetNanoseconds]] to ! ParseTimeZoneOffsetString(identifier).
        object->set_offset_nanoseconds(TRY(parse_time_zone_offset_string(global_object, identifier)));
    }
    // 5. Else,
    else {
        // a. Assert: ! CanonicalizeTimeZoneName(identifier) is identifier.
        VERIFY(canonicalize_time_zone_name(identifier) == identifier);

        // b. Set object.[[OffsetNanoseconds]] to undefined.
        // NOTE: No-op.
    }

    // 6. Return object.
    return object;
}

// 11.6.3 GetISOPartsFromEpoch ( epochNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-getisopartsfromepoch
ISODateTime get_iso_parts_from_epoch(BigInt const& epoch_nanoseconds)
{
    // 1. Let remainderNs be remainder(epochNanoseconds, 10^6).
    auto remainder_ns_bigint = epoch_nanoseconds.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000 }).remainder;
    auto remainder_ns = remainder_ns_bigint.to_base(10).to_int<i64>().value();

    // 2. Let epochMilliseconds be (epochNanoseconds − remainderNs) / 10^6.
    auto epoch_milliseconds_bigint = epoch_nanoseconds.big_integer().minus(remainder_ns_bigint).divided_by(Crypto::UnsignedBigInteger { 1'000'000 }).quotient;
    auto epoch_milliseconds = (double)epoch_milliseconds_bigint.to_base(10).to_int<i64>().value();

    // 3. Let year be ! YearFromTime(epochMilliseconds).
    auto year = year_from_time(epoch_milliseconds);

    // 4. Let month be ! MonthFromTime(epochMilliseconds) + 1.
    auto month = static_cast<u8>(month_from_time(epoch_milliseconds) + 1);

    // 5. Let day be ! DateFromTime(epochMilliseconds).
    auto day = date_from_time(epoch_milliseconds);

    // 6. Let hour be ! HourFromTime(epochMilliseconds).
    auto hour = hour_from_time(epoch_milliseconds);

    // 7. Let minute be ! MinFromTime(epochMilliseconds).
    auto minute = min_from_time(epoch_milliseconds);

    // 8. Let second be ! SecFromTime(epochMilliseconds).
    auto second = sec_from_time(epoch_milliseconds);

    // 9. Let millisecond be ! msFromTime(epochMilliseconds).
    auto millisecond = ms_from_time(epoch_milliseconds);

    // 10. Let microsecond be floor(remainderNs / 1000) modulo 1000.
    auto microsecond = static_cast<u16>((remainder_ns / 1000) % 1000);

    // 11. Let nanosecond be remainderNs modulo 1000.
    auto nanosecond = static_cast<u16>(remainder_ns % 1000);

    // 12. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
    return { .year = year, .month = month, .day = day, .hour = hour, .minute = minute, .second = second, .millisecond = millisecond, .microsecond = microsecond, .nanosecond = nanosecond };
}

// 11.6.5 GetIANATimeZoneOffsetNanoseconds ( epochNanoseconds, timeZoneIdentifier ), https://tc39.es/proposal-temporal/#sec-temporal-getianatimezoneoffsetnanoseconds
i64 get_iana_time_zone_offset_nanoseconds([[maybe_unused]] BigInt const& epoch_nanoseconds, [[maybe_unused]] String const& time_zone_identifier)
{
    // The abstract operation GetIANATimeZoneOffsetNanoseconds is an implementation-defined algorithm that returns an integer representing the offset of the IANA time zone identified by timeZoneIdentifier from UTC, at the instant corresponding to epochNanoseconds.
    // Given the same values of epochNanoseconds and timeZoneIdentifier, the result must be the same for the lifetime of the surrounding agent.
    // TODO: Implement this
    return 0;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNumericUTCOffset
static bool parse_time_zone_numeric_utc_offset_syntax(String const& offset_string, StringView& sign, StringView& hours, Optional<StringView>& minutes, Optional<StringView>& seconds, Optional<StringView>& fraction)
{
    DateTimeLexer lexer(offset_string);
    auto sign_part = lexer.consume_sign();
    if (!sign_part.has_value())
        return false;
    sign = *sign_part;
    auto hours_part = lexer.consume_hours();
    if (!hours_part.has_value())
        return false;
    hours = *hours_part;
    if (!lexer.tell_remaining())
        return true;
    auto uses_separator = lexer.consume_specific(':');
    minutes = lexer.consume_minutes_or_seconds();
    if (!minutes.has_value())
        return false;
    if (!lexer.tell_remaining())
        return true;
    if (lexer.consume_specific(':') != uses_separator)
        return false;
    seconds = lexer.consume_minutes_or_seconds();
    if (!seconds.has_value())
        return false;
    if (!lexer.tell_remaining())
        return true;
    if (!lexer.consume_specific('.') && !lexer.consume_specific(','))
        return false;
    fraction = lexer.consume_fractional_seconds();
    return fraction.has_value();
}

bool is_valid_time_zone_numeric_utc_offset_syntax(String const& offset_string)
{
    StringView discarded;
    Optional<StringView> optionally_discarded;
    // FIXME: This is very wasteful
    return parse_time_zone_numeric_utc_offset_syntax(offset_string, discarded, discarded, optionally_discarded, optionally_discarded, optionally_discarded);
}

// 11.6.8 ParseTimeZoneOffsetString ( offsetString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetimezoneoffsetstring
ThrowCompletionOr<double> parse_time_zone_offset_string(GlobalObject& global_object, String const& offset_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(offsetString) is String.

    // 2. If offsetString does not satisfy the syntax of a TimeZoneNumericUTCOffset (see 13.33), then
    // a. Throw a RangeError exception.
    // 3. Let sign, hours, minutes, seconds, and fraction be the parts of offsetString produced respectively by the TimeZoneUTCOffsetSign, TimeZoneUTCOffsetHour, TimeZoneUTCOffsetMinute, TimeZoneUTCOffsetSecond, and TimeZoneUTCOffsetFraction productions, or undefined if not present.
    StringView sign_part;
    StringView hours_part;
    Optional<StringView> minutes_part;
    Optional<StringView> seconds_part;
    Optional<StringView> fraction_part;
    auto success = parse_time_zone_numeric_utc_offset_syntax(offset_string, sign_part, hours_part, minutes_part, seconds_part, fraction_part);
    if (!success)
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidFormat, "TimeZone offset");

    // 4. If either hours or sign are undefined, throw a RangeError exception.
    // NOTE: Both of these checks are always false, due to the handling of Step 2

    double sign;
    // 5. If sign is the code unit 0x002D (HYPHEN-MINUS) or 0x2212 (MINUS SIGN), then
    if (sign_part.is_one_of("-", "\xE2\x88\x92")) {
        // a. Set sign to −1.
        sign = -1;
    }
    // 6. Else,
    else {
        // a. Set sign to 1.
        sign = 1;
    }

    // 7. Set hours to ! ToIntegerOrInfinity(hours).
    auto hours = Value(js_string(vm, hours_part)).to_integer_or_infinity(global_object);
    // 8. Set minutes to ! ToIntegerOrInfinity(minutes).
    auto minutes = Value(js_string(vm, minutes_part.value_or(""sv))).to_integer_or_infinity(global_object);
    // 9. Set seconds to ! ToIntegerOrInfinity(seconds).
    auto seconds = Value(js_string(vm, seconds_part.value_or(""sv))).to_integer_or_infinity(global_object);

    double nanoseconds;
    // 10. If fraction is not undefined, then
    if (fraction_part.has_value()) {
        // a. Set fraction to the string-concatenation of the previous value of fraction and the string "000000000".
        auto fraction = String::formatted("{}000000000", *fraction_part);
        // b. Let nanoseconds be the String value equal to the substring of fraction consisting of the code units with indices 0 (inclusive) through 9 (exclusive).
        // c. Set nanoseconds to ! ToIntegerOrInfinity(nanoseconds).
        nanoseconds = Value(js_string(vm, fraction_part->substring_view(0, 9))).to_integer_or_infinity(global_object);
    }
    // 11. Else,
    else {
        // a. Let nanoseconds be 0.
        nanoseconds = 0;
    }
    // 12. Return sign × (((hours × 60 + minutes) × 60 + seconds) × 10^9 + nanoseconds).
    return sign * (((hours * 60 + minutes) * 60 + seconds) * 1000000000 + nanoseconds);
}

// 11.6.9 FormatTimeZoneOffsetString ( offsetNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-formattimezoneoffsetstring
String format_time_zone_offset_string(double offset_nanoseconds)
{
    auto offset = static_cast<i64>(offset_nanoseconds);

    // 1. Assert: offsetNanoseconds is an integer.
    VERIFY(offset == offset_nanoseconds);

    StringBuilder builder;
    // 2. If offsetNanoseconds ≥ 0, let sign be "+"; otherwise, let sign be "-".
    if (offset >= 0)
        builder.append('+');
    else
        builder.append('-');

    // 3. Let nanoseconds be abs(offsetNanoseconds) modulo 10^9.
    auto nanoseconds = AK::abs(offset) % 1000000000;

    // 4. Let seconds be floor(offsetNanoseconds / 10^9) modulo 60.
    auto seconds = (offset / 1000000000) % 60;
    // 5. Let minutes be floor(offsetNanoseconds / (6 × 10^10)) modulo 60.
    auto minutes = (offset / 60000000000) % 60;
    // 6. Let hours be floor(offsetNanoseconds / (3.6 × 10^12)).
    auto hours = offset / 3600000000000;

    // 7. Let h be hours, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    builder.appendff("{:02}", hours);
    // 8. Let m be minutes, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    builder.appendff(":{:02}", minutes);
    // 9. Let s be seconds, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // Handled by steps 10 & 11

    // 10. If nanoseconds ≠ 0, then
    if (nanoseconds != 0) {
        // a. Let fraction be nanoseconds, formatted as a nine-digit decimal number, padded to the left with zeroes if necessary.
        // b. Set fraction to the longest possible substring of fraction starting at position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
        // c. Let post be the string-concatenation of the code unit 0x003A (COLON), s, the code unit 0x002E (FULL STOP), and fraction.
        builder.appendff(":{:02}.{:9}", seconds, nanoseconds);
    }
    // 11. Else if seconds ≠ 0, then
    else if (seconds != 0) {
        // a. Let post be the string-concatenation of the code unit 0x003A (COLON) and s.
        builder.appendff(":{:02}", seconds);
    }

    // 12. Return the string-concatenation of sign, h, the code unit 0x003A (COLON), m, and post.
    return builder.to_string();
}

// 11.6.10 ToTemporalTimeZone ( temporalTimeZoneLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaltimezone
ThrowCompletionOr<Object*> to_temporal_time_zone(GlobalObject& global_object, Value temporal_time_zone_like)
{
    auto& vm = global_object.vm();

    // 1. If Type(temporalTimeZoneLike) is Object, then
    if (temporal_time_zone_like.is_object()) {
        // a. If temporalTimeZoneLike has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(temporal_time_zone_like.as_object())) {
            auto& zoned_date_time = static_cast<ZonedDateTime&>(temporal_time_zone_like.as_object());

            // i. Return temporalTimeZoneLike.[[TimeZone]].
            return &zoned_date_time.time_zone();
        }

        // b. If ? HasProperty(temporalTimeZoneLike, "timeZone") is false, return temporalTimeZoneLike.
        auto has_property = temporal_time_zone_like.as_object().has_property(vm.names.timeZone);
        if (auto* exception = vm.exception())
            return throw_completion(exception->value());
        if (!has_property)
            return &temporal_time_zone_like.as_object();

        // c. Set temporalTimeZoneLike to ? Get(temporalTimeZoneLike, "timeZone").
        temporal_time_zone_like = temporal_time_zone_like.as_object().get(vm.names.timeZone);
        if (auto* exception = vm.exception())
            return throw_completion(exception->value());

        // d. If Type(temporalTimeZoneLike) is Object and ? HasProperty(temporalTimeZoneLike, "timeZone") is false, return temporalTimeZoneLike.
        if (temporal_time_zone_like.is_object()) {
            has_property = temporal_time_zone_like.as_object().has_property(vm.names.timeZone);
            if (auto* exception = vm.exception())
                return throw_completion(exception->value());
            if (!has_property)
                return &temporal_time_zone_like.as_object();
        }
    }

    // 2. Let identifier be ? ToString(temporalTimeZoneLike).
    auto identifier = temporal_time_zone_like.to_string(global_object);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());

    // 3. Let result be ? ParseTemporalTimeZone(identifier).
    auto result = TRY(parse_temporal_time_zone(global_object, identifier));

    // 4. Return ? CreateTemporalTimeZone(result).
    return TRY(create_temporal_time_zone(global_object, result));
}

// 11.6.11 GetOffsetNanosecondsFor ( timeZone, instant ), https://tc39.es/proposal-temporal/#sec-temporal-getoffsetnanosecondsfor
ThrowCompletionOr<double> get_offset_nanoseconds_for(GlobalObject& global_object, Value time_zone, Instant& instant)
{
    auto& vm = global_object.vm();

    // 1. Let getOffsetNanosecondsFor be ? GetMethod(timeZone, "getOffsetNanosecondsFor").
    auto* get_offset_nanoseconds_for = time_zone.get_method(global_object, vm.names.getOffsetNanosecondsFor);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());

    // 2. If getOffsetNanosecondsFor is undefined, set getOffsetNanosecondsFor to %Temporal.TimeZone.prototype.getOffsetNanosecondsFor%.
    if (!get_offset_nanoseconds_for)
        get_offset_nanoseconds_for = global_object.temporal_time_zone_prototype_get_offset_nanoseconds_for_function();

    // 3. Let offsetNanoseconds be ? Call(getOffsetNanosecondsFor, timeZone, « instant »).
    auto offset_nanoseconds_value = vm.call(*get_offset_nanoseconds_for, time_zone, &instant);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());

    // 4. If Type(offsetNanoseconds) is not Number, throw a TypeError exception.
    if (!offset_nanoseconds_value.is_number())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsNotA, "Offset nanoseconds value", "number");

    // 5. If ! IsIntegralNumber(offsetNanoseconds) is false, throw a RangeError exception.
    if (!offset_nanoseconds_value.is_integral_number())
        return vm.throw_completion<RangeError>(global_object, ErrorType::IsNotAn, "Offset nanoseconds value", "integral number");

    // 6. Set offsetNanoseconds to ℝ(offsetNanoseconds).
    auto offset_nanoseconds = offset_nanoseconds_value.as_double();

    // 7. If abs(offsetNanoseconds) > 86400 × 10^9, throw a RangeError exception.
    if (fabs(offset_nanoseconds) > 86400000000000.0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidOffsetNanosecondsValue);

    // 8. Return offsetNanoseconds.
    return offset_nanoseconds;
}

// 11.6.12 BuiltinTimeZoneGetOffsetStringFor ( timeZone, instant ), https://tc39.es/proposal-temporal/#sec-temporal-builtintimezonegetoffsetstringfor
ThrowCompletionOr<String> builtin_time_zone_get_offset_string_for(GlobalObject& global_object, Value time_zone, Instant& instant)
{
    // 1. Let offsetNanoseconds be ? GetOffsetNanosecondsFor(timeZone, instant).
    auto offset_nanoseconds = TRY(get_offset_nanoseconds_for(global_object, time_zone, instant));

    // 2. Return ! FormatTimeZoneOffsetString(offsetNanoseconds).
    return format_time_zone_offset_string(offset_nanoseconds);
}

// 11.6.13 BuiltinTimeZoneGetPlainDateTimeFor ( timeZone, instant, calendar ), https://tc39.es/proposal-temporal/#sec-temporal-builtintimezonegetplaindatetimefor
ThrowCompletionOr<PlainDateTime*> builtin_time_zone_get_plain_date_time_for(GlobalObject& global_object, Value time_zone, Instant& instant, Object& calendar)
{
    auto& vm = global_object.vm();

    // 1. Let offsetNanoseconds be ? GetOffsetNanosecondsFor(timeZone, instant).
    auto offset_nanoseconds = TRY(get_offset_nanoseconds_for(global_object, time_zone, instant));

    // 2. Let result be ! GetISOPartsFromEpoch(instant.[[Nanoseconds]]).
    auto result = get_iso_parts_from_epoch(instant.nanoseconds());

    // 3. Set result to ! BalanceISODateTime(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]] + offsetNanoseconds).
    result = balance_iso_date_time(result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond + offset_nanoseconds);

    // 4. Return ? CreateTemporalDateTime(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], calendar).
    auto* date_time = create_temporal_date_time(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond, calendar);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());
    return date_time;
}

}
