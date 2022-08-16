/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>

namespace JS::Intl {

// 17 RelativeTimeFormat Objects, https://tc39.es/ecma402/#relativetimeformat-objects
RelativeTimeFormat::RelativeTimeFormat(Object& prototype)
    : Object(prototype)
{
}

void RelativeTimeFormat::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_number_format)
        visitor.visit(m_number_format);
    if (m_plural_rules)
        visitor.visit(m_plural_rules);
}

void RelativeTimeFormat::set_numeric(StringView numeric)
{
    if (numeric == "always"sv) {
        m_numeric = Numeric::Always;
    } else if (numeric == "auto"sv) {
        m_numeric = Numeric::Auto;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView RelativeTimeFormat::numeric_string() const
{
    switch (m_numeric) {
    case Numeric::Always:
        return "always"sv;
    case Numeric::Auto:
        return "auto"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 17.5.1 SingularRelativeTimeUnit ( unit ), https://tc39.es/ecma402/#sec-singularrelativetimeunit
ThrowCompletionOr<Unicode::TimeUnit> singular_relative_time_unit(GlobalObject& global_object, StringView unit)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(unit) is String.

    // 2. If unit is "seconds", return "second".
    if (unit == "seconds"sv)
        return Unicode::TimeUnit::Second;
    // 3. If unit is "minutes", return "minute".
    if (unit == "minutes"sv)
        return Unicode::TimeUnit::Minute;
    // 4. If unit is "hours", return "hour".
    if (unit == "hours"sv)
        return Unicode::TimeUnit::Hour;
    // 5. If unit is "days", return "day".
    if (unit == "days"sv)
        return Unicode::TimeUnit::Day;
    // 6. If unit is "weeks", return "week".
    if (unit == "weeks"sv)
        return Unicode::TimeUnit::Week;
    // 7. If unit is "months", return "month".
    if (unit == "months"sv)
        return Unicode::TimeUnit::Month;
    // 8. If unit is "quarters", return "quarter".
    if (unit == "quarters"sv)
        return Unicode::TimeUnit::Quarter;
    // 9. If unit is "years", return "year".
    if (unit == "years"sv)
        return Unicode::TimeUnit::Year;

    // 10. If unit is not one of "second", "minute", "hour", "day", "week", "month", "quarter", or "year", throw a RangeError exception.
    // 11. Return unit.
    if (auto time_unit = Unicode::time_unit_from_string(unit); time_unit.has_value())
        return *time_unit;
    return vm.throw_completion<RangeError>(ErrorType::IntlInvalidUnit, unit);
}

// 17.5.2 PartitionRelativeTimePattern ( relativeTimeFormat, value, unit ), https://tc39.es/ecma402/#sec-PartitionRelativeTimePattern
ThrowCompletionOr<Vector<PatternPartitionWithUnit>> partition_relative_time_pattern(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit)
{
    auto& vm = global_object.vm();

    // 1. Assert: relativeTimeFormat has an [[InitializedRelativeTimeFormat]] internal slot.
    // 2. Assert: Type(value) is Number.
    // 3. Assert: Type(unit) is String.

    // 4. If value is NaN, +∞𝔽, or -∞𝔽, throw a RangeError exception.
    if (!Value(value).is_finite_number())
        return vm.throw_completion<RangeError>(ErrorType::IntlNumberIsNaNOrInfinity);

    // 5. Let unit be ? SingularRelativeTimeUnit(unit).
    auto time_unit = TRY(singular_relative_time_unit(global_object, unit));

    // 6. Let localeData be %RelativeTimeFormat%.[[LocaleData]].
    // 7. Let dataLocale be relativeTimeFormat.[[DataLocale]].
    auto const& data_locale = relative_time_format.data_locale();

    // 8. Let fields be localeData.[[<dataLocale>]].

    // 9. Let style be relativeTimeFormat.[[Style]].
    auto style = relative_time_format.style();

    // NOTE: The next steps form a "key" based on combining various formatting options into a string,
    //       then filtering the large set of locale data down to the pattern we are looking for. Instead,
    //       LibUnicode expects the individual options as enumeration values, and returns the couple of
    //       patterns that match those options.
    auto find_patterns_for_tense_or_number = [&](StringView tense_or_number) {
        // 10. If style is equal to "short", then
        //     a. Let entry be the string-concatenation of unit and "-short".
        // 11. Else if style is equal to "narrow", then
        //     a. Let entry be the string-concatenation of unit and "-narrow".
        // 12. Else,
        //     a. Let entry be unit.
        auto patterns = Unicode::get_relative_time_format_patterns(data_locale, time_unit, tense_or_number, style);

        // 13. If fields doesn't have a field [[<entry>]], then
        if (patterns.is_empty()) {
            // a. Let entry be unit.
            // NOTE: In the CLDR, the lack of "short" or "narrow" in the key implies "long".
            patterns = Unicode::get_relative_time_format_patterns(data_locale, time_unit, tense_or_number, Unicode::Style::Long);
        }

        // 14. Let patterns be fields.[[<entry>]].
        return patterns;
    };

    // 15. Let numeric be relativeTimeFormat.[[Numeric]].
    // 16. If numeric is equal to "auto", then
    if (relative_time_format.numeric() == RelativeTimeFormat::Numeric::Auto) {
        // a. Let valueString be ToString(value).
        auto value_string = MUST(Value(value).to_string(global_object));

        // b. If patterns has a field [[<valueString>]], then
        if (auto patterns = find_patterns_for_tense_or_number(value_string); !patterns.is_empty()) {
            VERIFY(patterns.size() == 1);

            // i. Let result be patterns.[[<valueString>]].
            auto result = patterns[0].pattern.to_string();

            // ii. Return a List containing the Record { [[Type]]: "literal", [[Value]]: result }.
            return Vector<PatternPartitionWithUnit> { { "literal"sv, move(result) } };
        }
    }

    // 17. If value is -0𝔽 or if value is less than 0, then
    StringView tense;
    if (Value(value).is_negative_zero() || (value < 0)) {
        // a. Let tl be "past".
        tense = "past"sv;

        // FIXME: The spec does not say to do this, but nothing makes sense after this with a negative value.
        value = fabs(value);
    }
    // 18. Else,
    else {
        // a. Let tl be "future".
        tense = "future"sv;
    }

    // 19. Let po be patterns.[[<tl>]].
    auto patterns = find_patterns_for_tense_or_number(tense);

    // 20. Let fv be ! PartitionNumberPattern(relativeTimeFormat.[[NumberFormat]], value).
    auto value_partitions = partition_number_pattern(global_object, relative_time_format.number_format(), Value(value));

    // 21. Let pr be ! ResolvePlural(relativeTimeFormat.[[PluralRules]], value).
    auto plurality = resolve_plural(relative_time_format.plural_rules(), Value(value));

    // 22. Let pattern be po.[[<pr>]].
    auto pattern = patterns.find_if([&](auto& p) { return p.plurality == plurality; });
    if (pattern == patterns.end())
        return Vector<PatternPartitionWithUnit> {};

    // 23. Return ! MakePartsList(pattern, unit, fv).
    return make_parts_list(pattern->pattern, Unicode::time_unit_to_string(time_unit), move(value_partitions));
}

// 17.5.3 MakePartsList ( pattern, unit, parts ), https://tc39.es/ecma402/#sec-makepartslist
Vector<PatternPartitionWithUnit> make_parts_list(StringView pattern, StringView unit, Vector<PatternPartition> parts)
{
    // 1. Let patternParts be PartitionPattern(pattern).
    auto pattern_parts = partition_pattern(pattern);

    // 2. Let result be a new empty List.
    Vector<PatternPartitionWithUnit> result;

    // 3. For each Record { [[Type]], [[Value]] } patternPart in patternParts, do
    for (auto& pattern_part : pattern_parts) {
        // a. If patternPart.[[Type]] is "literal", then
        if (pattern_part.type == "literal"sv) {
            // i. Append Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]], [[Unit]]: empty } to result.
            result.empend("literal"sv, move(pattern_part.value));
        }
        // b. Else,
        else {
            // i. Assert: patternPart.[[Type]] is "0".
            VERIFY(pattern_part.type == "0"sv);

            // ii. For each Record { [[Type]], [[Value]] } part in parts, do
            for (auto& part : parts) {
                // 1. Append Record { [[Type]]: part.[[Type]], [[Value]]: part.[[Value]], [[Unit]]: unit } to result.
                result.empend(part.type, move(part.value), unit);
            }
        }
    }

    // 4. Return result.
    return result;
}

// 17.5.4 FormatRelativeTime ( relativeTimeFormat, value, unit ), https://tc39.es/ecma402/#sec-FormatRelativeTime
ThrowCompletionOr<String> format_relative_time(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit)
{
    // 1. Let parts be ? PartitionRelativeTimePattern(relativeTimeFormat, value, unit).
    auto parts = TRY(partition_relative_time_pattern(global_object, relative_time_format, value, unit));

    // 2. Let result be an empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]], [[Unit]] } part in parts, do
    for (auto& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(move(part.value));
    }

    // 4. Return result.
    return result.build();
}

// 17.5.5 FormatRelativeTimeToParts ( relativeTimeFormat, value, unit ), https://tc39.es/ecma402/#sec-FormatRelativeTimeToParts
ThrowCompletionOr<Array*> format_relative_time_to_parts(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit)
{
    auto& vm = global_object.vm();
    auto& realm = *global_object.associated_realm();

    // 1. Let parts be ? PartitionRelativeTimePattern(relativeTimeFormat, value, unit).
    auto parts = TRY(partition_relative_time_pattern(global_object, relative_time_format, value, unit));

    // 2. Let result be ! ArrayCreate(0).
    auto* result = MUST(Array::create(realm, 0));

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]], [[Unit]] } part in parts, do
    for (auto& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%Object.prototype%).
        auto* object = Object::create(realm, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, move(part.value))));

        // d. If part.[[Unit]] is not empty, then
        if (!part.unit.is_empty()) {
            // i. Perform ! CreateDataPropertyOrThrow(O, "unit", part.[[Unit]]).
            MUST(object->create_data_property_or_throw(vm.names.unit, js_string(vm, part.unit)));
        }

        // e. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), O).
        MUST(result->create_data_property_or_throw(n, object));

        // f. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

}
