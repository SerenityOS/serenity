/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.compare, compare, 2, attr);
}

// 9.1.1 Temporal.PlainYearMonth ( isoYear, isoMonth [ , calendarLike [ , referenceISODay ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth
ThrowCompletionOr<Value> PlainYearMonthConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainYearMonth");
}

// 9.1.1 Temporal.PlainYearMonth ( isoYear, isoMonth [ , calendarLike [ , referenceISODay ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth
ThrowCompletionOr<Object*> PlainYearMonthConstructor::construct(FunctionObject& new_target)
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

    // 3. Let y be ? ToIntegerThrowOnInfinity(isoYear).
    auto y = TRY(to_integer_throw_on_infinity(global_object, iso_year, ErrorType::TemporalInvalidPlainYearMonth));

    // 4. Let m be ? ToIntegerThrowOnInfinity(isoMonth).
    auto m = TRY(to_integer_throw_on_infinity(global_object, iso_month, ErrorType::TemporalInvalidPlainYearMonth));

    // 5. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, calendar_like));

    // 6. Let ref be ? ToIntegerThrowOnInfinity(referenceISODay).
    auto ref = TRY(to_integer_throw_on_infinity(global_object, reference_iso_day, ErrorType::TemporalInvalidPlainYearMonth));

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behavior as the call to CreateTemporalYearMonth will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(y) || !AK::is_within_range<u8>(m) || !AK::is_within_range<u8>(ref))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 7. Return ? CreateTemporalYearMonth(y, m, calendar, ref, NewTarget).
    return TRY(create_temporal_year_month(global_object, y, m, *calendar, ref, &new_target));
}

// 9.2.2 Temporal.PlainYearMonth.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.from
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthConstructor::from)
{
    // 1. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    auto item = vm.argument(0);

    // 2. If Type(item) is Object and item has an [[InitializedTemporalYearMonth]] internal slot, then
    if (item.is_object() && is<PlainYearMonth>(item.as_object())) {
        // a. Perform ? ToTemporalOverflow(options).
        (void)TRY(to_temporal_overflow(global_object, options));

        auto& plain_year_month_object = static_cast<PlainYearMonth&>(item.as_object());

        // b. Return ? CreateTemporalYearMonth(item.[[ISOYear]], item.[[ISOMonth]], item.[[Calendar]], item.[[ISODay]]).
        return TRY(create_temporal_year_month(global_object, plain_year_month_object.iso_year(), plain_year_month_object.iso_month(), plain_year_month_object.calendar(), plain_year_month_object.iso_day()));
    }

    // 3. Return ? ToTemporalYearMonth(item, options).
    return TRY(to_temporal_year_month(global_object, item, options));
}

// 9.2.3 Temporal.PlainYearMonth.compare ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.compare
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthConstructor::compare)
{
    // 1. Set one to ? ToTemporalYearMonth(one).
    auto* one = TRY(to_temporal_year_month(global_object, vm.argument(0)));

    // 2. Set two to ? ToTemporalYearMonth(one).
    auto* two = TRY(to_temporal_year_month(global_object, vm.argument(1)));

    // 3. Return 𝔽(! CompareISODate(one.[[ISOYear]], one.[[ISOMonth]], one.[[ISODay]], two.[[ISOYear]], two.[[ISOMonth]], two.[[ISODay]])).
    return Value(compare_iso_date(one->iso_year(), one->iso_month(), one->iso_day(), two->iso_year(), two->iso_month(), two->iso_day()));
}

}
