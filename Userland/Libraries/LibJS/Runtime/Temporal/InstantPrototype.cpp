/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/InstantPrototype.h>

namespace JS::Temporal {

// 8.3 Properties of the Temporal.Instant Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-instant-prototype-object
InstantPrototype::InstantPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void InstantPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 8.3.2 Temporal.Instant.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.instant.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.Instant"), Attribute::Configurable);

    define_native_accessor(vm.names.epochSeconds, epoch_seconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMilliseconds, epoch_milliseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMicroseconds, epoch_microseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochNanoseconds, epoch_nanoseconds_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.round, round, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

static Instant* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Instant>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.Instant");
        return {};
    }
    return static_cast<Instant*>(this_object);
}

// 8.3.3 get Temporal.Instant.prototype.epochSeconds, https://tc39.es/proposal-temporal/#sec-get-temporal.instant.prototype.epochseconds
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::epoch_seconds_getter)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let ns be instant.[[Nanoseconds]].
    auto& ns = instant->nanoseconds();

    // 4. Let s be RoundTowardsZero(ℝ(ns) / 10^9).
    auto [s, _] = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000'000 });

    // 5. Return 𝔽(s).
    return Value((double)s.to_base(10).to_int<i64>().value());
}

// 8.3.4 get Temporal.Instant.prototype.epochMilliseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.instant.prototype.epochmilliseconds
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::epoch_milliseconds_getter)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let ns be instant.[[Nanoseconds]].
    auto& ns = instant->nanoseconds();

    // 4. Let ms be RoundTowardsZero(ℝ(ns) / 10^6).
    auto [ms, _] = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000 });

    // 5. Return 𝔽(ms).
    return Value((double)ms.to_base(10).to_int<i64>().value());
}

// 8.3.5 get Temporal.Instant.prototype.epochMicroseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.instant.prototype.epochmicroseconds
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::epoch_microseconds_getter)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let ns be instant.[[Nanoseconds]].
    auto& ns = instant->nanoseconds();

    // 4. Let µs be RoundTowardsZero(ℝ(ns) / 10^3).
    auto [us, _] = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000 });

    // 5. Return ℤ(µs).
    return js_bigint(vm.heap(), move(us));
}

// 8.3.6 get Temporal.Instant.prototype.epochNanoseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.instant.prototype.epochnanoseconds
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::epoch_nanoseconds_getter)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let ns be instant.[[Nanoseconds]].
    auto& ns = instant->nanoseconds();

    // 4. Return ns.
    return &ns;
}

// 8.3.11 Temporal.Instant.prototype.round ( options )
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::round)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Let smallestUnit be ? ToSmallestTemporalUnit(options, « "year", "month", "week", "day" », undefined).
    auto smallest_unit_value = to_smallest_temporal_unit(global_object, *options, { "year"sv, "month"sv, "week"sv, "day"sv }, {});
    if (vm.exception())
        return {};

    // 5. If smallestUnit is undefined, throw a RangeError exception.
    if (!smallest_unit_value.has_value()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, vm.names.undefined.as_string(), "smallestUnit");
        return {};
    }
    // At this point smallest_unit_value can only be a string
    auto& smallest_unit = *smallest_unit_value;

    // 6. Let roundingMode be ? ToTemporalRoundingMode(options, "halfExpand").
    auto rounding_mode = to_temporal_rounding_mode(global_object, *options, "halfExpand");
    if (vm.exception())
        return {};

    double maximum;
    // 7. If smallestUnit is "hour", then
    if (smallest_unit == "hour"sv) {
        // a. Let maximum be 24.
        maximum = 24;
    }
    // 8. Else if smallestUnit is "minute", then
    else if (smallest_unit == "minute"sv) {
        // a. Let maximum be 1440.
        maximum = 1440;
    }
    // 9. Else if smallestUnit is "second", then
    else if (smallest_unit == "second"sv) {
        // a. Let maximum be 86400.
        maximum = 86400;
    }
    // 10. Else if smallestUnit is "millisecond", then
    else if (smallest_unit == "millisecond"sv) {
        // a. Let maximum be 8.64 × 10^7.
        maximum = 86400000;
    }
    // 11. Else if smallestUnit is "microsecond", then
    else if (smallest_unit == "microsecond"sv) {
        // a. Let maximum be 8.64 × 10^10.
        maximum = 86400000000;
    }
    // 12. Else,
    else {
        // a. Assert: smallestUnit is "nanosecond".
        VERIFY(smallest_unit == "nanosecond"sv);
        // b. Let maximum be 8.64 × 10^13.
        maximum = 86400000000000;
    }

    // 13. Let roundingIncrement be ? ToTemporalRoundingIncrement(options, maximum, true).
    auto rounding_increment = to_temporal_rounding_increment(global_object, *options, maximum, true);
    if (vm.exception())
        return {};

    // 14. Let roundedNs be ? RoundTemporalInstant(instant.[[Nanoseconds]], roundingIncrement, smallestUnit, roundingMode).
    auto* rounded_ns = round_temporal_instant(global_object, instant->nanoseconds(), rounding_increment, smallest_unit, rounding_mode);
    if (vm.exception())
        return {};

    // 15. Return ! CreateTemporalInstant(roundedNs).
    return create_temporal_instant(global_object, *rounded_ns);
}

// 8.3.12 Temporal.Instant.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.instant.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::equals)
{
    // 1. Let instant be the this value.
    // 2. Perform ? RequireInternalSlot(instant, [[InitializedTemporalInstant]]).
    auto* instant = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set other to ? ToTemporalInstant(other).
    auto other = to_temporal_instant(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. If instant.[[Nanoseconds]] ≠ other.[[Nanoseconds]], return false.
    if (instant->nanoseconds().big_integer() != other->nanoseconds().big_integer())
        return Value(false);

    // 5. Return true.
    return Value(true);
}

// 8.3.16 Temporal.Instant.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.instant.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(InstantPrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.Instant", "a primitive value");
    return {};
}

}
