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
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
#include <LibWeb/HTML/Parser/HTMLToken.h>

namespace Web::HTML {

#define PARSE_ERROR()                                                         \
    do {                                                                      \
        dbg() << "Parse error! " << __PRETTY_FUNCTION__ << " @ " << __LINE__; \
    } while (0)

static Vector<FlyString> s_quirks_public_ids = {
    "+//Silmaril//dtd html Pro v0r11 19970101//",
    "-//AS//DTD HTML 3.0 asWedit + extensions//",
    "-//AdvaSoft Ltd//DTD HTML 3.0 asWedit + extensions//",
    "-//IETF//DTD HTML 2.0 Level 1//",
    "-//IETF//DTD HTML 2.0 Level 2//",
    "-//IETF//DTD HTML 2.0 Strict Level 1//",
    "-//IETF//DTD HTML 2.0 Strict Level 2//",
    "-//IETF//DTD HTML 2.0 Strict//",
    "-//IETF//DTD HTML 2.0//",
    "-//IETF//DTD HTML 2.1E//",
    "-//IETF//DTD HTML 3.0//",
    "-//IETF//DTD HTML 3.2 Final//",
    "-//IETF//DTD HTML 3.2//",
    "-//IETF//DTD HTML 3//",
    "-//IETF//DTD HTML Level 0//",
    "-//IETF//DTD HTML Level 1//",
    "-//IETF//DTD HTML Level 2//",
    "-//IETF//DTD HTML Level 3//",
    "-//IETF//DTD HTML Strict Level 0//",
    "-//IETF//DTD HTML Strict Level 1//",
    "-//IETF//DTD HTML Strict Level 2//",
    "-//IETF//DTD HTML Strict Level 3//",
    "-//IETF//DTD HTML Strict//",
    "-//IETF//DTD HTML//",
    "-//Metrius//DTD Metrius Presentational//",
    "-//Microsoft//DTD Internet Explorer 2.0 HTML Strict//",
    "-//Microsoft//DTD Internet Explorer 2.0 HTML//",
    "-//Microsoft//DTD Internet Explorer 2.0 Tables//",
    "-//Microsoft//DTD Internet Explorer 3.0 HTML Strict//",
    "-//Microsoft//DTD Internet Explorer 3.0 HTML//",
    "-//Microsoft//DTD Internet Explorer 3.0 Tables//",
    "-//Netscape Comm. Corp.//DTD HTML//",
    "-//Netscape Comm. Corp.//DTD Strict HTML//",
    "-//O'Reilly and Associates//DTD HTML 2.0//",
    "-//O'Reilly and Associates//DTD HTML Extended 1.0//",
    "-//O'Reilly and Associates//DTD HTML Extended Relaxed 1.0//",
    "-//SQ//DTD HTML 2.0 HoTMetaL + extensions//",
    "-//SoftQuad Software//DTD HoTMetaL PRO 6.0::19990601::extensions to HTML 4.0//",
    "-//SoftQuad//DTD HoTMetaL PRO 4.0::19971010::extensions to HTML 4.0//",
    "-//Spyglass//DTD HTML 2.0 Extended//",
    "-//Sun Microsystems Corp.//DTD HotJava HTML//",
    "-//Sun Microsystems Corp.//DTD HotJava Strict HTML//",
    "-//W3C//DTD HTML 3 1995-03-24//",
    "-//W3C//DTD HTML 3.2 Draft//",
    "-//W3C//DTD HTML 3.2 Final//",
    "-//W3C//DTD HTML 3.2//",
    "-//W3C//DTD HTML 3.2S Draft//",
    "-//W3C//DTD HTML 4.0 Frameset//",
    "-//W3C//DTD HTML 4.0 Transitional//",
    "-//W3C//DTD HTML Experimental 19960712//",
    "-//W3C//DTD HTML Experimental 970421//",
    "-//W3C//DTD W3 HTML//",
    "-//W3O//DTD W3 HTML 3.0//",
    "-//WebTechs//DTD Mozilla HTML 2.0//",
    "-//WebTechs//DTD Mozilla HTML//"
};

RefPtr<DOM::Document> parse_html_document(const StringView& data, const URL& url, const String& encoding)
{
    HTMLDocumentParser parser(data, encoding);
    parser.run(url);
    return parser.document();
}

HTMLDocumentParser::HTMLDocumentParser(const StringView& input, const String& encoding)
    : m_tokenizer(input, encoding)
{
    m_document = adopt(*new DOM::Document);
}

HTMLDocumentParser::HTMLDocumentParser(const StringView& input, const String& encoding, DOM::Document& existing_document)
    : m_tokenizer(input, encoding)
    , m_document(existing_document)
{
}

HTMLDocumentParser::~HTMLDocumentParser()
{
}

void HTMLDocumentParser::run(const URL& url)
{
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
#ifdef PARSER_DEBUG
            dbg() << "Stop parsing" << (m_parsing_fragment ? " fragment" : "") << "! :^)";
#endif
            break;
        }
    }

    flush_character_insertions();

    // "The end"

    m_document->set_ready_state("interactive");

    auto scripts_to_execute_when_parsing_has_finished = m_document->take_scripts_to_execute_when_parsing_has_finished({});
    for (auto& script : scripts_to_execute_when_parsing_has_finished) {
        script.execute_script();
    }

    m_document->dispatch_event(DOM::Event::create("DOMContentLoaded"));

    auto scripts_to_execute_as_soon_as_possible = m_document->take_scripts_to_execute_as_soon_as_possible({});
    for (auto& script : scripts_to_execute_as_soon_as_possible) {
        script.execute_script();
    }

    m_document->set_ready_state("complete");
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
    case InsertionMode::InCaption:
        handle_in_caption(token);
        break;
    case InsertionMode::InColumnGroup:
        handle_in_column_group(token);
        break;
    case InsertionMode::InTemplate:
        handle_in_template(token);
        break;
    case InsertionMode::InFrameset:
        handle_in_frameset(token);
        break;
    case InsertionMode::AfterFrameset:
        handle_after_frameset(token);
        break;
    case InsertionMode::AfterAfterFrameset:
        handle_after_after_frameset(token);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

DOM::QuirksMode HTMLDocumentParser::which_quirks_mode(const HTMLToken& doctype_token) const
{
    if (doctype_token.m_doctype.force_quirks)
        return DOM::QuirksMode::Yes;

    // NOTE: The tokenizer puts the name into lower case for us.
    if (doctype_token.m_doctype.name.to_string() != "html")
        return DOM::QuirksMode::Yes;

    auto public_identifier = doctype_token.m_doctype.public_identifier.to_string();
    auto system_identifier = doctype_token.m_doctype.system_identifier.to_string();

    if (public_identifier.equals_ignoring_case("-//W3O//DTD W3 HTML Strict 3.0//EN//"))
        return DOM::QuirksMode::Yes;

    if (public_identifier.equals_ignoring_case("-/W3C/DTD HTML 4.0 Transitional/EN"))
        return DOM::QuirksMode::Yes;

    if (public_identifier.equals_ignoring_case("HTML"))
        return DOM::QuirksMode::Yes;

    if (system_identifier.equals_ignoring_case("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd"))
        return DOM::QuirksMode::Yes;

    for (auto& public_id : s_quirks_public_ids) {
        if (public_identifier.starts_with(public_id, CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Yes;
    }

    if (doctype_token.m_doctype.missing_system_identifier) {
        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Frameset//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Yes;

        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Transitional//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Yes;
    }

    if (public_identifier.starts_with("-//W3C//DTD XHTML 1.0 Frameset//", CaseSensitivity::CaseInsensitive))
        return DOM::QuirksMode::Limited;

    if (public_identifier.starts_with("-//W3C//DTD XHTML 1.0 Transitional//", CaseSensitivity::CaseInsensitive))
        return DOM::QuirksMode::Limited;

    if (!doctype_token.m_doctype.missing_system_identifier) {
        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Frameset//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Limited;

        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Transitional//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Limited;
    }

    return DOM::QuirksMode::No;
}

void HTMLDocumentParser::handle_initial(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_comment()) {
        auto comment = adopt(*new DOM::Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype()) {
        auto doctype = adopt(*new DOM::DocumentType(document()));
        doctype->set_name(token.m_doctype.name.to_string());
        doctype->set_public_id(token.m_doctype.public_identifier.to_string());
        doctype->set_system_id(token.m_doctype.system_identifier.to_string());
        document().append_child(move(doctype));
        document().set_quirks_mode(which_quirks_mode(token));
        m_insertion_mode = InsertionMode::BeforeHTML;
        return;
    }

    PARSE_ERROR();
    document().set_quirks_mode(DOM::QuirksMode::Yes);
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
        auto comment = adopt(*new DOM::Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        auto element = create_element_for(token);
        document().append_child(element);
        m_stack_of_open_elements.push(move(element));
        m_insertion_mode = InsertionMode::BeforeHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::head, HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    auto element = create_element(document(), HTML::TagNames::html);
    document().append_child(element);
    m_stack_of_open_elements.push(element);
    // FIXME: If the Document is being loaded as part of navigation of a browsing context, then: run the application cache selection algorithm with no manifest, passing it the Document object.
    m_insertion_mode = InsertionMode::BeforeHead;
    process_using_the_rules_for(InsertionMode::BeforeHead, token);
    return;
}

DOM::Element& HTMLDocumentParser::current_node()
{
    return m_stack_of_open_elements.current_node();
}

DOM::Element& HTMLDocumentParser::node_before_current_node()
{
    return m_stack_of_open_elements.elements().at(m_stack_of_open_elements.elements().size() - 2);
}

HTMLDocumentParser::AdjustedInsertionLocation HTMLDocumentParser::find_appropriate_place_for_inserting_node()
{
    auto& target = current_node();
    HTMLDocumentParser::AdjustedInsertionLocation adjusted_insertion_location;

    if (m_foster_parenting && target.local_name().is_one_of(HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr)) {
        auto last_template = m_stack_of_open_elements.last_element_with_tag_name(HTML::TagNames::template_);
        auto last_table = m_stack_of_open_elements.last_element_with_tag_name(HTML::TagNames::table);
        if (last_template.element && (!last_table.element || last_template.index > last_table.index)) {
            // This returns the template content, so no need to check the parent is a template.
            return { downcast<HTMLTemplateElement>(last_template.element)->content(), nullptr };
        }
        if (!last_table.element) {
            ASSERT(m_parsing_fragment);
            // Guaranteed not to be a template element (it will be the html element),
            // so no need to check the parent is a template.
            return { m_stack_of_open_elements.elements().first(), nullptr };
        }
        if (last_table.element->parent_node())
            adjusted_insertion_location = { last_table.element->parent_node(), last_table.element };
        else
            adjusted_insertion_location = { m_stack_of_open_elements.element_before(*last_table.element), nullptr };
    } else {
        adjusted_insertion_location = { target, nullptr };
    }

    if (is<HTMLTemplateElement>(*adjusted_insertion_location.parent))
        return { downcast<HTMLTemplateElement>(*adjusted_insertion_location.parent).content(), nullptr };

    return adjusted_insertion_location;
}

NonnullRefPtr<DOM::Element> HTMLDocumentParser::create_element_for(const HTMLToken& token)
{
    auto element = create_element(document(), token.tag_name());
    for (auto& attribute : token.m_tag.attributes) {
        element->set_attribute(attribute.local_name_builder.to_string(), attribute.value_builder.to_string());
    }
    return element;
}

RefPtr<DOM::Element> HTMLDocumentParser::insert_html_element(const HTMLToken& token)
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    auto element = create_element_for(token);
    // FIXME: Check if it's possible to insert `element` at `adjusted_insertion_location`
    adjusted_insertion_location.parent->insert_before(element, adjusted_insertion_location.insert_before_sibling);
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::head) {
        auto element = insert_html_element(token);
        m_head_element = downcast<HTMLHeadElement>(*element);
        m_insertion_mode = InsertionMode::InHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::head, HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    m_head_element = downcast<HTMLHeadElement>(*insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::head)));
    m_insertion_mode = InsertionMode::InHead;
    process_using_the_rules_for(InsertionMode::InHead, token);
    return;
}

void HTMLDocumentParser::insert_comment(HTMLToken& token)
{
    auto data = token.m_comment_or_character.data.to_string();
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    adjusted_insertion_location.parent->insert_before(adopt(*new DOM::Comment(document(), data)), adjusted_insertion_location.insert_before_sibling);
}

void HTMLDocumentParser::handle_in_head(HTMLToken& token)
{
    if (token.is_parser_whitespace()) {
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link)) {
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::meta) {
        auto element = insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::title) {
        insert_html_element(token);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);
        m_original_insertion_mode = m_insertion_mode;
        m_insertion_mode = InsertionMode::Text;
        return;
    }

    if (token.is_start_tag() && ((token.tag_name() == HTML::TagNames::noscript && m_scripting_enabled) || token.tag_name() == HTML::TagNames::noframes || token.tag_name() == HTML::TagNames::style)) {
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::noscript && !m_scripting_enabled) {
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InHeadNoscript;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::script) {
        auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
        auto element = create_element_for(token);
        auto& script_element = downcast<HTMLScriptElement>(*element);
        script_element.set_parser_document({}, document());
        script_element.set_non_blocking({}, false);

        if (m_parsing_fragment) {
            TODO();
        }

        if (m_invoked_via_document_write) {
            TODO();
        }

        adjusted_insertion_location.parent->insert_before(element, adjusted_insertion_location.insert_before_sibling, false);
        m_stack_of_open_elements.push(element);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::ScriptData);
        m_original_insertion_mode = m_insertion_mode;
        m_insertion_mode = InsertionMode::Text;
        return;
    }
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::head) {
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::AfterHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::template_) {
        insert_html_element(token);
        m_list_of_active_formatting_elements.add_marker();
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InTemplate;
        m_stack_of_template_insertion_modes.append(InsertionMode::InTemplate);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            PARSE_ERROR();
            return;
        }

        generate_all_implied_end_tags_thoroughly();

        if (current_node().local_name() != HTML::TagNames::template_)
            PARSE_ERROR();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::template_);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        m_stack_of_template_insertion_modes.take_last();
        reset_the_insertion_mode_appropriately();
        return;
    }

    if ((token.is_start_tag() && token.tag_name() == HTML::TagNames::head) || token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::AfterHead;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::handle_in_head_noscript(HTMLToken& token)
{
    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::noscript) {
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InHead;
        return;
    }

    if (token.is_parser_whitespace() || token.is_comment() || (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::noframes, HTML::TagNames::style))) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::br) {
        goto AnythingElse;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::head, HTML::TagNames::noscript)) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    PARSE_ERROR();
    m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::InHead;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::parse_generic_raw_text_element(HTMLToken& token)
{
    insert_html_element(token);
    m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    m_original_insertion_mode = m_insertion_mode;
    m_insertion_mode = InsertionMode::Text;
}

DOM::Text* HTMLDocumentParser::find_character_insertion_node()
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    if (adjusted_insertion_location.insert_before_sibling) {
        TODO();
    }
    if (adjusted_insertion_location.parent->is_document())
        return nullptr;
    if (adjusted_insertion_location.parent->last_child() && adjusted_insertion_location.parent->last_child()->is_text())
        return downcast<DOM::Text>(adjusted_insertion_location.parent->last_child());
    auto new_text_node = adopt(*new DOM::Text(document(), ""));
    adjusted_insertion_location.parent->append_child(new_text_node);
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
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::body) {
        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InFrameset;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::noframes, HTML::TagNames::script, HTML::TagNames::style, HTML::TagNames::template_, HTML::TagNames::title)) {
        PARSE_ERROR();
        m_stack_of_open_elements.push(*m_head_element);
        process_using_the_rules_for(InsertionMode::InHead, token);
        m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) {
            return entry.ptr() == m_head_element;
        });
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if ((token.is_start_tag() && token.tag_name() == HTML::TagNames::head) || token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

AnythingElse:
    insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::body));
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::generate_implied_end_tags(const FlyString& exception)
{
    while (current_node().local_name() != exception && current_node().local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::generate_all_implied_end_tags_thoroughly()
{
    while (current_node().local_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::colgroup, HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))
        m_stack_of_open_elements.pop();
}

void HTMLDocumentParser::close_a_p_element()
{
    generate_implied_end_tags(HTML::TagNames::p);
    if (current_node().local_name() != HTML::TagNames::p) {
        PARSE_ERROR();
    }
    m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::p);
}

void HTMLDocumentParser::handle_after_body(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_comment()) {
        auto data = token.m_comment_or_character.data.to_string();
        auto& insertion_location = m_stack_of_open_elements.first();
        insertion_location.append_child(adopt(*new DOM::Comment(document(), data)));
        return;
    }

    if (token.is_doctype()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::html) {
        if (m_parsing_fragment) {
            PARSE_ERROR();
            return;
        }
        m_insertion_mode = InsertionMode::AfterAfterBody;
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    PARSE_ERROR();
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLDocumentParser::handle_after_after_body(HTMLToken& token)
{
    if (token.is_comment()) {
        auto comment = adopt(*new DOM::Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype() || token.is_parser_whitespace() || (token.is_start_tag() && token.tag_name() == HTML::TagNames::html)) {
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
    RefPtr<DOM::Element> entry = m_list_of_active_formatting_elements.entries().at(index).element;
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
    auto new_element = insert_html_element(HTMLToken::make_start_tag(entry->local_name()));

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
    if (current_node().local_name() == subject && !m_list_of_active_formatting_elements.contains(current_node())) {
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

    RefPtr<DOM::Element> furthest_block = m_stack_of_open_elements.topmost_special_node_below(*formatting_element);

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
        HTML::TagNames::address,
        HTML::TagNames::applet,
        HTML::TagNames::area,
        HTML::TagNames::article,
        HTML::TagNames::aside,
        HTML::TagNames::base,
        HTML::TagNames::basefont,
        HTML::TagNames::bgsound,
        HTML::TagNames::blockquote,
        HTML::TagNames::body,
        HTML::TagNames::br,
        HTML::TagNames::button,
        HTML::TagNames::caption,
        HTML::TagNames::center,
        HTML::TagNames::col,
        HTML::TagNames::colgroup,
        HTML::TagNames::dd,
        HTML::TagNames::details,
        HTML::TagNames::dir,
        HTML::TagNames::div,
        HTML::TagNames::dl,
        HTML::TagNames::dt,
        HTML::TagNames::embed,
        HTML::TagNames::fieldset,
        HTML::TagNames::figcaption,
        HTML::TagNames::figure,
        HTML::TagNames::footer,
        HTML::TagNames::form,
        HTML::TagNames::frame,
        HTML::TagNames::frameset,
        HTML::TagNames::h1,
        HTML::TagNames::h2,
        HTML::TagNames::h3,
        HTML::TagNames::h4,
        HTML::TagNames::h5,
        HTML::TagNames::h6,
        HTML::TagNames::head,
        HTML::TagNames::header,
        HTML::TagNames::hgroup,
        HTML::TagNames::hr,
        HTML::TagNames::html,
        HTML::TagNames::iframe,
        HTML::TagNames::img,
        HTML::TagNames::input,
        HTML::TagNames::keygen,
        HTML::TagNames::li,
        HTML::TagNames::link,
        HTML::TagNames::listing,
        HTML::TagNames::main,
        HTML::TagNames::marquee,
        HTML::TagNames::menu,
        HTML::TagNames::meta,
        HTML::TagNames::nav,
        HTML::TagNames::noembed,
        HTML::TagNames::noframes,
        HTML::TagNames::noscript,
        HTML::TagNames::object,
        HTML::TagNames::ol,
        HTML::TagNames::p,
        HTML::TagNames::param,
        HTML::TagNames::plaintext,
        HTML::TagNames::pre,
        HTML::TagNames::script,
        HTML::TagNames::section,
        HTML::TagNames::select,
        HTML::TagNames::source,
        HTML::TagNames::style,
        HTML::TagNames::summary,
        HTML::TagNames::table,
        HTML::TagNames::tbody,
        HTML::TagNames::td,
        HTML::TagNames::template_,
        HTML::TagNames::textarea,
        HTML::TagNames::tfoot,
        HTML::TagNames::th,
        HTML::TagNames::thead,
        HTML::TagNames::title,
        HTML::TagNames::tr,
        HTML::TagNames::track,
        HTML::TagNames::ul,
        HTML::TagNames::wbr,
        HTML::TagNames::xmp);
}

void HTMLDocumentParser::handle_in_body(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            PARSE_ERROR();
            return;
        }
        if (token.is_parser_whitespace()) {
            reconstruct_the_active_formatting_elements();
            insert_character(token.code_point());
            return;
        }
        reconstruct_the_active_formatting_elements();
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        PARSE_ERROR();
        if (m_stack_of_open_elements.contains(HTML::TagNames::template_))
            return;
        for (auto& attribute : token.m_tag.attributes) {
            if (current_node().has_attribute(attribute.local_name_builder.string_view()))
                continue;
            current_node().set_attribute(attribute.local_name_builder.to_string(), attribute.value_builder.to_string());
        }
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::noframes, HTML::TagNames::script, HTML::TagNames::style, HTML::TagNames::template_, HTML::TagNames::title)) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::body) {
        PARSE_ERROR();
        if (m_stack_of_open_elements.elements().size() == 1
            || m_stack_of_open_elements.elements().at(1).local_name() != HTML::TagNames::body
            || m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            ASSERT(m_parsing_fragment);
            return;
        }
        m_frameset_ok = false;
        auto& body_element = m_stack_of_open_elements.elements().at(1);
        for (auto& attribute : token.m_tag.attributes) {
            if (body_element.has_attribute(attribute.local_name_builder.string_view()))
                continue;
            body_element.set_attribute(attribute.local_name_builder.to_string(), attribute.value_builder.to_string());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        PARSE_ERROR();

        if (m_stack_of_open_elements.elements().size() == 1
            || m_stack_of_open_elements.elements().at(1).local_name() != HTML::TagNames::body) {
            ASSERT(m_parsing_fragment);
            return;
        }

        if (!m_frameset_ok)
            return;

        TODO();
    }

    if (token.is_end_of_file()) {
        if (!m_stack_of_template_insertion_modes.is_empty()) {
            process_using_the_rules_for(InsertionMode::InTemplate, token);
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::body, HTML::TagNames::html)) {
                PARSE_ERROR();
                break;
            }
        }

        stop_parsing();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::body) {
        if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::body)) {
            PARSE_ERROR();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::body, HTML::TagNames::html)) {
                PARSE_ERROR();
                break;
            }
        }

        m_insertion_mode = InsertionMode::AfterBody;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::html) {
        if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::body)) {
            PARSE_ERROR();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::body, HTML::TagNames::html)) {
                PARSE_ERROR();
                break;
            }
        }

        m_insertion_mode = InsertionMode::AfterBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::address, HTML::TagNames::article, HTML::TagNames::aside, HTML::TagNames::blockquote, HTML::TagNames::center, HTML::TagNames::details, HTML::TagNames::dialog, HTML::TagNames::dir, HTML::TagNames::div, HTML::TagNames::dl, HTML::TagNames::fieldset, HTML::TagNames::figcaption, HTML::TagNames::figure, HTML::TagNames::footer, HTML::TagNames::header, HTML::TagNames::hgroup, HTML::TagNames::main, HTML::TagNames::menu, HTML::TagNames::nav, HTML::TagNames::ol, HTML::TagNames::p, HTML::TagNames::section, HTML::TagNames::summary, HTML::TagNames::ul)) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6)) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        if (current_node().local_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6)) {
            PARSE_ERROR();
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::pre, HTML::TagNames::listing)) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();

        insert_html_element(token);

        m_frameset_ok = false;

        // If the next token is a U+000A LINE FEED (LF) character token,
        // then ignore that token and move on to the next one.
        // (Newlines at the start of pre blocks are ignored as an authoring convenience.)
        auto next_token = m_tokenizer.next_token();
        if (next_token.has_value() && next_token.value().is_character() && next_token.value().code_point() == '\n') {
            // Ignore it.
        } else {
            process_using_the_rules_for(m_insertion_mode, next_token.value());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::form) {
        if (m_form_element && !m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            PARSE_ERROR();
            return;
        }
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        auto element = insert_html_element(token);
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_))
            m_form_element = downcast<HTMLFormElement>(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::li) {
        m_frameset_ok = false;

        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<DOM::Element> node = m_stack_of_open_elements.elements()[i];

            if (node->local_name() == HTML::TagNames::li) {
                generate_implied_end_tags(HTML::TagNames::li);
                if (current_node().local_name() != HTML::TagNames::li) {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::li);
                break;
            }

            if (is_special_tag(node->local_name()) && !node->local_name().is_one_of(HTML::TagNames::address, HTML::TagNames::div, HTML::TagNames::p))
                break;
        }

        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();

        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt)) {
        m_frameset_ok = false;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<DOM::Element> node = m_stack_of_open_elements.elements()[i];
            if (node->local_name() == HTML::TagNames::dd) {
                generate_implied_end_tags(HTML::TagNames::dd);
                if (current_node().local_name() != HTML::TagNames::dd) {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::dd);
                break;
            }
            if (node->local_name() == HTML::TagNames::dt) {
                generate_implied_end_tags(HTML::TagNames::dt);
                if (current_node().local_name() != HTML::TagNames::dt) {
                    PARSE_ERROR();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::dt);
                break;
            }
            if (is_special_tag(node->local_name()) && !node->local_name().is_one_of(HTML::TagNames::address, HTML::TagNames::div, HTML::TagNames::p))
                break;
        }
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::plaintext) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        insert_html_element(token);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::PLAINTEXT);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::button) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::button)) {
            PARSE_ERROR();
            generate_implied_end_tags();
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::button);
        }
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::address, HTML::TagNames::article, HTML::TagNames::aside, HTML::TagNames::blockquote, HTML::TagNames::button, HTML::TagNames::center, HTML::TagNames::details, HTML::TagNames::dialog, HTML::TagNames::dir, HTML::TagNames::div, HTML::TagNames::dl, HTML::TagNames::fieldset, HTML::TagNames::figcaption, HTML::TagNames::figure, HTML::TagNames::footer, HTML::TagNames::header, HTML::TagNames::hgroup, HTML::TagNames::listing, HTML::TagNames::main, HTML::TagNames::menu, HTML::TagNames::nav, HTML::TagNames::ol, HTML::TagNames::pre, HTML::TagNames::section, HTML::TagNames::summary, HTML::TagNames::ul)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::form) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            auto node = m_form_element;
            m_form_element = nullptr;
            if (!node || !m_stack_of_open_elements.has_in_scope(*node)) {
                PARSE_ERROR();
                return;
            }
            generate_implied_end_tags();
            if (&current_node() != node) {
                PARSE_ERROR();
            }
            m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) { return entry.ptr() == node.ptr(); });
        } else {
            if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::form)) {
                PARSE_ERROR();
                return;
            }
            generate_implied_end_tags();
            if (current_node().local_name() != HTML::TagNames::form) {
                PARSE_ERROR();
            }
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::form);
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::p) {
        if (!m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p)) {
            PARSE_ERROR();
            insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::p));
        }
        close_a_p_element();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::li) {
        if (!m_stack_of_open_elements.has_in_list_item_scope(HTML::TagNames::li)) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags(HTML::TagNames::li);
        if (current_node().local_name() != HTML::TagNames::li) {
            PARSE_ERROR();
            dbg() << "Expected <li> current node, but had <" << current_node().local_name() << ">";
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::li);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags(token.tag_name());
        if (current_node().local_name() != token.tag_name()) {
            PARSE_ERROR();
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6)) {
        if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::h1)
            && !m_stack_of_open_elements.has_in_scope(HTML::TagNames::h2)
            && !m_stack_of_open_elements.has_in_scope(HTML::TagNames::h3)
            && !m_stack_of_open_elements.has_in_scope(HTML::TagNames::h4)
            && !m_stack_of_open_elements.has_in_scope(HTML::TagNames::h5)
            && !m_stack_of_open_elements.has_in_scope(HTML::TagNames::h6)) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();
        if (current_node().local_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        for (;;) {
            auto popped_element = m_stack_of_open_elements.pop();
            if (popped_element->local_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6))
                break;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::a) {
        if (auto* element = m_list_of_active_formatting_elements.last_element_with_tag_name_before_marker(HTML::TagNames::a)) {
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

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::b, HTML::TagNames::big, HTML::TagNames::code, HTML::TagNames::em, HTML::TagNames::font, HTML::TagNames::i, HTML::TagNames::s, HTML::TagNames::small, HTML::TagNames::strike, HTML::TagNames::strong, HTML::TagNames::tt, HTML::TagNames::u)) {
        reconstruct_the_active_formatting_elements();
        auto element = insert_html_element(token);
        m_list_of_active_formatting_elements.add(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::nobr) {
        reconstruct_the_active_formatting_elements();
        if (m_stack_of_open_elements.has_in_scope(HTML::TagNames::nobr)) {
            PARSE_ERROR();
            run_the_adoption_agency_algorithm(token);
            reconstruct_the_active_formatting_elements();
        }
        auto element = insert_html_element(token);
        m_list_of_active_formatting_elements.add(*element);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::a, HTML::TagNames::b, HTML::TagNames::big, HTML::TagNames::code, HTML::TagNames::em, HTML::TagNames::font, HTML::TagNames::i, HTML::TagNames::nobr, HTML::TagNames::s, HTML::TagNames::small, HTML::TagNames::strike, HTML::TagNames::strong, HTML::TagNames::tt, HTML::TagNames::u)) {
        if (run_the_adoption_agency_algorithm(token) == AdoptionAgencyAlgorithmOutcome::RunAnyOtherEndTagSteps)
            goto AnyOtherEndTag;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::applet, HTML::TagNames::marquee, HTML::TagNames::object)) {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_list_of_active_formatting_elements.add_marker();
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::applet, HTML::TagNames::marquee, HTML::TagNames::object)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();
        if (current_node().local_name() != token.tag_name()) {
            PARSE_ERROR();
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::table) {
        if (!document().in_quirks_mode()) {
            if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
                close_a_p_element();
        }
        insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::br) {
        token.drop_attributes();
        goto BRStartTag;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::area, HTML::TagNames::br, HTML::TagNames::embed, HTML::TagNames::img, HTML::TagNames::keygen, HTML::TagNames::wbr)) {
    BRStartTag:
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::input) {
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        auto type_attribute = token.attribute(HTML::AttributeNames::type);
        if (type_attribute.is_null() || !type_attribute.equals_ignoring_case("hidden")) {
            m_frameset_ok = false;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::param, HTML::TagNames::source, HTML::TagNames::track)) {
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::hr) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::image) {
        // Parse error. Change the token's tag name to HTML::TagNames::img and reprocess it. (Don't ask.)
        PARSE_ERROR();
        token.m_tag.tag_name.clear();
        token.m_tag.tag_name.append(HTML::TagNames::img);
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::textarea) {
        insert_html_element(token);

        m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);

        // If the next token is a U+000A LINE FEED (LF) character token,
        // then ignore that token and move on to the next one.
        // (Newlines at the start of pre blocks are ignored as an authoring convenience.)
        auto next_token = m_tokenizer.next_token();

        m_original_insertion_mode = m_insertion_mode;
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::Text;

        if (next_token.has_value() && next_token.value().is_character() && next_token.value().code_point() == '\n') {
            // Ignore it.
        } else {
            process_using_the_rules_for(m_insertion_mode, next_token.value());
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::xmp) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p)) {
            close_a_p_element();
        }
        reconstruct_the_active_formatting_elements();
        m_frameset_ok = false;
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::iframe) {
        m_frameset_ok = false;
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && ((token.tag_name() == HTML::TagNames::noembed) || (token.tag_name() == HTML::TagNames::noscript && m_scripting_enabled))) {
        parse_generic_raw_text_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::select) {
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

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::optgroup, HTML::TagNames::option)) {
        if (current_node().local_name() == HTML::TagNames::option)
            m_stack_of_open_elements.pop();
        reconstruct_the_active_formatting_elements();
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::rb, HTML::TagNames::rtc)) {
        if (m_stack_of_open_elements.has_in_scope(HTML::TagNames::ruby))
            generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::ruby)
            PARSE_ERROR();

        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::rp, HTML::TagNames::rt)) {
        if (m_stack_of_open_elements.has_in_scope(HTML::TagNames::ruby))
            generate_implied_end_tags(HTML::TagNames::rtc);

        if (current_node().local_name() != HTML::TagNames::rtc || current_node().local_name() != HTML::TagNames::ruby)
            PARSE_ERROR();

        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::math) {
        dbg() << "<math> element encountered.";
        reconstruct_the_active_formatting_elements();
        adjust_mathml_attributes(token);
        adjust_foreign_attributes(token);

        // FIXME: this should insert a foreign element, but lets just insert it normally for now :^)
        insert_html_element(token);

        if (token.is_self_closing()) {
            m_stack_of_open_elements.pop();
            token.acknowledge_self_closing_flag_if_set();
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::svg) {
        dbg() << "<svg> element encountered.";
        reconstruct_the_active_formatting_elements();
        adjust_svg_attributes(token);
        adjust_foreign_attributes(token);

        // FIXME: this should insert a foreign element, but lets just insert it normally for now :^)
        insert_html_element(token);

        if (token.is_self_closing()) {
            m_stack_of_open_elements.pop();
            token.acknowledge_self_closing_flag_if_set();
        }
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::frame, HTML::TagNames::head, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))) {
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
        RefPtr<DOM::Element> node;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            node = m_stack_of_open_elements.elements()[i];
            if (node->local_name() == token.tag_name()) {
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
            if (is_special_tag(node->local_name())) {
                PARSE_ERROR();
                return;
            }
        }
        return;
    }
}

void HTMLDocumentParser::adjust_mathml_attributes(HTMLToken& token)
{
    token.adjust_attribute_name("definitionurl", "definitionURL");
}

void HTMLDocumentParser::adjust_svg_attributes(HTMLToken& token)
{
    token.adjust_attribute_name("attributename", "attributeName");
    token.adjust_attribute_name("attributetype", "attributeType");
    token.adjust_attribute_name("basefrequency", "baseFrequency");
    token.adjust_attribute_name("baseprofile", "baseProfile");
    token.adjust_attribute_name("calcmode", "calcMode");
    token.adjust_attribute_name("clippathunits", "clipPathUnits");
    token.adjust_attribute_name("diffuseconstant", "diffuseConstant");
    token.adjust_attribute_name("edgemode", "edgeMode");
    token.adjust_attribute_name("filterunits", "filterUnits");
    token.adjust_attribute_name("glyphref", "glyphRef");
    token.adjust_attribute_name("gradienttransform", "gradientTransform");
    token.adjust_attribute_name("gradientunits", "gradientUnits");
    token.adjust_attribute_name("kernelmatrix", "kernelMatrix");
    token.adjust_attribute_name("kernelunitlength", "kernelUnitLength");
    token.adjust_attribute_name("keypoints", "keyPoints");
    token.adjust_attribute_name("keysplines", "keySplines");
    token.adjust_attribute_name("keytimes", "keyTimes");
    token.adjust_attribute_name("lengthadjust", "lengthAdjust");
    token.adjust_attribute_name("limitingconeangle", "limitingConeAngle");
    token.adjust_attribute_name("markerheight", "markerHeight");
    token.adjust_attribute_name("markerunits", "markerUnits");
    token.adjust_attribute_name("markerwidth", "markerWidth");
    token.adjust_attribute_name("maskcontentunits", "maskContentUnits");
    token.adjust_attribute_name("maskunits", "maskUnits");
    token.adjust_attribute_name("numoctaves", "numOctaves");
    token.adjust_attribute_name("pathlength", "pathLength");
    token.adjust_attribute_name("patterncontentunits", "patternContentUnits");
    token.adjust_attribute_name("patterntransform", "patternTransform");
    token.adjust_attribute_name("patternunits", "patternUnits");
    token.adjust_attribute_name("pointsatx", "pointsAtX");
    token.adjust_attribute_name("pointsaty", "pointsAtY");
    token.adjust_attribute_name("pointsatz", "pointsAtZ");
    token.adjust_attribute_name("preservealpha", "preserveAlpha");
    token.adjust_attribute_name("preserveaspectratio", "preserveAspectRatio");
    token.adjust_attribute_name("primitiveunits", "primitiveUnits");
    token.adjust_attribute_name("refx", "refX");
    token.adjust_attribute_name("refy", "refY");
    token.adjust_attribute_name("repeatcount", "repeatCount");
    token.adjust_attribute_name("repeatdur", "repeatDur");
    token.adjust_attribute_name("requiredextensions", "requiredExtensions");
    token.adjust_attribute_name("requiredfeatures", "requiredFeatures");
    token.adjust_attribute_name("specularconstant", "specularConstant");
    token.adjust_attribute_name("specularexponent", "specularExponent");
    token.adjust_attribute_name("spreadmethod", "spreadMethod");
    token.adjust_attribute_name("startoffset", "startOffset");
    token.adjust_attribute_name("stddeviation", "stdDeviation");
    token.adjust_attribute_name("stitchtiles", "stitchTiles");
    token.adjust_attribute_name("surfacescale", "surfaceScale");
    token.adjust_attribute_name("systemlanguage", "systemLanguage");
    token.adjust_attribute_name("tablevalues", "tableValues");
    token.adjust_attribute_name("targetx", "targetX");
    token.adjust_attribute_name("targety", "targetY");
    token.adjust_attribute_name("textlength", "textLength");
    token.adjust_attribute_name("viewbox", "viewBox");
    token.adjust_attribute_name("viewtarget", "viewTarget");
    token.adjust_attribute_name("xchannelselector", "xChannelSelector");
    token.adjust_attribute_name("ychannelselector", "yChannelSelector");
    token.adjust_attribute_name("zoomandpan", "zoomAndPan");
}
void HTMLDocumentParser::adjust_foreign_attributes(HTMLToken& token)
{
    auto xlink_namespace = "http://www.w3.org/1999/xlink";
    token.adjust_foreign_attribute("xlink:actuate", "xlink", "actuate", xlink_namespace);
    token.adjust_foreign_attribute("xlink:arcrole", "xlink", "arcrole", xlink_namespace);
    token.adjust_foreign_attribute("xlink:href", "xlink", "href", xlink_namespace);
    token.adjust_foreign_attribute("xlink:role", "xlink", "role", xlink_namespace);
    token.adjust_foreign_attribute("xlink:show", "xlink", "show", xlink_namespace);
    token.adjust_foreign_attribute("xlink:title", "xlink", "title", xlink_namespace);
    token.adjust_foreign_attribute("xlink:type", "xlink", "type", xlink_namespace);

    auto xml_namespace = "http://www.w3.org/XML/1998/namespace";
    token.adjust_foreign_attribute("xml:lang", "xml", "lang", xml_namespace);
    token.adjust_foreign_attribute("xml:space", "xml", "space", xml_namespace);

    auto xmlns_namespace = "http://www.w3.org/2000/xmlns/";
    token.adjust_foreign_attribute("xmlns", "", "xmlns", xmlns_namespace);
    token.adjust_foreign_attribute("xmlns:xlink", "xmlns", "xlink", xmlns_namespace);
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
        insert_character(token.code_point());
        return;
    }
    if (token.is_end_of_file()) {
        PARSE_ERROR();
        if (current_node().local_name() == HTML::TagNames::script)
            downcast<HTMLScriptElement>(current_node()).set_already_started({}, true);
        m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::script) {
        // Make sure the <script> element has up-to-date text content before preparing the script.
        flush_character_insertions();

        NonnullRefPtr<HTMLScriptElement> script = downcast<HTMLScriptElement>(current_node());
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

                if (the_script->failed_to_load())
                    return;

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
    while (!current_node().local_name().is_one_of(HTML::TagNames::table, HTML::TagNames::template_, HTML::TagNames::html))
        m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        ASSERT(m_parsing_fragment);
}

void HTMLDocumentParser::clear_the_stack_back_to_a_table_row_context()
{
    while (!current_node().local_name().is_one_of(HTML::TagNames::tr, HTML::TagNames::template_, HTML::TagNames::html))
        m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        ASSERT(m_parsing_fragment);
}

void HTMLDocumentParser::clear_the_stack_back_to_a_table_body_context()
{
    while (!current_node().local_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::template_, HTML::TagNames::html))
        m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        ASSERT(m_parsing_fragment);
}

void HTMLDocumentParser::handle_in_row(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::th, HTML::TagNames::td)) {
        clear_the_stack_back_to_a_table_row_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InCell;
        m_list_of_active_formatting_elements.add_marker();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::tr) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::td, HTML::TagNames::th)) {
        PARSE_ERROR();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLDocumentParser::close_the_cell()
{
    generate_implied_end_tags();
    if (!current_node().local_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
        PARSE_ERROR();
    }
    while (!current_node().local_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th))
        m_stack_of_open_elements.pop();
    m_stack_of_open_elements.pop();
    m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
    m_insertion_mode = InsertionMode::InRow;
}

void HTMLDocumentParser::handle_in_cell(HTMLToken& token)
{
    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        generate_implied_end_tags();

        if (current_node().local_name() != token.tag_name()) {
            PARSE_ERROR();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());

        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InRow;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::td) && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::th)) {
            ASSERT(m_parsing_fragment);
            PARSE_ERROR();
            return;
        }
        close_the_cell();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html)) {
        PARSE_ERROR();
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
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
        if (token.code_point() == 0) {
            PARSE_ERROR();
            return;
        }

        m_pending_table_character_tokens.append(token);
        return;
    }

    for (auto& pending_token : m_pending_table_character_tokens) {
        ASSERT(pending_token.is_character());
        if (!pending_token.is_parser_whitespace()) {
            // If any of the tokens in the pending table character tokens list
            // are character tokens that are not ASCII whitespace, then this is a parse error:
            // reprocess the character tokens in the pending table character tokens list using
            // the rules given in the "anything else" entry in the "in table" insertion mode.
            PARSE_ERROR();
            m_foster_parenting = true;
            process_using_the_rules_for(InsertionMode::InBody, token);
            m_foster_parenting = false;
            return;
        }
    }

    for (auto& pending_token : m_pending_table_character_tokens) {
        insert_character(pending_token.code_point());
    }

    m_insertion_mode = m_original_insertion_mode;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::handle_in_table_body(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::tr) {
        clear_the_stack_back_to_a_table_body_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InRow;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::th, HTML::TagNames::td)) {
        PARSE_ERROR();
        clear_the_stack_back_to_a_table_body_context();
        insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::tr));
        m_insertion_mode = InsertionMode::InRow;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            PARSE_ERROR();
            return;
        }
        clear_the_stack_back_to_a_table_body_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {

        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tbody)
            && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::thead)
            && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tfoot)) {
            PARSE_ERROR();
            return;
        }

        clear_the_stack_back_to_a_table_body_context();
        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(InsertionMode::InTable, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::td, HTML::TagNames::th, HTML::TagNames::tr)) {
        PARSE_ERROR();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLDocumentParser::handle_in_table(HTMLToken& token)
{
    if (token.is_character() && current_node().local_name().is_one_of(HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr)) {
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
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::caption) {
        clear_the_stack_back_to_a_table_context();
        m_list_of_active_formatting_elements.add_marker();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InCaption;
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::colgroup) {
        clear_the_stack_back_to_a_table_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InColumnGroup;
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::col) {
        clear_the_stack_back_to_a_table_context();
        insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::colgroup));
        m_insertion_mode = InsertionMode::InColumnGroup;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        clear_the_stack_back_to_a_table_context();
        insert_html_element(token);
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th, HTML::TagNames::tr)) {
        clear_the_stack_back_to_a_table_context();
        insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::tbody));
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::table) {
        PARSE_ERROR();
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::table))
            return;

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::table);

        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::table) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::table)) {
            PARSE_ERROR();
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::table);

        reset_the_insertion_mode_appropriately();
        return;
    }
    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        PARSE_ERROR();
        return;
    }
    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::style, HTML::TagNames::script, HTML::TagNames::template_))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_)) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::input) {
        auto type_attribute = token.attribute(HTML::AttributeNames::type);
        if (type_attribute.is_null() || !type_attribute.equals_ignoring_case("hidden")) {
            goto AnythingElse;
        }

        PARSE_ERROR();
        insert_html_element(token);

        // FIXME: Is this the correct interpretation of "Pop that input element off the stack of open elements."?
        //        Because this wording is the first time it's seen in the spec.
        //        Other times it's worded as: "Immediately pop the current node off the stack of open elements."
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::form) {
        PARSE_ERROR();
        if (m_form_element || m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            return;
        }

        m_form_element = downcast<HTMLFormElement>(*insert_html_element(token));

        // FIXME: See previous FIXME, as this is the same situation but for form.
        m_stack_of_open_elements.pop();
        return;
    }
    if (token.is_end_of_file()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

AnythingElse:
    PARSE_ERROR();
    m_foster_parenting = true;
    process_using_the_rules_for(InsertionMode::InBody, token);
    m_foster_parenting = false;
}

void HTMLDocumentParser::handle_in_select_in_table(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::td, HTML::TagNames::th)) {
        PARSE_ERROR();
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::td, HTML::TagNames::th)) {
        PARSE_ERROR();

        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name()))
            return;

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    process_using_the_rules_for(InsertionMode::InSelect, token);
}

void HTMLDocumentParser::handle_in_select(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            PARSE_ERROR();
            return;
        }
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::option) {
        if (current_node().local_name() == HTML::TagNames::option) {
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::optgroup) {
        if (current_node().local_name() == HTML::TagNames::option) {
            m_stack_of_open_elements.pop();
        }
        if (current_node().local_name() == HTML::TagNames::optgroup) {
            m_stack_of_open_elements.pop();
        }
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::optgroup) {
        if (current_node().local_name() == HTML::TagNames::option && node_before_current_node().local_name() == HTML::TagNames::optgroup)
            m_stack_of_open_elements.pop();

        if (current_node().local_name() == HTML::TagNames::optgroup) {
            m_stack_of_open_elements.pop();
        } else {
            PARSE_ERROR();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::option) {
        if (current_node().local_name() == HTML::TagNames::option) {
            m_stack_of_open_elements.pop();
        } else {
            PARSE_ERROR();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::select) {
        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            ASSERT(m_parsing_fragment);
            PARSE_ERROR();
            return;
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::select) {
        PARSE_ERROR();

        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            ASSERT(m_parsing_fragment);
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::input, HTML::TagNames::keygen, HTML::TagNames::textarea)) {
        PARSE_ERROR();

        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            ASSERT(m_parsing_fragment);
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::script, HTML::TagNames::template_)) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    PARSE_ERROR();
}

void HTMLDocumentParser::handle_in_caption(HTMLToken& token)
{
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::caption) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::caption)) {
            ASSERT(m_parsing_fragment);
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::caption)
            PARSE_ERROR();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::caption);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::caption)) {
            ASSERT(m_parsing_fragment);
            PARSE_ERROR();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::caption)
            PARSE_ERROR();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::caption);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        PARSE_ERROR();
        return;
    }

    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLDocumentParser::handle_in_column_group(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::col) {
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::colgroup) {
        if (current_node().local_name() != HTML::TagNames::colgroup) {
            PARSE_ERROR();
            return;
        }

        m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::col) {
        PARSE_ERROR();
        return;
    }

    if ((token.is_start_tag() || token.is_end_tag()) && token.tag_name() == HTML::TagNames::template_) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (current_node().local_name() != HTML::TagNames::colgroup) {
        PARSE_ERROR();
        return;
    }

    m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::InTable;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLDocumentParser::handle_in_template(HTMLToken& token)
{
    if (token.is_character() || token.is_comment() || token.is_doctype()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::noframes, HTML::TagNames::script, HTML::TagNames::style, HTML::TagNames::template_, HTML::TagNames::title)) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        m_stack_of_template_insertion_modes.take_last();
        m_stack_of_template_insertion_modes.append(InsertionMode::InTable);
        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::col) {
        m_stack_of_template_insertion_modes.take_last();
        m_stack_of_template_insertion_modes.append(InsertionMode::InColumnGroup);
        m_insertion_mode = InsertionMode::InColumnGroup;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::tr) {
        m_stack_of_template_insertion_modes.take_last();
        m_stack_of_template_insertion_modes.append(InsertionMode::InTableBody);
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
        m_stack_of_template_insertion_modes.take_last();
        m_stack_of_template_insertion_modes.append(InsertionMode::InRow);
        m_insertion_mode = InsertionMode::InRow;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag()) {
        m_stack_of_template_insertion_modes.take_last();
        m_stack_of_template_insertion_modes.append(InsertionMode::InBody);
        m_insertion_mode = InsertionMode::InBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag()) {
        PARSE_ERROR();
        return;
    }

    if (token.is_end_of_file()) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            ASSERT(m_parsing_fragment);
            stop_parsing();
            return;
        }

        PARSE_ERROR();
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::template_);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        m_stack_of_template_insertion_modes.take_last();
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
    }
}

void HTMLDocumentParser::handle_in_frameset(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::frameset) {
        // FIXME: If the current node is the root html element, then this is a parse error; ignore the token. (fragment case)

        m_stack_of_open_elements.pop();

        if (!m_parsing_fragment && current_node().local_name() != HTML::TagNames::frameset) {
            m_insertion_mode = InsertionMode::AfterFrameset;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frame) {
        insert_html_element(token);
        m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::noframes) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        //FIXME: If the current node is not the root html element, then this is a parse error.

        stop_parsing();
        return;
    }

    PARSE_ERROR();
}

void HTMLDocumentParser::handle_after_frameset(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        insert_character(token.code_point());
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

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::html) {
        m_insertion_mode = InsertionMode::AfterAfterFrameset;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::noframes) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    PARSE_ERROR();
}

void HTMLDocumentParser::handle_after_after_frameset(HTMLToken& token)
{
    if (token.is_comment()) {
        auto comment = adopt(*new DOM::Comment(document(), token.m_comment_or_character.data.to_string()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype() || token.is_parser_whitespace() || (token.is_start_tag() && token.tag_name() == HTML::TagNames::html)) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::noframes) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    PARSE_ERROR();
}

void HTMLDocumentParser::reset_the_insertion_mode_appropriately()
{
    for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
        bool last = i == 0;
        // NOTE: When parsing fragments, we substitute the context element for the root of the stack of open elements.
        RefPtr<DOM::Element> node;
        if (last && m_parsing_fragment) {
            node = m_context_element;
        } else {
            node = m_stack_of_open_elements.elements().at(i);
        }

        if (node->local_name() == HTML::TagNames::select) {
            TODO();
        }

        if (!last && node->local_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
            m_insertion_mode = InsertionMode::InCell;
            return;
        }

        if (node->local_name() == HTML::TagNames::tr) {
            m_insertion_mode = InsertionMode::InRow;
            return;
        }

        if (node->local_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::thead, HTML::TagNames::tfoot)) {
            m_insertion_mode = InsertionMode::InTableBody;
            return;
        }

        if (node->local_name() == HTML::TagNames::caption) {
            m_insertion_mode = InsertionMode::InCaption;
            return;
        }

        if (node->local_name() == HTML::TagNames::colgroup) {
            m_insertion_mode = InsertionMode::InColumnGroup;
            return;
        }

        if (node->local_name() == HTML::TagNames::table) {
            m_insertion_mode = InsertionMode::InTable;
            return;
        }

        if (node->local_name() == HTML::TagNames::template_) {
            m_insertion_mode = m_stack_of_template_insertion_modes.last();
            return;
        }

        if (!last && node->local_name() == HTML::TagNames::head) {
            m_insertion_mode = InsertionMode::InHead;
            return;
        }

        if (node->local_name() == HTML::TagNames::body) {
            m_insertion_mode = InsertionMode::InBody;
            return;
        }

        if (node->local_name() == HTML::TagNames::frameset) {
            ASSERT(m_parsing_fragment);
            m_insertion_mode = InsertionMode::InFrameset;
            return;
        }

        if (node->local_name() == HTML::TagNames::html) {
            if (!m_head_element) {
                ASSERT(m_parsing_fragment);
                m_insertion_mode = InsertionMode::BeforeHead;
                return;
            }

            m_insertion_mode = InsertionMode::AfterHead;
            return;
        }
    }

    ASSERT(m_parsing_fragment);
    m_insertion_mode = InsertionMode::InBody;
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

DOM::Document& HTMLDocumentParser::document()
{
    return *m_document;
}

NonnullRefPtrVector<DOM::Node> HTMLDocumentParser::parse_html_fragment(DOM::Element& context_element, const StringView& markup)
{
    HTMLDocumentParser parser(markup, "utf-8");
    parser.m_context_element = context_element;
    parser.m_parsing_fragment = true;
    parser.document().set_quirks_mode(context_element.document().mode());

    if (context_element.local_name().is_one_of(HTML::TagNames::title, HTML::TagNames::textarea)) {
        parser.m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::style, HTML::TagNames::xmp, HTML::TagNames::iframe, HTML::TagNames::noembed, HTML::TagNames::noframes)) {
        parser.m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::script)) {
        parser.m_tokenizer.switch_to({}, HTMLTokenizer::State::ScriptData);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::noscript)) {
        if (context_element.document().is_scripting_enabled())
            parser.m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::plaintext)) {
        parser.m_tokenizer.switch_to({}, HTMLTokenizer::State::PLAINTEXT);
    }

    auto root = create_element(context_element.document(), HTML::TagNames::html);
    parser.document().append_child(root);
    parser.m_stack_of_open_elements.push(root);

    if (context_element.local_name() == HTML::TagNames::template_) {
        parser.m_stack_of_template_insertion_modes.append(InsertionMode::InTemplate);
    }

    // FIXME: Create a start tag token whose name is the local name of context and whose attributes are the attributes of context.

    parser.reset_the_insertion_mode_appropriately();

    for (auto* form_candidate = &context_element; form_candidate; form_candidate = form_candidate->parent_element()) {
        if (is<HTMLFormElement>(*form_candidate)) {
            parser.m_form_element = downcast<HTMLFormElement>(*form_candidate);
            break;
        }
    }

    parser.run(context_element.document().url());

    NonnullRefPtrVector<DOM::Node> children;
    while (RefPtr<DOM::Node> child = root->first_child()) {
        root->remove_child(*child);
        context_element.document().adopt_node(*child);
        children.append(*child);
    }
    return children;
}

}
