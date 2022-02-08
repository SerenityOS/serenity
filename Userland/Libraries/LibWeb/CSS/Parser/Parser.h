/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/GeneralEnclosed.h>
#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/Supports.h>

namespace Web::CSS {

class CSSStyleSheet;
class CSSRule;
class CSSStyleRule;
struct StyleProperty;
enum class PropertyID;

class ParsingContext {
public:
    ParsingContext() = default;
    explicit ParsingContext(DOM::Document const&);
    explicit ParsingContext(DOM::ParentNode&);

    bool in_quirks_mode() const;
    DOM::Document const* document() const { return m_document; }
    AK::URL complete_url(String const&) const;

    PropertyID current_property_id() const { return m_current_property_id; }
    void set_current_property_id(PropertyID property_id) { m_current_property_id = property_id; }

private:
    DOM::Document const* m_document { nullptr };
    PropertyID m_current_property_id { PropertyID::Invalid };
};

template<typename T>
class TokenStream {
public:
    explicit TokenStream(Vector<T> const&);
    ~TokenStream();

    TokenStream(TokenStream<T> const&) = delete;

    bool has_next_token();
    T const& next_token();
    T const& peek_token(int offset = 0);
    T const& current_token();
    void reconsume_current_input_token();

    int position() const { return m_iterator_offset; }
    void rewind_to_position(int);

    void skip_whitespace();

    void dump_all_tokens();

private:
    Vector<T> const& m_tokens;
    int m_iterator_offset { -1 };

    T make_eof();
    T m_eof;
};

class Parser {
public:
    Parser(ParsingContext const&, StringView input, String const& encoding = "utf-8");
    ~Parser();

    // The normal parser entry point, for parsing stylesheets.
    NonnullRefPtr<CSSStyleSheet> parse_as_stylesheet();
    // For the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    NonnullRefPtrVector<CSSRule> parse_as_list_of_rules();
    // For use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    RefPtr<CSSRule> parse_as_rule();
    // Used in @supports conditions. [CSS3-CONDITIONAL]
    Optional<StyleProperty> parse_as_declaration();
    // For the contents of a style attribute, which parses text into the contents of a single style rule.
    RefPtr<PropertyOwningCSSStyleDeclaration> parse_as_list_of_declarations();
    // For things that need to consume a single value, like the parsing rules for attr().
    Optional<StyleComponentValueRule> parse_as_component_value();
    // For the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    Vector<StyleComponentValueRule> parse_as_list_of_component_values();
    Vector<Vector<StyleComponentValueRule>> parse_as_comma_separated_list_of_component_values();

    // Contrary to the name, these parse a comma-separated list of selectors, according to the spec.
    Optional<SelectorList> parse_as_selector();
    Optional<SelectorList> parse_as_relative_selector();

    NonnullRefPtrVector<MediaQuery> parse_as_media_query_list();
    RefPtr<MediaQuery> parse_as_media_query();

    RefPtr<Supports> parse_as_supports();

    RefPtr<StyleValue> parse_as_css_value(PropertyID);

    static RefPtr<StyleValue> parse_css_value(Badge<StyleComputer>, ParsingContext const&, PropertyID, Vector<StyleComponentValueRule> const&);

private:
    enum class ParsingResult {
        Done,
        IncludesIgnoredVendorPrefix,
        SyntaxError,
    };

    template<typename T>
    NonnullRefPtr<CSSStyleSheet> parse_a_stylesheet(TokenStream<T>&);
    template<typename T>
    NonnullRefPtrVector<CSSRule> parse_a_list_of_rules(TokenStream<T>&);
    template<typename T>
    RefPtr<CSSRule> parse_a_rule(TokenStream<T>&);
    template<typename T>
    Optional<StyleProperty> parse_a_declaration(TokenStream<T>&);
    template<typename T>
    RefPtr<PropertyOwningCSSStyleDeclaration> parse_a_list_of_declarations(TokenStream<T>&);
    template<typename T>
    Optional<StyleComponentValueRule> parse_a_component_value(TokenStream<T>&);
    template<typename T>
    Vector<StyleComponentValueRule> parse_a_list_of_component_values(TokenStream<T>&);
    template<typename T>
    Vector<Vector<StyleComponentValueRule>> parse_a_comma_separated_list_of_component_values(TokenStream<T>&);
    template<typename T>
    Result<SelectorList, ParsingResult> parse_a_selector(TokenStream<T>&);
    template<typename T>
    Result<SelectorList, ParsingResult> parse_a_relative_selector(TokenStream<T>&);
    template<typename T>
    Result<SelectorList, ParsingResult> parse_a_selector_list(TokenStream<T>&);
    template<typename T>
    Result<SelectorList, ParsingResult> parse_a_relative_selector_list(TokenStream<T>&);
    template<typename T>
    NonnullRefPtrVector<MediaQuery> parse_a_media_query_list(TokenStream<T>&);
    template<typename T>
    RefPtr<Supports> parse_a_supports(TokenStream<T>&);

    Optional<Selector::SimpleSelector::ANPlusBPattern> parse_a_n_plus_b_pattern(TokenStream<StyleComponentValueRule>&);

    template<typename T>
    [[nodiscard]] NonnullRefPtrVector<StyleRule> consume_a_list_of_rules(TokenStream<T>&, bool top_level);
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleRule> consume_an_at_rule(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] RefPtr<StyleRule> consume_a_qualified_rule(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] Vector<DeclarationOrAtRule> consume_a_list_of_declarations(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] Optional<StyleDeclarationRule> consume_a_declaration(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] StyleComponentValueRule consume_a_component_value(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleBlockRule> consume_a_simple_block(TokenStream<T>&);
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleFunctionRule> consume_a_function(TokenStream<T>&);

    [[nodiscard]] Optional<GeneralEnclosed> parse_general_enclosed(TokenStream<StyleComponentValueRule>&);

    [[nodiscard]] RefPtr<CSSRule> convert_to_rule(NonnullRefPtr<StyleRule>);
    [[nodiscard]] RefPtr<PropertyOwningCSSStyleDeclaration> convert_to_declaration(NonnullRefPtr<StyleBlockRule>);
    [[nodiscard]] Optional<StyleProperty> convert_to_style_property(StyleDeclarationRule const&);

    class Dimension {
    public:
        Dimension(Length&& value)
            : m_value(move(value))
        {
        }
        Dimension(Percentage&& value)
            : m_value(move(value))
        {
        }

        bool is_length() const;
        Length length() const;

        bool is_percentage() const;
        Percentage percentage() const;

        bool is_length_percentage() const;
        LengthPercentage length_percentage() const;

    private:
        Variant<Length, Percentage> m_value;
    };
    Optional<Dimension> parse_dimension(StyleComponentValueRule const&);
    Optional<Color> parse_color(StyleComponentValueRule const&);
    Optional<Length> parse_length(StyleComponentValueRule const&);

    enum class AllowedDataUrlType {
        None,
        Image,
    };
    Optional<AK::URL> parse_url_function(StyleComponentValueRule const&, AllowedDataUrlType = AllowedDataUrlType::None);

    Result<NonnullRefPtr<StyleValue>, ParsingResult> parse_css_value(PropertyID, TokenStream<StyleComponentValueRule>&);
    RefPtr<StyleValue> parse_css_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_builtin_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_dynamic_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_calculated_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_dimension_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_numeric_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_identifier_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_color_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_string_value(StyleComponentValueRule const&);
    RefPtr<StyleValue> parse_image_value(StyleComponentValueRule const&);
    template<typename ParseFunction>
    RefPtr<StyleValue> parse_comma_separated_value_list(Vector<StyleComponentValueRule> const&, ParseFunction);
    RefPtr<StyleValue> parse_simple_comma_separated_value_list(Vector<StyleComponentValueRule> const&);

    RefPtr<StyleValue> parse_background_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_single_background_position_value(TokenStream<StyleComponentValueRule>&);
    RefPtr<StyleValue> parse_single_background_repeat_value(TokenStream<StyleComponentValueRule>&);
    RefPtr<StyleValue> parse_single_background_size_value(TokenStream<StyleComponentValueRule>&);
    RefPtr<StyleValue> parse_border_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_border_radius_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_border_radius_shorthand_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_box_shadow_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_single_box_shadow_value(TokenStream<StyleComponentValueRule>&);
    RefPtr<StyleValue> parse_flex_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_flex_flow_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_font_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_font_family_value(Vector<StyleComponentValueRule> const&, size_t start_index = 0);
    RefPtr<StyleValue> parse_list_style_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_overflow_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_text_decoration_value(Vector<StyleComponentValueRule> const&);
    RefPtr<StyleValue> parse_transform_value(Vector<StyleComponentValueRule> const&);

    // calc() parsing, according to https://www.w3.org/TR/css-values-3/#calc-syntax
    OwnPtr<CalculatedStyleValue::CalcSum> parse_calc_sum(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcProduct> parse_calc_product(TokenStream<StyleComponentValueRule>&);
    Optional<CalculatedStyleValue::CalcValue> parse_calc_value(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcNumberSum> parse_calc_number_sum(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcNumberProduct> parse_calc_number_product(TokenStream<StyleComponentValueRule>&);
    Optional<CalculatedStyleValue::CalcNumberValue> parse_calc_number_value(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcProductPartWithOperator> parse_calc_product_part_with_operator(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcSumPartWithOperator> parse_calc_sum_part_with_operator(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcNumberProductPartWithOperator> parse_calc_number_product_part_with_operator(TokenStream<StyleComponentValueRule>& tokens);
    OwnPtr<CalculatedStyleValue::CalcNumberSumPartWithOperator> parse_calc_number_sum_part_with_operator(TokenStream<StyleComponentValueRule>&);
    OwnPtr<CalculatedStyleValue::CalcSum> parse_calc_expression(Vector<StyleComponentValueRule> const&);

    Result<NonnullRefPtr<Selector>, ParsingResult> parse_complex_selector(TokenStream<StyleComponentValueRule>&, bool allow_starting_combinator);
    Result<Selector::CompoundSelector, ParsingResult> parse_compound_selector(TokenStream<StyleComponentValueRule>&);
    Optional<Selector::Combinator> parse_selector_combinator(TokenStream<StyleComponentValueRule>&);
    Result<Selector::SimpleSelector, ParsingResult> parse_simple_selector(TokenStream<StyleComponentValueRule>&);

    NonnullRefPtr<MediaQuery> parse_media_query(TokenStream<StyleComponentValueRule>&);
    OwnPtr<MediaCondition> parse_media_condition(TokenStream<StyleComponentValueRule>&, MediaCondition::AllowOr allow_or);
    Optional<MediaFeature> parse_media_feature(TokenStream<StyleComponentValueRule>&);
    Optional<MediaQuery::MediaType> parse_media_type(TokenStream<StyleComponentValueRule>&);
    OwnPtr<MediaCondition> parse_media_in_parens(TokenStream<StyleComponentValueRule>&);
    Optional<MediaFeatureValue> parse_media_feature_value(TokenStream<StyleComponentValueRule>&);

    OwnPtr<Supports::Condition> parse_supports_condition(TokenStream<StyleComponentValueRule>&);
    Optional<Supports::InParens> parse_supports_in_parens(TokenStream<StyleComponentValueRule>&);
    Optional<Supports::Feature> parse_supports_feature(TokenStream<StyleComponentValueRule>&);

    static bool has_ignored_vendor_prefix(StringView);

    ParsingContext m_context;

    Tokenizer m_tokenizer;
    Vector<Token> m_tokens;
    TokenStream<Token> m_token_stream;
};

}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(CSS::ParsingContext const&, StringView);
RefPtr<CSS::PropertyOwningCSSStyleDeclaration> parse_css_declaration(CSS::ParsingContext const&, StringView);
RefPtr<CSS::StyleValue> parse_css_value(CSS::ParsingContext const&, StringView, CSS::PropertyID property_id = CSS::PropertyID::Invalid);
Optional<CSS::SelectorList> parse_selector(CSS::ParsingContext const&, StringView);
RefPtr<CSS::CSSRule> parse_css_rule(CSS::ParsingContext const&, StringView);
RefPtr<CSS::MediaQuery> parse_media_query(CSS::ParsingContext const&, StringView);
NonnullRefPtrVector<CSS::MediaQuery> parse_media_query_list(CSS::ParsingContext const&, StringView);
RefPtr<CSS::Supports> parse_css_supports(CSS::ParsingContext const&, StringView);

RefPtr<CSS::StyleValue> parse_html_length(DOM::Document const&, StringView);

}
