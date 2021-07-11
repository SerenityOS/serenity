/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>

namespace JS::Temporal {

// 8 Temporal.Instant Objects, https://tc39.es/proposal-temporal/#sec-temporal-instant-objects
Instant::Instant(BigInt& nanoseconds, Object& prototype)
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
Instant* create_temporal_instant(GlobalObject& global_object, BigInt& epoch_nanoseconds, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(epochNanoseconds) is BigInt.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 3. If newTarget is not present, set it to %Temporal.Instant%.
    if (!new_target)
        new_target = global_object.temporal_instant_constructor();

    // 4. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Instant.prototype%", « [[InitializedTemporalInstant]], [[Nanoseconds]] »).
    // 5. Set object.[[Nanoseconds]] to epochNanoseconds.
    auto* object = ordinary_create_from_constructor<Instant>(global_object, *new_target, &GlobalObject::temporal_instant_prototype, epoch_nanoseconds);
    if (vm.exception())
        return {};

    // 6. Return object.
    return object;
}

// 8.5.3 ToTemporalInstant ( item ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalinstant
Instant* to_temporal_instant(GlobalObject& global_object, Value item)
{
    auto& vm = global_object.vm();

    // 1. If Type(item) is Object, then
    if (item.is_object()) {
        // a. If item has an [[InitializedTemporalInstant]] internal slot, then
        if (is<Instant>(item.as_object())) {
            // i. Return item.
            return &static_cast<Instant&>(item.as_object());
        }
        // TODO:
        // b. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        // i. Return ! CreateTemporalInstant(item.[[Nanoseconds]]).
    }

    // 2. Let string be ? ToString(item).
    auto string = item.to_string(global_object);
    if (vm.exception())
        return {};

    // 3. Let epochNanoseconds be ? ParseTemporalInstant(string).
    auto* epoch_nanoseconds = parse_temporal_instant(global_object, string);
    if (vm.exception())
        return {};

    return create_temporal_instant(global_object, *epoch_nanoseconds);
}

// 8.5.4 ParseTemporalInstant ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalinstant
BigInt* parse_temporal_instant(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let result be ? ParseTemporalInstantString(isoString).
    auto result = parse_temporal_instant_string(global_object, iso_string);
    if (vm.exception())
        return {};

    // 3. Let offsetString be result.[[TimeZoneOffsetString]].
    auto& offset_string = result->time_zone_offset;

    // 4. Assert: offsetString is not undefined.
    VERIFY(offset_string.has_value());

    // 5. Let utc be ? GetEpochFromISOParts(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    auto* utc = get_epoch_from_iso_parts(global_object, result->year, result->month, result->day, result->hour, result->minute, result->second, result->millisecond, result->microsecond, result->nanosecond);
    if (vm.exception())
        return {};

    // 6. If utc < −8.64 × 10^21 or utc > 8.64 × 10^21, then
    if (utc->big_integer() < INSTANT_NANOSECONDS_MIN || utc->big_integer() > INSTANT_NANOSECONDS_MAX) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
        return {};
    }

    // 7. Let offsetNanoseconds be ? ParseTimeZoneOffsetString(offsetString).
    auto offset_nanoseconds = parse_time_zone_offset_string(global_object, *offset_string);
    if (vm.exception())
        return {};

    // 8. Return utc − offsetNanoseconds.
    return js_bigint(vm.heap(), utc->big_integer().minus(Crypto::SignedBigInteger::create_from(offset_nanoseconds)));
}

}
