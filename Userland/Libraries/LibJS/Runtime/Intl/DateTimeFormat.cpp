/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 11 DateTimeFormat Objects, https://tc39.es/ecma402/#datetimeformat-objects
DateTimeFormat::DateTimeFormat(Object& prototype)
    : Object(prototype)
{
}

DateTimeFormat::Style DateTimeFormat::style_from_string(StringView style)
{
    if (style == "full"sv)
        return Style::Full;
    if (style == "long"sv)
        return Style::Long;
    if (style == "medium"sv)
        return Style::Medium;
    if (style == "short"sv)
        return Style::Short;
    VERIFY_NOT_REACHED();
}

StringView DateTimeFormat::style_to_string(Style style)
{
    switch (style) {
    case Style::Full:
        return "full"sv;
    case Style::Long:
        return "long"sv;
    case Style::Medium:
        return "medium"sv;
    case Style::Short:
        return "short"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 11.1.1 InitializeDateTimeFormat ( dateTimeFormat, locales, options ), https://tc39.es/ecma402/#sec-initializedatetimeformat
ThrowCompletionOr<DateTimeFormat*> initialize_date_time_format(GlobalObject& global_object, DateTimeFormat& date_time_format, Value locales_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales_value));

    // 2. Let options be ? ToDateTimeOptions(options, "any", "date").
    auto* options = TRY(to_date_time_options(global_object, options_value, OptionRequired::Any, OptionDefaults::Date));

    // 3. Let opt be a new Record.
    LocaleOptions opt {};

    // 4. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, Value::Type::String, AK::Array { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 5. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 6. Let calendar be ? GetOption(options, "calendar", "string", undefined, undefined).
    auto calendar = TRY(get_option(global_object, *options, vm.names.calendar, Value::Type::String, {}, Empty {}));

    // 7. If calendar is not undefined, then
    if (!calendar.is_undefined()) {
        // a. If calendar does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
        if (!Unicode::is_type_identifier(calendar.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, calendar, "calendar"sv);

        // 8. Set opt.[[ca]] to calendar.
        opt.ca = calendar.as_string().string();
    }

    // 9. Let numberingSystem be ? GetOption(options, "numberingSystem", "string", undefined, undefined).
    auto numbering_system = TRY(get_option(global_object, *options, vm.names.numberingSystem, Value::Type::String, {}, Empty {}));

    // 10. If numberingSystem is not undefined, then
    if (!numbering_system.is_undefined()) {
        // a. If numberingSystem does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
        if (!Unicode::is_type_identifier(numbering_system.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, numbering_system, "numberingSystem"sv);

        // 11. Set opt.[[nu]] to numberingSystem.
        opt.nu = numbering_system.as_string().string();
    }

    // 12. Let hour12 be ? GetOption(options, "hour12", "boolean", undefined, undefined).
    auto hour12 = TRY(get_option(global_object, *options, vm.names.hour12, Value::Type::Boolean, {}, Empty {}));

    // 13. Let hourCycle be ? GetOption(options, "hourCycle", "string", « "h11", "h12", "h23", "h24" », undefined).
    auto hour_cycle = TRY(get_option(global_object, *options, vm.names.hourCycle, Value::Type::String, AK::Array { "h11"sv, "h12"sv, "h23"sv, "h24"sv }, Empty {}));

    // 14. If hour12 is not undefined, then
    if (!hour12.is_undefined()) {
        // a. Let hourCycle be null.
        hour_cycle = js_null();
    }

    // 15. Set opt.[[hc]] to hourCycle.
    if (!hour_cycle.is_nullish())
        opt.hc = hour_cycle.as_string().string();

    // 16. Let localeData be %DateTimeFormat%.[[LocaleData]].
    // 17. Let r be ResolveLocale(%DateTimeFormat%.[[AvailableLocales]], requestedLocales, opt, %DateTimeFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, DateTimeFormat::relevant_extension_keys());

    // 18. Set dateTimeFormat.[[Locale]] to r.[[locale]].
    date_time_format.set_locale(move(result.locale));

    // 19. Let calendar be r.[[ca]].
    // 20. Set dateTimeFormat.[[Calendar]] to calendar.
    date_time_format.set_calendar(result.ca.release_value());

    // 21. Set dateTimeFormat.[[HourCycle]] to r.[[hc]].
    date_time_format.set_hour_cycle(result.hc.release_value());

    // 22. Set dateTimeFormat.[[NumberingSystem]] to r.[[nu]].
    date_time_format.set_numbering_system(result.nu.release_value());

    // 23. Let dataLocale be r.[[dataLocale]].
    auto data_locale = move(result.data_locale);

    // Non-standard, the data locale is needed for LibUnicode lookups while formatting.
    date_time_format.set_data_locale(data_locale);

    // 24. Let timeZone be ? Get(options, "timeZone").
    auto time_zone_value = TRY(options->get(vm.names.timeZone));
    String time_zone;

    // 25. If timeZone is undefined, then
    if (time_zone_value.is_undefined()) {
        // a. Let timeZone be DefaultTimeZone().
        time_zone = Temporal::default_time_zone();
    }
    // 26. Else,
    else {
        // a. Let timeZone be ? ToString(timeZone).
        time_zone = TRY(time_zone_value.to_string(global_object));

        // b. If the result of IsValidTimeZoneName(timeZone) is false, then
        if (!Temporal::is_valid_time_zone_name(time_zone)) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, time_zone, vm.names.timeZone);
        }

        // c. Let timeZone be CanonicalizeTimeZoneName(timeZone).
        time_zone = Temporal::canonicalize_time_zone_name(time_zone);
    }

    // 27. Set dateTimeFormat.[[TimeZone]] to timeZone.
    date_time_format.set_time_zone(move(time_zone));

    // 28. Let opt be a new Record.
    Unicode::CalendarPattern format_options {};

    // 29. For each row of Table 4, except the header row, in table order, do
    TRY(for_each_calendar_field(global_object, format_options, [&](auto& option, auto const& property, auto const& defaults) -> ThrowCompletionOr<void> {
        using ValueType = typename RemoveReference<decltype(option)>::ValueType;

        // a. Let prop be the name given in the Property column of the row.

        // b. If prop is "fractionalSecondDigits", then
        if constexpr (IsIntegral<ValueType>) {
            // i. Let value be ? GetNumberOption(options, "fractionalSecondDigits", 1, 3, undefined).
            auto value = TRY(get_number_option(global_object, *options, property, 1, 3, {}));

            // d. Set opt.[[<prop>]] to value.
            if (value.has_value())
                option = static_cast<ValueType>(value.value());
        }
        // c. Else,
        else {
            // i. Let value be ? GetOption(options, prop, "string", « the strings given in the Values column of the row », undefined).
            auto value = TRY(get_option(global_object, *options, property, Value::Type::String, defaults, Empty {}));

            // d. Set opt.[[<prop>]] to value.
            if (!value.is_undefined())
                option = Unicode::calendar_pattern_style_from_string(value.as_string().string());
        }

        return {};
    }));

    // 30. Let dataLocaleData be localeData.[[<dataLocale>]].

    // 31. Let matcher be ? GetOption(options, "formatMatcher", "string", « "basic", "best fit" », "best fit").
    matcher = TRY(get_option(global_object, *options, vm.names.formatMatcher, Value::Type::String, AK::Array { "basic"sv, "best fit"sv }, "best fit"sv));

    // 32. Let dateStyle be ? GetOption(options, "dateStyle", "string", « "full", "long", "medium", "short" », undefined).
    auto date_style = TRY(get_option(global_object, *options, vm.names.dateStyle, Value::Type::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 33. Set dateTimeFormat.[[DateStyle]] to dateStyle.
    if (!date_style.is_undefined())
        date_time_format.set_date_style(date_style.as_string().string());

    // 34. Let timeStyle be ? GetOption(options, "timeStyle", "string", « "full", "long", "medium", "short" », undefined).
    auto time_style = TRY(get_option(global_object, *options, vm.names.timeStyle, Value::Type::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 35. Set dateTimeFormat.[[TimeStyle]] to timeStyle.
    if (!time_style.is_undefined())
        date_time_format.set_time_style(time_style.as_string().string());

    Optional<Unicode::CalendarPattern> best_format {};

    // 36. If dateStyle is not undefined or timeStyle is not undefined, then
    if (date_time_format.has_date_style() || date_time_format.has_time_style()) {
        // a. For each row in Table 4, except the header row, do
        TRY(for_each_calendar_field(global_object, format_options, [&](auto const& option, auto const& property, auto const&) -> ThrowCompletionOr<void> {
            // i. Let prop be the name given in the Property column of the row.
            // ii. Let p be opt.[[<prop>]].
            // iii. If p is not undefined, then
            if (option.has_value()) {
                // 1. Throw a TypeError exception.
                return vm.throw_completion<TypeError>(global_object, ErrorType::IntlInvalidDateTimeFormatOption, property, "dateStyle or timeStyle"sv);
            }

            return {};
        }));

        // b. Let styles be dataLocaleData.[[styles]].[[<calendar>]].
        // c. Let bestFormat be DateTimeStyleFormat(dateStyle, timeStyle, styles).
        best_format = date_time_style_format(data_locale, date_time_format);
    }
    // 37. Else,
    else {
        // a. Let formats be dataLocaleData.[[formats]].[[<calendar>]].
        auto formats = Unicode::get_calendar_available_formats(data_locale, date_time_format.calendar());

        // b. If matcher is "basic", then
        if (matcher.as_string().string() == "basic"sv) {
            // i. Let bestFormat be BasicFormatMatcher(opt, formats).
            best_format = basic_format_matcher(format_options, move(formats));
        }
        // c. Else,
        else {
            // i. Let bestFormat be BestFitFormatMatcher(opt, formats).
            best_format = best_fit_format_matcher(format_options, move(formats));
        }
    }

    // Non-standard, best_format will be empty if Unicode data generation is disabled.
    if (!best_format.has_value())
        return &date_time_format;

    // 38. For each row in Table 4, except the header row, in table order, do
    date_time_format.for_each_calendar_field_zipped_with(*best_format, [&](auto& date_time_format_field, auto const& best_format_field, auto) {
        // a. Let prop be the name given in the Property column of the row.
        // b. If bestFormat has a field [[<prop>]], then
        if (best_format_field.has_value()) {
            // i. Let p be bestFormat.[[<prop>]].
            // ii. Set dateTimeFormat's internal slot whose name is the Internal Slot column of the row to p.
            date_time_format_field = best_format_field;
        }
    });

    String pattern;

    // 39. If dateTimeFormat.[[Hour]] is undefined, then
    if (!date_time_format.has_hour()) {
        // a. Set dateTimeFormat.[[HourCycle]] to undefined.
        date_time_format.clear_hour_cycle();

        // b. Let pattern be bestFormat.[[pattern]].
        pattern = move(best_format->pattern);

        // FIXME: Implement step c when range formats are parsed by LibUnicode.
        // c. Let rangePatterns be bestFormat.[[rangePatterns]].
    }
    // 40. Else,
    else {
        // a. Let hcDefault be dataLocaleData.[[hourCycle]].
        auto default_hour_cycle = Unicode::get_default_regional_hour_cycle(data_locale);
        VERIFY(default_hour_cycle.has_value());

        // b. Let hc be dateTimeFormat.[[HourCycle]].
        // c. If hc is null, then
        //     i. Set hc to hcDefault.
        auto hour_cycle = date_time_format.has_hour_cycle() ? date_time_format.hour_cycle() : *default_hour_cycle;

        // d. If hour12 is not undefined, then
        if (!hour12.is_undefined()) {
            // i. If hour12 is true, then
            if (hour12.as_bool()) {
                // 1. If hcDefault is "h11" or "h23", then
                if ((default_hour_cycle == Unicode::HourCycle::H11) || (default_hour_cycle == Unicode::HourCycle::H23)) {
                    // a. Set hc to "h11".
                    hour_cycle = Unicode::HourCycle::H11;
                }
                // 2. Else,
                else {
                    // a. Set hc to "h12".
                    hour_cycle = Unicode::HourCycle::H12;
                }
            }
            // ii. Else,
            else {
                // 1. Assert: hour12 is false.
                // 2. If hcDefault is "h11" or "h23", then
                if ((default_hour_cycle == Unicode::HourCycle::H11) || (default_hour_cycle == Unicode::HourCycle::H23)) {
                    // a. Set hc to "h23".
                    hour_cycle = Unicode::HourCycle::H23;
                }
                // 3. Else,
                else {
                    // a. Set hc to "h24".
                    hour_cycle = Unicode::HourCycle::H24;
                }
            }
        }

        // e. Set dateTimeFormat.[[HourCycle]] to hc.
        date_time_format.set_hour_cycle(hour_cycle);

        // FIXME: Implement steps f.ii and g.ii when range formats are parsed by LibUnicode.

        // f. If dateTimeformat.[[HourCycle]] is "h11" or "h12", then
        if ((hour_cycle == Unicode::HourCycle::H11) || (hour_cycle == Unicode::HourCycle::H12)) {
            // i. Let pattern be bestFormat.[[pattern12]].
            if (best_format->pattern12.has_value()) {
                pattern = best_format->pattern12.release_value();
            } else {
                // Non-standard, LibUnicode only provides [[pattern12]] when [[pattern]] has a day
                // period. Other implementations provide [[pattern12]] as a copy of [[pattern]].
                pattern = move(best_format->pattern);
            }

            // ii. Let rangePatterns be bestFormat.[[rangePatterns12]].
        }
        // g. Else,
        else {
            // i. Let pattern be bestFormat.[[pattern]].
            pattern = move(best_format->pattern);

            // ii. Let rangePatterns be bestFormat.[[rangePatterns]].
        }
    }

    // 41. Set dateTimeFormat.[[Pattern]] to pattern.
    date_time_format.set_pattern(move(pattern));

    // FIXME: Implement step 42 when range formats are parsed by LibUnicode.
    // 42. Set dateTimeFormat.[[RangePatterns]] to rangePatterns.

    // 43. Return dateTimeFormat.
    return &date_time_format;
}

// 11.1.2 ToDateTimeOptions ( options, required, defaults ), https://tc39.es/ecma402/#sec-todatetimeoptions
ThrowCompletionOr<Object*> to_date_time_options(GlobalObject& global_object, Value options_value, OptionRequired required, OptionDefaults defaults)
{
    auto& vm = global_object.vm();

    // 1. If options is undefined, let options be null; otherwise let options be ? ToObject(options).
    Object* options = nullptr;
    if (!options_value.is_undefined())
        options = TRY(options_value.to_object(global_object));

    // 2. Let options be OrdinaryObjectCreate(options).
    options = Object::create(global_object, options);

    // 3. Let needDefaults be true.
    bool needs_defaults = true;

    // 4. If required is "date" or "any", then
    if ((required == OptionRequired::Date) || (required == OptionRequired::Any)) {
        // a. For each property name prop of « "weekday", "year", "month", "day" », do
        for (auto const& property : AK::Array { vm.names.weekday, vm.names.year, vm.names.month, vm.names.day }) {
            // i. Let value be ? Get(options, prop).
            auto value = TRY(options->get(property));

            // ii. If value is not undefined, let needDefaults be false.
            if (!value.is_undefined())
                needs_defaults = false;
        }
    }

    // 5. If required is "time" or "any", then
    if ((required == OptionRequired::Time) || (required == OptionRequired::Any)) {
        // a. For each property name prop of « "dayPeriod", "hour", "minute", "second", "fractionalSecondDigits" », do
        for (auto const& property : AK::Array { vm.names.dayPeriod, vm.names.hour, vm.names.minute, vm.names.second, vm.names.fractionalSecondDigits }) {
            // i. Let value be ? Get(options, prop).
            auto value = TRY(options->get(property));

            // ii. If value is not undefined, let needDefaults be false.
            if (!value.is_undefined())
                needs_defaults = false;
        }
    }

    // 6. Let dateStyle be ? Get(options, "dateStyle").
    auto date_style = TRY(options->get(vm.names.dateStyle));

    // 7. Let timeStyle be ? Get(options, "timeStyle").
    auto time_style = TRY(options->get(vm.names.timeStyle));

    // 8. If dateStyle is not undefined or timeStyle is not undefined, let needDefaults be false.
    if (!date_style.is_undefined() || !time_style.is_undefined())
        needs_defaults = false;

    // 9. If required is "date" and timeStyle is not undefined, then
    if ((required == OptionRequired::Date) && !time_style.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::IntlInvalidDateTimeFormatOption, "timeStyle"sv, "date"sv);
    }

    // 10. If required is "time" and dateStyle is not undefined, then
    if ((required == OptionRequired::Time) && !date_style.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::IntlInvalidDateTimeFormatOption, "dateStyle"sv, "time"sv);
    }

    // 11. If needDefaults is true and defaults is either "date" or "all", then
    if (needs_defaults && ((defaults == OptionDefaults::Date) || (defaults == OptionDefaults::All))) {
        // a. For each property name prop of « "year", "month", "day" », do
        for (auto const& property : AK::Array { vm.names.year, vm.names.month, vm.names.day }) {
            // i. Perform ? CreateDataPropertyOrThrow(options, prop, "numeric").
            TRY(options->create_data_property_or_throw(property, js_string(vm, "numeric"sv)));
        }
    }

    // 12. If needDefaults is true and defaults is either "time" or "all", then
    if (needs_defaults && ((defaults == OptionDefaults::Time) || (defaults == OptionDefaults::All))) {
        // a. For each property name prop of « "hour", "minute", "second" », do
        for (auto const& property : AK::Array { vm.names.hour, vm.names.minute, vm.names.second }) {
            // i. Perform ? CreateDataPropertyOrThrow(options, prop, "numeric").
            TRY(options->create_data_property_or_throw(property, js_string(vm, "numeric"sv)));
        }
    }

    // 13. Return options.
    return options;
}

// 11.1.3 DateTimeStyleFormat ( dateStyle, timeStyle, styles ), https://tc39.es/ecma402/#sec-date-time-style-format
Optional<Unicode::CalendarPattern> date_time_style_format(StringView data_locale, DateTimeFormat& date_time_format)
{
    Unicode::CalendarPattern time_format {};
    Unicode::CalendarPattern date_format {};

    auto get_pattern = [&](auto type, auto style) -> Optional<Unicode::CalendarPattern> {
        auto formats = Unicode::get_calendar_format(data_locale, date_time_format.calendar(), type);

        if (formats.has_value()) {
            switch (style) {
            case DateTimeFormat::Style::Full:
                return formats->full_format;
            case DateTimeFormat::Style::Long:
                return formats->long_format;
            case DateTimeFormat::Style::Medium:
                return formats->medium_format;
            case DateTimeFormat::Style::Short:
                return formats->short_format;
            }
        }

        return {};
    };

    // 1. If timeStyle is not undefined, then
    if (date_time_format.has_time_style()) {
        // a. Assert: timeStyle is one of "full", "long", "medium", or "short".
        // b. Let timeFormat be styles.[[TimeFormat]].[[<timeStyle>]].
        auto pattern = get_pattern(Unicode::CalendarFormatType::Time, date_time_format.time_style());
        if (!pattern.has_value())
            return {};

        time_format = pattern.release_value();
    }

    // 2. If dateStyle is not undefined, then
    if (date_time_format.has_date_style()) {
        // a. Assert: dateStyle is one of "full", "long", "medium", or "short".
        // b. Let dateFormat be styles.[[DateFormat]].[[<dateStyle>]].
        auto pattern = get_pattern(Unicode::CalendarFormatType::Date, date_time_format.date_style());
        if (!pattern.has_value())
            return {};

        date_format = pattern.release_value();
    }

    // 3. If dateStyle is not undefined and timeStyle is not undefined, then
    if (date_time_format.has_date_style() && date_time_format.has_time_style()) {
        // a. Let format be a new Record.
        Unicode::CalendarPattern format {};

        // b. Add to format all fields from dateFormat except [[pattern]] and [[rangePatterns]].
        format.for_each_calendar_field_zipped_with(date_format, [](auto& format_field, auto const& date_format_field, auto) {
            format_field = date_format_field;
        });

        // c. Add to format all fields from timeFormat except [[pattern]], [[rangePatterns]], [[pattern12]], and [[rangePatterns12]], if present.
        format.for_each_calendar_field_zipped_with(time_format, [](auto& format_field, auto const& time_format_field, auto) {
            if (time_format_field.has_value())
                format_field = time_format_field;
        });

        // d. Let connector be styles.[[DateTimeFormat]].[[<dateStyle>]].
        auto connector = get_pattern(Unicode::CalendarFormatType::DateTime, date_time_format.date_style());
        if (!connector.has_value())
            return {};

        // e. Let pattern be the string connector with the substring "{0}" replaced with timeFormat.[[pattern]] and the substring "{1}" replaced with dateFormat.[[pattern]].
        auto pattern = connector->pattern.replace("{0}"sv, time_format.pattern).replace("{1}"sv, date_format.pattern);

        // f. Set format.[[pattern]] to pattern.
        format.pattern = move(pattern);

        // g. If timeFormat has a [[pattern12]] field, then
        if (time_format.pattern12.has_value()) {
            // i. Let pattern12 be the string connector with the substring "{0}" replaced with timeFormat.[[pattern12]] and the substring "{1}" replaced with dateFormat.[[pattern]].
            auto pattern12 = connector->pattern.replace("{0}"sv, *time_format.pattern12).replace("{1}"sv, date_format.pattern);

            // ii. Set format.[[pattern12]] to pattern12.
            format.pattern12 = move(pattern12);
        }

        // FIXME: Implement steps h-j when range formats are parsed by LibUnicode.
        // h. Let dateTimeRangeFormat be styles.[[DateTimeRangeFormat]].[[<dateStyle>]].[[<timeStyle>]].
        // i. Set format.[[rangePatterns]] to dateTimeRangeFormat.[[rangePatterns]].
        // j. If dateTimeRangeFormat has a [[rangePatterns12]] field, then
        //     i. Set format.[[rangePatterns12]] to dateTimeRangeFormat.[[rangePatterns12]].

        // k. Return format.
        return format;
    }

    // 4. If timeStyle is not undefined, then
    if (date_time_format.has_time_style()) {
        // a. Return timeFormat.
        return time_format;
    }

    // 5. Assert: dateStyle is not undefined.
    VERIFY(date_time_format.has_date_style());

    // 6. Return dateFormat.
    return date_format;
}

// 11.1.4 BasicFormatMatcher ( options, formats ), https://tc39.es/ecma402/#sec-basicformatmatcher
Optional<Unicode::CalendarPattern> basic_format_matcher(Unicode::CalendarPattern const& options, Vector<Unicode::CalendarPattern> formats)
{
    // 1. Let removalPenalty be 120.
    constexpr int removal_penalty = 120;

    // 2. Let additionPenalty be 20.
    constexpr int addition_penalty = 20;

    // 3. Let longLessPenalty be 8.
    constexpr int long_less_penalty = 8;

    // 4. Let longMorePenalty be 6.
    constexpr int long_more_penalty = 6;

    // 5. Let shortLessPenalty be 6.
    constexpr int short_less_penalty = 6;

    // 6. Let shortMorePenalty be 3.
    constexpr int short_more_penalty = 3;

    // 7. Let bestScore be -Infinity.
    int best_score = NumericLimits<int>::min();

    // 8. Let bestFormat be undefined.
    Optional<Unicode::CalendarPattern> best_format;

    // 9. Assert: Type(formats) is List.
    // 10. For each element format of formats, do
    for (auto& format : formats) {
        // a. Let score be 0.
        int score = 0;

        // b. For each property name property shown in Table 4, do
        format.for_each_calendar_field_zipped_with(options, [&](auto const& format_prop, auto const& options_prop, auto) {
            using ValueType = typename RemoveReference<decltype(options_prop)>::ValueType;

            // i. If options has a field [[<property>]], let optionsProp be options.[[<property>]]; else let optionsProp be undefined.
            // ii. If format has a field [[<property>]], let formatProp be format.[[<property>]]; else let formatProp be undefined.

            // iii. If optionsProp is undefined and formatProp is not undefined, decrease score by additionPenalty.
            if (!options_prop.has_value() && format_prop.has_value()) {
                score -= addition_penalty;
            }
            // iv. Else if optionsProp is not undefined and formatProp is undefined, decrease score by removalPenalty.
            else if (options_prop.has_value() && !format_prop.has_value()) {
                score -= removal_penalty;
            }
            // v. Else if optionsProp ≠ formatProp, then
            else if (options_prop != format_prop) {
                using ValuesType = Conditional<IsIntegral<ValueType>, AK::Array<u8, 3>, AK::Array<Unicode::CalendarPatternStyle, 5>>;
                ValuesType values {};

                // 1. If property is "fractionalSecondDigits", then
                if constexpr (IsIntegral<ValueType>) {
                    // a. Let values be « 1𝔽, 2𝔽, 3𝔽 ».
                    values = { 1, 2, 3 };
                }
                // 2. Else,
                else {
                    // a. Let values be « "2-digit", "numeric", "narrow", "short", "long" ».
                    values = {
                        Unicode::CalendarPatternStyle::TwoDigit,
                        Unicode::CalendarPatternStyle::Numeric,
                        Unicode::CalendarPatternStyle::Narrow,
                        Unicode::CalendarPatternStyle::Short,
                        Unicode::CalendarPatternStyle::Long,
                    };
                }

                // 3. Let optionsPropIndex be the index of optionsProp within values.
                auto options_prop_index = static_cast<int>(find_index(values.begin(), values.end(), *options_prop));

                // 4. Let formatPropIndex be the index of formatProp within values.
                auto format_prop_index = static_cast<int>(find_index(values.begin(), values.end(), *format_prop));

                // 5. Let delta be max(min(formatPropIndex - optionsPropIndex, 2), -2).
                int delta = max(min(format_prop_index - options_prop_index, 2), -2);

                // 6. If delta = 2, decrease score by longMorePenalty.
                if (delta == 2)
                    score -= long_more_penalty;
                // 7. Else if delta = 1, decrease score by shortMorePenalty.
                else if (delta == 1)
                    score -= short_more_penalty;
                // 8. Else if delta = -1, decrease score by shortLessPenalty.
                else if (delta == -1)
                    score -= short_less_penalty;
                // 9. Else if delta = -2, decrease score by longLessPenalty.
                else if (delta == -2)
                    score -= long_less_penalty;
            }
        });

        // c. If score > bestScore, then
        if (score > best_score) {
            // i. Let bestScore be score.
            best_score = score;

            // ii. Let bestFormat be format.
            best_format = format;
        }
    }

    if (!best_format.has_value())
        return {};

    // Non-standard, if the user provided options that differ from the best format's options, keep
    // the user's options. This is expected by TR-35:
    //
    //     It is not necessary to supply dateFormatItems with skeletons for every field length; fields
    //     in the skeleton and pattern are expected to be expanded in parallel to handle a request.
    //     https://unicode.org/reports/tr35/tr35-dates.html#Matching_Skeletons
    //
    // Rather than generating an prohibitively large amount of nearly-duplicate patterns, which only
    // differ by field length, we expand the field lengths here.
    best_format->for_each_calendar_field_zipped_with(options, [&](auto& best_format_field, auto const& option_field, auto field_type) {
        switch (field_type) {
        case Unicode::CalendarPattern::Field::FractionalSecondDigits:
            if (best_format->second.has_value() && option_field.has_value())
                best_format_field = option_field;
            break;

        case Unicode::CalendarPattern::Field::Hour:
        case Unicode::CalendarPattern::Field::Minute:
        case Unicode::CalendarPattern::Field::Second:
            break;

        default:
            if (best_format_field.has_value() && option_field.has_value())
                best_format_field = option_field;
            break;
        }
    });

    // 11. Return bestFormat.
    return best_format;
}

// 11.1.5 BestFitFormatMatcher ( options, formats ), https://tc39.es/ecma402/#sec-bestfitformatmatcher
Optional<Unicode::CalendarPattern> best_fit_format_matcher(Unicode::CalendarPattern const& options, Vector<Unicode::CalendarPattern> formats)
{
    // When the BestFitFormatMatcher abstract operation is called with two arguments options and formats, it performs
    // implementation dependent steps, which should return a set of component representations that a typical user of
    // the selected locale would perceive as at least as good as the one returned by BasicFormatMatcher.
    return basic_format_matcher(options, move(formats));
}

}
