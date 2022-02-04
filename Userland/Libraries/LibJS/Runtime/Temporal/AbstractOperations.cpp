/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/DateTimeLexer.h>
#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <stdlib.h>

namespace JS::Temporal {

static Optional<OptionType> to_option_type(Value value)
{
    if (value.is_boolean())
        return OptionType::Boolean;
    if (value.is_string())
        return OptionType::String;
    if (value.is_number())
        return OptionType::Number;
    return {};
}

// 13.1 IterableToListOfType ( items, elementTypes ), https://tc39.es/proposal-temporal/#sec-iterabletolistoftype
ThrowCompletionOr<MarkedValueList> iterable_to_list_of_type(GlobalObject& global_object, Value items, Vector<OptionType> const& element_types)
{
    auto& vm = global_object.vm();
    auto& heap = global_object.heap();

    // 1. Let iteratorRecord be ? GetIterator(items, sync).
    auto iterator_record = TRY(get_iterator(global_object, items, IteratorHint::Sync));

    // 2. Let values be a new empty List.
    MarkedValueList values(heap);

    // 3. Let next be true.
    auto next = true;
    // 4. Repeat, while next is not false,
    while (next) {
        // a. Set next to ? IteratorStep(iteratorRecord).
        auto* iterator_result = TRY(iterator_step(global_object, iterator_record));
        next = iterator_result;

        // b. If next is not false, then
        if (next) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = TRY(iterator_value(global_object, *iterator_result));
            // ii. If Type(nextValue) is not an element of elementTypes, then
            if (auto type = to_option_type(next_value); !type.has_value() || !element_types.contains_slow(*type)) {
                // 1. Let completion be ThrowCompletion(a newly created TypeError object).
                auto completion = vm.throw_completion<TypeError>(global_object, ErrorType::IterableToListOfTypeInvalidValue, next_value.to_string_without_side_effects());
                // 2. Return ? IteratorClose(iteratorRecord, completion).
                return iterator_close(global_object, iterator_record, move(completion));
            }
            // iii. Append nextValue to the end of the List values.
            values.append(next_value);
        }
    }

    // 5. Return values.
    return { move(values) };
}

// 13.2 GetOptionsObject ( options ), https://tc39.es/proposal-temporal/#sec-getoptionsobject
ThrowCompletionOr<Object*> get_options_object(GlobalObject& global_object, Value options)
{
    auto& vm = global_object.vm();

    // 1. If options is undefined, then
    if (options.is_undefined()) {
        // a. Return ! OrdinaryObjectCreate(null).
        return Object::create(global_object, nullptr);
    }

    // 2. If Type(options) is Object, then
    if (options.is_object()) {
        // a. Return options.
        return &options.as_object();
    }

    // 3. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, "Options");
}

// 13.3 GetOption ( options, property, types, values, fallback ), https://tc39.es/proposal-temporal/#sec-getoption
ThrowCompletionOr<Value> get_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback)
{
    VERIFY(property.is_string());

    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.
    // 2. Assert: Each element of types is Boolean, String, or Number.

    // 3. Let value be ? Get(options, property).
    auto value = TRY(options.get(property));

    // 4. If value is undefined, return fallback.
    if (value.is_undefined())
        return fallback;

    OptionType type;
    // 5. If types contains Type(value), then
    if (auto value_type = to_option_type(value); value_type.has_value() && types.contains_slow(*value_type)) {
        // a. Let type be Type(value).
        type = *value_type;
    }
    // 6. Else,
    else {
        // a. Let type be the last element of types.
        type = types.last();
    }

    // 7. If type is Boolean, then
    if (type == OptionType::Boolean) {
        // a. Set value to ! ToBoolean(value).
        value = Value(value.to_boolean());
    }
    // 8. Else if type is Number, then
    else if (type == OptionType::Number) {
        // a. Set value to ? ToNumber(value).
        value = TRY(value.to_number(global_object));

        // b. If value is NaN, throw a RangeError exception.
        if (value.is_nan())
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, vm.names.NaN.as_string(), property.as_string());
    }
    // 9. Else,
    else {
        // a. Set value to ? ToString(value).
        value = TRY(value.to_primitive_string(global_object));
    }

    // 10. If values is not empty, then
    if (!values.is_empty()) {
        VERIFY(value.is_string());
        // a. If values does not contain value, throw a RangeError exception.
        if (!values.contains_slow(value.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_string().string(), property.as_string());
    }

    // 11. Return value.
    return value;
}

// 13.4 GetStringOrNumberOption ( options, property, stringValues, minimum, maximum, fallback ), https://tc39.es/proposal-temporal/#sec-getstringornumberoption
template<typename NumberType>
ThrowCompletionOr<Variant<String, NumberType>> get_string_or_number_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Vector<StringView> const& string_values, NumberType minimum, NumberType maximum, Value fallback)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.

    // 2. Let value be ? GetOption(options, property, « Number, String », empty, fallback).
    auto value = TRY(get_option(global_object, options, property, { OptionType::Number, OptionType::String }, {}, fallback));

    // 3. If Type(value) is Number, then
    if (value.is_number()) {
        // a. If value < minimum or value > maximum, throw a RangeError exception.
        if (value.as_double() < minimum || value.as_double() > maximum)
            return vm.template throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_double(), property.as_string());

        // b. Return floor(ℝ(value)).
        return static_cast<NumberType>(floor(value.as_double()));
    }

    // 4. Assert: Type(value) is String.
    VERIFY(value.is_string());

    // 5. If stringValues does not contain value, throw a RangeError exception.
    if (!string_values.contains_slow(value.as_string().string()))
        return vm.template throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_string().string(), property.as_string());

    // 6. Return value.
    return value.as_string().string();
}

// 13.5 ToTemporalOverflow ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaloverflow
ThrowCompletionOr<String> to_temporal_overflow(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "overflow", « String », « "constrain", "reject" », "constrain").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.overflow, { OptionType::String }, { "constrain"sv, "reject"sv }, js_string(vm, "constrain")));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.6 ToTemporalDisambiguation ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldisambiguation
ThrowCompletionOr<String> to_temporal_disambiguation(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "disambiguation", « String », « "compatible", "earlier", "later", "reject" », "compatible").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.disambiguation, { OptionType::String }, { "compatible"sv, "earlier"sv, "later"sv, "reject"sv }, js_string(vm, "compatible")));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.7 ToTemporalRoundingMode ( normalizedOptions, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingmode
ThrowCompletionOr<String> to_temporal_rounding_mode(GlobalObject& global_object, Object const& normalized_options, String const& fallback)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "roundingMode", « String », « "ceil", "floor", "trunc", "halfExpand" », fallback).
    auto option = TRY(get_option(global_object, normalized_options, vm.names.roundingMode, { OptionType::String }, { "ceil"sv, "floor"sv, "trunc"sv, "halfExpand"sv }, js_string(vm, fallback)));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.8 NegateTemporalRoundingMode ( roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-negatetemporalroundingmode
StringView negate_temporal_rounding_mode(String const& rounding_mode)
{
    // 1. If roundingMode is "ceil", return "floor".
    if (rounding_mode == "ceil"sv)
        return "floor"sv;

    // 2. If roundingMode is "floor", return "ceil".
    if (rounding_mode == "floor"sv)
        return "ceil"sv;

    // 3. Return roundingMode.
    return rounding_mode;
}

// 13.9 ToTemporalOffset ( normalizedOptions, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaloffset
ThrowCompletionOr<String> to_temporal_offset(GlobalObject& global_object, Object const& normalized_options, String const& fallback)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "offset", « String », « "prefer", "use", "ignore", "reject" », fallback).
    auto option = TRY(get_option(global_object, normalized_options, vm.names.offset, { OptionType::String }, { "prefer"sv, "use"sv, "ignore"sv, "reject"sv }, js_string(vm, fallback)));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.10 ToShowCalendarOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowcalendaroption
ThrowCompletionOr<String> to_show_calendar_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "calendarName", « String », « "auto", "always", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.calendarName, { OptionType::String }, { "auto"sv, "always"sv, "never"sv }, js_string(vm, "auto"sv)));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.11 ToShowTimeZoneNameOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowtimezonenameoption
ThrowCompletionOr<String> to_show_time_zone_name_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "timeZoneName", « String », « "auto", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.timeZoneName, { OptionType::String }, { "auto"sv, "never"sv }, js_string(vm, "auto"sv)));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.12 ToShowOffsetOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowoffsetoption
ThrowCompletionOr<String> to_show_offset_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "offset", « String », « "auto", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.offset, { OptionType::String }, { "auto"sv, "never"sv }, js_string(vm, "auto"sv)));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.13 ToTemporalRoundingIncrement ( normalizedOptions, dividend, inclusive ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingincrement
ThrowCompletionOr<u64> to_temporal_rounding_increment(GlobalObject& global_object, Object const& normalized_options, Optional<double> dividend, bool inclusive)
{
    auto& vm = global_object.vm();

    double maximum;
    // 1. If dividend is undefined, then
    if (!dividend.has_value()) {
        // a. Let maximum be +∞.
        maximum = INFINITY;
    }
    // 2. Else if inclusive is true, then
    else if (inclusive) {
        // a. Let maximum be dividend.
        maximum = *dividend;
    }
    // 3. Else if dividend is more than 1, then
    else if (*dividend > 1) {
        // a. Let maximum be dividend − 1.
        maximum = *dividend - 1;
    }
    // 4. Else,
    else {
        // a. Let maximum be 1.
        maximum = 1;
    }

    // 5. Let increment be ? GetOption(normalizedOptions, "roundingIncrement", « Number », empty, 1).
    auto increment_value = TRY(get_option(global_object, normalized_options, vm.names.roundingIncrement, { OptionType::Number }, {}, Value(1)));
    VERIFY(increment_value.is_number());
    auto increment = increment_value.as_double();

    // 6. If increment < 1 or increment > maximum, throw a RangeError exception.
    if (increment < 1 || increment > maximum)
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, increment, "roundingIncrement");

    // 7. Set increment to floor(ℝ(increment)).
    auto floored_increment = static_cast<u64>(increment);

    // 8. If dividend is not undefined and dividend modulo increment is not zero, then
    if (dividend.has_value() && static_cast<u64>(*dividend) % floored_increment != 0)
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, increment, "roundingIncrement");

    // 9. Return increment.
    return floored_increment;
}

// 13.14 ToTemporalDateTimeRoundingIncrement ( normalizedOptions, smallestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldatetimeroundingincrement
ThrowCompletionOr<u64> to_temporal_date_time_rounding_increment(GlobalObject& global_object, Object const& normalized_options, StringView smallest_unit)
{
    double maximum;

    // 1. If smallestUnit is "day", then
    if (smallest_unit == "day"sv) {
        // a. Let maximum be 1.
        maximum = 1;
    }
    // 2. Else if smallestUnit is "hour", then
    else if (smallest_unit == "hour"sv) {
        // a. Let maximum be 24.
        maximum = 24;
    }
    // 3. Else if smallestUnit is "minute" or "second", then
    else if (smallest_unit.is_one_of("minute"sv, "second"sv)) {
        // a. Let maximum be 60.
        maximum = 60;
    }
    // 4. Else,
    else {
        // a. Assert: smallestUnit is "millisecond", "microsecond", or "nanosecond".
        VERIFY(smallest_unit.is_one_of("millisecond"sv, "microsecond"sv, "nanosecond"sv));

        // b. Let maximum be 1000.
        maximum = 1000;
    }

    // 5. Return ? ToTemporalRoundingIncrement(normalizedOptions, maximum, false).
    return to_temporal_rounding_increment(global_object, normalized_options, maximum, false);
}

// 13.15 ToSecondsStringPrecision ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-tosecondsstringprecision
ThrowCompletionOr<SecondsStringPrecision> to_seconds_string_precision(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // Let smallestUnit be ? ToSmallestTemporalUnit(normalizedOptions, « "year", "month", "week", "day", "hour" », undefined).
    auto smallest_unit = TRY(to_smallest_temporal_unit(global_object, normalized_options, { "year"sv, "month"sv, "week"sv, "day"sv, "hour"sv }, {}));

    // 2. If smallestUnit is "minute", then
    if (smallest_unit == "minute"sv) {
        // a. Return the Record { [[Precision]]: "minute", [[Unit]]: "minute", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = "minute"sv, .unit = "minute"sv, .increment = 1 };
    }

    // 3. If smallestUnit is "second", then
    if (smallest_unit == "second"sv) {
        // a. Return the Record { [[Precision]]: 0, [[Unit]]: "second", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 0, .unit = "second"sv, .increment = 1 };
    }

    // 4. If smallestUnit is "millisecond", then
    if (smallest_unit == "millisecond"sv) {
        // a. Return the Record { [[Precision]]: 3, [[Unit]]: "millisecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 3, .unit = "millisecond"sv, .increment = 1 };
    }

    // 5. If smallestUnit is "microsecond", then
    if (smallest_unit == "microsecond"sv) {
        // a. Return the Record { [[Precision]]: 6, [[Unit]]: "microsecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 6, .unit = "microsecond"sv, .increment = 1 };
    }

    // 6. If smallestUnit is "nanosecond", then
    if (smallest_unit == "nanosecond"sv) {
        // a. Return the Record { [[Precision]]: 9, [[Unit]]: "nanosecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 9, .unit = "nanosecond"sv, .increment = 1 };
    }

    // 7. Assert: smallestUnit is undefined.
    VERIFY(!smallest_unit.has_value());

    // 8. Let digits be ? GetStringOrNumberOption(normalizedOptions, "fractionalSecondDigits", « "auto" », 0, 9, "auto").
    auto digits_variant = TRY(get_string_or_number_option<u8>(global_object, normalized_options, vm.names.fractionalSecondDigits, { "auto"sv }, 0, 9, js_string(vm, "auto"sv)));

    // 9. If digits is "auto", then
    if (digits_variant.has<String>()) {
        VERIFY(digits_variant.get<String>() == "auto"sv);
        // a. Return the Record { [[Precision]]: "auto", [[Unit]]: "nanosecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = "auto"sv, .unit = "nanosecond"sv, .increment = 1 };
    }

    auto digits = digits_variant.get<u8>();

    // 10. If digits is 0, then
    if (digits == 0) {
        // a. Return the Record { [[Precision]]: 0, [[Unit]]: "second", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 0, .unit = "second"sv, .increment = 1 };
    }

    // 11. If digits is 1, 2, or 3, then
    if (digits == 1 || digits == 2 || digits == 3) {
        // a. Return the Record { [[Precision]]: digits, [[Unit]]: "millisecond", [[Increment]]: 10^(3 − digits) }.
        return SecondsStringPrecision { .precision = digits, .unit = "millisecond"sv, .increment = (u32)pow(10, 3 - digits) };
    }

    // 12. If digits is 4, 5, or 6, then
    if (digits == 4 || digits == 5 || digits == 6) {
        // a. Return the Record { [[Precision]]: digits, [[Unit]]: "microsecond", [[Increment]]: 10^(6 − digits) }.
        return SecondsStringPrecision { .precision = digits, .unit = "microsecond"sv, .increment = (u32)pow(10, 6 - digits) };
    }

    // 13. Assert: digits is 7, 8, or 9.
    VERIFY(digits == 7 || digits == 8 || digits == 9);

    // 14. Return the Record { [[Precision]]: digits, [[Unit]]: "nanosecond", [[Increment]]: 10^(9 − digits) }.
    return SecondsStringPrecision { .precision = digits, .unit = "nanosecond"sv, .increment = (u32)pow(10, 9 - digits) };
}

// https://tc39.es/proposal-temporal/#table-temporal-singular-and-plural-units
static HashMap<StringView, StringView> plural_to_singular_units = {
    { "years"sv, "year"sv },
    { "months"sv, "month"sv },
    { "weeks"sv, "week"sv },
    { "days"sv, "day"sv },
    { "hours"sv, "hour"sv },
    { "minutes"sv, "minute"sv },
    { "seconds"sv, "second"sv },
    { "milliseconds"sv, "millisecond"sv },
    { "microseconds"sv, "microsecond"sv },
    { "nanoseconds"sv, "nanosecond"sv }
};

// 13.16 ToLargestTemporalUnit ( normalizedOptions, disallowedUnits, fallback [ , autoValue ] ), https://tc39.es/proposal-temporal/#sec-temporal-tolargesttemporalunit
ThrowCompletionOr<Optional<String>> to_largest_temporal_unit(GlobalObject& global_object, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback, Optional<String> auto_value)
{
    auto& vm = global_object.vm();

    // 1. Assert: disallowedUnits does not contain fallback.
    // 2. Assert: disallowedUnits does not contain "auto".
    // 3. Assert: autoValue is not present or fallback is "auto".
    VERIFY(!auto_value.has_value() || fallback == "auto"sv);
    // 4. Assert: autoValue is not present or disallowedUnits does not contain autoValue.

    // 5. Let largestUnit be ? GetOption(normalizedOptions, "largestUnit", « String », « "auto", "year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond", "microseconds", "nanosecond", "nanoseconds" », fallback).
    auto largest_unit_value = TRY(get_option(global_object, normalized_options, vm.names.largestUnit, { OptionType::String }, { "auto"sv, "year"sv, "years"sv, "month"sv, "months"sv, "week"sv, "weeks"sv, "day"sv, "days"sv, "hour"sv, "hours"sv, "minute"sv, "minutes"sv, "second"sv, "seconds"sv, "millisecond"sv, "milliseconds"sv, "microsecond"sv, "microseconds"sv, "nanosecond"sv, "nanoseconds"sv }, fallback.has_value() ? js_string(vm, *fallback) : js_undefined()));

    // OPTIMIZATION: We skip the following string-only checks for the fallback to tidy up the code a bit
    if (largest_unit_value.is_undefined())
        return Optional<String> {};
    VERIFY(largest_unit_value.is_string());
    auto largest_unit = largest_unit_value.as_string().string();

    // 6. If largestUnit is "auto" and autoValue is present, then
    if (largest_unit == "auto"sv && auto_value.has_value()) {
        // a. Return autoValue.
        return *auto_value;
    }

    // 7. If largestUnit is in the Plural column of Table 12, then
    if (auto singular_unit = plural_to_singular_units.get(largest_unit); singular_unit.has_value()) {
        // a. Set largestUnit to the corresponding Singular value of the same row.
        largest_unit = singular_unit.value();
    }

    // 8. If disallowedUnits contains largestUnit, then
    if (disallowed_units.contains_slow(largest_unit)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, largest_unit, vm.names.largestUnit.as_string());
    }

    // 9. Return largestUnit.
    return largest_unit;
}

// 13.17 ToSmallestTemporalUnit ( normalizedOptions, disallowedUnits, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-tosmallesttemporalunit
ThrowCompletionOr<Optional<String>> to_smallest_temporal_unit(GlobalObject& global_object, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback)
{
    auto& vm = global_object.vm();

    // 1. Assert: disallowedUnits does not contain fallback.

    // 2. Let smallestUnit be ? GetOption(normalizedOptions, "smallestUnit", « String », « "year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond", "microseconds", "nanosecond", "nanoseconds" », fallback).
    auto smallest_unit_value = TRY(get_option(global_object, normalized_options, vm.names.smallestUnit, { OptionType::String }, { "year"sv, "years"sv, "month"sv, "months"sv, "week"sv, "weeks"sv, "day"sv, "days"sv, "hour"sv, "hours"sv, "minute"sv, "minutes"sv, "second"sv, "seconds"sv, "millisecond"sv, "milliseconds"sv, "microsecond"sv, "microseconds"sv, "nanosecond"sv, "nanoseconds"sv }, fallback.has_value() ? js_string(vm, *fallback) : js_undefined()));

    // OPTIMIZATION: We skip the following string-only checks for the fallback to tidy up the code a bit
    if (smallest_unit_value.is_undefined())
        return Optional<String> {};
    VERIFY(smallest_unit_value.is_string());
    auto smallest_unit = smallest_unit_value.as_string().string();

    // 3. If smallestUnit is in the Plural column of Table 12, then
    if (auto singular_unit = plural_to_singular_units.get(smallest_unit); singular_unit.has_value()) {
        // a. Set smallestUnit to the corresponding Singular value of the same row.
        smallest_unit = singular_unit.value();
    }

    // 4. If disallowedUnits contains smallestUnit, then
    if (disallowed_units.contains_slow(smallest_unit)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, smallest_unit, vm.names.smallestUnit.as_string());
    }

    // 5. Return smallestUnit.
    return smallest_unit;
}

// 13.18 ToTemporalDurationTotalUnit ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldurationtotalunit
ThrowCompletionOr<String> to_temporal_duration_total_unit(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Let unit be ? GetOption(normalizedOptions, "unit", « String », « "year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond", "microseconds", "nanosecond", "nanoseconds" », undefined).
    auto unit_value = TRY(get_option(global_object, normalized_options, vm.names.unit, { OptionType::String }, { "year"sv, "years"sv, "month"sv, "months"sv, "week"sv, "weeks"sv, "day"sv, "days"sv, "hour"sv, "hours"sv, "minute"sv, "minutes"sv, "second"sv, "seconds"sv, "millisecond"sv, "milliseconds"sv, "microsecond"sv, "microseconds"sv, "nanosecond"sv, "nanoseconds"sv }, js_undefined()));

    // 2. If unit is undefined, then
    if (unit_value.is_undefined()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::IsUndefined, "unit option value"sv);
    }

    auto unit = unit_value.as_string().string();

    // 3. If unit is in the Plural column of Table 12, then
    if (auto singular_unit = plural_to_singular_units.get(unit); singular_unit.has_value()) {
        // a. Set unit to the corresponding Singular value of the same row.
        unit = *singular_unit;
    }

    // 4. Return unit.
    return unit;
}

// 13.20 ToRelativeTemporalObject ( options ), https://tc39.es/proposal-temporal/#sec-temporal-torelativetemporalobject
ThrowCompletionOr<Value> to_relative_temporal_object(GlobalObject& global_object, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.

    // 2. Let value be ? Get(options, "relativeTo").
    auto value = TRY(options.get(vm.names.relativeTo));

    // 3. If value is undefined, then
    if (value.is_undefined()) {
        // a. Return value.
        return value;
    }

    // 4. Let offsetBehaviour be option.
    auto offset_behavior = OffsetBehavior::Option;

    // 5. Let matchBehaviour be match exactly.
    auto match_behavior = MatchBehavior::MatchExactly;

    ISODateTime result;
    Value offset_string;
    Value time_zone;
    Object* calendar = nullptr;

    // 6. If Type(value) is Object, then
    if (value.is_object()) {
        auto& value_object = value.as_object();

        // a. If value has either an [[InitializedTemporalDate]] or [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<PlainDate>(value_object) || is<ZonedDateTime>(value_object)) {
            // i. Return value.
            return value;
        }

        // b. If value has an [[InitializedTemporalDateTime]] internal slot, then
        if (is<PlainDateTime>(value_object)) {
            auto& plain_date_time = static_cast<PlainDateTime&>(value_object);

            // i. Return ? CreateTemporalDate(value.[[ISOYear]], value.[[ISOMonth]], value.[[ISODay]], 0, 0, 0, 0, 0, 0, value.[[Calendar]]).
            return TRY(create_temporal_date(global_object, plain_date_time.iso_year(), plain_date_time.iso_month(), plain_date_time.iso_day(), plain_date_time.calendar()));
        }

        // c. Let calendar be ? GetTemporalCalendarWithISODefault(value).
        calendar = TRY(get_temporal_calendar_with_iso_default(global_object, value_object));

        // d. Let fieldNames be ? CalendarFields(calendar, « "day", "hour", "microsecond", "millisecond", "minute", "month", "monthCode", "nanosecond", "second", "year" »).
        auto field_names = TRY(calendar_fields(global_object, *calendar, { "day"sv, "hour"sv, "microsecond"sv, "millisecond"sv, "minute"sv, "month"sv, "monthCode"sv, "nanosecond"sv, "second"sv, "year"sv }));

        // e. Let fields be ? PrepareTemporalFields(value, fieldNames, «»).
        auto* fields = TRY(prepare_temporal_fields(global_object, value_object, field_names, {}));

        // f. Let dateOptions be ! OrdinaryObjectCreate(null).
        auto* date_options = Object::create(global_object, nullptr);

        // g. Perform ! CreateDataPropertyOrThrow(dateOptions, "overflow", "constrain").
        MUST(date_options->create_data_property_or_throw(vm.names.overflow, js_string(vm, "constrain"sv)));

        // h. Let result be ? InterpretTemporalDateTimeFields(calendar, fields, dateOptions).
        result = TRY(interpret_temporal_date_time_fields(global_object, *calendar, *fields, *date_options));

        // i. Let offsetString be ? Get(value, "offset").
        offset_string = TRY(value_object.get(vm.names.offset));

        // j. Let timeZone be ? Get(value, "timeZone").
        time_zone = TRY(value_object.get(vm.names.timeZone));

        // k. If timeZone is not undefined, then
        if (!time_zone.is_undefined()) {
            // i. Set timeZone to ? ToTemporalTimeZone(timeZone).
            time_zone = TRY(to_temporal_time_zone(global_object, time_zone));
        }

        // l. If offsetString is undefined, then
        if (offset_string.is_undefined()) {
            // i. Set offsetBehaviour to wall.
            offset_behavior = OffsetBehavior::Wall;
        }
    }
    // 7. Else,
    else {
        // a. Let string be ? ToString(value).
        auto string = TRY(value.to_string(global_object));

        // b. Let result be ? ParseTemporalRelativeToString(string).
        auto parsed_result = TRY(parse_temporal_relative_to_string(global_object, string));

        // NOTE: The ISODateTime struct inside `parsed_result` will be moved into `result` at the end of this path to avoid mismatching names.
        //       Thus, all remaining references to `result` in this path actually refer to `parsed_result`.

        // c. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
        calendar = TRY(to_temporal_calendar_with_iso_default(global_object, parsed_result.date_time.calendar.has_value() ? js_string(vm, *parsed_result.date_time.calendar) : js_undefined()));

        // d. Let offsetString be result.[[TimeZoneOffsetString]].
        offset_string = parsed_result.time_zone.offset_string.has_value() ? js_string(vm, *parsed_result.time_zone.offset_string) : js_undefined();

        // e. Let timeZoneName be result.[[TimeZoneIANAName]].
        auto time_zone_name = parsed_result.time_zone.name;

        // f. If timeZoneName is not undefined, then
        if (time_zone_name.has_value()) {
            // i. If ParseText(! StringToCodePoints(timeZoneName), TimeZoneNumericUTCOffset) is not a List of errors, then
            // FIXME: Logic error in the spec (check for no errors -> check for errors).
            //        See: https://github.com/tc39/proposal-temporal/pull/2000
            if (!is_valid_time_zone_numeric_utc_offset_syntax(*time_zone_name)) {
                // 1. If ! IsValidTimeZoneName(timeZoneName) is false, throw a RangeError exception.
                if (!is_valid_time_zone_name(*time_zone_name))
                    return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeZoneName, *time_zone_name);

                // 2. Set timeZoneName to ! CanonicalizeTimeZoneName(timeZoneName).
                time_zone_name = canonicalize_time_zone_name(*time_zone_name);
            }

            // ii. Let timeZone be ! CreateTemporalTimeZone(timeZoneName).
            time_zone = MUST(create_temporal_time_zone(global_object, *time_zone_name));
        }
        // g. Else,
        else {
            // i. Let timeZone be undefined.
            time_zone = js_undefined();
        }

        // h. If result.[[TimeZoneZ]] is true, then
        if (parsed_result.time_zone.z) {
            // i. Set offsetBehaviour to exact.
            offset_behavior = OffsetBehavior::Exact;
        }
        // i. Else if offsetString is undefined, then
        else if (offset_string.is_undefined()) {
            // i. Set offsetBehaviour to wall.
            offset_behavior = OffsetBehavior::Wall;
        }

        // j. Set matchBehaviour to match minutes.
        match_behavior = MatchBehavior::MatchMinutes;

        // See NOTE above about why this is done.
        result = move(parsed_result.date_time);
    }

    // 8. If timeZone is not undefined, then
    if (!time_zone.is_undefined()) {
        double offset_ns;

        // a. If offsetBehaviour is option, then
        if (offset_behavior == OffsetBehavior::Option) {
            // i. Set offsetString to ? ToString(offsetString).
            // NOTE: offsetString is not used after this path, so we don't need to put this into the original offset_string which is of type JS::Value.
            auto actual_offset_string = TRY(offset_string.to_string(global_object));

            // ii. Let offsetNs be ? ParseTimeZoneOffsetString(offsetString).
            offset_ns = TRY(parse_time_zone_offset_string(global_object, actual_offset_string));
        }
        // b. Else,
        else {
            // i. Let offsetNs be 0.
            offset_ns = 0;
        }

        // c. Let epochNanoseconds be ? InterpretISODateTimeOffset(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], offsetBehaviour, offsetNs, timeZone, "compatible", "reject", matchBehaviour).
        auto* epoch_nanoseconds = TRY(interpret_iso_date_time_offset(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond, offset_behavior, offset_ns, time_zone, "compatible"sv, "reject"sv, match_behavior));

        // d. Return ! CreateTemporalZonedDateTime(epochNanoseconds, timeZone, calendar).
        return MUST(create_temporal_zoned_date_time(global_object, *epoch_nanoseconds, time_zone.as_object(), *calendar));
    }

    // 9. Return ! CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return TRY(create_temporal_date(global_object, result.year, result.month, result.day, *calendar));
}

// 13.21 ValidateTemporalUnitRange ( largestUnit, smallestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-validatetemporalunitrange
ThrowCompletionOr<void> validate_temporal_unit_range(GlobalObject& global_object, StringView largest_unit, StringView smallest_unit)
{
    auto& vm = global_object.vm();

    // 1. If smallestUnit is "year" and largestUnit is not "year", then
    if (smallest_unit == "year"sv && largest_unit != "year"sv) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 2. If smallestUnit is "month" and largestUnit is not "year" or "month", then
    if (smallest_unit == "month"sv && !largest_unit.is_one_of("year"sv, "month"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 3. If smallestUnit is "week" and largestUnit is not one of "year", "month", or "week", then
    if (smallest_unit == "week"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 4. If smallestUnit is "day" and largestUnit is not one of "year", "month", "week", or "day", then
    if (smallest_unit == "day"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 5. If smallestUnit is "hour" and largestUnit is not one of "year", "month", "week", "day", or "hour", then
    if (smallest_unit == "hour"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv, "hour"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 6. If smallestUnit is "minute" and largestUnit is "second", "millisecond", "microsecond", or "nanosecond", then
    if (smallest_unit == "minute"sv && largest_unit.is_one_of("second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 7. If smallestUnit is "second" and largestUnit is "millisecond", "microsecond", or "nanosecond", then
    if (smallest_unit == "second"sv && largest_unit.is_one_of("millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 8. If smallestUnit is "millisecond" and largestUnit is "microsecond" or "nanosecond", then
    if (smallest_unit == "millisecond"sv && largest_unit.is_one_of("microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }
    // 9. If smallestUnit is "microsecond" and largestUnit is "nanosecond", then
    if (smallest_unit == "microsecond"sv && largest_unit == "nanosecond"sv) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
    }

    return {};
}

// 13.22 LargerOfTwoTemporalUnits ( u1, u2 ), https://tc39.es/proposal-temporal/#sec-temporal-largeroftwotemporalunits
StringView larger_of_two_temporal_units(StringView unit1, StringView unit2)
{
    // 1. If either u1 or u2 is "year", return "year".
    if (unit1 == "year"sv || unit2 == "year"sv)
        return "year"sv;
    // 2. If either u1 or u2 is "month", return "month".
    if (unit1 == "month"sv || unit2 == "month"sv)
        return "month"sv;
    // 3. If either u1 or u2 is "week", return "week".
    if (unit1 == "week"sv || unit2 == "week"sv)
        return "week"sv;
    // 4. If either u1 or u2 is "day", return "day".
    if (unit1 == "day"sv || unit2 == "day"sv)
        return "day"sv;
    // 5. If either u1 or u2 is "hour", return "hour".
    if (unit1 == "hour"sv || unit2 == "hour"sv)
        return "hour"sv;
    // 6. If either u1 or u2 is "minute", return "minute".
    if (unit1 == "minute"sv || unit2 == "minute"sv)
        return "minute"sv;
    // 7. If either u1 or u2 is "second", return "second".
    if (unit1 == "second"sv || unit2 == "second"sv)
        return "second"sv;
    // 8. If either u1 or u2 is "millisecond", return "millisecond".
    if (unit1 == "millisecond"sv || unit2 == "millisecond"sv)
        return "millisecond"sv;
    // 9. If either u1 or u2 is "microsecond", return "microsecond".
    if (unit1 == "microsecond"sv || unit2 == "microsecond"sv)
        return "microsecond"sv;
    // 10. Return "nanosecond".
    return "nanosecond"sv;
}

// 13.23 MergeLargestUnitOption ( options, largestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-mergelargestunitoption
ThrowCompletionOr<Object*> merge_largest_unit_option(GlobalObject& global_object, Object& options, String largest_unit)
{
    auto& vm = global_object.vm();

    // 1. Let merged be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* merged = Object::create(global_object, global_object.object_prototype());

    // 2. Let keys be ? EnumerableOwnPropertyNames(options, key).
    auto keys = TRY(options.enumerable_own_property_names(Object::PropertyKind::Key));

    // 3. For each element nextKey of keys, do
    for (auto& key : keys) {
        auto next_key = MUST(PropertyKey::from_value(global_object, key));

        // a. Let propValue be ? Get(options, nextKey).
        auto prop_value = TRY(options.get(next_key));

        // b. Perform ! CreateDataPropertyOrThrow(merged, nextKey, propValue).
        MUST(merged->create_data_property_or_throw(next_key, prop_value));
    }

    // 4. Perform ! CreateDataPropertyOrThrow(merged, "largestUnit", largestUnit).
    MUST(merged->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, move(largest_unit))));

    // 5. Return merged.
    return merged;
}

// 13.24 MaximumTemporalDurationRoundingIncrement ( unit ), https://tc39.es/proposal-temporal/#sec-temporal-maximumtemporaldurationroundingincrement
Optional<u16> maximum_temporal_duration_rounding_increment(StringView unit)
{
    // 1. If unit is "year", "month", "week", or "day", then
    if (unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        // a. Return undefined.
        return {};
    }

    // 2. If unit is "hour", then
    if (unit == "hour"sv) {
        // a. Return 24.
        return 24;
    }

    // 3. If unit is "minute" or "second", then
    if (unit.is_one_of("minute"sv, "second"sv)) {
        // a. Return 60.
        return 60;
    }

    // 4. Assert: unit is one of "millisecond", "microsecond", or "nanosecond".
    VERIFY(unit.is_one_of("millisecond"sv, "microsecond"sv, "nanosecond"sv));

    // 5. Return 1000.
    return 1000;
}

// 13.25 RejectObjectWithCalendarOrTimeZone ( object ), https://tc39.es/proposal-temporal/#sec-temporal-rejectobjectwithcalendarortimezone
ThrowCompletionOr<void> reject_object_with_calendar_or_time_zone(GlobalObject& global_object, Object& object)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(object) is Object.

    // 2. If object has an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], [[InitializedTemporalTime]], [[InitializedTemporalYearMonth]], or [[InitializedTemporalZonedDateTime]] internal slot, then
    if (is<PlainDate>(object) || is<PlainDateTime>(object) || is<PlainMonthDay>(object) || is<PlainTime>(object) || is<PlainYearMonth>(object) || is<ZonedDateTime>(object)) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalObjectMustNotHave, "calendar or timeZone");
    }

    // 3. Let calendarProperty be ? Get(object, "calendar").
    auto calendar_property = TRY(object.get(vm.names.calendar));

    // 4. If calendarProperty is not undefined, then
    if (!calendar_property.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalObjectMustNotHave, "calendar");
    }

    // 5. Let timeZoneProperty be ? Get(object, "timeZone").
    auto time_zone_property = TRY(object.get(vm.names.timeZone));

    // 6. If timeZoneProperty is not undefined, then
    if (!time_zone_property.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalObjectMustNotHave, "timeZone");
    }

    return {};
}

// 13.26 FormatSecondsStringPart ( second, millisecond, microsecond, nanosecond, precision ), https://tc39.es/proposal-temporal/#sec-temporal-formatsecondsstringpart
String format_seconds_string_part(u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Variant<StringView, u8> const& precision)
{
    // 1. Assert: second, millisecond, microsecond and nanosecond are integers.

    // Non-standard sanity check
    if (precision.has<StringView>())
        VERIFY(precision.get<StringView>().is_one_of("minute"sv, "auto"sv));

    // 2. If precision is "minute", return "".
    if (precision.has<StringView>() && precision.get<StringView>() == "minute"sv)
        return String::empty();

    // 3. Let secondsString be the string-concatenation of the code unit 0x003A (COLON) and second formatted as a two-digit decimal number, padded to the left with zeroes if necessary.
    auto seconds_string = String::formatted(":{:02}", second);

    // 4. Let fraction be millisecond × 10^6 + microsecond × 10^3 + nanosecond.
    u32 fraction = millisecond * 1'000'000 + microsecond * 1'000 + nanosecond;

    String fraction_string;

    // 5. If precision is "auto", then
    if (precision.has<StringView>() && precision.get<StringView>() == "auto"sv) {
        // a. If fraction is 0, return secondsString.
        if (fraction == 0)
            return seconds_string;

        // b. Set fraction to fraction formatted as a nine-digit decimal number, padded to the left with zeroes if necessary.
        fraction_string = String::formatted("{:09}", fraction);

        // c. Set fraction to the longest possible substring of fraction starting at position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
        fraction_string = fraction_string.trim("0"sv, TrimMode::Right);
    }
    // 6. Else,
    else {
        // a. If precision is 0, return secondsString.
        if (precision.get<u8>() == 0)
            return seconds_string;

        // b. Set fraction to fraction formatted as a nine-digit decimal number, padded to the left with zeroes if necessary.
        fraction_string = String::formatted("{:09}", fraction);

        // c. Set fraction to the substring of fraction from 0 to precision.
        fraction_string = fraction_string.substring(0, precision.get<u8>());
    }

    // 7. Return the string-concatenation of secondsString, the code unit 0x002E (FULL STOP), and fraction.
    return String::formatted("{}.{}", seconds_string, fraction_string);
}

// 13.27 Sign ( n ), https://tc39.es/proposal-temporal/#sec-temporal-sign
double sign(double n)
{
    // 1. If n is NaN, n is +0𝔽, or n is −0𝔽, return n.
    if (isnan(n) || n == 0)
        return n;

    // 2. If n < +0𝔽, return −1𝔽.
    if (n < 0)
        return -1;

    // 3. Return 1𝔽.
    return 1;
}

double sign(Crypto::SignedBigInteger const& n)
{
    // 1. If n is NaN, n is +0𝔽, or n is −0𝔽, return n.
    if (n == Crypto::SignedBigInteger { 0 })
        return n.is_negative() ? -0 : 0;

    // 2. If n < +0𝔽, return −1𝔽.
    if (n.is_negative())
        return -1;

    // 3. Return 1𝔽.
    return 1;
}

// 13.28 ConstrainToRange ( x, minimum, maximum ), https://tc39.es/proposal-temporal/#sec-temporal-constraintorange
double constrain_to_range(double x, double minimum, double maximum)
{
    // 1. Assert: x, minimum and maximum are mathematical values.

    // 2. Return min(max(x, minimum), maximum).
    return min(max(x, minimum), maximum);
}

// NOTE: We have two variants of this function, one using doubles and one using BigInts - most of the time
// doubles will be fine, but take care to choose the right one. The spec is not very clear about this, as
// it uses mathematical values which can be arbitrarily (but not infinitely) large.
// Incidentally V8's Temporal implementation does the same :^)

// 13.31 RoundNumberToIncrement ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrement
i64 round_number_to_increment(double x, u64 increment, StringView rounding_mode)
{
    // 1. Assert: x and increment are mathematical values.
    // 2. Assert: roundingMode is "ceil", "floor", "trunc", or "halfExpand".
    VERIFY(rounding_mode == "ceil"sv || rounding_mode == "floor"sv || rounding_mode == "trunc"sv || rounding_mode == "halfExpand"sv);

    // 3. Let quotient be x / increment.
    auto quotient = x / (double)increment;

    double rounded;

    // 4. If roundingMode is "ceil", then
    if (rounding_mode == "ceil"sv) {
        // a. Let rounded be −floor(−quotient).
        rounded = -floor(-quotient);
    }
    // 5. Else if roundingMode is "floor", then
    else if (rounding_mode == "floor"sv) {
        // a. Let rounded be floor(quotient).
        rounded = floor(quotient);
    }
    // 6. Else if roundingMode is "trunc", then
    else if (rounding_mode == "trunc"sv) {
        // a. Let rounded be the integral part of quotient, removing any fractional digits.
        rounded = trunc(quotient);
    }
    // 7. Else,
    else {
        // a. Let rounded be ! RoundHalfAwayFromZero(quotient).
        rounded = round(quotient);
    }

    // 8. Return rounded × increment.
    return (i64)rounded * (i64)increment;
}

// 13.31 RoundNumberToIncrement ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrement
BigInt* round_number_to_increment(GlobalObject& global_object, BigInt const& x, u64 increment, StringView rounding_mode)
{
    auto& heap = global_object.heap();

    // 1. Assert: x and increment are mathematical values.
    // 2. Assert: roundingMode is "ceil", "floor", "trunc", or "halfExpand".
    VERIFY(rounding_mode == "ceil"sv || rounding_mode == "floor"sv || rounding_mode == "trunc"sv || rounding_mode == "halfExpand"sv);

    // OPTIMIZATION: If the increment is 1 the number is always rounded
    if (increment == 1)
        return js_bigint(heap, x.big_integer());

    auto increment_big_int = Crypto::UnsignedBigInteger::create_from(increment);
    // 3. Let quotient be x / increment.
    auto division_result = x.big_integer().divided_by(increment_big_int);

    // OPTIMIZATION: If there's no remainder the number is already rounded
    if (division_result.remainder == Crypto::UnsignedBigInteger { 0 })
        return js_bigint(heap, x.big_integer());

    Crypto::SignedBigInteger rounded = move(division_result.quotient);
    // 4. If roundingMode is "ceil", then
    if (rounding_mode == "ceil"sv) {
        // a. Let rounded be −floor(−quotient).
        if (!division_result.remainder.is_negative())
            rounded = rounded.plus(Crypto::UnsignedBigInteger { 1 });
    }
    // 5. Else if roundingMode is "floor", then
    else if (rounding_mode == "floor"sv) {
        // a. Let rounded be floor(quotient).
        if (division_result.remainder.is_negative())
            rounded = rounded.minus(Crypto::UnsignedBigInteger { 1 });
    }
    // 6. Else if roundingMode is "trunc", then
    else if (rounding_mode == "trunc"sv) {
        // a. Let rounded be the integral part of quotient, removing any fractional digits.
        // NOTE: This is a no-op
    }
    // 7. Else,
    else {
        // a. Let rounded be ! RoundHalfAwayFromZero(quotient).
        if (division_result.remainder.multiplied_by(Crypto::UnsignedBigInteger { 2 }).unsigned_value() >= increment_big_int) {
            if (division_result.remainder.is_negative())
                rounded = rounded.minus(Crypto::UnsignedBigInteger { 1 });
            else
                rounded = rounded.plus(Crypto::UnsignedBigInteger { 1 });
        }
    }

    // 8. Return rounded × increment.
    return js_bigint(heap, rounded.multiplied_by(increment_big_int));
}

// 13.33 ParseISODateTime ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parseisodatetime
ThrowCompletionOr<ISODateTime> parse_iso_date_time(GlobalObject& global_object, ParseResult const& parse_result)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let year, month, day, fraction, and calendar be the parts of isoString produced respectively by the DateYear, DateMonth, DateDay, TimeFraction, and CalendarName productions, or undefined if not present.
    auto year_part = parse_result.date_year;
    auto month_part = parse_result.date_month;
    auto day_part = parse_result.date_day;
    auto fraction_part = parse_result.time_fraction;
    auto calendar_part = parse_result.calendar_name;

    // 3. Let hour be the part of isoString produced by the TimeHour, TimeHourNotValidMonth, TimeHourNotThirtyOneDayMonth, or TimeHourTwoOnly productions, or undefined if none of those are present.
    auto hour_part = parse_result.time_hour;
    if (!hour_part.has_value())
        hour_part = parse_result.time_hour_not_valid_month;
    if (!hour_part.has_value())
        hour_part = parse_result.time_hour_not_thirty_one_day_month;
    if (!hour_part.has_value())
        hour_part = parse_result.time_hour_two_only;

    // 4. Let minute be the part of isoString produced by the TimeMinute, TimeMinuteNotValidDay, TimeMinuteThirtyOnly, or TimeMinuteThirtyOneOnly productions, or undefined if none of those are present.
    auto minute_part = parse_result.time_minute;
    if (!minute_part.has_value())
        minute_part = parse_result.time_minute_not_valid_day;
    if (!minute_part.has_value())
        minute_part = parse_result.time_minute_thirty_only;
    if (!minute_part.has_value())
        minute_part = parse_result.time_minute_thirty_one_only;

    // 5. Let second be the part of isoString produced by the TimeSecond or TimeSecondNotValidMonth productions, or undefined if neither of those are present.
    auto second_part = parse_result.time_second;
    if (!second_part.has_value())
        second_part = parse_result.time_second_not_valid_month;

    // 6. If the first code unit of year is 0x2212 (MINUS SIGN), replace it with the code unit 0x002D (HYPHEN-MINUS).
    String normalized_year;
    if (year_part.has_value() && year_part->starts_with("\xE2\x88\x92"sv))
        normalized_year = String::formatted("-{}", year_part->substring_view(3));
    else
        normalized_year = year_part.value_or("0");

    // 7. If ! SameValue(year, "-000000") is true, throw a RangeError exception.
    if (normalized_year == "-000000"sv)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidExtendedYearNegativeZero);

    // 8. Set year to ! ToIntegerOrInfinity(year).
    auto year = *normalized_year.to_int<i32>();

    u8 month;
    // 9. If month is undefined, then
    if (!month_part.has_value()) {
        // a. Set month to 1.
        month = 1;
    }
    // 10. Else,
    else {
        // a. Set month to ! ToIntegerOrInfinity(month).
        month = *month_part->to_uint<u8>();
    }

    u8 day;
    // 11. If day is undefined, then
    if (!day_part.has_value()) {
        // a. Set day to 1.
        day = 1;
    }
    // 12. Else,
    else {
        // a. Set day to ! ToIntegerOrInfinity(day).
        day = *day_part->to_uint<u8>();
    }

    // 13. Set hour to ! ToIntegerOrInfinity(hour).
    u8 hour = *hour_part.value_or("0"sv).to_uint<u8>();

    // 14. Set minute to ! ToIntegerOrInfinity(minute).
    u8 minute = *minute_part.value_or("0"sv).to_uint<u8>();

    // 15. Set second to ! ToIntegerOrInfinity(second).
    u8 second = *second_part.value_or("0"sv).to_uint<u8>();

    // 16. If second is 60, then
    if (second == 60) {
        // a. Set second to 59.
        second = 59;
    }

    u16 millisecond;
    u16 microsecond;
    u16 nanosecond;
    // 17. If fraction is not undefined, then
    if (fraction_part.has_value()) {
        // a. Set fraction to the string-concatenation of the previous value of fraction and the string "000000000".
        auto fraction = String::formatted("{}000000000", *fraction_part);
        // b. Let millisecond be the String value equal to the substring of fraction from 1 to 4.
        // c. Set millisecond to ! ToIntegerOrInfinity(millisecond).
        millisecond = *fraction.substring(1, 3).to_uint<u16>();
        // d. Let microsecond be the String value equal to the substring of fraction from 4 to 7.
        // e. Set microsecond to ! ToIntegerOrInfinity(microsecond).
        microsecond = *fraction.substring(4, 3).to_uint<u16>();
        // f. Let nanosecond be the String value equal to the substring of fraction from 7 to 10.
        // g. Set nanosecond to ! ToIntegerOrInfinity(nanosecond).
        nanosecond = *fraction.substring(7, 3).to_uint<u16>();
    }
    // 18. Else,
    else {
        // a. Let millisecond be 0.
        millisecond = 0;
        // b. Let microsecond be 0.
        microsecond = 0;
        // c. Let nanosecond be 0.
        nanosecond = 0;
    }

    // 19. If ! IsValidISODate(year, month, day) is false, throw a RangeError exception.
    if (!is_valid_iso_date(year, month, day))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidISODate);

    // 20. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
    if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTime);

    // 21. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond, [[Calendar]]: calendar }.
    return ISODateTime { .year = year, .month = month, .day = day, .hour = hour, .minute = minute, .second = second, .millisecond = millisecond, .microsecond = microsecond, .nanosecond = nanosecond, .calendar = Optional<String>(move(calendar_part)) };
}

// 13.34 ParseTemporalInstantString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalinstantstring
ThrowCompletionOr<TemporalInstant> parse_temporal_instant_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalInstantString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalInstantString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidInstantString, iso_string);
    }

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 4. Let timeZoneResult be ? ParseTemporalTimeZoneString(isoString).
    auto time_zone_result = TRY(parse_temporal_time_zone_string(global_object, iso_string));

    // 5. Let offsetString be timeZoneResult.[[OffsetString]].
    auto offset_string = time_zone_result.offset_string;

    // 6. If timeZoneResult.[[Z]] is true, then
    if (time_zone_result.z) {
        // a. Set offsetString to "+00:00".
        offset_string = "+00:00"sv;
    }

    // 7. Assert: offsetString is not undefined.
    VERIFY(offset_string.has_value());

    // 8. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[TimeZoneOffsetString]]: offsetString }.
    return TemporalInstant { .year = result.year, .month = result.month, .day = result.day, .hour = result.hour, .minute = result.minute, .second = result.second, .millisecond = result.millisecond, .microsecond = result.microsecond, .nanosecond = result.nanosecond, .time_zone_offset = move(offset_string) };
}

// 13.35 ParseTemporalZonedDateTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalzoneddatetimestring
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_zoned_date_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalZonedDateTimeString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalZonedDateTimeString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidZonedDateTimeString, iso_string);
    }

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 4. Let timeZoneResult be ? ParseTemporalTimeZoneString(isoString).
    auto time_zone_result = TRY(parse_temporal_time_zone_string(global_object, iso_string));

    // 5. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]], [[TimeZoneZ]]: timeZoneResult.[[Z]], [[TimeZoneOffsetString]]: timeZoneResult.[[OffsetString]], [[TimeZoneName]]: timeZoneResult.[[Name]] }.
    // NOTE: This returns the two structs together instead of separated to avoid a copy in ToTemporalZonedDateTime, as the spec tries to put the result of InterpretTemporalDateTimeFields and ParseTemporalZonedDateTimeString into the same `result` variable.
    // InterpretTemporalDateTimeFields returns an ISODateTime, so the moved in `result` here is subsequently moved into ParseTemporalZonedDateTimeString's `result` variable.
    return TemporalZonedDateTime { .date_time = move(result), .time_zone = move(time_zone_result) };
}

// 13.36 ParseTemporalCalendarString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalcalendarstring
ThrowCompletionOr<String> parse_temporal_calendar_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalCalendarString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalCalendarString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarString, iso_string);
    }

    // 3. Let id be the part of isoString produced by the CalendarName production, or undefined if not present.
    auto id_part = parse_result->calendar_name;

    // 4. If id is undefined, then
    if (!id_part.has_value()) {
        // a. Return "iso8601".
        return "iso8601"sv;
    }

    // 5. Return id.
    return id_part.value();
}

// 13.37 ParseTemporalDateString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatestring
ThrowCompletionOr<TemporalDate> parse_temporal_date_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalDateString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalDateString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateString, iso_string);
    }

    // 3. If isoString contains a UTCDesignator, then
    if (parse_result->utc_designator.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateStringUTCDesignator, iso_string);
    }

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalDate { .year = result.year, .month = result.month, .day = result.day, .calendar = move(result.calendar) };
}

// 13.38 ParseTemporalDateTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatetimestring
ThrowCompletionOr<ISODateTime> parse_temporal_date_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalDateTimeString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateTimeString, iso_string);
    }

    // 3. If isoString contains a UTCDesignator, then
    if (parse_result->utc_designator.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateTimeStringUTCDesignator, iso_string);
    }

    // 4. Return ? ParseISODateTime(isoString).
    return parse_iso_date_time(global_object, *parse_result);
}

// 13.39 ParseTemporalDurationString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldurationstring
ThrowCompletionOr<TemporalDuration> parse_temporal_duration_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let duration be ParseText(! StringToCodePoints(isoString), TemporalDurationString).
    auto parse_result = parse_iso8601(Production::TemporalDurationString, iso_string);

    // 3. If duration is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationString, iso_string);

    // 4. Let each of sign, years, months, weeks, days, hours, fHours, minutes, fMinutes, seconds, and fSeconds be the source text matched by the respective Sign, DurationYears, DurationMonths, DurationWeeks, DurationDays, DurationWholeHours, DurationHoursFraction, DurationWholeMinutes, DurationMinutesFraction, DurationWholeSeconds, and DurationSecondsFraction Parse Node enclosed by duration, or an empty sequence of code points if not present.
    auto sign_part = parse_result->sign;
    auto years_part = parse_result->duration_years;
    auto months_part = parse_result->duration_months;
    auto weeks_part = parse_result->duration_weeks;
    auto days_part = parse_result->duration_days;
    auto hours_part = parse_result->duration_whole_hours;
    auto f_hours_part = parse_result->duration_hours_fraction;
    auto minutes_part = parse_result->duration_whole_minutes;
    auto f_minutes_part = parse_result->duration_minutes_fraction;
    auto seconds_part = parse_result->duration_whole_seconds;
    auto f_seconds_part = parse_result->duration_seconds_fraction;

    // FIXME: I can has StringView::to<double>()?

    // 5. Let yearsMV be ! ToIntegerOrInfinity(CodePointsToString(years)).
    auto years = strtod(String { years_part.value_or("0"sv) }.characters(), nullptr);

    // 6. Let monthsMV be ! ToIntegerOrInfinity(CodePointsToString(months)).
    auto months = strtod(String { months_part.value_or("0"sv) }.characters(), nullptr);

    // 7. Let weeksMV be ! ToIntegerOrInfinity(CodePointsToString(weeks)).
    auto weeks = strtod(String { weeks_part.value_or("0"sv) }.characters(), nullptr);

    // 8. Let daysMV be ! ToIntegerOrInfinity(CodePointsToString(days)).
    auto days = strtod(String { days_part.value_or("0"sv) }.characters(), nullptr);

    // 9. Let hoursMV be ! ToIntegerOrInfinity(CodePointsToString(hours)).
    auto hours = strtod(String { hours_part.value_or("0"sv) }.characters(), nullptr);

    double minutes;

    // 10. If fHours is not empty, then
    if (f_hours_part.has_value()) {
        // a. If any of minutes, fMinutes, seconds, fSeconds is not empty, throw a RangeError exception.
        if (minutes_part.has_value() || f_minutes_part.has_value() || seconds_part.has_value() || f_seconds_part.has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationStringFractionNotLast, iso_string, "hours"sv, "minutes or seconds"sv);

        // b. Let fHoursDigits be the substring of ! CodePointsToString(fHours) from 1.
        auto f_hours_digits = f_hours_part->substring_view(1);

        // c. Let fHoursScale be the length of fHoursDigits.
        auto f_hours_scale = (double)f_hours_digits.length();

        // d. Let minutesMV be ! ToIntegerOrInfinity(fHoursDigits) / 10^fHoursScale × 60.
        minutes = strtod(String { f_hours_digits }.characters(), nullptr) / pow(10, f_hours_scale) * 60;
    }
    // 11. Else,
    else {
        // a. Let minutesMV be ! ToIntegerOrInfinity(CodePointsToString(minutes)).
        minutes = strtod(String { minutes_part.value_or("0"sv) }.characters(), nullptr);
    }

    double seconds;

    // 12. If fMinutes is not empty, then
    if (f_minutes_part.has_value()) {
        // a. If any of seconds, fSeconds is not empty, throw a RangeError exception.
        if (seconds_part.has_value() || f_seconds_part.has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationStringFractionNotLast, iso_string, "minutes"sv, "seconds"sv);

        // b. Let fMinutesDigits be the substring of ! CodePointsToString(fMinutes) from 1.
        auto f_minutes_digits = f_minutes_part->substring_view(1);

        // c. Let fMinutesScale be the length of fMinutesDigits.
        auto f_minutes_scale = (double)f_minutes_digits.length();

        // d. Let secondsMV be ! ToIntegerOrInfinity(fMinutesDigits) / 10^fMinutesScale × 60.
        seconds = strtod(String { f_minutes_digits }.characters(), nullptr) / pow(10, f_minutes_scale) * 60;
    }
    // 13. Else if seconds is not empty, then
    else if (seconds_part.has_value()) {
        // a. Let secondsMV be ! ToIntegerOrInfinity(CodePointsToString(seconds)).
        seconds = strtod(String { *seconds_part }.characters(), nullptr);
    }
    // 14. Else,
    else {
        // a. Let secondsMV be remainder(minutesMV, 1) × 60.
        seconds = fmod(minutes, 1) * 60;
    }

    double milliseconds;

    // 15. If fSeconds is not empty, then
    if (f_seconds_part.has_value()) {
        // a. Let fSecondsDigits be the substring of ! CodePointsToString(fSeconds) from 1.
        auto f_seconds_digits = f_seconds_part->substring_view(1);

        // b. Let fSecondsScale be the length of fSecondsDigits.
        auto f_seconds_scale = (double)f_seconds_digits.length();

        // c. Let millisecondsMV be ! ToIntegerOrInfinity(fSecondsDigits) / 10^fSecondsScale × 1000.
        milliseconds = strtod(String { f_seconds_digits }.characters(), nullptr) / pow(10, f_seconds_scale) * 1000;
    }
    // 16. Else,
    else {
        // a. Let millisecondsMV be remainder(secondsMV, 1) × 1000.
        milliseconds = fmod(seconds, 1) * 1000;
    }

    // FIXME: This suffers from floating point (im)precision issues - e.g. "PT0.0000001S" ends up
    //        getting parsed as 99.999999 nanoseconds, which is floor()'d to 99 instead of the
    //        expected 100. Oof. This is the reason all of these are suffixed with "MV" in the spec:
    //        mathematical values are not supposed to have this issue.

    // 17. Let microsecondsMV be remainder(millisecondsMV, 1) × 1000.
    auto microseconds = fmod(milliseconds, 1) * 1000;

    // 18. Let nanosecondsMV be remainder(microsecondsMV, 1) × 1000.
    auto nanoseconds = fmod(microseconds, 1) * 1000;

    i8 factor;

    // 19. If sign contains the code point 0x002D (HYPHEN-MINUS) or 0x2212 (MINUS SIGN), then
    if (sign_part.has_value() && sign_part->is_one_of("-", "\u2212")) {
        // a. Let factor be −1.
        factor = -1;
    }
    // 20. Else,
    else {
        // a. Let factor be 1.
        factor = 1;
    }

    // 21. Return the Record { [[Years]]: yearsMV × factor, [[Months]]: monthsMV × factor, [[Weeks]]: weeksMV × factor, [[Days]]: daysMV × factor, [[Hours]]: hoursMV × factor, [[Minutes]]: floor(minutesMV) × factor, [[Seconds]]: floor(secondsMV) × factor, [[Milliseconds]]: floor(millisecondsMV) × factor, [[Microseconds]]: floor(microsecondsMV) × factor, [[Nanoseconds]]: floor(nanosecondsMV) × factor }.
    return TemporalDuration { .years = years * factor, .months = months * factor, .weeks = weeks * factor, .days = days * factor, .hours = hours * factor, .minutes = floor(minutes) * factor, .seconds = floor(seconds) * factor, .milliseconds = floor(milliseconds) * factor, .microseconds = floor(microseconds) * factor, .nanoseconds = floor(nanoseconds) * factor };
}

// 13.40 ParseTemporalMonthDayString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalmonthdaystring
ThrowCompletionOr<TemporalMonthDay> parse_temporal_month_day_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalMonthDayString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalMonthDayString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthDayString, iso_string);
    }

    // 3. If isoString contains a UTCDesignator, then
    if (parse_result->utc_designator.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthDayStringUTCDesignator, iso_string);
    }

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Let year be result.[[Year]].
    Optional<i32> year = result.year;

    // 6. If no part of isoString is produced by the DateYear production, then
    if (!parse_result->date_year.has_value()) {
        // a. Set year to undefined.
        year = {};
    }

    // 7. Return the Record { [[Year]]: year, [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalMonthDay { .year = year, .month = result.month, .day = result.day, .calendar = move(result.calendar) };
}

// 13.41 ParseTemporalRelativeToString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalrelativetostring
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_relative_to_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalRelativeToString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalRelativeToString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidRelativeToString, iso_string);
    }

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    bool z;
    Optional<String> offset_string;
    Optional<String> time_zone;

    // 4. If isoString satisfies the syntax of a TemporalZonedDateTimeString (see 13.33), then
    parse_result = parse_iso8601(Production::TemporalZonedDateTimeString, iso_string);
    if (parse_result.has_value()) {
        // a. Let timeZoneResult be ! ParseTemporalTimeZoneString(isoString).
        auto time_zone_result = MUST(parse_temporal_time_zone_string(global_object, iso_string));

        // b. Let z be timeZoneResult.[[Z]].
        z = time_zone_result.z;

        // c. Let offsetString be timeZoneResult.[[OffsetString]].
        offset_string = time_zone_result.offset_string;

        // d. Let timeZone be timeZoneResult.[[Name]].
        time_zone = time_zone_result.name;
    }
    // 5. Else,
    else {
        // a. Let z be false.
        z = false;

        // b. Let offsetString be undefined.
        // c. Let timeZone be undefined.
    }

    // 6. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]], [[TimeZoneZ]]: z, [[TimeZoneOffsetString]]: offsetString, [[TimeZoneIANAName]]: timeZone }.
    return TemporalZonedDateTime { .date_time = move(result), .time_zone = { .z = z, .offset_string = move(offset_string), .name = move(time_zone) } };
}

// 13.42 ParseTemporalTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimestring
ThrowCompletionOr<TemporalTime> parse_temporal_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalTimeString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalTimeString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeString, iso_string);
    }

    // 3. If isoString contains a UTCDesignator, then
    if (parse_result->utc_designator.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeStringUTCDesignator, iso_string);
    }

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Assert: ParseText(! StringToCodePoints(isoString), CalendarDate) is a List of errors.
    // 6. Assert: ParseText(! StringToCodePoints(isoString), DateSpecYearMonth) is a List of errors.
    // 7. Assert: ParseText(! StringToCodePoints(isoString), DateSpecMonthDay) is a List of errors.

    // 8. Return the Record { [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalTime { .hour = result.hour, .minute = result.minute, .second = result.second, .millisecond = result.millisecond, .microsecond = result.microsecond, .nanosecond = result.nanosecond, .calendar = move(result.calendar) };
}

// 13.43 ParseTemporalTimeZoneString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimezonestring
ThrowCompletionOr<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let parseResult be ParseText(! StringToCodePoints(isoString), TemporalTimeZoneString).
    auto parse_result = parse_iso8601(Production::TemporalTimeZoneString, iso_string);

    // 3. If parseResult is a List of errors, then
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeZoneString, iso_string);
    }

    // 4. Let each of z, offsetString, and name be the source text matched by the respective UTCDesignator, TimeZoneNumericUTCOffset, and TimeZoneIANAName Parse Node enclosed by parseResult, or an empty sequence of code points if not present.
    auto z = parse_result->utc_designator;
    auto offset_string = parse_result->time_zone_numeric_utc_offset;
    auto name = parse_result->time_zone_iana_name;

    // 5. If name is empty, then
    //    a. Set name to undefined.
    // 6. Else,
    //    a. Set name to ! CodePointsToString(name).
    // NOTE: No-op.

    // 7. If z is not empty, then
    if (z.has_value()) {
        // a. Return the Record { [[Z]]: true, [[OffsetString]]: undefined, [[Name]]: name }.
        return TemporalTimeZone { .z = true, .offset_string = {}, .name = Optional<String>(move(name)) };
    }

    // 8. If offsetString is empty, then
    //    a. Set offsetString to undefined.
    // 9. Else,
    //    a. Set offsetString to ! CodePointsToString(offsetString).
    // NOTE: No-op.

    // 10. Return the Record { [[Z]]: false, [[OffsetString]]: offsetString, [[Name]]: name }.
    return TemporalTimeZone { .z = false, .offset_string = Optional<String>(move(offset_string)), .name = Optional<String>(move(name)) };
}

// 13.44 ParseTemporalYearMonthString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalyearmonthstring
ThrowCompletionOr<TemporalYearMonth> parse_temporal_year_month_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalYearMonthString (see 13.33), then
    auto parse_result = parse_iso8601(Production::TemporalYearMonthString, iso_string);
    if (!parse_result.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidYearMonthString, iso_string);
    }

    // 3. If isoString contains a UTCDesignator, then
    if (parse_result->utc_designator.has_value()) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidYearMonthStringUTCDesignator, iso_string);
    }

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalYearMonth { .year = result.year, .month = result.month, .day = result.day, .calendar = move(result.calendar) };
}

// 13.45 ToPositiveInteger ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-topositiveinteger
ThrowCompletionOr<double> to_positive_integer(GlobalObject& global_object, Value argument)
{
    auto& vm = global_object.vm();

    // 1. Let integer be ? ToIntegerThrowOnInfinity(argument).
    auto integer = TRY(to_integer_throw_on_infinity(global_object, argument, ErrorType::TemporalPropertyMustBePositiveInteger));

    // 2. If integer ≤ 0, then
    if (integer <= 0) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalPropertyMustBePositiveInteger);
    }

    // 3. Return integer.
    return integer;
}

// 13.48 PrepareTemporalFields ( fields, fieldNames, requiredFields ), https://tc39.es/proposal-temporal/#sec-temporal-preparetemporalfields
ThrowCompletionOr<Object*> prepare_temporal_fields(GlobalObject& global_object, Object const& fields, Vector<String> const& field_names, Vector<StringView> const& required_fields)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let result be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* result = Object::create(global_object, global_object.object_prototype());
    VERIFY(result);

    // 3. For each value property of fieldNames, do
    for (auto& property : field_names) {
        // a. Let value be ? Get(fields, property).
        auto value = TRY(fields.get(property));

        // b. If value is undefined, then
        if (value.is_undefined()) {
            // i. If requiredFields contains property, then
            if (required_fields.contains_slow(property)) {
                // 1. Throw a TypeError exception.
                return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, property);
            }
            // ii. If property is in the Property column of Table 13, then
            // NOTE: The other properties in the table are automatically handled as their default value is undefined
            if (property.is_one_of("hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
                // 1. Set value to the corresponding Default value of the same row.
                value = Value(0);
            }
        }
        // c. Else,
        else {
            // i. If property is in the Property column of Table 13 and there is a Conversion value in the same row, then
            // 1. Let Conversion represent the abstract operation named by the Conversion value of the same row.
            // 2. Set value to ? Conversion(value).
            if (property.is_one_of("year"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv, "eraYear"sv))
                value = Value(TRY(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite)));
            else if (property.is_one_of("month"sv, "day"sv))
                value = Value(TRY(to_positive_integer(global_object, value)));
            else if (property.is_one_of("monthCode"sv, "offset"sv, "era"sv))
                value = TRY(value.to_primitive_string(global_object));
        }

        // d. Perform ! CreateDataPropertyOrThrow(result, property, value).
        MUST(result->create_data_property_or_throw(property, value));
    }

    // 4. Return result.
    return result;
}

// 13.49 PreparePartialTemporalFields ( fields, fieldNames ), https://tc39.es/proposal-temporal/#sec-temporal-preparepartialtemporalfields
ThrowCompletionOr<Object*> prepare_partial_temporal_fields(GlobalObject& global_object, Object const& fields, Vector<String> const& field_names)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let result be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* result = Object::create(global_object, global_object.object_prototype());

    // 3. Let any be false.
    auto any = false;

    // 4. For each value property of fieldNames, do
    for (auto& property : field_names) {
        // a. Let value be ? Get(fields, property).
        auto value = TRY(fields.get(property));

        // b. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. If property is in the Property column of Table 13, then
            // 1. Let Conversion represent the abstract operation named by the Conversion value of the same row.
            // 2. Set value to ? Conversion(value).
            if (property.is_one_of("year"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv, "eraYear"sv))
                value = Value(TRY(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite)));
            else if (property.is_one_of("month"sv, "day"sv))
                value = Value(TRY(to_positive_integer(global_object, value)));
            else if (property.is_one_of("monthCode"sv, "offset"sv, "era"sv))
                value = TRY(value.to_primitive_string(global_object));

            // iii. Perform ! CreateDataPropertyOrThrow(result, property, value).
            MUST(result->create_data_property_or_throw(property, value));
        }
    }

    // 5. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalObjectMustHaveOneOf, String::join(", "sv, field_names));
    }

    // 6. Return result.
    return result;
}

}
