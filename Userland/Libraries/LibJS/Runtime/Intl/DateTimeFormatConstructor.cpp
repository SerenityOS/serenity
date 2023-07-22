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

ThrowCompletionOr<void> DateTimeFormatConstructor::initialize(Realm& realm)
{
    MUST_OR_THROW_OOM(NativeFunction::initialize(realm));

    auto& vm = this->vm();

    // 11.2.1 Intl.DateTimeFormat.prototype, https://tc39.es/ecma402/#sec-intl.datetimeformat.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().intl_date_time_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    return {};
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

    // 3. If required is not "any" or defaults is not "date", then
    if (required != OptionRequired::Any || defaults != OptionDefaults::Date) {
        // a. Set options to ? ToDateTimeOptions(options, required, defaults).
        options_value = TRY(to_date_time_options(vm, options_value, required, defaults));
    }

    // 4. Set options to ? ToDateTimeOptions(options, "any", "date").
    auto* options = TRY(to_date_time_options(vm, options_value, OptionRequired::Any, OptionDefaults::Date));

    // 5. Let opt be a new Record.
    LocaleOptions opt {};

    // 6. Let matcher be ? GetOption(options, "localeMatcher", string, « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(vm, *options, vm.names.localeMatcher, OptionType::String, AK::Array { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 7. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 8. Let calendar be ? GetOption(options, "calendar", string, empty, undefined).
    auto calendar = TRY(get_option(vm, *options, vm.names.calendar, OptionType::String, {}, Empty {}));

    // 9. If calendar is not undefined, then
    if (!calendar.is_undefined()) {
        // a. If calendar cannot be matched by the type Unicode locale nonterminal, throw a RangeError exception.
        if (!::Locale::is_type_identifier(TRY(calendar.as_string().utf8_string_view())))
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, calendar, "calendar"sv);

        // 10. Set opt.[[ca]] to calendar.
        opt.ca = TRY(calendar.as_string().utf8_string());
    }

    // 11. Let numberingSystem be ? GetOption(options, "numberingSystem", string, empty, undefined).
    auto numbering_system = TRY(get_option(vm, *options, vm.names.numberingSystem, OptionType::String, {}, Empty {}));

    // 12. If numberingSystem is not undefined, then
    if (!numbering_system.is_undefined()) {
        // a. If numberingSystem cannot be matched by the type Unicode locale nonterminal, throw a RangeError exception.
        if (!::Locale::is_type_identifier(TRY(numbering_system.as_string().utf8_string_view())))
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, numbering_system, "numberingSystem"sv);

        // 13. Set opt.[[nu]] to numberingSystem.
        opt.nu = TRY(numbering_system.as_string().utf8_string());
    }

    // 14. Let hour12 be ? GetOption(options, "hour12", boolean, empty, undefined).
    auto hour12 = TRY(get_option(vm, *options, vm.names.hour12, OptionType::Boolean, {}, Empty {}));

    // 15. Let hourCycle be ? GetOption(options, "hourCycle", string, « "h11", "h12", "h23", "h24" », undefined).
    auto hour_cycle = TRY(get_option(vm, *options, vm.names.hourCycle, OptionType::String, AK::Array { "h11"sv, "h12"sv, "h23"sv, "h24"sv }, Empty {}));

    // 16. If hour12 is not undefined, then
    if (!hour12.is_undefined()) {
        // a. Set hourCycle to null.
        hour_cycle = js_null();
    }

    // 17. Set opt.[[hc]] to hourCycle.
    if (!hour_cycle.is_nullish())
        opt.hc = TRY(hour_cycle.as_string().utf8_string());

    // 18. Let localeData be %DateTimeFormat%.[[LocaleData]].
    // 19. Let r be ResolveLocale(%DateTimeFormat%.[[AvailableLocales]], requestedLocales, opt, %DateTimeFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = MUST_OR_THROW_OOM(resolve_locale(vm, requested_locales, opt, DateTimeFormat::relevant_extension_keys()));

    // 20. Set dateTimeFormat.[[Locale]] to r.[[locale]].
    date_time_format->set_locale(move(result.locale));

    // 21. Let resolvedCalendar be r.[[ca]].
    // 22. Set dateTimeFormat.[[Calendar]] to resolvedCalendar.
    if (result.ca.has_value())
        date_time_format->set_calendar(result.ca.release_value());

    // 23. Set dateTimeFormat.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        date_time_format->set_numbering_system(result.nu.release_value());

    // 24. Let dataLocale be r.[[dataLocale]].
    auto data_locale = move(result.data_locale);

    // Non-standard, the data locale is needed for LibUnicode lookups while formatting.
    date_time_format->set_data_locale(data_locale);

    // 25. Let dataLocaleData be localeData.[[<dataLocale>]].
    // 26. Let hcDefault be dataLocaleData.[[hourCycle]].
    auto default_hour_cycle = TRY_OR_THROW_OOM(vm, ::Locale::get_default_regional_hour_cycle(data_locale));

    // Non-standard, default_hour_cycle will be empty if Unicode data generation is disabled.
    if (!default_hour_cycle.has_value()) {
        date_time_format->set_time_zone(TRY_OR_THROW_OOM(vm, String::from_utf8(default_time_zone())));
        return date_time_format;
    }

    Optional<::Locale::HourCycle> hour_cycle_value;

    // 27. If hour12 is true, then
    if (hour12.is_boolean() && hour12.as_bool()) {
        // a. If hcDefault is "h11" or "h23", let hc be "h11". Otherwise, let hc be "h12".
        if ((default_hour_cycle == ::Locale::HourCycle::H11) || (default_hour_cycle == ::Locale::HourCycle::H23))
            hour_cycle_value = ::Locale::HourCycle::H11;
        else
            hour_cycle_value = ::Locale::HourCycle::H12;
    }
    // 28. Else if hour12 is false, then
    else if (hour12.is_boolean() && !hour12.as_bool()) {
        // a. If hcDefault is "h11" or "h23", let hc be "h23". Otherwise, let hc be "h24".
        if ((default_hour_cycle == ::Locale::HourCycle::H11) || (default_hour_cycle == ::Locale::HourCycle::H23))
            hour_cycle_value = ::Locale::HourCycle::H23;
        else
            hour_cycle_value = ::Locale::HourCycle::H24;
    }
    // 29. Else,
    else {
        // a. Assert: hour12 is undefined.
        VERIFY(hour12.is_undefined());

        // b. Let hc be r.[[hc]].
        if (result.hc.has_value())
            hour_cycle_value = ::Locale::hour_cycle_from_string(*result.hc);

        // c. If hc is null, set hc to hcDefault.
        if (!hour_cycle_value.has_value())
            hour_cycle_value = default_hour_cycle;
    }

    // 30. Set dateTimeFormat.[[HourCycle]] to hc.
    if (hour_cycle_value.has_value())
        date_time_format->set_hour_cycle(*hour_cycle_value);

    // 31. Let timeZone be ? Get(options, "timeZone").
    auto time_zone_value = TRY(options->get(vm.names.timeZone));
    String time_zone;

    // 32. If timeZone is undefined, then
    if (time_zone_value.is_undefined()) {
        // a. Set timeZone to DefaultTimeZone().
        time_zone = TRY_OR_THROW_OOM(vm, String::from_utf8(default_time_zone()));
    }
    // 33. Else,
    else {
        // a. Set timeZone to ? ToString(timeZone).
        time_zone = TRY(time_zone_value.to_string(vm));

        // b. If IsAvailableTimeZoneName(timeZone) is false, then
        if (!Temporal::is_available_time_zone_name(time_zone)) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(ErrorType::OptionIsNotValidValue, time_zone, vm.names.timeZone);
        }

        // c. Set timeZone to CanonicalizeTimeZoneName(timeZone).
        time_zone = MUST_OR_THROW_OOM(Temporal::canonicalize_time_zone_name(vm, time_zone));
    }

    // 34. Set dateTimeFormat.[[TimeZone]] to timeZone.
    date_time_format->set_time_zone(move(time_zone));

    // 35. Let formatOptions be a new Record.
    ::Locale::CalendarPattern format_options {};

    // 36. Set formatOptions.[[hourCycle]] to hc.
    format_options.hour_cycle = hour_cycle_value;

    // 37. Let hasExplicitFormatComponents be false.
    // NOTE: Instead of using a boolean, we track any explicitly provided component name for nicer exception messages.
    PropertyKey const* explicit_format_component = nullptr;

    // 38. For each row of Table 6, except the header row, in table order, do
    TRY(for_each_calendar_field(vm, format_options, [&](auto& option, auto const& property, auto const& values) -> ThrowCompletionOr<void> {
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
                option = ::Locale::calendar_pattern_style_from_string(TRY(value.as_string().utf8_string_view()));

                // e. If value is not undefined, then
                //     i. Set hasExplicitFormatComponents to true.
                explicit_format_component = &property;
            }
        }

        return {};
    }));

    // 39. Let matcher be ? GetOption(options, "formatMatcher", string, « "basic", "best fit" », "best fit").
    matcher = TRY(get_option(vm, *options, vm.names.formatMatcher, OptionType::String, AK::Array { "basic"sv, "best fit"sv }, "best fit"sv));

    // 40. Let dateStyle be ? GetOption(options, "dateStyle", string, « "full", "long", "medium", "short" », undefined).
    auto date_style = TRY(get_option(vm, *options, vm.names.dateStyle, OptionType::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 41. Set dateTimeFormat.[[DateStyle]] to dateStyle.
    if (!date_style.is_undefined())
        date_time_format->set_date_style(TRY(date_style.as_string().utf8_string_view()));

    // 42. Let timeStyle be ? GetOption(options, "timeStyle", string, « "full", "long", "medium", "short" », undefined).
    auto time_style = TRY(get_option(vm, *options, vm.names.timeStyle, OptionType::String, AK::Array { "full"sv, "long"sv, "medium"sv, "short"sv }, Empty {}));

    // 43. Set dateTimeFormat.[[TimeStyle]] to timeStyle.
    if (!time_style.is_undefined())
        date_time_format->set_time_style(TRY(time_style.as_string().utf8_string_view()));

    Optional<::Locale::CalendarPattern> best_format {};

    // 44. If dateStyle is not undefined or timeStyle is not undefined, then
    if (date_time_format->has_date_style() || date_time_format->has_time_style()) {
        // a. If hasExplicitFormatComponents is true, then
        if (explicit_format_component != nullptr) {
            // i. Throw a TypeError exception.
            return vm.throw_completion<TypeError>(ErrorType::IntlInvalidDateTimeFormatOption, *explicit_format_component, "dateStyle or timeStyle"sv);
        }

        // b. Let styles be dataLocaleData.[[styles]].[[<resolvedCalendar>]].
        // c. Let bestFormat be DateTimeStyleFormat(dateStyle, timeStyle, styles).
        best_format = MUST_OR_THROW_OOM(date_time_style_format(vm, data_locale, date_time_format));
    }
    // 45. Else,
    else {
        // a. Let formats be dataLocaleData.[[formats]].[[<resolvedCalendar>]].
        auto formats = TRY_OR_THROW_OOM(vm, ::Locale::get_calendar_available_formats(data_locale, date_time_format->calendar()));

        // b. If matcher is "basic", then
        if (TRY(matcher.as_string().utf8_string_view()) == "basic"sv) {
            // i. Let bestFormat be BasicFormatMatcher(formatOptions, formats).
            best_format = basic_format_matcher(format_options, move(formats));
        }
        // c. Else,
        else {
            // i. Let bestFormat be BestFitFormatMatcher(formatOptions, formats).
            best_format = best_fit_format_matcher(format_options, move(formats));
        }
    }

    // 46. For each row in Table 6, except the header row, in table order, do
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

    // 47. If dateTimeFormat.[[Hour]] is undefined, then
    if (!date_time_format->has_hour()) {
        // a. Set dateTimeFormat.[[HourCycle]] to undefined.
        date_time_format->clear_hour_cycle();
    }

    // 48. If dateTimeFormat.[[HourCycle]] is "h11" or "h12", then
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
        range_patterns = TRY_OR_THROW_OOM(vm, ::Locale::get_calendar_range12_formats(data_locale, date_time_format->calendar(), best_format->skeleton));
    }
    // 49. Else,
    else {
        // a. Let pattern be bestFormat.[[pattern]].
        pattern = move(best_format->pattern);

        // b. Let rangePatterns be bestFormat.[[rangePatterns]].
        range_patterns = TRY_OR_THROW_OOM(vm, ::Locale::get_calendar_range_formats(data_locale, date_time_format->calendar(), best_format->skeleton));
    }

    // 50. Set dateTimeFormat.[[Pattern]] to pattern.
    date_time_format->set_pattern(move(pattern));

    // 51. Set dateTimeFormat.[[RangePatterns]] to rangePatterns.
    date_time_format->set_range_patterns(move(range_patterns));

    // 52. Return dateTimeFormat.
    return date_time_format;
}

}
