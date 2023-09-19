/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Function.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS::Parser {

Function::Function(FlyString name, Vector<ComponentValue>&& values)
    : m_name(move(name))
    , m_values(move(values))
{
}

Function::~Function() = default;

String Function::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append('(');
    for (auto& item : m_values)
        builder.append(item.to_string());
    builder.append(')');

    return MUST(builder.to_string());
}

}
