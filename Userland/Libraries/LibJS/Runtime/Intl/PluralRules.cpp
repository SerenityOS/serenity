/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/PluralRules.h>

namespace JS::Intl {

// 16 PluralRules Objects, https://tc39.es/ecma402/#pluralrules-objects
PluralRules::PluralRules(Object& prototype)
    : NumberFormatBase(prototype)
{
}

void PluralRules::set_type(StringView type)
{
    if (type == "cardinal"sv) {
        m_type = Type::Cardinal;
    } else if (type == "ordinal"sv) {
        m_type = Type::Ordinal;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView PluralRules::type_string() const
{
    switch (m_type) {
    case Type::Cardinal:
        return "cardinal"sv;
    case Type::Ordinal:
        return "ordinal"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
