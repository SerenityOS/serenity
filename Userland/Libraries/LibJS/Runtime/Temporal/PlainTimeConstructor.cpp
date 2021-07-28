/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
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

    // 2. Let hour be ? ToIntegerOrInfinity(hour).
    auto hour = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 3. If hour is +∞ or -∞, throw a RangeError exception.
    if (Value(hour).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 4. Let minute be ? ToIntegerOrInfinity(hour).
    auto minute = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 5. If minute is +∞ or -∞, throw a RangeError exception.
    if (Value(minute).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 6. Let second be ? ToIntegerOrInfinity(hour).
    auto second = vm.argument(2).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 7. If second is +∞ or -∞, throw a RangeError exception.
    if (Value(second).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 8. Let millisecond be ? ToIntegerOrInfinity(hour).
    auto millisecond = vm.argument(3).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 9. If millisecond is +∞ or -∞, throw a RangeError exception.
    if (Value(millisecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 10. Let microsecond be ? ToIntegerOrInfinity(hour).
    auto microsecond = vm.argument(4).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 11. If microsecond is +∞ or -∞, throw a RangeError exception.
    if (Value(microsecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 12. Let nanosecond be ? ToIntegerOrInfinity(hour).
    auto nanosecond = vm.argument(5).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 13. If nanosecond is +∞ or -∞, throw a RangeError exception.
    if (Value(nanosecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalTime will immediately check that these values are valid
    // ISO values (for hours: 0 - 23, for minutes and seconds: 0 - 59, milliseconds, microseconds, and nanoseconds: 0 - 999) all of which
    // are subsets of this check.
    if (!AK::is_within_range<u8>(hour) || !AK::is_within_range<u8>(minute) || !AK::is_within_range<u8>(second) || !AK::is_within_range<u16>(millisecond) || !AK::is_within_range<u16>(microsecond) || !AK::is_within_range<u16>(nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 14. Return ? CreateTemporalTime(hour, minute, second, millisecond, microsecond, nanosecond, NewTarget).
    return create_temporal_time(global_object, hour, minute, second, millisecond, microsecond, nanosecond, &new_target);
}

}
