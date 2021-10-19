/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_old_native_function(vm.names.from, from, 1, attr);

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

    // 2. Let y be ? ToIntegerThrowOnInfinity(years).
    auto y = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(0), ErrorType::TemporalInvalidDuration));

    // 3. Let mo be ? ToIntegerThrowOnInfinity(months).
    auto mo = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(1), ErrorType::TemporalInvalidDuration));

    // 4. Let w be ? ToIntegerThrowOnInfinity(weeks).
    auto w = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(2), ErrorType::TemporalInvalidDuration));

    // 5. Let d be ? ToIntegerThrowOnInfinity(days).
    auto d = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(3), ErrorType::TemporalInvalidDuration));

    // 6. Let h be ? ToIntegerThrowOnInfinity(hours).
    auto h = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(4), ErrorType::TemporalInvalidDuration));

    // 7. Let m be ? ToIntegerThrowOnInfinity(minutes).
    auto m = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(5), ErrorType::TemporalInvalidDuration));

    // 8. Let s be ? ToIntegerThrowOnInfinity(seconds).
    auto s = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(6), ErrorType::TemporalInvalidDuration));

    // 9. Let ms be ? ToIntegerThrowOnInfinity(milliseconds).
    auto ms = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(7), ErrorType::TemporalInvalidDuration));

    // 10. Let mis be ? ToIntegerThrowOnInfinity(microseconds).
    auto mis = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(8), ErrorType::TemporalInvalidDuration));

    // 11. Let ns be ? ToIntegerThrowOnInfinity(nanoseconds).
    auto ns = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, vm.argument(9), ErrorType::TemporalInvalidDuration));

    // 12. Return ? CreateTemporalDuration(y, mo, w, d, h, m, s, ms, mis, ns, NewTarget).
    return TRY_OR_DISCARD(create_temporal_duration(global_object, y, mo, w, d, h, m, s, ms, mis, ns, &new_target));
}

// 7.2.2 Temporal.Duration.from ( item ), https://tc39.es/proposal-temporal/#sec-temporal.duration.from
JS_DEFINE_OLD_NATIVE_FUNCTION(DurationConstructor::from)
{
    auto item = vm.argument(0);

    // 1. If Type(item) is Object and item has an [[InitializedTemporalDuration]] internal slot, then
    if (item.is_object() && is<Duration>(item.as_object())) {
        auto& duration = static_cast<Duration&>(item.as_object());

        // a. Return ? CreateTemporalDuration(item.[[Years]], item.[[Months]], item.[[Weeks]], item.[[Days]], item.[[Hours]], item.[[Minutes]], item.[[Seconds]], item.[[Milliseconds]], item.[[Microseconds]], item.[[Nanoseconds]]).
        return TRY_OR_DISCARD(create_temporal_duration(global_object, duration.years(), duration.months(), duration.weeks(), duration.days(), duration.hours(), duration.minutes(), duration.seconds(), duration.milliseconds(), duration.microseconds(), duration.nanoseconds()));
    }

    // 2. Return ? ToTemporalDuration(item).
    return TRY_OR_DISCARD(to_temporal_duration(global_object, item));
}

}
