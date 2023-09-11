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

bool ComponentValue::is_function(StringView name) const
{
    return is_function() && function().name().equals_ignoring_ascii_case(name);
}

bool ComponentValue::is_ident(StringView ident) const
{
    return is(Token::Type::Ident) && token().ident().equals_ignoring_ascii_case(ident);
}

String ComponentValue::to_string() const
{
    return m_value.visit(
        [](Token const& token) { return token.to_string(); },
        [](NonnullRefPtr<Block> const& block) { return block->to_string(); },
        [](NonnullRefPtr<Function> const& function) { return function->to_string(); });
}

String ComponentValue::to_debug_string() const
{
    return m_value.visit(
        [](Token const& token) {
            return MUST(String::formatted("Token: {}", token.to_debug_string()));
        },
        [](NonnullRefPtr<Block> const& block) {
            return MUST(String::formatted("Block: {}", block->to_string()));
        },
        [](NonnullRefPtr<Function> const& function) {
            return MUST(String::formatted("Function: {}", function->to_string()));
        });
}

}
