/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

struct MatchingRule {
    RefPtr<CSSStyleRule> rule;
    size_t style_sheet_index { 0 };
    size_t rule_index { 0 };
    size_t selector_index { 0 };
    u32 specificity { 0 };
};

class StyleResolver {
public:
    explicit StyleResolver(DOM::Document&);
    ~StyleResolver();

    DOM::Document& document() { return m_document; }
    const DOM::Document& document() const { return m_document; }

    NonnullRefPtr<StyleProperties> resolve_style(DOM::Element&) const;

    Vector<MatchingRule> collect_matching_rules(const DOM::Element&) const;
    void sort_matching_rules(Vector<MatchingRule>&) const;
    struct CustomPropertyResolutionTuple {
        Optional<StyleProperty> style {};
        u32 specificity { 0 };
    };
    CustomPropertyResolutionTuple resolve_custom_property_with_specificity(DOM::Element&, const String&) const;
    Optional<StyleProperty> resolve_custom_property(DOM::Element&, const String&) const;

    static bool is_inherited_property(CSS::PropertyID);

private:
    template<typename Callback>
    void for_each_stylesheet(Callback) const;

    DOM::Document& m_document;
};

}
