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
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

struct MatchingRule {
    JS::GCPtr<CSSStyleRule const> rule;
    JS::GCPtr<CSSStyleSheet const> sheet;
    size_t style_sheet_index { 0 };
    size_t rule_index { 0 };
    size_t selector_index { 0 };
    u32 specificity { 0 };
    bool contains_pseudo_element { false };
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

    FontCache& font_cache() const { return m_font_cache; }

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

    RefPtr<Gfx::Font const> compute_font_for_style_values(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element, StyleValue const& font_family, StyleValue const& font_size, StyleValue const& font_style, StyleValue const& font_weight, StyleValue const& font_stretch) const;

    struct AnimationKey {
        CSS::CSSStyleDeclaration const* source_declaration;
        DOM::Element const* element;
    };

    struct AnimationTiming {
        struct Linear { };
        struct CubicBezier {
            // Regular parameters
            double x1;
            double y1;
            double x2;
            double y2;

            struct CachedSample {
                double x;
                double y;
                double t;
            };
            mutable Vector<CachedSample, 64> m_cached_x_samples = {};

            CachedSample sample_around(double x) const;
            bool operator==(CubicBezier const& other) const
            {
                return x1 == other.x1 && y1 == other.y1 && x2 == other.x2 && y2 == other.y2;
            }
        };
        struct Steps {
            size_t number_of_steps;
            bool jump_at_start;
            bool jump_at_end;
        };

        Variant<Linear, CubicBezier, Steps> timing_function;
    };

private:
    enum class ComputeStyleMode {
        Normal,
        CreatePseudoElementStyleIfNeeded,
    };

    class FontLoader;
    struct MatchingFontCandidate;

    ErrorOr<RefPtr<StyleProperties>> compute_style_impl(DOM::Element&, Optional<CSS::Selector::PseudoElement>, ComputeStyleMode) const;
    ErrorOr<void> compute_cascaded_values(StyleProperties&, DOM::Element&, Optional<CSS::Selector::PseudoElement>, bool& did_match_any_pseudo_element_rules, ComputeStyleMode) const;
    static RefPtr<Gfx::Font const> find_matching_font_weight_ascending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive);
    static RefPtr<Gfx::Font const> find_matching_font_weight_descending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive);
    RefPtr<Gfx::Font const> font_matching_algorithm(FontFaceKey const& key, float font_size_in_pt) const;
    void compute_font(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void compute_defaulted_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void absolutize_values(StyleProperties&, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;
    void transform_box_type_if_needed(StyleProperties&, DOM::Element const&, Optional<CSS::Selector::PseudoElement>) const;

    void compute_defaulted_property_value(StyleProperties&, DOM::Element const*, CSS::PropertyID, Optional<CSS::Selector::PseudoElement>) const;

    void set_all_properties(DOM::Element&, Optional<CSS::Selector::PseudoElement>, StyleProperties&, StyleValue const&, DOM::Document&, CSS::CSSStyleDeclaration const*, StyleProperties::PropertyValues const& properties_for_revert) const;

    template<typename Callback>
    void for_each_stylesheet(CascadeOrigin, Callback) const;

    CSSPixelRect viewport_rect() const;
    [[nodiscard]] Length::FontMetrics calculate_root_element_font_metrics(StyleProperties const&) const;
    CSSPixels parent_or_root_element_line_height(DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const;

    struct MatchingRuleSet {
        Vector<MatchingRule> user_agent_rules;
        Vector<MatchingRule> user_rules;
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
    OwnPtr<RuleCache> m_user_rule_cache;
    OwnPtr<RuleCache> m_user_agent_rule_cache;
    JS::Handle<CSSStyleSheet> m_user_style_sheet;

    mutable FontCache m_font_cache;

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

    struct AnimationStateSnapshot {
        Array<RefPtr<StyleValue const>, to_underlying(last_property_id) + 1> state;
    };

    struct Animation {
        String name;
        Optional<CSS::Time> duration; // "auto" if not set.
        CSS::Time delay;
        Optional<size_t> iteration_count; // Infinite if not set.
        AnimationTiming timing_function;
        CSS::AnimationDirection direction;
        CSS::AnimationFillMode fill_mode;
        WeakPtr<DOM::Element> owning_element;

        CSS::Percentage progress { 0 };
        CSS::Time remaining_delay { 0, CSS::Time::Type::Ms };
        AnimationState current_state { AnimationState::Before };
        size_t current_iteration { 1 };

        mutable AnimationStateSnapshot initial_state {};
        mutable OwnPtr<AnimationStateSnapshot> active_state_if_fill_forward {};

        AnimationStepTransition step(CSS::Time const& time_step);
        ErrorOr<void> collect_into(StyleProperties&, RuleCache const&) const;
        bool is_done() const;

    private:
        float compute_output_progress(float input_progress) const;
        bool is_animating_backwards() const;
    };

    mutable HashMap<AnimationKey, NonnullOwnPtr<Animation>> m_active_animations;
    mutable HashMap<AnimationKey, OwnPtr<AnimationStateSnapshot>> m_finished_animations; // If fill-mode is forward/both, this is non-null and contains the final state.
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
