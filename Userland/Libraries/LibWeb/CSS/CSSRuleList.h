/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Iterator.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom/#the-cssrulelist-interface
class CSSRuleList
    : public RefCounted<CSSRuleList>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CSSRuleListWrapper;

    static NonnullRefPtr<CSSRuleList> create(NonnullRefPtrVector<CSSRule>&& rules)
    {
        return adopt_ref(*new CSSRuleList(move(rules)));
    }
    ~CSSRuleList();

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

    bool is_supported_property_index(u32 index) const;

    DOM::ExceptionOr<void> remove_a_css_rule(u32 index);
    DOM::ExceptionOr<unsigned> insert_a_css_rule(NonnullRefPtr<CSSRule>, u32 index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    void evaluate_media_queries(DOM::Window const&);

private:
    explicit CSSRuleList(NonnullRefPtrVector<CSSRule>&&);

    NonnullRefPtrVector<CSSRule> m_rules;
};

}
