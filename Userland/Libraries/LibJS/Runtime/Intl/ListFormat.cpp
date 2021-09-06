/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/ListFormat.h>

namespace JS::Intl {

// 13 ListFomat Objects, https://tc39.es/ecma402/#listformat-objects
ListFormat::ListFormat(Object& prototype)
    : Object(prototype)
{
}

void ListFormat::set_type(StringView type)
{
    if (type == "conjunction"sv) {
        m_type = Type::Conjunction;
    } else if (type == "disjunction"sv) {
        m_type = Type::Disjunction;
    } else if (type == "unit"sv) {
        m_type = Type::Unit;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView ListFormat::type_string() const
{
    switch (m_type) {
    case Type::Conjunction:
        return "conjunction"sv;
    case Type::Disjunction:
        return "disjunction"sv;
    case Type::Unit:
        return "unit"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void ListFormat::set_style(StringView style)
{
    if (style == "narrow"sv) {
        m_style = Style::Narrow;
    } else if (style == "short"sv) {
        m_style = Style::Short;
    } else if (style == "long"sv) {
        m_style = Style::Long;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView ListFormat::style_string() const
{
    switch (m_style) {
    case Style::Narrow:
        return "narrow"sv;
    case Style::Short:
        return "short"sv;
    case Style::Long:
        return "long"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
