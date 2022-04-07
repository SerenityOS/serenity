/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>

namespace JS::Temporal {

// 6.1 The Temporal.ZonedDateTime Constructor, https://tc39.es/proposal-temporal/#sec-temporal-zoneddatetime-constructor
ZonedDateTimeConstructor::ZonedDateTimeConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.ZonedDateTime.as_string(), *global_object.function_prototype())
{
}

void ZonedDateTimeConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 6.2.1 Temporal.ZonedDateTime.prototype, https://tc39.es/proposal-temporal/#sec-temporal-zoneddatetime-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_zoned_date_time_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.compare, compare, 2, attr);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 6.1.1 Temporal.ZonedDateTime ( epochNanoseconds, timeZoneLike [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime
ThrowCompletionOr<Value> ZonedDateTimeConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, then
    // a. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.ZonedDateTime");
}

// 6.1.1 Temporal.ZonedDateTime ( epochNanoseconds, timeZoneLike [ , calendarLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime
ThrowCompletionOr<Object*> ZonedDateTimeConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Set epochNanoseconds to ? ToBigInt(epochNanoseconds).
    auto* epoch_nanoseconds = TRY(vm.argument(0).to_bigint(global_object));

    // 3. If ! IsValidEpochNanoseconds(epochNanoseconds) is false, throw a RangeError exception.
    if (!is_valid_epoch_nanoseconds(*epoch_nanoseconds))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidEpochNanoseconds);

    // 4. Let timeZone be ? ToTemporalTimeZone(timeZoneLike).
    auto* time_zone = TRY(to_temporal_time_zone(global_object, vm.argument(1)));

    // 5. Let calendar be ? ToTemporalCalendarWithISODefault(calendarLike).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, vm.argument(2)));

    // 6. Return ? CreateTemporalZonedDateTime(epochNanoseconds, timeZone, calendar, NewTarget).
    return TRY(create_temporal_zoned_date_time(global_object, *epoch_nanoseconds, *time_zone, *calendar, &new_target));
}

// 6.2.2 Temporal.ZonedDateTime.from ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.from
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimeConstructor::from)
{
    // 1. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(1)));

    auto item = vm.argument(0);

    // 2. If Type(item) is Object and item has an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (item.is_object() && is<ZonedDateTime>(item.as_object())) {
        auto& item_object = static_cast<ZonedDateTime&>(item.as_object());

        // a. Perform ? ToTemporalOverflow(options).
        (void)TRY(to_temporal_overflow(global_object, options));

        // b. Perform ? ToTemporalDisambiguation(options).
        (void)TRY(to_temporal_disambiguation(global_object, options));

        // c. Perform ? ToTemporalOffset(options, "reject").
        (void)TRY(to_temporal_offset(global_object, options, "reject"));

        // d. Return ! CreateTemporalZonedDateTime(item.[[Nanoseconds]], item.[[TimeZone]], item.[[Calendar]]).
        return MUST(create_temporal_zoned_date_time(global_object, item_object.nanoseconds(), item_object.time_zone(), item_object.calendar()));
    }

    // 3. Return ? ToTemporalZonedDateTime(item, options).
    return TRY(to_temporal_zoned_date_time(global_object, item, options));
}

// 6.2.3 Temporal.ZonedDateTime.compare ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.compare
JS_DEFINE_NATIVE_FUNCTION(ZonedDateTimeConstructor::compare)
{
    // 1. Set one to ? ToTemporalZonedDateTime(one).
    auto* one = TRY(to_temporal_zoned_date_time(global_object, vm.argument(0)));

    // 2. Set two to ? ToTemporalZonedDateTime(two).
    auto* two = TRY(to_temporal_zoned_date_time(global_object, vm.argument(1)));

    // 3. Return 𝔽(! CompareEpochNanoseconds(one.[[Nanoseconds]], two.[[Nanoseconds]])).
    return Value(compare_epoch_nanoseconds(one->nanoseconds(), two->nanoseconds()));
}

}
