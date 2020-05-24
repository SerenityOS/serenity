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

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLFormElement.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/Parser/HTMLDocumentParser.h>
#include <LibWeb/Parser/HTMLToken.h>

namespace Web {

HTMLDocumentParser::HTMLDocumentParser(const StringView& input)
    : m_tokenizer(input)
{
}

HTMLDocumentParser::~HTMLDocumentParser()
{
}

void HTMLDocumentParser::run()
{
    m_document = adopt(*new Document);

    for (;;) {
        auto optional_token = m_tokenizer.next_token();
        if (!optional_token.has_value())
            return;
        auto& token = optional_token.value();

        dbg() << "[" << insertion_mode_name() << "] " << token.to_string();

        switch (m_insertion_mode) {
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
        default:
            ASSERT_NOT_REACHED();
        }
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
    if (token.is_start_tag() && token.tag_name() == "head") {
        auto element = insert_html_element(token);
        m_head_element = to<HTMLHeadElement>(element);
        m_insertion_mode = InsertionMode::InHead;
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_in_head(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name() == "meta") {
        auto element = insert_html_element(token);
        m_stack_of_open_elements.pop();
        if (token.is_self_closing()) {
            ASSERT_NOT_REACHED();
        }
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

void HTMLDocumentParser::handle_after_head(HTMLToken& token)
{
    if (token.is_character()) {
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

    {
        Vector<String> names = { "base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title" };
        if (token.is_start_tag() && names.contains_slow(token.tag_name())) {
            ASSERT_NOT_REACHED();
        }
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        ASSERT_NOT_REACHED();
    }

    if (token.is_end_tag() && (token.tag_name() == "body" || token.tag_name() == "html" || token.tag_name() == "br")) {
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

void HTMLDocumentParser::generate_implied_end_tags()
{
    Vector<String> names { "dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc" };
    while (names.contains_slow(current_node().tag_name()))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::handle_after_body(HTMLToken& token)
{
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
    if (token.is_end_of_file()) {
        dbg() << "Stop parsing! :^)";
        return;
    }
    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_in_body(HTMLToken& token)
{
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

    {
        Vector<String> names { "address", "article", "aside", "blockquote", "center", "details", "dialog", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "main", "menu", "nav", "ol", "p", "section", "summary", "ul" };
        if (token.is_start_tag() && names.contains_slow(token.tag_name())) {
            // FIXME: If the stack of open elements has a p element in button scope, then close a p element.
            insert_html_element(token);
            return;
        }

        if (token.is_end_tag() && names.contains_slow(token.tag_name())) {
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
    }

    ASSERT_NOT_REACHED();
}

void HTMLDocumentParser::handle_text(HTMLToken&)
{
    ASSERT_NOT_REACHED();
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
