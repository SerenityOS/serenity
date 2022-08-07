/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Iterator.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom/#the-cssrulelist-interface
class CSSRuleList : public Bindings::LegacyPlatformObject {
    JS_OBJECT(CSSRuleList, Bindings::LegacyPlatformObject);

public:
    CSSRuleList& impl() { return *this; }

    static CSSRuleList* create(Bindings::WindowObject&, NonnullRefPtrVector<CSSRule>&& rules);
    CSSRuleList(Bindings::WindowObject&, NonnullRefPtrVector<CSSRule>&&);
    ~CSSRuleList() = default;

    RefPtr<CSSRule> item(size_t index) const
    {
        if (index >= length())
            return nullptr;
        return m_rules[index];
    }
    size_t length() const { return m_rules.size(); }

    using ConstIterator = AK::SimpleIterator<AK::NonnullPtrVector<NonnullRefPtr<CSSRule>> const, CSSRule const>;
    using Iterator = AK::SimpleIterator<AK::NonnullPtrVector<NonnullRefPtr<CSSRule>>, CSSRule>;

    ConstIterator const begin() const { return m_rules.begin(); }
    Iterator begin() { return m_rules.begin(); }

    ConstIterator const end() const { return m_rules.end(); }
    Iterator end() { return m_rules.end(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    DOM::ExceptionOr<void> remove_a_css_rule(u32 index);
    DOM::ExceptionOr<unsigned> insert_a_css_rule(Variant<StringView, NonnullRefPtr<CSSRule>>, u32 index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    // Returns whether the match state of any media queries changed after evaluation.
    bool evaluate_media_queries(HTML::Window const&);

private:
    NonnullRefPtrVector<CSSRule> m_rules;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::GlobalObject&, Web::CSS::CSSRuleList& object) { return &object; }
using CSSRuleListWrapper = Web::CSS::CSSRuleList;
}
