/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/DateTimeLexer.h>
#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

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
    auto iterator_record = get_iterator(global_object, items, IteratorHint::Sync);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());

    // 2. Let values be a new empty List.
    MarkedValueList values(heap);

    // 3. Let next be true.
    auto next = true;
    // 4. Repeat, while next is not false,
    while (next) {
        // a. Set next to ? IteratorStep(iteratorRecord).
        auto* iterator_result = iterator_step(global_object, *iterator_record);
        if (auto* exception = vm.exception())
            return throw_completion(exception->value());
        next = iterator_result;

        // b. If next is not false, then
        if (next) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = iterator_value(global_object, *iterator_result);
            if (auto* exception = vm.exception())
                return throw_completion(exception->value());
            // ii. If Type(nextValue) is not an element of elementTypes, then
            if (auto type = to_option_type(next_value); !type.has_value() || !element_types.contains_slow(*type)) {
                // 1. Let completion be ThrowCompletion(a newly created TypeError object).
                auto completion = vm.throw_completion<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
                // 2. Return ? IteratorClose(iteratorRecord, completion).
                iterator_close(*iterator_record);
                return completion;
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
Value get_option(GlobalObject& global_object, Object const& options, PropertyName const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback)
{
    VERIFY(property.is_string());

    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.
    // 2. Assert: Each element of types is Boolean, String, or Number.

    // 3. Let value be ? Get(options, property).
    auto value = options.get(property);
    if (vm.exception())
        return {};

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
        value = value.to_number(global_object);
        if (vm.exception())
            return {};
        // b. If value is NaN, throw a RangeError exception.
        if (value.is_nan()) {
            vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, vm.names.NaN.as_string(), property.as_string());
            return {};
        }
    }
    // 9. Else,
    else {
        // a. Set value to ? ToString(value).
        value = value.to_primitive_string(global_object);
        if (vm.exception())
            return {};
    }

    // 10. If values is not empty, then
    if (!values.is_empty()) {
        VERIFY(value.is_string());
        // a. If values does not contain value, throw a RangeError exception.
        if (!values.contains_slow(value.as_string().string())) {
            vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_string().string(), property.as_string());
            return {};
        }
    }

    // 11. Return value.
    return value;
}

// 13.4 GetStringOrNumberOption ( options, property, stringValues, minimum, maximum, fallback ), https://tc39.es/proposal-temporal/#sec-getstringornumberoption
template<typename NumberType>
Optional<Variant<String, NumberType>> get_string_or_number_option(GlobalObject& global_object, Object const& options, PropertyName const& property, Vector<StringView> const& string_values, NumberType minimum, NumberType maximum, Value fallback)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(options) is Object.

    // 2. Let value be ? GetOption(options, property, « Number, String », empty, fallback).
    auto value = get_option(global_object, options, property, { OptionType::Number, OptionType::String }, {}, fallback);
    if (vm.exception())
        return {};

    // 3. If Type(value) is Number, then
    if (value.is_number()) {
        // a. If value < minimum or value > maximum, throw a RangeError exception.
        if (value.as_double() < minimum || value.as_double() > maximum) {
            vm.template throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_double(), property.as_string());
            return {};
        }

        // b. Return floor(ℝ(value)).
        return floor(value.as_double());
    }

    // 4. Assert: Type(value) is String.
    VERIFY(value.is_string());

    // 5. If stringValues does not contain value, throw a RangeError exception.
    if (!string_values.contains_slow(value.as_string().string())) {
        vm.template throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_string().string(), property.as_string());
        return {};
    }

    // 6. Return value.
    return value.as_string().string();
}

// 13.6 ToTemporalOverflow ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaloverflow
Optional<String> to_temporal_overflow(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "overflow", « String », « "constrain", "reject" », "constrain").
    auto option = get_option(global_object, normalized_options, vm.names.overflow, { OptionType::String }, { "constrain"sv, "reject"sv }, js_string(vm, "constrain"));
    if (vm.exception())
        return {};

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.8 ToTemporalRoundingMode ( normalizedOptions, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingmode
Optional<String> to_temporal_rounding_mode(GlobalObject& global_object, Object const& normalized_options, String const& fallback)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "roundingMode", « String », « "ceil", "floor", "trunc", "halfExpand" », fallback).
    auto option = get_option(global_object, normalized_options, vm.names.roundingMode, { OptionType::String }, { "ceil"sv, "floor"sv, "trunc"sv, "halfExpand"sv }, js_string(vm, fallback));
    if (vm.exception())
        return {};

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.11 ToShowCalendarOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowcalendaroption
Optional<String> to_show_calendar_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "calendarName", « String », « "auto", "always", "never" », "auto").
    auto option = get_option(global_object, normalized_options, vm.names.calendarName, { OptionType::String }, { "auto"sv, "always"sv, "never"sv }, js_string(vm, "auto"sv));
    if (vm.exception())
        return {};

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.14 ToTemporalRoundingIncrement ( normalizedOptions, dividend, inclusive ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingincrement
u64 to_temporal_rounding_increment(GlobalObject& global_object, Object const& normalized_options, Optional<double> dividend, bool inclusive)
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
    auto increment_value = get_option(global_object, normalized_options, vm.names.roundingIncrement, { OptionType::Number }, {}, Value(1));
    if (vm.exception())
        return {};
    VERIFY(increment_value.is_number());
    auto increment = increment_value.as_double();

    // 6. If increment < 1 or increment > maximum, throw a RangeError exception.
    if (increment < 1 || increment > maximum) {
        vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, increment, "roundingIncrement");
        return {};
    }

    // 7. Set increment to floor(ℝ(increment)).
    auto floored_increment = static_cast<u64>(increment);

    // 8. If dividend is not undefined and dividend modulo increment is not zero, then
    if (dividend.has_value() && static_cast<u64>(*dividend) % floored_increment != 0) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, increment, "roundingIncrement");
        return {};
    }

    // 9. Return increment.
    return floored_increment;
}

// 13.16 ToSecondsStringPrecision ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-tosecondsstringprecision
Optional<SecondsStringPrecision> to_seconds_string_precision(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // Let smallestUnit be ? ToSmallestTemporalUnit(normalizedOptions, « "year", "month", "week", "day", "hour" », undefined).
    auto smallest_unit = to_smallest_temporal_unit(global_object, normalized_options, { "year"sv, "month"sv, "week"sv, "day"sv, "hour"sv }, {});
    if (vm.exception())
        return {};

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
    auto digits_variant = get_string_or_number_option<u8>(global_object, normalized_options, vm.names.fractionalSecondDigits, { "auto"sv }, 0, 9, js_string(vm, "auto"sv));
    if (vm.exception())
        return {};

    // 9. If digits is "auto", then
    if (digits_variant->has<String>()) {
        VERIFY(digits_variant->get<String>() == "auto"sv);
        // a. Return the Record { [[Precision]]: "auto", [[Unit]]: "nanosecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = "auto"sv, .unit = "nanosecond"sv, .increment = 1 };
    }

    auto digits = digits_variant->get<u8>();

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

// 13.17 ToLargestTemporalUnit ( normalizedOptions, disallowedUnits, fallback [ , autoValue ] ), https://tc39.es/proposal-temporal/#sec-temporal-tolargesttemporalunit
Optional<String> to_largest_temporal_unit(GlobalObject& global_object, Object const& normalized_options, Vector<StringView> const& disallowed_units, String const& fallback, Optional<String> auto_value)
{
    auto& vm = global_object.vm();

    // 1. Assert: disallowedUnits does not contain fallback.
    // 2. Assert: disallowedUnits does not contain "auto".
    // 3. Assert: autoValue is not present or fallback is "auto".
    VERIFY(!auto_value.has_value() || fallback == "auto"sv);
    // 4. Assert: autoValue is not present or disallowedUnits does not contain autoValue.

    // 5. Let largestUnit be ? GetOption(normalizedOptions, "largestUnit", « String », « "auto", "year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond", "microseconds", "nanosecond", "nanoseconds" », fallback).
    auto largest_unit_value = get_option(global_object, normalized_options, vm.names.largestUnit, { OptionType::String }, { "auto"sv, "year"sv, "years"sv, "month"sv, "months"sv, "week"sv, "weeks"sv, "day"sv, "days"sv, "hour"sv, "hours"sv, "minute"sv, "minutes"sv, "second"sv, "seconds"sv, "millisecond"sv, "milliseconds"sv, "microsecond"sv, "microseconds"sv, "nanosecond"sv, "nanoseconds"sv }, js_string(vm, fallback));
    if (vm.exception())
        return {};
    auto largest_unit = largest_unit_value.as_string().string();

    // 6. If largestUnit is "auto" and autoValue is present, then
    if (largest_unit == "auto"sv && auto_value.has_value()) {
        // a. Return autoValue.
        return auto_value;
    }

    // 7. If largestUnit is in the Plural column of Table 12, then
    if (auto singular_unit = plural_to_singular_units.get(largest_unit); singular_unit.has_value()) {
        // a. Set largestUnit to the corresponding Singular value of the same row.
        largest_unit = singular_unit.value();
    }

    // 8. If disallowedUnits contains largestUnit, then
    if (disallowed_units.contains_slow(largest_unit)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, largest_unit, vm.names.largestUnit.as_string());
        return {};
    }

    // 9. Return largestUnit.
    return largest_unit;
}

// 13.18 ToSmallestTemporalUnit ( normalizedOptions, disallowedUnits, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-tosmallesttemporalunit
Optional<String> to_smallest_temporal_unit(GlobalObject& global_object, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback)
{
    auto& vm = global_object.vm();

    // 1. Assert: disallowedUnits does not contain fallback.

    // 2. Let smallestUnit be ? GetOption(normalizedOptions, "smallestUnit", « String », « "year", "years", "month", "months", "week", "weeks", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond", "microseconds", "nanosecond", "nanoseconds" », fallback).
    auto smallest_unit_value = get_option(global_object, normalized_options, vm.names.smallestUnit, { OptionType::String }, { "year"sv, "years"sv, "month"sv, "months"sv, "week"sv, "weeks"sv, "day"sv, "days"sv, "hour"sv, "hours"sv, "minute"sv, "minutes"sv, "second"sv, "seconds"sv, "millisecond"sv, "milliseconds"sv, "microsecond"sv, "microseconds"sv, "nanosecond"sv, "nanoseconds"sv }, fallback.has_value() ? js_string(vm, *fallback) : js_undefined());
    if (vm.exception())
        return {};

    // OPTIMIZATION: We skip the following string-only checks for the fallback to tidy up the code a bit
    if (smallest_unit_value.is_undefined())
        return {};
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
        vm.throw_exception<RangeError>(global_object, ErrorType::OptionIsNotValidValue, smallest_unit, vm.names.smallestUnit.as_string());
        return {};
    }

    // 5. Return smallestUnit.
    return smallest_unit;
}

// 13.22 ValidateTemporalUnitRange ( largestUnit, smallestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-validatetemporalunitrange
void validate_temporal_unit_range(GlobalObject& global_object, StringView largest_unit, StringView smallest_unit)
{
    auto& vm = global_object.vm();

    // 1. If smallestUnit is "year" and largestUnit is not "year", then
    if (smallest_unit == "year"sv && largest_unit != "year"sv) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 2. If smallestUnit is "month" and largestUnit is not "year" or "month", then
    if (smallest_unit == "month"sv && !largest_unit.is_one_of("year"sv, "month"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 3. If smallestUnit is "week" and largestUnit is not one of "year", "month", or "week", then
    if (smallest_unit == "week"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 4. If smallestUnit is "day" and largestUnit is not one of "year", "month", "week", or "day", then
    if (smallest_unit == "day"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 5. If smallestUnit is "hour" and largestUnit is not one of "year", "month", "week", "day", or "hour", then
    if (smallest_unit == "hour"sv && !largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv, "hour"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 6. If smallestUnit is "minute" and largestUnit is "second", "millisecond", "microsecond", or "nanosecond", then
    if (smallest_unit == "minute"sv && largest_unit.is_one_of("second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 7. If smallestUnit is "second" and largestUnit is "millisecond", "microsecond", or "nanosecond", then
    if (smallest_unit == "second"sv && largest_unit.is_one_of("millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 8. If smallestUnit is "millisecond" and largestUnit is "microsecond" or "nanosecond", then
    if (smallest_unit == "millisecond"sv && largest_unit.is_one_of("microsecond"sv, "nanosecond"sv)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
    // 9. If smallestUnit is "microsecond" and largestUnit is "nanosecond", then
    if (smallest_unit == "microsecond"sv && largest_unit == "nanosecond"sv) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, smallest_unit, largest_unit);
        return;
    }
}

// 13.23 LargerOfTwoTemporalUnits ( u1, u2 ), https://tc39.es/proposal-temporal/#sec-temporal-largeroftwotemporalunits
String larger_of_two_temporal_units(StringView unit1, StringView unit2)
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

// 13.25 MaximumTemporalDurationRoundingIncrement ( unit ), https://tc39.es/proposal-temporal/#sec-temporal-maximumtemporaldurationroundingincrement
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

// 13.26 RejectTemporalCalendarType ( object ), https://tc39.es/proposal-temporal/#sec-temporal-rejecttemporalcalendartype
void reject_temporal_calendar_type(GlobalObject& global_object, Object& object)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(object) is Object.

    // 2. If object has an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], [[InitializedTemporalTime]], [[InitializedTemporalYearMonth]], or [[InitializedTemporalZonedDateTime]] internal slot, then
    if (is<PlainDate>(object) || is<PlainDateTime>(object) || is<PlainMonthDay>(object) || is<PlainTime>(object) || is<PlainYearMonth>(object) || is<ZonedDateTime>(object)) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalPlainTimeWithArgumentMustNotHave, "calendar or timeZone");
    }
}

// 13.27 FormatSecondsStringPart ( second, millisecond, microsecond, nanosecond, precision ), https://tc39.es/proposal-temporal/#sec-temporal-formatsecondsstringpart
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

// 13.29 ConstrainToRange ( x, minimum, maximum ), https://tc39.es/proposal-temporal/#sec-temporal-constraintorange
double constrain_to_range(double x, double minimum, double maximum)
{
    return min(max(x, minimum), maximum);
}

// 13.32 RoundNumberToIncrement ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrement
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

    // OPTIMIZATION: If theres no remainder there number is already rounded
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

// 13.34 ParseISODateTime ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parseisodatetime
Optional<ISODateTime> parse_iso_date_time(GlobalObject& global_object, [[maybe_unused]] String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. Let year, month, day, hour, minute, second, fraction, and calendar be the parts of isoString produced respectively by the DateYear, DateMonth, DateDay, TimeHour, TimeMinute, TimeSecond, TimeFractionalPart, and CalendarName productions, or undefined if not present.
    Optional<StringView> year_part;
    Optional<StringView> month_part;
    Optional<StringView> day_part;
    Optional<StringView> hour_part;
    Optional<StringView> minute_part;
    Optional<StringView> second_part;
    Optional<StringView> fraction_part;
    Optional<StringView> calendar_part;
    TODO();

    // 3. Let year be the part of isoString produced by the DateYear production.
    // 4. If the first code unit of year is 0x2212 (MINUS SIGN), replace it with the code unit 0x002D (HYPHEN-MINUS).
    String normalized_year;
    if (year_part.has_value() && year_part->starts_with("\xE2\x88\x92"sv))
        normalized_year = String::formatted("-{}", year_part->substring_view(3));
    else
        normalized_year = year_part.value_or("");

    // 5. Set year to ! ToIntegerOrInfinity(year).
    i32 year = Value(js_string(vm, normalized_year)).to_integer_or_infinity(global_object);

    u8 month;
    // 6. If month is undefined, then
    if (!month_part.has_value()) {
        // a. Set month to 1.
        month = 1;
    }
    // 7. Else,
    else {
        // a. Set month to ! ToIntegerOrInfinity(month).
        month = *month_part->to_uint<u8>();
    }

    u8 day;
    // 8. If day is undefined, then
    if (!day_part.has_value()) {
        // a. Set day to 1.
        day = 1;
    }
    // 9. Else,
    else {
        // a. Set day to ! ToIntegerOrInfinity(day).
        day = *day_part->to_uint<u8>();
    }

    // 10. Set hour to ! ToIntegerOrInfinity(hour).
    u8 hour = hour_part->to_uint<u8>().value_or(0);

    // 11. Set minute to ! ToIntegerOrInfinity(minute).
    u8 minute = minute_part->to_uint<u8>().value_or(0);

    // 12. Set second to ! ToIntegerOrInfinity(second).
    u8 second = second_part->to_uint<u8>().value_or(0);

    // 13. If second is 60, then
    if (second == 60) {
        // a. Set second to 59.
        second = 59;
    }

    u16 millisecond;
    u16 microsecond;
    u16 nanosecond;
    // 14. If fraction is not undefined, then
    if (fraction_part.has_value()) {
        // a. Set fraction to the string-concatenation of the previous value of fraction and the string "000000000".
        auto fraction = String::formatted("{}000000000", *fraction_part);
        // b. Let millisecond be the String value equal to the substring of fraction from 0 to 3.
        // c. Set millisecond to ! ToIntegerOrInfinity(millisecond).
        millisecond = *fraction.substring(0, 3).to_uint<u16>();
        // d. Let microsecond be the String value equal to the substring of fraction from 3 to 6.
        // e. Set microsecond to ! ToIntegerOrInfinity(microsecond).
        microsecond = *fraction.substring(3, 3).to_uint<u16>();
        // f. Let nanosecond be the String value equal to the substring of fraction from 6 to 9.
        // g. Set nanosecond to ! ToIntegerOrInfinity(nanosecond).
        nanosecond = *fraction.substring(6, 3).to_uint<u16>();
    }
    // 15. Else,
    else {
        // a. Let millisecond be 0.
        millisecond = 0;
        // b. Let microsecond be 0.
        microsecond = 0;
        // c. Let nanosecond be 0.
        nanosecond = 0;
    }

    // 16. If ! IsValidISODate(year, month, day) is false, throw a RangeError exception.
    if (!is_valid_iso_date(year, month, day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidISODate);
        return {};
    }

    // 17. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
    if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidTime);
        return {};
    }

    // 18. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond, [[Calendar]]: calendar }.
    return ISODateTime { .year = year, .month = month, .day = day, .hour = hour, .minute = minute, .second = second, .millisecond = millisecond, .microsecond = microsecond, .nanosecond = nanosecond, .calendar = calendar_part.has_value() ? *calendar_part : Optional<String>() };
}

// 13.35 ParseTemporalInstantString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalinstantstring
Optional<TemporalInstant> parse_temporal_instant_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalInstantString (see 13.33), then
    // a. Throw a RangeError exception.
    // TODO

    // 3. Let result be ! ParseISODateTime(isoString).
    auto result = parse_iso_date_time(global_object, iso_string);

    // 4. Let timeZoneResult be ? ParseTemporalTimeZoneString(isoString).
    auto time_zone_result = parse_temporal_time_zone_string(global_object, iso_string);
    if (vm.exception())
        return {};

    // 5. Let offsetString be timeZoneResult.[[OffsetString]].
    auto offset_string = time_zone_result->offset;

    // 6. If timeZoneResult.[[Z]] is true, then
    if (time_zone_result->z) {
        // a. Set offsetString to "+00:00".
        offset_string = "+00:00"sv;
    }

    // 7. Assert: offsetString is not undefined.
    VERIFY(offset_string.has_value());

    // 8. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[TimeZoneOffsetString]]: offsetString }.
    return TemporalInstant { .year = result->year, .month = result->month, .day = result->day, .hour = result->hour, .minute = result->minute, .second = result->second, .millisecond = result->millisecond, .microsecond = result->microsecond, .nanosecond = result->nanosecond, .time_zone_offset = move(offset_string) };
}

// 13.37 ParseTemporalCalendarString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalcalendarstring
Optional<String> parse_temporal_calendar_string(GlobalObject& global_object, [[maybe_unused]] String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalCalendarString (see 13.33), then
    // a. Throw a RangeError exception.
    // 3. Let id be the part of isoString produced by the CalendarName production, or undefined if not present.
    Optional<StringView> id_part;
    TODO();

    // 4. If id is undefined, then
    if (!id_part.has_value()) {
        // a. Return "iso8601".
        return "iso8601";
    }

    // 5. If ! IsBuiltinCalendar(id) is false, then
    if (!is_builtin_calendar(*id_part)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidCalendarIdentifier, *id_part);
        return {};
    }

    // 6. Return id.
    return id_part.value();
}

// 13.38 ParseTemporalDateString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatestring
Optional<TemporalDate> parse_temporal_date_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalDateString (see 13.33), then
    // a. Throw a RangeError exception.
    // TODO

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = parse_iso_date_time(global_object, iso_string);
    if (vm.exception())
        return {};

    // 4. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalDate { .year = result->year, .month = result->month, .day = result->day, .calendar = move(result->calendar) };
}

// 13.39 ParseTemporalDateTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatetimestring
Optional<ISODateTime> parse_temporal_date_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.33), then
    // a. Throw a RangeError exception.
    // TODO

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = parse_iso_date_time(global_object, iso_string);
    if (vm.exception())
        return {};

    // 4. Return result.
    return result;
}

// 13.40 ParseTemporalDurationString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldurationstring
Optional<TemporalDuration> parse_temporal_duration_string(GlobalObject& global_object, String const& iso_string)
{
    (void)global_object;
    (void)iso_string;
    TODO();
}

// 13.43 ParseTemporalTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimestring
Optional<TemporalTime> parse_temporal_time_string(GlobalObject& global_object, [[maybe_unused]] String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalTimeString (see 13.33), then
    // a. Throw a RangeError exception.
    // TODO

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = parse_iso_date_time(global_object, iso_string);
    if (vm.exception())
        return {};

    // 4. Return the Record { [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalTime { .hour = result->hour, .minute = result->minute, .second = result->second, .millisecond = result->millisecond, .microsecond = result->microsecond, .nanosecond = result->nanosecond, .calendar = move(result->calendar) };
}

// 13.44 ParseTemporalTimeZoneString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimezonestring
Optional<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject& global_object, [[maybe_unused]] String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalTimeZoneString (see 13.33), then
    // a. Throw a RangeError exception.
    // 3. Let z, sign, hours, minutes, seconds, fraction and name be the parts of isoString produced respectively by the UTCDesignator, TimeZoneUTCOffsetSign, TimeZoneUTCOffsetHour, TimeZoneUTCOffsetMinute, TimeZoneUTCOffsetSecond, TimeZoneUTCOffsetFraction, and TimeZoneIANAName productions, or undefined if not present.
    Optional<StringView> z_part;
    Optional<StringView> sign_part;
    Optional<StringView> hours_part;
    Optional<StringView> minutes_part;
    Optional<StringView> seconds_part;
    Optional<StringView> fraction_part;
    Optional<StringView> name_part;
    TODO();

    // 4. If z is not undefined, then
    if (z_part.has_value()) {
        // a. Return the Record { [[Z]]: true, [[OffsetString]]: undefined, [[Name]]: name }.
        return TemporalTimeZone { .z = true, .offset = {}, .name = name_part.has_value() ? String { *name_part } : Optional<String> {} };
    }

    Optional<String> offset;
    // 5. If hours is undefined, then
    if (!hours_part.has_value()) {
        // a. Let offsetString be undefined.
        // NOTE: No-op.
    }
    // 6. Else,
    else {
        // a. Assert: sign is not undefined.
        VERIFY(sign_part.has_value());

        // b. Set hours to ! ToIntegerOrInfinity(hours).
        u8 hours = Value(js_string(vm, *hours_part)).to_integer_or_infinity(global_object);

        u8 sign;
        // c. If sign is the code unit 0x002D (HYPHEN-MINUS) or the code unit 0x2212 (MINUS SIGN), then
        if (sign_part->is_one_of("-", "\u2212")) {
            // i. Set sign to −1.
            sign = -1;
        }
        // d. Else,
        else {
            // i. Set sign to 1.
            sign = 1;
        }

        // e. Set minutes to ! ToIntegerOrInfinity(minutes).
        u8 minutes = Value(js_string(vm, minutes_part.value_or(""sv))).to_integer_or_infinity(global_object);

        // f. Set seconds to ! ToIntegerOrInfinity(seconds).
        u8 seconds = Value(js_string(vm, seconds_part.value_or(""sv))).to_integer_or_infinity(global_object);

        i32 nanoseconds;
        // g. If fraction is not undefined, then
        if (fraction_part.has_value()) {
            // i. Set fraction to the string-concatenation of the previous value of fraction and the string "000000000".
            auto fraction = String::formatted("{}000000000", *fraction_part);
            // ii. Let nanoseconds be the String value equal to the substring of fraction from 0 to 9.
            // iii. Set nanoseconds to ! ToIntegerOrInfinity(nanoseconds).
            nanoseconds = Value(js_string(vm, fraction.substring(0, 9))).to_integer_or_infinity(global_object);
        }
        // h. Else,
        else {
            // i. Let nanoseconds be 0.
            nanoseconds = 0;
        }
        // i. Let offsetNanoseconds be sign × (((hours × 60 + minutes) × 60 + seconds) × 10^9 + nanoseconds).
        auto offset_nanoseconds = sign * (((hours * 60 + minutes) * 60 + seconds) * 1000000000 + nanoseconds);
        // j. Let offsetString be ! FormatTimeZoneOffsetString(offsetNanoseconds).
        offset = format_time_zone_offset_string(offset_nanoseconds);
    }

    Optional<String> name;
    // 7. If name is not undefined, then
    if (name_part.has_value()) {
        // a. If ! IsValidTimeZoneName(name) is false, throw a RangeError exception.
        if (!is_valid_time_zone_name(*name_part)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidTimeZoneName);
            return {};
        }
        // b. Set name to ! CanonicalizeTimeZoneName(name).
        name = canonicalize_time_zone_name(*name_part);
    }

    // 8. Return the Record { [[Z]]: false, [[OffsetString]]: offsetString, [[Name]]: name }.
    return TemporalTimeZone { .z = false, .offset = offset, .name = name };
}

// 13.45 ParseTemporalYearMonthString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalyearmonthstring
Optional<TemporalYearMonth> parse_temporal_year_month_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(isoString) is String.

    // 2. If isoString does not satisfy the syntax of a TemporalYearMonthString (see 13.33), then
    // a. Throw a RangeError exception.
    // TODO

    // 3. Let result be ? ParseISODateTime(isoString).
    auto result = parse_iso_date_time(global_object, iso_string);
    if (vm.exception())
        return {};

    // 4. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalYearMonth { .year = result->year, .month = result->month, .day = result->day, .calendar = move(result->calendar) };
}

// 13.46 ToPositiveInteger ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-topositiveinteger
double to_positive_integer(GlobalObject& global_object, Value argument)
{
    auto& vm = global_object.vm();

    // 1. Let integer be ? ToIntegerThrowOnInfinity(argument).
    auto integer = TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, argument, ErrorType::TemporalPropertyMustBePositiveInteger));

    // 2. If integer ≤ 0, then
    if (integer <= 0) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalPropertyMustBePositiveInteger);
        return {};
    }

    // 3. Return integer.
    return integer;
}

// 13.48 PrepareTemporalFields ( fields, fieldNames, requiredFields ), https://tc39.es/proposal-temporal/#sec-temporal-preparetemporalfields
Object* prepare_temporal_fields(GlobalObject& global_object, Object const& fields, Vector<String> const& field_names, Vector<StringView> const& required_fields)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let result be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* result = Object::create(global_object, global_object.object_prototype());
    VERIFY(result);

    // 3. For each value property of fieldNames, do
    for (auto& property : field_names) {
        // a. Let value be ? Get(fields, property).
        auto value = fields.get(property);
        if (vm.exception())
            return {};

        // b. If value is undefined, then
        if (value.is_undefined()) {
            // i. If requiredFields contains property, then
            if (required_fields.contains_slow(property)) {
                // 1. Throw a TypeError exception.
                vm.throw_exception<TypeError>(global_object, ErrorType::TemporalMissingRequiredProperty, property);
                return {};
            }
            // ii. If property is in the Property column of Table 13, then
            // NOTE: The other properties in the table are automatically handled as their default value is undefined
            if (property.is_one_of("hour", "minute", "second", "millisecond", "microsecond", "nanosecond")) {
                // 1. Set value to the corresponding Default value of the same row.
                value = Value(0);
            }
        }
        // c. Else,
        else {
            // i. If property is in the Property column of Table 13 and there is a Conversion value in the same row, then
            // 1. Let Conversion represent the abstract operation named by the Conversion value of the same row.
            // 2. Set value to ? Conversion(value).
            if (property.is_one_of("year", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond", "eraYear")) {
                value = Value(TRY_OR_DISCARD(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite)));
            } else if (property.is_one_of("month", "day")) {
                value = Value(to_positive_integer(global_object, value));
                if (vm.exception())
                    return {};
            } else if (property.is_one_of("monthCode", "offset", "era")) {
                value = value.to_primitive_string(global_object);
                if (vm.exception())
                    return {};
            }
        }

        // d. Perform ! CreateDataPropertyOrThrow(result, property, value).
        result->create_data_property_or_throw(property, value);
    }

    // 4. Return result.
    return result;
}

}
