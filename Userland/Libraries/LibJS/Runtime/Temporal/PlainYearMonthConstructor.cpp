/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthConstructor.h>

namespace JS::Temporal {

// 9.1 The Temporal.PlainYearMonth Constructor, https://tc39.es/proposal-temporal/#sec-temporal-plainyearmonth-constructor
PlainYearMonthConstructor::PlainYearMonthConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PlainYearMonth.as_string(), *global_object.function_prototype())
{
}

void PlainYearMonthConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 9.2.1 Temporal.PlainYearMonth.prototype, https://tc39.es/proposal-temporal/#sec-temporal-plainyearmonth-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_plain_year_month_prototype(), 0);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 9.1.1 Temporal.PlainYearMonth ( isoYear, isoMonth [ , calendarLike [ , referenceISODay ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth
Value PlainYearMonthConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainYearMonth");
    return {};
}

// 9.1.1 Temporal.PlainYearMonth ( isoYear, isoMonth [ , calendarLike [ , referenceISODay ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth
Value PlainYearMonthConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto iso_year = vm.argument(0);
    auto iso_month = vm.argument(1);
    auto calendar_like = vm.argument(2);
    auto reference_iso_day = vm.argument(3);

    // 2. If referenceISODay is undefined, then
    if (reference_iso_day.is_undefined()) {
        // a. Set referenceISODay to 1𝔽.
        reference_iso_day = Value(1);
    }

    // 3. Let y be ? ToIntegerOrInfinity(isoYear).
    auto y = iso_year.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 4. If y is +∞ or -∞, throw a RangeError exception.
    if (Value(y).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 5. Let m be ? ToIntegerOrInfinity(isoMonth).
    auto m = iso_month.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 6. If m is +∞ or -∞, throw a RangeError exception.
    if (Value(m).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 7. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = to_temporal_calendar_with_iso_default(global_object, calendar_like);
    if (vm.exception())
        return {};

    // 8. Let ref be ? ToIntegerOrInfinity(referenceISODay).
    auto ref = reference_iso_day.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 9. If ref is +∞ or -∞, throw a RangeError exception.
    if (Value(ref).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalYearMonth will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(y) || !AK::is_within_range<u8>(m) || !AK::is_within_range<u8>(ref)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 10. Return ? CreateTemporalYearMonth(y, m, calendar, ref, NewTarget).
    return create_temporal_year_month(global_object, y, m, *calendar, ref, &new_target);
}

}
