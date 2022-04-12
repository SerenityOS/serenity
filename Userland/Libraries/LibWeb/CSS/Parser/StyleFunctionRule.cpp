/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

StyleFunctionRule::StyleFunctionRule(String name)
    : m_name(move(name))
{
}

StyleFunctionRule::StyleFunctionRule(String name, Vector<Parser::ComponentValue>&& values)
    : m_name(move(name))
    , m_values(move(values))
{
}

StyleFunctionRule::~StyleFunctionRule() = default;

String StyleFunctionRule::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append("(");
    builder.join(" ", m_values);
    builder.append(")");

    return builder.to_string();
}

}
