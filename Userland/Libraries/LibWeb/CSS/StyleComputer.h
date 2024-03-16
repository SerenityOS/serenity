/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibWeb/Animations/KeyframeEffect.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSKeyframesRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-cascade/#origin
enum class CascadeOrigin {
    Author,
    User,
    UserAgent,
    Animation,
    Transition,
};

struct MatchingRule {
    CascadeOrigin cascade_origin;
    JS::GCPtr<DOM::ShadowRoot const> shadow_root;
    JS::GCPtr<CSSStyleRule const> rule;
    JS::GCPtr<CSSStyleSheet const> sheet;
    size_t style_sheet_index { 0 };
    size_t rule_index { 0 };
    size_t selector_index { 0 };

    u32 specificity { 0 };
    bool contains_pseudo_element { false };
    bool contains_root_pseudo_class { false };
};

struct FontFaceKey {
    FlyString family_name;
    int weight { 0 };
    int slope { 0 };

    [[nodiscard]] u32 hash() const { return pair_int_hash(family_name.hash(), pair_int_hash(weight, slope)); }
    [[nodiscard]] bool operator==(FontFaceKey const&) const = default;
};

class StyleComputer {
public:
    static void for_each_property_expanding_shorthands(PropertyID, StyleValue const&, Function<void(PropertyID, StyleValue const&)> const& set_longhand_property);
    static void set_property_expanding_shorthands(StyleProperties&, PropertyID, StyleValue const&, CSS::CSSStyleDeclaration const*, StyleProperties::PropertyValues const& properties_for_revert, StyleProperties::Important = StyleProperties::Important::No);
    static NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID, DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type> = {});

    explicit StyleComputer(DOM::Document&);
    ~StyleComputer();

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

    NonnullRefPtr<StyleProperties> create_document_style() const;

    NonnullRefPtr<StyleProperties> compute_style(DOM::Element&, Optional<CSS::Selector::PseudoElement::Type> = {}) const;
    RefPtr<StyleProperties> compute_pseudo_element_style_if_needed(DOM::Element&, Optional<CSS::Selector::PseudoElement::Type>) const;

    Vector<MatchingRule> collect_matching_rules(DOM::Element const&, CascadeOrigin, Optional<CSS::Selector::PseudoElement::Type>) const;

    void invalidate_rule_cache();

    Gfx::Font const& initial_font() const;

    void did_load_font(FlyString const& family_name);

    void load_fonts_from_sheet(CSSStyleSheet const&);

    RefPtr<Gfx::FontCascadeList const> compute_font_for_style_values(DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, StyleValue const& font_family, StyleValue const& font_size, StyleValue const& font_style, StyleValue const& font_weight, StyleValue const& font_stretch, int math_depth = 0) const;

    void set_viewport_rect(Badge<DOM::Document>, CSSPixelRect const& viewport_rect) { m_viewport_rect = viewport_rect; }

    enum class AnimationRefresh {
        No,
        Yes,
    };
    void collect_animation_into(JS::NonnullGCPtr<Animations::KeyframeEffect> animation, StyleProperties& style_properties, AnimationRefresh = AnimationRefresh::No) const;

private:
    enum class ComputeStyleMode {
        Normal,
        CreatePseudoElementStyleIfNeeded,
    };

    class FontLoader;
    struct MatchingFontCandidate;

    RefPtr<StyleProperties> compute_style_impl(DOM::Element&, Optional<CSS::Selector::PseudoElement::Type>, ComputeStyleMode) const;
    void compute_cascaded_values(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement::Type>, bool& did_match_any_pseudo_element_rules, ComputeStyleMode) const;
    static RefPtr<Gfx::FontCascadeList const> find_matching_font_weight_ascending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive);
    static RefPtr<Gfx::FontCascadeList const> find_matching_font_weight_descending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive);
    RefPtr<Gfx::FontCascadeList const> font_matching_algorithm(FontFaceKey const& key, float font_size_in_pt) const;
    void compute_font(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type>) const;
    void compute_math_depth(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type>) const;
    void compute_defaulted_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type>) const;
    void absolutize_values(StyleProperties&) const;
    void resolve_effective_overflow_values(StyleProperties&) const;
    void transform_box_type_if_needed(StyleProperties&, DOM::Element const&, Optional<CSS::Selector::PseudoElement::Type>) const;

    void compute_defaulted_property_value(StyleProperties&, DOM::Element const*, CSS::PropertyID, Optional<CSS::Selector::PseudoElement::Type>) const;

    void set_all_properties(DOM::Element&, Optional<CSS::Selector::PseudoElement::Type>, StyleProperties&, StyleValue const&, DOM::Document&, CSS::CSSStyleDeclaration const*, StyleProperties::PropertyValues const& properties_for_revert, StyleProperties::Important = StyleProperties::Important::No) const;

    template<typename Callback>
    void for_each_stylesheet(CascadeOrigin, Callback) const;

    [[nodiscard]] CSSPixelRect viewport_rect() const { return m_viewport_rect; }

    [[nodiscard]] Length::FontMetrics calculate_root_element_font_metrics(StyleProperties const&) const;

    struct MatchingRuleSet {
        Vector<MatchingRule> user_agent_rules;
        Vector<MatchingRule> user_rules;
        Vector<MatchingRule> author_rules;
    };

    void cascade_declarations(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement::Type>, Vector<MatchingRule> const&, CascadeOrigin, Important) const;

    void build_rule_cache();
    void build_rule_cache_if_needed() const;

    JS::NonnullGCPtr<DOM::Document> m_document;

    struct RuleCache {
        HashMap<FlyString, Vector<MatchingRule>> rules_by_id;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_class;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_tag_name;
        Vector<MatchingRule> pseudo_element_rules;
        Vector<MatchingRule> root_rules;
        Vector<MatchingRule> other_rules;

        HashMap<FlyString, NonnullRefPtr<Animations::KeyframeEffect::KeyFrameSet>> rules_by_animation_keyframes;
    };

    NonnullOwnPtr<RuleCache> make_rule_cache_for_cascade_origin(CascadeOrigin);

    RuleCache const& rule_cache_for_cascade_origin(CascadeOrigin) const;

    OwnPtr<RuleCache> m_author_rule_cache;
    OwnPtr<RuleCache> m_user_rule_cache;
    OwnPtr<RuleCache> m_user_agent_rule_cache;
    JS::Handle<CSSStyleSheet> m_user_style_sheet;

    using FontLoaderList = Vector<NonnullOwnPtr<FontLoader>>;
    HashMap<FontFaceKey, FontLoaderList> m_loaded_fonts;

    Length::FontMetrics m_default_font_metrics;
    Length::FontMetrics m_root_element_font_metrics;

    CSSPixelRect m_viewport_rect;
};

}
