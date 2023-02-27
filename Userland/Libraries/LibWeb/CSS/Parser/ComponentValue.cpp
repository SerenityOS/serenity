/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Block.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/Function.h>

namespace Web::CSS::Parser {

ComponentValue::ComponentValue(Token token)
    : m_value(token)
{
}
ComponentValue::ComponentValue(NonnullRefPtr<Function> function)
    : m_value(function)
{
}
ComponentValue::ComponentValue(NonnullRefPtr<Block> block)
    : m_value(block)
{
}

ComponentValue::~ComponentValue() = default;

ErrorOr<String> ComponentValue::to_string() const
{
    return m_value.visit(
        [](Token const& token) { return token.to_string(); },
        [](NonnullRefPtr<Block> const& block) { return block->to_string(); },
        [](NonnullRefPtr<Function> const& function) { return function->to_string(); });
}

ErrorOr<String> ComponentValue::to_debug_string() const
{
    return m_value.visit(
        [](Token const& token) -> ErrorOr<String> {
            return String::formatted("Token: {}", TRY(token.to_debug_string()));
        },
        [](NonnullRefPtr<Block> const& block) -> ErrorOr<String> {
            return String::formatted("Block: {}", TRY(block->to_string()));
        },
        [](NonnullRefPtr<Function> const& function) -> ErrorOr<String> {
            return String::formatted("Function: {}", TRY(function->to_string()));
        });
}

}
