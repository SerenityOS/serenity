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
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom/#the-cssrulelist-interface
class CSSRuleList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(CSSRuleList, Bindings::LegacyPlatformObject);

public:
    static CSSRuleList* create(HTML::Window&, JS::MarkedVector<CSSRule*> const&);
    static CSSRuleList* create_empty(HTML::Window&);

    explicit CSSRuleList(HTML::Window&);
    ~CSSRuleList() = default;

    CSSRule const* item(size_t index) const
    {
        if (index >= length())
            return nullptr;
        return &m_rules[index];
    }

    CSSRule* item(size_t index)
    {
        if (index >= length())
            return nullptr;
        return &m_rules[index];
    }

    size_t length() const { return m_rules.size(); }

    using ConstIterator = AK::SimpleIterator<Vector<CSSRule&> const, CSSRule const>;
    using Iterator = AK::SimpleIterator<Vector<CSSRule&>, CSSRule>;

    ConstIterator const begin() const { return m_rules.begin(); }
    Iterator begin() { return m_rules.begin(); }

    ConstIterator const end() const { return m_rules.end(); }
    Iterator end() { return m_rules.end(); }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    WebIDL::ExceptionOr<void> remove_a_css_rule(u32 index);
    WebIDL::ExceptionOr<unsigned> insert_a_css_rule(Variant<StringView, CSSRule*>, u32 index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    // Returns whether the match state of any media queries changed after evaluation.
    bool evaluate_media_queries(HTML::Window const&);

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Vector<CSSRule&> m_rules;
};

}
