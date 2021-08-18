/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainTimeConstructor.h>

namespace JS::Temporal {

// 4.1 The Temporal.PlainTime Constructor, https://tc39.es/proposal-temporal/#sec-temporal-plaintime-constructor
PlainTimeConstructor::PlainTimeConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PlainTime.as_string(), *global_object.function_prototype())
{
}

void PlainTimeConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 4.2.1 Temporal.PlainTime.prototype, https://tc39.es/proposal-temporal/#sec-temporal-plaintime-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_plain_time_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 4.1.1 Temporal.PlainTime ( [ hour [ , minute [ , second [ , millisecond [ , microsecond [ , nanosecond ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime
Value PlainTimeConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainTime");
    return {};
}

// 4.1.1 Temporal.PlainTime ( [ hour [ , minute [ , second [ , millisecond [ , microsecond [ , nanosecond ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime
Value PlainTimeConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let hour be ? ToIntegerThrowOnInfinity(hour).
    auto hour = to_integer_throw_on_infinity(global_object, vm.argument(0), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // 3. Let minute be ? ToIntegerThrowOnInfinity(hour).
    auto minute = to_integer_throw_on_infinity(global_object, vm.argument(1), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // 4. Let second be ? ToIntegerThrowOnInfinity(hour).
    auto second = to_integer_throw_on_infinity(global_object, vm.argument(2), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // 5. Let millisecond be ? ToIntegerThrowOnInfinity(hour).
    auto millisecond = to_integer_throw_on_infinity(global_object, vm.argument(3), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // 6. Let microsecond be ? ToIntegerThrowOnInfinity(hour).
    auto microsecond = to_integer_throw_on_infinity(global_object, vm.argument(4), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // 7. Let nanosecond be ? ToIntegerThrowOnInfinity(hour).
    auto nanosecond = to_integer_throw_on_infinity(global_object, vm.argument(5), ErrorType::TemporalInvalidPlainTime);
    if (vm.exception())
        return {};

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalTime will immediately check that these values are valid
    // ISO values (for hours: 0 - 23, for minutes and seconds: 0 - 59, milliseconds, microseconds, and nanoseconds: 0 - 999) all of which
    // are subsets of this check.
    if (!AK::is_within_range<u8>(hour) || !AK::is_within_range<u8>(minute) || !AK::is_within_range<u8>(second) || !AK::is_within_range<u16>(millisecond) || !AK::is_within_range<u16>(microsecond) || !AK::is_within_range<u16>(nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 8. Return ? CreateTemporalTime(hour, minute, second, millisecond, microsecond, nanosecond, NewTarget).
    return create_temporal_time(global_object, hour, minute, second, millisecond, microsecond, nanosecond, &new_target);
}

}
