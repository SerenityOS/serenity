/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Utf32View.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLFormElement.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/DOM/HTMLScriptElement.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Parser/HTMLDocumentParser.h>
#include <LibWeb/Parser/HTMLToken.h>

#define TODO()                \
    do {                      \
        ASSERT_NOT_REACHED(); \
    } while (0)

#define PARSE_ERROR()            \
    do {                         \
        dbg() << "Parse error!"; \
    } while (0)

namespace Web {

HTMLDocumentParser::HTMLDocumentParser(const StringView& input)
    : m_tokenizer(input)
{
}

HTMLDocumentParser::~HTMLDocumentParser()
{
}

void HTMLDocumentParser::run(const URL& url)
{
    m_document = adopt(*new Document);
    m_document->set_url(url);

    for (;;) {
        auto optional_token = m_tokenizer.next_token();
        if (!optional_token.has_value())
            return;
        auto& token = optional_token.value();

        dbg() << "[" << insertion_mode_name() << "] " << token.to_string();
        process_using_the_rules_for(m_insertion_mode, token);
    }
}

void HTMLDocumentParser::process_using_the_rules_for(InsertionMode mode, HTMLToken& token)
{
    switch (mode) {
    case InsertionMode::Initial:
        handle_initial(token);
        break;
    case InsertionMode::BeforeHTML:
        handle_before_html(token);
        break;
    case InsertionMode::BeforeHead:
        handle_before_head(token);
        break;
    case InsertionMode::InHead:
        handle_in_head(token);
        break;
    case InsertionMode::InHeadNoscript:
        handle_in_head_noscript(token);
        break;
    case InsertionMode::AfterHead:
        handle_after_head(token);
        break;
    case InsertionMode::InBody:
        handle_in_body(token);
        break;
    case InsertionMode::AfterBody:
        handle_after_body(token);
        break;
    case InsertionMode::AfterAfterBody:
        handle_after_after_body(token);
        break;
    case InsertionMode::Text:
        handle_text(token);
        break;
    case InsertionMode::InTable:
        handle_in_table(token);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

void HTMLDocumentParser::handle_initial(HTMLToken& token)
{
    if (token.type() == HTMLToken::Type::DOCTYPE) {
        auto doctype = adopt(*new DocumentType(document()));
        doctype->set_name(token.m_doctype.name.to_string());
        document().append_child(move(doctype));
        m_insertion_mode = InsertionMode::BeforeHTML;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_before_html(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "html") {
        auto element = create_element_for(token);
        document().append_child(element);
        m_stack_of_open_elements.push(move(element));
        m_insertion_mode = InsertionMode::BeforeHead;
        return;
    }
    ASSERT_NOT_REACHED();
}

Element& HTMLDocumentParser::current_node()
{
    return m_stack_of_open_elements.current_node();
}

RefPtr<Node> HTMLDocumentParser::find_appropriate_place_for_inserting_node()
{
    auto& target = current_node();
    if (m_foster_parenting) {
        ASSERT_NOT_REACHED();
    }
    return target;
}

NonnullRefPtr<Element> HTMLDocumentParser::create_element_for(HTMLToken& token)
{
    auto element = create_element(document(), token.tag_name());
    for (auto& attribute : token.m_tag.attributes) {
        element->set_attribute(attribute.name_builder.to_string(), attribute.value_builder.to_string());
    }
    return element;
}

RefPtr<Element> HTMLDocumentParser::insert_html_element(HTMLToken& token)
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    auto element = create_element_for(token);
    // FIXME: Check if it's possible to insert `element` at `adjusted_insertion_location`
    adjusted_insertion_location->append_child(element);
    m_stack_of_open_elements.push(element);
    return element;
}

void HTMLDocumentParser::handle_before_head(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "head") {
        auto element = insert_html_element(token);
        m_head_element = to<HTMLHeadElement>(element);
        m_insertion_mode = InsertionMode::InHead;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::insert_comment(HTMLToken& token)
{
    auto data = token.m_comment_or_character.data.to_string();
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    adjusted_insertion_location->append_child(adopt(*new Comment(document(), data)));
}

void HTMLDocumentParser::handle_in_head(HTMLToken& token)
{
    if (token.is_parser_whitespace()) {
        insert_character(token.codepoint());
        return;
    }

    if (token.is_comment()) {
        insert_comment(token);
        return;
    }

    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "html") {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("base", "basefont", "bgsound", "link")) {
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "title") {
        insert_html_element(token);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);
        m_original_insertion_mode = m_insertion_mode;
        m_insertion_mode = InsertionMode::Text;
        return;
    }

    if (token.is_start_tag() && ((token.tag_name() == "noscript" && m_scripting_enabled) || token.tag_name() == "noframes" || token.tag_name() == "style")) {
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "script") {
        auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
        auto element = create_element_for(token);
        auto& script_element = to<HTMLScriptElement>(*element);
        script_element.set_parser_document({}, document());
        script_element.set_non_blocking({}, false);

        if (m_parsing_fragment) {
            TODO();
        }

        if (m_invoked_via_document_write) {
            TODO();
        }

        adjusted_insertion_location->append_child(element, false);
        m_stack_of_open_elements.push(element);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::ScriptData);
        m_original_insertion_mode = m_insertion_mode;
        m_insertion_mode = InsertionMode::Text;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "meta") {
        auto element = insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }
    if (token.is_end_tag() && token.tag_name() == "head") {
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::AfterHead;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_in_head_noscript(HTMLToken&)
{
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::parse_generic_raw_text_element(HTMLToken& token)
{
    insert_html_element(token);
    m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    m_original_insertion_mode = m_insertion_mode;
    m_insertion_mode = InsertionMode::Text;
}

void HTMLDocumentParser::insert_character(u32 data)
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    if (adjusted_insertion_location->is_document())
        return;
    if (adjusted_insertion_location->last_child() && adjusted_insertion_location->last_child()->is_text()) {
        auto& existing_text_node = to<Text>(*adjusted_insertion_location->last_child());
        StringBuilder builder;
        builder.append(existing_text_node.data());
        builder.append(Utf32View { &data, 1 });
        existing_text_node.set_data(builder.to_string());
        return;
    }
    StringBuilder builder;
    builder.append(Utf32View { &data, 1 });
    adjusted_insertion_location->append_child(adopt(*new Text(document(), builder.to_string())));
}

void HTMLDocumentParser::handle_after_head(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.is_parser_whitespace()) {
            insert_character(token.codepoint());
            return;
        }

        ASSERT_NOT_REACHED();
    }

    if (token.is_comment()) {
        ASSERT_NOT_REACHED();
    }

    if (token.is_doctype()) {
        ASSERT_NOT_REACHED();
    }

    if (token.is_start_tag() && token.tag_name() == "html") {
        ASSERT_NOT_REACHED();
    }

    if (token.is_start_tag() && token.tag_name() == "body") {
        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "frameset") {
        ASSERT_NOT_REACHED();
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title")) {
        ASSERT_NOT_REACHED();
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        ASSERT_NOT_REACHED();
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "html", "br")) {
        goto AnythingElse;
    }

    if ((token.is_start_tag() && token.tag_name() == "head") || token.is_end_tag()) {
        ASSERT_NOT_REACHED();
    }

AnythingElse:
    HTMLToken fake_body_token;
    fake_body_token.m_type = HTMLToken::Type::StartTag;
    fake_body_token.m_tag.tag_name.append("body");
    insert_html_element(fake_body_token);
    m_insertion_mode = InsertionMode::InBody;
    // FIXME: Reprocess the current token in InBody!
}

void HTMLDocumentParser::generate_implied_end_tags(const FlyString& exception)
{
    while (current_node().tag_name() != exception && current_node().tag_name().is_one_of("dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc"))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::close_a_p_element()
{
    generate_implied_end_tags("p");
    if (current_node().tag_name() != "p") {
        PARSE_ERROR();
    }
    for (;;) {
        auto popped_element = m_stack_of_open_elements.pop();
        if (popped_element->tag_name() == "p")
            break;
    }
}

void HTMLDocumentParser::handle_after_body(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "html") {
        if (m_parsing_fragment) {
            ASSERT_NOT_REACHED();
        }
        m_insertion_mode = InsertionMode::AfterAfterBody;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_after_after_body(HTMLToken& token)
{
    if (token.is_doctype() || token.is_parser_whitespace() || (token.is_start_tag() && token.tag_name() == "html")) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_of_file()) {
        dbg() << "Stop parsing! :^)";
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::reconstruct_the_active_formatting_elements()
{
    // FIXME: This needs to care about "markers"

    if (m_list_of_active_formatting_elements.is_empty())
        return;

    if (m_stack_of_open_elements.contains(m_list_of_active_formatting_elements.last()))
        return;

    ssize_t index = m_list_of_active_formatting_elements.size() - 1;
    RefPtr<Element> entry = m_list_of_active_formatting_elements.at(index);

Rewind:
    if (m_list_of_active_formatting_elements.size() == 1) {
        goto Create;
    }

    --index;
    entry = m_list_of_active_formatting_elements.at(index);

    if (!m_stack_of_open_elements.contains(*entry))
        goto Rewind;

Advance:
    ++index;
    entry = m_list_of_active_formatting_elements.at(index);

Create:
    // FIXME: Hold on to the real token!
    HTMLToken fake_token;
    fake_token.m_type = HTMLToken::Type::StartTag;
    fake_token.m_tag.tag_name.append(entry->tag_name());
    auto new_element = insert_html_element(fake_token);

    m_list_of_active_formatting_elements.ptr_at(index) = *new_element;

    if (index != (ssize_t)m_list_of_active_formatting_elements.size() - 1)
        goto Advance;
}

void HTMLDocumentParser::handle_in_body(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.codepoint() == 0) {
            ASSERT_NOT_REACHED();
        }
        if (token.is_parser_whitespace()) {
            reconstruct_the_active_formatting_elements();
            insert_character(token.codepoint());
            return;
        }
        reconstruct_the_active_formatting_elements();
        insert_character(token.codepoint());
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "body") {
        if (!m_stack_of_open_elements.has_in_scope("body")) {
            ASSERT_NOT_REACHED();
        }

        // FIXME: Otherwise, if there is a node in the stack of open elements that is
        // not either a dd element, a dt element, an li element, an optgroup element,
        // an option element, a p element, an rb element, an rp element, an rt element,
        // an rtc element, a tbody element, a td element, a tfoot element, a th element,
        // a thead element, a tr element, the body element, or the html element,
        // then this is a parse error.

        m_insertion_mode = InsertionMode::AfterBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        if (current_node().tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
            // FIXME: This is a parse error!
            TODO();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
        if (!m_stack_of_open_elements.has_in_scope("h1")
            && !m_stack_of_open_elements.has_in_scope("h2")
            && !m_stack_of_open_elements.has_in_scope("h3")
            && !m_stack_of_open_elements.has_in_scope("h4")
            && !m_stack_of_open_elements.has_in_scope("h5")
            && !m_stack_of_open_elements.has_in_scope("h6")) {
            TODO();
        }

        generate_implied_end_tags();
        if (current_node().tag_name() != token.tag_name()) {
            TODO();
        }

        for (;;) {
            auto popped_element = m_stack_of_open_elements.pop();
            if (popped_element->tag_name() == "h1"
                || popped_element->tag_name() == "h2"
                || popped_element->tag_name() == "h3"
                || popped_element->tag_name() == "h4"
                || popped_element->tag_name() == "h5"
                || popped_element->tag_name() == "h6") {
                break;
            }
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "p") {
        if (!m_stack_of_open_elements.has_in_button_scope("p")) {
            TODO();
        }
        close_a_p_element();
        return;
    }

    {
        if (token.is_start_tag() && token.tag_name().is_one_of("b", "big", "code", "em", "font", "i", "s", "small", "strike", "strong", "tt", "u")) {
            reconstruct_the_active_formatting_elements();
            auto element = insert_html_element(token);
            m_list_of_active_formatting_elements.append(*element);
            return;
        }
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("address", "article", "aside", "blockquote", "center", "details", "dialog", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "main", "menu", "nav", "ol", "p", "section", "summary", "ul")) {
        // FIXME: If the stack of open elements has a p element in button scope, then close a p element.
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("address", "article", "aside", "blockquote", "center", "details", "dialog", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "main", "menu", "nav", "ol", "p", "section", "summary", "ul")) {
        // FIXME: If the stack of open elements has a p element in button scope, then close a p element.

        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            ASSERT_NOT_REACHED();
        }

        generate_implied_end_tags();

        if (current_node().tag_name() != token.tag_name()) {
            ASSERT_NOT_REACHED();
        }

        m_stack_of_open_elements.pop();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "table") {
        // FIXME: If the Document is not set to quirks mode,
        //        and the stack of open elements has a p element in button scope, then close a p element.

        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if (token.is_start_tag()) {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag()) {
        RefPtr<Element> node;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            node = m_stack_of_open_elements.elements()[i];
            if (node->tag_name() == token.tag_name()) {
                generate_implied_end_tags(token.tag_name());
                if (node != current_node()) {
                    // It's a parse error
                    TODO();
                }
                while (&current_node() != node) {
                    m_stack_of_open_elements.pop();
                }
                m_stack_of_open_elements.pop();
                break;
            }
            // FIXME: Handle special elements!
        }
        return;
    }

    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::increment_script_nesting_level()
{
    ++m_script_nesting_level;
}

void HTMLDocumentParser::decrement_script_nesting_level()
{
    ASSERT(m_script_nesting_level);
    --m_script_nesting_level;
}

void HTMLDocumentParser::handle_text(HTMLToken& token)
{
    if (token.is_character()) {
        insert_character(token.codepoint());
        return;
    }
    if (token.is_end_tag() && token.tag_name() == "script") {
        NonnullRefPtr<HTMLScriptElement> script = to<HTMLScriptElement>(current_node());
        m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        // FIXME: Handle tokenizer insertion point stuff here.
        increment_script_nesting_level();
        script->prepare_script({});
        decrement_script_nesting_level();
        if (script_nesting_level() == 0)
            m_parser_pause_flag = false;
        // FIXME: Handle tokenizer insertion point stuff here too.
        return;
    }
    if (token.is_end_tag()) {
        m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_in_table(HTMLToken& token)
{
    if (token.is_character() && current_node().tag_name().is_one_of("table", "tbody", "tfoot", "thead", "tr")) {
        TODO();
    }
    if (token.is_comment()) {
        insert_comment(token);
        return;
    }
    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }
    if (token.is_start_tag() && token.tag_name() == "caption") {
        TODO();
    }
    if (token.is_start_tag() && token.tag_name() == "colgroup") {
        TODO();
    }
    if (token.is_start_tag() && token.tag_name() == "col") {
        TODO();
    }
    if (token.is_start_tag() && token.tag_name().is_one_of("tbody", "tfoot", "thead")) {
        TODO();
    }
    if (token.is_start_tag() && token.tag_name().is_one_of("td", "th", "tr")) {
        TODO();
    }
    if (token.is_start_tag() && token.tag_name() == "table") {
        PARSE_ERROR();
        TODO();
    }
    if (token.is_end_tag()) {
        if (!m_stack_of_open_elements.has_in_table_scope("table")) {
            PARSE_ERROR();
            return;
        }
        while (current_node().tag_name() != "table")
            m_stack_of_open_elements.pop();
        m_stack_of_open_elements.pop();

        reset_the_insertion_mode_appropriately();
        return;
    }
    TODO();
}

void HTMLDocumentParser::reset_the_insertion_mode_appropriately()
{
    TODO();
}

const char* HTMLDocumentParser::insertion_mode_name() const
{
    switch (m_insertion_mode) {
#define __ENUMERATE_INSERTION_MODE(mode) \
    case InsertionMode::mode:            \
        return #mode;
        ENUMERATE_INSERTION_MODES
#undef __ENUMERATE_INSERTION_MODE
    }
    ASSERT_NOT_REACHED();
}

Document& HTMLDocumentParser::document()
{
    return *m_document;
}
}
