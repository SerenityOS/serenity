/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainDateTimeConstructor.h>

namespace JS::Temporal {

// 5.1 The Temporal.PlainDateTime Constructor, https://tc39.es/proposal-temporal/#sec-temporal-plaindatetime-constructor
PlainDateTimeConstructor::PlainDateTimeConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PlainDateTime.as_string(), *global_object.function_prototype())
{
}

void PlainDateTimeConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 5.2.1 Temporal.PlainDateTime.prototype, https://tc39.es/proposal-temporal/#sec-temporal-plaindatetime-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_plain_date_time_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);

    define_direct_property(vm.names.length, Value(3), Attribute::Configurable);
}

// 5.1.1 Temporal.PlainDateTime ( isoYear, isoMonth, isoDay [ , hour [ , minute [ , second [ , millisecond [ , microsecond [ , nanosecond [ , calendarLike ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime
Value PlainDateTimeConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainDateTime");
    return {};
}

// 5.1.1 Temporal.PlainDateTime ( isoYear, isoMonth, isoDay [ , hour [ , minute [ , second [ , millisecond [ , microsecond [ , nanosecond [ , calendarLike ] ] ] ] ] ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime
Value PlainDateTimeConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let isoYear be ? ToIntegerThrowOnInfinity(isoYear).
    auto iso_year = to_integer_throw_on_infinity(global_object, vm.argument(0), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 3. Let isoMonth be ? ToIntegerThrowOnInfinity(isoMonth).
    auto iso_month = to_integer_throw_on_infinity(global_object, vm.argument(1), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 4. Let isoDay be ? ToIntegerThrowOnInfinity(isoDay).
    auto iso_day = to_integer_throw_on_infinity(global_object, vm.argument(2), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 5. Let hour be ? ToIntegerThrowOnInfinity(hour).
    auto hour = to_integer_throw_on_infinity(global_object, vm.argument(3), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 6. Let minute be ? ToIntegerThrowOnInfinity(minute).
    auto minute = to_integer_throw_on_infinity(global_object, vm.argument(4), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 7. Let second be ? ToIntegerThrowOnInfinity(second).
    auto second = to_integer_throw_on_infinity(global_object, vm.argument(5), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 8. Let millisecond be ? ToIntegerThrowOnInfinity(millisecond).
    auto millisecond = to_integer_throw_on_infinity(global_object, vm.argument(6), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 9. Let microsecond be ? ToIntegerThrowOnInfinity(microsecond).
    auto microsecond = to_integer_throw_on_infinity(global_object, vm.argument(7), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 10. Let nanosecond be ? ToIntegerThrowOnInfinity(nanosecond).
    auto nanosecond = to_integer_throw_on_infinity(global_object, vm.argument(8), ErrorType::TemporalInvalidPlainDateTime);
    if (vm.exception())
        return {};

    // 11. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = to_temporal_calendar_with_iso_default(global_object, vm.argument(9));
    if (vm.exception())
        return {};

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalDateTime will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31, for hours: 0 - 23, for minutes and seconds: 0 - 59,
    // milliseconds, microseconds, and nanoseconds: 0 - 999) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(iso_year) || !AK::is_within_range<u8>(iso_month) || !AK::is_within_range<u8>(iso_day) || !AK::is_within_range<u8>(hour) || !AK::is_within_range<u8>(minute) || !AK::is_within_range<u8>(second) || !AK::is_within_range<u16>(millisecond) || !AK::is_within_range<u16>(microsecond) || !AK::is_within_range<u16>(nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 12. Return ? CreateTemporalDateTime(isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond, calendar, NewTarget).
    return create_temporal_date_time(global_object, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond, *calendar, &new_target);
}

// 5.2.2 Temporal.PlainDateTime.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.from
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimeConstructor::from)
{
    auto item = vm.argument(0);

    // 1. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 2. If Type(item) is Object and item has an [[InitializedTemporalDateTime]] internal slot, then
    if (item.is_object() && is<PlainDateTime>(item.as_object())) {
        auto& plain_date_time = static_cast<PlainDateTime&>(item.as_object());

        // a. Perform ? ToTemporalOverflow(options).
        (void)to_temporal_overflow(global_object, *options);
        if (vm.exception())
            return {};

        // b. Return ? CreateTemporalDateTime(item.[[ISOYear]], item.[[ISOMonth]], item.[[ISODay]], item.[[ISOHour]], item.[[ISOMinute]], item.[[ISOSecond]], item.[[ISOMillisecond]], item.[[ISOMicrosecond]], item.[[ISONanosecond]], item.[[Calendar]]).
        return create_temporal_date_time(global_object, plain_date_time.iso_year(), plain_date_time.iso_month(), plain_date_time.iso_day(), plain_date_time.iso_hour(), plain_date_time.iso_minute(), plain_date_time.iso_second(), plain_date_time.iso_millisecond(), plain_date_time.iso_microsecond(), plain_date_time.iso_nanosecond(), plain_date_time.calendar());
    }

    // 3. Return ? ToTemporalDateTime(item, options).
    return to_temporal_date_time(global_object, item, options);
}

}
