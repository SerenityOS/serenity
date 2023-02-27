/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
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

ErrorOr<String> Block::to_string() const
{
    StringBuilder builder;

    TRY(builder.try_append(m_token.bracket_string()));
    TRY(builder.try_join(' ', m_values));
    TRY(builder.try_append(m_token.bracket_mirror_string()));

    return builder.to_string();
}

}
