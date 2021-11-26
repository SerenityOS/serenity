/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
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

public:
    static NonnullRefPtr<CSSMediaRule> create(NonnullRefPtr<MediaList>&& media_queries, NonnullRefPtrVector<CSSRule>&& rules)
    {
        return adopt_ref(*new CSSMediaRule(move(media_queries), move(rules)));
    }

    ~CSSMediaRule();

    virtual StringView class_name() const override { return "CSSMediaRule"; };
    virtual Type type() const override { return Type::Media; };

    virtual String condition_text() const override;
    virtual void set_condition_text(String) override;
    virtual bool condition_matches() const override { return m_media->matches(); }

    NonnullRefPtr<MediaList> const& media() const { return m_media; }

    bool evaluate(DOM::Window const& window) { return m_media->evaluate(window); }

private:
    explicit CSSMediaRule(NonnullRefPtr<MediaList>&&, NonnullRefPtrVector<CSSRule>&&);

    virtual String serialized() const override;

    NonnullRefPtr<MediaList> m_media;
};

template<>
inline bool CSSRule::fast_is<CSSMediaRule>() const { return type() == CSSRule::Type::Media; }

}
