/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Declaration.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS::Parser {

Declaration::Declaration(FlyString name, Vector<ComponentValue> values, Important important)
    : m_name(move(name))
    , m_values(move(values))
    , m_important(move(important))
{
}

Declaration::~Declaration() = default;

String Declaration::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append(": "sv);
    builder.join(' ', m_values);

    if (m_important == Important::Yes)
        builder.append(" !important"sv);

    return MUST(builder.to_string());
}

}
