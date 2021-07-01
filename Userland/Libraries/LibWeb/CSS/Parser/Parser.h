/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/AtStyleRule.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/QualifiedStyleRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>
#include <LibWeb/CSS/Selector.h>

namespace Web::CSS {

class Parser {
public:
    Parser(const StringView& input, const String& encoding = "utf-8");
    ~Parser();

    // The normal parser entry point, for parsing stylesheets.
    NonnullRefPtrVector<QualifiedStyleRule> parse_as_stylesheet();
    // For the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    NonnullRefPtrVector<QualifiedStyleRule> parse_as_list_of_rules();
    // For use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    RefPtr<QualifiedStyleRule> parse_as_rule();
    // Used in @supports conditions. [CSS3-CONDITIONAL]
    Optional<StyleDeclarationRule> parse_as_declaration();
    // For the contents of a style attribute, which parses text into the contents of a single style rule.
    Vector<DeclarationOrAtRule> parse_as_list_of_declarations();
    // For things that need to consume a single value, like the parsing rules for attr().
    Optional<StyleComponentValueRule> parse_as_component_value();
    // For the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    Vector<StyleComponentValueRule> parse_as_list_of_component_values();

    Vector<StyleComponentValueRule> parse_as_list_of_comma_separated_component_values();

    Vector<CSS::Selector::ComplexSelector> parse_selectors(Vector<StyleComponentValueRule> parts);

    // FIXME: https://www.w3.org/TR/selectors-4/
    Optional<String> parse_a_selector() { return {}; }
    Optional<String> parse_a_relative_selector() { return {}; }
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

    void dump_all_tokens();

private:
    Token next_token();
    Token peek_token();
    Token current_token();
    void reconsume_current_input_token();
    bool is_combinator(String);

    NonnullRefPtrVector<QualifiedStyleRule> consume_a_list_of_rules(bool top_level);
    NonnullRefPtr<AtStyleRule> consume_an_at_rule();
    RefPtr<QualifiedStyleRule> consume_a_qualified_rule();
    Vector<DeclarationOrAtRule> consume_a_list_of_declarations();
    Optional<StyleDeclarationRule> consume_a_declaration(Vector<StyleComponentValueRule>);
    Optional<StyleDeclarationRule> consume_a_declaration();
    StyleComponentValueRule consume_a_component_value();
    NonnullRefPtr<StyleBlockRule> consume_a_simple_block();
    NonnullRefPtr<StyleFunctionRule> consume_a_function();

    Tokenizer m_tokenizer;
    Vector<Token> m_tokens;
    int m_iterator_offset { -1 };
};

}
