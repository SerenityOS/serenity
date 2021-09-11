/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

Locale* Locale::create(GlobalObject& global_object, Unicode::LocaleID const& locale_id)
{
    return global_object.heap().allocate<Locale>(global_object, locale_id, *global_object.intl_locale_prototype());
}

Vector<StringView> const& Locale::relevant_extension_keys()
{
    // 14.2.2 Internal slots, https://tc39.es/ecma402/#sec-intl.locale-internal-slots
    // The value of the [[RelevantExtensionKeys]] internal slot is « "ca", "co", "hc", "kf", "kn", "nu" ».
    // If %Collator%.[[RelevantExtensionKeys]] does not contain "kf", then remove "kf" from %Locale%.[[RelevantExtensionKeys]].
    // If %Collator%.[[RelevantExtensionKeys]] does not contain "kn", then remove "kn" from %Locale%.[[RelevantExtensionKeys]].

    // FIXME: We do not yet have an Intl.Collator object. For now, we behave as if "kf" and "kn" exist, as test262 depends on it.
    static Vector<StringView> relevant_extension_keys { "ca"sv, "co"sv, "hc"sv, "kf"sv, "kn"sv, "nu"sv };
    return relevant_extension_keys;
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

}
