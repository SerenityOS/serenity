/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IterationDecision.h>
#include <AK/NumericLimits.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Utf16String.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>
#include <math.h>

namespace JS::Intl {

// 11 DateTimeFormat Objects, https://tc39.es/ecma402/#datetimeformat-objects
DateTimeFormat::DateTimeFormat(Object& prototype)
    : Object(prototype)
{
}

void DateTimeFormat::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_bound_format)
        visitor.visit(m_bound_format);
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

// 11.5.1 ToDateTimeOptions ( options, required, defaults ), https://tc39.es/ecma402/#sec-todatetimeoptions
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

// 11.5.2 DateTimeStyleFormat ( dateStyle, timeStyle, styles ), https://tc39.es/ecma402/#sec-date-time-style-format
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

        // NOTE: Our implementation of steps h-j differ from the spec. LibUnicode does not attach range patterns to the
        //       format pattern; rather, lookups for range patterns are performed separately based on the format pattern's
        //       skeleton. So we form a new skeleton here and defer the range pattern lookups.
        format.skeleton = Unicode::combine_skeletons(date_format.skeleton, time_format.skeleton);

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

// 11.5.3 BasicFormatMatcher ( options, formats ), https://tc39.es/ecma402/#sec-basicformatmatcher
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

    // 7. Let offsetPenalty be 1.
    constexpr int offset_penalty = 1;

    // 8. Let bestScore be -Infinity.
    int best_score = NumericLimits<int>::min();

    // 9. Let bestFormat be undefined.
    Optional<Unicode::CalendarPattern> best_format;

    // 10. Assert: Type(formats) is List.
    // 11. For each element format of formats, do
    for (auto& format : formats) {
        // a. Let score be 0.
        int score = 0;

        // b. For each property name property shown in Table 6, do
        format.for_each_calendar_field_zipped_with(options, [&](auto const& format_prop, auto const& options_prop, auto type) {
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
            // v. Else if property is "timeZoneName", then
            else if (type == Unicode::CalendarPattern::Field::TimeZoneName) {
                // This is needed to avoid a compile error. Although we only enter this branch for TimeZoneName,
                // the lambda we are in will be generated with property types other than CalendarPatternStyle.
                auto compare_prop = [](auto prop, auto test) { return prop == static_cast<ValueType>(test); };

                // 1. If optionsProp is "short" or "shortGeneric", then
                if (compare_prop(options_prop, Unicode::CalendarPatternStyle::Short) || compare_prop(options_prop, Unicode::CalendarPatternStyle::ShortGeneric)) {
                    // a. If formatProp is "shortOffset", decrease score by offsetPenalty.
                    if (compare_prop(format_prop, Unicode::CalendarPatternStyle::ShortOffset))
                        score -= offset_penalty;
                    // b. Else if formatProp is "longOffset", decrease score by (offsetPenalty + shortMorePenalty).
                    else if (compare_prop(format_prop, Unicode::CalendarPatternStyle::LongOffset))
                        score -= offset_penalty + short_more_penalty;
                    // c. Else if optionsProp is "short" and formatProp is "long", decrease score by shortMorePenalty.
                    else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::Short) || compare_prop(format_prop, Unicode::CalendarPatternStyle::Long))
                        score -= short_more_penalty;
                    // d. Else if optionsProp is "shortGeneric" and formatProp is "longGeneric", decrease score by shortMorePenalty.
                    else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::ShortGeneric) || compare_prop(format_prop, Unicode::CalendarPatternStyle::LongGeneric))
                        score -= short_more_penalty;
                    // e. Else if optionsProp ≠ formatProp, decrease score by removalPenalty.
                    else if (options_prop != format_prop)
                        score -= removal_penalty;
                }
                // 2. Else if optionsProp is "shortOffset" and formatProp is "longOffset", decrease score by shortMorePenalty.
                else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::ShortOffset) || compare_prop(format_prop, Unicode::CalendarPatternStyle::LongOffset)) {
                    score -= short_more_penalty;
                }
                // 3. Else if optionsProp is "long" or "longGeneric", then
                else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::Long) || compare_prop(options_prop, Unicode::CalendarPatternStyle::LongGeneric)) {
                    // a. If formatProp is "longOffset", decrease score by offsetPenalty.
                    if (compare_prop(format_prop, Unicode::CalendarPatternStyle::LongOffset))
                        score -= offset_penalty;
                    // b. Else if formatProp is "shortOffset", decrease score by (offsetPenalty + longLessPenalty).
                    else if (compare_prop(format_prop, Unicode::CalendarPatternStyle::ShortOffset))
                        score -= offset_penalty + long_less_penalty;
                    // c. Else if optionsProp is "long" and formatProp is "short", decrease score by longLessPenalty.
                    else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::Long) || compare_prop(format_prop, Unicode::CalendarPatternStyle::Short))
                        score -= long_less_penalty;
                    // d. Else if optionsProp is "longGeneric" and formatProp is "shortGeneric", decrease score by longLessPenalty.
                    else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::LongGeneric) || compare_prop(format_prop, Unicode::CalendarPatternStyle::ShortGeneric))
                        score -= long_less_penalty;
                    // e. Else if optionsProp ≠ formatProp, decrease score by removalPenalty.
                    else if (options_prop != format_prop)
                        score -= removal_penalty;
                }
                // 4. Else if optionsProp is "longOffset" and formatProp is "shortOffset", decrease score by longLessPenalty.
                else if (compare_prop(options_prop, Unicode::CalendarPatternStyle::LongOffset) || compare_prop(format_prop, Unicode::CalendarPatternStyle::ShortOffset)) {
                    score -= long_less_penalty;
                }
                // 5. Else if optionsProp ≠ formatProp, decrease score by removalPenalty.
                else if (options_prop != format_prop) {
                    score -= removal_penalty;
                }
            }
            // vi. Else if optionsProp ≠ formatProp, then
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

    // 12. Return bestFormat.
    return best_format;
}

// 11.5.4 BestFitFormatMatcher ( options, formats ), https://tc39.es/ecma402/#sec-bestfitformatmatcher
Optional<Unicode::CalendarPattern> best_fit_format_matcher(Unicode::CalendarPattern const& options, Vector<Unicode::CalendarPattern> formats)
{
    // When the BestFitFormatMatcher abstract operation is called with two arguments options and formats, it performs
    // implementation dependent steps, which should return a set of component representations that a typical user of
    // the selected locale would perceive as at least as good as the one returned by BasicFormatMatcher.
    return basic_format_matcher(options, move(formats));
}

struct StyleAndValue {
    StringView name {};
    Unicode::CalendarPatternStyle style {};
    i32 value { 0 };
};

static Optional<StyleAndValue> find_calendar_field(StringView name, Unicode::CalendarPattern const& options, Unicode::CalendarPattern const* range_options, LocalTime const& local_time)
{
    auto make_style_and_value = [](auto name, auto style, auto fallback_style, auto value) {
        if (style.has_value())
            return StyleAndValue { name, *style, static_cast<i32>(value) };
        return StyleAndValue { name, fallback_style, static_cast<i32>(value) };
    };

    constexpr auto weekday = "weekday"sv;
    constexpr auto era = "era"sv;
    constexpr auto year = "year"sv;
    constexpr auto month = "month"sv;
    constexpr auto day = "day"sv;
    constexpr auto hour = "hour"sv;
    constexpr auto minute = "minute"sv;
    constexpr auto second = "second"sv;

    Optional<Unicode::CalendarPatternStyle> empty;

    if (name == weekday)
        return make_style_and_value(weekday, range_options ? range_options->weekday : empty, *options.weekday, local_time.weekday);
    if (name == era)
        return make_style_and_value(era, range_options ? range_options->era : empty, *options.era, local_time.era);
    if (name == year)
        return make_style_and_value(year, range_options ? range_options->year : empty, *options.year, local_time.year);
    if (name == month)
        return make_style_and_value(month, range_options ? range_options->month : empty, *options.month, local_time.month);
    if (name == day)
        return make_style_and_value(day, range_options ? range_options->day : empty, *options.day, local_time.day);
    if (name == hour)
        return make_style_and_value(hour, range_options ? range_options->hour : empty, *options.hour, local_time.hour);
    if (name == minute)
        return make_style_and_value(minute, range_options ? range_options->minute : empty, *options.minute, local_time.minute);
    if (name == second)
        return make_style_and_value(second, range_options ? range_options->second : empty, *options.second, local_time.second);
    return {};
}

// 11.5.6 FormatDateTimePattern ( dateTimeFormat, patternParts, x, rangeFormatOptions ), https://tc39.es/ecma402/#sec-formatdatetimepattern
ThrowCompletionOr<Vector<PatternPartition>> format_date_time_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, Vector<PatternPartition> pattern_parts, double time, Unicode::CalendarPattern const* range_format_options)
{
    auto& vm = global_object.vm();

    // 1. Let x be TimeClip(x).
    time = time_clip(time);

    // 2. If x is NaN, throw a RangeError exception.
    if (isnan(time))
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidTime);

    // 3. Let locale be dateTimeFormat.[[Locale]].
    auto const& locale = date_time_format.locale();
    auto const& data_locale = date_time_format.data_locale();

    auto construct_number_format = [&](auto* options) -> ThrowCompletionOr<NumberFormat*> {
        auto* number_format = TRY(construct(global_object, *global_object.intl_number_format_constructor(), js_string(vm, locale), options));
        return static_cast<NumberFormat*>(number_format);
    };

    // 4. Let nfOptions be OrdinaryObjectCreate(null).
    auto* number_format_options = Object::create(global_object, nullptr);

    // 5. Perform ! CreateDataPropertyOrThrow(nfOptions, "useGrouping", false).
    MUST(number_format_options->create_data_property_or_throw(vm.names.useGrouping, Value(false)));

    // 6. Let nf be ? Construct(%NumberFormat%, « locale, nfOptions »).
    auto* number_format = TRY(construct_number_format(number_format_options));

    // 7. Let nf2Options be OrdinaryObjectCreate(null).
    auto* number_format_options2 = Object::create(global_object, nullptr);

    // 8. Perform ! CreateDataPropertyOrThrow(nf2Options, "minimumIntegerDigits", 2).
    MUST(number_format_options2->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(2)));

    // 9. Perform ! CreateDataPropertyOrThrow(nf2Options, "useGrouping", false).
    MUST(number_format_options2->create_data_property_or_throw(vm.names.useGrouping, Value(false)));

    // 10. Let nf2 be ? Construct(%NumberFormat%, « locale, nf2Options »).
    auto* number_format2 = TRY(construct_number_format(number_format_options2));

    // 11. Let fractionalSecondDigits be dateTimeFormat.[[FractionalSecondDigits]].
    Optional<u8> fractional_second_digits;
    NumberFormat* number_format3 = nullptr;

    // 12. If fractionalSecondDigits is not undefined, then
    if (date_time_format.has_fractional_second_digits()) {
        fractional_second_digits = date_time_format.fractional_second_digits();

        // a. Let nf3Options be OrdinaryObjectCreate(null).
        auto* number_format_options3 = Object::create(global_object, nullptr);

        // b. Perform ! CreateDataPropertyOrThrow(nf3Options, "minimumIntegerDigits", fractionalSecondDigits).
        MUST(number_format_options3->create_data_property_or_throw(vm.names.minimumIntegerDigits, Value(*fractional_second_digits)));

        // c. Perform ! CreateDataPropertyOrThrow(nf3Options, "useGrouping", false).
        MUST(number_format_options3->create_data_property_or_throw(vm.names.useGrouping, Value(false)));

        // d. Let nf3 be ? Construct(%NumberFormat%, « locale, nf3Options »).
        number_format3 = TRY(construct_number_format(number_format_options3));
    }

    // 13. Let tm be ToLocalTime(x, dateTimeFormat.[[Calendar]], dateTimeFormat.[[TimeZone]]).
    auto local_time = TRY(to_local_time(global_object, time, date_time_format.calendar(), date_time_format.time_zone()));

    // 14. Let result be a new empty List.
    Vector<PatternPartition> result;

    // 15. For each Record { [[Type]], [[Value]] } patternPart in patternParts, do
    for (auto& pattern_part : pattern_parts) {
        // a. Let p be patternPart.[[Type]].
        auto part = pattern_part.type;

        // b. If p is "literal", then
        if (part == "literal"sv) {
            // i. Append a new Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]] } as the last element of the list result.
            result.append({ "literal"sv, move(pattern_part.value) });
        }

        // c. Else if p is equal to "fractionalSecondDigits", then
        else if (part == "fractionalSecondDigits"sv) {
            // i. Let v be tm.[[Millisecond]].
            auto value = local_time.millisecond;

            // ii. Let v be floor(v × 10^(fractionalSecondDigits - 3)).
            value = floor(value * pow(10, static_cast<int>(*fractional_second_digits) - 3));

            // iii. Let fv be FormatNumeric(nf3, v).
            auto formatted_value = format_numeric(global_object, *number_format3, Value(value));

            // iv. Append a new Record { [[Type]]: "fractionalSecond", [[Value]]: fv } as the last element of result.
            result.append({ "fractionalSecond"sv, move(formatted_value) });
        }

        // d. Else if p is equal to "dayPeriod", then
        else if (part == "dayPeriod"sv) {
            String formatted_value;

            // i. Let f be the value of dateTimeFormat's internal slot whose name is the Internal Slot column of the matching row.
            auto style = date_time_format.day_period();

            // ii. Let fv be a String value representing the day period of tm in the form given by f; the String value depends upon the implementation and the effective locale of dateTimeFormat.
            auto symbol = Unicode::get_calendar_day_period_symbol_for_hour(data_locale, date_time_format.calendar(), style, local_time.hour);
            if (symbol.has_value())
                formatted_value = *symbol;

            // iii. Append a new Record { [[Type]]: p, [[Value]]: fv } as the last element of the list result.
            result.append({ "dayPeriod"sv, move(formatted_value) });
        }

        // e. Else if p is equal to "timeZoneName", then
        else if (part == "timeZoneName"sv) {
            // i. Let f be dateTimeFormat.[[TimeZoneName]].
            auto style = date_time_format.time_zone_name();

            // ii. Let v be dateTimeFormat.[[TimeZone]].
            auto const& value = date_time_format.time_zone();

            // iii. Let fv be a String value representing v in the form given by f; the String value depends upon the implementation and the effective locale of dateTimeFormat.
            //      The String value may also depend on the value of the [[InDST]] field of tm if f is "short", "long", "shortOffset", or "longOffset".
            //      If the implementation does not have a localized representation of f, then use the String value of v itself.
            auto formatted_value = Unicode::format_time_zone(data_locale, value, style, local_time.time_since_epoch());

            // iv. Append a new Record { [[Type]]: p, [[Value]]: fv } as the last element of the list result.
            result.append({ "timeZoneName"sv, move(formatted_value) });
        }

        // f. Else if p matches a Property column of the row in Table 6, then
        else if (auto style_and_value = find_calendar_field(part, date_time_format, range_format_options, local_time); style_and_value.has_value()) {
            String formatted_value;

            // i. If rangeFormatOptions is not undefined, let f be the value of rangeFormatOptions's field whose name matches p.
            // ii. Else, let f be the value of dateTimeFormat's internal slot whose name is the Internal Slot column of the matching row.
            // NOTE: find_calendar_field handles resolving rangeFormatOptions and dateTimeFormat fields.
            auto style = style_and_value->style;

            // iii. Let v be the value of tm's field whose name is the Internal Slot column of the matching row.
            auto value = style_and_value->value;

            // iv. If p is "year" and v ≤ 0, let v be 1 - v.
            if ((part == "year"sv) && (value <= 0))
                value = 1 - value;

            // v. If p is "month", increase v by 1.
            if (part == "month"sv)
                ++value;

            if (part == "hour"sv) {
                auto hour_cycle = date_time_format.hour_cycle();

                // vi. If p is "hour" and dateTimeFormat.[[HourCycle]] is "h11" or "h12", then
                if ((hour_cycle == Unicode::HourCycle::H11) || (hour_cycle == Unicode::HourCycle::H12)) {
                    // 1. Let v be v modulo 12.
                    value = value % 12;

                    // 2. If v is 0 and dateTimeFormat.[[HourCycle]] is "h12", let v be 12.
                    if ((value == 0) && (hour_cycle == Unicode::HourCycle::H12))
                        value = 12;
                }

                // vii. If p is "hour" and dateTimeFormat.[[HourCycle]] is "h24", then
                if (hour_cycle == Unicode::HourCycle::H24) {
                    // 1. If v is 0, let v be 24.
                    if (value == 0)
                        value = 24;
                }
            }

            switch (style) {
            // viii. If f is "numeric", then
            case Unicode::CalendarPatternStyle::Numeric:
                // 1. Let fv be FormatNumeric(nf, v).
                formatted_value = format_numeric(global_object, *number_format, Value(value));
                break;

            // ix. Else if f is "2-digit", then
            case Unicode::CalendarPatternStyle::TwoDigit:
                // 1. Let fv be FormatNumeric(nf2, v).
                formatted_value = format_numeric(global_object, *number_format2, Value(value));

                // 2. If the "length" property of fv is greater than 2, let fv be the substring of fv containing the last two characters.
                // NOTE: The first length check here isn't enough, but lets us avoid UTF-16 transcoding when the formatted value is ASCII.
                if (formatted_value.length() > 2) {
                    Utf16String utf16_formatted_value { formatted_value };
                    if (utf16_formatted_value.length_in_code_units() > 2)
                        formatted_value = utf16_formatted_value.substring_view(utf16_formatted_value.length_in_code_units() - 2).to_utf8();
                }

                break;

            // x. Else if f is "narrow", "short", or "long", then let fv be a String value representing v in the form given by f; the String value depends upon the implementation and the effective locale and calendar of dateTimeFormat.
            //    If p is "month" and rangeFormatOptions is undefined, then the String value may also depend on whether dateTimeFormat.[[Day]] is undefined.
            //    If p is "month" and rangeFormatOptions is not undefined, then the String value may also depend on whether rangeFormatOptions.[[day]] is undefined.
            //    If p is "era" and rangeFormatOptions is undefined, then the String value may also depend on whether dateTimeFormat.[[Era]] is undefined.
            //    If p is "era" and rangeFormatOptions is not undefined, then the String value may also depend on whether rangeFormatOptions.[[era]] is undefined.
            //    If the implementation does not have a localized representation of f, then use the String value of v itself.
            case Unicode::CalendarPatternStyle::Narrow:
            case Unicode::CalendarPatternStyle::Short:
            case Unicode::CalendarPatternStyle::Long: {
                Optional<StringView> symbol;

                if (part == "era"sv)
                    symbol = Unicode::get_calendar_era_symbol(data_locale, date_time_format.calendar(), style, static_cast<Unicode::Era>(value));
                else if (part == "month"sv)
                    symbol = Unicode::get_calendar_month_symbol(data_locale, date_time_format.calendar(), style, static_cast<Unicode::Month>(value - 1));
                else if (part == "weekday"sv)
                    symbol = Unicode::get_calendar_weekday_symbol(data_locale, date_time_format.calendar(), style, static_cast<Unicode::Weekday>(value));

                formatted_value = symbol.value_or(String::number(value));
                break;
            }

            default:
                VERIFY_NOT_REACHED();
            }

            // xi. Append a new Record { [[Type]]: p, [[Value]]: fv } as the last element of the list result.
            result.append({ style_and_value->name, move(formatted_value) });
        }

        // g. Else if p is equal to "ampm", then
        else if (part == "ampm"sv) {
            String formatted_value;

            // i. Let v be tm.[[Hour]].
            auto value = local_time.hour;

            // ii. If v is greater than 11, then
            if (value > 11) {
                // 1. Let fv be an implementation and locale dependent String value representing "post meridiem".
                auto symbol = Unicode::get_calendar_day_period_symbol(data_locale, date_time_format.calendar(), Unicode::CalendarPatternStyle::Short, Unicode::DayPeriod::PM);
                formatted_value = symbol.value_or("PM"sv);
            }
            // iii. Else,
            else {
                // 1. Let fv be an implementation and locale dependent String value representing "ante meridiem".
                auto symbol = Unicode::get_calendar_day_period_symbol(data_locale, date_time_format.calendar(), Unicode::CalendarPatternStyle::Short, Unicode::DayPeriod::AM);
                formatted_value = symbol.value_or("AM"sv);
            }

            // iv. Append a new Record { [[Type]]: "dayPeriod", [[Value]]: fv } as the last element of the list result.
            result.append({ "dayPeriod"sv, move(formatted_value) });
        }

        // h. Else if p is equal to "relatedYear", then
        else if (part == "relatedYear"sv) {
            // i. Let v be tm.[[RelatedYear]].
            // ii. Let fv be FormatNumeric(nf, v).
            // iii. Append a new Record { [[Type]]: "relatedYear", [[Value]]: fv } as the last element of the list result.

            // FIXME: Implement this when relatedYear is supported.
        }

        // i. Else if p is equal to "yearName", then
        else if (part == "yearName"sv) {
            // i. Let v be tm.[[YearName]].
            // ii. Let fv be an implementation and locale dependent String value representing v.
            // iii. Append a new Record { [[Type]]: "yearName", [[Value]]: fv } as the last element of the list result.

            // FIXME: Implement this when yearName is supported.
        }

        // Non-standard, TR-35 requires the decimal separator before injected {fractionalSecondDigits} partitions
        // to adhere to the selected locale. This depends on other generated data, so it is deferred to here.
        else if (part == "decimal"sv) {
            auto decimal_symbol = Unicode::get_number_system_symbol(data_locale, date_time_format.numbering_system(), Unicode::NumericSymbol::Decimal).value_or("."sv);
            result.append({ "literal"sv, decimal_symbol });
        }

        // j. Else,
        else {
            // i. Let unknown be an implementation-, locale-, and numbering system-dependent String based on x and p.
            // ii. Append a new Record { [[Type]]: "unknown", [[Value]]: unknown } as the last element of result.

            // LibUnicode doesn't generate any "unknown" patterns.
            VERIFY_NOT_REACHED();
        }
    }

    // 16. Return result.
    return result;
}

// 11.5.7 PartitionDateTimePattern ( dateTimeFormat, x ), https://tc39.es/ecma402/#sec-partitiondatetimepattern
ThrowCompletionOr<Vector<PatternPartition>> partition_date_time_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, double time)
{
    // 1. Let patternParts be PartitionPattern(dateTimeFormat.[[Pattern]]).
    auto pattern_parts = partition_pattern(date_time_format.pattern());

    // 2. Let result be ? FormatDateTimePattern(dateTimeFormat, patternParts, x, undefined).
    auto result = TRY(format_date_time_pattern(global_object, date_time_format, move(pattern_parts), time, nullptr));

    // 3. Return result.
    return result;
}

// 11.5.8 FormatDateTime ( dateTimeFormat, x ), https://tc39.es/ecma402/#sec-formatdatetime
ThrowCompletionOr<String> format_date_time(GlobalObject& global_object, DateTimeFormat& date_time_format, double time)
{
    // 1. Let parts be ? PartitionDateTimePattern(dateTimeFormat, x).
    auto parts = TRY(partition_date_time_pattern(global_object, date_time_format, time));

    // 2. Let result be the empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(move(part.value));
    }

    // 4. Return result.
    return result.build();
}

// 11.5.9 FormatDateTimeToParts ( dateTimeFormat, x ), https://tc39.es/ecma402/#sec-formatdatetimetoparts
ThrowCompletionOr<Array*> format_date_time_to_parts(GlobalObject& global_object, DateTimeFormat& date_time_format, double time)
{
    auto& vm = global_object.vm();

    // 1. Let parts be ? PartitionDateTimePattern(dateTimeFormat, x).
    auto parts = TRY(partition_date_time_pattern(global_object, date_time_format, time));

    // 2. Let result be ! ArrayCreate(0).
    auto* result = MUST(Array::create(global_object, 0));

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%Object.prototype%).
        auto* object = Object::create(global_object, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, move(part.value))));

        // d. Perform ! CreateDataProperty(result, ! ToString(n), O).
        MUST(result->create_data_property_or_throw(n, object));

        // e. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

template<typename Callback>
void for_each_range_pattern_field(LocalTime const& time1, LocalTime const& time2, Callback&& callback)
{
    // Table 4: Range pattern fields, https://tc39.es/ecma402/#table-datetimeformat-rangepatternfields
    if (callback(static_cast<u8>(time1.era), static_cast<u8>(time2.era), Unicode::CalendarRangePattern::Field::Era) == IterationDecision::Break)
        return;
    if (callback(time1.year, time2.year, Unicode::CalendarRangePattern::Field::Year) == IterationDecision::Break)
        return;
    if (callback(time1.month, time2.month, Unicode::CalendarRangePattern::Field::Month) == IterationDecision::Break)
        return;
    if (callback(time1.day, time2.day, Unicode::CalendarRangePattern::Field::Day) == IterationDecision::Break)
        return;
    if (callback(time1.hour, time2.hour, Unicode::CalendarRangePattern::Field::AmPm) == IterationDecision::Break)
        return;
    if (callback(time1.hour, time2.hour, Unicode::CalendarRangePattern::Field::DayPeriod) == IterationDecision::Break)
        return;
    if (callback(time1.hour, time2.hour, Unicode::CalendarRangePattern::Field::Hour) == IterationDecision::Break)
        return;
    if (callback(time1.minute, time2.minute, Unicode::CalendarRangePattern::Field::Minute) == IterationDecision::Break)
        return;
    if (callback(time1.second, time2.second, Unicode::CalendarRangePattern::Field::Second) == IterationDecision::Break)
        return;
    if (callback(time1.millisecond, time2.millisecond, Unicode::CalendarRangePattern::Field::FractionalSecondDigits) == IterationDecision::Break)
        return;
}

template<typename Callback>
ThrowCompletionOr<void> for_each_range_pattern_with_source(Unicode::CalendarRangePattern& pattern, Callback&& callback)
{
    TRY(callback(pattern.start_range, "startRange"sv));
    TRY(callback(pattern.separator, "shared"sv));
    TRY(callback(pattern.end_range, "endRange"sv));
    return {};
}

// 11.5.10 PartitionDateTimeRangePattern ( dateTimeFormat, x, y ), https://tc39.es/ecma402/#sec-partitiondatetimerangepattern
ThrowCompletionOr<Vector<PatternPartitionWithSource>> partition_date_time_range_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, double start, double end)
{
    auto& vm = global_object.vm();

    // 1. Let x be TimeClip(x).
    start = time_clip(start);

    // 2. If x is NaN, throw a RangeError exception.
    if (isnan(start))
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidTime);

    // 3. Let y be TimeClip(y).
    end = time_clip(end);

    // 4. If y is NaN, throw a RangeError exception.
    if (isnan(end))
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlInvalidTime);

    // 5. If x is greater than y, throw a RangeError exception.
    if (start > end)
        return vm.throw_completion<RangeError>(global_object, ErrorType::IntlStartTimeAfterEndTime, start, end);

    // 6. Let tm1 be ToLocalTime(x, dateTimeFormat.[[Calendar]], dateTimeFormat.[[TimeZone]]).
    auto start_local_time = TRY(to_local_time(global_object, start, date_time_format.calendar(), date_time_format.time_zone()));

    // 7. Let tm2 be ToLocalTime(y, dateTimeFormat.[[Calendar]], dateTimeFormat.[[TimeZone]]).
    auto end_local_time = TRY(to_local_time(global_object, end, date_time_format.calendar(), date_time_format.time_zone()));

    // 8. Let rangePatterns be dateTimeFormat.[[RangePatterns]].
    auto range_patterns = date_time_format.range_patterns();

    // 9. Let rangePattern be undefined.
    Optional<Unicode::CalendarRangePattern> range_pattern;

    // 10. Let dateFieldsPracticallyEqual be true.
    bool date_fields_practically_equal = true;

    // 11. Let patternContainsLargerDateField be false.
    bool pattern_contains_larger_date_field = false;

    // 12. While dateFieldsPracticallyEqual is true and patternContainsLargerDateField is false, repeat for each row of Table 4 in order, except the header row:
    for_each_range_pattern_field(start_local_time, end_local_time, [&](auto start_value, auto end_value, auto field_name) {
        // a. Let fieldName be the name given in the Range Pattern Field column of the row.

        // b. If rangePatterns has a field [[<fieldName>]], let rp be rangePatterns.[[<fieldName>]]; else let rp be undefined.
        Optional<Unicode::CalendarRangePattern> pattern;
        for (auto const& range : range_patterns) {
            if (range.field == field_name) {
                pattern = range;
                break;
            }
        }

        // c. If rangePattern is not undefined and rp is undefined, then
        if (range_pattern.has_value() && !pattern.has_value()) {
            // i. Set patternContainsLargerDateField to true.
            pattern_contains_larger_date_field = true;
        }
        // d. Else,
        else {
            // i. Let rangePattern be rp.
            range_pattern = pattern;

            switch (field_name) {
            // ii. If fieldName is equal to [[AmPm]], then
            case Unicode::CalendarRangePattern::Field::AmPm: {
                // 1. Let v1 be tm1.[[Hour]].
                // 2. Let v2 be tm2.[[Hour]].

                // 3. If v1 is greater than 11 and v2 less or equal than 11, or v1 is less or equal than 11 and v2 is greater than 11, then
                if ((start_value > 11 && end_value <= 11) || (start_value <= 11 && end_value > 11)) {
                    // a. Set dateFieldsPracticallyEqual to false.
                    date_fields_practically_equal = false;
                }

                break;
            }
            // iii. Else if fieldName is equal to [[DayPeriod]], then
            case Unicode::CalendarRangePattern::Field::DayPeriod: {
                // 1. Let v1 be a String value representing the day period of tm1; the String value depends upon the implementation and the effective locale of dateTimeFormat.
                auto start_period = Unicode::get_calendar_day_period_symbol_for_hour(date_time_format.data_locale(), date_time_format.calendar(), Unicode::CalendarPatternStyle::Short, start_value);

                // 2. Let v2 be a String value representing the day period of tm2; the String value depends upon the implementation and the effective locale of dateTimeFormat.
                auto end_period = Unicode::get_calendar_day_period_symbol_for_hour(date_time_format.data_locale(), date_time_format.calendar(), Unicode::CalendarPatternStyle::Short, end_value);

                // 3. If v1 is not equal to v2, then
                if (start_period != end_period) {
                    // a. Set dateFieldsPracticallyEqual to false.
                    date_fields_practically_equal = false;
                }

                break;
            }
            // iv. Else if fieldName is equal to [[FractionalSecondDigits]], then
            case Unicode::CalendarRangePattern::Field::FractionalSecondDigits: {
                // 1. Let fractionalSecondDigits be dateTimeFormat.[[FractionalSecondDigits]].
                Optional<u8> fractional_second_digits;
                if (date_time_format.has_fractional_second_digits())
                    fractional_second_digits = date_time_format.fractional_second_digits();

                // 2. If fractionalSecondDigits is undefined, then
                if (!fractional_second_digits.has_value()) {
                    // a. Set fractionalSecondDigits to 3.
                    fractional_second_digits = 3;
                }

                // 3. Let v1 be tm1.[[Millisecond]].
                // 4. Let v2 be tm2.[[Millisecond]].

                // 5. Let v1 be floor(v1 × 10( fractionalSecondDigits - 3 )).
                start_value = floor(start_value * pow(10, static_cast<int>(*fractional_second_digits) - 3));

                // 6. Let v2 be floor(v2 × 10( fractionalSecondDigits - 3 )).
                end_value = floor(end_value * pow(10, static_cast<int>(*fractional_second_digits) - 3));

                // 7. If v1 is not equal to v2, then
                if (start_value != end_value) {
                    // a. Set dateFieldsPracticallyEqual to false.
                    date_fields_practically_equal = false;
                }

                break;
            }

            // v. Else,
            default: {
                // 1. Let v1 be tm1.[[<fieldName>]].
                // 2. Let v2 be tm2.[[<fieldName>]].

                // 3. If v1 is not equal to v2, then
                if (start_value != end_value) {
                    // a. Set dateFieldsPracticallyEqual to false.
                    date_fields_practically_equal = false;
                }

                break;
            }
            }
        }

        if (date_fields_practically_equal && !pattern_contains_larger_date_field)
            return IterationDecision::Continue;
        return IterationDecision::Break;
    });

    // 13. If dateFieldsPracticallyEqual is true, then
    if (date_fields_practically_equal) {
        // a. Let pattern be dateTimeFormat.[[Pattern]].
        auto const& pattern = date_time_format.pattern();

        // b. Let patternParts be PartitionPattern(pattern).
        auto pattern_parts = partition_pattern(pattern);

        // c. Let result be ? FormatDateTimePattern(dateTimeFormat, patternParts, x, undefined).
        auto raw_result = TRY(format_date_time_pattern(global_object, date_time_format, move(pattern_parts), start, nullptr));
        auto result = PatternPartitionWithSource::create_from_parent_list(move(raw_result));

        // d. For each Record { [[Type]], [[Value]] } r in result, do
        for (auto& part : result) {
            // i. Set r.[[Source]] to "shared".
            part.source = "shared"sv;
        }

        // e. Return result.
        return result;
    }

    // 14. Let result be a new empty List.
    Vector<PatternPartitionWithSource> result;

    // 15. If rangePattern is undefined, then
    if (!range_pattern.has_value()) {
        // a. Let rangePattern be rangePatterns.[[Default]].
        range_pattern = Unicode::get_calendar_default_range_format(date_time_format.data_locale(), date_time_format.calendar());

        // Non-standard, range_pattern will be empty if Unicode data generation is disabled.
        if (!range_pattern.has_value())
            return result;

        // Non-standard, LibUnicode leaves the CLDR's {0} and {1} partitions in the default patterns
        // to be replaced at runtime with the DateTimeFormat object's pattern.
        auto const& pattern = date_time_format.pattern();

        if (range_pattern->start_range.contains("{0}"sv)) {
            range_pattern->start_range = range_pattern->start_range.replace("{0}"sv, pattern);
            range_pattern->end_range = range_pattern->end_range.replace("{1}"sv, pattern);
        } else {
            range_pattern->start_range = range_pattern->start_range.replace("{1}"sv, pattern);
            range_pattern->end_range = range_pattern->end_range.replace("{0}"sv, pattern);
        }

        // FIXME: The above is not sufficient. For example, if the start date is days before the end date, and only the timeStyle
        //        option is provided, the resulting range will not include the differing dates. We will likely need to implement
        //        step 3 here: https://unicode.org/reports/tr35/tr35-dates.html#intervalFormats
    }

    // 16. For each Record { [[Pattern]], [[Source]] } rangePatternPart in rangePattern.[[PatternParts]], do
    TRY(for_each_range_pattern_with_source(*range_pattern, [&](auto const& pattern, auto source) -> ThrowCompletionOr<void> {
        // a. Let pattern be rangePatternPart.[[Pattern]].
        // b. Let source be rangePatternPart.[[Source]].

        // c. If source is "startRange" or "shared", then
        //     i. Let z be x.
        // d. Else,
        //     i. Let z be y.
        auto time = ((source == "startRange") || (source == "shared")) ? start : end;

        // e. Let patternParts be PartitionPattern(pattern).
        auto pattern_parts = partition_pattern(pattern);

        // f. Let partResult be ? FormatDateTimePattern(dateTimeFormat, patternParts, z, rangePattern).
        auto raw_part_result = TRY(format_date_time_pattern(global_object, date_time_format, move(pattern_parts), time, &range_pattern.value()));
        auto part_result = PatternPartitionWithSource::create_from_parent_list(move(raw_part_result));

        // g. For each Record { [[Type]], [[Value]] } r in partResult, do
        for (auto& part : part_result) {
            // i. Set r.[[Source]] to source.
            part.source = source;
        }

        // h. Add all elements in partResult to result in order.
        result.extend(move(part_result));
        return {};
    }));

    // 17. Return result.
    return result;
}

// 11.5.11 FormatDateTimeRange ( dateTimeFormat, x, y ), https://tc39.es/ecma402/#sec-formatdatetimerange
ThrowCompletionOr<String> format_date_time_range(GlobalObject& global_object, DateTimeFormat& date_time_format, double start, double end)
{
    // 1. Let parts be ? PartitionDateTimeRangePattern(dateTimeFormat, x, y).
    auto parts = TRY(partition_date_time_range_pattern(global_object, date_time_format, start, end));

    // 2. Let result be the empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]], [[Source]] } part in parts, do
    for (auto& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(move(part.value));
    }

    // 4. Return result.
    return result.build();
}

// 11.5.12 FormatDateTimeRangeToParts ( dateTimeFormat, x, y ), https://tc39.es/ecma402/#sec-formatdatetimerangetoparts
ThrowCompletionOr<Array*> format_date_time_range_to_parts(GlobalObject& global_object, DateTimeFormat& date_time_format, double start, double end)
{
    auto& vm = global_object.vm();

    // 1. Let parts be ? PartitionDateTimeRangePattern(dateTimeFormat, x, y).
    auto parts = TRY(partition_date_time_range_pattern(global_object, date_time_format, start, end));

    // 2. Let result be ! ArrayCreate(0).
    auto* result = MUST(Array::create(global_object, 0));

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]], [[Source]] } part in parts, do
    for (auto& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%ObjectPrototype%).
        auto* object = Object::create(global_object, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, move(part.value))));

        // d. Perform ! CreateDataPropertyOrThrow(O, "source", part.[[Source]]).
        MUST(object->create_data_property_or_throw(vm.names.source, js_string(vm, part.source)));

        // e. Perform ! CreateDataProperty(result, ! ToString(n), O).
        MUST(result->create_data_property_or_throw(n, object));

        // f. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

// 11.5.13 ToLocalTime ( t, calendar, timeZone ), https://tc39.es/ecma402/#sec-tolocaltime
ThrowCompletionOr<LocalTime> to_local_time(GlobalObject& global_object, double time, StringView calendar, StringView time_zone)
{
    // 1. Assert: Type(t) is Number.

    // 2. If calendar is "gregory", then
    if (calendar == "gregory"sv) {
        // a. Let timeZoneOffset be the value calculated according to LocalTZA(t, true) where the local time zone is replaced with timezone timeZone.
        double time_zone_offset = local_tza(time, true, time_zone);

        // b. Let tz be the time value t + timeZoneOffset.
        double zoned_time = time + time_zone_offset;

        auto year = year_from_time(zoned_time);

        // c. Return a record with fields calculated from tz according to Table 7.
        return LocalTime {
            // WeekDay(tz) specified in es2022's Week Day.
            .weekday = week_day(zoned_time),
            // Let year be YearFromTime(tz) specified in es2022's Year Number. If year is less than 0, return 'BC', else, return 'AD'.
            .era = year < 0 ? Unicode::Era::BC : Unicode::Era::AD,
            // YearFromTime(tz) specified in es2022's Year Number.
            .year = year,
            // undefined.
            .related_year = js_undefined(),
            // undefined.
            .year_name = js_undefined(),
            // MonthFromTime(tz) specified in es2022's Month Number.
            .month = month_from_time(zoned_time),
            // DateFromTime(tz) specified in es2022's Date Number.
            .day = date_from_time(zoned_time),
            // HourFromTime(tz) specified in es2022's Hours, Minutes, Second, and Milliseconds.
            .hour = hour_from_time(zoned_time),
            // MinFromTime(tz) specified in es2022's Hours, Minutes, Second, and Milliseconds.
            .minute = min_from_time(zoned_time),
            // SecFromTime(tz) specified in es2022's Hours, Minutes, Second, and Milliseconds.
            .second = sec_from_time(zoned_time),
            // msFromTime(tz) specified in es2022's Hours, Minutes, Second, and Milliseconds.
            .millisecond = ms_from_time(zoned_time),
        };
    }

    // 3. Else,
    //     a. Return a record with the fields of Column 1 of Table 7 calculated from t for the given calendar and timeZone. The calculations should use best available information about the specified calendar and timeZone, including current and historical information about time zone offsets from UTC and daylight saving time rules.
    // FIXME: Implement this when non-Gregorian calendars are supported by LibUnicode.
    return global_object.vm().throw_completion<InternalError>(global_object, ErrorType::NotImplemented, "Non-Gregorian calendars"sv);
}

}
