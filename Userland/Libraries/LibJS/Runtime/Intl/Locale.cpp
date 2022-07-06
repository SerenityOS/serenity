/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibTimeZone/TimeZone.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

Locale* Locale::create(GlobalObject& global_object, Unicode::LocaleID const& locale_id)
{
    return global_object.heap().allocate<Locale>(global_object, locale_id, *global_object.intl_locale_prototype());
}

// 14 Locale Objects, https://tc39.es/ecma402/#locale-objects
Locale::Locale(Object& prototype)
    : Object(prototype)
{
}

Locale::Locale(Unicode::LocaleID const& locale_id, Object& prototype)
    : Object(prototype)
{
    set_locale(locale_id.to_string());

    for (auto const& extension : locale_id.extensions) {
        if (!extension.has<Unicode::LocaleExtension>())
            continue;

        for (auto const& keyword : extension.get<Unicode::LocaleExtension>().keywords) {
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
static Array* create_array_from_list_or_restricted(GlobalObject& global_object, Vector<StringView> list, Optional<String> restricted)
{
    auto& vm = global_object.vm();

    // 1. If restricted is not undefined, then
    if (restricted.has_value()) {
        // a. Set list to « restricted ».
        list = { *restricted };
    }

    // 2. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(global_object, list, [&vm](auto value) {
        return js_string(vm, value);
    });
}

// 1.1.2 CalendarsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-calendars-of-locale
Array* calendars_of_locale(GlobalObject& global_object, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Calendar]].
    Optional<String> restricted = locale_object.has_calendar() ? locale_object.calendar() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(Unicode::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical calendar identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, sorted in descending preference of those in common use for date and time formatting in locale.
    auto list = Unicode::get_keywords_for_locale(locale, "ca"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(global_object, move(list), move(restricted));
}

// 1.1.3 CollationsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-collations-of-locale
Array* collations_of_locale(GlobalObject& global_object, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[Collation]].
    Optional<String> restricted = locale_object.has_collation() ? locale_object.collation() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(Unicode::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical collation identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, sorted in descending preference of those in common use for string comparison in locale. The values "standard" and "search" must be excluded from list.
    // FIXME: Retrieve this data from the CLDR when we fully support collation. This matches Intl.supportedValuesOf.
    Vector<StringView> list { "default"sv };

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(global_object, move(list), move(restricted));
}

// 1.1.4 HourCyclesOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-hour-cycles-of-locale
Array* hour_cycles_of_locale(GlobalObject& global_object, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[HourCycle]].
    Optional<String> restricted = locale_object.has_hour_cycle() ? locale_object.hour_cycle() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(Unicode::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique hour cycle identifiers, which must be lower case String values indicating either the 12-hour format ("h11", "h12") or the 24-hour format ("h23", "h24"), sorted in descending preference of those in common use for date and time formatting in locale.
    auto list = Unicode::get_keywords_for_locale(locale, "hc"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(global_object, move(list), move(restricted));
}

// 1.1.5 NumberingSystemsOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-numbering-systems-of-locale
Array* numbering_systems_of_locale(GlobalObject& global_object, Locale const& locale_object)
{
    // 1. Let restricted be loc.[[NumberingSystem]].
    Optional<String> restricted = locale_object.has_numbering_system() ? locale_object.numbering_system() : Optional<String> {};

    // 2. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 3. Assert: locale matches the unicode_locale_id production.
    VERIFY(Unicode::parse_unicode_locale_id(locale).has_value());

    // 4. Let list be a List of 1 or more unique canonical numbering system identifiers, which must be lower case String values conforming to the type sequence from UTS 35 Unicode Locale Identifier, section 3.2, sorted in descending preference of those in common use for formatting numeric values in locale.
    auto list = Unicode::get_keywords_for_locale(locale, "nu"sv);

    // 5. Return ! CreateArrayFromListOrRestricted( list, restricted ).
    return create_array_from_list_or_restricted(global_object, move(list), move(restricted));
}

// 1.1.6 TimeZonesOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-time-zones-of-locale
// NOTE: Our implementation takes a region rather than a Locale object to avoid needlessly parsing the locale twice.
Array* time_zones_of_locale(GlobalObject& global_object, StringView region)
{
    auto& vm = global_object.vm();

    // 1. Let locale be loc.[[Locale]].
    // 2. Assert: locale matches the unicode_locale_id production.
    // 3. Let region be the substring of locale corresponding to the unicode_region_subtag production of the unicode_language_id.

    // 4. Let list be a List of unique canonical time zone identifiers, which must be String values indicating a canonical Zone name of the IANA Time Zone Database, ordered as if an Array of the same values had been sorted using %Array.prototype.sort% using undefined as comparefn, of those in common use in region. If no time zones are commonly used in region, let list be a new empty List.
    auto list = TimeZone::time_zones_in_region(region);
    quick_sort(list);

    // 5. Return ! CreateArrayFromList( list ).
    return Array::create_from<StringView>(global_object, list, [&vm](auto value) {
        return js_string(vm, value);
    });
}

// 1.1.7 CharacterDirectionOfLocale ( loc ), https://tc39.es/proposal-intl-locale-info/#sec-character-direction-of-locale
StringView character_direction_of_locale(Locale const& locale_object)
{
    // 1. Let locale be loc.[[Locale]].
    auto const& locale = locale_object.locale();

    // 2. Assert: locale matches the unicode_locale_id production.
    VERIFY(Unicode::parse_unicode_locale_id(locale).has_value());

    // 3. If the default general ordering of characters (characterOrder) within a line in locale is right-to-left, return "rtl".
    // NOTE: LibUnicode handles both LTR and RTL character orders in this call, not just RTL. We then fallback to LTR
    //       below if LibUnicode doesn't conclusively know the character order for this locale.
    if (auto character_order = Unicode::character_order_for_locale(locale); character_order.has_value())
        return Unicode::character_order_to_string(*character_order);

    // 4. Return "ltr".
    return "ltr"sv;
}

}
