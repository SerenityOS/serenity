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
#include <AK/RedBlackTree.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSKeyframesRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/TokenStream.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

struct MatchingRule {
    JS::GCPtr<CSSStyleRule const> rule;
    size_t style_sheet_index { 0 };
    size_t rule_index { 0 };
    size_t selector_index { 0 };
    u32 specificity { 0 };
    bool contains_pseudo_element { false };
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
    Vector<NonnullRefPtr<PropertyDependencyNode>> m_children;
    bool m_marked { false };
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
    explicit StyleComputer(DOM::Document&);
    ~StyleComputer();

    DOM::Document& document() { return m_document; }
    DOM::Document const& document() const { return m_document; }

    NonnullRefPtr<StyleProperties> create_document_style() const;

    ErrorOr<NonnullRefPtr<StyleProperties>> compute_style(DOM::Element&, Optional<CSS::Selector::PseudoElement> = {}) const;
    ErrorOr<RefPtr<StyleProperties>> compute_pseudo_element_style_if_needed(DOM::Element&, Optional<CSS::Selector::PseudoElement>) const;

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

    struct AnimationKey {
        CSS::CSSStyleDeclaration const* source_declaration;
        DOM::Element const* element;
    };

private:
    enum class ComputeStyleMode {
        Normal,
        CreatePseudoElementStyleIfNeeded,
    };

    ErrorOr<RefPtr<StyleProperties>> compute_style_impl(DOM::Element&, Optional<CSS::Selector::PseudoElement>, ComputeStyleMode) const;
    ErrorOr<void> compute_cascaded_values(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement>, bool& did_match_any_pseudo_element_rules, ComputeStyleMode) const;
    void compute_font(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void compute_defaulted_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    ErrorOr<void> absolutize_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void transform_box_type_if_needed(StyleProperties&, DOM::Element const&, Optional<CSS::Selector::PseudoElement>) const;

    void compute_defaulted_property_value(StyleProperties&, DOM::Element const*, CSS::PropertyID, Optional<CSS::Selector::PseudoElement>) const;

    RefPtr<StyleValue> resolve_unresolved_style_value(DOM::Element&, Optional<CSS::Selector::PseudoElement>, PropertyID, UnresolvedStyleValue const&) const;
    bool expand_variables(DOM::Element&, Optional<CSS::Selector::PseudoElement>, StringView property_name, HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const;
    bool expand_unresolved_values(DOM::Element&, StringView property_name, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const;

    template<typename Callback>
    void for_each_stylesheet(CascadeOrigin, Callback) const;

    CSSPixelRect viewport_rect() const;
    [[nodiscard]] Length::FontMetrics calculate_root_element_font_metrics(StyleProperties const&) const;
    CSSPixels parent_or_root_element_line_height(DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;

    struct MatchingRuleSet {
        Vector<MatchingRule> user_agent_rules;
        Vector<MatchingRule> author_rules;
    };

    void cascade_declarations(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement>, Vector<MatchingRule> const&, CascadeOrigin, Important) const;

    void build_rule_cache();
    void build_rule_cache_if_needed() const;

    JS::NonnullGCPtr<DOM::Document> m_document;

    struct AnimationKeyFrameSet {
        struct ResolvedKeyFrame {
            struct UseInitial { };
            Array<Variant<Empty, UseInitial, NonnullRefPtr<StyleValue const>>, to_underlying(last_property_id) + 1> resolved_properties {};
        };
        RedBlackTree<u64, ResolvedKeyFrame> keyframes_by_key;
    };

    struct RuleCache {
        HashMap<FlyString, Vector<MatchingRule>> rules_by_id;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_class;
        HashMap<FlyString, Vector<MatchingRule>> rules_by_tag_name;
        Vector<MatchingRule> other_rules;

        HashMap<FlyString, NonnullOwnPtr<AnimationKeyFrameSet>> rules_by_animation_keyframes;
    };

    NonnullOwnPtr<RuleCache> make_rule_cache_for_cascade_origin(CascadeOrigin);

    RuleCache const& rule_cache_for_cascade_origin(CascadeOrigin) const;

    void ensure_animation_timer() const;

    OwnPtr<RuleCache> m_author_rule_cache;
    OwnPtr<RuleCache> m_user_agent_rule_cache;

    class FontLoader;
    HashMap<FontFaceKey, NonnullOwnPtr<FontLoader>> m_loaded_fonts;

    Length::FontMetrics m_default_font_metrics;
    Length::FontMetrics m_root_element_font_metrics;

    constexpr static u64 AnimationKeyFrameKeyScaleFactor = 1000; // 0..100000

    enum class AnimationStepTransition {
        NoTransition,
        IdleOrBeforeToActive,
        IdleOrBeforeToAfter,
        ActiveToBefore,
        ActiveToActiveChangingTheIteration,
        ActiveToAfter,
        AfterToActive,
        AfterToBefore,
        Cancelled,
    };
    enum class AnimationState {
        Before,
        After,
        Idle,
        Active,
    };

    struct Animation {
        String name;
        CSS::Time duration;
        CSS::Time delay;
        Optional<size_t> iteration_count; // Infinite if not set.
        CSS::AnimationDirection direction;
        CSS::AnimationFillMode fill_mode;
        WeakPtr<DOM::Element> owning_element;

        CSS::Percentage progress { 0 };
        CSS::Time remaining_delay { 0, CSS::Time::Type::Ms };
        AnimationState current_state { AnimationState::Before };
        mutable Array<RefPtr<StyleValue const>, to_underlying(last_property_id) + 1> initial_state {};

        AnimationStepTransition step(CSS::Time const& time_step);
        ErrorOr<void> collect_into(StyleProperties&, RuleCache const&) const;
        bool is_done() const;
    };

    mutable HashMap<AnimationKey, NonnullOwnPtr<Animation>> m_active_animations;
    mutable RefPtr<Platform::Timer> m_animation_driver_timer;
};

}

template<>
struct AK::Traits<Web::CSS::StyleComputer::AnimationKey> : public AK::GenericTraits<Web::CSS::StyleComputer::AnimationKey> {
    static unsigned hash(Web::CSS::StyleComputer::AnimationKey const& k) { return pair_int_hash(ptr_hash(k.source_declaration), ptr_hash(k.element)); }
    static bool equals(Web::CSS::StyleComputer::AnimationKey const& a, Web::CSS::StyleComputer::AnimationKey const& b)
    {
        return a.element == b.element && a.source_declaration == b.source_declaration;
    }
};
