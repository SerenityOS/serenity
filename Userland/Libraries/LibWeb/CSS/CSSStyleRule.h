/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullRefPtr.h>
#include <YAK/NonnullRefPtrVector.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSStyleRule : public CSSRule {
    YAK_MAKE_NONCOPYABLE(CSSStyleRule);
    YAK_MAKE_NONMOVABLE(CSSStyleRule);

public:
    static NonnullRefPtr<CSSStyleRule> create(NonnullRefPtrVector<Selector>&& selectors, NonnullRefPtr<CSSStyleDeclaration>&& declaration)
    {
        return adopt_ref(*new CSSStyleRule(move(selectors), move(declaration)));
    }

    ~CSSStyleRule();

    const NonnullRefPtrVector<Selector>& selectors() const { return m_selectors; }
    const CSSStyleDeclaration& declaration() const { return m_declaration; }

    virtual StringView class_name() const { return "CSSStyleRule"; };
    virtual Type type() const { return Type::Style; };

private:
    CSSStyleRule(NonnullRefPtrVector<Selector>&&, NonnullRefPtr<CSSStyleDeclaration>&&);

    NonnullRefPtrVector<Selector> m_selectors;
    NonnullRefPtr<CSSStyleDeclaration> m_declaration;
};

template<>
inline bool CSSRule::fast_is<CSSStyleRule>() const { return type() == CSSRule::Type::Style; }

}
