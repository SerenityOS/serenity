/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Block.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/Declaration.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Function.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

StyleRule::StyleRule(StyleRule::Type type)
    : m_type(type)
{
}
StyleRule::~StyleRule() = default;

template<class SeparatorType, class CollectionType>
void append_with_to_string(StringBuilder& builder, SeparatorType& separator, CollectionType& collection)
{
    bool first = true;
    for (auto& item : collection) {
        if (first)
            first = false;
        else
            builder.append(separator);
        builder.append(item.to_string());
    }
}

String StyleRule::to_string() const
{
    StringBuilder builder;

    if (is_at_rule()) {
        builder.append("@");
        serialize_an_identifier(builder, m_at_rule_name);
    }

    append_with_to_string(builder, " ", m_prelude);

    if (m_block)
        builder.append(m_block->to_string());
    else
        builder.append(';');

    return builder.to_string();
}
}
