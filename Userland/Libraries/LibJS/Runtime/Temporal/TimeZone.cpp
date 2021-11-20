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
#include <LibJS/Runtime/IteratorOperations.h>
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

    // 3. If result.[[Name]] is not undefined, return result.[[Name]].
    if (result.name.has_value())
        return *result.name;

    // 4. If result.[[Z]] is true, return "UTC".
    if (result.z)
        return String { "UTC" };

    // 5. Return result.[[OffsetString]].
    return *result.offset;
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
    // 1. Assert: epochNanoseconds is an integer.

    // 2. Let remainderNs be remainder(epochNanoseconds, 10^6).
    auto remainder_ns_bigint = epoch_nanoseconds.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000 }).remainder;
    auto remainder_ns = remainder_ns_bigint.to_double();

    // 3. Let epochMilliseconds be (epochNanoseconds − remainderNs) / 10^6.
    auto epoch_milliseconds_bigint = epoch_nanoseconds.big_integer().minus(remainder_ns_bigint).divided_by(Crypto::UnsignedBigInteger { 1'000'000 }).quotient;
    auto epoch_milliseconds = epoch_milliseconds_bigint.to_double();

    // 4. Let year be ! YearFromTime(epochMilliseconds).
    auto year = year_from_time(epoch_milliseconds);

    // 5. Let month be ! MonthFromTime(epochMilliseconds) + 1.
    auto month = static_cast<u8>(month_from_time(epoch_milliseconds) + 1);

    // 6. Let day be ! DateFromTime(epochMilliseconds).
    auto day = date_from_time(epoch_milliseconds);

    // 7. Let hour be ! HourFromTime(epochMilliseconds).
    auto hour = hour_from_time(epoch_milliseconds);

    // 8. Let minute be ! MinFromTime(epochMilliseconds).
    auto minute = min_from_time(epoch_milliseconds);

    // 9. Let second be ! SecFromTime(epochMilliseconds).
    auto second = sec_from_time(epoch_milliseconds);

    // 10. Let millisecond be ! msFromTime(epochMilliseconds).
    auto millisecond = ms_from_time(epoch_milliseconds);

    // 11. Let microsecond be floor(remainderNs / 1000) modulo 1000.
    auto microsecond = modulo(floor(remainder_ns / 1000), 1000.0);

    // 12. Let nanosecond be remainderNs modulo 1000.
    auto nanosecond = modulo(remainder_ns, 1000.0);

    // 13. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
    return { .year = year, .month = month, .day = day, .hour = hour, .minute = minute, .second = second, .millisecond = millisecond, .microsecond = static_cast<u16>(microsecond), .nanosecond = static_cast<u16>(nanosecond) };
}

// 11.6.4 GetIANATimeZoneEpochValue ( timeZoneIdentifier, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-getianatimezoneepochvalue
MarkedValueList get_iana_time_zone_epoch_value(GlobalObject& global_object, StringView time_zone_identifier, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    // The abstract operation GetIANATimeZoneEpochValue is an implementation-defined algorithm that returns a List of integers. Each integer in the List represents a number of nanoseconds since the Unix epoch in UTC that may correspond to the given calendar date and wall-clock time in the IANA time zone identified by timeZoneIdentifier.
    // When the input represents a local time repeating multiple times at a negative time zone transition (e.g. when the daylight saving time ends or the time zone offset is decreased due to a time zone rule change), the returned List will have more than one element. When the input represents a skipped local time at a positive time zone transition (e.g. when the daylight saving time starts or the time zone offset is increased due to a time zone rule change), the returned List will be empty. Otherwise, the returned List will have one element.

    VERIFY(time_zone_identifier == "UTC"sv);
    // FIXME: MarkedValueList<T> for T != Value would still be nice.
    auto& vm = global_object.vm();
    auto list = MarkedValueList { vm.heap() };
    list.append(get_epoch_from_iso_parts(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond));
    return list;
}

// 11.6.5 GetIANATimeZoneOffsetNanoseconds ( epochNanoseconds, timeZoneIdentifier ), https://tc39.es/proposal-temporal/#sec-temporal-getianatimezoneoffsetnanoseconds
i64 get_iana_time_zone_offset_nanoseconds([[maybe_unused]] BigInt const& epoch_nanoseconds, [[maybe_unused]] String const& time_zone_identifier)
{
    // The abstract operation GetIANATimeZoneOffsetNanoseconds is an implementation-defined algorithm that returns an integer representing the offset of the IANA time zone identified by timeZoneIdentifier from UTC, at the instant corresponding to epochNanoseconds.
    // Given the same values of epochNanoseconds and timeZoneIdentifier, the result must be the same for the lifetime of the surrounding agent.
    // TODO: Implement this
    return 0;
}

// 11.6.6 GetIANATimeZoneNextTransition ( epochNanoseconds, timeZoneIdentifier ), https://tc39.es/proposal-temporal/#sec-temporal-getianatimezonenexttransition
BigInt* get_iana_time_zone_next_transition(GlobalObject&, [[maybe_unused]] BigInt const& epoch_nanoseconds, [[maybe_unused]] StringView time_zone_identifier)
{
    // The abstract operation GetIANATimeZoneNextTransition is an implementation-defined algorithm that returns an integer representing the number of nanoseconds since the Unix epoch in UTC that corresponds to the first time zone transition after epochNanoseconds in the IANA time zone identified by timeZoneIdentifier or null if no such transition exists.

    // TODO: Implement this
    return nullptr;
}

// 11.6.7 GetIANATimeZonePreviousTransition ( epochNanoseconds, timeZoneIdentifier ), https://tc39.es/proposal-temporal/#sec-temporal-getianatimezoneprevioustransition
BigInt* get_iana_time_zone_previous_transition(GlobalObject&, [[maybe_unused]] BigInt const& epoch_nanoseconds, [[maybe_unused]] StringView time_zone_identifier)
{
    // The abstract operation GetIANATimeZonePreviousTransition is an implementation-defined algorithm that returns an integer representing the number of nanoseconds since the Unix epoch in UTC that corresponds to the last time zone transition before epochNanoseconds in the IANA time zone identified by timeZoneIdentifier or null if no such transition exists.

    // TODO: Implement this
    return nullptr;
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
    auto hours = MUST(Value(js_string(vm, hours_part)).to_integer_or_infinity(global_object));
    // 8. Set minutes to ! ToIntegerOrInfinity(minutes).
    auto minutes = MUST(Value(js_string(vm, minutes_part.value_or(""sv))).to_integer_or_infinity(global_object));
    // 9. Set seconds to ! ToIntegerOrInfinity(seconds).
    auto seconds = MUST(Value(js_string(vm, seconds_part.value_or(""sv))).to_integer_or_infinity(global_object));

    double nanoseconds;
    // 10. If fraction is not undefined, then
    if (fraction_part.has_value()) {
        // a. Set fraction to the string-concatenation of the previous value of fraction and the string "000000000".
        auto fraction = String::formatted("{}000000000", *fraction_part);
        // b. Let nanoseconds be the String value equal to the substring of fraction consisting of the code units with indices 0 (inclusive) through 9 (exclusive).
        // c. Set nanoseconds to ! ToIntegerOrInfinity(nanoseconds).
        nanoseconds = MUST(Value(js_string(vm, fraction.substring_view(0, 9))).to_integer_or_infinity(global_object));
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

    // 3. Let _offsetNanoseconds_ be abs(_offsetNanoseconds_).
    offset = AK::abs(offset);

    // 4. Let nanoseconds be offsetNanoseconds modulo 10^9.
    auto nanoseconds = offset % 1000000000;

    // 5. Let seconds be floor(offsetNanoseconds / 10^9) modulo 60.
    auto seconds = (offset / 1000000000) % 60;
    // 6. Let minutes be floor(offsetNanoseconds / (6 × 10^10)) modulo 60.
    auto minutes = (offset / 60000000000) % 60;
    // 7. Let hours be floor(offsetNanoseconds / (3.6 × 10^12)).
    auto hours = offset / 3600000000000;

    // 8. Let h be hours, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    builder.appendff("{:02}", hours);
    // 9. Let m be minutes, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    builder.appendff(":{:02}", minutes);
    // 10. Let s be seconds, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // Handled by steps 10 & 11

    // 11. If nanoseconds ≠ 0, then
    if (nanoseconds != 0) {
        // a. Let fraction be nanoseconds, formatted as a nine-digit decimal number, padded to the left with zeroes if necessary.
        // b. Set fraction to the longest possible substring of fraction starting at position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
        // c. Let post be the string-concatenation of the code unit 0x003A (COLON), s, the code unit 0x002E (FULL STOP), and fraction.
        builder.appendff(":{:02}.{}", seconds, String::formatted("{:09}", nanoseconds).trim("0"sv, TrimMode::Right));
    }
    // 12. Else if seconds ≠ 0, then
    else if (seconds != 0) {
        // a. Let post be the string-concatenation of the code unit 0x003A (COLON) and s.
        builder.appendff(":{:02}", seconds);
    }
    // 13. Else,
    //    a. Let post be the empty String.

    // 14. Return the string-concatenation of sign, h, the code unit 0x003A (COLON), m, and post.
    return builder.to_string();
}

// 11.6.10 FormatISOTimeZoneOffsetString ( offsetNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-formatisotimezoneoffsetstring
String format_iso_time_zone_offset_string(double offset_nanoseconds)
{
    // 1. Assert: offsetNanoseconds is an integer.
    VERIFY(trunc(offset_nanoseconds) == offset_nanoseconds);

    // 2. Set offsetNanoseconds to ! RoundNumberToIncrement(offsetNanoseconds, 60 × 10^9, "halfExpand").
    offset_nanoseconds = round_number_to_increment(offset_nanoseconds, 60000000000, "halfExpand"sv);

    // 3. If offsetNanoseconds ≥ 0, let sign be "+"; otherwise, let sign be "-".
    auto sign = offset_nanoseconds >= 0 ? "+"sv : "-"sv;

    // 4. Set offsetNanoseconds to abs(offsetNanoseconds).
    offset_nanoseconds = fabs(offset_nanoseconds);

    // 5. Let minutes be offsetNanoseconds / (60 × 10^9) modulo 60.
    auto minutes = fmod(offset_nanoseconds / 60000000000, 60);

    // 6. Let hours be floor(offsetNanoseconds / (3600 × 10^9)).
    auto hours = floor(offset_nanoseconds / 3600000000000);

    // 7. Let h be hours, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 8. Let m be minutes, formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 9. Return the string-concatenation of sign, h, the code unit 0x003A (COLON), and m.
    return String::formatted("{}{:02}:{:02}", sign, (u32)hours, (u32)minutes);
}

// 11.6.11 ToTemporalTimeZone ( temporalTimeZoneLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaltimezone
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
        if (!TRY(temporal_time_zone_like.as_object().has_property(vm.names.timeZone)))
            return &temporal_time_zone_like.as_object();

        // c. Set temporalTimeZoneLike to ? Get(temporalTimeZoneLike, "timeZone").
        temporal_time_zone_like = TRY(temporal_time_zone_like.as_object().get(vm.names.timeZone));

        // d. If Type(temporalTimeZoneLike) is Object and ? HasProperty(temporalTimeZoneLike, "timeZone") is false, return temporalTimeZoneLike.
        if (temporal_time_zone_like.is_object() && !TRY(temporal_time_zone_like.as_object().has_property(vm.names.timeZone)))
            return &temporal_time_zone_like.as_object();
    }

    // 2. Let identifier be ? ToString(temporalTimeZoneLike).
    auto identifier = TRY(temporal_time_zone_like.to_string(global_object));

    // 3. Let result be ? ParseTemporalTimeZone(identifier).
    auto result = TRY(parse_temporal_time_zone(global_object, identifier));

    // 4. Return ? CreateTemporalTimeZone(result).
    return TRY(create_temporal_time_zone(global_object, result));
}

// 11.6.12 GetOffsetNanosecondsFor ( timeZone, instant ), https://tc39.es/proposal-temporal/#sec-temporal-getoffsetnanosecondsfor
ThrowCompletionOr<double> get_offset_nanoseconds_for(GlobalObject& global_object, Value time_zone, Instant& instant)
{
    auto& vm = global_object.vm();

    // 1. Let getOffsetNanosecondsFor be ? GetMethod(timeZone, "getOffsetNanosecondsFor").
    auto* get_offset_nanoseconds_for = TRY(time_zone.get_method(global_object, vm.names.getOffsetNanosecondsFor));

    // 2. Let offsetNanoseconds be ? Call(getOffsetNanosecondsFor, timeZone, « instant »).
    auto offset_nanoseconds_value = TRY(call(global_object, get_offset_nanoseconds_for, time_zone, &instant));

    // 3. If Type(offsetNanoseconds) is not Number, throw a TypeError exception.
    if (!offset_nanoseconds_value.is_number())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsNotA, "Offset nanoseconds value", "number");

    // 4. If ! IsIntegralNumber(offsetNanoseconds) is false, throw a RangeError exception.
    if (!offset_nanoseconds_value.is_integral_number())
        return vm.throw_completion<RangeError>(global_object, ErrorType::IsNotAn, "Offset nanoseconds value", "integral number");

    // 5. Set offsetNanoseconds to ℝ(offsetNanoseconds).
    auto offset_nanoseconds = offset_nanoseconds_value.as_double();

    // 6. If abs(offsetNanoseconds) > 86400 × 10^9, throw a RangeError exception.
    if (fabs(offset_nanoseconds) > 86400000000000.0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidOffsetNanosecondsValue);

    // 7. Return offsetNanoseconds.
    return offset_nanoseconds;
}

// 11.6.13 BuiltinTimeZoneGetOffsetStringFor ( timeZone, instant ), https://tc39.es/proposal-temporal/#sec-temporal-builtintimezonegetoffsetstringfor
ThrowCompletionOr<String> builtin_time_zone_get_offset_string_for(GlobalObject& global_object, Value time_zone, Instant& instant)
{
    // 1. Let offsetNanoseconds be ? GetOffsetNanosecondsFor(timeZone, instant).
    auto offset_nanoseconds = TRY(get_offset_nanoseconds_for(global_object, time_zone, instant));

    // 2. Return ! FormatTimeZoneOffsetString(offsetNanoseconds).
    return format_time_zone_offset_string(offset_nanoseconds);
}

// 11.6.14 BuiltinTimeZoneGetPlainDateTimeFor ( timeZone, instant, calendar ), https://tc39.es/proposal-temporal/#sec-temporal-builtintimezonegetplaindatetimefor
ThrowCompletionOr<PlainDateTime*> builtin_time_zone_get_plain_date_time_for(GlobalObject& global_object, Value time_zone, Instant& instant, Object& calendar)
{
    // 1. Assert: instant has an [[InitializedTemporalInstant]] internal slot.

    // 2. Let offsetNanoseconds be ? GetOffsetNanosecondsFor(timeZone, instant).
    auto offset_nanoseconds = TRY(get_offset_nanoseconds_for(global_object, time_zone, instant));

    // 3. Let result be ! GetISOPartsFromEpoch(instant.[[Nanoseconds]]).
    auto result = get_iso_parts_from_epoch(instant.nanoseconds());

    // 4. Set result to ! BalanceISODateTime(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]] + offsetNanoseconds).
    result = balance_iso_date_time(result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond + offset_nanoseconds);

    // 5. Return ? CreateTemporalDateTime(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], calendar).
    return create_temporal_date_time(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond, calendar);
}

// 11.6.15 BuiltinTimeZoneGetInstantFor ( timeZone, dateTime, disambiguation ), https://tc39.es/proposal-temporal/#sec-temporal-builtintimezonegetinstantfor
ThrowCompletionOr<Instant*> builtin_time_zone_get_instant_for(GlobalObject& global_object, Value time_zone, PlainDateTime& date_time, StringView disambiguation)
{
    // 1. Assert: dateTime has an [[InitializedTemporalDateTime]] internal slot.

    // 2. Let possibleInstants be ? GetPossibleInstantsFor(timeZone, dateTime).
    auto possible_instants = TRY(get_possible_instants_for(global_object, time_zone, date_time));

    // 3. Return ? DisambiguatePossibleInstants(possibleInstants, timeZone, dateTime, disambiguation).
    return disambiguate_possible_instants(global_object, possible_instants, time_zone, date_time, disambiguation);
}

// 11.6.16 DisambiguatePossibleInstants ( possibleInstants, timeZone, dateTime, disambiguation ), https://tc39.es/proposal-temporal/#sec-temporal-disambiguatepossibleinstants
ThrowCompletionOr<Instant*> disambiguate_possible_instants(GlobalObject& global_object, Vector<Value> const& possible_instants, Value time_zone, PlainDateTime& date_time, StringView disambiguation)
{
    // TODO: MarkedValueList<T> would be nice, then we could pass a Vector<Instant*> here and wouldn't need the casts...

    auto& vm = global_object.vm();

    // 1. Assert: dateTime has an [[InitializedTemporalDateTime]] internal slot.

    // 2. Let n be possibleInstants's length.
    auto n = possible_instants.size();

    // 3. If n = 1, then
    if (n == 1) {
        // a. Return possibleInstants[0].
        auto& instant = possible_instants[0];
        return &static_cast<Instant&>(const_cast<Object&>(instant.as_object()));
    }

    // 4. If n ≠ 0, then
    if (n != 0) {
        // a. If disambiguation is "earlier" or "compatible", then
        if (disambiguation.is_one_of("earlier"sv, "compatible"sv)) {
            // i. Return possibleInstants[0].
            auto& instant = possible_instants[0];
            return &static_cast<Instant&>(const_cast<Object&>(instant.as_object()));
        }

        // b. If disambiguation is "later", then
        if (disambiguation == "later"sv) {
            // i. Return possibleInstants[n − 1].
            auto& instant = possible_instants[n - 1];
            return &static_cast<Instant&>(const_cast<Object&>(instant.as_object()));
        }

        // c. Assert: disambiguation is "reject".
        VERIFY(disambiguation == "reject"sv);

        // d. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDisambiguatePossibleInstantsRejectMoreThanOne);
    }

    // 5. Assert: n = 0.
    VERIFY(n == 0);

    // 6. If disambiguation is "reject", then
    if (disambiguation == "reject"sv) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDisambiguatePossibleInstantsRejectZero);
    }

    // 7. Let epochNanoseconds be ! GetEpochFromISOParts(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]]).
    auto* epoch_nanoseconds = get_epoch_from_iso_parts(global_object, date_time.iso_year(), date_time.iso_month(), date_time.iso_day(), date_time.iso_hour(), date_time.iso_minute(), date_time.iso_second(), date_time.iso_millisecond(), date_time.iso_microsecond(), date_time.iso_nanosecond());

    // 8. Let dayBefore be ! CreateTemporalInstant(epochNanoseconds − 8.64 × 10^13).
    auto* day_before = MUST(create_temporal_instant(global_object, *js_bigint(vm, epoch_nanoseconds->big_integer().minus("86400000000000"_sbigint))));

    // 9. Let dayAfter be ! CreateTemporalInstant(epochNanoseconds + 8.64 × 10^13).
    auto* day_after = MUST(create_temporal_instant(global_object, *js_bigint(vm, epoch_nanoseconds->big_integer().plus("86400000000000"_sbigint))));

    // 10. Let offsetBefore be ? GetOffsetNanosecondsFor(timeZone, dayBefore).
    auto offset_before = TRY(get_offset_nanoseconds_for(global_object, time_zone, *day_before));

    // 11. Let offsetAfter be ? GetOffsetNanosecondsFor(timeZone, dayAfter).
    auto offset_after = TRY(get_offset_nanoseconds_for(global_object, time_zone, *day_after));

    // 12. Let nanoseconds be offsetAfter − offsetBefore.
    auto nanoseconds = offset_after - offset_before;

    // 13. If disambiguation is "earlier", then
    if (disambiguation == "earlier"sv) {
        // a. Let earlier be ? AddDateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], dateTime.[[Calendar]], 0, 0, 0, 0, 0, 0, 0, 0, 0, −nanoseconds, undefined).
        auto earlier = TRY(add_date_time(global_object, date_time.iso_year(), date_time.iso_month(), date_time.iso_day(), date_time.iso_hour(), date_time.iso_minute(), date_time.iso_second(), date_time.iso_millisecond(), date_time.iso_microsecond(), date_time.iso_nanosecond(), date_time.calendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, -nanoseconds, nullptr));

        // b. Let earlierDateTime be ! CreateTemporalDateTime(earlier.[[Year]], earlier.[[Month]], earlier.[[Day]], earlier.[[Hour]], earlier.[[Minute]], earlier.[[Second]], earlier.[[Millisecond]], earlier.[[Microsecond]], earlier.[[Nanosecond]], dateTime.[[Calendar]]).
        auto* earlier_date_time = MUST(create_temporal_date_time(global_object, earlier.year, earlier.month, earlier.day, earlier.hour, earlier.minute, earlier.second, earlier.millisecond, earlier.microsecond, earlier.nanosecond, date_time.calendar()));

        // c. Set possibleInstants to ? GetPossibleInstantsFor(timeZone, earlierDateTime).
        auto possible_instants_mvl = TRY(get_possible_instants_for(global_object, time_zone, *earlier_date_time));

        // d. If possibleInstants is empty, throw a RangeError exception.
        if (possible_instants_mvl.is_empty())
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDisambiguatePossibleInstantsEarlierZero);

        // e. Return possibleInstants[0].
        auto& instant = possible_instants_mvl[0];
        return &static_cast<Instant&>(const_cast<Object&>(instant.as_object()));
    }

    // 14. Assert: disambiguation is "compatible" or "later".
    VERIFY(disambiguation.is_one_of("compatible"sv, "later"sv));

    // 15. Let later be ? AddDateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], dateTime.[[Calendar]], 0, 0, 0, 0, 0, 0, 0, 0, 0, nanoseconds, undefined).
    auto later = TRY(add_date_time(global_object, date_time.iso_year(), date_time.iso_month(), date_time.iso_day(), date_time.iso_hour(), date_time.iso_minute(), date_time.iso_second(), date_time.iso_millisecond(), date_time.iso_microsecond(), date_time.iso_nanosecond(), date_time.calendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, nanoseconds, nullptr));

    // 16. Let laterDateTime be ! CreateTemporalDateTime(later.[[Year]], later.[[Month]], later.[[Day]], later.[[Hour]], later.[[Minute]], later.[[Second]], later.[[Millisecond]], later.[[Microsecond]], later.[[Nanosecond]], dateTime.[[Calendar]]).
    auto* later_date_time = MUST(create_temporal_date_time(global_object, later.year, later.month, later.day, later.hour, later.minute, later.second, later.millisecond, later.microsecond, later.nanosecond, date_time.calendar()));

    // 17. Set possibleInstants to ? GetPossibleInstantsFor(timeZone, laterDateTime).
    auto possible_instants_mvl = TRY(get_possible_instants_for(global_object, time_zone, *later_date_time));

    // 18. Set n to possibleInstants's length.
    n = possible_instants_mvl.size();

    // 19. If n = 0, throw a RangeError exception.
    if (n == 0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDisambiguatePossibleInstantsZero);

    // 20. Return possibleInstants[n − 1].
    auto& instant = possible_instants_mvl[n - 1];
    return &static_cast<Instant&>(const_cast<Object&>(instant.as_object()));
}

// 11.6.17 GetPossibleInstantsFor ( timeZone, dateTime ), https://tc39.es/proposal-temporal/#sec-temporal-getpossibleinstantsfor
ThrowCompletionOr<MarkedValueList> get_possible_instants_for(GlobalObject& global_object, Value time_zone, PlainDateTime& date_time)
{
    auto& vm = global_object.vm();

    // 1. Assert: dateTime has an [[InitializedTemporalDateTime]] internal slot.

    // 2. Let possibleInstants be ? Invoke(timeZone, "getPossibleInstantsFor", « dateTime »).
    auto possible_instants = TRY(time_zone.invoke(global_object, vm.names.getPossibleInstantsFor, &date_time));

    // 3. Let iteratorRecord be ? GetIterator(possibleInstants, sync).
    auto* iterator = TRY(get_iterator(global_object, possible_instants, IteratorHint::Sync));

    // 4. Let list be a new empty List.
    auto list = MarkedValueList { vm.heap() };

    // 5. Let next be true.
    Object* next = nullptr;

    // 6. Repeat, while next is not false,
    do {
        // a. Set next to ? IteratorStep(iteratorRecord).
        next = TRY(iterator_step(global_object, *iterator));

        // b. If next is not false, then
        if (next) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = TRY(iterator_value(global_object, *next));

            // ii. If Type(nextValue) is not Object or nextValue does not have an [[InitializedTemporalInstant]] internal slot, then
            if (!next_value.is_object() || !is<Instant>(next_value.as_object())) {
                // 1. Let completion be ThrowCompletion(a newly created TypeError object).
                auto completion = vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.Instant");

                // 2. Return ? IteratorClose(iteratorRecord, completion).
                return iterator_close(*iterator, move(completion));
            }

            // iii. Append nextValue to the end of the List list.
            list.append(next_value);
        }
    } while (next != nullptr);

    // 7. Return list.
    return { move(list) };
}

// 11.6.18 TimeZoneEquals ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal-timezoneequals
ThrowCompletionOr<bool> time_zone_equals(GlobalObject& global_object, Object& one, Object& two)
{
    // 1. If one and two are the same Object value, return true.
    if (&one == &two)
        return true;

    // 2. Let timeZoneOne be ? ToString(one).
    auto time_zone_one = TRY(Value(&one).to_string(global_object));

    // 3. Let timeZoneTwo be ? ToString(two).
    auto time_zone_two = TRY(Value(&two).to_string(global_object));

    // 4. If timeZoneOne is timeZoneTwo, return true.
    if (time_zone_one == time_zone_two)
        return true;

    // 5. Return false.
    return false;
}

}
