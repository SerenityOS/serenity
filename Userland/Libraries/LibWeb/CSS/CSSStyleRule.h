/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSStyleRule final : public CSSRule {
    AK_MAKE_NONCOPYABLE(CSSStyleRule);
    AK_MAKE_NONMOVABLE(CSSStyleRule);

public:
    using WrapperType = Bindings::CSSStyleRuleWrapper;

    static NonnullRefPtr<CSSStyleRule> create(NonnullRefPtrVector<Selector>&& selectors, NonnullRefPtr<CSSStyleDeclaration>&& declaration)
    {
        return adopt_ref(*new CSSStyleRule(move(selectors), move(declaration)));
    }

    virtual ~CSSStyleRule() override;

    const NonnullRefPtrVector<Selector>& selectors() const { return m_selectors; }
    const CSSStyleDeclaration& declaration() const { return m_declaration; }

    virtual StringView class_name() const override { return "CSSStyleRule"; };
    virtual Type type() const override { return Type::Style; };

    String selector_text() const;
    void set_selector_text(StringView);

    CSSStyleDeclaration* style();

private:
    CSSStyleRule(NonnullRefPtrVector<Selector>&&, NonnullRefPtr<CSSStyleDeclaration>&&);

    virtual String serialized() const override;

    NonnullRefPtrVector<Selector> m_selectors;
    NonnullRefPtr<CSSStyleDeclaration> m_declaration;
};

template<>
inline bool CSSRule::fast_is<CSSStyleRule>() const { return type() == CSSRule::Type::Style; }

}
