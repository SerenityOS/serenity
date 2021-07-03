/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class CSSStyleSheet;
class CSSRule;
class CSSStyleRule;
struct StyleProperty;

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
    // For the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    NonnullRefPtrVector<CSSRule> parse_as_list_of_rules();
    // For use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    RefPtr<CSSRule> parse_as_rule();
    // Used in @supports conditions. [CSS3-CONDITIONAL]
    Optional<StyleProperty> parse_as_declaration();
    // For the contents of a style attribute, which parses text into the contents of a single style rule.
    Vector<StyleProperty> parse_as_list_of_declarations();
    // For things that need to consume a single value, like the parsing rules for attr().
    Optional<StyleComponentValueRule> parse_as_component_value();
    // For the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    Vector<StyleComponentValueRule> parse_as_list_of_component_values();

    Vector<Vector<StyleComponentValueRule>> parse_as_comma_separated_list_of_component_values();

    Optional<Selector> parse_single_selector(Vector<StyleComponentValueRule> parts, bool is_relative = false);

    // FIXME: https://www.w3.org/TR/selectors-4/
    // Contrary to the name, these parse a comma-separated list of selectors, according to the spec.
    Vector<Selector> parse_a_selector();
    Vector<Selector> parse_a_selector(Vector<Vector<StyleComponentValueRule>>&);
    Vector<Selector> parse_a_relative_selector();
    bool match_a_selector_against_an_element() { return false; }
    bool match_a_selector_against_a_pseudo_element() { return false; }
    bool match_a_selector_against_a_tree() { return false; }

    // FIXME: https://drafts.csswg.org/css-backgrounds-3/
    static Optional<String> as_valid_background_repeat(String input) { return input; }
    static Optional<String> as_valid_background_attachment(String input) { return input; }
    static Optional<String> as_valid_background_position(String input) { return input; }
    static Optional<String> as_valid_background_clip(String input) { return input; }
    static Optional<String> as_valid_background_origin(String input) { return input; }
    static Optional<String> as_valid_background_size(String input) { return input; }
    static Optional<String> as_valid_border_style(String input) { return input; }
    static Optional<String> as_valid_border_image_repeat(String input) { return input; }

private:
    Token next_token() { return m_token_stream.next_token(); }
    Token peek_token() { return m_token_stream.peek_token(); }
    Token current_token() { return m_token_stream.current_token(); }
    void reconsume_current_input_token() { m_token_stream.reconsume_current_input_token(); }

    NonnullRefPtrVector<StyleRule> consume_a_list_of_rules(bool top_level);
    NonnullRefPtr<StyleRule> consume_an_at_rule();
    RefPtr<StyleRule> consume_a_qualified_rule();
    Vector<DeclarationOrAtRule> consume_a_list_of_declarations();
    Optional<StyleDeclarationRule> consume_a_declaration(Vector<StyleComponentValueRule>);
    Optional<StyleDeclarationRule> consume_a_declaration();
    StyleComponentValueRule consume_a_component_value();
    NonnullRefPtr<StyleBlockRule> consume_a_simple_block();
    NonnullRefPtr<StyleFunctionRule> consume_a_function();

    RefPtr<CSSRule> convert_rule(NonnullRefPtr<StyleRule>);

    ParsingContext m_context;

    Tokenizer m_tokenizer;
    Vector<Token> m_tokens;
    TokenStream<Token> m_token_stream;
};

}
