/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
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

    // 2. Let result be a new Record.
    Temporal::DurationRecord result;

    // 3. Let any be false.
    auto any = false;

    // 4. For each row in Table 1, except the header row, in table order, do
    for (auto const& duration_instances_component : duration_instances_components) {
        // a. Let valueSlot be the Value Slot value.
        auto value_slot = duration_instances_component.value_slot;

        // b. Let unit be the Unit value.
        auto unit = duration_instances_component.unit;

        // c. Let value be ? Get(input, unit).
        auto value = TRY(input_object.get(FlyString(unit)));

        double value_number;
        // d. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;
            // ii. Set value to ? ToIntegerWithoutRounding(value).
            value_number = TRY(Temporal::to_integer_without_rounding(vm, value, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, unit, value));
        }
        // e. Else,
        else {
            // i. Set value to 0.
            value_number = 0;
        }

        // f. Set the field of result whose name is valueSlot to value.
        result.*value_slot = value_number;
    }

    // 5. If any is false, throw a TypeError exception.
    if (!any)
        return vm.throw_completion<TypeError>(ErrorType::TemporalInvalidDurationLikeObject);

    // 6. Return result.
    return result;
}

// 1.1.4 DurationSign ( record ), https://tc39.es/proposal-intl-duration-format/#sec-durationsign
i8 duration_sign(Temporal::DurationRecord const& record)
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
    // 1. Let sign be ! DurationSign(record).
    auto sign = duration_sign(record);

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
ThrowCompletionOr<DurationUnitOptions> get_duration_unit_options(VM& vm, String const& unit, Object const& options, StringView base_style, Span<StringView const> styles_list, StringView digital_base, Optional<String> const& previous_style)
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
ThrowCompletionOr<Vector<PatternPartition>> partition_duration_format_pattern(VM& vm, DurationFormat const& duration_format, Temporal::DurationRecord const& duration)
{
    auto& realm = *vm.current_realm();

    // 1. Let result be a new empty List.
    Vector<PatternPartition> result;

    // 2. For each row in Table 1, except the header row, in table order, do
    for (size_t i = 0; i < duration_instances_components.size(); ++i) {
        auto const& duration_instances_component = duration_instances_components[i];

        // a. Let styleSlot be the Style Slot value.
        auto style_slot = duration_instances_component.get_style_slot;

        decltype(DurationInstanceComponent::get_style_slot) next_style_slot = nullptr;
        // FIXME: Missing spec step - If this is not the last row
        if (i < duration_instances_components.size() - 1) {
            // b. Let nextStyleSlot be the Style Slot value of the next row.
            next_style_slot = duration_instances_components[i + 1].get_style_slot;
        }

        // c. Let displaySlot be the Display Slot value.
        auto display_slot = duration_instances_component.get_display_slot;

        // d. Let valueSlot be the Value Slot value.
        auto value_slot = duration_instances_component.value_slot;

        // e. Let unit be the Unit value.
        auto unit = duration_instances_component.unit;

        // f. Let style be the current value of the styleSlot slot of durationFormat.
        auto style = (duration_format.*style_slot)();

        DurationFormat::ValueStyle next_style = DurationFormat::ValueStyle::Long;
        // FIXME: Missing spec step - If this is not the last row
        if (next_style_slot) {
            // g. Let nextStyle be the current value of the nextStyleSlot slot of durationFormat.
            next_style = (duration_format.*next_style_slot)();
        }

        // h. Let nfOpts be ! OrdinaryObjectCreate(null).
        auto* number_format_options = Object::create(realm, nullptr);

        // i. Let value be 0.
        auto value = Value(0);

        // j. Let done be false.
        auto done = false;

        // k. If unit is "seconds", "milliseconds" or "microseconds" and nextStyle is "numeric", then
        if (unit.is_one_of("seconds"sv, "milliseconds"sv, "microseconds"sv) && next_style == DurationFormat::ValueStyle::Numeric) {
            // i. Set value to duration.[[Microseconds]] + duration.[[Nanoseconds]] / 1000.
            auto value_number = duration.microseconds + (duration.nanoseconds / 1000);

            // ii. If unit is "seconds" or "milliseconds", then
            if (unit.is_one_of("seconds"sv, "milliseconds"sv)) {
                // 1. Set value to duration.[[Milliseconds]] + value / 1000.
                value_number = duration.milliseconds + (value_number / 1000);

                // 2. If unit is "seconds", then
                if (unit == "seconds"sv) {
                    // a. Set value to duration.[[Seconds]] + value / 1000.
                    value_number = duration.seconds + (value_number / 1000);
                }
            }
            value = Value(value_number);

            // iii. Perform ! CreateDataPropertyOrThrow(nfOpts, "maximumFractionDigits", durationFormat.[[FractionalDigits]]).
            MUST(number_format_options->create_data_property_or_throw(vm.names.maximumFractionDigits, duration_format.has_fractional_digits() ? Value(duration_format.fractional_digits()) : js_undefined()));

            // iv. Perform ! CreateDataPropertyOrThrow(nfOpts, "minimumFractionDigits", durationFormat.[[FractionalDigits]]).
            MUST(number_format_options->create_data_property_or_throw(vm.names.minimumFractionDigits, duration_format.has_fractional_digits() ? Value(duration_format.fractional_digits()) : js_undefined()));

            // v. Set done to true.
            done = true;
        }
        // l. Else,
        else {
            // i. Set value to the current value of the valueSlot slot of duration.
            value = Value(duration.*value_slot);
        }

        // m. If style is "2-digit", then
        if (style == DurationFormat::ValueStyle::TwoDigit) {
            // i. Perform ! CreateDataPropertyOrThrow(nfOpts, "minimumIntegerDigits", 2).
            MUST(number_format_options->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(2)));
        }

        // FIXME: Missing spec step - Let display be the current value of the displaySlot slot of durationFormat.
        auto display = (duration_format.*display_slot)();

        // n. If value is +0𝔽 or -0𝔽 and display is "auto", then
        if ((value.is_negative_zero() || value.is_positive_zero()) && display == DurationFormat::Display::Auto) {
            // i. Skip to the next iteration.
            continue;
        }

        // o. Let nf be ? Construct(%NumberFormat%, « durationFormat.[[Locale]], nfOpts »).
        auto* number_format = static_cast<Intl::NumberFormat*>(TRY(construct(vm, *realm.global_object().intl_number_format_constructor(), js_string(vm, duration_format.locale()), number_format_options)));

        // FIXME: durationFormat.[[NumberFormat]] is not a thing, the spec likely means 'nf' in this case
        // p. Let num be ! FormatNumeric(durationFormat.[[NumberFormat]], value).
        auto number = format_numeric(vm, *number_format, value);

        // q. Let dataLocale be durationFormat.[[DataLocale]].
        auto const& data_locale = duration_format.data_locale();

        // r. Let dataLocaleData be the current value of the dataLocale slot of %DurationFormat%.[[LocaleData]].

        // s. If style is "2-digit" or "numeric", then
        if (style == DurationFormat::ValueStyle::TwoDigit || style == DurationFormat::ValueStyle::Numeric) {
            // i. Append the new Record { [[Type]]: unit, [[Value]]: num} to the end of result.
            result.append({ unit, number });

            // ii. If unit is "hours" or "minutes", then
            if (unit.is_one_of("hours"sv, "minutes"sv)) {
                // 1. Let separator be dataLocaleData.[[formats]].[[digital]].[[separator]].
                auto separator = Unicode::get_number_system_symbol(data_locale, duration_format.numbering_system(), Unicode::NumericSymbol::TimeSeparator).value_or(":"sv);

                // 2. Append the new Record { [[Type]]: "literal", [[Value]]: separator} to the end of result.
                result.append({ "literal"sv, separator });
            }
        }

        // t. Else,
        else {
            // i. Let pr be ? Construct(%PluralRules%, « durationFormat.[[Locale]] »).
            auto* plural_rules = TRY(construct(vm, *realm.global_object().intl_plural_rules_constructor(), js_string(vm, duration_format.locale())));

            // ii. Let prv be ! ResolvePlural(pr, value).
            auto plurality = resolve_plural(static_cast<PluralRules&>(*plural_rules), value);

            auto formats = Unicode::get_unit_formats(data_locale, duration_instances_component.unit_singular, static_cast<Unicode::Style>(style));
            auto pattern = formats.find_if([&](auto& p) { return p.plurality == plurality; });
            if (pattern == formats.end())
                continue;

            // iii. Let template be the current value of the prv slot of the unit slot of the style slot of dataLocaleData.[[formats]].
            auto template_ = convert_number_format_pattern_to_duration_format_template(*pattern);

            // FIXME: MakePartsList takes a list, not a string, so likely missing spec step: Let fv be ! PartitionNumberPattern(nf, value).
            auto formatted_value = partition_number_pattern(vm, *number_format, value);

            // FIXME: Spec issue - see above, fv instead of num
            // iv. Let parts be ! MakePartsList(template, unit, num).
            auto parts = make_parts_list(template_, unit, formatted_value);

            // v. Let concat be an empty String.
            StringBuilder concat;

            // vi. For each element part in parts, in List order, do
            for (auto const& part : parts) {
                // 1. Set concat to the string-concatenation of concat and part.[[Value]].
                concat.append(part.value);

                // FIXME: It's not clear why this step is here, the unit is functional, it should not be part of the final formatted text
                // 2. If part has a [[Unit]] field, then
                // if (!part.unit.is_null()) {
                //     // a. Set concat to the string-concatenation of concat and part.[[Unit]].
                //     concat.append(part.unit);
                // }
            }

            // vii. Append the new Record { [[Type]]: unit, [[Value]]: concat } to the end of result.
            result.append({ unit, concat.build() });
        }

        // u. If done is true, then
        if (done) {
            // i. Stop iteration.
            break;
        }
    }

    // 3. Let lf be ? Construct(%ListFormat%, « durationFormat.[[Locale]] »).
    auto* list_format = static_cast<Intl::ListFormat*>(TRY(construct(vm, *realm.global_object().intl_list_format_constructor(), js_string(vm, duration_format.locale()))));

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

    // 4. Set result to ! CreatePartsFromList(lf, result).
    auto final_result = create_parts_from_list(*list_format, string_result);

    // 5. Return result.
    return final_result;
}

}
