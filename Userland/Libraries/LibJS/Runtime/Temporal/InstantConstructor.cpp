/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

}
