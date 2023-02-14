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

ErrorOr<String> Function::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    TRY(builder.try_append('('));
    TRY(builder.try_join(' ', m_values));
    TRY(builder.try_append(')'));

    return builder.to_string();
}

}
