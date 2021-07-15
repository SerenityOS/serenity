/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>

namespace JS::Temporal {

// 7.1 The Temporal.Duration Constructor, https://tc39.es/proposal-temporal/#sec-temporal-duration-constructor
DurationConstructor::DurationConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Duration.as_string(), *global_object.function_prototype())
{
}

void DurationConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 7.2.1 Temporal.Duration.prototype, https://tc39.es/proposal-temporal/#sec-temporal-duration-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_duration_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 7.1.1 Temporal.Duration ( [ years [ , months [ , weeks [ , days [ , hours [ , minutes [ , seconds [ , milliseconds [ , microseconds [ , nanoseconds ] ] ] ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration
Value DurationConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, then
    // a. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.Duration");
    return {};
}

// 7.1.1 Temporal.Duration ( [ years [ , months [ , weeks [ , days [ , hours [ , minutes [ , seconds [ , milliseconds [ , microseconds [ , nanoseconds ] ] ] ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration
Value DurationConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let y be ? ToIntegerOrInfinity(years).
    auto y = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 3. Let mo be ? ToIntegerOrInfinity(months).
    auto mo = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 4. Let w be ? ToIntegerOrInfinity(weeks).
    auto w = vm.argument(2).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 5. Let d be ? ToIntegerOrInfinity(days).
    auto d = vm.argument(3).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 6. Let h be ? ToIntegerOrInfinity(hours).
    auto h = vm.argument(4).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 7. Let m be ? ToIntegerOrInfinity(minutes).
    auto m = vm.argument(5).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 8. Let s be ? ToIntegerOrInfinity(seconds).
    auto s = vm.argument(6).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 9. Let ms be ? ToIntegerOrInfinity(milliseconds).
    auto ms = vm.argument(7).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 10. Let mis be ? ToIntegerOrInfinity(microseconds).
    auto mis = vm.argument(8).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 11. Let ns be ? ToIntegerOrInfinity(nanoseconds).
    auto ns = vm.argument(9).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 12. Return ? CreateTemporalDuration(y, mo, w, d, h, m, s, ms, mis, ns, NewTarget).
    return create_temporal_duration(global_object, y, mo, w, d, h, m, s, ms, mis, ns, &new_target);
}

}
