/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/FontFace.h>
#include <LibWeb/CSS/GeneralEnclosed.h>
#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/CSS/Parser/Block.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/Declaration.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Function.h>
#include <LibWeb/CSS/Parser/Rule.h>
#include <LibWeb/CSS/Parser/TokenStream.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <LibWeb/CSS/Position.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/Ratio.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>
#include <LibWeb/CSS/StyleValues/CalculatedStyleValue.h>
#include <LibWeb/CSS/Supports.h>
#include <LibWeb/CSS/UnicodeRange.h>
#include <LibWeb/Forward.h>

namespace Web::CSS::Parser {

class ParsingContext {
public:
    explicit ParsingContext(JS::Realm&);
    explicit ParsingContext(DOM::Document const&);
    explicit ParsingContext(DOM::Document const&, AK::URL);
    explicit ParsingContext(DOM::ParentNode&);

    bool in_quirks_mode() const;
    DOM::Document const* document() const { return m_document; }
    HTML::Window const* window() const;
    AK::URL complete_url(StringView) const;

    PropertyID current_property_id() const { return m_current_property_id; }
    void set_current_property_id(PropertyID property_id) { m_current_property_id = property_id; }

    JS::Realm& realm() const { return m_realm; }

private:
    JS::NonnullGCPtr<JS::Realm> m_realm;
    JS::GCPtr<DOM::Document const> m_document;
    PropertyID m_current_property_id { PropertyID::Invalid };
    AK::URL m_url;
};

class Parser {
public:
    static ErrorOr<Parser> create(ParsingContext const&, StringView input, StringView encoding = "utf-8"sv);

    Parser(Parser&&);

    CSSStyleSheet* parse_as_css_stylesheet(Optional<AK::URL> location);
    ElementInlineCSSStyleDeclaration* parse_as_style_attribute(DOM::Element&);
    CSSRule* parse_as_css_rule();
    Optional<StyleProperty> parse_as_supports_condition();

    enum class SelectorParsingMode {
        Standard,
        // `<forgiving-selector-list>` and `<forgiving-relative-selector-list>`
        // are handled with this parameter, not as separate functions.
        // https://drafts.csswg.org/selectors/#forgiving-selector
        Forgiving
    };
    // Contrary to the name, these parse a comma-separated list of selectors, according to the spec.
    Optional<SelectorList> parse_as_selector(SelectorParsingMode = SelectorParsingMode::Standard);
    Optional<SelectorList> parse_as_relative_selector(SelectorParsingMode = SelectorParsingMode::Standard);

    Vector<NonnullRefPtr<MediaQuery>> parse_as_media_query_list();
    RefPtr<MediaQuery> parse_as_media_query();

    RefPtr<Supports> parse_as_supports();

    ErrorOr<RefPtr<StyleValue>> parse_as_css_value(PropertyID);

    static ErrorOr<RefPtr<StyleValue>> parse_css_value(Badge<StyleComputer>, ParsingContext const&, PropertyID, Vector<ComponentValue> const&);
    static ErrorOr<RefPtr<CalculatedStyleValue>> parse_calculated_value(Badge<StyleComputer>, ParsingContext const&, Vector<ComponentValue> const&);

    [[nodiscard]] LengthOrCalculated parse_as_sizes_attribute();

private:
    Parser(ParsingContext const&, Vector<Token>);

    enum class ParseError {
        IncludesIgnoredVendorPrefix,
        InternalError,
        SyntaxError,
    };
    template<typename T>
    using ParseErrorOr = ErrorOr<T, ParseError>;

    // "Parse a stylesheet" is intended to be the normal parser entry point, for parsing stylesheets.
    struct ParsedStyleSheet {
        Optional<AK::URL> location;
        Vector<NonnullRefPtr<Rule>> rules;
    };
    template<typename T>
    ParsedStyleSheet parse_a_stylesheet(TokenStream<T>&, Optional<AK::URL> location);

    // "Parse a list of rules" is intended for the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    template<typename T>
    Vector<NonnullRefPtr<Rule>> parse_a_list_of_rules(TokenStream<T>&);

    // "Parse a rule" is intended for use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    template<typename T>
    RefPtr<Rule> parse_a_rule(TokenStream<T>&);

    // "Parse a declaration" is used in @supports conditions. [CSS3-CONDITIONAL]
    template<typename T>
    Optional<Declaration> parse_a_declaration(TokenStream<T>&);

    template<typename T>
    Vector<DeclarationOrAtRule> parse_a_style_blocks_contents(TokenStream<T>&);

    // "Parse a list of declarations" is for the contents of a style attribute, which parses text into the contents of a single style rule.
    template<typename T>
    Vector<DeclarationOrAtRule> parse_a_list_of_declarations(TokenStream<T>&);

    // "Parse a component value" is for things that need to consume a single value, like the parsing rules for attr().
    template<typename T>
    Optional<ComponentValue> parse_a_component_value(TokenStream<T>&);

    // "Parse a list of component values" is for the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    template<typename T>
    Vector<ComponentValue> parse_a_list_of_component_values(TokenStream<T>&);

    template<typename T>
    Vector<Vector<ComponentValue>> parse_a_comma_separated_list_of_component_values(TokenStream<T>&);

    enum class SelectorType {
        Standalone,
        Relative
    };
    template<typename T>
    ParseErrorOr<SelectorList> parse_a_selector_list(TokenStream<T>&, SelectorType, SelectorParsingMode = SelectorParsingMode::Standard);

    template<typename T>
    Vector<NonnullRefPtr<MediaQuery>> parse_a_media_query_list(TokenStream<T>&);
    template<typename T>
    RefPtr<Supports> parse_a_supports(TokenStream<T>&);

    Optional<Selector::SimpleSelector::ANPlusBPattern> parse_a_n_plus_b_pattern(TokenStream<ComponentValue>&);

    enum class TopLevel {
        No,
        Yes
    };
    template<typename T>
    [[nodiscard]] Vector<NonnullRefPtr<Rule>> consume_a_list_of_rules(TokenStream<T>&, TopLevel);
    template<typename T>
    [[nodiscard]] NonnullRefPtr<Rule> consume_an_at_rule(TokenStream<T>&);
    template<typename T>
    RefPtr<Rule> consume_a_qualified_rule(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] Vector<DeclarationOrAtRule> consume_a_style_blocks_contents(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] Vector<DeclarationOrAtRule> consume_a_list_of_declarations(TokenStream<T>&);
    template<typename T>
    Optional<Declaration> consume_a_declaration(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] ComponentValue consume_a_component_value(TokenStream<T>&);
    template<typename T>
    NonnullRefPtr<Block> consume_a_simple_block(TokenStream<T>&);
    template<typename T>
    NonnullRefPtr<Function> consume_a_function(TokenStream<T>&);

    Optional<GeneralEnclosed> parse_general_enclosed(TokenStream<ComponentValue>&);

    CSSRule* parse_font_face_rule(TokenStream<ComponentValue>&);
    Vector<FontFace::Source> parse_font_face_src(TokenStream<ComponentValue>&);

    CSSRule* convert_to_rule(NonnullRefPtr<Rule>);
    PropertyOwningCSSStyleDeclaration* convert_to_style_declaration(Vector<DeclarationOrAtRule> const& declarations);
    Optional<StyleProperty> convert_to_style_property(Declaration const&);

    class Dimension {
    public:
        Dimension(Angle&& value)
            : m_value(move(value))
        {
        }

        Dimension(Frequency&& value)
            : m_value(move(value))
        {
        }

        Dimension(Length&& value)
            : m_value(move(value))
        {
        }
        Dimension(Percentage&& value)
            : m_value(move(value))
        {
        }

        Dimension(Resolution&& value)
            : m_value(move(value))
        {
        }

        Dimension(Time&& value)
            : m_value(move(value))
        {
        }

        bool is_angle() const;
        Angle angle() const;

        bool is_angle_percentage() const;
        AnglePercentage angle_percentage() const;

        bool is_frequency() const;
        Frequency frequency() const;

        bool is_frequency_percentage() const;
        FrequencyPercentage frequency_percentage() const;

        bool is_length() const;
        Length length() const;

        bool is_length_percentage() const;
        LengthPercentage length_percentage() const;

        bool is_percentage() const;
        Percentage percentage() const;

        bool is_resolution() const;
        Resolution resolution() const;

        bool is_time() const;
        Time time() const;

        bool is_time_percentage() const;
        TimePercentage time_percentage() const;

    private:
        Variant<Angle, Frequency, Length, Percentage, Resolution, Time> m_value;
    };
    Optional<Dimension> parse_dimension(ComponentValue const&);
    Optional<Color> parse_rgb_or_hsl_color(StringView function_name, Vector<ComponentValue> const&);
    Optional<Color> parse_color(ComponentValue const&);
    Optional<Length> parse_length(ComponentValue const&);
    [[nodiscard]] Optional<LengthOrCalculated> parse_source_size_value(ComponentValue const&);
    Optional<Ratio> parse_ratio(TokenStream<ComponentValue>&);
    Optional<UnicodeRange> parse_unicode_range(TokenStream<ComponentValue>&);
    Optional<UnicodeRange> parse_unicode_range(StringView);
    Optional<GridSize> parse_grid_size(ComponentValue const&);
    Optional<GridMinMax> parse_min_max(Vector<ComponentValue> const&);
    Optional<GridRepeat> parse_repeat(Vector<ComponentValue> const&);
    Optional<ExplicitGridTrack> parse_track_sizing_function(ComponentValue const&);
    Optional<PositionValue> parse_position(TokenStream<ComponentValue>&, PositionValue initial_value = PositionValue::center());

    Optional<AK::URL> parse_url_function(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_url_value(ComponentValue const&);

    Optional<Vector<LinearColorStopListElement>> parse_linear_color_stop_list(TokenStream<ComponentValue>&);
    Optional<Vector<AngularColorStopListElement>> parse_angular_color_stop_list(TokenStream<ComponentValue>&);

    ErrorOr<RefPtr<StyleValue>> parse_linear_gradient_function(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_conic_gradient_function(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_radial_gradient_function(ComponentValue const&);

    ParseErrorOr<NonnullRefPtr<StyleValue>> parse_css_value(PropertyID, TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_css_value_for_property(PropertyID, TokenStream<ComponentValue>&);
    struct PropertyAndValue {
        PropertyID property;
        RefPtr<StyleValue> style_value;
    };
    ErrorOr<PropertyAndValue> parse_css_value_for_properties(ReadonlySpan<PropertyID>, TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_builtin_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_dynamic_value(ComponentValue const&);
    ErrorOr<RefPtr<CalculatedStyleValue>> parse_calculated_value(Vector<ComponentValue> const&);
    // NOTE: Implemented in generated code. (GenerateCSSMathFunctions.cpp)
    ErrorOr<OwnPtr<CalculationNode>> parse_math_function(PropertyID, Function const&);
    ErrorOr<OwnPtr<CalculationNode>> parse_a_calc_function_node(Function const&);
    ErrorOr<RefPtr<StyleValue>> parse_dimension_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_integer_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_number_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_identifier_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_color_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_rect_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_ratio_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_string_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_image_value(ComponentValue const&);
    ErrorOr<RefPtr<StyleValue>> parse_paint_value(TokenStream<ComponentValue>&);
    template<typename ParseFunction>
    ErrorOr<RefPtr<StyleValue>> parse_comma_separated_value_list(Vector<ComponentValue> const&, ParseFunction);
    ErrorOr<RefPtr<StyleValue>> parse_simple_comma_separated_value_list(PropertyID, Vector<ComponentValue> const&);

    ErrorOr<RefPtr<StyleValue>> parse_filter_value_list_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_aspect_ratio_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_background_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_single_background_position_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_single_background_position_x_or_y_value(TokenStream<ComponentValue>&, PropertyID);
    ErrorOr<RefPtr<StyleValue>> parse_single_background_repeat_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_single_background_size_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_border_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_border_radius_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_border_radius_shorthand_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_content_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_display_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_flex_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_flex_flow_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_font_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_font_family_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_list_style_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_overflow_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_place_content_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_place_items_value(Vector<ComponentValue> const&);
    enum class AllowInsetKeyword {
        No,
        Yes,
    };
    ErrorOr<RefPtr<StyleValue>> parse_shadow_value(Vector<ComponentValue> const&, AllowInsetKeyword);
    ErrorOr<RefPtr<StyleValue>> parse_single_shadow_value(TokenStream<ComponentValue>&, AllowInsetKeyword);
    ErrorOr<RefPtr<StyleValue>> parse_text_decoration_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_text_decoration_line_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_easing_value(TokenStream<ComponentValue>&);
    ErrorOr<RefPtr<StyleValue>> parse_transform_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_transform_origin_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_track_size_list(Vector<ComponentValue> const&, bool allow_separate_line_name_blocks = false);
    ErrorOr<RefPtr<StyleValue>> parse_grid_auto_track_sizes(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_track_size_list_shorthand_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_track_placement(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_track_placement_shorthand_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_template_areas_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_area_shorthand_value(Vector<ComponentValue> const&);
    ErrorOr<RefPtr<StyleValue>> parse_grid_shorthand_value(Vector<ComponentValue> const&);

    ErrorOr<OwnPtr<CalculationNode>> parse_a_calculation(Vector<ComponentValue> const&);

    ParseErrorOr<NonnullRefPtr<Selector>> parse_complex_selector(TokenStream<ComponentValue>&, SelectorType);
    ParseErrorOr<Optional<Selector::CompoundSelector>> parse_compound_selector(TokenStream<ComponentValue>&);
    Optional<Selector::Combinator> parse_selector_combinator(TokenStream<ComponentValue>&);

    ParseErrorOr<Selector::SimpleSelector> parse_attribute_simple_selector(ComponentValue const&);
    ParseErrorOr<Selector::SimpleSelector> parse_pseudo_simple_selector(TokenStream<ComponentValue>&);
    ParseErrorOr<Optional<Selector::SimpleSelector>> parse_simple_selector(TokenStream<ComponentValue>&);

    NonnullRefPtr<MediaQuery> parse_media_query(TokenStream<ComponentValue>&);
    OwnPtr<MediaCondition> parse_media_condition(TokenStream<ComponentValue>&, MediaCondition::AllowOr allow_or);
    Optional<MediaFeature> parse_media_feature(TokenStream<ComponentValue>&);
    Optional<MediaQuery::MediaType> parse_media_type(TokenStream<ComponentValue>&);
    OwnPtr<MediaCondition> parse_media_in_parens(TokenStream<ComponentValue>&);
    Optional<MediaFeatureValue> parse_media_feature_value(MediaFeatureID, TokenStream<ComponentValue>&);

    OwnPtr<Supports::Condition> parse_supports_condition(TokenStream<ComponentValue>&);
    Optional<Supports::InParens> parse_supports_in_parens(TokenStream<ComponentValue>&);
    Optional<Supports::Feature> parse_supports_feature(TokenStream<ComponentValue>&);

    static bool has_ignored_vendor_prefix(StringView);
    static bool is_builtin(StringView);

    struct PropertiesAndCustomProperties {
        Vector<StyleProperty> properties;
        HashMap<DeprecatedString, StyleProperty> custom_properties;
    };

    PropertiesAndCustomProperties extract_properties(Vector<DeclarationOrAtRule> const&);

    ParsingContext m_context;

    Vector<Token> m_tokens;
    TokenStream<Token> m_token_stream;
};

}

namespace Web {

CSS::CSSStyleSheet* parse_css_stylesheet(CSS::Parser::ParsingContext const&, StringView, Optional<AK::URL> location = {});
CSS::ElementInlineCSSStyleDeclaration* parse_css_style_attribute(CSS::Parser::ParsingContext const&, StringView, DOM::Element&);
ErrorOr<RefPtr<CSS::StyleValue>> parse_css_value(CSS::Parser::ParsingContext const&, StringView, CSS::PropertyID property_id = CSS::PropertyID::Invalid);
Optional<CSS::SelectorList> parse_selector(CSS::Parser::ParsingContext const&, StringView);
CSS::CSSRule* parse_css_rule(CSS::Parser::ParsingContext const&, StringView);
RefPtr<CSS::MediaQuery> parse_media_query(CSS::Parser::ParsingContext const&, StringView);
Vector<NonnullRefPtr<CSS::MediaQuery>> parse_media_query_list(CSS::Parser::ParsingContext const&, StringView);
RefPtr<CSS::Supports> parse_css_supports(CSS::Parser::ParsingContext const&, StringView);
Optional<CSS::StyleProperty> parse_css_supports_condition(CSS::Parser::ParsingContext const&, StringView);

}
