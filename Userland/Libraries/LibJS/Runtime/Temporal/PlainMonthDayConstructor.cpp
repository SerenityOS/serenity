/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
}

// 10.1.1 Temporal.PlainMonthDay ( isoMonth, isoDay [ , calendarLike [ , referenceISOYear ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday
ThrowCompletionOr<Value> PlainMonthDayConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainMonthDay");
}

// 10.1.1 Temporal.PlainMonthDay ( isoMonth, isoDay [ , calendarLike [ , referenceISOYear ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday
ThrowCompletionOr<Object*> PlainMonthDayConstructor::construct(FunctionObject& new_target)
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

    // 3. Let m be ? ToIntegerThrowOnInfinity(isoMonth).
    auto m = TRY(to_integer_throw_on_infinity(global_object, iso_month, ErrorType::TemporalInvalidPlainMonthDay));

    // 4. Let d be ? ToIntegerThrowOnInfinity(isoDay).
    auto d = TRY(to_integer_throw_on_infinity(global_object, iso_day, ErrorType::TemporalInvalidPlainMonthDay));

    // 5. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, calendar_like));

    // 6. Let ref be ? ToIntegerThrowOnInfinity(referenceISOYear).
    auto ref = TRY(to_integer_throw_on_infinity(global_object, reference_iso_year, ErrorType::TemporalInvalidPlainMonthDay));

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behavior as the call to CreateTemporalMonthDay will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(ref) || !AK::is_within_range<u8>(m) || !AK::is_within_range<u8>(d))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 7. Return ? CreateTemporalMonthDay(m, d, calendar, ref, NewTarget).
    return TRY(create_temporal_month_day(global_object, m, d, *calendar, ref, &new_target));
}

// 10.2.2 Temporal.PlainMonthDay.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.from
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayConstructor::from)
{
    auto item = vm.argument(0);

    // 1. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 2. If Type(item) is Object and item has an [[InitializedTemporalMonthDay]] internal slot, then
    if (item.is_object() && is<PlainMonthDay>(item.as_object())) {
        // a. Perform ? ToTemporalOverflow(options).
        (void)TRY(to_temporal_overflow(global_object, options));

        auto& plain_month_day_object = static_cast<PlainMonthDay&>(item.as_object());

        // b. Return ? CreateTemporalMonthDay(item.[[ISOMonth]], item.[[ISODay]], item.[[Calendar]], item.[[ISOYear]]).
        return TRY(create_temporal_month_day(global_object, plain_month_day_object.iso_month(), plain_month_day_object.iso_day(), plain_month_day_object.calendar(), plain_month_day_object.iso_year()));
    }

    // 3. Return ? ToTemporalMonthDay(item, options).
    return TRY(to_temporal_month_day(global_object, item, options));
}

}
