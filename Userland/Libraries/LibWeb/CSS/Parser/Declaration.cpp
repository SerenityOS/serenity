/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Declaration.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS::Parser {

Declaration::Declaration() = default;
Declaration::~Declaration() = default;

String Declaration::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append(": ");
    builder.join(" ", m_values);

    if (m_important == Important::Yes)
        builder.append(" !important");

    return builder.to_string();
}

}
