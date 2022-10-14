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

DeprecatedString ComponentValue::to_deprecated_string() const
{
    return m_value.visit(
        [](Token const& token) { return token.to_deprecated_string(); },
        [](NonnullRefPtr<Block> const& block) { return block->to_deprecated_string(); },
        [](NonnullRefPtr<Function> const& function) { return function->to_deprecated_string(); });
}

DeprecatedString ComponentValue::to_debug_string() const
{
    return m_value.visit(
        [](Token const& token) {
            return DeprecatedString::formatted("Token: {}", token.to_debug_string());
        },
        [](NonnullRefPtr<Block> const& block) {
            return DeprecatedString::formatted("Block: {}", block->to_deprecated_string());
        },
        [](NonnullRefPtr<Function> const& function) {
            return DeprecatedString::formatted("Function: {}", function->to_deprecated_string());
        });
}

}
