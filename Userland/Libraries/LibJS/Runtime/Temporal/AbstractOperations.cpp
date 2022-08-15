/*
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/DateTimeLexer.h>
#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <LibJS/Runtime/AbstractOperations.h>
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
ThrowCompletionOr<MarkedVector<Value>> iterable_to_list_of_type(GlobalObject& global_object, Value items, Vector<OptionType> const& element_types)
{
    auto& vm = global_object.vm();
    auto& heap = global_object.heap();

    // 1. Let iteratorRecord be ? GetIterator(items, sync).
    auto iterator_record = TRY(get_iterator(global_object, items, IteratorHint::Sync));

    // 2. Let values be a new empty List.
    MarkedVector<Value> values(heap);

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
    auto& realm = *global_object.associated_realm();

    // 1. If options is undefined, then
    if (options.is_undefined()) {
        // a. Return OrdinaryObjectCreate(null).
        return Object::create(realm, nullptr);
    }

    // 2. If Type(options) is Object, then
    if (options.is_object()) {
        // a. Return options.
        return &options.as_object();
    }

    // 3. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, "Options");
}

// 13.3 GetOption ( options, property, type, values, fallback ), https://tc39.es/proposal-temporal/#sec-getoption
ThrowCompletionOr<Value> get_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, OptionType type, Span<StringView const> values, OptionDefault const& default_)
{
    VERIFY(property.is_string());

    auto& vm = global_object.vm();

    // 1. Let value be ? Get(options, property).
    auto value = TRY(options.get(property));

    // 2. If value is undefined, then
    if (value.is_undefined()) {
        // a. If default is required, throw a RangeError exception.
        if (default_.has<GetOptionRequired>())
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, "undefined"sv, property.as_string());

        // b. Return default.
        return default_.visit(
            [](GetOptionRequired) -> Value { VERIFY_NOT_REACHED(); },
            [](Empty) { return js_undefined(); },
            [](bool b) { return Value(b); },
            [](double d) { return Value(d); },
            [&vm](StringView s) { return Value(js_string(vm, s)); });
    }

    // 5. If type is "boolean", then
    if (type == OptionType::Boolean) {
        // a. Set value to ToBoolean(value).
        value = Value(value.to_boolean());
    }
    // 6. Else if type is "number", then
    else if (type == OptionType::Number) {
        // a. Set value to ? ToNumber(value).
        value = TRY(value.to_number(global_object));

        // b. If value is NaN, throw a RangeError exception.
        if (value.is_nan())
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, vm.names.NaN.as_string(), property.as_string());
    }
    // 7. Else,
    else {
        // a. Assert: type is "string".
        VERIFY(type == OptionType::String);

        // b. Set value to ? ToString(value).
        value = TRY(value.to_primitive_string(global_object));
    }

    // 8. If values is not undefined and values does not contain an element equal to value, throw a RangeError exception.
    if (!values.is_empty()) {
        // NOTE: Every location in the spec that invokes GetOption with type=boolean also has values=undefined.
        VERIFY(value.is_string());
        if (!values.contains_slow(value.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, value.as_string().string(), property.as_string());
    }

    // 9. Return value.
    return value;
}

// 13.4 ToTemporalOverflow ( options ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaloverflow
ThrowCompletionOr<String> to_temporal_overflow(GlobalObject& global_object, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is undefined, return "constrain".
    if (options == nullptr)
        return "constrain"sv;

    // 2. Return ? GetOption(options, "overflow", "string", « "constrain", "reject" », "constrain").
    auto option = TRY(get_option(global_object, *options, vm.names.overflow, OptionType::String, { "constrain"sv, "reject"sv }, "constrain"sv));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.5 ToTemporalDisambiguation ( options ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldisambiguation
ThrowCompletionOr<String> to_temporal_disambiguation(GlobalObject& global_object, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is undefined, return "compatible".
    if (options == nullptr)
        return "compatible"sv;

    // 2. Return ? GetOption(options, "disambiguation", "string", « "compatible", "earlier", "later", "reject" », "compatible").
    auto option = TRY(get_option(global_object, *options, vm.names.disambiguation, OptionType::String, { "compatible"sv, "earlier"sv, "later"sv, "reject"sv }, "compatible"sv));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.6 ToTemporalRoundingMode ( normalizedOptions, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingmode
ThrowCompletionOr<String> to_temporal_rounding_mode(GlobalObject& global_object, Object const& normalized_options, String const& fallback)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "roundingMode", "string", « "ceil", "floor", "trunc", "halfExpand" », fallback).
    auto option = TRY(get_option(global_object, normalized_options, vm.names.roundingMode, OptionType::String, { "ceil"sv, "floor"sv, "trunc"sv, "halfExpand"sv }, fallback.view()));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.7 NegateTemporalRoundingMode ( roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-negatetemporalroundingmode
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

// 13.8 ToTemporalOffset ( options, fallback ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaloffset
ThrowCompletionOr<String> to_temporal_offset(GlobalObject& global_object, Object const* options, String const& fallback)
{
    auto& vm = global_object.vm();

    // 1. If options is undefined, return fallback.
    if (options == nullptr)
        return fallback;

    // 2. Return ? GetOption(options, "offset", "string", « "prefer", "use", "ignore", "reject" », fallback).
    auto option = TRY(get_option(global_object, *options, vm.names.offset, OptionType::String, { "prefer"sv, "use"sv, "ignore"sv, "reject"sv }, fallback.view()));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.9 ToShowCalendarOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowcalendaroption
ThrowCompletionOr<String> to_show_calendar_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "calendarName", "string", « "auto", "always", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.calendarName, OptionType::String, { "auto"sv, "always"sv, "never"sv }, "auto"sv));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.10 ToShowTimeZoneNameOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowtimezonenameoption
ThrowCompletionOr<String> to_show_time_zone_name_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "timeZoneName", "string, « "auto", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.timeZoneName, OptionType::String, { "auto"sv, "never"sv }, "auto"sv));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.11 ToShowOffsetOption ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-toshowoffsetoption
ThrowCompletionOr<String> to_show_offset_option(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Return ? GetOption(normalizedOptions, "offset", "string", « "auto", "never" », "auto").
    auto option = TRY(get_option(global_object, normalized_options, vm.names.offset, OptionType::String, { "auto"sv, "never"sv }, "auto"sv));

    VERIFY(option.is_string());
    return option.as_string().string();
}

// 13.12 ToTemporalRoundingIncrement ( normalizedOptions, dividend, inclusive ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalroundingincrement
ThrowCompletionOr<u64> to_temporal_rounding_increment(GlobalObject& global_object, Object const& normalized_options, Optional<double> dividend, bool inclusive)
{
    auto& vm = global_object.vm();

    double maximum;
    // 1. If dividend is undefined, then
    if (!dividend.has_value()) {
        // a. Let maximum be +∞𝔽.
        maximum = INFINITY;
    }
    // 2. Else if inclusive is true, then
    else if (inclusive) {
        // a. Let maximum be 𝔽(dividend).
        maximum = *dividend;
    }
    // 3. Else if dividend is more than 1, then
    else if (*dividend > 1) {
        // a. Let maximum be 𝔽(dividend - 1).
        maximum = *dividend - 1;
    }
    // 4. Else,
    else {
        // a. Let maximum be 1𝔽.
        maximum = 1;
    }

    // 5. Let increment be ? GetOption(normalizedOptions, "roundingIncrement", "number", undefined, 1𝔽).
    auto increment_value = TRY(get_option(global_object, normalized_options, vm.names.roundingIncrement, OptionType::Number, {}, 1.0));
    VERIFY(increment_value.is_number());
    auto increment = increment_value.as_double();

    // 6. If increment < 1𝔽 or increment > maximum, throw a RangeError exception.
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

// 13.13 ToTemporalDateTimeRoundingIncrement ( normalizedOptions, smallestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldatetimeroundingincrement
ThrowCompletionOr<u64> to_temporal_date_time_rounding_increment(GlobalObject& global_object, Object const& normalized_options, StringView smallest_unit)
{
    u16 maximum;

    // 1. If smallestUnit is "day", then
    if (smallest_unit == "day"sv) {
        // a. Let maximum be 1.
        maximum = 1;
    }
    // 2. Else,
    else {
        // a. Let maximum be ! MaximumTemporalDurationRoundingIncrement(smallestUnit).
        // b. Assert: maximum is not undefined.
        maximum = *maximum_temporal_duration_rounding_increment(smallest_unit);
    }

    // 3. Return ? ToTemporalRoundingIncrement(normalizedOptions, maximum, false).
    return to_temporal_rounding_increment(global_object, normalized_options, maximum, false);
}

// 13.14 ToSecondsStringPrecision ( normalizedOptions ), https://tc39.es/proposal-temporal/#sec-temporal-tosecondsstringprecision
ThrowCompletionOr<SecondsStringPrecision> to_seconds_string_precision(GlobalObject& global_object, Object const& normalized_options)
{
    auto& vm = global_object.vm();

    // 1. Let smallestUnit be ? GetTemporalUnit(normalizedOptions, "smallestUnit", time, undefined).
    auto smallest_unit = TRY(get_temporal_unit(global_object, normalized_options, vm.names.smallestUnit, UnitGroup::Time, Optional<StringView> {}));

    // 2. If smallestUnit is "hour", throw a RangeError exception.
    if (smallest_unit == "hour"sv)
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, *smallest_unit, "smallestUnit"sv);

    // 3. If smallestUnit is "minute", then
    if (smallest_unit == "minute"sv) {
        // a. Return the Record { [[Precision]]: "minute", [[Unit]]: "minute", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = "minute"sv, .unit = "minute"sv, .increment = 1 };
    }

    // 4. If smallestUnit is "second", then
    if (smallest_unit == "second"sv) {
        // a. Return the Record { [[Precision]]: 0, [[Unit]]: "second", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 0, .unit = "second"sv, .increment = 1 };
    }

    // 5. If smallestUnit is "millisecond", then
    if (smallest_unit == "millisecond"sv) {
        // a. Return the Record { [[Precision]]: 3, [[Unit]]: "millisecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 3, .unit = "millisecond"sv, .increment = 1 };
    }

    // 6. If smallestUnit is "microsecond", then
    if (smallest_unit == "microsecond"sv) {
        // a. Return the Record { [[Precision]]: 6, [[Unit]]: "microsecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 6, .unit = "microsecond"sv, .increment = 1 };
    }

    // 7. If smallestUnit is "nanosecond", then
    if (smallest_unit == "nanosecond"sv) {
        // a. Return the Record { [[Precision]]: 9, [[Unit]]: "nanosecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 9, .unit = "nanosecond"sv, .increment = 1 };
    }

    // 8. Assert: smallestUnit is undefined.
    VERIFY(!smallest_unit.has_value());

    // 9. Let fractionalDigitsVal be ? Get(normalizedOptions, "fractionalSecondDigits").
    auto fractional_digits_value = TRY(normalized_options.get(vm.names.fractionalSecondDigits));

    // 10. If Type(fractionalDigitsVal) is not Number, then
    if (!fractional_digits_value.is_number()) {
        // a. If fractionalDigitsVal is not undefined, then
        if (!fractional_digits_value.is_undefined()) {
            // i. If ? ToString(fractionalDigitsVal) is not "auto", throw a RangeError exception.
            if (TRY(fractional_digits_value.to_string(global_object)) != "auto"sv)
                return vm.template throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, fractional_digits_value, "fractionalSecondDigits"sv);
        }

        // b. Return the Record { [[Precision]]: "auto", [[Unit]]: "nanosecond", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = "auto"sv, .unit = "nanosecond"sv, .increment = 1 };
    }

    // 11. If fractionalDigitsVal is NaN, +∞𝔽, or -∞𝔽, throw a RangeError exception.
    if (fractional_digits_value.is_nan() || fractional_digits_value.is_infinity())
        return vm.template throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, fractional_digits_value, "fractionalSecondDigits"sv);

    // 12. Let fractionalDigitCount be RoundTowardsZero(ℝ(fractionalDigitsVal)).
    auto fractional_digit_count_unchecked = trunc(fractional_digits_value.as_double());

    // 13. If fractionalDigitCount < 0 or fractionalDigitCount > 9, throw a RangeError exception.
    if (fractional_digit_count_unchecked < 0 || fractional_digit_count_unchecked > 9)
        return vm.template throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, fractional_digits_value, "fractionalSecondDigits"sv);

    auto fractional_digit_count = static_cast<u8>(fractional_digit_count_unchecked);

    // 14. If fractionalDigitCount is 0, then
    if (fractional_digit_count == 0) {
        // a. Return the Record { [[Precision]]: 0, [[Unit]]: "second", [[Increment]]: 1 }.
        return SecondsStringPrecision { .precision = 0, .unit = "second"sv, .increment = 1 };
    }

    // 15. If fractionalDigitCount is 1, 2, or 3, then
    if (fractional_digit_count == 1 || fractional_digit_count == 2 || fractional_digit_count == 3) {
        // a. Return the Record { [[Precision]]: fractionalDigitCount, [[Unit]]: "millisecond", [[Increment]]: 10^(3 - fractionalDigitCount) }.
        return SecondsStringPrecision { .precision = fractional_digit_count, .unit = "millisecond"sv, .increment = (u32)pow(10, 3 - fractional_digit_count) };
    }

    // 16. If fractionalDigitCount is 4, 5, or 6, then
    if (fractional_digit_count == 4 || fractional_digit_count == 5 || fractional_digit_count == 6) {
        // a. Return the Record { [[Precision]]: fractionalDigitCount, [[Unit]]: "microsecond", [[Increment]]: 10^(6 - fractionalDigitCount) }.
        return SecondsStringPrecision { .precision = fractional_digit_count, .unit = "microsecond"sv, .increment = (u32)pow(10, 6 - fractional_digit_count) };
    }

    // 17. Assert: fractionalDigitCount is 7, 8, or 9.
    VERIFY(fractional_digit_count == 7 || fractional_digit_count == 8 || fractional_digit_count == 9);

    // 18. Return the Record { [[Precision]]: fractionalDigitCount, [[Unit]]: "nanosecond", [[Increment]]: 10^(9 - fractionalDigitCount) }.
    return SecondsStringPrecision { .precision = fractional_digit_count, .unit = "nanosecond"sv, .increment = (u32)pow(10, 9 - fractional_digit_count) };
}

struct TemporalUnit {
    StringView singular;
    StringView plural;
    UnitGroup category;
};

// https://tc39.es/proposal-temporal/#table-temporal-units
static Vector<TemporalUnit> temporal_units = {
    { "year"sv, "years"sv, UnitGroup::Date },
    { "month"sv, "months"sv, UnitGroup::Date },
    { "week"sv, "weeks"sv, UnitGroup::Date },
    { "day"sv, "days"sv, UnitGroup::Date },
    { "hour"sv, "hours"sv, UnitGroup::Time },
    { "minute"sv, "minutes"sv, UnitGroup::Time },
    { "second"sv, "seconds"sv, UnitGroup::Time },
    { "millisecond"sv, "milliseconds"sv, UnitGroup::Time },
    { "microsecond"sv, "microseconds"sv, UnitGroup::Time },
    { "nanosecond"sv, "nanoseconds"sv, UnitGroup::Time }
};

// 13.15 GetTemporalUnit ( normalizedOptions, key, unitGroup, default [ , extraValues ] ), https://tc39.es/proposal-temporal/#sec-temporal-gettemporalunit
ThrowCompletionOr<Optional<String>> get_temporal_unit(GlobalObject& global_object, Object const& normalized_options, PropertyKey const& key, UnitGroup unit_group, TemporalUnitDefault const& default_, Vector<StringView> const& extra_values)
{
    auto& vm = global_object.vm();

    // 1. Let singularNames be a new empty List.
    Vector<StringView> singular_names;

    // 2. For each row of Table 13, except the header row, in table order, do
    for (auto const& row : temporal_units) {
        // a. Let unit be the value in the Singular column of the row.
        auto unit = row.singular;

        // b. If the Category column of the row is date and unitGroup is date or datetime, append unit to singularNames.
        if (row.category == UnitGroup::Date && (unit_group == UnitGroup::Date || unit_group == UnitGroup::DateTime))
            singular_names.append(unit);
        // c. Else if the Category column of the row is time and unitGroup is time or datetime, append unit to singularNames.
        else if (row.category == UnitGroup::Time && (unit_group == UnitGroup::Time || unit_group == UnitGroup::DateTime))
            singular_names.append(unit);
    }

    // 3. If extraValues is present, then
    if (!extra_values.is_empty()) {
        // a. Set singularNames to the list-concatenation of singularNames and extraValues.
        singular_names.extend(extra_values);
    }

    OptionDefault default_value;

    // 4. If default is required, then
    if (default_.has<TemporalUnitRequired>()) {
        // a. Let defaultValue be undefined.
        default_value = {};
    }
    // 5. Else,
    else {
        auto default_string = default_.get<Optional<StringView>>();

        // a. Let defaultValue be default.
        default_value = default_string.has_value() ? OptionDefault { *default_string } : OptionDefault {};

        // b. If defaultValue is not undefined and singularNames does not contain defaultValue, then
        if (default_string.has_value() && !singular_names.contains_slow(*default_string)) {
            // i. Append defaultValue to singularNames.
            singular_names.append(*default_string);
        }
    }

    // 6. Let allowedValues be a copy of singularNames.
    auto allowed_values = singular_names;

    // 7. For each element singularName of singularNames, do
    for (auto const& singular_name : singular_names) {
        for (auto const& row : temporal_units) {
            // a. If singularName is listed in the Singular column of Table 13, then
            if (singular_name == row.singular) {
                // i. Let pluralName be the value in the Plural column of the corresponding row.
                auto plural_name = row.plural;

                // ii. Append pluralName to allowedValues.
                allowed_values.append(plural_name);
            }
        }
    }

    // 8. NOTE: For each singular Temporal unit name that is contained within allowedValues, the corresponding plural name is also contained within it.

    // 9. Let value be ? GetOption(normalizedOptions, key, "string", allowedValues, defaultValue).
    auto option_value = TRY(get_option(global_object, normalized_options, key, OptionType::String, allowed_values.span(), default_value));

    // 10. If value is undefined and default is required, throw a RangeError exception.
    if (option_value.is_undefined() && default_.has<TemporalUnitRequired>())
        return vm.throw_completion<RangeError>(global_object, ErrorType::IsUndefined, String::formatted("{} option value", key.as_string()));

    Optional<String> value = option_value.is_undefined()
        ? Optional<String> {}
        : option_value.as_string().string();

    // 11. If value is listed in the Plural column of Table 13, then
    for (auto const& row : temporal_units) {
        if (row.plural == value) {
            // a. Set value to the value in the Singular column of the corresponding row.
            value = row.singular;
        }
    }

    // 12. Return value.
    return value;
}

// 13.16 ToRelativeTemporalObject ( options ), https://tc39.es/proposal-temporal/#sec-temporal-torelativetemporalobject
ThrowCompletionOr<Value> to_relative_temporal_object(GlobalObject& global_object, Object const& options)
{
    auto& vm = global_object.vm();
    auto& realm = *global_object.associated_realm();

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
        auto* fields = TRY(prepare_temporal_fields(global_object, value_object, field_names, Vector<StringView> {}));

        // f. Let dateOptions be OrdinaryObjectCreate(null).
        auto* date_options = Object::create(realm, nullptr);

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
            // i. If ParseText(StringToCodePoints(timeZoneName), TimeZoneNumericUTCOffset) is a List of errors, then
            if (!is_valid_time_zone_numeric_utc_offset_syntax(*time_zone_name)) {
                // 1. If IsValidTimeZoneName(timeZoneName) is false, throw a RangeError exception.
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

    // 9. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return TRY(create_temporal_date(global_object, result.year, result.month, result.day, *calendar));
}

// 13.17 LargerOfTwoTemporalUnits ( u1, u2 ), https://tc39.es/proposal-temporal/#sec-temporal-largeroftwotemporalunits
StringView larger_of_two_temporal_units(StringView unit1, StringView unit2)
{
    // 1. Assert: Both u1 and u2 are listed in the Singular column of Table 13.

    // 2. For each row of Table 13, except the header row, in table order, do
    for (auto const& row : temporal_units) {
        // a. Let unit be the value in the Singular column of the row.
        auto unit = row.singular;

        // b. If SameValue(u1, unit) is true, return unit.
        if (unit1 == unit)
            return unit;

        // c. If SameValue(u2, unit) is true, return unit.
        if (unit2 == unit)
            return unit;
    }
    VERIFY_NOT_REACHED();
}

// 13.18 MergeLargestUnitOption ( options, largestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-mergelargestunitoption
ThrowCompletionOr<Object*> merge_largest_unit_option(GlobalObject& global_object, Object const& options, String largest_unit)
{
    auto& vm = global_object.vm();
    auto& realm = *global_object.associated_realm();

    // 1. Let merged be OrdinaryObjectCreate(null).
    auto* merged = Object::create(realm, nullptr);

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

// 13.19 MaximumTemporalDurationRoundingIncrement ( unit ), https://tc39.es/proposal-temporal/#sec-temporal-maximumtemporaldurationroundingincrement
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

// 13.20 RejectObjectWithCalendarOrTimeZone ( object ), https://tc39.es/proposal-temporal/#sec-temporal-rejectobjectwithcalendarortimezone
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

// 13.21 FormatSecondsStringPart ( second, millisecond, microsecond, nanosecond, precision ), https://tc39.es/proposal-temporal/#sec-temporal-formatsecondsstringpart
String format_seconds_string_part(u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Variant<StringView, u8> const& precision)
{
    // 1. Assert: second, millisecond, microsecond, and nanosecond are integers.

    // Non-standard sanity check
    if (precision.has<StringView>())
        VERIFY(precision.get<StringView>().is_one_of("minute"sv, "auto"sv));

    // 2. If precision is "minute", return "".
    if (precision.has<StringView>() && precision.get<StringView>() == "minute"sv)
        return String::empty();

    // 3. Let secondsString be the string-concatenation of the code unit 0x003A (COLON) and ToZeroPaddedDecimalString(second, 2).
    auto seconds_string = String::formatted(":{:02}", second);

    // 4. Let fraction be millisecond × 10^6 + microsecond × 10^3 + nanosecond.
    u32 fraction = millisecond * 1'000'000 + microsecond * 1'000 + nanosecond;

    String fraction_string;

    // 5. If precision is "auto", then
    if (precision.has<StringView>() && precision.get<StringView>() == "auto"sv) {
        // a. If fraction is 0, return secondsString.
        if (fraction == 0)
            return seconds_string;

        // b. Set fraction to ToZeroPaddedDecimalString(fraction, 9).
        fraction_string = String::formatted("{:09}", fraction);

        // c. Set fraction to the longest possible substring of fraction starting at position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
        fraction_string = fraction_string.trim("0"sv, TrimMode::Right);
    }
    // 6. Else,
    else {
        // a. If precision is 0, return secondsString.
        if (precision.get<u8>() == 0)
            return seconds_string;

        // b. Set fraction to ToZeroPaddedDecimalString(fraction, 9)
        fraction_string = String::formatted("{:09}", fraction);

        // c. Set fraction to the substring of fraction from 0 to precision.
        fraction_string = fraction_string.substring(0, precision.get<u8>());
    }

    // 7. Return the string-concatenation of secondsString, the code unit 0x002E (FULL STOP), and fraction.
    return String::formatted("{}.{}", seconds_string, fraction_string);
}

// 13.23 GetUnsignedRoundingMode ( roundingMode, isNegative ), https://tc39.es/proposal-temporal/#sec-temporal-getunsignedroundingmode
UnsignedRoundingMode get_unsigned_rounding_mode(StringView rounding_mode, bool is_negative)
{
    // 1. If isNegative is true, return the specification type in the third column of Table 14 where the first column is roundingMode and the second column is "negative".
    if (is_negative) {
        if (rounding_mode == "ceil"sv)
            return UnsignedRoundingMode::Zero;
        if (rounding_mode == "floor"sv)
            return UnsignedRoundingMode::Infinity;
        if (rounding_mode == "expand"sv)
            return UnsignedRoundingMode::Infinity;
        if (rounding_mode == "trunc"sv)
            return UnsignedRoundingMode::Zero;
        if (rounding_mode == "halfCeil"sv)
            return UnsignedRoundingMode::HalfZero;
        if (rounding_mode == "halfFloor"sv)
            return UnsignedRoundingMode::HalfInfinity;
        if (rounding_mode == "halfExpand"sv)
            return UnsignedRoundingMode::HalfInfinity;
        if (rounding_mode == "halfTrunc"sv)
            return UnsignedRoundingMode::HalfZero;
        if (rounding_mode == "halfEven"sv)
            return UnsignedRoundingMode::HalfEven;
        VERIFY_NOT_REACHED();
    }
    // 2. Else, return the specification type in the third column of Table 14 where the first column is roundingMode and the second column is "positive".
    else {
        if (rounding_mode == "ceil"sv)
            return UnsignedRoundingMode::Infinity;
        if (rounding_mode == "floor"sv)
            return UnsignedRoundingMode::Zero;
        if (rounding_mode == "expand"sv)
            return UnsignedRoundingMode::Infinity;
        if (rounding_mode == "trunc"sv)
            return UnsignedRoundingMode::Zero;
        if (rounding_mode == "halfCeil"sv)
            return UnsignedRoundingMode::HalfInfinity;
        if (rounding_mode == "halfFloor"sv)
            return UnsignedRoundingMode::HalfZero;
        if (rounding_mode == "halfExpand"sv)
            return UnsignedRoundingMode::HalfInfinity;
        if (rounding_mode == "halfTrunc"sv)
            return UnsignedRoundingMode::HalfZero;
        if (rounding_mode == "halfEven"sv)
            return UnsignedRoundingMode::HalfEven;
        VERIFY_NOT_REACHED();
    }
}

// NOTE: We have two variants of these functions, one using doubles and one using BigInts - most of the time
// doubles will be fine, but take care to choose the right one. The spec is not very clear about this, as
// it uses mathematical values which can be arbitrarily (but not infinitely) large.
// Incidentally V8's Temporal implementation does the same :^)

// 13.24 ApplyUnsignedRoundingMode ( x, r1, r2, unsignedRoundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-applyunsignedroundingmode
double apply_unsigned_rounding_mode(double x, double r1, double r2, Optional<UnsignedRoundingMode> const& unsigned_rounding_mode)
{
    // 1. If x is equal to r1, return r1.
    if (x == r1)
        return r1;

    // 2. Assert: r1 < x < r2.
    VERIFY(r1 < x && x < r2);

    // 3. Assert: unsignedRoundingMode is not undefined.
    VERIFY(unsigned_rounding_mode.has_value());

    // 4. If unsignedRoundingMode is zero, return r1.
    if (unsigned_rounding_mode == UnsignedRoundingMode::Zero)
        return r1;

    // 5. If unsignedRoundingMode is infinity, return r2.
    if (unsigned_rounding_mode == UnsignedRoundingMode::Infinity)
        return r2;

    // 6. Let d1 be x – r1.
    auto d1 = x - r1;

    // 7. Let d2 be r2 – x.
    auto d2 = r2 - x;

    // 8. If d1 < d2, return r1.
    if (d1 < d2)
        return r1;

    // 9. If d2 < d1, return r2.
    if (d2 < d1)
        return r2;

    // 10. Assert: d1 is equal to d2.
    VERIFY(d1 == d2);

    // 11. If unsignedRoundingMode is half-zero, return r1.
    if (unsigned_rounding_mode == UnsignedRoundingMode::HalfZero)
        return r1;

    // 12. If unsignedRoundingMode is half-infinity, return r2.
    if (unsigned_rounding_mode == UnsignedRoundingMode::HalfInfinity)
        return r2;

    // 13. Assert: unsignedRoundingMode is half-even.
    VERIFY(unsigned_rounding_mode == UnsignedRoundingMode::HalfEven);

    // 14. Let cardinality be (r1 / (r2 – r1)) modulo 2.
    auto cardinality = modulo((r1 / (r2 - r1)), 2);

    // 15. If cardinality is 0, return r1.
    if (cardinality == 0)
        return r1;

    // 16. Return r2.
    return r2;
}

// 13.24 ApplyUnsignedRoundingMode ( x, r1, r2, unsignedRoundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-applyunsignedroundingmode
Crypto::SignedBigInteger apply_unsigned_rounding_mode(Crypto::SignedDivisionResult const& x, Crypto::SignedBigInteger const& r1, Crypto::SignedBigInteger const& r2, Optional<UnsignedRoundingMode> const& unsigned_rounding_mode, Crypto::UnsignedBigInteger const& increment)
{
    // 1. If x is equal to r1, return r1.
    if (x.quotient == r1 && x.remainder.unsigned_value().is_zero())
        return r1;

    // 2. Assert: r1 < x < r2.
    // NOTE: Skipped for the sake of performance

    // 3. Assert: unsignedRoundingMode is not undefined.
    VERIFY(unsigned_rounding_mode.has_value());

    // 4. If unsignedRoundingMode is zero, return r1.
    if (unsigned_rounding_mode == UnsignedRoundingMode::Zero)
        return r1;

    // 5. If unsignedRoundingMode is infinity, return r2.
    if (unsigned_rounding_mode == UnsignedRoundingMode::Infinity)
        return r2;

    // 6. Let d1 be x – r1.
    auto d1 = x.remainder.unsigned_value();

    // 7. Let d2 be r2 – x.
    auto d2 = increment.minus(x.remainder.unsigned_value());

    // 8. If d1 < d2, return r1.
    if (d1 < d2)
        return r1;

    // 9. If d2 < d1, return r2.
    if (d2 < d1)
        return r2;

    // 10. Assert: d1 is equal to d2.
    // NOTE: Skipped for the sake of performance

    // 11. If unsignedRoundingMode is half-zero, return r1.
    if (unsigned_rounding_mode == UnsignedRoundingMode::HalfZero)
        return r1;

    // 12. If unsignedRoundingMode is half-infinity, return r2.
    if (unsigned_rounding_mode == UnsignedRoundingMode::HalfInfinity)
        return r2;

    // 13. Assert: unsignedRoundingMode is half-even.
    VERIFY(unsigned_rounding_mode == UnsignedRoundingMode::HalfEven);

    // 14. Let cardinality be (r1 / (r2 – r1)) modulo 2.
    auto cardinality = modulo(r1.divided_by(r2.minus(r1)).quotient, "2"_bigint);

    // 15. If cardinality is 0, return r1.
    if (cardinality.unsigned_value().is_zero())
        return r1;

    // 16. Return r2.
    return r2;
}

// 13.25 RoundNumberToIncrement ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrement
double round_number_to_increment(double x, u64 increment, StringView rounding_mode)
{
    VERIFY(rounding_mode == "ceil"sv || rounding_mode == "floor"sv || rounding_mode == "trunc"sv || rounding_mode == "halfExpand"sv);

    // 1. Let quotient be x / increment.
    auto quotient = x / static_cast<double>(increment);

    bool is_negative;

    // 2. If quotient < 0, then
    if (quotient < 0) {
        // a. Let isNegative be true.
        is_negative = true;

        // b. Set quotient to -quotient.
        quotient = -quotient;
    }
    // 3. Else,
    else {
        // a. Let isNegative be false.
        is_negative = false;
    }

    // 4. Let unsignedRoundingMode be GetUnsignedRoundingMode(roundingMode, isNegative).
    auto unsigned_rounding_mode = get_unsigned_rounding_mode(rounding_mode, is_negative);

    // 5. Let r1 be the largest integer such that r1 ≤ quotient.
    auto r1 = floor(quotient);

    // 6. Let r2 be the smallest integer such that r2 > quotient.
    auto r2 = ceil(quotient);
    if (quotient == r2)
        r2++;

    // 7. Let rounded be ApplyUnsignedRoundingMode(quotient, r1, r2, unsignedRoundingMode).
    auto rounded = apply_unsigned_rounding_mode(quotient, r1, r2, unsigned_rounding_mode);

    // 8. If isNegative is true, set rounded to -rounded.
    if (is_negative)
        rounded = -rounded;

    // 9. Return rounded × increment.
    return rounded * static_cast<double>(increment);
}

// 13.25 RoundNumberToIncrement ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrement
Crypto::SignedBigInteger round_number_to_increment(Crypto::SignedBigInteger const& x, u64 increment, StringView rounding_mode)
{
    VERIFY(rounding_mode == "ceil"sv || rounding_mode == "floor"sv || rounding_mode == "trunc"sv || rounding_mode == "halfExpand"sv);

    // OPTIMIZATION: If the increment is 1 the number is always rounded
    if (increment == 1)
        return x;

    auto increment_big_int = Crypto::UnsignedBigInteger::create_from(increment);

    // 1. Let quotient be x / increment.
    auto division_result = x.divided_by(increment_big_int);

    // OPTIMIZATION: If there's no remainder the number is already rounded
    if (division_result.remainder.unsigned_value().is_zero())
        return x;

    bool is_negative;

    // 2. If quotient < 0, then
    if (division_result.quotient.is_negative()) {
        // a. Let isNegative be true.
        is_negative = true;

        // b. Set quotient to -quotient.
        division_result.quotient.negate();
        division_result.remainder.negate();
    }
    // 3. Else,
    else {
        // a. Let isNegative be false.
        is_negative = false;
    }

    // 4. Let unsignedRoundingMode be GetUnsignedRoundingMode(roundingMode, isNegative).
    auto unsigned_rounding_mode = get_unsigned_rounding_mode(rounding_mode, is_negative);

    // 5. Let r1 be the largest integer such that r1 ≤ quotient.
    auto r1 = division_result.quotient;

    // 6. Let r2 be the smallest integer such that r2 > quotient.
    auto r2 = division_result.quotient.plus("1"_bigint);

    // 7. Let rounded be ApplyUnsignedRoundingMode(quotient, r1, r2, unsignedRoundingMode).
    auto rounded = apply_unsigned_rounding_mode(division_result, r1, r2, unsigned_rounding_mode, increment_big_int);

    // 8. If isNegative is true, set rounded to -rounded.
    if (is_negative)
        rounded.negate();

    // 9. Return rounded × increment.
    return rounded.multiplied_by(increment_big_int);
}

// 13.26 RoundNumberToIncrementAsIfPositive ( x, increment, roundingMode ), https://tc39.es/proposal-temporal/#sec-temporal-roundnumbertoincrementasifpositive
Crypto::SignedBigInteger round_number_to_increment_as_if_positive(Crypto::SignedBigInteger const& x, u64 increment, StringView rounding_mode)
{
    VERIFY(rounding_mode == "ceil"sv || rounding_mode == "floor"sv || rounding_mode == "trunc"sv || rounding_mode == "halfExpand"sv);

    // OPTIMIZATION: If the increment is 1 the number is always rounded
    if (increment == 1)
        return x;

    auto increment_big_int = Crypto::UnsignedBigInteger::create_from(increment);

    // 1. Let quotient be x / increment.
    auto division_result = x.divided_by(increment_big_int);

    // OPTIMIZATION: If there's no remainder the number is already rounded
    if (division_result.remainder.unsigned_value().is_zero())
        return x;

    // 2. Let unsignedRoundingMode be GetUnsignedRoundingMode(roundingMode, false).
    auto unsigned_rounding_mode = get_unsigned_rounding_mode(rounding_mode, false);

    // 3. Let r1 be the largest integer such that r1 ≤ quotient.
    auto r1 = division_result.quotient;

    // 4. Let r2 be the smallest integer such that r2 > quotient.
    auto r2 = division_result.quotient.plus("1"_bigint);

    // 5. Let rounded be ApplyUnsignedRoundingMode(quotient, r1, r2, unsignedRoundingMode).
    auto rounded = apply_unsigned_rounding_mode(division_result, r1, r2, unsigned_rounding_mode, increment_big_int);

    // 6. Return rounded × increment.
    return rounded.multiplied_by(increment_big_int);
}

// 13.28 ParseISODateTime ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parseisodatetime
ThrowCompletionOr<ISODateTime> parse_iso_date_time(GlobalObject& global_object, ParseResult const& parse_result)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be empty.
    // 2. For each nonterminal goal of « TemporalDateTimeString, TemporalInstantString, TemporalMonthDayString, TemporalTimeString, TemporalYearMonthString, TemporalZonedDateTimeString », do
    //    a. If parseResult is not a Parse Node, set parseResult to ParseText(StringToCodePoints(isoString), goal).
    // 3. Assert: parseResult is a Parse Node.
    // NOTE: All of this is done by receiving an already parsed ISO string (ParseResult).

    // 4. Let each of year, month, day, fSeconds, and calendar be the source text matched by the respective DateYear, DateMonth, DateDay, TimeFraction, and CalendarName Parse Node contained within parseResult, or an empty sequence of code points if not present.
    auto year = parse_result.date_year;
    auto month = parse_result.date_month;
    auto day = parse_result.date_day;
    auto f_seconds = parse_result.time_fraction;
    auto calendar = parse_result.calendar_name;

    // 5. Let hour be the source text matched by the TimeHour, TimeHourNotValidMonth, TimeHourNotThirtyOneDayMonth, or TimeHourTwoOnly Parse Node contained within parseResult, or an empty sequence of code points if none of those are present.
    auto hour = parse_result.time_hour;
    if (!hour.has_value())
        hour = parse_result.time_hour_not_valid_month;
    if (!hour.has_value())
        hour = parse_result.time_hour_not_thirty_one_day_month;
    if (!hour.has_value())
        hour = parse_result.time_hour_two_only;

    // 6. Let minute be the source text matched by the TimeMinute, TimeMinuteNotValidDay, TimeMinuteThirtyOnly, or TimeMinuteThirtyOneOnly Parse Node contained within parseResult, or an empty sequence of code points if none of those are present.
    auto minute = parse_result.time_minute;
    if (!minute.has_value())
        minute = parse_result.time_minute_not_valid_day;
    if (!minute.has_value())
        minute = parse_result.time_minute_thirty_only;
    if (!minute.has_value())
        minute = parse_result.time_minute_thirty_one_only;

    // 7. Let second be the source text matched by the TimeSecond or TimeSecondNotValidMonth Parse Node contained within parseResult, or an empty sequence of code points if neither of those are present.
    auto second = parse_result.time_second;
    if (!second.has_value())
        second = parse_result.time_second_not_valid_month;

    // 8. If the first code point of year is U+2212 (MINUS SIGN), replace the first code point with U+002D (HYPHEN-MINUS).
    Optional<String> normalized_year;
    if (year.has_value()) {
        normalized_year = year->starts_with("\xE2\x88\x92"sv)
            ? String::formatted("-{}", year->substring_view(3))
            : String { *year };
    }

    // 9. Let yearMV be ! ToIntegerOrInfinity(CodePointsToString(year)).
    auto year_mv = *normalized_year.value_or("0"sv).to_int<i32>();

    // 10. If month is empty, then
    //    a. Let monthMV be 1.
    // 11. Else,
    //    a. Let monthMV be ! ToIntegerOrInfinity(CodePointsToString(month)).
    auto month_mv = *month.value_or("1"sv).to_uint<u8>();

    // 12. If day is empty, then
    //    a. Let dayMV be 1.
    // 13. Else,
    //    a. Let dayMV be ! ToIntegerOrInfinity(CodePointsToString(day)).
    auto day_mv = *day.value_or("1"sv).to_uint<u8>();

    // 14. Let hourMV be ! ToIntegerOrInfinity(CodePointsToString(hour)).
    auto hour_mv = *hour.value_or("0"sv).to_uint<u8>();

    // 15. Let minuteMV be ! ToIntegerOrInfinity(CodePointsToString(minute)).
    auto minute_mv = *minute.value_or("0"sv).to_uint<u8>();

    // 16. Let secondMV be ! ToIntegerOrInfinity(CodePointsToString(second)).
    auto second_mv = *second.value_or("0"sv).to_uint<u8>();

    // 17. If secondMV is 60, then
    if (second_mv == 60) {
        // a. Set secondMV to 59.
        second_mv = 59;
    }

    u16 millisecond_mv;
    u16 microsecond_mv;
    u16 nanosecond_mv;

    // 18. If fSeconds is not empty, then
    if (f_seconds.has_value()) {
        // a. Let fSecondsDigits be the substring of CodePointsToString(fSeconds) from 1.
        auto f_seconds_digits = f_seconds->substring_view(1);

        // b. Let fSecondsDigitsExtended be the string-concatenation of fSecondsDigits and "000000000".
        auto f_seconds_digits_extended = String::formatted("{}000000000", f_seconds_digits);

        // c. Let millisecond be the substring of fSecondsDigitsExtended from 0 to 3.
        auto millisecond = f_seconds_digits_extended.substring(0, 3);

        // d. Let microsecond be the substring of fSecondsDigitsExtended from 3 to 6.
        auto microsecond = f_seconds_digits_extended.substring(3, 3);

        // e. Let nanosecond be the substring of fSecondsDigitsExtended from 6 to 9.
        auto nanosecond = f_seconds_digits_extended.substring(6, 3);

        // f. Let millisecondMV be ! ToIntegerOrInfinity(millisecond).
        millisecond_mv = *millisecond.to_uint<u16>();

        // g. Let microsecondMV be ! ToIntegerOrInfinity(microsecond).
        microsecond_mv = *microsecond.to_uint<u16>();

        // h. Let nanosecondMV be ! ToIntegerOrInfinity(nanosecond).
        nanosecond_mv = *nanosecond.to_uint<u16>();
    }
    // 19. Else,
    else {
        // a. Let millisecondMV be 0.
        millisecond_mv = 0;

        // b. Let microsecondMV be 0.
        microsecond_mv = 0;

        // c. Let nanosecondMV be 0.
        nanosecond_mv = 0;
    }

    // 20. If IsValidISODate(yearMV, monthMV, dayMV) is false, throw a RangeError exception.
    if (!is_valid_iso_date(year_mv, month_mv, day_mv))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidISODate);

    // 21. If IsValidTime(hourMV, minuteMV, secondMV, millisecondMV, microsecondMV, nanosecondMV) is false, throw a RangeError exception.
    if (!is_valid_time(hour_mv, minute_mv, second_mv, millisecond_mv, microsecond_mv, nanosecond_mv))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTime);

    Optional<String> calendar_val;

    // 22. If calendar is empty, then
    if (!calendar.has_value()) {
        // a. Let calendarVal be undefined.
        calendar_val = {};
    }
    // 23. Else,
    else {
        // a. Let calendarVal be CodePointsToString(calendar).
        // NOTE: This turns the StringView into a String.
        calendar_val = *calendar;
    }

    // 24. Return the Record { [[Year]]: yearMV, [[Month]]: monthMV, [[Day]]: dayMV, [[Hour]]: hourMV, [[Minute]]: minuteMV, [[Second]]: secondMV, [[Millisecond]]: millisecondMV, [[Microsecond]]: microsecondMV, [[Nanosecond]]: nanosecondMV, [[Calendar]]: calendarVal, }.
    return ISODateTime { .year = year_mv, .month = month_mv, .day = day_mv, .hour = hour_mv, .minute = minute_mv, .second = second_mv, .millisecond = millisecond_mv, .microsecond = microsecond_mv, .nanosecond = nanosecond_mv, .calendar = move(calendar_val) };
}

// 13.29 ParseTemporalInstantString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalinstantstring
ThrowCompletionOr<TemporalInstant> parse_temporal_instant_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. If ParseText(StringToCodePoints(isoString), TemporalInstantString) is a List of errors, throw a RangeError exception.
    auto parse_result = parse_iso8601(Production::TemporalInstantString, iso_string);
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidInstantString, iso_string);

    // 2. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 3. Let timeZoneResult be ? ParseTemporalTimeZoneString(isoString).
    auto time_zone_result = TRY(parse_temporal_time_zone_string(global_object, iso_string));

    // 4. Let offsetString be timeZoneResult.[[OffsetString]].
    auto offset_string = time_zone_result.offset_string;

    // 5. If timeZoneResult.[[Z]] is true, then
    if (time_zone_result.z) {
        // a. Set offsetString to "+00:00".
        offset_string = "+00:00"sv;
    }

    // 6. Assert: offsetString is not undefined.
    VERIFY(offset_string.has_value());

    // 7. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[TimeZoneOffsetString]]: offsetString }.
    return TemporalInstant { .year = result.year, .month = result.month, .day = result.day, .hour = result.hour, .minute = result.minute, .second = result.second, .millisecond = result.millisecond, .microsecond = result.microsecond, .nanosecond = result.nanosecond, .time_zone_offset = move(offset_string) };
}

// 13.30 ParseTemporalZonedDateTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalzoneddatetimestring
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_zoned_date_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. If ParseText(StringToCodePoints(isoString), TemporalZonedDateTimeString) is a List of errors, throw a RangeError exception.
    auto parse_result = parse_iso8601(Production::TemporalZonedDateTimeString, iso_string);
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidZonedDateTimeString, iso_string);

    // 2. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 3. Let timeZoneResult be ? ParseTemporalTimeZoneString(isoString).
    auto time_zone_result = TRY(parse_temporal_time_zone_string(global_object, iso_string));

    // 4. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]], [[TimeZoneZ]]: timeZoneResult.[[Z]], [[TimeZoneOffsetString]]: timeZoneResult.[[OffsetString]], [[TimeZoneName]]: timeZoneResult.[[Name]] }.
    // NOTE: This returns the two structs together instead of separated to avoid a copy in ToTemporalZonedDateTime, as the spec tries to put the result of InterpretTemporalDateTimeFields and ParseTemporalZonedDateTimeString into the same `result` variable.
    // InterpretTemporalDateTimeFields returns an ISODateTime, so the moved in `result` here is subsequently moved into ParseTemporalZonedDateTimeString's `result` variable.
    return TemporalZonedDateTime { .date_time = move(result), .time_zone = move(time_zone_result) };
}

// 13.31 ParseTemporalCalendarString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalcalendarstring
ThrowCompletionOr<String> parse_temporal_calendar_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalCalendarString).
    auto parse_result = parse_iso8601(Production::TemporalCalendarString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarString, iso_string);

    // 3. Let id be the source text matched by the CalendarName Parse Node contained within parseResult, or an empty sequence of code points if not present.
    auto id = parse_result->calendar_name;

    // 4. If id is empty, then
    if (!id.has_value()) {
        // a. Return "iso8601".
        return "iso8601"sv;
    }

    // 5. Return CodePointsToString(id).
    return id.value();
}

// 13.32 ParseTemporalDateString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatestring
ThrowCompletionOr<TemporalDate> parse_temporal_date_string(GlobalObject& global_object, String const& iso_string)
{
    // 1. Let parts be ? ParseTemporalDateTimeString(isoString).
    auto parts = TRY(parse_temporal_date_time_string(global_object, iso_string));

    // 2. Return the Record { [[Year]]: parts.[[Year]], [[Month]]: parts.[[Month]], [[Day]]: parts.[[Day]], [[Calendar]]: parts.[[Calendar]] }.
    return TemporalDate { .year = parts.year, .month = parts.month, .day = parts.day, .calendar = move(parts.calendar) };
}

// 13.33 ParseTemporalDateTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldatetimestring
ThrowCompletionOr<ISODateTime> parse_temporal_date_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalDateTimeString).
    auto parse_result = parse_iso8601(Production::TemporalDateTimeString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateTimeString, iso_string);

    // 3. If parseResult contains a UTCDesignator Parse Node, throw a RangeError exception.
    if (parse_result->utc_designator.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateTimeStringUTCDesignator, iso_string);

    // 4. Return ? ParseISODateTime(isoString).
    return parse_iso_date_time(global_object, *parse_result);
}

// 13.34 ParseTemporalDurationString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaldurationstring
ThrowCompletionOr<DurationRecord> parse_temporal_duration_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let duration be ParseText(StringToCodePoints(isoString), TemporalDurationString).
    auto parse_result = parse_iso8601(Production::TemporalDurationString, iso_string);

    // 2. If duration is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationString, iso_string);

    // 3. Let each of sign, years, months, weeks, days, hours, fHours, minutes, fMinutes, seconds, and fSeconds be the source text matched by the respective Sign, DurationYears, DurationMonths, DurationWeeks, DurationDays, DurationWholeHours, DurationHoursFraction, DurationWholeMinutes, DurationMinutesFraction, DurationWholeSeconds, and DurationSecondsFraction Parse Node contained within duration, or an empty sequence of code points if not present.
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

    // 4. Let yearsMV be ! ToIntegerOrInfinity(CodePointsToString(years)).
    auto years = strtod(String { years_part.value_or("0"sv) }.characters(), nullptr);

    // 5. Let monthsMV be ! ToIntegerOrInfinity(CodePointsToString(months)).
    auto months = strtod(String { months_part.value_or("0"sv) }.characters(), nullptr);

    // 6. Let weeksMV be ! ToIntegerOrInfinity(CodePointsToString(weeks)).
    auto weeks = strtod(String { weeks_part.value_or("0"sv) }.characters(), nullptr);

    // 7. Let daysMV be ! ToIntegerOrInfinity(CodePointsToString(days)).
    auto days = strtod(String { days_part.value_or("0"sv) }.characters(), nullptr);

    // 8. Let hoursMV be ! ToIntegerOrInfinity(CodePointsToString(hours)).
    auto hours = strtod(String { hours_part.value_or("0"sv) }.characters(), nullptr);

    double minutes;

    // 9. If fHours is not empty, then
    if (f_hours_part.has_value()) {
        // a. If any of minutes, fMinutes, seconds, fSeconds is not empty, throw a RangeError exception.
        if (minutes_part.has_value() || f_minutes_part.has_value() || seconds_part.has_value() || f_seconds_part.has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationStringFractionNotLast, iso_string, "hours"sv, "minutes or seconds"sv);

        // b. Let fHoursDigits be the substring of CodePointsToString(fHours) from 1.
        auto f_hours_digits = f_hours_part->substring_view(1);

        // c. Let fHoursScale be the length of fHoursDigits.
        auto f_hours_scale = (double)f_hours_digits.length();

        // d. Let minutesMV be ! ToIntegerOrInfinity(fHoursDigits) / 10^fHoursScale × 60.
        minutes = strtod(String { f_hours_digits }.characters(), nullptr) / pow(10, f_hours_scale) * 60;
    }
    // 10. Else,
    else {
        // a. Let minutesMV be ! ToIntegerOrInfinity(CodePointsToString(minutes)).
        minutes = strtod(String { minutes_part.value_or("0"sv) }.characters(), nullptr);
    }

    double seconds;

    // 11. If fMinutes is not empty, then
    if (f_minutes_part.has_value()) {
        // a. If any of seconds, fSeconds is not empty, throw a RangeError exception.
        if (seconds_part.has_value() || f_seconds_part.has_value())
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationStringFractionNotLast, iso_string, "minutes"sv, "seconds"sv);

        // b. Let fMinutesDigits be the substring of CodePointsToString(fMinutes) from 1.
        auto f_minutes_digits = f_minutes_part->substring_view(1);

        // c. Let fMinutesScale be the length of fMinutesDigits.
        auto f_minutes_scale = (double)f_minutes_digits.length();

        // d. Let secondsMV be ! ToIntegerOrInfinity(fMinutesDigits) / 10^fMinutesScale × 60.
        seconds = strtod(String { f_minutes_digits }.characters(), nullptr) / pow(10, f_minutes_scale) * 60;
    }
    // 12. Else if seconds is not empty, then
    else if (seconds_part.has_value()) {
        // a. Let secondsMV be ! ToIntegerOrInfinity(CodePointsToString(seconds)).
        seconds = strtod(String { *seconds_part }.characters(), nullptr);
    }
    // 13. Else,
    else {
        // a. Let secondsMV be remainder(minutesMV, 1) × 60.
        seconds = fmod(minutes, 1) * 60;
    }

    double milliseconds;

    // 14. If fSeconds is not empty, then
    if (f_seconds_part.has_value()) {
        // a. Let fSecondsDigits be the substring of CodePointsToString(fSeconds) from 1.
        auto f_seconds_digits = f_seconds_part->substring_view(1);

        // b. Let fSecondsScale be the length of fSecondsDigits.
        auto f_seconds_scale = (double)f_seconds_digits.length();

        // c. Let millisecondsMV be ! ToIntegerOrInfinity(fSecondsDigits) / 10^fSecondsScale × 1000.
        milliseconds = strtod(String { f_seconds_digits }.characters(), nullptr) / pow(10, f_seconds_scale) * 1000;
    }
    // 15. Else,
    else {
        // a. Let millisecondsMV be remainder(secondsMV, 1) × 1000.
        milliseconds = fmod(seconds, 1) * 1000;
    }

    // FIXME: This suffers from floating point (im)precision issues - e.g. "PT0.0000001S" ends up
    //        getting parsed as 99.999999 nanoseconds, which is floor()'d to 99 instead of the
    //        expected 100. Oof. This is the reason all of these are suffixed with "MV" in the spec:
    //        mathematical values are not supposed to have this issue.

    // 16. Let microsecondsMV be remainder(millisecondsMV, 1) × 1000.
    auto microseconds = fmod(milliseconds, 1) * 1000;

    // 17. Let nanosecondsMV be remainder(microsecondsMV, 1) × 1000.
    auto nanoseconds = fmod(microseconds, 1) * 1000;

    i8 factor;

    // 18. If sign contains the code point U+002D (HYPHEN-MINUS) or U+2212 (MINUS SIGN), then
    if (sign_part.has_value() && sign_part->is_one_of("-", "\u2212")) {
        // a. Let factor be -1.
        factor = -1;
    }
    // 19. Else,
    else {
        // a. Let factor be 1.
        factor = 1;
    }

    // 20. Return ? CreateDurationRecord(yearsMV × factor, monthsMV × factor, weeksMV × factor, daysMV × factor, hoursMV × factor, floor(minutesMV) × factor, floor(secondsMV) × factor, floor(millisecondsMV) × factor, floor(microsecondsMV) × factor, floor(nanosecondsMV) × factor).
    return create_duration_record(global_object, years * factor, months * factor, weeks * factor, days * factor, hours * factor, floor(minutes) * factor, floor(seconds) * factor, floor(milliseconds) * factor, floor(microseconds) * factor, floor(nanoseconds) * factor);
}

// 13.35 ParseTemporalMonthDayString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalmonthdaystring
ThrowCompletionOr<TemporalMonthDay> parse_temporal_month_day_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalMonthDayString).
    auto parse_result = parse_iso8601(Production::TemporalMonthDayString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthDayString, iso_string);

    // 3. If parseResult contains a UTCDesignator Parse Node, throw a RangeError exception.
    if (parse_result->utc_designator.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthDayStringUTCDesignator, iso_string);

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Let year be result.[[Year]].
    Optional<i32> year = result.year;

    // 6. If parseResult does not contain a DateYear Parse Node, then
    if (!parse_result->date_year.has_value()) {
        // a. Set year to undefined.
        year = {};
    }

    // 7. Return the Record { [[Year]]: year, [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalMonthDay { .year = year, .month = result.month, .day = result.day, .calendar = move(result.calendar) };
}

// 13.36 ParseTemporalRelativeToString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalrelativetostring
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_relative_to_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. If ParseText(StringToCodePoints(isoString), TemporalDateTimeString) is a List of errors, throw a RangeError exception.
    auto parse_result = parse_iso8601(Production::TemporalDateTimeString, iso_string);
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDateTimeString, iso_string);

    // 2. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    bool z;
    Optional<String> offset_string;
    Optional<String> time_zone;

    // 3. If ParseText(StringToCodePoints(isoString), TemporalZonedDateTimeString) is a Parse Node, then
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
    // 4. Else,
    else {
        // a. Let z be false.
        z = false;

        // b. Let offsetString be undefined.
        // c. Let timeZone be undefined.
    }

    // 5. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]], [[TimeZoneZ]]: z, [[TimeZoneOffsetString]]: offsetString, [[TimeZoneIANAName]]: timeZone }.
    return TemporalZonedDateTime { .date_time = move(result), .time_zone = { .z = z, .offset_string = move(offset_string), .name = move(time_zone) } };
}

// 13.37 ParseTemporalTimeString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimestring
ThrowCompletionOr<TemporalTime> parse_temporal_time_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalTimeString).
    auto parse_result = parse_iso8601(Production::TemporalTimeString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeString, iso_string);

    // 3. If parseResult contains a UTCDesignator Parse Node, throw a RangeError exception.
    if (parse_result->utc_designator.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeStringUTCDesignator, iso_string);

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Return the Record { [[Hour]]: result.[[Hour]], [[Minute]]: result.[[Minute]], [[Second]]: result.[[Second]], [[Millisecond]]: result.[[Millisecond]], [[Microsecond]]: result.[[Microsecond]], [[Nanosecond]]: result.[[Nanosecond]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalTime { .hour = result.hour, .minute = result.minute, .second = result.second, .millisecond = result.millisecond, .microsecond = result.microsecond, .nanosecond = result.nanosecond, .calendar = move(result.calendar) };
}

// 13.38 ParseTemporalTimeZoneString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporaltimezonestring
ThrowCompletionOr<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalTimeZoneString).
    auto parse_result = parse_iso8601(Production::TemporalTimeZoneString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeZoneString, iso_string);

    // 3. Let each of z, offsetString, and name be the source text matched by the respective UTCDesignator, TimeZoneNumericUTCOffset, and TimeZoneIdentifier Parse Nodes contained within parseResult, or an empty sequence of code points if not present.
    auto z = parse_result->utc_designator;
    auto offset_string = parse_result->time_zone_numeric_utc_offset;
    auto name = parse_result->time_zone_identifier;

    // 4. If name is empty, then
    //     a. Set name to undefined.
    //  5. Else,
    //     a. Set name to CodePointsToString(name).
    //  NOTE: No-op.

    // 6. If z is not empty, then
    if (z.has_value()) {
        // a. Return the Record { [[Z]]: true, [[OffsetString]]: undefined, [[Name]]: name }.
        return TemporalTimeZone { .z = true, .offset_string = {}, .name = Optional<String>(move(name)) };
    }

    // 7. If offsetString is empty, then
    //    a. Set offsetString to undefined.
    // 8. Else,
    //    a. Set offsetString to CodePointsToString(offsetString).
    // NOTE: No-op.

    // 9. Return the Record { [[Z]]: false, [[OffsetString]]: offsetString, [[Name]]: name }.
    return TemporalTimeZone { .z = false, .offset_string = Optional<String>(move(offset_string)), .name = Optional<String>(move(name)) };
}

// 13.39 ParseTemporalYearMonthString ( isoString ), https://tc39.es/proposal-temporal/#sec-temporal-parsetemporalyearmonthstring
ThrowCompletionOr<TemporalYearMonth> parse_temporal_year_month_string(GlobalObject& global_object, String const& iso_string)
{
    auto& vm = global_object.vm();

    // 1. Let parseResult be ParseText(StringToCodePoints(isoString), TemporalYearMonthString).
    auto parse_result = parse_iso8601(Production::TemporalYearMonthString, iso_string);

    // 2. If parseResult is a List of errors, throw a RangeError exception.
    if (!parse_result.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidYearMonthString, iso_string);

    // 3. If parseResult contains a UTCDesignator Parse Node, throw a RangeError exception.
    if (parse_result->utc_designator.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidYearMonthStringUTCDesignator, iso_string);

    // 4. Let result be ? ParseISODateTime(isoString).
    auto result = TRY(parse_iso_date_time(global_object, *parse_result));

    // 5. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[Calendar]]: result.[[Calendar]] }.
    return TemporalYearMonth { .year = result.year, .month = result.month, .day = result.day, .calendar = move(result.calendar) };
}

// 13.40 ToPositiveInteger ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-topositiveinteger
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

// 13.43 PrepareTemporalFields ( fields, fieldNames, requiredFields ), https://tc39.es/proposal-temporal/#sec-temporal-preparetemporalfields
ThrowCompletionOr<Object*> prepare_temporal_fields(GlobalObject& global_object, Object const& fields, Vector<String> const& field_names, Variant<PrepareTemporalFieldsPartial, Vector<StringView>> const& required_fields)
{
    auto& vm = global_object.vm();
    auto& realm = *global_object.associated_realm();

    // 1. Let result be OrdinaryObjectCreate(null).
    auto* result = Object::create(realm, nullptr);
    VERIFY(result);

    // 2. Let any be false.
    auto any = false;

    // 3. For each value property of fieldNames, do
    for (auto& property : field_names) {
        // a. Let value be ? Get(fields, property).
        auto value = TRY(fields.get(property));

        // b. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. If property is in the Property column of Table 15 and there is a Conversion value in the same row, then
            // 1. Let Conversion be the Conversion value of the same row.
            // 2. If Conversion is ToIntegerThrowOnInfinity, then
            if (property.is_one_of("year"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv, "eraYear"sv)) {
                // a. Set value to ? ToIntegerThrowOnInfinity(value).
                // b. Set value to 𝔽(value).
                value = Value(TRY(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite)));
            }
            // 3. Else if Conversion is ToPositiveInteger, then
            else if (property.is_one_of("month"sv, "day"sv)) {
                // a. Set value to ? ToPositiveInteger(value).
                // b. Set value to 𝔽(value).
                value = Value(TRY(to_positive_integer(global_object, value)));
            }
            // 4. Else,
            else if (property.is_one_of("monthCode"sv, "offset"sv, "era"sv)) {
                // a. Assert: Conversion is ToString.
                // b. Set value to ? ToString(value).
                value = TRY(value.to_primitive_string(global_object));
            }

            // iii. Perform ! CreateDataPropertyOrThrow(result, property, value).
            MUST(result->create_data_property_or_throw(property, value));
        }
        // c. Else if requiredFields is a List, then
        else if (required_fields.has<Vector<StringView>>()) {
            // i. If requiredFields contains property, then
            if (required_fields.get<Vector<StringView>>().contains_slow(property)) {
                // 1. Throw a TypeError exception.
                return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, property);
            }
            // ii. If property is in the Property column of Table 13, then
            // NOTE: The other properties in the table are automatically handled as their default value is undefined
            if (property.is_one_of("hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
                // 1. Set value to the corresponding Default value of the same row.
                value = Value(0);
            }

            // iii. Perform ! CreateDataPropertyOrThrow(result, property, value).
            MUST(result->create_data_property_or_throw(property, value));
        }
    }

    // 4. If requiredFields is partial and any is false, then
    if (required_fields.has<PrepareTemporalFieldsPartial>() && !any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalObjectMustHaveOneOf, String::join(", "sv, field_names));
    }

    // 5. Return result.
    return result;
}

// 13.44 GetDifferenceSettings ( operation, options, unitGroup, disallowedUnits, fallbackSmallestUnit, smallestLargestDefaultUnit ), https://tc39.es/proposal-temporal/#sec-temporal-getdifferencesettings
ThrowCompletionOr<DifferenceSettings> get_difference_settings(GlobalObject& global_object, DifferenceOperation operation, Value options_value, UnitGroup unit_group, Vector<StringView> const& disallowed_units, TemporalUnitDefault const& fallback_smallest_unit, StringView smallest_largest_default_unit)
{
    auto& vm = global_object.vm();

    // 1. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, options_value));

    // 2. Let smallestUnit be ? GetTemporalUnit(options, "smallestUnit", unitGroup, fallbackSmallestUnit).
    auto smallest_unit = TRY(get_temporal_unit(global_object, *options, vm.names.smallestUnit, unit_group, fallback_smallest_unit));

    // 3. If disallowedUnits contains smallestUnit, throw a RangeError exception.
    if (disallowed_units.contains_slow(*smallest_unit))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, *smallest_unit, "smallestUnit"sv);

    // 4. Let defaultLargestUnit be ! LargerOfTwoTemporalUnits(smallestLargestDefaultUnit, smallestUnit).
    auto default_largest_unit = larger_of_two_temporal_units(smallest_largest_default_unit, *smallest_unit);

    // 5. Let largestUnit be ? GetTemporalUnit(options, "largestUnit", unitGroup, "auto").
    auto largest_unit = TRY(get_temporal_unit(global_object, *options, vm.names.largestUnit, unit_group, { "auto"sv }));

    // 6. If disallowedUnits contains largestUnit, throw a RangeError exception.
    if (disallowed_units.contains_slow(*largest_unit))
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, *largest_unit, "largestUnit"sv);

    // 7. If largestUnit is "auto", set largestUnit to defaultLargestUnit.
    if (largest_unit == "auto"sv)
        largest_unit = default_largest_unit;

    // 8. If LargerOfTwoTemporalUnits(largestUnit, smallestUnit) is not largestUnit, throw a RangeError exception.
    if (larger_of_two_temporal_units(*largest_unit, *smallest_unit) != largest_unit)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidUnitRange, *smallest_unit, *largest_unit);

    // 9. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 10. If operation is since, then
    if (operation == DifferenceOperation::Since) {
        // a. Set roundingMode to ! NegateTemporalRoundingMode(roundingMode).
        rounding_mode = negate_temporal_rounding_mode(rounding_mode);
    }

    // 11. Let maximum be ! MaximumTemporalDurationRoundingIncrement(smallestUnit).
    auto maximum = maximum_temporal_duration_rounding_increment(*smallest_unit);

    // 12. Let roundingIncrement be ? ToTemporalRoundingIncrement(options, maximum, false).
    auto rounding_increment = TRY(to_temporal_rounding_increment(global_object, *options, Optional<double> { maximum }, false));

    // 13. Return the Record { [[SmallestUnit]]: smallestUnit, [[LargestUnit]]: largestUnit, [[RoundingMode]]: roundingMode, [[RoundingIncrement]]: roundingIncrement, [[Options]]: options }.
    return DifferenceSettings {
        .smallest_unit = smallest_unit.release_value(),
        .largest_unit = largest_unit.release_value(),
        .rounding_mode = move(rounding_mode),
        .rounding_increment = rounding_increment,
        .options = *options,
    };
}

}
