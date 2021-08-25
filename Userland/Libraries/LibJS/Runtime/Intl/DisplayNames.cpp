/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>

namespace JS::Intl {

// 12 DisplayNames Objects, https://tc39.es/ecma402/#intl-displaynames-objects
DisplayNames::DisplayNames(Object& prototype)
    : Object(prototype)
{
}

void DisplayNames::set_style(StringView style)
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

StringView DisplayNames::style_string() const
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

void DisplayNames::set_type(StringView type)
{
    if (type == "language"sv) {
        m_type = Type::Language;
    } else if (type == "region"sv) {
        m_type = Type::Region;
    } else if (type == "script"sv) {
        m_type = Type::Script;
    } else if (type == "currency"sv) {
        m_type = Type::Currency;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView DisplayNames::type_string() const
{
    switch (m_type) {
    case Type::Language:
        return "language"sv;
    case Type::Region:
        return "region"sv;
    case Type::Script:
        return "script"sv;
    case Type::Currency:
        return "currency"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void DisplayNames::set_fallback(StringView fallback)
{
    if (fallback == "none"sv) {
        m_fallback = Fallback::None;
    } else if (fallback == "code"sv) {
        m_fallback = Fallback::Code;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView DisplayNames::fallback_string() const
{
    switch (m_fallback) {
    case Fallback::None:
        return "none"sv;
    case Fallback::Code:
        return "code"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
