/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormat.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
#include <LibJS/Runtime/Intl/PluralRulesConstructor.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>

namespace JS::Intl {

// 1 DurationFormat Objects, https://tc39.es/proposal-intl-duration-format/#durationformat-objects
DurationFormat::DurationFormat(Object& prototype)
    : Object(prototype)
{
}

DurationFormat::Style DurationFormat::style_from_string(StringView style)
{
    if (style == "long"sv)
        return Style::Long;
    if (style == "short"sv)
        return Style::Short;
    if (style == "narrow"sv)
        return Style::Narrow;
    if (style == "digital"sv)
        return Style::Digital;
    VERIFY_NOT_REACHED();
}

StringView DurationFormat::style_to_string(Style style)
{
    switch (style) {
    case Style::Long:
        return "long"sv;
    case Style::Short:
        return "short"sv;
    case Style::Narrow:
        return "narrow"sv;
    case Style::Digital:
        return "digital"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

DurationFormat::ValueStyle DurationFormat::date_style_from_string(StringView date_style)
{
    if (date_style == "long"sv)
        return ValueStyle::Long;
    if (date_style == "short"sv)
        return ValueStyle::Short;
    if (date_style == "narrow"sv)
        return ValueStyle::Narrow;
    VERIFY_NOT_REACHED();
}

DurationFormat::ValueStyle DurationFormat::time_style_from_string(StringView time_style)
{
    if (time_style == "long"sv)
        return ValueStyle::Long;
    if (time_style == "short"sv)
        return ValueStyle::Short;
    if (time_style == "narrow"sv)
        return ValueStyle::Narrow;
    if (time_style == "numeric"sv)
        return ValueStyle::Numeric;
    if (time_style == "2-digit"sv)
        return ValueStyle::TwoDigit;
    VERIFY_NOT_REACHED();
}

DurationFormat::ValueStyle DurationFormat::sub_second_style_from_string(StringView sub_second_style)
{
    if (sub_second_style == "long"sv)
        return ValueStyle::Long;
    if (sub_second_style == "short"sv)
        return ValueStyle::Short;
    if (sub_second_style == "narrow"sv)
        return ValueStyle::Narrow;
    if (sub_second_style == "numeric"sv)
        return ValueStyle::Numeric;
    VERIFY_NOT_REACHED();
}

DurationFormat::Display DurationFormat::display_from_string(StringView display)
{
    if (display == "auto"sv)
        return Display::Auto;
    if (display == "always"sv)
        return Display::Always;
    VERIFY_NOT_REACHED();
}

StringView DurationFormat::value_style_to_string(ValueStyle value_style)
{
    switch (value_style) {
    case ValueStyle::Long:
        return "long"sv;
    case ValueStyle::Short:
        return "short"sv;
    case ValueStyle::Narrow:
        return "narrow"sv;
    case ValueStyle::Numeric:
        return "numeric"sv;
    case ValueStyle::TwoDigit:
        return "2-digit"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

StringView DurationFormat::display_to_string(Display display)
{
    switch (display) {
    case Display::Auto:
        return "auto"sv;
    case Display::Always:
        return "always"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 1.1.3 ToDurationRecord ( input ), https://tc39.es/proposal-intl-duration-format/#sec-todurationrecord
ThrowCompletionOr<Temporal::DurationRecord> to_duration_record(VM& vm, Value input)
{
    // 1. If Type(input) is not Object, throw a TypeError exception.
    if (!input.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, input);
    auto& input_object = input.as_object();

    // 2. Let result be a new Duration Record with each field set to 0.
    Temporal::DurationRecord result = {};

    // 3. Let any be false.
    auto any = false;

    // 4. For each row in Table 1, except the header row, in table order, do
    for (auto const& duration_instances_component : duration_instances_components) {
        // a. Let valueSlot be the Value Slot value of the current row.
        auto value_slot = duration_instances_component.value_slot;

        // b. Let unit be the Unit value of the current row.
        auto unit = duration_instances_component.unit;

        // c. Let value be ? Get(input, unit).
        auto value = TRY(input_object.get(FlyString(unit)));

        // d. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. Set value to ? ToIntegerWithoutRounding(value).
            auto value_number = TRY(Temporal::to_integer_without_rounding(vm, value, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, unit, value));

            // iii. Set result.[[<valueSlot>]] to value.
            result.*value_slot = value_number;
        }
    }

    // 5. If any is false, throw a TypeError exception.
    if (!any)
        return vm.throw_completion<TypeError>(ErrorType::TemporalInvalidDurationLikeObject);

    // 6. Return result.
    return result;
}

// 1.1.4 DurationRecordSign ( record ), https://tc39.es/proposal-intl-duration-format/#sec-durationrecordsign
i8 duration_record_sign(Temporal::DurationRecord const& record)
{
    // 1. For each row in Table 1, except the header row, in table order, do
    for (auto const& duration_instances_component : duration_instances_components) {
        // a. Let valueSlot be the Value Slot value.
        auto value_slot = duration_instances_component.value_slot;

        // b. Let v be value of the valueSlot slot of record.
        auto value = record.*value_slot;

        // c. If v < 0, return -1.
        if (value < 0)
            return -1;

        // d. If v > 0, return 1.
        if (value > 0)
            return 1;
    }

    // 2. Return 0.
    return 0;
}

// 1.1.5 IsValidDurationRecord ( record ), https://tc39.es/proposal-intl-duration-format/#sec-isvaliddurationrecord
bool is_valid_duration_record(Temporal::DurationRecord const& record)
{
    // 1. Let sign be ! DurationRecordSign(record).
    auto sign = duration_record_sign(record);

    // 2. For each row in Table 1, except the header row, in table order, do
    for (auto const& duration_instances_component : duration_instances_components) {
        // a. Let valueSlot be the Value Slot value.
        auto value_slot = duration_instances_component.value_slot;

        // b. Let v be value of the valueSlot slot of record.
        auto value = record.*value_slot;

        // c. If 𝔽(v) is not finite, return false.
        if (!isfinite(value))
            return false;

        // d. If v < 0 and sign > 0, return false.
        if (value < 0 && sign > 0)
            return false;

        // e. If v > 0 and sign < 0, return false.
        if (value > 0 && sign < 0)
            return false;
    }

    // 3. Return true.
    return true;
}

// 1.1.6 GetDurationUnitOptions ( unit, options, baseStyle, stylesList, digitalBase, prevStyle ), https://tc39.es/proposal-intl-duration-format/#sec-getdurationunitoptions
ThrowCompletionOr<DurationUnitOptions> get_duration_unit_options(VM& vm, String const& unit, Object const& options, StringView base_style, Span<StringView const> styles_list, StringView digital_base, StringView previous_style)
{
    // 1. Let style be ? GetOption(options, unit, "string", stylesList, undefined).
    auto style_value = TRY(get_option(vm, options, unit, OptionType::String, styles_list, Empty {}));

    // 2. Let displayDefault be "always".
    auto display_default = "always"sv;

    String style;

    // 3. If style is undefined, then
    if (style_value.is_undefined()) {
        // a. Set displayDefault to "auto".
        display_default = "auto"sv;

        // b. If baseStyle is "digital", then
        if (base_style == "digital"sv) {
            // i. Set style to digitalBase.
            style = digital_base;
        }
        // c. Else,
        else {
            // i. Set style to baseStyle.
            style = base_style;
        }
    } else {
        style = style_value.as_string().string();
    }

    // 4. Let displayField be the string-concatenation of unit and "Display".
    auto display_field = String::formatted("{}Display", unit);

    // 5. Let display be ? GetOption(options, displayField, "string", « "auto", "always" », displayDefault).
    auto display = TRY(get_option(vm, options, display_field, OptionType::String, { "auto"sv, "always"sv }, display_default));

    // 6. If prevStyle is "numeric" or "2-digit", then
    if (previous_style == "numeric"sv || previous_style == "2-digit"sv) {
        // a. If style is not "numeric" or "2-digit", then
        if (style != "numeric"sv && style != "2-digit"sv) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(ErrorType::IntlNonNumericOr2DigitAfterNumericOr2Digit);
        }
        // b. Else if unit is "minutes" or "seconds", then
        else if (unit == "minutes"sv || unit == "seconds"sv) {
            // i. Set style to "2-digit".
            style = "2-digit"sv;
        }
    }

    // 7. Return the Record { [[Style]]: style, [[Display]]: display }.
    return DurationUnitOptions { .style = move(style), .display = display.as_string().string() };
}

// FIXME: LibUnicode currently only exposes unit patterns converted to an ECMA402 NumberFormat-specific format,
//  since DurationFormat only needs a tiny subset of it, it's much easier to just convert it to the expected format
//  here, but at some point we should split the the NumberFormat exporter to export both formats of the data.
static String convert_number_format_pattern_to_duration_format_template(Unicode::NumberFormat const& number_format)
{
    auto result = number_format.zero_format.replace("{number}"sv, "{0}"sv, ReplaceMode::FirstOnly);

    for (size_t i = 0; i < number_format.identifiers.size(); ++i)
        result = result.replace(String::formatted("{{unitIdentifier:{}}}", i), number_format.identifiers[i], ReplaceMode::FirstOnly);

    return result;
}

// 1.1.7 PartitionDurationFormatPattern ( durationFormat, duration ), https://tc39.es/proposal-intl-duration-format/#sec-partitiondurationformatpattern
Vector<PatternPartition> partition_duration_format_pattern(VM& vm, DurationFormat const& duration_format, Temporal::DurationRecord const& duration)
{
    auto& realm = *vm.current_realm();

    // 1. Let result be a new empty List.
    Vector<PatternPartition> result;

    // 2. Let done be false.
    bool done = false;

    // 3. While done is false, repeat for each row in Table 1 in order, except the header row:
    for (size_t i = 0; !done && i < duration_instances_components.size(); ++i) {
        auto const& duration_instances_component = duration_instances_components[i];

        // a. Let styleSlot be the Style Slot value.
        auto style_slot = duration_instances_component.get_style_slot;

        // b. Let displaySlot be the Display Slot value.
        auto display_slot = duration_instances_component.get_display_slot;

        // c. Let valueSlot be the Value Slot value.
        auto value_slot = duration_instances_component.value_slot;

        // d. Let unit be the Unit value.
        auto unit = duration_instances_component.unit;

        // e. Let style be durationFormat.[[<styleSlot>]].
        auto style = (duration_format.*style_slot)();

        // f. Let display be durationFormat.[[<displaySlot>]].
        auto display = (duration_format.*display_slot)();

        // g. Let value be duration.[[<valueSlot>]].
        auto value = duration.*value_slot;

        // h. Let nfOpts be ! OrdinaryObjectCreate(null).
        auto* number_format_options = Object::create(realm, nullptr);

        // i. If unit is "seconds", "milliseconds", or "microseconds", then
        if (unit.is_one_of("seconds"sv, "milliseconds"sv, "microseconds"sv)) {
            DurationFormat::ValueStyle next_style;

            // i. If unit is "seconds", then
            if (unit == "seconds"sv) {
                // 1. Let nextStyle be durationFormat.[[MillisecondsStyle]].
                next_style = duration_format.milliseconds_style();
            }
            // ii. Else if unit is "milliseconds", then
            else if (unit == "milliseconds"sv) {
                // 1. Let nextStyle be durationFormat.[[MicrosecondsStyle]].
                next_style = duration_format.microseconds_style();
            }
            // iii. Else,
            else {
                // 1. Let nextStyle be durationFormat.[[NanosecondsStyle]].
                next_style = duration_format.nanoseconds_style();
            }

            // iv. If nextStyle is "numeric", then
            if (next_style == DurationFormat::ValueStyle::Numeric) {
                // 1. If unit is "seconds", then
                if (unit == "seconds"sv) {
                    // a. Set value to value + duration.[[Milliseconds]] / 10^3 + duration.[[Microseconds]] / 10^6 + duration.[[Nanoseconds]] / 10^9.
                    value += duration.milliseconds / 1'000.0 + duration.microseconds / 1'000'000.0 + duration.nanoseconds / 1'000'000'000.0;
                }
                // 2. Else if unit is "milliseconds", then
                else if (unit == "milliseconds"sv) {
                    // a. Set value to value + duration.[[Microseconds]] / 10^3 + duration.[[Nanoseconds]] / 10^6.
                    value += duration.microseconds / 1'000.0 + duration.nanoseconds / 1'000'000.0;
                }
                // 3. Else,
                else {
                    // a. Set value to value + duration.[[Nanoseconds]] / 10^3.
                    value += duration.nanoseconds / 1'000.0;
                }

                // 4. Perform ! CreateDataPropertyOrThrow(nfOpts, "maximumFractionDigits", durationFormat.[[FractionalDigits]]).
                MUST(number_format_options->create_data_property_or_throw(vm.names.maximumFractionDigits, duration_format.has_fractional_digits() ? Value(duration_format.fractional_digits()) : js_undefined()));

                // 5. Perform ! CreateDataPropertyOrThrow(nfOpts, "minimumFractionDigits", durationFormat.[[FractionalDigits]]).
                MUST(number_format_options->create_data_property_or_throw(vm.names.minimumFractionDigits, duration_format.has_fractional_digits() ? Value(duration_format.fractional_digits()) : js_undefined()));

                // 6. Set done to true.
                done = true;
            }
        }

        // j. If style is "2-digit", then
        if (style == DurationFormat::ValueStyle::TwoDigit) {
            // i. Perform ! CreateDataPropertyOrThrow(nfOpts, "minimumIntegerDigits", 2𝔽).
            MUST(number_format_options->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(2)));
        }

        // k. If value is not 0 or display is not "auto", then
        if (value != 0.0 || display != DurationFormat::Display::Auto) {
            // i. Let nf be ! Construct(%NumberFormat%, « durationFormat.[[Locale]], nfOpts »).
            auto* number_format = static_cast<NumberFormat*>(MUST(construct(vm, *realm.intrinsics().intl_number_format_constructor(), js_string(vm, duration_format.locale()), number_format_options)));

            // ii. Let dataLocale be durationFormat.[[DataLocale]].
            auto const& data_locale = duration_format.data_locale();

            // iii. Let dataLocaleData be %DurationFormat%.[[LocaleData]].[[<dataLocale>]].

            // iv. If style is "2-digit" or "numeric", then
            if (style == DurationFormat::ValueStyle::TwoDigit || style == DurationFormat::ValueStyle::Numeric) {
                // 1. Let num be ! FormatNumeric(nf, 𝔽(value)).
                auto number = format_numeric(vm, *number_format, MathematicalValue(value));

                // 2. Append the new Record { [[Type]]: unit, [[Value]]: num} to the end of result.
                result.append({ unit, number });

                // 3. If unit is "hours" or "minutes", then
                if (unit.is_one_of("hours"sv, "minutes"sv)) {
                    double next_value = 0.0;
                    DurationFormat::Display next_display;

                    // a. If unit is "hours", then
                    if (unit == "hours"sv) {
                        // i. Let nextValue be duration.[[Minutes]].
                        next_value = duration.minutes;

                        // ii. Let nextDisplay be durationFormat.[[MinutesDisplay]].
                        next_display = duration_format.minutes_display();
                    }
                    // b. Else,
                    else {
                        // i. Let nextValue be duration.[[Seconds]].
                        next_value = duration.seconds;

                        // ii. Let nextDisplay be durationFormat.[[SecondsDisplay]].
                        next_display = duration_format.seconds_display();

                        // iii. If durationFormat.[[MillisecondsStyle]] is "numeric", then
                        if (duration_format.milliseconds_style() == DurationFormat::ValueStyle::Numeric) {
                            // i. Set nextValue to nextValue + duration.[[Milliseconds]] / 10^3 + duration.[[Microseconds]] / 10^6 + duration.[[Nanoseconds]] / 10^9.
                            next_value += duration.milliseconds / 1'000.0 + duration.microseconds / 1'000'000.0 + duration.nanoseconds / 1'000'000'000.0;
                        }
                    }

                    // c. If nextValue is not 0 or nextDisplay is not "auto", then
                    if (next_value != 0.0 || next_display != DurationFormat::Display::Auto) {
                        // i. Let separator be dataLocaleData.[[formats]].[[digital]].[[separator]].
                        auto separator = Unicode::get_number_system_symbol(data_locale, duration_format.numbering_system(), Unicode::NumericSymbol::TimeSeparator).value_or(":"sv);

                        // ii. Append the new Record { [[Type]]: "literal", [[Value]]: separator} to the end of result.
                        result.append({ "literal"sv, separator });
                    }
                }
            }
            // v. Else,
            else {
                // 1. Let num be ! PartitionNumberPattern(nf, 𝔽(value)).
                auto number = partition_number_pattern(vm, *number_format, MathematicalValue(value));

                // 2. Let pr be ! Construct(%PluralRules%, « durationFormat.[[Locale]] »).
                auto* plural_rules = static_cast<PluralRules*>(MUST(construct(vm, *realm.intrinsics().intl_plural_rules_constructor(), js_string(vm, duration_format.locale()))));

                // 3. Let prv be ! ResolvePlural(pr, 𝔽(value)).
                auto plurality = resolve_plural(*plural_rules, Value(value));

                auto formats = Unicode::get_unit_formats(data_locale, duration_instances_component.unit_singular, static_cast<Unicode::Style>(style));
                auto pattern = formats.find_if([&](auto& p) { return p.plurality == plurality; });
                if (pattern == formats.end())
                    continue;

                // 4. Let template be dataLocaleData.[[formats]].[[<style>]].[[<unit>]].[[<prv>]].
                auto template_ = convert_number_format_pattern_to_duration_format_template(*pattern);

                // 5. Let parts be ! MakePartsList(template, unit, num).
                auto parts = make_parts_list(template_, unit, move(number));

                // 6. Let concat be an empty String.
                StringBuilder concat;

                // 7. For each Record { [[Type]], [[Value]], [[Unit]] } part in parts, do
                for (auto const& part : parts) {
                    // a. Set concat to the string-concatenation of concat and part.[[Value]].
                    concat.append(part.value);
                }

                // 8. Append the new Record { [[Type]]: unit, [[Value]]: concat } to the end of result.
                result.append({ unit, concat.build() });
            }
        }
    }

    // 4. Let lf be ! Construct(%ListFormat%, « durationFormat.[[Locale]] »).
    auto* list_format = static_cast<ListFormat*>(MUST(construct(vm, *realm.intrinsics().intl_list_format_constructor(), js_string(vm, duration_format.locale()))));

    // FIXME: CreatePartsFromList expects a list of strings and creates a list of Pattern Partition records, but we already created a list of Pattern Partition records
    //  so we try to hack something together from it that looks mostly right
    Vector<String> string_result;
    bool merge = false;
    for (size_t i = 0; i < result.size(); ++i) {
        auto const& part = result[i];
        if (part.type == "literal") {
            string_result.last() = String::formatted("{}{}", string_result.last(), part.value);
            merge = true;
            continue;
        }
        if (merge) {
            string_result.last() = String::formatted("{}{}", string_result.last(), part.value);
            merge = false;
            continue;
        }
        string_result.append(part.value);
    }

    // 5. Set result to ! CreatePartsFromList(lf, result).
    auto final_result = create_parts_from_list(*list_format, string_result);

    // 6. Return result.
    return final_result;
}

}
