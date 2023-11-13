/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibLocale/DateTimeFormat.h>
#include <LibLocale/Locale.h>
#include <LibTimeZone/TimeZone.h>

namespace JS::Intl {

NonnullGCPtr<Locale> Locale::create(Realm& realm, ::Locale::LocaleID locale_id)
{
    auto locale = realm.heap().allocate<Locale>(realm, realm.intrinsics().intl_locale_prototype());
    locale->set_locale(locale_id.to_string());

    for (auto& extension : locale_id.extensions) {
        if (!extension.has<::Locale::LocaleExtension>())
            continue;

        for (auto& keyword : extension.get<::Locale::LocaleExtension>().keywords) {
            if (keyword.key == "ca"sv)
                locale->set_calendar(move(keyword.value));
            else if (keyword.key == "co"sv)
                locale->set_collation(move(keyword.value));
            else if (keyword.key == "hc"sv)
                locale->set_hour_cycle(move(keyword.value));
            else if (keyword.key == "kf"sv)
                locale->set_case_first(move(keyword.value));
            else if (keyword.key == "kn"sv)
                locale->set_numeric(keyword.value.is_empty());
            else if (keyword.key == "nu"sv)
                locale->set_numbering_system(move(keyword.value));
        }

        break;
    }

    return locale;
}

// 14 Locale Objects, https://tc39.es/ecma402/#locale-objects
Locale::Locale(Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
{
}

// 1.1.1 CreateArrayFromListOrRestricted ( list , restricted )
static NonnullGCPtr<Array> create_array_from_list_or_restricted(VM& vm, Vector<StringView> list, Optional<String> restricted)
{
    auto& realm = *vm.current_realm();

    // 1. If restricted is not undefined, then
    if (restricted.has_value()) {
        // a. Set list to « restricted ».
        list = { *restricted };
    }

    // 2. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(realm, list, [&vm](auto value) {
        return PrimitiveString::create(vm, MUST(String::from_utf8(value)));
    });
}

// 1.1.2 CalendarsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-calendars-of-locale
NonnullGCPtr<Array> calendars_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Calendar]].
    Optional<String> restricted = locale_object.has_calendar() ? locale_object.calendar() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical calendar identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, sorted in descending preference of those in common use for date and time formatting in locale.
    auto list = ::Locale::get_keywords_for_locale(locale, "ca"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(vm, move(list), move(restricted));
}

// 1.1.3 CollationsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-collations-of-locale
NonnullGCPtr<Array> collations_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Collation]].
    Optional<String> restricted = locale_object.has_collation() ? locale_object.collation() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical collation identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, ordered as if an Array of the same values had been sorted, using %Array.prototype.sort% using undefined as comparefn, of those in common use for string comparison in locale. The values "standard" and "search" must be excluded from list.
    auto list = ::Locale::get_keywords_for_locale(locale, "co"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(vm, move(list), move(restricted));
}

// 1.1.4 HourCyclesOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-hour-cycles-of-locale
NonnullGCPtr<Array> hour_cycles_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[HourCycle]].
    Optional<String> restricted = locale_object.has_hour_cycle() ? locale_object.hour_cycle() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique hour cycle identifiers, which must be lower case String values indicating either the 12-hour format ("h11", "h12") or the 24-hour format ("h23", "h24"), sorted in descending preference of those in common use for date and time formatting in locale.
    auto list = ::Locale::get_keywords_for_locale(locale, "hc"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(vm, move(list), move(restricted));
}

// 1.1.5 NumberingSystemsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-numbering-systems-of-locale
NonnullGCPtr<Array> numbering_systems_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[NumberingSystem]].
    Optional<String> restricted = locale_object.has_numbering_system() ? locale_object.numbering_system() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical numbering system identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, sorted in descending preference of those in common use for formatting numeric values in locale.
    auto list = ::Locale::get_keywords_for_locale(locale, "nu"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(vm, move(list), move(restricted));
}

// 1.1.6 TimeZonesOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-time-zones-of-locale
// NOTE: Our implementation takes a region rather than a Locale object to avoid needlessly parsing the locale twice.
NonnullGCPtr<Array> time_zones_of_locale(VM& vm, StringView region)
{
    auto& realm = *vm.current_realm();

    // 1. Let locale be loc.[[Locale]].
    // 2. Assert: locale matches the unicode_locale_id production.
    // 3. Let region be the substring of locale corresponding to the unicode_region_subtag production of the unicode_language_id.

    // 4. Let list be a List of unique canonical time zone identifiers, which must be String values indicating a canonical Zone name of the IANA Time Zone Database, ordered as if an Array of the same values had been sorted using %Array.prototype.sort% using undefined as comparefn, of those in common use in region. If no time zones are commonly used in region, let list be a new empty List.
    auto list = TimeZone::time_zones_in_region(region);
    quick_sort(list);

    // 5. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(realm, list, [&vm](auto value) {
        return PrimitiveString::create(vm, String::from_utf8(value).release_value());
    });
}

// 1.1.7 CharacterDirectionOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-character-direction-of-locale
StringView character_direction_of_locale(Locale const& locale_object)
{
    // 1. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 2. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 3. If the default general ordering of characters (characterOrder) within a line in locale is right-to-left, return "rtl".
    // NOTE: LibUnicode handles both LTR and RTL character orders in this call, not just RTL. We then fallback to LTR
    //       below if LibUnicode doesn't conclusively know the character order for this locale.
    if (auto character_order = ::Locale::character_order_for_locale(locale); character_order.has_value())
        return ::Locale::character_order_to_string(*character_order);

    // 4. Return "ltr".
    return "ltr"sv;
}

// 1.1.8 WeekdayToNumber ( fw ), https://tc39.es/proposal-intl-locale-info/#sec-weekday-to-number
// FIXME: Spec issue: The spec definitions of WeekdayToNumber and WeekdayToString are backwards.
//        https://github.com/tc39/proposal-intl-locale-info/issues/78
Optional<u8> weekday_to_number(StringView weekday)
{
    struct WeekdayToNumber {
        StringView type;
        u8 number { 0 };
    };

    // Table 2: First Day Type and Number, https://tc39.es/proposal-intl-locale-info/#table-locale-first-day-type-number
    static constexpr auto weekday_to_number_table = AK::Array {
        WeekdayToNumber { "mon"sv, 1 },
        WeekdayToNumber { "tue"sv, 2 },
        WeekdayToNumber { "wed"sv, 3 },
        WeekdayToNumber { "thu"sv, 4 },
        WeekdayToNumber { "fri"sv, 5 },
        WeekdayToNumber { "sat"sv, 6 },
        WeekdayToNumber { "sun"sv, 7 },
    };

    // 1. For each row of Table 2, except the header row, in table order, do
    for (auto const& row : weekday_to_number_table) {
        // a. Let t be the name given in the Type column of the row.
        auto type = row.type;

        // b. Let n be the name given in the Number column of the row.
        auto number = row.number;

        // c. If fw is equal to t, return n.
        if (weekday == type)
            return number;
    }

    // 2. Assert: Should not reach here.
    // FIXME: Spec issue: This is currently reachable if an invalid value is provided as a locale extension,
    //        for example "en-u-fw-100". We return "undefined" for now to avoid crashing.
    //        https://github.com/tc39/proposal-intl-locale-info/issues/78
    return {};
}

// 1.1.9 WeekdayToString ( fw ), https://tc39.es/proposal-intl-locale-info/#sec-weekday-to-string
// FIXME: Spec issue: The spec definitions of WeekdayToNumber and WeekdayToString are backwards.
//        https://github.com/tc39/proposal-intl-locale-info/issues/78
StringView weekday_to_string(StringView weekday)
{
    struct WeekdayToString {
        StringView value;
        StringView type;
    };

    // Table 1: First Day Value and Type, https://tc39.es/proposal-intl-locale-info/#table-locale-first-day-option-type
    static constexpr auto weekday_to_string_table = AK::Array {
        WeekdayToString { "mon"sv, "mon"sv },
        WeekdayToString { "tue"sv, "tue"sv },
        WeekdayToString { "wed"sv, "wed"sv },
        WeekdayToString { "thu"sv, "thu"sv },
        WeekdayToString { "fri"sv, "fri"sv },
        WeekdayToString { "sat"sv, "sat"sv },
        WeekdayToString { "sun"sv, "sun"sv },
        WeekdayToString { "0"sv, "sun"sv },
        WeekdayToString { "1"sv, "mon"sv },
        WeekdayToString { "2"sv, "tue"sv },
        WeekdayToString { "3"sv, "wed"sv },
        WeekdayToString { "4"sv, "thu"sv },
        WeekdayToString { "5"sv, "fri"sv },
        WeekdayToString { "6"sv, "sat"sv },
        WeekdayToString { "7"sv, "sun"sv },
    };

    // 1. For each row of Table 1, except the header row, in table order, do
    for (auto const& row : weekday_to_string_table) {
        // a. Let v be the name given in the Value column of the row.
        auto value = row.value;

        // b. Let t be the name given in the Type column of the row.
        auto type = row.type;

        // c. If fw is equal to v, return t.
        if (weekday == value)
            return type;
    }

    // 2. Assert: Should not reach here.
    VERIFY_NOT_REACHED();
}

static u8 weekday_to_integer(Optional<::Locale::Weekday> weekday, ::Locale::Weekday falllback)
{
    // NOTE: This fallback will be used if LibUnicode data generation is disabled. Its value should
    //       be that of the default region ("001") in the CLDR.
    switch (weekday.value_or(falllback)) {
    case ::Locale::Weekday::Monday:
        return 1;
    case ::Locale::Weekday::Tuesday:
        return 2;
    case ::Locale::Weekday::Wednesday:
        return 3;
    case ::Locale::Weekday::Thursday:
        return 4;
    case ::Locale::Weekday::Friday:
        return 5;
    case ::Locale::Weekday::Saturday:
        return 6;
    case ::Locale::Weekday::Sunday:
        return 7;
    }

    VERIFY_NOT_REACHED();
}

static Vector<u8> weekend_of_locale(StringView locale)
{
    auto weekend_start = weekday_to_integer(::Locale::get_locale_weekend_start(locale), ::Locale::Weekday::Saturday);
    auto weekend_end = weekday_to_integer(::Locale::get_locale_weekend_end(locale), ::Locale::Weekday::Sunday);

    // There currently aren't any regions in the CLDR which wrap around from Sunday (7) to Monday (1).
    // If this changes, this logic will need to be updated to handle that.
    VERIFY(weekend_start <= weekend_end);

    Vector<u8> weekend;
    weekend.ensure_capacity(weekend_end - weekend_start + 1);

    for (auto day = weekend_start; day <= weekend_end; ++day)
        weekend.unchecked_append(day);

    return weekend;
}

// 1.1.10 WeekInfoOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-week-info-of-locale
WeekInfo week_info_of_locale(Locale const& locale_object)
{
    // 1. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 2. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 3. Let r be a record whose fields are defined by Table 3, with values based on locale.
    WeekInfo week_info {};
    week_info.minimal_days = ::Locale::get_locale_minimum_days(locale).value_or(1);
    week_info.first_day = weekday_to_integer(::Locale::get_locale_first_day(locale), ::Locale::Weekday::Monday);
    week_info.weekend = weekend_of_locale(locale);

    // 4. Let fw be loc.[[FirstDayOfWeek]].
    // 5. If fw is not undefined, then
    if (locale_object.has_first_day_of_week()) {
        // a. Set r.[[FirstDay]] to fw.
        week_info.first_day = locale_object.first_day_of_week();
    }

    // 6. Return r.
    return week_info;
}

}
