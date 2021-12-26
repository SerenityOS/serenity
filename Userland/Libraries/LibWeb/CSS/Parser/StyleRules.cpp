/*
 * Copyright (c) 2020-2021, SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/AtStyleRule.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/QualifiedStyleRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>

namespace Web::CSS {

AtStyleRule::AtStyleRule() { }
AtStyleRule::~AtStyleRule() { }

DeclarationOrAtRule::DeclarationOrAtRule(AtStyleRule at)
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

QualifiedStyleRule::QualifiedStyleRule() { }
QualifiedStyleRule::~QualifiedStyleRule() { }

StyleBlockRule::StyleBlockRule() { }
StyleBlockRule::~StyleBlockRule() { }

StyleComponentValueRule::StyleComponentValueRule(ComponentType type)
    : m_type(type)
{
}
StyleComponentValueRule::~StyleComponentValueRule() { }

StyleDeclarationRule::StyleDeclarationRule() { }
StyleDeclarationRule::~StyleDeclarationRule() { }

StyleFunctionRule::StyleFunctionRule() { }
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
        builder.append(item.to_string());
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

String AtStyleRule::to_string() const
{
    StringBuilder builder;
    builder.append("@");
    builder.append(m_name);

    builder.append(QualifiedStyleRule::to_string());

    return builder.to_string();
}

String DeclarationOrAtRule::to_string() const
{
    StringBuilder builder;
    switch (m_type) {
    default:
    case DeclarationType::At:
        builder.append(m_at.to_string());
        break;
    case DeclarationType::Declaration:
        builder.append(m_declaration.to_string());
        break;
    }

    return builder.to_string();
}

String QualifiedStyleRule::to_string() const
{
    StringBuilder builder;

    append_raw(builder, " ", m_prelude);
    builder.append(m_block.to_string());

    return builder.to_string();
}

String StyleBlockRule::to_string() const
{
    StringBuilder builder;

    builder.append(m_token.bracket_string());
    append_raw(builder, ", ", m_values);
    builder.append(m_token.bracket_mirror_string());

    return builder.to_string();
}

String StyleComponentValueRule::to_string() const
{
    StringBuilder builder;

    switch (m_type) {
    case ComponentType::Token:
        builder.append(m_token.to_string());
        break;
    case ComponentType::Function:
        builder.append(m_function.to_string());
        break;
    case ComponentType::Block:
        builder.append(m_block.to_string());
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
    append_raw(builder, ", ", m_values);
    builder.append(");");

    return builder.to_string();
}
}
