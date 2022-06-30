/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormat.h>
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

// 1.1.1 ToDurationRecord ( input ), https://tc39.es/proposal-intl-duration-format/#sec-todurationrecord
ThrowCompletionOr<Temporal::DurationRecord> to_duration_record(GlobalObject& global_object, Value input)
{
    auto& vm = global_object.vm();

    // 1. If Type(input) is not Object, throw a TypeError exception.
    if (!input.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, input);
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
            value_number = TRY(Temporal::to_integer_without_rounding(global_object, value, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, unit, value));
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
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidDurationLikeObject);

    // 6. Return result.
    return result;
}

// 1.1.2 GetDurationUnitOptions ( unit, options, baseStyle, stylesList, digitalBase, prevStyle ), https://tc39.es/proposal-intl-duration-format/#sec-getdurationunitoptions
ThrowCompletionOr<DurationUnitOptions> get_duration_unit_options(GlobalObject& global_object, String const& unit, Object const& options, StringView base_style, Span<StringView const> styles_list, StringView digital_base, Optional<String> const& previous_style)
{
    auto& vm = global_object.vm();

    // 1. Let style be ? GetOption(options, unit, "string", stylesList, undefined).
    auto style_value = TRY(get_option(global_object, options, unit, OptionType::String, styles_list, Empty {}));

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
    auto display = TRY(get_option(global_object, options, display_field, OptionType::String, { "auto"sv, "always"sv }, display_default));

    // 6. If prevStyle is "numeric" or "2-digit", then
    if (previous_style == "numeric"sv || previous_style == "2-digit"sv) {
        // a. If style is not "numeric" or "2-digit", then
        if (style != "numeric"sv && style != "2-digit"sv) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::IntlNonNumericOr2DigitAfterNumericOr2Digit);
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

}
