/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
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
    explicit ParsingContext(DOM::Document const&);
    explicit ParsingContext(DOM::ParentNode const&);

    bool in_quirks_mode() const;

    URL complete_url(String const&) const;

private:
    const DOM::Document* m_document { nullptr };
};

template<typename T>
class TokenStream {
public:
    explicit TokenStream(Vector<T> const&);
    ~TokenStream();

    bool has_next_token();
    T const& next_token();
    T const& peek_token();
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
    template<typename T>
    NonnullRefPtr<CSSStyleSheet> parse_as_stylesheet(TokenStream<T>&);

    // For the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    NonnullRefPtrVector<CSSRule> parse_as_list_of_rules();
    template<typename T>
    NonnullRefPtrVector<CSSRule> parse_as_list_of_rules(TokenStream<T>&);

    // For use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    RefPtr<CSSRule> parse_as_rule();
    template<typename T>
    RefPtr<CSSRule> parse_as_rule(TokenStream<T>&);

    // Used in @supports conditions. [CSS3-CONDITIONAL]
    Optional<StyleProperty> parse_as_declaration();
    template<typename T>
    Optional<StyleProperty> parse_as_declaration(TokenStream<T>&);

    // For the contents of a style attribute, which parses text into the contents of a single style rule.
    RefPtr<CSSStyleDeclaration> parse_as_list_of_declarations();
    template<typename T>
    RefPtr<CSSStyleDeclaration> parse_as_list_of_declarations(TokenStream<T>&);

    // For things that need to consume a single value, like the parsing rules for attr().
    Optional<StyleComponentValueRule> parse_as_component_value();
    template<typename T>
    Optional<StyleComponentValueRule> parse_as_component_value(TokenStream<T>&);

    // For the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    Vector<StyleComponentValueRule> parse_as_list_of_component_values();
    template<typename T>
    Vector<StyleComponentValueRule> parse_as_list_of_component_values(TokenStream<T>&);

    Vector<Vector<StyleComponentValueRule>> parse_as_comma_separated_list_of_component_values();
    template<typename T>
    Vector<Vector<StyleComponentValueRule>> parse_as_comma_separated_list_of_component_values(TokenStream<T>&);

    template<typename T>
    RefPtr<Selector> parse_single_selector(TokenStream<T>&, bool is_relative = false);

    Optional<Selector::SimpleSelector::NthChildPattern> parse_nth_child_pattern(TokenStream<StyleComponentValueRule>&);

    // FIXME: https://www.w3.org/TR/selectors-4/
    // Contrary to the name, these parse a comma-separated list of selectors, according to the spec.
    NonnullRefPtrVector<Selector> parse_a_selector();
    template<typename T>
    NonnullRefPtrVector<Selector> parse_a_selector(TokenStream<T>&);

    NonnullRefPtrVector<Selector> parse_a_relative_selector();
    template<typename T>
    NonnullRefPtrVector<Selector> parse_a_relative_selector(TokenStream<T>&);

    RefPtr<StyleValue> parse_css_value(PropertyID, TokenStream<StyleComponentValueRule>&);
    static RefPtr<StyleValue> parse_css_value(ParsingContext const&, PropertyID, StyleComponentValueRule const&);

private:
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
    [[nodiscard]] RefPtr<CSSStyleDeclaration> convert_to_declaration(NonnullRefPtr<StyleBlockRule>);
    [[nodiscard]] Optional<StyleProperty> convert_to_style_property(StyleDeclarationRule&);

    static Optional<float> try_parse_float(StringView string);

    static RefPtr<StyleValue> parse_keyword_or_custom_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_length_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_numeric_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_identifier_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_color_value(ParsingContext const&, StyleComponentValueRule const&);
    static RefPtr<StyleValue> parse_string_value(ParsingContext const&, StyleComponentValueRule const&);

    ParsingContext m_context;

    Tokenizer m_tokenizer;
    Vector<Token> m_tokens;
    TokenStream<Token> m_token_stream;
};

}
