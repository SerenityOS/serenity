/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
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

    auto join_keyword_types = [](auto const& types) {
        StringBuilder builder;
        builder.join('-', types);
        return builder.build();
    };

    for (auto const& extension : locale_id.extensions) {
        if (!extension.has<Unicode::LocaleExtension>())
            continue;

        for (auto const& keyword : extension.get<Unicode::LocaleExtension>().keywords) {
            if (keyword.key == "ca"sv) {
                set_calendar(join_keyword_types(keyword.types));
            } else if (keyword.key == "co"sv) {
                set_collation(join_keyword_types(keyword.types));
            } else if (keyword.key == "hc"sv) {
                set_hour_cycle(join_keyword_types(keyword.types));
            } else if (keyword.key == "kf"sv) {
                set_case_first(join_keyword_types(keyword.types));
            } else if (keyword.key == "kn"sv) {
                set_numeric(keyword.types.is_empty());
            } else if (keyword.key == "nu"sv) {
                set_numbering_system(join_keyword_types(keyword.types));
            }
        }

        break;
    }
}

}
