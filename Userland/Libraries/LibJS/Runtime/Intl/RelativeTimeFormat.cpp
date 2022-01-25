/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>

namespace JS::Intl {

// 17 RelativeTimeFormat Objects, https://tc39.es/ecma402/#relativetimeformat-objects
RelativeTimeFormat::RelativeTimeFormat(Object& prototype)
    : Object(prototype)
{
}

void RelativeTimeFormat::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_number_format)
        visitor.visit(m_number_format);
}

void RelativeTimeFormat::set_numeric(StringView numeric)
{
    if (numeric == "always"sv) {
        m_numeric = Numeric::Always;
    } else if (numeric == "auto"sv) {
        m_numeric = Numeric::Auto;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView RelativeTimeFormat::numeric_string() const
{
    switch (m_numeric) {
    case Numeric::Always:
        return "always"sv;
    case Numeric::Auto:
        return "auto"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
