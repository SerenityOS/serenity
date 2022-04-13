/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Selector.h>
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
    NonnullRefPtr<StyleProperties> compute_style(DOM::Element&, Optional<CSS::Selector::PseudoElement> = {}) const;

    // https://www.w3.org/TR/css-cascade/#origin
    enum class CascadeOrigin {
        Author,
        User,
        UserAgent,
        Animation,
        Transition,
    };

    Vector<MatchingRule> collect_matching_rules(DOM::Element const&, CascadeOrigin, Optional<CSS::Selector::PseudoElement>) const;

    void invalidate_rule_cache();

    Gfx::Font const& initial_font() const;

    void did_load_font(FlyString const& family_name);

    void load_fonts_from_sheet(CSSStyleSheet const&);

private:
    void compute_cascaded_values(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement>) const;
    void compute_font(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void compute_defaulted_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void absolutize_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void transform_box_type_if_needed(StyleProperties&, DOM::Element const&, Optional<CSS::Selector::PseudoElement>) const;

    void compute_defaulted_property_value(StyleProperties&, DOM::Element const*, CSS::PropertyID, Optional<CSS::Selector::PseudoElement>) const;

    RefPtr<StyleValue> resolve_unresolved_style_value(DOM::Element&, PropertyID, UnresolvedStyleValue const&) const;
    bool expand_unresolved_values(DOM::Element&, StringView property_name, HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Vector<Parser::ComponentValue> const& source, Vector<Parser::ComponentValue>& dest, size_t source_start_index) const;

    template<typename Callback>
    void for_each_stylesheet(CascadeOrigin, Callback) const;

    Gfx::IntRect viewport_rect() const;
    float root_element_font_size() const;

    struct MatchingRuleSet {
        Vector<MatchingRule> user_agent_rules;
        Vector<MatchingRule> author_rules;
    };

    void cascade_declarations(StyleProperties&, DOM::Element&, Vector<MatchingRule> const&, CascadeOrigin, Important important) const;

    void build_rule_cache();
    void build_rule_cache_if_needed() const;

    DOM::Document& m_document;

    struct RuleCache {
        HashMap<FlyString, Vector<MatchingRule>> rules_by_id;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_class;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_tag_name;
        HashMap<Selector::PseudoElement, Vector<MatchingRule>> rules_by_pseudo_element;
        Vector<MatchingRule> other_rules;
    };
    OwnPtr<RuleCache> m_rule_cache;

    class FontLoader;
    HashMap<String, NonnullOwnPtr<FontLoader>> m_loaded_fonts;
};

}
