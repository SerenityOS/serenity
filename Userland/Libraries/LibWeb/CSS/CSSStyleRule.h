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
    JS_OBJECT(CSSStyleRule, CSSRule);
    AK_MAKE_NONCOPYABLE(CSSStyleRule);
    AK_MAKE_NONMOVABLE(CSSStyleRule);

public:
    static CSSStyleRule* create(Bindings::WindowObject&, NonnullRefPtrVector<Selector>&&, NonnullRefPtr<CSSStyleDeclaration>&&);
    CSSStyleRule(Bindings::WindowObject&, NonnullRefPtrVector<Selector>&&, NonnullRefPtr<CSSStyleDeclaration>&&);

    virtual ~CSSStyleRule() override = default;

    CSSStyleRule& impl() { return *this; }

    NonnullRefPtrVector<Selector> const& selectors() const { return m_selectors; }
    CSSStyleDeclaration const& declaration() const { return m_declaration; }

    virtual Type type() const override { return Type::Style; };

    String selector_text() const;
    void set_selector_text(StringView);

    CSSStyleDeclaration* style();

private:
    virtual String serialized() const override;

    NonnullRefPtrVector<Selector> m_selectors;
    NonnullRefPtr<CSSStyleDeclaration> m_declaration;
};

template<>
inline bool CSSRule::fast_is<CSSStyleRule>() const { return type() == CSSRule::Type::Style; }

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::CSSStyleRule& object) { return &object; }
using CSSStyleRuleWrapper = Web::CSS::CSSStyleRule;
}
