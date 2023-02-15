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

ErrorOr<String> Declaration::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    TRY(builder.try_append(": "sv));
    TRY(builder.try_join(' ', m_values));

    if (m_important == Important::Yes)
        TRY(builder.try_append(" !important"sv));

    return builder.to_string();
}

}
