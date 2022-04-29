/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
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
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.compare, compare, 2, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 7.1.1 Temporal.Duration ( [ years [ , months [ , weeks [ , days [ , hours [ , minutes [ , seconds [ , milliseconds [ , microseconds [ , nanoseconds ] ] ] ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration
ThrowCompletionOr<Value> DurationConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, then
    // a. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.Duration");
}

// 7.1.1 Temporal.Duration ( [ years [ , months [ , weeks [ , days [ , hours [ , minutes [ , seconds [ , milliseconds [ , microseconds [ , nanoseconds ] ] ] ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration
ThrowCompletionOr<Object*> DurationConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let y be ? ToIntegerWithoutRounding(years).
    auto y = TRY(to_integer_without_rounding(global_object, vm.argument(0), ErrorType::TemporalInvalidDuration));

    // 3. Let mo be ? ToIntegerWithoutRounding(months).
    auto mo = TRY(to_integer_without_rounding(global_object, vm.argument(1), ErrorType::TemporalInvalidDuration));

    // 4. Let w be ? ToIntegerWithoutRounding(weeks).
    auto w = TRY(to_integer_without_rounding(global_object, vm.argument(2), ErrorType::TemporalInvalidDuration));

    // 5. Let d be ? ToIntegerWithoutRounding(days).
    auto d = TRY(to_integer_without_rounding(global_object, vm.argument(3), ErrorType::TemporalInvalidDuration));

    // 6. Let h be ? ToIntegerWithoutRounding(hours).
    auto h = TRY(to_integer_without_rounding(global_object, vm.argument(4), ErrorType::TemporalInvalidDuration));

    // 7. Let m be ? ToIntegerWithoutRounding(minutes).
    auto m = TRY(to_integer_without_rounding(global_object, vm.argument(5), ErrorType::TemporalInvalidDuration));

    // 8. Let s be ? ToIntegerWithoutRounding(seconds).
    auto s = TRY(to_integer_without_rounding(global_object, vm.argument(6), ErrorType::TemporalInvalidDuration));

    // 9. Let ms be ? ToIntegerWithoutRounding(milliseconds).
    auto ms = TRY(to_integer_without_rounding(global_object, vm.argument(7), ErrorType::TemporalInvalidDuration));

    // 10. Let mis be ? ToIntegerWithoutRounding(microseconds).
    auto mis = TRY(to_integer_without_rounding(global_object, vm.argument(8), ErrorType::TemporalInvalidDuration));

    // 11. Let ns be ? ToIntegerWithoutRounding(nanoseconds).
    auto ns = TRY(to_integer_without_rounding(global_object, vm.argument(9), ErrorType::TemporalInvalidDuration));

    // 12. Return ? CreateTemporalDuration(y, mo, w, d, h, m, s, ms, mis, ns, NewTarget).
    return TRY(create_temporal_duration(global_object, y, mo, w, d, h, m, s, ms, mis, ns, &new_target));
}

// 7.2.2 Temporal.Duration.from ( item ), https://tc39.es/proposal-temporal/#sec-temporal.duration.from
JS_DEFINE_NATIVE_FUNCTION(DurationConstructor::from)
{
    auto item = vm.argument(0);

    // 1. If Type(item) is Object and item has an [[InitializedTemporalDuration]] internal slot, then
    if (item.is_object() && is<Duration>(item.as_object())) {
        auto& duration = static_cast<Duration&>(item.as_object());

        // a. Return ? CreateTemporalDuration(item.[[Years]], item.[[Months]], item.[[Weeks]], item.[[Days]], item.[[Hours]], item.[[Minutes]], item.[[Seconds]], item.[[Milliseconds]], item.[[Microseconds]], item.[[Nanoseconds]]).
        return TRY(create_temporal_duration(global_object, duration.years(), duration.months(), duration.weeks(), duration.days(), duration.hours(), duration.minutes(), duration.seconds(), duration.milliseconds(), duration.microseconds(), duration.nanoseconds()));
    }

    // 2. Return ? ToTemporalDuration(item).
    return TRY(to_temporal_duration(global_object, item));
}

// 7.2.3 Temporal.Duration.compare ( one, two [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration.compare
JS_DEFINE_NATIVE_FUNCTION(DurationConstructor::compare)
{
    // 1. Set one to ? ToTemporalDuration(one).
    auto* one = TRY(to_temporal_duration(global_object, vm.argument(0)));

    // 2. Set two to ? ToTemporalDuration(two).
    auto* two = TRY(to_temporal_duration(global_object, vm.argument(1)));

    // 3. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(2)));

    // 4. Let relativeTo be ? ToRelativeTemporalObject(options).
    auto relative_to = TRY(to_relative_temporal_object(global_object, *options));

    // 5. Let shift1 be ? CalculateOffsetShift(relativeTo, one.[[Years]], one.[[Months]], one.[[Weeks]], one.[[Days]], 0, 0, 0, 0, 0, 0, 0).
    auto shift1 = TRY(calculate_offset_shift(global_object, relative_to, one->years(), one->months(), one->weeks(), one->days(), 0, 0, 0, 0, 0, 0));

    // 6. Let shift2 be ? CalculateOffsetShift(relativeTo, two.[[Years]], two.[[Months]], two.[[Weeks]], two.[[Days]], 0, 0, 0, 0, 0, 0, 0).
    auto shift2 = TRY(calculate_offset_shift(global_object, relative_to, two->years(), two->months(), two->weeks(), two->days(), 0, 0, 0, 0, 0, 0));

    double days1;
    double days2;

    // 7. If any of one.[[Years]], two.[[Years]], one.[[Months]], two.[[Months]], one.[[Weeks]], or two.[[Weeks]] are not 0, then
    if (one->years() != 0 || two->years() != 0 || one->months() != 0 || two->months() != 0 || one->weeks() != 0 || two->weeks() != 0) {
        // a. Let unbalanceResult1 be ? UnbalanceDurationRelative(one.[[Years]], one.[[Months]], one.[[Weeks]], one.[[Days]], "day", relativeTo).
        auto unbalance_result1 = TRY(unbalance_duration_relative(global_object, one->years(), one->months(), one->weeks(), one->days(), "day", relative_to));

        // b. Let unbalanceResult2 be ? UnbalanceDurationRelative(two.[[Years]], two.[[Months]], two.[[Weeks]], two.[[Days]], "day", relativeTo).
        auto unbalance_result2 = TRY(unbalance_duration_relative(global_object, two->years(), two->months(), two->weeks(), two->days(), "day", relative_to));

        // c. Let days1 be unbalanceResult1.[[Days]].
        days1 = unbalance_result1.days;

        // d. Let days2 be unbalanceResult2.[[Days]].
        days2 = unbalance_result2.days;
    }
    // 8. Else,
    else {
        // a. Let days1 be one.[[Days]].
        days1 = one->days();

        // b. Let days2 be two.[[Days]].
        days2 = two->days();
    }

    // 9. Let ns1 be ! TotalDurationNanoseconds(days1, one.[[Hours]], one.[[Minutes]], one.[[Seconds]], one.[[Milliseconds]], one.[[Microseconds]], one.[[Nanoseconds]], shift1).
    auto ns1 = total_duration_nanoseconds(days1, one->hours(), one->minutes(), one->seconds(), one->milliseconds(), one->microseconds(), Crypto::SignedBigInteger::create_from((i64)one->nanoseconds()), shift1);

    // 10. Let ns2 be ! TotalDurationNanoseconds(days2, two.[[Hours]], two.[[Minutes]], two.[[Seconds]], two.[[Milliseconds]], two.[[Microseconds]], two.[[Nanoseconds]], shift2).
    auto ns2 = total_duration_nanoseconds(days2, two->hours(), two->minutes(), two->seconds(), two->milliseconds(), two->microseconds(), Crypto::SignedBigInteger::create_from((i64)two->nanoseconds()), shift2);

    // 11. If ns1 > ns2, return 1𝔽.
    if (ns1 > ns2)
        return Value(1);

    // 12. If ns1 < ns2, return -1𝔽.
    if (ns1 < ns2)
        return Value(-1);

    // 13. Return +0𝔽.
    return Value(0);
}

}
