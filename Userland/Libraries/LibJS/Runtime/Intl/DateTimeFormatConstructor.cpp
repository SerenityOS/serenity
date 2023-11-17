/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibLocale/DateTimeFormat.h>
#include <LibLocale/Locale.h>

namespace JS::Intl {

// 11.1 The Intl.DateTimeFormat Constructor, https://tc39.es/ecma402/#sec-intl-datetimeformat-constructor
DateTimeFormatConstructor::DateTimeFormatConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.DateTimeFormat.as_string(), realm.intrinsics().function_prototype())
{
}

void DateTimeFormatConstructor::initialize(Realm& realm)
{
    Base::initialize(realm);

    auto& vm = this->vm();

    // 11.2.1 Intl.DateTimeFormat.prototype, https://tc39.es/ecma402/#sec-intl.datetimeformat.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().intl_date_time_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 11.1.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<Value> DateTimeFormatConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object, else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 11.1.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<NonnullGCPtr<Object>> DateTimeFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let dateTimeFormat be ? CreateDateTimeFormat(newTarget, locales, options, any, date).
    auto date_time_format = TRY(create_date_time_format(vm, new_target, locales, options, OptionRequired::Any, OptionDefaults::Date));

    // 3. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Let this be the this value.
    //     b. Return ? ChainDateTimeFormat(dateTimeFormat, NewTarget, this).

    // 4. Return dateTimeFormat.
    return date_time_format;
}

// 11.2.2 Intl.DateTimeFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(DateTimeFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %DateTimeFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(vm, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(vm, requested_locales, options));
}

// 11.1.2 CreateDateTimeFormat ( newTarget, locales, options, required, defaults ), https://tc39.es/ecma402/#sec-createdatetimeformat
ThrowCompletionOr<NonnullGCPtr<DateTimeFormat>> create_date_time_format(VM& vm, FunctionObject& new_target, Value locales_value, Value options_value, OptionRequired required, OptionDefaults defaults)
{
    // 1. Let dateTimeFormat be ? OrdinaryCreateFromConstructor(newTarget, "%DateTimeFormat.prototype%", « [[InitializedDateTimeFormat]], [[Locale]], [[Calendar]], [[NumberingSystem]], [[TimeZone]], [[Weekday]], [[Era]], [[Year]], [[Month]], [[Day]], [[DayPeriod]], [[Hour]], [[Minute]], [[Second]], [[FractionalSecondDigits]], [[TimeZoneName]], [[HourCycle]], [[DateStyle]], [[TimeStyle]], [[Pattern]], [[RangePatterns]], [[BoundFormat]] »).
    auto date_time_format = TRY(ordinary_create_from_constructor<DateTimeFormat>(vm, new_target, &Intrinsics::intl_date_time_format_prototype));

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(vm, locales_value));

    // 3. Set options to ? CoerceOptionsToObject(options).
    auto* options = TRY(coerce_options_to_object(vm, options_value));

    // 4. Let opt be a new Record.
    LocaleOptions opt {};

    // 5. Let matcher be ? GetOption(options, "localeMatcher", string, « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(vm, *options, vm.names.localeMatcher, OptionType::String, AK::Array { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 6. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 7. Let calendar be ? GetOption(options, "calendar", string, empty, undefined).
    auto calendar = TRY(get_option(vm, *options, vm.names.calendar, OptionType::String, {}, Empty {}));

    // 8. If calendar is not undefined, then
    if (!calendar.is_undefined()) {
        // a. If calendar cannot be matched by the type Unicode locale nonterminal, throw a RangeError exception.
        if (!::Locale::is_type_identifier(calendar.as_string().utf8_string_view()))
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, calendar, "calendar"sv);

        // 9. Set opt.[[ca]] to calendar.
        opt.ca = calendar.as_string().utf8_string();
    }

    // 10. Let numberingSystem be ? GetOption(options, "numberingSystem", string, empty, undefined).
    auto numbering_system = TRY(get_option(vm, *options, vm.names.numberingSystem, OptionType::String, {}, Empty {}));

    // 11. If numberingSystem is not undefined, then
    if (!numbering_system.is_undefined()) {
        // a. If numberingSystem cannot be matched by the type Unicode locale nonterminal, throw a RangeError exception.
        if (!::Locale::is_type_identifier(numbering_system.as_string().utf8_string_view()))
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, numbering_system, "numberingSystem"sv);

        // 12. Set opt.[[nu]] to numberingSystem.
        opt.nu = numbering_system.as_string().utf8_string();
    }

    // 13. Let hour12 be ? GetOption(options, "hour12", boolean, empty, undefined).
    auto hour12 = TRY(get_option(vm, *options, vm.names.hour12, OptionType::Boolean, {}, Empty {}));

    // 14. Let hourCycle be ? GetOption(options, "hourCycle", string, « "h11", "h12", "h23", "h24" », undefined).
    auto hour_cycle = TRY(get_option(vm, *options, vm.names.hourCycle, OptionType::String, AK::Array { "h11"sv, "h12"sv, "h23"sv, "h24"sv }, Empty {}));

    // 15. If hour12 is not undefined, then
    if (!hour12.is_undefined()) {
        // a. Set hourCycle to null.
        hour_cycle = js_null();
    }

    // 16. Set opt.[[hc]] to hourCycle.
    if (!hour_cycle.is_nullish())
        opt.hc = hour_cycle.as_string().utf8_string();

    // 17. Let localeData be %DateTimeFormat%.[[LocaleData]].
    // 18. Let r be ResolveLocale(%DateTimeFormat%.[[AvailableLocales]], requestedLocales, opt, %DateTimeFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, DateTimeFormat::relevant_extension_keys());

    // 19. Set dateTimeFormat.[[Locale]] to r.[[locale]].
    date_time_format->set_locale(move(result.locale));

    // 20. Let resolvedCalendar be r.[[ca]].
    // 21. Set dateTimeFormat.[[Calendar]] to resolvedCalendar.
    if (result.ca.has_value())
        date_time_format->set_calendar(result.ca.release_value());

    // 22. Set dateTimeFormat.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        date_time_format->set_numbering_system(result.nu.release_value());

    // 23. Let dataLocale be r.[[dataLocale]].
    auto data_locale = move(result.data_locale);

    // Non-standard, the data locale is needed for LibUnicode lookups while formatting.
    date_time_format->set_data_locale(data_locale);

    // 24. Let dataLocaleData be localeData.[[<dataLocale>]].
    Optional<::Locale::HourCycle> hour_cycle_value;

    auto set_locale_hour_cycle = [&](auto... candidate_hour_cycles) {
        // Locale hour cycles (parsed from timeData.json) are stored in preference order. Use the
        // first hour cycle that matches any of the provided candidates. There may be no matches if
        // e.g. Unicode data generation is disabled.
        auto locale_hour_cycles = ::Locale::get_locale_hour_cycles(data_locale);

        auto index = locale_hour_cycles.find_first_index_if([&](auto hour_cycle) {
            return ((hour_cycle == candidate_hour_cycles) || ...);
        });

        if (index.has_value())
            hour_cycle_value = locale_hour_cycles[*index];
    };

    // 25. If hour12 is true, then
    if (hour12.is_boolean() && hour12.as_bool()) {
        // a. Let hc be dataLocaleData.[[hourCycle12]].
        set_locale_hour_cycle(::Locale::HourCycle::H11, ::Locale::HourCycle::H12);
    }
    // 26. Else if hour12 is false, then
    else if (hour12.is_boolean() && !hour12.as_bool()) {
        // a. Let hc be dataLocaleData.[[hourCycle24]].
        set_locale_hour_cycle(::Locale::HourCycle::H23, ::Locale::HourCycle::H24);
    }
    // 27. Else,
    else {
        // a. Assert: hour12 is undefined.
        VERIFY(hour12.is_undefined());

        // b. Let hc be r.[[hc]].
        if (result.hc.has_value())
            hour_cycle_value = ::Locale::hour_cycle_from_string(*result.hc);

        // c. If hc is null, set hc to dataLocaleData.[[hourCycle]].
        if (!hour_cycle_value.has_value())
            hour_cycle_value = ::Locale::get_default_regional_hour_cycle(data_locale);
    }

    // 28. Set dateTimeFormat.[[HourCycle]] to hc.
    if (hour_cycle_value.has_value())
        date_time_format->set_hour_cycle(*hour_cycle_value);

    // 29. Let timeZone be ? Get(options, "timeZone").
    auto time_zone_value = TRY(options->get(vm.names.timeZone));
    String time_zone;

    // 30. If timeZone is undefined, then
    if (time_zone_value.is_undefined()) {
        // a. Set timeZone to DefaultTimeZone().
        time_zone = MUST(String::from_utf8(system_time_zone_identifier()));
    }
    // 31. Else,
    else {
        // a. Set timeZone to ? ToString(timeZone).
        time_zone = TRY(time_zone_value.to_string(vm));
    }

    // 32. If IsTimeZoneOffsetString(timeZone) is true, then
    if (is_time_zone_offset_string(time_zone)) {
        // a. Let parseResult be ParseText(StringToCodePoints(timeZone), UTCOffset).
        auto parse_result = Temporal::parse_iso8601(Temporal::Production::TimeZoneNumericUTCOffset, time_zone);

        // b. Assert: parseResult is a Parse Node.
        VERIFY(parse_result.has_value());

        // c. If parseResult contains more than one MinuteSecond Parse Node, throw a RangeError exception.
        if (parse_result->time_zone_utc_offset_second.has_value())
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, time_zone, vm.names.timeZone);

        // d. Let offsetNanoseconds be ParseTimeZoneOffsetString(timeZone).
        auto offset_nanoseconds = parse_time_zone_offset_string(time_zone);

        // e. Let offsetMinutes be offsetNanoseconds / (6 × 10^10).
        auto offset_minutes = offset_nanoseconds / 60'000'000'000;

        // f. Assert: offsetMinutes is an integer.
        VERIFY(trunc(offset_minutes) == offset_minutes);

        // g. Set timeZone to FormatOffsetTimeZoneIdentifier(offsetMinutes).
        time_zone = format_offset_time_zone_identifier(offset_minutes);
    }
    // 33. Else if IsValidTimeZoneName(timeZone) is true, then
    else if (Temporal::is_available_time_zone_name(time_zone)) {
        // a. Set timeZone to CanonicalizeTimeZoneName(timeZone).
        time_zone = MUST(Temporal::canonicalize_time_zone_name(vm, time_zone));
    }
    // 34. Else,
    else {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, time_zone, vm.names.timeZone);
    }

    // 35. Set dateTimeFormat.[[TimeZone]] to timeZone.
    date_time_format->set_time_zone(move(time_zone));

    // 36. Let formatOptions be a new Record.
    ::Locale::CalendarPattern format_options {};

    // 37. Set formatOptions.[[hourCycle]] to hc.
    format_options.hour_cycle = hour_cycle_value;

    // 38. Let hasExplicitFormatComponents be false.
    // NOTE: Instead of using a boolean, we track any explicitly provided component name for nicer exception messages.
    PropertyKey const* explicit_format_component = nullptr;

    // 39. For each row of Table 6, except the header row, in table order, do
    TRY(for_each_calendar_field(vm, format_options, [&](auto& option, PropertyKey const& property, auto const& values) -> ThrowCompletionOr<void> {
        using ValueType = typename RemoveReference<decltype(option)>::ValueType;

        // a. Let prop be the name given in the Property column of the row.

        // b. If prop is "fractionalSecondDigits", then
        if constexpr (IsIntegral<ValueType>) {
            // i. Let value be ? GetNumberOption(options, "fractionalSecondDigits", 1, 3, undefined).
            auto value = TRY(get_number_option(vm, *options, property, 1, 3, {}));

            // d. Set formatOptions.[[<prop>]] to value.
            if (value.has_value()) {
                option = static_cast<ValueType>(value.value());

                // e. If value is not undefined, then
                //     i. Set hasExplicitFormatComponents to true.
                explicit_format_component = &property;
            }
        }
        // c. Else,
        else {
            // i. Let values be a List whose elements are the strings given in the Values column of the row.
            // ii. Let value be ? GetOption(options, prop, string, values, undefined).
            auto value = TRY(get_option(vm, *options, property, OptionType::String, values, Empty {}));

            // d. Set formatOptions.[[<prop>]] to value.
            if (!value.is_undefined()) {
                option = ::Locale::calendar_pattern_style_from_string(value.as_string().utf8_string_view());

                // e. If value is not undefined, then
                //     i. Set hasExplicitFormatComponents to true.
                explicit_format_component = &property;
            }
        }

        return {};
    }));

    // 40. Let matcher be ? GetOption(options, "formatMatcher", string, « "basic", "best fit" », "best fit").
    matcher = TRY(get_option(vm, *options, vm.names.formatMatcher, OptionType::String, AK::Array { "basic"sv, "best fit"sv }, "best fit"sv));

    // 41. Let dateStyle be ? GetOption(options, "dateStyle", string, « "full", "long", "medium", "short" », undefined).
    auto date_style = TRY(get_option(vm, *options, vm.names.dateStyle, OptionType::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 42. Set dateTimeFormat.[[DateStyle]] to dateStyle.
    if (!date_style.is_undefined())
        date_time_format->set_date_style(date_style.as_string().utf8_string_view());

    // 43. Let timeStyle be ? GetOption(options, "timeStyle", string, « "full", "long", "medium", "short" », undefined).
    auto time_style = TRY(get_option(vm, *options, vm.names.timeStyle, OptionType::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 44. Set dateTimeFormat.[[TimeStyle]] to timeStyle.
    if (!time_style.is_undefined())
        date_time_format->set_time_style(time_style.as_string().utf8_string_view());

    Optional<::Locale::CalendarPattern> best_format {};

    // 45. If dateStyle is not undefined or timeStyle is not undefined, then
    if (date_time_format->has_date_style() || date_time_format->has_time_style()) {
        // a. If hasExplicitFormatComponents is true, then
        if (explicit_format_component != nullptr) {
            // i. Throw a TypeError exception.
            return vm.throw_completion<TypeError>(ErrorType::IntlInvalidDateTimeFormatOption, *explicit_format_component, "dateStyle or timeStyle"sv);
        }

        // b. If required is date and timeStyle is not undefined, then
        if (required == OptionRequired::Date && !time_style.is_undefined()) {
            // i. Throw a TypeError exception.
            return vm.throw_completion<TypeError>(ErrorType::IntlInvalidDateTimeFormatOption, "timeStyle"sv, "date"sv);
        }

        // c. If required is time and dateStyle is not undefined, then
        if (required == OptionRequired::Time && !date_style.is_undefined()) {
            // i. Throw a TypeError exception.
            return vm.throw_completion<TypeError>(ErrorType::IntlInvalidDateTimeFormatOption, "dateStyle"sv, "time"sv);
        }

        // d. Let styles be dataLocaleData.[[styles]].[[<resolvedCalendar>]].
        // e. Let bestFormat be DateTimeStyleFormat(dateStyle, timeStyle, styles).
        best_format = date_time_style_format(data_locale, date_time_format);
    }
    // 46. Else,
    else {
        // a. Let needDefaults be true.
        bool needs_defaults = true;

        // b. If required is date or any, then
        if (required == OptionRequired::Date || required == OptionRequired::Any) {
            // i. For each property name prop of « "weekday", "year", "month", "day" », do
            auto check_property_value = [&](auto const& value) {
                // 1. Let value be formatOptions.[[<prop>]].
                // 2. If value is not undefined, let needDefaults be false.
                if (value.has_value())
                    needs_defaults = false;
            };

            check_property_value(format_options.weekday);
            check_property_value(format_options.year);
            check_property_value(format_options.month);
            check_property_value(format_options.day);
        }

        // c. If required is time or any, then
        if (required == OptionRequired::Time || required == OptionRequired::Any) {
            // i. For each property name prop of « "dayPeriod", "hour", "minute", "second", "fractionalSecondDigits" », do
            auto check_property_value = [&](auto const& value) {
                // 1. Let value be formatOptions.[[<prop>]].
                // 2. If value is not undefined, let needDefaults be false.
                if (value.has_value())
                    needs_defaults = false;
            };

            check_property_value(format_options.day_period);
            check_property_value(format_options.hour);
            check_property_value(format_options.minute);
            check_property_value(format_options.second);
            check_property_value(format_options.fractional_second_digits);
        }

        // d. If needDefaults is true and defaults is either date or all, then
        if (needs_defaults && (defaults == OptionDefaults::Date || defaults == OptionDefaults::All)) {
            // i. For each property name prop of « "year", "month", "day" », do
            auto set_property_value = [&](auto& value) {
                // 1. Set formatOptions.[[<prop>]] to "numeric".
                value = ::Locale::CalendarPatternStyle::Numeric;
            };

            set_property_value(format_options.year);
            set_property_value(format_options.month);
            set_property_value(format_options.day);
        }

        // e. If needDefaults is true and defaults is either time or all, then
        if (needs_defaults && (defaults == OptionDefaults::Time || defaults == OptionDefaults::All)) {
            // i. For each property name prop of « "hour", "minute", "second" », do
            auto set_property_value = [&](auto& value) {
                // 1. Set formatOptions.[[<prop>]] to "numeric".
                value = ::Locale::CalendarPatternStyle::Numeric;
            };

            set_property_value(format_options.hour);
            set_property_value(format_options.minute);
            set_property_value(format_options.second);
        }

        // f. Let formats be dataLocaleData.[[formats]].[[<resolvedCalendar>]].
        auto formats = ::Locale::get_calendar_available_formats(data_locale, date_time_format->calendar());

        // g. If matcher is "basic", then
        if (matcher.as_string().utf8_string_view() == "basic"sv) {
            // i. Let bestFormat be BasicFormatMatcher(formatOptions, formats).
            best_format = basic_format_matcher(format_options, move(formats));
        }
        // h. Else,
        else {
            // i. Let bestFormat be BestFitFormatMatcher(formatOptions, formats).
            best_format = best_fit_format_matcher(format_options, move(formats));
        }
    }

    // 47. For each row in Table 6, except the header row, in table order, do
    date_time_format->for_each_calendar_field_zipped_with(*best_format, [&](auto& date_time_format_field, auto const& best_format_field, auto) {
        // a. Let prop be the name given in the Property column of the row.
        // b. If bestFormat has a field [[<prop>]], then
        if (best_format_field.has_value()) {
            // i. Let p be bestFormat.[[<prop>]].
            // ii. Set dateTimeFormat's internal slot whose name is the Internal Slot column of the row to p.
            date_time_format_field = best_format_field;
        }
    });

    String pattern;
    Vector<::Locale::CalendarRangePattern> range_patterns;

    // 48. If dateTimeFormat.[[Hour]] is undefined, then
    if (!date_time_format->has_hour()) {
        // a. Set dateTimeFormat.[[HourCycle]] to undefined.
        date_time_format->clear_hour_cycle();
    }

    // 49. If dateTimeFormat.[[HourCycle]] is "h11" or "h12", then
    if ((hour_cycle_value == ::Locale::HourCycle::H11) || (hour_cycle_value == ::Locale::HourCycle::H12)) {
        // a. Let pattern be bestFormat.[[pattern12]].
        if (best_format->pattern12.has_value()) {
            pattern = best_format->pattern12.release_value();
        } else {
            // Non-standard, LibUnicode only provides [[pattern12]] when [[pattern]] has a day
            // period. Other implementations provide [[pattern12]] as a copy of [[pattern]].
            pattern = move(best_format->pattern);
        }

        // b. Let rangePatterns be bestFormat.[[rangePatterns12]].
        range_patterns = ::Locale::get_calendar_range12_formats(data_locale, date_time_format->calendar(), best_format->skeleton);
    }
    // 50. Else,
    else {
        // a. Let pattern be bestFormat.[[pattern]].
        pattern = move(best_format->pattern);

        // b. Let rangePatterns be bestFormat.[[rangePatterns]].
        range_patterns = ::Locale::get_calendar_range_formats(data_locale, date_time_format->calendar(), best_format->skeleton);
    }

    // 51. Set dateTimeFormat.[[Pattern]] to pattern.
    date_time_format->set_pattern(move(pattern));

    // 52. Set dateTimeFormat.[[RangePatterns]] to rangePatterns.
    date_time_format->set_range_patterns(move(range_patterns));

    // 53. Return dateTimeFormat.
    return date_time_format;
}

// 11.1.3 FormatOffsetTimeZoneIdentifier ( offsetMinutes ), https://tc39.es/ecma402/#sec-formatoffsettimezoneidentifier
String format_offset_time_zone_identifier(double offset_minutes)
{
    // 1. If offsetMinutes ≥ 0, let sign be the code unit 0x002B (PLUS SIGN); otherwise, let sign be the code unit 0x002D (HYPHEN-MINUS).
    auto sign = offset_minutes >= 0.0 ? '+' : '-';

    // 2. Let absoluteMinutes be abs(offsetMinutes).
    auto absolute_minutes = fabs(offset_minutes);

    // 3. Let hours be floor(absoluteMinutes / 60).
    auto hours = static_cast<i64>(floor(absolute_minutes / 60.0));

    // 4. Let minutes be absoluteMinutes modulo 60.
    auto minutes = static_cast<i64>(modulo(absolute_minutes, 60.0));

    // 5. Return the string-concatenation of sign, ToZeroPaddedDecimalString(hours, 2), the code unit 0x003A (COLON), and ToZeroPaddedDecimalString(minutes, 2).
    return MUST(String::formatted("{}{:02}:{:02}", sign, hours, minutes));
}

}
