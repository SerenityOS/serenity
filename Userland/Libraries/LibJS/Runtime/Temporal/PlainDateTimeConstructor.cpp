/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <LibJS/Runtime/GlobalObject.h>
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

    // 2. Let isoYear be ? ToIntegerOrInfinity(isoYear).
    auto iso_year = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 3. If isoYear is +∞ or -∞, throw a RangeError exception.
    if (Value(iso_year).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 4. Let isoMonth be ? ToIntegerOrInfinity(isoMonth).
    auto iso_month = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 5. If isoMonth is +∞ or -∞, throw a RangeError exception.
    if (Value(iso_month).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 6. Let isoDay be ? ToIntegerOrInfinity(isoDay).
    auto iso_day = vm.argument(2).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 7. If isoDay is +∞ or -∞, throw a RangeError exception.
    if (Value(iso_day).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 8. Let hour be ? ToIntegerOrInfinity(hour).
    auto hour = vm.argument(3).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 9. If hour is +∞ or -∞, throw a RangeError exception.
    if (Value(hour).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // Let minute be ? ToIntegerOrInfinity(minute).
    auto minute = vm.argument(4).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 11. If minute is +∞ or -∞, throw a RangeError exception.
    if (Value(minute).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 12. Let second be ? ToIntegerOrInfinity(second).
    auto second = vm.argument(5).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 13. If second is +∞ or -∞, throw a RangeError exception.
    if (Value(second).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 14. Let millisecond be ? ToIntegerOrInfinity(millisecond).
    auto millisecond = vm.argument(6).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 15. If millisecond is +∞ or -∞, throw a RangeError exception.
    if (Value(millisecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 16. Let microsecond be ? ToIntegerOrInfinity(microsecond).
    auto microsecond = vm.argument(7).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 17. If microsecond is +∞ or -∞, throw a RangeError exception.
    if (Value(microsecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 18. Let nanosecond be ? ToIntegerOrInfinity(nanosecond).
    auto nanosecond = vm.argument(8).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 19. If nanosecond is +∞ or -∞, throw a RangeError exception.
    if (Value(nanosecond).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 20. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
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

    // 21. Return ? CreateTemporalDateTime(isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond, calendar, NewTarget).
    return create_temporal_date_time(global_object, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond, *calendar, &new_target);
}

}
