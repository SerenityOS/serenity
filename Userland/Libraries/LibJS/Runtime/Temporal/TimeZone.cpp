/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>
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
    if (is_valid_time_zone_numeric_utc_offset(identifier)) {
        // TODO:
        // a. Set object.[[OffsetNanoseconds]] to ! ParseTimeZoneOffsetString(identifier).
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

}
