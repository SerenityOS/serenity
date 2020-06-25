/*
 * Copyright (c) 2020, SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/CSSTokenizer.h>
#include <LibWeb/CSS/Parser/Rules/AtStyleRule.h>
#include <LibWeb/CSS/Parser/Rules/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Rules/QualifiedStyleRule.h>
#include <LibWeb/CSS/Parser/Rules/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/Rules/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/Rules/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/Rules/StyleFunctionRule.h>
#include <LibWeb/CSS/Selector.h>

namespace Web {

class CSSParser {
public:
    CSSParser(const StringView& input, const String& encoding = "utf-8");
    ~CSSParser();

    //the normal parser entry point, for parsing stylesheets.
    Vector<QualifiedStyleRule> parse_as_stylesheet();
    // for the content of at-rules such as @media. It differs from "Parse a stylesheet" in the handling of <CDO-token> and <CDC-token>.
    Vector<QualifiedStyleRule> parse_as_list_of_rules();
    // for use by the CSSStyleSheet#insertRule method, and similar functions which might exist, which parse text into a single rule.
    Optional<QualifiedStyleRule> parse_as_rule();
    // used in @supports conditions. [CSS3-CONDITIONAL]
    Optional<StyleDeclarationRule> parse_as_declaration();
    // for the contents of a style attribute, which parses text into the contents of a single style rule.
    Vector<DeclarationOrAtRule> parse_as_list_of_declarations();
    // for things that need to consume a single value, like the parsing rules for attr().
    Optional<StyleComponentValueRule> parse_as_component_value();
    // for the contents of presentational attributes, which parse text into a single declaration’s value, or for parsing a stand-alone selector [SELECT] or list of Media Queries [MEDIAQ], as in Selectors API or the media HTML attribute.
    Vector<StyleComponentValueRule> parse_as_list_of_component_values();

    Vector<StyleComponentValueRule> parse_as_list_of_comma_separated_component_values();

    Vector<CSS::Selector::ComplexSelector> parse_selectors(Vector<String> parts);

    // FIXME: https://www.w3.org/TR/selectors-4/
    Optional<String> parse_a_selector() { return {}; };
    Optional<String> parse_a_relative_selector() { return {}; };
    bool match_a_selector_against_an_element() { return false; };
    bool match_a_selector_against_a_pseudo_element() { return false; };
    bool match_a_selector_against_a_tree() { return false; };

    // FIXME: https://drafts.csswg.org/css-backgrounds-3/
    static Optional<String> as_valid_background_repeat(String input) { return input; };
    static Optional<String> as_valid_background_attachment(String input) { return input; };
    static Optional<String> as_valid_background_position(String input) { return input; };
    static Optional<String> as_valid_background_clip(String input) { return input; };
    static Optional<String> as_valid_background_origin(String input) { return input; };
    static Optional<String> as_valid_background_size(String input) { return input; };
    static Optional<String> as_valid_border_style(String input) { return input; };
    static Optional<String> as_valid_border_image_repeat(String input) { return input; };

    void dbg_all_tokens();

private:
    CSSToken next_token();
    CSSToken peek_token();
    CSSToken current_token();
    void reconsume_current_input_token();

    Vector<QualifiedStyleRule> consume_a_list_of_rules(bool top_level);
    AtStyleRule consume_an_at_rule();
    Optional<QualifiedStyleRule> consume_a_qualified_rule();
    Vector<DeclarationOrAtRule> consume_a_list_of_declarations();
    Optional<StyleDeclarationRule> consume_a_declaration(Vector<StyleComponentValueRule>);
    Optional<StyleDeclarationRule> consume_a_declaration();
    StyleComponentValueRule consume_a_component_value();
    StyleBlockRule consume_a_simple_block();
    StyleFunctionRule consume_a_function();

    CSSTokenizer m_tokenizer;
    Vector<CSSToken> m_tokens;
    int m_iterator_offset { -1 };
};

}
