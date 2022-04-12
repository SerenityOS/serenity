/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS::Parser {

StyleRule::StyleRule(StyleRule::Type type, FlyString name, Vector<ComponentValue> prelude, RefPtr<Block> block)
    : m_type(type)
    , m_at_rule_name(move(name))
    , m_prelude(move(prelude))
    , m_block(move(block))
{
}

StyleRule::~StyleRule() = default;

String StyleRule::to_string() const
{
    StringBuilder builder;

    if (is_at_rule()) {
        builder.append("@");
        serialize_an_identifier(builder, m_at_rule_name);
    }

    builder.join(" ", m_prelude);

    if (m_block)
        builder.append(m_block->to_string());
    else
        builder.append(';');

    return builder.to_string();
}

}
