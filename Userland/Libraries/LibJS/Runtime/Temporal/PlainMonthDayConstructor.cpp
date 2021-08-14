/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayConstructor.h>

namespace JS::Temporal {

// 10.1 The Temporal.PlainMonthDay Constructor, https://tc39.es/proposal-temporal/#sec-temporal-plainmonthday-constructor
PlainMonthDayConstructor::PlainMonthDayConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PlainMonthDay.as_string(), *global_object.function_prototype())
{
}

void PlainMonthDayConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 10.2.1 Temporal.PlainMonthDay.prototype, https://tc39.es/proposal-temporal/#sec-temporal-plainmonthday-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_plain_month_day_prototype(), 0);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 10.1.1 Temporal.PlainMonthDay ( isoMonth, isoDay [ , calendarLike [ , referenceISOYear ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday
Value PlainMonthDayConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainMonthDay");
    return {};
}

// 10.1.1 Temporal.PlainMonthDay ( isoMonth, isoDay [ , calendarLike [ , referenceISOYear ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday
Value PlainMonthDayConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto iso_month = vm.argument(0);
    auto iso_day = vm.argument(1);
    auto calendar_like = vm.argument(2);
    auto reference_iso_year = vm.argument(3);

    // 2. If referenceISOYear is undefined, then
    if (reference_iso_year.is_undefined()) {
        // a. Set referenceISOYear to 1972𝔽.
        reference_iso_year = Value(1972);
    }

    // 3. Let m be ? ToIntegerOrInfinity(isoMonth).
    auto m = iso_month.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 4. If m is +∞ or -∞, throw a RangeError exception.
    if (Value(m).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainMonthDay);
        return {};
    }

    // 5. Let d be ? ToIntegerOrInfinity(isoDay).
    auto d = iso_day.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 6. If d is +∞ or -∞, throw a RangeError exception.
    if (Value(d).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainMonthDay);
        return {};
    }

    // 7. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = to_temporal_calendar_with_iso_default(global_object, calendar_like);
    if (vm.exception())
        return {};

    // 8. Let ref be ? ToIntegerOrInfinity(referenceISOYear).
    auto ref = reference_iso_year.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 9. If ref is +∞ or -∞, throw a RangeError exception.
    if (Value(ref).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainMonthDay);
        return {};
    }

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalMonthDay will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(ref) || !AK::is_within_range<u8>(m) || !AK::is_within_range<u8>(d)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 10. Return ? CreateTemporalMonthDay(m, d, calendar, ref, NewTarget).
    return create_temporal_month_day(global_object, m, d, *calendar, ref, &new_target);
}

}
