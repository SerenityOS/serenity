/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Block.h>

namespace Web::CSS::Parser {

Block::Block(Token token, Vector<ComponentValue>&& values)
    : m_token(move(token))
    , m_values(move(values))
{
}

Block::~Block() = default;

DeprecatedString Block::to_deprecated_string() const
{
    StringBuilder builder;

    builder.append(m_token.bracket_string());
    builder.join(' ', m_values);
    builder.append(m_token.bracket_mirror_string());

    return builder.to_deprecated_string();
}

}
