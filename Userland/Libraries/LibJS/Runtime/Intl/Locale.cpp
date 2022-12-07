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

Locale* Locale::create(Realm& realm, ::Locale::LocaleID const& locale_id)
{
    return realm.heap().allocate<Locale>(realm, locale_id, *realm.intrinsics().intl_locale_prototype());
}

// 14 Locale Objects, https://tc39.es/ecma402/#locale-objects
Locale::Locale(Object& prototype)
    : Object(prototype)
{
}

Locale::Locale(::Locale::LocaleID const& locale_id, Object& prototype)
    : Object(prototype)
{
    set_locale(locale_id.to_deprecated_string());

    for (auto const& extension : locale_id.extensions) {
        if (!extension.has<::Locale::LocaleExtension>())
            continue;

        for (auto const& keyword : extension.get<::Locale::LocaleExtension>().keywords) {
            if (keyword.key == "ca"sv) {
                set_calendar(keyword.value);
            } else if (keyword.key == "co"sv) {
                set_collation(keyword.value);
            } else if (keyword.key == "hc"sv) {
                set_hour_cycle(keyword.value);
            } else if (keyword.key == "kf"sv) {
                set_case_first(keyword.value);
            } else if (keyword.key == "kn"sv) {
                set_numeric(keyword.value.is_empty());
            } else if (keyword.key == "nu"sv) {
                set_numbering_system(keyword.value);
            }
        }

        break;
    }
}

// 1.1.1 CreateArrayFromListOrRestricted ( list , restricted )
static Array* create_array_from_list_or_restricted(VM& vm, Vector<StringView> list, Optional<DeprecatedString> restricted)
{
    auto& realm = *vm.current_realm();

    // 1. If restricted is not undefined, then
    if (restricted.has_value()) {
        // a. Set list to « restricted ».
        list = { *restricted };
    }

    // 2. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(realm, list, [&vm](auto value) {
        return PrimitiveString::create(vm, value);
    });
}

// 1.1.2 CalendarsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-calendars-of-locale
Array* calendars_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Calendar]].
    Optional<DeprecatedString> restricted = locale_object.has_calendar() ? locale_object.calendar() : Optional<DeprecatedString> {};

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
Array* collations_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Collation]].
    Optional<DeprecatedString> restricted = locale_object.has_collation() ? locale_object.collation() : Optional<DeprecatedString> {};

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
Array* hour_cycles_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[HourCycle]].
    Optional<DeprecatedString> restricted = locale_object.has_hour_cycle() ? locale_object.hour_cycle() : Optional<DeprecatedString> {};

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
Array* numbering_systems_of_locale(VM& vm, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[NumberingSystem]].
    Optional<DeprecatedString> restricted = locale_object.has_numbering_system() ? locale_object.numbering_system() : Optional<DeprecatedString> {};

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
Array* time_zones_of_locale(VM& vm, StringView region)
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
        return PrimitiveString::create(vm, value);
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

// 1.1.8 WeekInfoOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-week-info-of-locale
WeekInfo week_info_of_locale(Locale const& locale_object)
{
    // 1. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 2. Assert: locale matches the unicode_locale_id production.
    VERIFY(::Locale::parse_unicode_locale_id(locale).has_value());

    // 3. Return a record whose fields are defined by Table 1, with values based on locale.
    WeekInfo week_info {};
    week_info.minimal_days = ::Locale::get_locale_minimum_days(locale).value_or(1);
    week_info.first_day = weekday_to_integer(::Locale::get_locale_first_day(locale), ::Locale::Weekday::Monday);
    week_info.weekend = weekend_of_locale(locale);

    return week_info;
}

}
