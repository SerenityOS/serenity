/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 11.1 The Intl.DateTimeFormat Constructor, https://tc39.es/ecma402/#sec-intl-datetimeformat-constructor
DateTimeFormatConstructor::DateTimeFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.DateTimeFormat.as_string(), *global_object.function_prototype())
{
}

void DateTimeFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 11.2.1 Intl.DateTimeFormat.prototype, https://tc39.es/ecma402/#sec-intl.datetimeformat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_date_time_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 11.1.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<Value> DateTimeFormatConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object, else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 11.1.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<Object*> DateTimeFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let dateTimeFormat be ? OrdinaryCreateFromConstructor(newTarget, "%DateTimeFormat.prototype%", « [[InitializedDateTimeFormat]], [[Locale]], [[Calendar]], [[NumberingSystem]], [[TimeZone]], [[Weekday]], [[Era]], [[Year]], [[Month]], [[Day]], [[DayPeriod]], [[Hour]], [[Minute]], [[Second]], [[FractionalSecondDigits]], [[TimeZoneName]], [[HourCycle]], [[Pattern]], [[BoundFormat]] »).
    auto* date_time_format = TRY(ordinary_create_from_constructor<DateTimeFormat>(global_object, new_target, &GlobalObject::intl_date_time_format_prototype));

    // 3. Perform ? InitializeDateTimeFormat(dateTimeFormat, locales, options).
    TRY(initialize_date_time_format(global_object, *date_time_format, locales, options));

    // 4. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Let this be the this value.
    //     b. Return ? ChainDateTimeFormat(dateTimeFormat, NewTarget, this).

    // 5. Return dateTimeFormat.
    return date_time_format;
}

// 11.2.2 Intl.DateTimeFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(DateTimeFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %DateTimeFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

// 11.1.2 InitializeDateTimeFormat ( dateTimeFormat, locales, options ), https://tc39.es/ecma402/#sec-initializedatetimeformat
ThrowCompletionOr<DateTimeFormat*> initialize_date_time_format(GlobalObject& global_object, DateTimeFormat& date_time_format, Value locales_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales_value));

    // 2. Set options to ? ToDateTimeOptions(options, "any", "date").
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
        // a. Set hourCycle to null.
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

    // 19. Set resolvedCalendar to r.[[ca]].
    // 20. Set dateTimeFormat.[[Calendar]] to resolvedCalendar.
    if (result.ca.has_value())
        date_time_format.set_calendar(result.ca.release_value());

    // 21. Set dateTimeFormat.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        date_time_format.set_numbering_system(result.nu.release_value());

    // 22. Let dataLocale be r.[[dataLocale]].
    auto data_locale = move(result.data_locale);

    // Non-standard, the data locale is needed for LibUnicode lookups while formatting.
    date_time_format.set_data_locale(data_locale);

    // 23. Let dataLocaleData be localeData.[[<dataLocale>]].
    // 24. Let hcDefault be dataLocaleData.[[hourCycle]].
    auto default_hour_cycle = Unicode::get_default_regional_hour_cycle(data_locale);

    // Non-standard, default_hour_cycle will be empty if Unicode data generation is disabled.
    if (!default_hour_cycle.has_value())
        return &date_time_format;

    Optional<Unicode::HourCycle> hour_cycle_value;

    // 25. If hour12 is true, then
    if (hour12.is_boolean() && hour12.as_bool()) {
        // a. If hcDefault is "h11" or "h23", let hc be "h11". Otherwise, let hc be "h12".
        if ((default_hour_cycle == Unicode::HourCycle::H11) || (default_hour_cycle == Unicode::HourCycle::H23))
            hour_cycle_value = Unicode::HourCycle::H11;
        else
            hour_cycle_value = Unicode::HourCycle::H12;

    }
    // 26. Else if hour12 is false, then
    else if (hour12.is_boolean() && !hour12.as_bool()) {
        // a. If hcDefault is "h11" or "h23", let hc be "h23". Otherwise, let hc be "h24".
        if ((default_hour_cycle == Unicode::HourCycle::H11) || (default_hour_cycle == Unicode::HourCycle::H23))
            hour_cycle_value = Unicode::HourCycle::H23;
        else
            hour_cycle_value = Unicode::HourCycle::H24;

    }
    // 27. Else,
    else {
        // a. Assert: hour12 is undefined.
        VERIFY(hour12.is_undefined());

        // b. Let hc be r.[[hc]].
        if (result.hc.has_value())
            hour_cycle_value = Unicode::hour_cycle_from_string(*result.hc);

        // c. If hc is null, set hc to hcDefault.
        if (!hour_cycle_value.has_value())
            hour_cycle_value = default_hour_cycle;
    }

    // 28. Set dateTimeFormat.[[HourCycle]] to hc.
    if (hour_cycle_value.has_value())
        date_time_format.set_hour_cycle(*hour_cycle_value);

    // 29. Let timeZone be ? Get(options, "timeZone").
    auto time_zone_value = TRY(options->get(vm.names.timeZone));
    String time_zone;

    // 30. If timeZone is undefined, then
    if (time_zone_value.is_undefined()) {
        // a. Set timeZone to ! DefaultTimeZone().
        time_zone = Temporal::default_time_zone();
    }
    // 31. Else,
    else {
        // a. Set timeZone to ? ToString(timeZone).
        time_zone = TRY(time_zone_value.to_string(global_object));

        // b. If the result of IsValidTimeZoneName(timeZone) is false, then
        if (!Temporal::is_valid_time_zone_name(time_zone)) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, time_zone, vm.names.timeZone);
        }

        // c. Set timeZone to ! CanonicalizeTimeZoneName(timeZone).
        time_zone = Temporal::canonicalize_time_zone_name(time_zone);
    }

    // 32. Set dateTimeFormat.[[TimeZone]] to timeZone.
    date_time_format.set_time_zone(move(time_zone));

    // 33. Let formatOptions be a new Record.
    Unicode::CalendarPattern format_options {};

    // 34. Set formatOptions.[[hourCycle]] to hc.
    format_options.hour_cycle = hour_cycle_value;

    // 35. Let hasExplicitFormatComponents be false.
    // NOTE: Instead of using a boolean, we track any explicitly provided component name for nicer exception messages.
    PropertyKey const* explicit_format_component = nullptr;

    // 36. For each row of Table 6, except the header row, in table order, do
    TRY(for_each_calendar_field(global_object, format_options, [&](auto& option, auto const& property, auto const& values) -> ThrowCompletionOr<void> {
        using ValueType = typename RemoveReference<decltype(option)>::ValueType;

        // a. Let prop be the name given in the Property column of the row.

        // b. If prop is "fractionalSecondDigits", then
        if constexpr (IsIntegral<ValueType>) {
            // i. Let value be ? GetNumberOption(options, "fractionalSecondDigits", 1, 3, undefined).
            auto value = TRY(get_number_option(global_object, *options, property, 1, 3, {}));

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
            // ii. Let value be ? GetOption(options, prop, "string", values, undefined).
            auto value = TRY(get_option(global_object, *options, property, Value::Type::String, values, Empty {}));

            // d. Set formatOptions.[[<prop>]] to value.
            if (!value.is_undefined()) {
                option = Unicode::calendar_pattern_style_from_string(value.as_string().string());

                // e. If value is not undefined, then
                //     i. Set hasExplicitFormatComponents to true.
                explicit_format_component = &property;
            }
        }

        return {};
    }));

    // 37. Let matcher be ? GetOption(options, "formatMatcher", "string", « "basic", "best fit" », "best fit").
    matcher = TRY(get_option(global_object, *options, vm.names.formatMatcher, Value::Type::String, AK::Array { "basic"sv, "best fit"sv }, "best fit"sv));

    // 38. Let dateStyle be ? GetOption(options, "dateStyle", "string", « "full", "long", "medium", "short" », undefined).
    auto date_style = TRY(get_option(global_object, *options, vm.names.dateStyle, Value::Type::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 39. Set dateTimeFormat.[[DateStyle]] to dateStyle.
    if (!date_style.is_undefined())
        date_time_format.set_date_style(date_style.as_string().string());

    // 40. Let timeStyle be ? GetOption(options, "timeStyle", "string", « "full", "long", "medium", "short" », undefined).
    auto time_style = TRY(get_option(global_object, *options, vm.names.timeStyle, Value::Type::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 41. Set dateTimeFormat.[[TimeStyle]] to timeStyle.
    if (!time_style.is_undefined())
        date_time_format.set_time_style(time_style.as_string().string());

    Optional<Unicode::CalendarPattern> best_format {};

    // 42. If dateStyle is not undefined or timeStyle is not undefined, then
    if (date_time_format.has_date_style() || date_time_format.has_time_style()) {
        // a. If hasExplicitFormatComponents is true, then
        if (explicit_format_component != nullptr) {
            // i. Throw a TypeError exception.
            return vm.throw_completion<TypeError>(global_object, ErrorType::IntlInvalidDateTimeFormatOption, *explicit_format_component, "dateStyle or timeStyle"sv);
        }

        // b. Let styles be dataLocaleData.[[styles]].[[<resolvedCalendar>]].
        // c. Let bestFormat be DateTimeStyleFormat(dateStyle, timeStyle, styles).
        best_format = date_time_style_format(data_locale, date_time_format);
    }
    // 43. Else,
    else {
        // a. Let formats be dataLocaleData.[[formats]].[[<resolvedCalendar>]].
        auto formats = Unicode::get_calendar_available_formats(data_locale, date_time_format.calendar());

        // b. If matcher is "basic", then
        if (matcher.as_string().string() == "basic"sv) {
            // i. Let bestFormat be BasicFormatMatcher(formatOptions, formats).
            best_format = basic_format_matcher(format_options, move(formats));
        }
        // c. Else,
        else {
            // i. Let bestFormat be BestFitFormatMatcher(formatOptions, formats).
            best_format = best_fit_format_matcher(format_options, move(formats));
        }
    }

    // 44. For each row in Table 6, except the header row, in table order, do
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
    Vector<Unicode::CalendarRangePattern> range_patterns;

    // 45. If dateTimeFormat.[[Hour]] is undefined, then
    if (!date_time_format.has_hour()) {
        // a. Set dateTimeFormat.[[HourCycle]] to undefined.
        date_time_format.clear_hour_cycle();
    }

    // 46. If dateTimeformat.[[HourCycle]] is "h11" or "h12", then
    if ((hour_cycle_value == Unicode::HourCycle::H11) || (hour_cycle_value == Unicode::HourCycle::H12)) {
        // a. Let pattern be bestFormat.[[pattern12]].
        if (best_format->pattern12.has_value()) {
            pattern = best_format->pattern12.release_value();
        } else {
            // Non-standard, LibUnicode only provides [[pattern12]] when [[pattern]] has a day
            // period. Other implementations provide [[pattern12]] as a copy of [[pattern]].
            pattern = move(best_format->pattern);
        }

        // b. Let rangePatterns be bestFormat.[[rangePatterns12]].
        range_patterns = Unicode::get_calendar_range12_formats(data_locale, date_time_format.calendar(), best_format->skeleton);
    }
    // 47. Else,
    else {
        // a. Let pattern be bestFormat.[[pattern]].
        pattern = move(best_format->pattern);

        // b. Let rangePatterns be bestFormat.[[rangePatterns]].
        range_patterns = Unicode::get_calendar_range_formats(data_locale, date_time_format.calendar(), best_format->skeleton);
    }

    // 48. Set dateTimeFormat.[[Pattern]] to pattern.
    date_time_format.set_pattern(move(pattern));

    // 49. Set dateTimeFormat.[[RangePatterns]] to rangePatterns.
    date_time_format.set_range_patterns(move(range_patterns));

    // 50. Return dateTimeFormat.
    return &date_time_format;
}

}
