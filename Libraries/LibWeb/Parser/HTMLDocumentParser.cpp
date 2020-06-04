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

//#define PARSER_DEBUG

#include <AK/Utf32View.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLFormElement.h>
#include <LibWeb/DOM/HTMLHeadElement.h>
#include <LibWeb/DOM/HTMLScriptElement.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Parser/HTMLDocumentParser.h>
#include <LibWeb/Parser/HTMLToken.h>

#define PARSE_ERROR()                                                         \
    do {                                                                      \
        dbg() << "Parse error! " << __PRETTY_FUNCTION__ << " @ " << __LINE__; \
    } while (0)

namespace Web {

HTMLDocumentParser::HTMLDocumentParser(const StringView& input, const String& encoding)
    : m_tokenizer(input, encoding)
{
}

HTMLDocumentParser::~HTMLDocumentParser()
{
}

void HTMLDocumentParser::run(const URL& url)
{
    m_document = adopt(*new Document);
    m_document->set_url(url);
    m_document->set_source(m_tokenizer.source());

    for (;;) {
        auto optional_token = m_tokenizer.next_token();
        if (!optional_token.has_value())
            break;
        auto& token = optional_token.value();

#ifdef PARSER_DEBUG
        dbg() << "[" << insertion_mode_name() << "] " << token.to_string();
#endif
        process_using_the_rules_for(m_insertion_mode, token);

        if (m_stop_parsing) {
            dbg() << "Stop parsing! :^)";
            break;
        }
    }

    flush_character_insertions();

    // "The end"

    auto scripts_to_execute_when_parsing_has_finished = m_document->take_scripts_to_execute_when_parsing_has_finished({});
    for (auto& script : scripts_to_execute_when_parsing_has_finished) {
        script.execute_script();
    }

    m_document->dispatch_event(Event::create("DOMContentLoaded"));

    auto scripts_to_execute_as_soon_as_possible = m_document->take_scripts_to_execute_as_soon_as_possible({});
    for (auto& script : scripts_to_execute_as_soon_as_possible) {
        script.execute_script();
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
    case InsertionMode::InTableBody:
        handle_in_table_body(token);
        break;
    case InsertionMode::InRow:
        handle_in_row(token);
        break;
    case InsertionMode::InCell:
        handle_in_cell(token);
        break;
    case InsertionMode::InTableText:
        handle_in_table_text(token);
        break;
    case InsertionMode::InSelectInTable:
        handle_in_select_in_table(token);
        break;
    case InsertionMode::InSelect:
        handle_in_select(token);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

void HTMLDocumentParser::handle_initial(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_comment()) {
        auto comment = adopt(*new Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype()) {
        auto doctype = adopt(*new DocumentType(document()));
        doctype->set_name(token.m_doctype.name.to_string());
        document().append_child(move(doctype));
        m_insertion_mode = InsertionMode::BeforeHTML;
        return;
    }

    PARSE_ERROR();
    document().set_quirks_mode(true);
    m_insertion_mode = InsertionMode::BeforeHTML;
    process_using_the_rules_for(InsertionMode::BeforeHTML, token);
}

void HTMLDocumentParser::handle_before_html(HTMLToken& token)
{
    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_comment()) {
        auto comment = adopt(*new Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

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

    if (token.is_end_tag() && token.tag_name().is_one_of("head", "body", "html", "br")) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    auto element = create_element(document(), "html");
    document().append_child(element);
    m_stack_of_open_elements.push(element);
    // FIXME: If the Document is being loaded as part of navigation of a browsing context, then: run the application cache selection algorithm with no manifest, passing it the Document object.
    m_insertion_mode = InsertionMode::BeforeHead;
    process_using_the_rules_for(InsertionMode::BeforeHead, token);
    return;
}

Element& HTMLDocumentParser::current_node()
{
    return m_stack_of_open_elements.current_node();
}

Element& HTMLDocumentParser::node_before_current_node()
{
    return m_stack_of_open_elements.elements().at(m_stack_of_open_elements.elements().size() - 2);
}

RefPtr<Node> HTMLDocumentParser::find_appropriate_place_for_inserting_node()
{
    auto& target = current_node();
    if (m_foster_parenting) {
        TODO();
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

    if (token.is_start_tag() && token.tag_name() == "head") {
        auto element = insert_html_element(token);
        m_head_element = to<HTMLHeadElement>(element);
        m_insertion_mode = InsertionMode::InHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("head", "body", "html", "br")) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    HTMLToken fake_head_token;
    fake_head_token.m_type = HTMLToken::Type::StartTag;
    fake_head_token.m_tag.tag_name.append("head");
    m_head_element = to<HTMLHeadElement>(insert_html_element(fake_head_token));
    m_insertion_mode = InsertionMode::InHead;
    process_using_the_rules_for(InsertionMode::InHead, token);
    return;
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

    if (token.is_start_tag() && token.tag_name() == "meta") {
        auto element = insert_html_element(token);
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
    if (token.is_end_tag() && token.tag_name() == "head") {
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::AfterHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "html", "br")) {
        TODO();
    }

    if (token.is_start_tag() && token.tag_name() == "template") {
        // FIXME: Support this properly
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        // FIXME: Support this properly
        ASSERT(current_node().tag_name() == "template");
        m_stack_of_open_elements.pop();
        return;
    }

    if ((token.is_start_tag() && token.tag_name() == "head") || token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

    m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::AfterHead;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::handle_in_head_noscript(HTMLToken&)
{
    TODO();
}

void HTMLDocumentParser::parse_generic_raw_text_element(HTMLToken& token)
{
    insert_html_element(token);
    m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    m_original_insertion_mode = m_insertion_mode;
    m_insertion_mode = InsertionMode::Text;
}

Text* HTMLDocumentParser::find_character_insertion_node()
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    if (adjusted_insertion_location->is_document())
        return nullptr;
    if (adjusted_insertion_location->last_child() && adjusted_insertion_location->last_child()->is_text())
        return to<Text>(adjusted_insertion_location->last_child());
    auto new_text_node = adopt(*new Text(document(), ""));
    adjusted_insertion_location->append_child(new_text_node);
    return new_text_node;
}

void HTMLDocumentParser::flush_character_insertions()
{
    if (m_character_insertion_builder.is_empty())
        return;
    m_character_insertion_node->set_data(m_character_insertion_builder.to_string());
    m_character_insertion_node->parent()->children_changed();
    m_character_insertion_builder.clear();
}

void HTMLDocumentParser::insert_character(u32 data)
{
    auto node = find_character_insertion_node();
    if (node == m_character_insertion_node) {
        m_character_insertion_builder.append(Utf32View { &data, 1 });
        return;
    }
    if (!m_character_insertion_node) {
        m_character_insertion_node = node;
        m_character_insertion_builder.append(Utf32View { &data, 1 });
        return;
    }
    flush_character_insertions();
    m_character_insertion_node = node;
    m_character_insertion_builder.append(Utf32View { &data, 1 });
}

void HTMLDocumentParser::handle_after_head(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
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

    if (token.is_start_tag() && token.tag_name() == "body") {
        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "frameset") {
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InFrameset;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title")) {
        PARSE_ERROR();
        m_stack_of_open_elements.push(*m_head_element);
        process_using_the_rules_for(InsertionMode::InHead, token);
        m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) {
            return entry.ptr() == m_head_element;
        });
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        TODO();
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "html", "br")) {
        goto AnythingElse;
    }

    if ((token.is_start_tag() && token.tag_name() == "head") || token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    HTMLToken fake_body_token;
    fake_body_token.m_type = HTMLToken::Type::StartTag;
    fake_body_token.m_tag.tag_name.append("body");
    insert_html_element(fake_body_token);
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(m_insertion_mode, token);
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
    m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("p");
}

void HTMLDocumentParser::handle_after_body(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_comment()) {
        TODO();
    }

    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "html") {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "html") {
        if (m_parsing_fragment) {
            TODO();
        }
        m_insertion_mode = InsertionMode::AfterAfterBody;
        return;
    }

    PARSE_ERROR();
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLDocumentParser::handle_after_after_body(HTMLToken& token)
{
    if (token.is_comment()) {
        auto comment = adopt(*new Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype() || token.is_parser_whitespace() || (token.is_start_tag() && token.tag_name() == "html")) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    PARSE_ERROR();
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::reconstruct_the_active_formatting_elements()
{
    // FIXME: This needs to care about "markers"

    if (m_list_of_active_formatting_elements.is_empty())
        return;

    if (m_list_of_active_formatting_elements.entries().last().is_marker())
        return;

    if (m_stack_of_open_elements.contains(*m_list_of_active_formatting_elements.entries().last().element))
        return;

    ssize_t index = m_list_of_active_formatting_elements.entries().size() - 1;
    RefPtr<Element> entry = m_list_of_active_formatting_elements.entries().at(index).element;
    ASSERT(entry);

Rewind:
    if (index == 0) {
        goto Create;
    }

    --index;
    entry = m_list_of_active_formatting_elements.entries().at(index).element;
    ASSERT(entry);

    if (!m_stack_of_open_elements.contains(*entry))
        goto Rewind;

Advance:
    ++index;
    entry = m_list_of_active_formatting_elements.entries().at(index).element;
    ASSERT(entry);

Create:
    // FIXME: Hold on to the real token!
    HTMLToken fake_token;
    fake_token.m_type = HTMLToken::Type::StartTag;
    fake_token.m_tag.tag_name.append(entry->tag_name());
    auto new_element = insert_html_element(fake_token);

    m_list_of_active_formatting_elements.entries().at(index).element = *new_element;

    if (index != (ssize_t)m_list_of_active_formatting_elements.entries().size() - 1)
        goto Advance;
}

HTMLDocumentParser::AdoptionAgencyAlgorithmOutcome HTMLDocumentParser::run_the_adoption_agency_algorithm(HTMLToken& token)
{
    auto subject = token.tag_name();

    // If the current node is an HTML element whose tag name is subject,
    // and the current node is not in the list of active formatting elements,
    // then pop the current node off the stack of open elements, and return.
    if (current_node().tag_name() == subject && !m_list_of_active_formatting_elements.contains(current_node())) {
        m_stack_of_open_elements.pop();
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    size_t outer_loop_counter = 0;

    //OuterLoop:
    if (outer_loop_counter >= 8)
        return AdoptionAgencyAlgorithmOutcome::DoNothing;

    ++outer_loop_counter;

    auto formatting_element = m_list_of_active_formatting_elements.last_element_with_tag_name_before_marker(subject);
    if (!formatting_element)
        return AdoptionAgencyAlgorithmOutcome::RunAnyOtherEndTagSteps;

    if (!m_stack_of_open_elements.contains(*formatting_element)) {
        PARSE_ERROR();
        // FIXME: If formatting element is not in the stack of open elements,
        // then this is a parse error; remove the element from the list, and return.
        TODO();
    }

    if (!m_stack_of_open_elements.has_in_scope(*formatting_element)) {
        PARSE_ERROR();
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    if (formatting_element != &current_node()) {
        PARSE_ERROR();
    }

    RefPtr<Element> furthest_block = m_stack_of_open_elements.topmost_special_node_below(*formatting_element);

    if (!furthest_block) {
        while (&current_node() != formatting_element)
            m_stack_of_open_elements.pop();
        m_stack_of_open_elements.pop();

        m_list_of_active_formatting_elements.remove(*formatting_element);
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    // FIXME: Implement the rest of the AAA :^)

    TODO();
}

bool HTMLDocumentParser::is_special_tag(const FlyString& tag_name)
{
    return tag_name.is_one_of(
        "address",
        "applet",
        "area",
        "article",
        "aside",
        "base",
        "basefont",
        "bgsound",
        "blockquote",
        "body",
        "br",
        "button",
        "caption",
        "center",
        "col",
        "colgroup",
        "dd",
        "details",
        "dir",
        "div",
        "dl",
        "dt",
        "embed",
        "fieldset",
        "figcaption",
        "figure",
        "footer",
        "form",
        "frame",
        "frameset",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "head",
        "header",
        "hgroup",
        "hr",
        "html",
        "iframe",
        "img",
        "input",
        "keygen",
        "li",
        "link",
        "listing",
        "main",
        "marquee",
        "menu",
        "meta",
        "nav",
        "noembed",
        "noframes",
        "noscript",
        "object",
        "ol",
        "p",
        "param",
        "plaintext",
        "pre",
        "script",
        "section",
        "select",
        "source",
        "style",
        "summary",
        "table",
        "tbody",
        "td",
        "template",
        "textarea",
        "tfoot",
        "th",
        "thead",
        "title",
        "tr",
        "track",
        "ul",
        "wbr",
        "xmp");
}

void HTMLDocumentParser::handle_in_body(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.codepoint() == 0) {
            PARSE_ERROR();
            return;
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

    if (token.is_comment()) {
        insert_comment(token);
        return;
    }

    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "html") {
        PARSE_ERROR();
        if (m_stack_of_open_elements.contains("template"))
            return;
        for (auto& attribute : token.m_tag.attributes) {
            if (current_node().has_attribute(attribute.name_builder.string_view()))
                continue;
            current_node().set_attribute(attribute.name_builder.to_string(), attribute.value_builder.to_string());
        }
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of("base", "basefont", "bgsound", "link", "meta", "noframes", "script", "style", "template", "title")) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "body") {
        PARSE_ERROR();
        if (m_stack_of_open_elements.elements().size() == 1
            || node_before_current_node().tag_name() != "body"
            || m_stack_of_open_elements.contains("template")) {
            return;
        }
        m_frameset_ok = false;
        for (auto& attribute : token.m_tag.attributes) {
            if (node_before_current_node().has_attribute(attribute.name_builder.string_view()))
                continue;
            node_before_current_node().set_attribute(attribute.name_builder.to_string(), attribute.value_builder.to_string());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "frameset") {
        TODO();
    }

    if (token.is_end_of_file()) {
        // FIXME: If the stack of template insertion modes is not empty,
        // then process the token using the rules for the "in template" insertion mode.

        // FIXME: If there is a node in the stack of open elements that is not either
        // a dd element, a dt element, an li element, an optgroup element, an option element,
        // a p element, an rb element, an rp element, an rt element, an rtc element,
        // a tbody element, a td element, a tfoot element, a th element, a thead element,
        // a tr element, the body element, or the html element, then this is a parse error.

        stop_parsing();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "body") {
        if (!m_stack_of_open_elements.has_in_scope("body")) {
            PARSE_ERROR();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.tag_name().is_one_of("dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc", "tbody", "td", "tfoot", "th", "thead", "tr", "body", "html")) {
                PARSE_ERROR();
                break;
            }
        }

        m_insertion_mode = InsertionMode::AfterBody;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "html") {
        if (!m_stack_of_open_elements.has_in_scope("body")) {
            PARSE_ERROR();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.tag_name().is_one_of("dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc", "tbody", "td", "tfoot", "th", "thead", "tr", "body", "html")) {
                PARSE_ERROR();
                break;
            }
        }

        m_insertion_mode = InsertionMode::AfterBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("address", "article", "aside", "blockquote", "center", "details", "dialog", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "main", "menu", "nav", "ol", "p", "section", "summary", "ul")) {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        if (current_node().tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
            PARSE_ERROR();
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("pre", "listing")) {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();

        insert_html_element(token);

        m_frameset_ok = false;

        // If the next token is a U+000A LINE FEED (LF) character token,
        // then ignore that token and move on to the next one.
        // (Newlines at the start of pre blocks are ignored as an authoring convenience.)
        auto next_token = m_tokenizer.next_token();
        if (next_token.has_value() && next_token.value().is_character() && next_token.value().codepoint() == '\n') {
            // Ignore it.
        } else {
            process_using_the_rules_for(m_insertion_mode, next_token.value());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "form") {
        if (m_form_element && m_stack_of_open_elements.contains("template")) {
            PARSE_ERROR();
            return;
        }
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        auto element = insert_html_element(token);
        if (!m_stack_of_open_elements.contains("template"))
            m_form_element = to<HTMLFormElement>(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "li") {
        m_frameset_ok = false;

        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<Element> node = m_stack_of_open_elements.elements()[i];

            if (node->tag_name() == "li") {
                generate_implied_end_tags("li");
                if (current_node().tag_name() != "li") {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("li");
                break;
            }

            if (is_special_tag(node->tag_name()) && !node->tag_name().is_one_of("address", "div", "p"))
                break;
        }

        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();

        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("dd", "dt")) {
        m_frameset_ok = false;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<Element> node = m_stack_of_open_elements.elements()[i];
            if (node->tag_name() == "dd") {
                generate_implied_end_tags("dd");
                if (current_node().tag_name() != "dd") {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("dd");
                break;
            }
            if (node->tag_name() == "dt") {
                generate_implied_end_tags("dt");
                if (current_node().tag_name() != "dt") {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("dt");
                break;
            }
            if (is_special_tag(node->tag_name()) && !node->tag_name().is_one_of("address", "div", "p"))
                break;
        }
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "plaintext") {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        insert_html_element(token);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::PLAINTEXT);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "button") {
        if (m_stack_of_open_elements.has_in_button_scope("button")) {
            PARSE_ERROR();
            generate_implied_end_tags();
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("button");
        }
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("address", "article", "aside", "blockquote", "button", "center", "details", "dialog", "dir", "div", "dl", "fieldset", "figcaption", "figure", "footer", "header", "hgroup", "listing", "main", "menu", "nav", "ol", "pre", "section", "summary", "ul")) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();

        if (current_node().tag_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "form") {
        if (!m_stack_of_open_elements.contains("template")) {
            auto node = m_form_element;
            m_form_element = nullptr;
            if (!node || m_stack_of_open_elements.has_in_scope(*node)) {
                PARSE_ERROR();
                return;
            }
            generate_implied_end_tags();
            if (&current_node() != node) {
                PARSE_ERROR();
            }
            m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) { return entry.ptr() == node.ptr(); });
        } else {
            if (!m_stack_of_open_elements.has_in_scope("form")) {
                PARSE_ERROR();
                return;
            }
            generate_implied_end_tags();
            if (current_node().tag_name() != "form") {
                PARSE_ERROR();
            }
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("form");
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "p") {
        if (!m_stack_of_open_elements.has_in_button_scope("p")) {
            PARSE_ERROR();
            HTMLToken fake_p_token;
            fake_p_token.m_type = HTMLToken::Type::StartTag;
            fake_p_token.m_tag.tag_name.append("p");
            insert_html_element(fake_p_token);
        }
        close_a_p_element();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "li") {
        if (!m_stack_of_open_elements.has_in_list_item_scope("li")) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags("li");
        if (current_node().tag_name() != "li") {
            PARSE_ERROR();
            dbg() << "Expected <li> current node, but had <" << current_node().tag_name() << ">";
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("li");
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("dd", "dt")) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags(token.tag_name());
        if (current_node().tag_name() != token.tag_name()) {
            PARSE_ERROR();
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6")) {
        if (!m_stack_of_open_elements.has_in_scope("h1")
            && !m_stack_of_open_elements.has_in_scope("h2")
            && !m_stack_of_open_elements.has_in_scope("h3")
            && !m_stack_of_open_elements.has_in_scope("h4")
            && !m_stack_of_open_elements.has_in_scope("h5")
            && !m_stack_of_open_elements.has_in_scope("h6")) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();
        if (current_node().tag_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        for (;;) {
            auto popped_element = m_stack_of_open_elements.pop();
            if (popped_element->tag_name().is_one_of("h1", "h2", "h3", "h4", "h5", "h6"))
                break;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "a") {
        if (auto* element = m_list_of_active_formatting_elements.last_element_with_tag_name_before_marker("a")) {
            PARSE_ERROR();
            if (run_the_adoption_agency_algorithm(token) == AdoptionAgencyAlgorithmOutcome::RunAnyOtherEndTagSteps)
                goto AnyOtherEndTag;
            m_list_of_active_formatting_elements.remove(*element);
            m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) {
                return entry.ptr() == element;
            });
        }
        reconstruct_the_active_formatting_elements();
        auto element = insert_html_element(token);
        m_list_of_active_formatting_elements.add(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("b", "big", "code", "em", "font", "i", "s", "small", "strike", "strong", "tt", "u")) {
        reconstruct_the_active_formatting_elements();
        auto element = insert_html_element(token);
        m_list_of_active_formatting_elements.add(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "nobr") {
        reconstruct_the_active_formatting_elements();
        if (m_stack_of_open_elements.has_in_scope("nobr")) {
            PARSE_ERROR();
            run_the_adoption_agency_algorithm(token);
            reconstruct_the_active_formatting_elements();
        }
        auto element = insert_html_element(token);
        m_list_of_active_formatting_elements.add(*element);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("a", "b", "big", "code", "em", "font", "i", "nobr", "s", "small", "strike", "strong", "tt", "u")) {
        if (run_the_adoption_agency_algorithm(token) == AdoptionAgencyAlgorithmOutcome::RunAnyOtherEndTagSteps)
            goto AnyOtherEndTag;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("applet", "marquee", "object")) {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_list_of_active_formatting_elements.add_marker();
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("applet", "marquee", "object")) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();
        if (current_node().tag_name() != token.tag_name()) {
            PARSE_ERROR();
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "table") {
        if (!document().in_quirks_mode()) {
            if (m_stack_of_open_elements.has_in_button_scope("p"))
                close_a_p_element();
        }
        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "br") {
        token.drop_attributes();
        goto BRStartTag;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("area", "br", "embed", "img", "keygen", "wbr")) {
    BRStartTag:
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "input") {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        auto type_attribute = token.attribute(HTML::AttributeNames::type);
        if (type_attribute.is_null() || type_attribute != "hidden") {
            m_frameset_ok = false;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("param", "source", "track")) {
        TODO();
    }

    if (token.is_start_tag() && token.tag_name() == "hr") {
        if (m_stack_of_open_elements.has_in_button_scope("p"))
            close_a_p_element();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "image") {
        // Parse error. Change the token's tag name to "img" and reprocess it. (Don't ask.)
        PARSE_ERROR();
        token.m_tag.tag_name.clear();
        token.m_tag.tag_name.append("img");
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "textarea") {
        insert_html_element(token);

        // If the next token is a U+000A LINE FEED (LF) character token,
        // then ignore that token and move on to the next one.
        // (Newlines at the start of pre blocks are ignored as an authoring convenience.)
        auto next_token = m_tokenizer.next_token();

        m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);
        m_original_insertion_mode = m_insertion_mode;
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::Text;

        if (next_token.has_value() && next_token.value().is_character() && next_token.value().codepoint() == '\n') {
            // Ignore it.
        } else {
            process_using_the_rules_for(m_insertion_mode, next_token.value());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "xmp") {
        if (m_stack_of_open_elements.has_in_button_scope("p")) {
            close_a_p_element();
        }
        reconstruct_the_active_formatting_elements();
        m_frameset_ok = false;
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "iframe") {
        m_frameset_ok = false;
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && ((token.tag_name() == "noembed") || (token.tag_name() == "noscript" && m_scripting_enabled))) {
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "select") {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_frameset_ok = false;
        switch (m_insertion_mode) {
        case InsertionMode::InTable:
        case InsertionMode::InCaption:
        case InsertionMode::InTableBody:
        case InsertionMode::InRow:
        case InsertionMode::InCell:
            m_insertion_mode = InsertionMode::InSelectInTable;
            break;
        default:
            m_insertion_mode = InsertionMode::InSelect;
            break;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("optgroup", "option")) {
        if (current_node().tag_name() == "option")
            m_stack_of_open_elements.pop();
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("rb", "rtc")) {
        TODO();
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("rp", "rt")) {
        TODO();
    }

    if (token.is_start_tag() && token.tag_name() == "math") {
        dbg() << "<math> element encountered.";
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "svg") {
        dbg() << "<svg> element encountered.";
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of("caption", "col", "colgroup", "frame", "head", "tbody", "td", "tfoot", "th", "thead", "tr"))) {
        PARSE_ERROR();
        return;
    }

    // Any other start tag
    if (token.is_start_tag()) {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag()) {
    AnyOtherEndTag:
        RefPtr<Element> node;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            node = m_stack_of_open_elements.elements()[i];
            if (node->tag_name() == token.tag_name()) {
                generate_implied_end_tags(token.tag_name());
                if (node != current_node()) {
                    PARSE_ERROR();
                }
                while (&current_node() != node) {
                    m_stack_of_open_elements.pop();
                }
                m_stack_of_open_elements.pop();
                break;
            }
            if (is_special_tag(node->tag_name())) {
                PARSE_ERROR();
                return;
            }
        }
        return;
    }

    TODO();
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

        while (document().pending_parsing_blocking_script()) {
            if (script_nesting_level() != 0) {
                m_parser_pause_flag = true;
                // FIXME: Abort the processing of any nested invocations of the tokenizer,
                //        yielding control back to the caller. (Tokenization will resume when
                //        the caller returns to the "outer" tree construction stage.)
                TODO();
            } else {
                auto the_script = document().take_pending_parsing_blocking_script({});
                m_tokenizer.set_blocked(true);

                // FIXME: If the parser's Document has a style sheet that is blocking scripts
                //        or the script's "ready to be parser-executed" flag is not set:
                //        spin the event loop until the parser's Document has no style sheet
                //        that is blocking scripts and the script's "ready to be parser-executed"
                //        flag is set.

                ASSERT(the_script->is_ready_to_be_parser_executed());

                if (m_aborted)
                    return;

                m_tokenizer.set_blocked(false);

                // FIXME: Handle tokenizer insertion point stuff here too.

                ASSERT(script_nesting_level() == 0);
                increment_script_nesting_level();

                the_script->execute_script();

                decrement_script_nesting_level();
                ASSERT(script_nesting_level() == 0);
                m_parser_pause_flag = false;

                // FIXME: Handle tokenizer insertion point stuff here too.
            }
        }
        return;
    }

    if (token.is_end_tag()) {
        m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        return;
    }
    TODO();
}

void HTMLDocumentParser::clear_the_stack_back_to_a_table_context()
{
    while (!current_node().tag_name().is_one_of("table", "template", "html"))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::clear_the_stack_back_to_a_table_row_context()
{
    while (!current_node().tag_name().is_one_of("tr", "template", "html"))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::clear_the_stack_back_to_a_table_body_context()
{
    while (!current_node().tag_name().is_one_of("tbody", "tfoot", "thead", "template", "html"))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::handle_in_row(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name().is_one_of("th", "td")) {
        clear_the_stack_back_to_a_table_row_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InCell;
        m_list_of_active_formatting_elements.add_marker();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "tr") {
        if (!m_stack_of_open_elements.has_in_table_scope("tr")) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("caption", "col", "colgroup", "tbody", "tfoot", "thead", "tr")) {
        if (m_stack_of_open_elements.has_in_table_scope("tr")) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("tbody", "tfoot", "thead")) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        if (!m_stack_of_open_elements.has_in_table_scope("tr")) {
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "caption", "col", "colgroup", "html", "td", "th")) {
        PARSE_ERROR();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLDocumentParser::close_the_cell()
{
    generate_implied_end_tags();
    if (!current_node().tag_name().is_one_of("td", "th")) {
        PARSE_ERROR();
    }
    while (!current_node().tag_name().is_one_of("td", "th"))
        m_stack_of_open_elements.pop();
    m_stack_of_open_elements.pop();
    m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
    m_insertion_mode = InsertionMode::InRow;
}

void HTMLDocumentParser::handle_in_cell(HTMLToken& token)
{
    if (token.is_end_tag() && token.tag_name().is_one_of("td", "th")) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags();

        if (current_node().tag_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());

        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InRow;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of("caption", "col", "colgroup", "tbody", "td", "tfoot", "th", "thead", "tr")) {
        if (!m_stack_of_open_elements.has_in_table_scope("td") && m_stack_of_open_elements.has_in_table_scope("th")) {
            PARSE_ERROR();
            return;
        }
        close_the_cell();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "caption", "col", "colgroup", "html")) {
        PARSE_ERROR();
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("table", "tbody", "tfoot", "thead", "tr")) {
        if (m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        close_the_cell();
        // Reprocess the token.
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLDocumentParser::handle_in_table_text(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.codepoint() == 0) {
            PARSE_ERROR();
            return;
        }

        m_pending_table_character_tokens.append(token);
        return;
    }

    for (auto& pending_token : m_pending_table_character_tokens) {
        ASSERT(pending_token.is_character());
        if (!pending_token.is_parser_whitespace()) {
            // FIXME: If any of the tokens in the pending table character tokens list
            // are character tokens that are not ASCII whitespace, then this is a parse error:
            // reprocess the character tokens in the pending table character tokens list using
            // the rules given in the "anything else" entry in the "in table" insertion mode.
            TODO();
        }
    }

    for (auto& pending_token : m_pending_table_character_tokens) {
        insert_character(pending_token.codepoint());
    }

    m_insertion_mode = m_original_insertion_mode;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::handle_in_table_body(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name() == "tr") {
        clear_the_stack_back_to_a_table_body_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InRow;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("th", "td")) {
        PARSE_ERROR();
        clear_the_stack_back_to_a_table_body_context();

        HTMLToken fake_tr_token;
        fake_tr_token.m_type = HTMLToken::Type::StartTag;
        fake_tr_token.m_tag.tag_name.append("tr");
        insert_html_element(fake_tr_token);

        m_insertion_mode = InsertionMode::InRow;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("tbody", "tfoot", "thead")) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_body_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of("caption", "col", "colgroup", "tbody", "tfoot", "thead"))
        || (token.is_end_tag() && token.tag_name() == "table")) {
        // FIXME: If the stack of open elements does not have a tbody, thead, or tfoot element in table scope, this is a parse error; ignore the token.

        clear_the_stack_back_to_a_table_body_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(InsertionMode::InTable, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of("body", "caption", "col", "colgroup", "html", "td", "th", "tr")) {
        PARSE_ERROR();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLDocumentParser::handle_in_table(HTMLToken& token)
{
    if (token.is_character() && current_node().tag_name().is_one_of("table", "tbody", "tfoot", "thead", "tr")) {
        m_pending_table_character_tokens.clear();
        m_original_insertion_mode = m_insertion_mode;
        m_insertion_mode = InsertionMode::InTableText;
        process_using_the_rules_for(InsertionMode::InTableText, token);
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
        clear_the_stack_back_to_a_table_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of("td", "th", "tr")) {
        clear_the_stack_back_to_a_table_context();
        HTMLToken fake_tbody_token;
        fake_tbody_token.m_type = HTMLToken::Type::StartTag;
        fake_tbody_token.m_tag.tag_name.append("tbody");
        insert_html_element(fake_tbody_token);
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(InsertionMode::InTableBody, token);
        return;
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

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("table");

        reset_the_insertion_mode_appropriately();
        return;
    }
    TODO();
}

void HTMLDocumentParser::handle_in_select_in_table(HTMLToken& token)
{
    (void)token;
    TODO();
}

void HTMLDocumentParser::handle_in_select(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.codepoint() == 0) {
            PARSE_ERROR();
            return;
        }
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

    if (token.is_start_tag() && token.tag_name() == "option") {
        if (current_node().tag_name() == "option") {
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "optgroup") {
        if (current_node().tag_name() == "option") {
            m_stack_of_open_elements.pop();
        }
        if (current_node().tag_name() == "optgroup") {
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "optgroup") {
        if (current_node().tag_name() == "option" && node_before_current_node().tag_name() == "optgroup")
            m_stack_of_open_elements.pop();

        if (current_node().tag_name() == "optgroup") {
            m_stack_of_open_elements.pop();
        } else {
            PARSE_ERROR();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "option") {
        if (current_node().tag_name() == "option") {
            m_stack_of_open_elements.pop();
        } else {
            PARSE_ERROR();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "select") {
        if (m_stack_of_open_elements.has_in_select_scope("select")) {
            PARSE_ERROR();
            return;
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("select");
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == "select") {
        PARSE_ERROR();

        if (!m_stack_of_open_elements.has_in_select_scope("select"))
            return;

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("select");
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("input", "keygen", "textarea")) {
        PARSE_ERROR();

        if (!m_stack_of_open_elements.has_in_select_scope("select")) {
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped("select");
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of("script", "template")) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == "template") {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    PARSE_ERROR();
}

void HTMLDocumentParser::reset_the_insertion_mode_appropriately()
{
    for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
        RefPtr<Element> node = m_stack_of_open_elements.elements().at(i);

        if (node->tag_name() == "select") {
            TODO();
        }

        if (node->tag_name().is_one_of("td", "th")) {
            m_insertion_mode = InsertionMode::InCell;
            return;
        }

        if (node->tag_name() == "tr") {
            m_insertion_mode = InsertionMode::InRow;
            return;
        }

        if (node->tag_name().is_one_of("tbody", "thead", "tfoot")) {
            m_insertion_mode = InsertionMode::InTableBody;
            return;
        }

        if (node->tag_name() == "caption") {
            m_insertion_mode = InsertionMode::InCaption;
            return;
        }

        if (node->tag_name() == "colgroup") {
            m_insertion_mode = InsertionMode::InColumnGroup;
            return;
        }

        if (node->tag_name() == "table") {
            m_insertion_mode = InsertionMode::InTable;
            return;
        }

        if (node->tag_name() == "template") {
            TODO();
        }

        if (node->tag_name() == "body") {
            m_insertion_mode = InsertionMode::InBody;
            return;
        }

        if (node->tag_name() == "frameset") {
            m_insertion_mode = InsertionMode::InFrameset;
            if (m_parsing_fragment) {
                TODO();
            }
            return;
        }

        if (node->tag_name() == "html") {
            TODO();
        }
    }

    m_insertion_mode = InsertionMode::InBody;
    if (m_parsing_fragment) {
        TODO();
    }
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
