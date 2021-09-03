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
DeclarationOrAtRule::~DeclarationOrAtRule() { }

StyleRule::StyleRule(StyleRule::Type type)
    : m_type(type)
{
}
StyleRule::~StyleRule() { }

StyleBlockRule::StyleBlockRule() { }
StyleBlockRule::~StyleBlockRule() { }

StyleComponentValueRule::StyleComponentValueRule(Token token)
    : m_type(StyleComponentValueRule::ComponentType::Token)
    , m_token(token)
{
}
StyleComponentValueRule::StyleComponentValueRule(NonnullRefPtr<StyleFunctionRule> function)
    : m_type(StyleComponentValueRule::ComponentType::Function)
    , m_function(function)
{
}
StyleComponentValueRule::StyleComponentValueRule(NonnullRefPtr<StyleBlockRule> block)
    : m_type(StyleComponentValueRule::ComponentType::Block)
    , m_block(block)
{
}
StyleComponentValueRule::~StyleComponentValueRule() { }

StyleDeclarationRule::StyleDeclarationRule() { }
StyleDeclarationRule::~StyleDeclarationRule() { }

StyleFunctionRule::StyleFunctionRule(String name)
    : m_name(name)
{
}
StyleFunctionRule::~StyleFunctionRule() { }

template<class SeparatorType, class CollectionType>
void append_with_to_string(StringBuilder& builder, SeparatorType& separator, CollectionType& collection)
{
    bool first = true;
    for (auto& item : collection) {
        if (first)
            first = false;
        else
            builder.append(separator);
        builder.append(item.to_debug_string());
    }
}

template<class SeparatorType, class CollectionType>
void append_raw(StringBuilder& builder, SeparatorType& separator, CollectionType& collection)
{
    bool first = true;
    for (auto& item : collection) {
        if (first)
            first = false;
        else
            builder.append(separator);
        builder.append(item);
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

    if (m_type == Type::At) {
        builder.append("@");
        builder.append(m_name);
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
    append_with_to_string(builder, ", ", m_values);
    builder.append(m_token.bracket_mirror_string());

    return builder.to_string();
}

String StyleComponentValueRule::to_debug_string() const
{
    StringBuilder builder;

    switch (m_type) {
    case ComponentType::Token:
        builder.append("Token: ");
        builder.append(m_token.to_debug_string());
        break;
    case ComponentType::Function:
        builder.append("Function: ");
        builder.append(m_function->to_string());
        break;
    case ComponentType::Block:
        builder.append("Block: ");
        builder.append(m_block->to_string());
        break;
    }

    return builder.to_string();
}

String StyleDeclarationRule::to_string() const
{
    StringBuilder builder;

    builder.append(m_name);
    builder.append(": ");
    append_with_to_string(builder, " ", m_values);

    if (m_important)
        builder.append(" !important");

    return builder.to_string();
}

String StyleFunctionRule::to_string() const
{
    StringBuilder builder;

    builder.append(m_name);
    builder.append("(");
    append_with_to_string(builder, ", ", m_values);
    builder.append(")");

    return builder.to_string();
}
}
