/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
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

class PropertyDependencyNode : public RefCounted<PropertyDependencyNode> {
public:
    static NonnullRefPtr<PropertyDependencyNode> create(String name)
    {
        return adopt_ref(*new PropertyDependencyNode(move(name)));
    }

    void add_child(NonnullRefPtr<PropertyDependencyNode>);
    bool has_cycles();

private:
    explicit PropertyDependencyNode(String name);

    String m_name;
    NonnullRefPtrVector<PropertyDependencyNode> m_children;
    bool m_marked { false };
};

class StyleComputer {
public:
    explicit StyleComputer(DOM::Document&);
    ~StyleComputer();

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

    NonnullRefPtr<StyleProperties> create_document_style() const;
    NonnullRefPtr<StyleProperties> compute_style(DOM::Element&) const;

    // https://www.w3.org/TR/css-cascade/#origin
    enum class CascadeOrigin {
        Any, // FIXME: This is not part of the spec. Get rid of it.
        Author,
        User,
        UserAgent,
        Animation,
        Transition,
    };

    Vector<MatchingRule> collect_matching_rules(DOM::Element const&, CascadeOrigin = CascadeOrigin::Any) const;
    void sort_matching_rules(Vector<MatchingRule>&) const;
    struct CustomPropertyResolutionTuple {
        Optional<StyleProperty> style {};
        u32 specificity { 0 };
    };
    CustomPropertyResolutionTuple resolve_custom_property_with_specificity(DOM::Element&, String const&) const;
    Optional<StyleProperty> resolve_custom_property(DOM::Element&, String const&) const;

private:
    void compute_cascaded_values(StyleProperties&, DOM::Element&) const;
    void compute_font(StyleProperties&, DOM::Element const*) const;
    void compute_defaulted_values(StyleProperties&, DOM::Element const*) const;
    void absolutize_values(StyleProperties&, DOM::Element const*) const;
    void transform_box_type_if_needed(StyleProperties&, DOM::Element const&) const;

    void compute_defaulted_property_value(StyleProperties&, DOM::Element const*, CSS::PropertyID) const;

    RefPtr<StyleValue> resolve_unresolved_style_value(DOM::Element&, PropertyID, UnresolvedStyleValue const&) const;
    bool expand_unresolved_values(DOM::Element&, StringView property_name, HashMap<String, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Vector<StyleComponentValueRule> const& source, Vector<StyleComponentValueRule>& dest, size_t source_start_index = 0) const;

    template<typename Callback>
    void for_each_stylesheet(CascadeOrigin, Callback) const;

    struct MatchingRuleSet {
        Vector<MatchingRule> user_agent_rules;
        Vector<MatchingRule> author_rules;
    };

    void cascade_declarations(StyleProperties&, DOM::Element&, Vector<MatchingRule> const&, CascadeOrigin, bool important) const;

    DOM::Document& m_document;
};

}
