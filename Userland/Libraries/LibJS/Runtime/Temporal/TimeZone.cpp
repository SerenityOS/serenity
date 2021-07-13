/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateTimeLexer.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/TimeZoneConstructor.h>

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

// 11.6.2 CreateTemporalTimeZone ( identifier [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaltimezone
Object* create_temporal_time_zone(GlobalObject& global_object, String const& identifier, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. If newTarget is not present, set it to %Temporal.TimeZone%.
    if (!new_target)
        new_target = global_object.temporal_time_zone_constructor();

    // 2. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.TimeZone.prototype%", « [[InitializedTemporalTimeZone]], [[Identifier]], [[OffsetNanoseconds]] »).
    // 3. Set object.[[Identifier]] to identifier.
    auto* object = ordinary_create_from_constructor<TimeZone>(global_object, *new_target, &GlobalObject::temporal_time_zone_prototype, identifier);
    if (vm.exception())
        return {};

    // 4. If identifier satisfies the syntax of a TimeZoneNumericUTCOffset (see 13.33), then
    if (is_valid_time_zone_numeric_utc_offset_syntax(identifier)) {
        // a. Set object.[[OffsetNanoseconds]] to ! ParseTimeZoneOffsetString(identifier).
        object->set_offset_nanoseconds(parse_time_zone_offset_string(global_object, identifier));
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
double parse_time_zone_offset_string(GlobalObject& global_object, String const& offset_string)
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
    if (!success) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidFormat, "TimeZone offset");
        return {};
    }

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

}
