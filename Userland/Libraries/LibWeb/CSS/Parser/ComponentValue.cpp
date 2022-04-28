/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
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
            return String::formatted("Token: {}", token.to_debug_string());
        },
        [](NonnullRefPtr<Block> const& block) {
            return String::formatted("Function: {}", block->to_string());
        },
        [](NonnullRefPtr<Function> const& function) {
            return String::formatted("Block: {}", function->to_string());
        });
}

}
