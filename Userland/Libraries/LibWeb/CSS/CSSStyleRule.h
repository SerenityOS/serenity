/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSStyleRule final : public CSSRule {
    WEB_PLATFORM_OBJECT(CSSStyleRule, CSSRule);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSStyleRule>> create(JS::Realm&, Vector<NonnullRefPtr<Selector>>&&, CSSStyleDeclaration&);

    virtual ~CSSStyleRule() override = default;

    Vector<NonnullRefPtr<Selector>> const& selectors() const { return m_selectors; }
    CSSStyleDeclaration const& declaration() const { return m_declaration; }

    virtual Type type() const override { return Type::Style; };

    DeprecatedString selector_text() const;
    void set_selector_text(StringView);

    CSSStyleDeclaration* style();

private:
    CSSStyleRule(JS::Realm&, Vector<NonnullRefPtr<Selector>>&&, CSSStyleDeclaration&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;
    virtual DeprecatedString serialized() const override;

    Vector<NonnullRefPtr<Selector>> m_selectors;
    JS::NonnullGCPtr<CSSStyleDeclaration> m_declaration;
};

template<>
inline bool CSSRule::fast_is<CSSStyleRule>() const { return type() == CSSRule::Type::Style; }

}
