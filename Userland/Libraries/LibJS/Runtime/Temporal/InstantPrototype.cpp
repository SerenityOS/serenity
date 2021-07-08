/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/GlobalObject.h>
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

    define_native_accessor(vm.names.epochSeconds, epoch_seconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMilliseconds, epoch_milliseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMicroseconds, epoch_microseconds_getter, {}, Attribute::Configurable);

    // 8.3.2 Temporal.Instant.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.instant.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.Instant"), Attribute::Configurable);
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
    auto [s, _] = ns.big_integer().divided_by(Crypto::SignedBigInteger::create_from(1'000'000'000));

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
    auto [ms, _] = ns.big_integer().divided_by(Crypto::SignedBigInteger::create_from(1'000'000));

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
    auto [us, _] = ns.big_integer().divided_by(Crypto::SignedBigInteger::create_from(1'000));

    // 5. Return ℤ(µs).
    return js_bigint(vm.heap(), move(us));
}

}
