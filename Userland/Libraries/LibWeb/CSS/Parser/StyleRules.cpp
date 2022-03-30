/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

DeclarationOrAtRule::DeclarationOrAtRule(RefPtr<StyleRule> at)
    : m_type(DeclarationType::At)
    , m_at(move(at))
{
}
DeclarationOrAtRule::DeclarationOrAtRule(StyleDeclarationRule declaration)
    : m_type(DeclarationType::Declaration)
    , m_declaration(move(declaration))
{
}
DeclarationOrAtRule::~DeclarationOrAtRule() = default;

StyleRule::StyleRule(StyleRule::Type type)
    : m_type(type)
{
}
StyleRule::~StyleRule() = default;

StyleBlockRule::StyleBlockRule() = default;
StyleBlockRule::~StyleBlockRule() = default;

StyleComponentValueRule::StyleComponentValueRule(Token token)
    : m_value(token)
{
}
StyleComponentValueRule::StyleComponentValueRule(NonnullRefPtr<StyleFunctionRule> function)
    : m_value(function)
{
}
StyleComponentValueRule::StyleComponentValueRule(NonnullRefPtr<StyleBlockRule> block)
    : m_value(block)
{
}
StyleComponentValueRule::~StyleComponentValueRule() = default;

StyleDeclarationRule::StyleDeclarationRule() = default;
StyleDeclarationRule::~StyleDeclarationRule() = default;

StyleFunctionRule::StyleFunctionRule(String name)
    : m_name(move(name))
{
}

StyleFunctionRule::StyleFunctionRule(String name, Vector<StyleComponentValueRule>&& values)
    : m_name(move(name))
    , m_values(move(values))
{
}
StyleFunctionRule::~StyleFunctionRule() = default;

template<class SeparatorType, class CollectionType>
void append_with_to_string(StringBuilder& builder, SeparatorType& separator, CollectionType& collection)
{
    bool first = true;
    for (auto& item : collection) {
        if (first)
            first = false;
        else
            builder.append(separator);
        builder.append(item.to_string());
    }
}

String DeclarationOrAtRule::to_string() const
{
    StringBuilder builder;
    switch (m_type) {
    default:
    case DeclarationType::At:
        builder.append(m_at->to_string());
        break;
    case DeclarationType::Declaration:
        builder.append(m_declaration.to_string());
        break;
    }

    return builder.to_string();
}

String StyleRule::to_string() const
{
    StringBuilder builder;

    if (is_at_rule()) {
        builder.append("@");
        serialize_an_identifier(builder, m_at_rule_name);
    }

    append_with_to_string(builder, " ", m_prelude);

    if (m_block)
        builder.append(m_block->to_string());
    else
        builder.append(';');

    return builder.to_string();
}

String StyleBlockRule::to_string() const
{
    StringBuilder builder;

    builder.append(m_token.bracket_string());
    append_with_to_string(builder, " ", m_values);
    builder.append(m_token.bracket_mirror_string());

    return builder.to_string();
}

String StyleComponentValueRule::to_string() const
{
    return m_value.visit(
        [](Token const& token) { return token.to_string(); },
        [](NonnullRefPtr<StyleBlockRule> const& block) { return block->to_string(); },
        [](NonnullRefPtr<StyleFunctionRule> const& function) { return function->to_string(); });
}

String StyleComponentValueRule::to_debug_string() const
{
    return m_value.visit(
        [](Token const& token) {
            return String::formatted("Token: {}", token.to_debug_string());
        },
        [](NonnullRefPtr<StyleBlockRule> const& block) {
            return String::formatted("Function: {}", block->to_string());
        },
        [](NonnullRefPtr<StyleFunctionRule> const& function) {
            return String::formatted("Block: {}", function->to_string());
        });
}

String StyleDeclarationRule::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append(": ");
    append_with_to_string(builder, " ", m_values);

    if (m_important == Important::Yes)
        builder.append(" !important");

    return builder.to_string();
}

String StyleFunctionRule::to_string() const
{
    StringBuilder builder;

    serialize_an_identifier(builder, m_name);
    builder.append("(");
    append_with_to_string(builder, " ", m_values);
    builder.append(")");

    return builder.to_string();
}
}
