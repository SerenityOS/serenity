/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/Collator.h>

namespace JS::Intl {

JS_DEFINE_ALLOCATOR(Collator);

// 10 Collator Objects, https://tc39.es/ecma402/#collator-objects
Collator::Collator(Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
{
}

void Collator::set_usage(StringView type)
{
    if (type == "sort"sv)
        m_usage = Usage::Sort;
    else if (type == "search"sv)
        m_usage = Usage::Search;
    else
        VERIFY_NOT_REACHED();
}

StringView Collator::usage_string() const
{
    switch (m_usage) {
    case Usage::Sort:
        return "sort"sv;
    case Usage::Search:
        return "search"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Collator::set_sensitivity(StringView type)
{
    if (type == "base"sv)
        m_sensitivity = Sensitivity::Base;
    else if (type == "accent"sv)
        m_sensitivity = Sensitivity::Accent;
    else if (type == "case"sv)
        m_sensitivity = Sensitivity::Case;
    else if (type == "variant"sv)
        m_sensitivity = Sensitivity::Variant;
    else
        VERIFY_NOT_REACHED();
}

StringView Collator::sensitivity_string() const
{
    switch (m_sensitivity) {
    case Sensitivity::Base:
        return "base"sv;
    case Sensitivity::Accent:
        return "accent"sv;
    case Sensitivity::Case:
        return "case"sv;
    case Sensitivity::Variant:
        return "variant"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Collator::set_case_first(StringView case_first)
{
    if (case_first == "upper"sv)
        m_case_first = CaseFirst::Upper;
    else if (case_first == "lower"sv)
        m_case_first = CaseFirst::Lower;
    else if (case_first == "false"sv)
        m_case_first = CaseFirst::False;
    else
        VERIFY_NOT_REACHED();
}

StringView Collator::case_first_string() const
{
    switch (m_case_first) {
    case CaseFirst::Upper:
        return "upper"sv;
    case CaseFirst::Lower:
        return "lower"sv;
    case CaseFirst::False:
        return "false"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}
void Collator::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_bound_compare);
}

}
