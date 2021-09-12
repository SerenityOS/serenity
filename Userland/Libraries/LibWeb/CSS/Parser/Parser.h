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
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class CSSStyleSheet;
class CSSRule;
class CSSStyleRule;
struct StyleProperty;
enum class PropertyID;

class ParsingContext {
public:
    ParsingContext();
    explicit ParsingContext(DOM::Document&);
    explicit ParsingContext(DOM::ParentNode&);

    bool in_quirks_mode() const;
    DOM::Document* document() const { return m_document; }
    URL complete_url(String const&) const;

    PropertyID current_property_id() const { return m_current_property_id; }
    void set_current_property_id(PropertyID property_id) { m_current_property_id = property_id; }

private:
    DOM::Document* m_document { nullptr };
    PropertyID m_current_property_id { PropertyID::Invalid };
};

template<typename T>
class TokenStream {
public:
    explicit TokenStream(Vector<T> const&);
    ~TokenStream();

    bool has_next_token();
    T const& next_token();
    T const& peek_token(int offset = 0);
    T const& current_token();
    void reconsume_current_input_token();

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
    Parser(ParsingContext const&, StringView const& input, String const& encoding = "utf-8");
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

    RefPtr<StyleValue> parse_as_css_value(PropertyID);

private:
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
    Optional<SelectorList> parse_a_selector(TokenStream<T>&);
    template<typename T>
    Optional<SelectorList> parse_a_relative_selector(TokenStream<T>&);
    template<typename T>
    Optional<SelectorList> parse_a_selector_list(TokenStream<T>&);
    template<typename T>
    Optional<SelectorList> parse_a_relative_selector_list(TokenStream<T>&);

    Optional<Selector::SimpleSelector::ANPlusBPattern> parse_a_n_plus_b_pattern(TokenStream<StyleComponentValueRule>&);

    [[nodiscard]] NonnullRefPtrVector<StyleRule> consume_a_list_of_rules(bool top_level);
    template<typename T>
    [[nodiscard]] NonnullRefPtrVector<StyleRule> consume_a_list_of_rules(TokenStream<T>&, bool top_level);

    [[nodiscard]] NonnullRefPtr<StyleRule> consume_an_at_rule();
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleRule> consume_an_at_rule(TokenStream<T>&);

    [[nodiscard]] RefPtr<StyleRule> consume_a_qualified_rule();
    template<typename T>
    [[nodiscard]] RefPtr<StyleRule> consume_a_qualified_rule(TokenStream<T>&);

    [[nodiscard]] Vector<DeclarationOrAtRule> consume_a_list_of_declarations();
    template<typename T>
    [[nodiscard]] Vector<DeclarationOrAtRule> consume_a_list_of_declarations(TokenStream<T>&);

    [[nodiscard]] Optional<StyleDeclarationRule> consume_a_declaration();
    template<typename T>
    [[nodiscard]] Optional<StyleDeclarationRule> consume_a_declaration(TokenStream<T>&);

    [[nodiscard]] StyleComponentValueRule consume_a_component_value();
    template<typename T>
    [[nodiscard]] StyleComponentValueRule consume_a_component_value(TokenStream<T>&);

    [[nodiscard]] NonnullRefPtr<StyleBlockRule> consume_a_simple_block();
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleBlockRule> consume_a_simple_block(TokenStream<T>&);

    [[nodiscard]] NonnullRefPtr<StyleFunctionRule> consume_a_function();
    template<typename T>
    [[nodiscard]] NonnullRefPtr<StyleFunctionRule> consume_a_function(TokenStream<T>&);

    [[nodiscard]] RefPtr<CSSRule> convert_to_rule(NonnullRefPtr<StyleRule>);
    [[nodiscard]] RefPtr<PropertyOwningCSSStyleDeclaration> convert_to_declaration(NonnullRefPtr<StyleBlockRule>);
    [[nodiscard]] Optional<StyleProperty> convert_to_style_property(StyleDeclarationRule&);

    static Optional<float> try_parse_float(StringView string);
    static Optional<Color> parse_color(ParsingContext const&, StyleComponentValueRule const&);
    static Optional<Length> parse_length(ParsingContext const&, StyleComponentValueRule const&);
    static Optional<URL> parse_url_function(ParsingContext const&, StyleComponentValueRule const&);

    RefPtr<StyleValue> parse_css_value(PropertyID, TokenStream<StyleComponentValueRule>&);
    static RefPtr<StyleValue> parse_css_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_builtin_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_dynamic_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_length_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_numeric_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_identifier_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_color_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_string_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_image_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_background_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_background_image_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_background_repeat_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_border_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_border_radius_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_border_radius_shorthand_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_box_shadow_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_flex_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_flex_flow_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_font_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_font_family_value(ParsingContext const&, Vector<StyleComponentValueRule> const&, size_t start_index = 0);
    static RefPtr<StyleValue> parse_list_style_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_overflow_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);
    static RefPtr<StyleValue> parse_text_decoration_value(ParsingContext const&, Vector<StyleComponentValueRule> const&);

    // calc() parsing, according to https://www.w3.org/TR/css-values-3/#calc-syntax
    static OwnPtr<CalculatedStyleValue::CalcSum> parse_calc_sum(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcProduct> parse_calc_product(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static Optional<CalculatedStyleValue::CalcValue> parse_calc_value(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcNumberSum> parse_calc_number_sum(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcNumberProduct> parse_calc_number_product(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static Optional<CalculatedStyleValue::CalcNumberValue> parse_calc_number_value(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcProductPartWithOperator> parse_calc_product_part_with_operator(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcSumPartWithOperator> parse_calc_sum_part_with_operator(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcNumberProductPartWithOperator> parse_calc_number_product_part_with_operator(ParsingContext const&, TokenStream<StyleComponentValueRule>& tokens);
    static OwnPtr<CalculatedStyleValue::CalcNumberSumPartWithOperator> parse_calc_number_sum_part_with_operator(ParsingContext const&, TokenStream<StyleComponentValueRule>&);
    static OwnPtr<CalculatedStyleValue::CalcSum> parse_calc_expression(ParsingContext const&, Vector<StyleComponentValueRule> const&);

    enum class SelectorParsingResult {
        Done,
        SyntaxError,
    };

    RefPtr<Selector> parse_complex_selector(TokenStream<StyleComponentValueRule>&, bool allow_starting_combinator);
    Result<Selector::CompoundSelector, SelectorParsingResult> parse_compound_selector(TokenStream<StyleComponentValueRule>&);
    Optional<Selector::Combinator> parse_selector_combinator(TokenStream<StyleComponentValueRule>&);
    Result<Selector::SimpleSelector, SelectorParsingResult> parse_simple_selector(TokenStream<StyleComponentValueRule>&);

    static bool has_ignored_vendor_prefix(StringView const&);

    ParsingContext m_context;

    Tokenizer m_tokenizer;
    Vector<Token> m_tokens;
    TokenStream<Token> m_token_stream;
};

}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(CSS::ParsingContext const&, StringView const&);
RefPtr<CSS::PropertyOwningCSSStyleDeclaration> parse_css_declaration(CSS::ParsingContext const&, StringView const&);
RefPtr<CSS::StyleValue> parse_css_value(CSS::ParsingContext const&, StringView const&, CSS::PropertyID property_id = CSS::PropertyID::Invalid);
Optional<CSS::SelectorList> parse_selector(CSS::ParsingContext const&, StringView const&);

RefPtr<CSS::StyleValue> parse_html_length(DOM::Document const&, StringView const&);

}
