/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateConstructor.h>

namespace JS::Temporal {

// 3.1 The Temporal.PlainDate Constructor, https://tc39.es/proposal-temporal/#sec-temporal-plaindate-constructor
PlainDateConstructor::PlainDateConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PlainDate.as_string(), *global_object.function_prototype())
{
}

void PlainDateConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 3.2.1 Temporal.PlainDate.prototype, https://tc39.es/proposal-temporal/#sec-temporal-plaindate-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_plain_date_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.compare, compare, 2, attr);

    define_direct_property(vm.names.length, Value(3), Attribute::Configurable);
}

// 3.1.1 Temporal.PlainDate ( isoYear, isoMonth, isoDay [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate
Value PlainDateConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.PlainDate");
    return {};
}

// 3.1.1 Temporal.PlainDate ( isoYear, isoMonth, isoDay [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate
Value PlainDateConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let y be ? ToIntegerOrInfinity(isoYear).
    auto y = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    // 3. If y is +∞ or -∞, throw a RangeError exception.
    if (Value(y).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 4. Let m be ? ToIntegerOrInfinity(isoMonth).
    auto m = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    // 5. If m is +∞ or -∞, throw a RangeError exception.
    if (Value(m).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 6. Let d be ? ToIntegerOrInfinity(isoDay).
    auto d = vm.argument(2).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    // 7. If d is +∞ or -∞, throw a RangeError exception.
    if (Value(d).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 8. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = to_temporal_calendar_with_iso_default(global_object, vm.argument(3));
    if (vm.exception())
        return {};

    // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
    // This does not change the exposed behaviour as the call to CreateTemporalDate will immediately check that these values are valid
    // ISO values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
    if (!AK::is_within_range<i32>(y) || !AK::is_within_range<u8>(m) || !AK::is_within_range<u8>(d)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 9. Return ? CreateTemporalDate(y, m, d, calendar, NewTarget).
    return create_temporal_date(global_object, y, m, d, *calendar, &new_target);
}

// 3.2.2 Temporal.PlainDate.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.from
JS_DEFINE_NATIVE_FUNCTION(PlainDateConstructor::from)
{
    // 1. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    auto item = vm.argument(0);
    // 2. If Type(item) is Object and item has an [[InitializedTemporalDate]] internal slot, then
    if (item.is_object() && is<PlainDate>(item.as_object())) {
        auto& plain_date_item = static_cast<PlainDate&>(item.as_object());
        // a. Perform ? ToTemporalOverflow(options).
        (void)to_temporal_overflow(global_object, *options);
        if (vm.exception())
            return {};
        // b. Return ? CreateTemporalDate(item.[[ISOYear]], item.[[ISOMonth]], item.[[ISODay]], item.[[Calendar]]).
        return create_temporal_date(global_object, plain_date_item.iso_year(), plain_date_item.iso_month(), plain_date_item.iso_day(), plain_date_item.calendar());
    }

    // 3. Return ? ToTemporalDate(item, options).
    return to_temporal_date(global_object, item, options);
}

// 3.2.3 Temporal.PlainDate.compare ( one, two ), https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindate-constructor
JS_DEFINE_NATIVE_FUNCTION(PlainDateConstructor::compare)
{
    // 1. Set one to ? ToTemporalDate(one).
    auto* one = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};
    // 2. Set two to ? ToTemporalDate(two).
    auto* two = to_temporal_date(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    // 3. Return 𝔽(! CompareISODate(one.[[ISOYear]], one.[[ISOMonth]], one.[[ISODay]], two.[[ISOYear]], two.[[ISOMonth]], two.[[ISODay]])).
    return Value(compare_iso_date(one->iso_year(), one->iso_month(), one->iso_day(), two->iso_year(), two->iso_month(), two->iso_day()));
}

}
