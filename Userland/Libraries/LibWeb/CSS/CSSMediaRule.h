/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSConditionRule.h>
#include <LibWeb/CSS/MediaList.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-conditional-3/#the-cssmediarule-interface
class CSSMediaRule final : public CSSConditionRule {
    AK_MAKE_NONCOPYABLE(CSSMediaRule);
    AK_MAKE_NONMOVABLE(CSSMediaRule);
    JS_OBJECT(CSSMediaRule, CSSConditionRule);

public:
    CSSMediaRule& impl() { return *this; }

    static CSSMediaRule* create(Bindings::WindowObject&, MediaList& media_queries, CSSRuleList&);
    explicit CSSMediaRule(Bindings::WindowObject&, MediaList&, CSSRuleList&);

    virtual ~CSSMediaRule() = default;

    virtual Type type() const override { return Type::Media; };

    virtual String condition_text() const override;
    virtual void set_condition_text(String) override;
    virtual bool condition_matches() const override { return m_media.matches(); }

    MediaList* media() const { return &m_media; }

    bool evaluate(HTML::Window const& window) { return m_media.evaluate(window); }

private:
    virtual void visit_edges(Cell::Visitor&) override;
    virtual String serialized() const override;

    MediaList& m_media;
};

template<>
inline bool CSSRule::fast_is<CSSMediaRule>() const { return type() == CSSRule::Type::Media; }

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::CSSMediaRule& object) { return &object; }
using CSSMediaRuleWrapper = Web::CSS::CSSMediaRule;
}
