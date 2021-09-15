/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.compare, compare, 2, attr);

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
    // This does not change the exposed behavior as the call to CreateTemporalTime will immediately check that these values are valid
    // ISO values (for hours: 0 - 23, for minutes and seconds: 0 - 59, milliseconds, microseconds, and nanoseconds: 0 - 999) all of which
    // are subsets of this check.
    if (!AK::is_within_range<u8>(hour) || !AK::is_within_range<u8>(minute) || !AK::is_within_range<u8>(second) || !AK::is_within_range<u16>(millisecond) || !AK::is_within_range<u16>(microsecond) || !AK::is_within_range<u16>(nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);
        return {};
    }

    // 8. Return ? CreateTemporalTime(hour, minute, second, millisecond, microsecond, nanosecond, NewTarget).
    return TRY_OR_DISCARD(create_temporal_time(global_object, hour, minute, second, millisecond, microsecond, nanosecond, &new_target));
}

// 4.2.2 Temporal.PlainTime.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.from
JS_DEFINE_NATIVE_FUNCTION(PlainTimeConstructor::from)
{
    // 1. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 2. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = to_temporal_overflow(global_object, *options);
    if (vm.exception())
        return {};

    auto item = vm.argument(0);

    // 3. If Type(item) is Object and item has an [[InitializedTemporalTime]] internal slot, then
    if (item.is_object() && is<PlainTime>(item.as_object())) {
        auto& plain_time = static_cast<PlainTime&>(item.as_object());
        // a. Return ? CreateTemporalTime(item.[[ISOHour]], item.[[ISOMinute]], item.[[ISOSecond]], item.[[ISOMillisecond]], item.[[ISOMicrosecond]], item.[[ISONanosecond]]).
        return TRY_OR_DISCARD(create_temporal_time(global_object, plain_time.iso_hour(), plain_time.iso_minute(), plain_time.iso_second(), plain_time.iso_millisecond(), plain_time.iso_microsecond(), plain_time.iso_nanosecond()));
    }

    // 4. Return ? ToTemporalTime(item, overflow).
    return TRY_OR_DISCARD(to_temporal_time(global_object, item, *overflow));
}

// 4.2.3 Temporal.PlainTime.compare ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.compare
JS_DEFINE_NATIVE_FUNCTION(PlainTimeConstructor::compare)
{
    // 1. Set one to ? ToTemporalTime(one).
    auto* one = TRY_OR_DISCARD(to_temporal_time(global_object, vm.argument(0)));

    // 2. Set two to ? ToTemporalTime(two).
    auto* two = TRY_OR_DISCARD(to_temporal_time(global_object, vm.argument(1)));

    // 3. Return 𝔽(! CompareTemporalTime(one.[[ISOHour]], one.[[ISOMinute]], one.[[ISOSecond]], one.[[ISOMillisecond]], one.[[ISOMicrosecond]], one.[[ISONanosecond]], two.[[ISOHour]], two.[[ISOMinute]], two.[[ISOSecond]], two.[[ISOMillisecond]], two.[[ISOMicrosecond]], two.[[ISONanosecond]])).
    return Value(compare_temporal_time(one->iso_hour(), one->iso_minute(), one->iso_second(), one->iso_millisecond(), one->iso_microsecond(), one->iso_nanosecond(), two->iso_hour(), two->iso_minute(), two->iso_second(), two->iso_millisecond(), two->iso_microsecond(), two->iso_nanosecond()));
}

}
