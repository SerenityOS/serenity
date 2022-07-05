/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
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

}
