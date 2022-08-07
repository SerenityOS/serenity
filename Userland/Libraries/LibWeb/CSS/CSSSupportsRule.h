/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/CSS/CSSConditionRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/Supports.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-conditional-3/#the-csssupportsrule-interface
class CSSSupportsRule final : public CSSConditionRule {
    JS_OBJECT(CSSSupportsRule, CSSConditionRule);
    AK_MAKE_NONCOPYABLE(CSSSupportsRule);
    AK_MAKE_NONMOVABLE(CSSSupportsRule);

public:
    static CSSSupportsRule* create(Bindings::WindowObject&, NonnullRefPtr<Supports>&&, CSSRuleList&);
    explicit CSSSupportsRule(Bindings::WindowObject&, NonnullRefPtr<Supports>&&, CSSRuleList&);

    virtual ~CSSSupportsRule() = default;

    CSSSupportsRule& impl() { return *this; }

    virtual Type type() const override { return Type::Supports; };

    String condition_text() const override;
    void set_condition_text(String) override;
    virtual bool condition_matches() const override { return m_supports->matches(); }

private:
    virtual String serialized() const override;

    NonnullRefPtr<Supports> m_supports;
};

template<>
inline bool CSSRule::fast_is<CSSSupportsRule>() const { return type() == CSSRule::Type::Supports; }

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::CSSSupportsRule& object) { return &object; }
using CSSSupportsRuleWrapper = Web::CSS::CSSSupportsRule;
}
