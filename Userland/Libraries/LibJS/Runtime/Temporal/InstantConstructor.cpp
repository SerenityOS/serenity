/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>

namespace JS::Temporal {

// 8.1 The Temporal.Instant Constructor, https://tc39.es/proposal-temporal/#sec-temporal-instant-constructor
InstantConstructor::InstantConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Instant.as_string(), *global_object.function_prototype())
{
}

void InstantConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 8.2.1 Temporal.Instant.prototype, https://tc39.es/proposal-temporal/#sec-temporal-instant-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_instant_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.fromEpochSeconds, from_epoch_seconds, 1, attr);
    define_native_function(vm.names.fromEpochMilliseconds, from_epoch_milliseconds, 1, attr);
    define_native_function(vm.names.fromEpochMicroseconds, from_epoch_microseconds, 1, attr);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 8.1.1 Temporal.Instant ( epochNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal.instant
Value InstantConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, then
    // a. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.Instant");
    return {};
}

// 8.1.1 Temporal.Instant ( epochNanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal.instant
Value InstantConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let epochNanoseconds be ? ToBigInt(epochNanoseconds).
    auto* epoch_nanoseconds = vm.argument(0).to_bigint(global_object);
    if (vm.exception())
        return {};

    // 3. If ! IsValidEpochNanoseconds(epochNanoseconds) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*epoch_nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
        return {};
    }

    // 4. Return ? CreateTemporalInstant(epochNanoseconds, NewTarget).
    return create_temporal_instant(global_object, *epoch_nanoseconds, &new_target);
}

// 8.2.3 Temporal.Instant.fromEpochSeconds ( epochSeconds ), https://tc39.es/proposal-temporal/#sec-temporal.instant.fromepochseconds
JS_DEFINE_NATIVE_FUNCTION(InstantConstructor::from_epoch_seconds)
{
    // 1. Set epochSeconds to ? ToNumber(epochSeconds).
    auto epoch_seconds_value = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};

    // 2. Set epochSeconds to ? NumberToBigInt(epochSeconds).
    auto* epoch_seconds = number_to_bigint(global_object, epoch_seconds_value);
    if (vm.exception())
        return {};

    // 3. Let epochNanoseconds be epochSeconds × 10^9ℤ.
    auto* epoch_nanoseconds = js_bigint(vm.heap(), epoch_seconds->big_integer().multiplied_by(Crypto::UnsignedBigInteger { 1'000'000'000 }));

    // 4. If ! IsValidEpochNanoseconds(epochNanoseconds) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*epoch_nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
        return {};
    }

    // 5. Return ? CreateTemporalInstant(epochNanoseconds).
    return create_temporal_instant(global_object, *epoch_nanoseconds);
}

// 8.2.4 Temporal.Instant.fromEpochMilliseconds ( epochMilliseconds ), https://tc39.es/proposal-temporal/#sec-temporal.instant.fromepochmilliseconds
JS_DEFINE_NATIVE_FUNCTION(InstantConstructor::from_epoch_milliseconds)
{
    // 1. Set epochMilliseconds to ? ToNumber(epochMilliseconds).
    auto epoch_milliseconds_value = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};

    // 2. Set epochMilliseconds to ? NumberToBigInt(epochMilliseconds).
    auto* epoch_milliseconds = number_to_bigint(global_object, epoch_milliseconds_value);
    if (vm.exception())
        return {};

    // 3. Let epochNanoseconds be epochMilliseconds × 10^6ℤ.
    auto* epoch_nanoseconds = js_bigint(vm.heap(), epoch_milliseconds->big_integer().multiplied_by(Crypto::UnsignedBigInteger { 1'000'000 }));

    // 4. If ! IsValidEpochNanoseconds(epochNanoseconds) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*epoch_nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
        return {};
    }

    // 5. Return ? CreateTemporalInstant(epochNanoseconds).
    return create_temporal_instant(global_object, *epoch_nanoseconds);
}

// 8.2.5 Temporal.Instant.fromEpochMicroseconds ( epochMicroseconds )
JS_DEFINE_NATIVE_FUNCTION(InstantConstructor::from_epoch_microseconds)
{
    // 1. Set epochMicroseconds to ? ToBigInt(epochMicroseconds).
    auto* epoch_microseconds = vm.argument(0).to_bigint(global_object);
    if (vm.exception())
        return {};

    // 2. Let epochNanoseconds be epochMicroseconds × 1000ℤ.
    auto* epoch_nanoseconds = js_bigint(vm.heap(), epoch_microseconds->big_integer().multiplied_by(Crypto::UnsignedBigInteger { 1'000 }));

    // 3. If ! IsValidEpochNanoseconds(epochNanoseconds) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*epoch_nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);
        return {};
    }

    // 4. Return ? CreateTemporalInstant(epochNanoseconds).
    return create_temporal_instant(global_object, *epoch_nanoseconds);
}

}
