/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/SourceLocation.h>
#include <AK/Utf32View.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ProcessingInstruction.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLHeadElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/HTML/Parser/HTMLEncodingDetection.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Parser/HTMLToken.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::HTML {

static inline void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln("Parse error! {}", location);
}

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

// https://html.spec.whatwg.org/multipage/parsing.html#mathml-text-integration-point
static bool is_mathml_text_integration_point(DOM::Element const&)
{
    // FIXME: Implement.
    return false;
}

// https://html.spec.whatwg.org/multipage/parsing.html#html-integration-point
static bool is_html_integration_point(DOM::Element const& element)
{
    // A node is an HTML integration point if it is one of the following elements:
    // FIXME: A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "text/html"
    // FIXME: A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "application/xhtml+xml"

    // An SVG foreignObject element
    // An SVG desc element
    // An SVG title element
    if (element.tag_name().is_one_of(SVG::TagNames::foreignObject, SVG::TagNames::desc, SVG::TagNames::title))
        return true;

    return false;
}

RefPtr<DOM::Document> parse_html_document(StringView data, const AK::URL& url, const String& encoding)
{
    auto document = DOM::Document::create(url);
    auto parser = HTMLParser::create(document, data, encoding);
    parser->run(url);
    return document;
}

HTMLParser::HTMLParser(DOM::Document& document, StringView input, const String& encoding)
    : m_tokenizer(input, encoding)
    , m_document(document)
{
    m_tokenizer.set_parser({}, *this);
    m_document->set_parser({}, *this);
    m_document->set_should_invalidate_styles_on_attribute_changes(false);
    auto standardized_encoding = TextCodec::get_standardized_encoding(encoding);
    VERIFY(standardized_encoding.has_value());
    m_document->set_encoding(standardized_encoding.value());
}

HTMLParser::HTMLParser(DOM::Document& document)
    : m_document(document)
{
    m_document->set_parser({}, *this);
    m_tokenizer.set_parser({}, *this);
}

HTMLParser::~HTMLParser()
{
    m_document->set_should_invalidate_styles_on_attribute_changes(true);
}

void HTMLParser::run()
{
    for (;;) {
        // FIXME: Find a better way to say that we come from Document::close() and want to process EOF.
        if (!m_tokenizer.is_eof_inserted() && m_tokenizer.is_insertion_point_reached())
            return;

        auto optional_token = m_tokenizer.next_token();
        if (!optional_token.has_value())
            break;
        auto& token = optional_token.value();

        dbgln_if(PARSER_DEBUG, "[{}] {}", insertion_mode_name(), token.to_string());

        // https://html.spec.whatwg.org/multipage/parsing.html#tree-construction-dispatcher
        // As each token is emitted from the tokenizer, the user agent must follow the appropriate steps from the following list, known as the tree construction dispatcher:
        if (m_stack_of_open_elements.is_empty()
            || adjusted_current_node().namespace_() == Namespace::HTML
            || (is_html_integration_point(adjusted_current_node()) && (token.is_start_tag() || token.is_character()))
            || token.is_end_of_file()) {
            // -> If the stack of open elements is empty
            // -> If the adjusted current node is an element in the HTML namespace
            // FIXME: -> If the adjusted current node is a MathML text integration point and the token is a start tag whose tag name is neither "mglyph" nor "malignmark"
            // FIXME: -> If the adjusted current node is a MathML text integration point and the token is a character token
            // FIXME: -> If the adjusted current node is a MathML annotation-xml element and the token is a start tag whose tag name is "svg"
            // -> If the adjusted current node is an HTML integration point and the token is a start tag
            // -> If the adjusted current node is an HTML integration point and the token is a character token
            // -> If the token is an end-of-file token

            // Process the token according to the rules given in the section corresponding to the current insertion mode in HTML content.
            process_using_the_rules_for(m_insertion_mode, token);
        } else {
            // -> Otherwise

            // Process the token according to the rules given in the section for parsing tokens in foreign content.
            process_using_the_rules_for_foreign_content(token);
        }

        if (m_stop_parsing) {
            dbgln_if(PARSER_DEBUG, "Stop parsing{}! :^)", m_parsing_fragment ? " fragment" : "");
            break;
        }
    }

    flush_character_insertions();
}

void HTMLParser::run(const AK::URL& url)
{
    m_document->set_url(url);
    m_document->set_source(m_tokenizer.source());
    run();
    the_end();
    m_document->detach_parser({});
}

// https://html.spec.whatwg.org/multipage/parsing.html#the-end
void HTMLParser::the_end()
{
    // Once the user agent stops parsing the document, the user agent must run the following steps:

    // FIXME: 1. If the active speculative HTML parser is not null, then stop the speculative HTML parser and return.

    // 2. Set the insertion point to undefined.
    m_tokenizer.undefine_insertion_point();

    // 3. Update the current document readiness to "interactive".
    m_document->update_readiness(HTML::DocumentReadyState::Interactive);

    // 4. Pop all the nodes off the stack of open elements.
    while (!m_stack_of_open_elements.is_empty())
        (void)m_stack_of_open_elements.pop();

    // 5. While the list of scripts that will execute when the document has finished parsing is not empty:
    while (!m_document->scripts_to_execute_when_parsing_has_finished().is_empty()) {
        // 1. Spin the event loop until the first script in the list of scripts that will execute when the document has finished parsing
        //    has its "ready to be parser-executed" flag set and the parser's Document has no style sheet that is blocking scripts.
        main_thread_event_loop().spin_until([&] {
            return m_document->scripts_to_execute_when_parsing_has_finished().first().is_ready_to_be_parser_executed()
                && !m_document->has_a_style_sheet_that_is_blocking_scripts();
        });

        // 2. Execute the first script in the list of scripts that will execute when the document has finished parsing.
        m_document->scripts_to_execute_when_parsing_has_finished().first().execute_script();

        // 3. Remove the first script element from the list of scripts that will execute when the document has finished parsing (i.e. shift out the first entry in the list).
        (void)m_document->scripts_to_execute_when_parsing_has_finished().take_first();
    }

    // 6. Queue a global task on the DOM manipulation task source given the Document's relevant global object to run the following substeps:
    old_queue_global_task_with_document(HTML::Task::Source::DOMManipulation, m_document, [document = m_document]() mutable {
        // FIXME: 1. Set the Document's load timing info's DOM content loaded event start time to the current high resolution time given the Document's relevant global object.

        // 2. Fire an event named DOMContentLoaded at the Document object, with its bubbles attribute initialized to true.
        auto content_loaded_event = DOM::Event::create(HTML::EventNames::DOMContentLoaded);
        content_loaded_event->set_bubbles(true);
        document->dispatch_event(content_loaded_event);

        // FIXME: 3. Set the Document's load timing info's DOM content loaded event end time to the current high resolution time given the Document's relevant global object.

        // FIXME: 4. Enable the client message queue of the ServiceWorkerContainer object whose associated service worker client is the Document object's relevant settings object.

        // FIXME: 5. Invoke WebDriver BiDi DOM content loaded with the Document's browsing context, and a new WebDriver BiDi navigation status whose id is the Document object's navigation id, status is "pending", and url is the Document object's URL.
    });

    // 7. Spin the event loop until the set of scripts that will execute as soon as possible and the list of scripts that will execute in order as soon as possible are empty.
    main_thread_event_loop().spin_until([&] {
        return m_document->scripts_to_execute_as_soon_as_possible().is_empty();
    });

    // 8. Spin the event loop until there is nothing that delays the load event in the Document.
    // FIXME: Track down all the things that are supposed to delay the load event.
    main_thread_event_loop().spin_until([&] {
        return m_document->number_of_things_delaying_the_load_event() == 0;
    });

    // 9. Queue a global task on the DOM manipulation task source given the Document's relevant global object to run the following steps:
    old_queue_global_task_with_document(HTML::Task::Source::DOMManipulation, m_document, [document = m_document]() mutable {
        // 1. Update the current document readiness to "complete".
        document->update_readiness(HTML::DocumentReadyState::Complete);

        // 2. If the Document object's browsing context is null, then abort these steps.
        if (!document->browsing_context())
            return;

        // 3. Let window be the Document's relevant global object.
        NonnullRefPtr<DOM::Window> window = document->window();

        // FIXME: 4. Set the Document's load timing info's load event start time to the current high resolution time given window.

        // 5. Fire an event named load at window, with legacy target override flag set.
        // FIXME: The legacy target override flag is currently set by a virtual override of dispatch_event()
        //        We should reorganize this so that the flag appears explicitly here instead.
        window->dispatch_event(DOM::Event::create(HTML::EventNames::load));

        // FIXME: 6. Invoke WebDriver BiDi load complete with the Document's browsing context, and a new WebDriver BiDi navigation status whose id is the Document object's navigation id, status is "complete", and url is the Document object's URL.

        // FIXME: 7. Set the Document object's navigation id to null.

        // FIXME: 8. Set the Document's load timing info's load event end time to the current high resolution time given window.

        // 9. Assert: Document's page showing is false.
        VERIFY(!document->page_showing());

        // 10. Set the Document's page showing flag to true.
        document->set_page_showing(true);

        // 11. Fire a page transition event named pageshow at window with false.
        window->fire_a_page_transition_event(HTML::EventNames::pageshow, false);

        // 12. Completely finish loading the Document.
        document->completely_finish_loading();

        // FIXME: 13. Queue the navigation timing entry for the Document.
    });

    // FIXME: 10. If the Document's print when loaded flag is set, then run the printing steps.

    // 11. The Document is now ready for post-load tasks.
    m_document->set_ready_for_post_load_tasks(true);
}

void HTMLParser::process_using_the_rules_for(InsertionMode mode, HTMLToken& token)
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
        VERIFY_NOT_REACHED();
    }
}

DOM::QuirksMode HTMLParser::which_quirks_mode(const HTMLToken& doctype_token) const
{
    if (doctype_token.doctype_data().force_quirks)
        return DOM::QuirksMode::Yes;

    // NOTE: The tokenizer puts the name into lower case for us.
    if (doctype_token.doctype_data().name != "html")
        return DOM::QuirksMode::Yes;

    auto const& public_identifier = doctype_token.doctype_data().public_identifier;
    auto const& system_identifier = doctype_token.doctype_data().system_identifier;

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

    if (doctype_token.doctype_data().missing_system_identifier) {
        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Frameset//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Yes;

        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Transitional//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Yes;
    }

    if (public_identifier.starts_with("-//W3C//DTD XHTML 1.0 Frameset//", CaseSensitivity::CaseInsensitive))
        return DOM::QuirksMode::Limited;

    if (public_identifier.starts_with("-//W3C//DTD XHTML 1.0 Transitional//", CaseSensitivity::CaseInsensitive))
        return DOM::QuirksMode::Limited;

    if (!doctype_token.doctype_data().missing_system_identifier) {
        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Frameset//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Limited;

        if (public_identifier.starts_with("-//W3C//DTD HTML 4.01 Transitional//", CaseSensitivity::CaseInsensitive))
            return DOM::QuirksMode::Limited;
    }

    return DOM::QuirksMode::No;
}

void HTMLParser::handle_initial(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_comment()) {
        auto comment = adopt_ref(*new DOM::Comment(document(), token.comment()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_doctype()) {
        auto doctype = adopt_ref(*new DOM::DocumentType(document()));
        doctype->set_name(token.doctype_data().name);
        doctype->set_public_id(token.doctype_data().public_identifier);
        doctype->set_system_id(token.doctype_data().system_identifier);
        document().append_child(move(doctype));
        document().set_quirks_mode(which_quirks_mode(token));
        m_insertion_mode = InsertionMode::BeforeHTML;
        return;
    }

    log_parse_error();
    document().set_quirks_mode(DOM::QuirksMode::Yes);
    m_insertion_mode = InsertionMode::BeforeHTML;
    process_using_the_rules_for(InsertionMode::BeforeHTML, token);
}

void HTMLParser::handle_before_html(HTMLToken& token)
{
    if (token.is_doctype()) {
        log_parse_error();
        return;
    }

    if (token.is_comment()) {
        auto comment = adopt_ref(*new DOM::Comment(document(), token.comment()));
        document().append_child(move(comment));
        return;
    }

    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        auto element = create_element_for(token, Namespace::HTML);
        document().append_child(element);
        m_stack_of_open_elements.push(move(element));
        m_insertion_mode = InsertionMode::BeforeHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::head, HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        log_parse_error();
        return;
    }

AnythingElse:
    auto element = create_element(document(), HTML::TagNames::html, Namespace::HTML);
    document().append_child(element);
    m_stack_of_open_elements.push(element);
    // FIXME: If the Document is being loaded as part of navigation of a browsing context, then: run the application cache selection algorithm with no manifest, passing it the Document object.
    m_insertion_mode = InsertionMode::BeforeHead;
    process_using_the_rules_for(InsertionMode::BeforeHead, token);
    return;
}

DOM::Element& HTMLParser::current_node()
{
    return m_stack_of_open_elements.current_node();
}

DOM::Element& HTMLParser::adjusted_current_node()
{
    if (m_parsing_fragment && m_stack_of_open_elements.elements().size() == 1)
        return *m_context_element;

    return current_node();
}

DOM::Element& HTMLParser::node_before_current_node()
{
    return m_stack_of_open_elements.elements().at(m_stack_of_open_elements.elements().size() - 2);
}

// https://html.spec.whatwg.org/multipage/parsing.html#appropriate-place-for-inserting-a-node
HTMLParser::AdjustedInsertionLocation HTMLParser::find_appropriate_place_for_inserting_node()
{
    auto& target = current_node();
    HTMLParser::AdjustedInsertionLocation adjusted_insertion_location;

    // 2. Determine the adjusted insertion location using the first matching steps from the following list:

    // `-> If foster parenting is enabled and target is a table, tbody, tfoot, thead, or tr element
    if (m_foster_parenting && target.local_name().is_one_of(HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr)) {
        // 1. Let last template be the last template element in the stack of open elements, if any.
        auto last_template = m_stack_of_open_elements.last_element_with_tag_name(HTML::TagNames::template_);
        // 2. Let last table be the last table element in the stack of open elements, if any.
        auto last_table = m_stack_of_open_elements.last_element_with_tag_name(HTML::TagNames::table);
        // 3. If there is a last template and either there is no last table,
        //    or there is one, but last template is lower (more recently added) than last table in the stack of open elements,
        if (last_template.element && (!last_table.element || last_template.index > last_table.index)) {
            // then: let adjusted insertion location be inside last template's template contents, after its last child (if any), and abort these steps.

            // NOTE: This returns the template content, so no need to check the parent is a template.
            return { verify_cast<HTMLTemplateElement>(last_template.element)->content(), nullptr };
        }
        // 4. If there is no last table, then let adjusted insertion location be inside the first element in the stack of open elements (the html element),
        //    after its last child (if any), and abort these steps. (fragment case)
        if (!last_table.element) {
            VERIFY(m_parsing_fragment);
            // Guaranteed not to be a template element (it will be the html element),
            // so no need to check the parent is a template.
            return { m_stack_of_open_elements.elements().first(), nullptr };
        }
        // 5. If last table has a parent node, then let adjusted insertion location be inside last table's parent node, immediately before last table, and abort these steps.
        if (last_table.element->parent_node()) {
            adjusted_insertion_location = { last_table.element->parent_node(), last_table.element };
        } else {
            // 6. Let previous element be the element immediately above last table in the stack of open elements.
            auto previous_element = m_stack_of_open_elements.element_immediately_above(*last_table.element);

            // 7. Let adjusted insertion location be inside previous element, after its last child (if any).
            adjusted_insertion_location = { previous_element, nullptr };
        }
    } else {
        // `-> Otherwise
        //     Let adjusted insertion location be inside target, after its last child (if any).
        adjusted_insertion_location = { target, nullptr };
    }

    if (is<HTMLTemplateElement>(*adjusted_insertion_location.parent))
        return { verify_cast<HTMLTemplateElement>(*adjusted_insertion_location.parent).content(), nullptr };

    return adjusted_insertion_location;
}

NonnullRefPtr<DOM::Element> HTMLParser::create_element_for(const HTMLToken& token, const FlyString& namespace_)
{
    auto element = create_element(document(), token.tag_name(), namespace_);
    token.for_each_attribute([&](auto& attribute) {
        element->set_attribute(attribute.local_name, attribute.value);
        return IterationDecision::Continue;
    });
    return element;
}

// https://html.spec.whatwg.org/multipage/parsing.html#insert-a-foreign-element
NonnullRefPtr<DOM::Element> HTMLParser::insert_foreign_element(const HTMLToken& token, const FlyString& namespace_)
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();

    // FIXME: Pass in adjusted_insertion_location.parent as the intended parent.
    auto element = create_element_for(token, namespace_);

    auto pre_insertion_validity = adjusted_insertion_location.parent->ensure_pre_insertion_validity(element, adjusted_insertion_location.insert_before_sibling);

    // NOTE: If it's not possible to insert the element at the adjusted insertion location, the element is simply dropped.
    if (!pre_insertion_validity.is_exception()) {
        if (!m_parsing_fragment) {
            // FIXME: push a new element queue onto element's relevant agent's custom element reactions stack.
        }

        adjusted_insertion_location.parent->insert_before(element, adjusted_insertion_location.insert_before_sibling);

        if (!m_parsing_fragment) {
            // FIXME: pop the element queue from element's relevant agent's custom element reactions stack, and invoke custom element reactions in that queue.
        }
    }

    m_stack_of_open_elements.push(element);
    return element;
}

NonnullRefPtr<DOM::Element> HTMLParser::insert_html_element(const HTMLToken& token)
{
    return insert_foreign_element(token, Namespace::HTML);
}

void HTMLParser::handle_before_head(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        return;
    }

    if (token.is_comment()) {
        insert_comment(token);
        return;
    }

    if (token.is_doctype()) {
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::head) {
        auto element = insert_html_element(token);
        m_head_element = verify_cast<HTMLHeadElement>(*element);
        m_insertion_mode = InsertionMode::InHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::head, HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_end_tag()) {
        log_parse_error();
        return;
    }

AnythingElse:
    m_head_element = verify_cast<HTMLHeadElement>(*insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::head)));
    m_insertion_mode = InsertionMode::InHead;
    process_using_the_rules_for(InsertionMode::InHead, token);
    return;
}

void HTMLParser::insert_comment(HTMLToken& token)
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    adjusted_insertion_location.parent->insert_before(adopt_ref(*new DOM::Comment(document(), token.comment())), adjusted_insertion_location.insert_before_sibling);
}

void HTMLParser::handle_in_head(HTMLToken& token)
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link)) {
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::meta) {
        auto element = insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::title) {
        (void)insert_html_element(token);
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
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InHeadNoscript;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::script) {
        auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
        auto element = create_element_for(token, Namespace::HTML);
        auto& script_element = verify_cast<HTMLScriptElement>(*element);
        script_element.set_parser_document({}, document());
        script_element.set_non_blocking({}, false);

        if (m_parsing_fragment) {
            script_element.set_already_started({}, true);
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
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::AfterHead;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::html, HTML::TagNames::br)) {
        goto AnythingElse;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::template_) {
        (void)insert_html_element(token);
        m_list_of_active_formatting_elements.add_marker();
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InTemplate;
        m_stack_of_template_insertion_modes.append(InsertionMode::InTemplate);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::template_) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            log_parse_error();
            return;
        }

        generate_all_implied_end_tags_thoroughly();

        if (current_node().local_name() != HTML::TagNames::template_)
            log_parse_error();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::template_);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        m_stack_of_template_insertion_modes.take_last();
        reset_the_insertion_mode_appropriately();
        return;
    }

    if ((token.is_start_tag() && token.tag_name() == HTML::TagNames::head) || token.is_end_tag()) {
        log_parse_error();
        return;
    }

AnythingElse:
    (void)m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::AfterHead;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLParser::handle_in_head_noscript(HTMLToken& token)
{
    if (token.is_doctype()) {
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::noscript) {
        (void)m_stack_of_open_elements.pop();
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
        log_parse_error();
        return;
    }

AnythingElse:
    log_parse_error();
    (void)m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::InHead;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLParser::parse_generic_raw_text_element(HTMLToken& token)
{
    (void)insert_html_element(token);
    m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    m_original_insertion_mode = m_insertion_mode;
    m_insertion_mode = InsertionMode::Text;
}

DOM::Text* HTMLParser::find_character_insertion_node()
{
    auto adjusted_insertion_location = find_appropriate_place_for_inserting_node();
    if (adjusted_insertion_location.insert_before_sibling) {
        TODO();
    }
    if (adjusted_insertion_location.parent->is_document())
        return nullptr;
    if (adjusted_insertion_location.parent->last_child() && adjusted_insertion_location.parent->last_child()->is_text())
        return verify_cast<DOM::Text>(adjusted_insertion_location.parent->last_child());
    auto new_text_node = adopt_ref(*new DOM::Text(document(), ""));
    adjusted_insertion_location.parent->append_child(new_text_node);
    return new_text_node;
}

void HTMLParser::flush_character_insertions()
{
    if (m_character_insertion_builder.is_empty())
        return;
    m_character_insertion_node->set_data(m_character_insertion_builder.to_string());
    m_character_insertion_node->parent()->children_changed();
    m_character_insertion_builder.clear();
}

void HTMLParser::insert_character(u32 data)
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

void HTMLParser::handle_after_head(HTMLToken& token)
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::body) {
        (void)insert_html_element(token);
        m_frameset_ok = false;
        m_insertion_mode = InsertionMode::InBody;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InFrameset;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::base, HTML::TagNames::basefont, HTML::TagNames::bgsound, HTML::TagNames::link, HTML::TagNames::meta, HTML::TagNames::noframes, HTML::TagNames::script, HTML::TagNames::style, HTML::TagNames::template_, HTML::TagNames::title)) {
        log_parse_error();
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
        log_parse_error();
        return;
    }

AnythingElse:
    (void)insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::body));
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLParser::generate_implied_end_tags(const FlyString& exception)
{
    while (current_node().local_name() != exception && current_node().local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc))
        (void)m_stack_of_open_elements.pop();
}

void HTMLParser::generate_all_implied_end_tags_thoroughly()
{
    while (current_node().local_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::colgroup, HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))
        (void)m_stack_of_open_elements.pop();
}

void HTMLParser::close_a_p_element()
{
    generate_implied_end_tags(HTML::TagNames::p);
    if (current_node().local_name() != HTML::TagNames::p) {
        log_parse_error();
    }
    m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::p);
}

void HTMLParser::handle_after_body(HTMLToken& token)
{
    if (token.is_character() && token.is_parser_whitespace()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_comment()) {
        auto& insertion_location = m_stack_of_open_elements.first();
        insertion_location.append_child(adopt_ref(*new DOM::Comment(document(), token.comment())));
        return;
    }

    if (token.is_doctype()) {
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::html) {
        if (m_parsing_fragment) {
            log_parse_error();
            return;
        }
        m_insertion_mode = InsertionMode::AfterAfterBody;
        return;
    }

    if (token.is_end_of_file()) {
        stop_parsing();
        return;
    }

    log_parse_error();
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLParser::handle_after_after_body(HTMLToken& token)
{
    if (token.is_comment()) {
        auto comment = adopt_ref(*new DOM::Comment(document(), token.comment()));
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

    log_parse_error();
    m_insertion_mode = InsertionMode::InBody;
    process_using_the_rules_for(m_insertion_mode, token);
}

// https://html.spec.whatwg.org/multipage/parsing.html#reconstruct-the-active-formatting-elements
void HTMLParser::reconstruct_the_active_formatting_elements()
{
    // 1. If there are no entries in the list of active formatting elements, then there is nothing to reconstruct; stop this algorithm.
    if (m_list_of_active_formatting_elements.is_empty())
        return;

    // 2. If the last (most recently added) entry in the list of active formatting elements is a marker, or if it is an element that is in the stack of open elements,
    //    then there is nothing to reconstruct; stop this algorithm.
    if (m_list_of_active_formatting_elements.entries().last().is_marker())
        return;

    if (m_stack_of_open_elements.contains(*m_list_of_active_formatting_elements.entries().last().element))
        return;

    // 3. Let entry be the last (most recently added) element in the list of active formatting elements.
    size_t index = m_list_of_active_formatting_elements.entries().size() - 1;

    // NOTE: Entry will never be null, but must be a pointer instead of a reference to allow rebinding.
    auto* entry = &m_list_of_active_formatting_elements.entries().at(index);

Rewind:
    // 4. Rewind: If there are no entries before entry in the list of active formatting elements, then jump to the step labeled create.
    if (index == 0)
        goto Create;

    // 5. Let entry be the entry one earlier than entry in the list of active formatting elements.
    --index;
    entry = &m_list_of_active_formatting_elements.entries().at(index);

    // 6. If entry is neither a marker nor an element that is also in the stack of open elements, go to the step labeled rewind.
    if (!entry->is_marker() && !m_stack_of_open_elements.contains(*entry->element))
        goto Rewind;

Advance:
    // 7. Advance: Let entry be the element one later than entry in the list of active formatting elements.
    ++index;
    entry = &m_list_of_active_formatting_elements.entries().at(index);

Create:
    // 8. Create: Insert an HTML element for the token for which the element entry was created, to obtain new element.
    VERIFY(!entry->is_marker());

    // FIXME: Hold on to the real token!
    auto new_element = insert_html_element(HTMLToken::make_start_tag(entry->element->local_name()));

    // 9. Replace the entry for entry in the list with an entry for new element.
    m_list_of_active_formatting_elements.entries().at(index).element = new_element;

    // 10. If the entry for new element in the list of active formatting elements is not the last entry in the list, return to the step labeled advance.
    if (index != m_list_of_active_formatting_elements.entries().size() - 1)
        goto Advance;
}

HTMLParser::AdoptionAgencyAlgorithmOutcome HTMLParser::run_the_adoption_agency_algorithm(HTMLToken& token)
{
    auto subject = token.tag_name();

    // If the current node is an HTML element whose tag name is subject,
    // and the current node is not in the list of active formatting elements,
    // then pop the current node off the stack of open elements, and return.
    if (current_node().local_name() == subject && !m_list_of_active_formatting_elements.contains(current_node())) {
        (void)m_stack_of_open_elements.pop();
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    auto formatting_element = m_list_of_active_formatting_elements.last_element_with_tag_name_before_marker(subject);
    if (!formatting_element)
        return AdoptionAgencyAlgorithmOutcome::RunAnyOtherEndTagSteps;

    if (!m_stack_of_open_elements.contains(*formatting_element)) {
        log_parse_error();
        m_list_of_active_formatting_elements.remove(*formatting_element);
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    if (!m_stack_of_open_elements.has_in_scope(*formatting_element)) {
        log_parse_error();
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    if (formatting_element != &current_node()) {
        log_parse_error();
    }

    RefPtr<DOM::Element> furthest_block = m_stack_of_open_elements.topmost_special_node_below(*formatting_element);

    if (!furthest_block) {
        while (&current_node() != formatting_element)
            (void)m_stack_of_open_elements.pop();
        (void)m_stack_of_open_elements.pop();

        m_list_of_active_formatting_elements.remove(*formatting_element);
        return AdoptionAgencyAlgorithmOutcome::DoNothing;
    }

    // FIXME: Implement the rest of the AAA :^)
    return AdoptionAgencyAlgorithmOutcome::DoNothing;
}

bool HTMLParser::is_special_tag(const FlyString& tag_name, const FlyString& namespace_)
{
    if (namespace_ == Namespace::HTML) {
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
    } else if (namespace_ == Namespace::SVG) {
        return tag_name.is_one_of(
            SVG::TagNames::desc,
            SVG::TagNames::foreignObject,
            SVG::TagNames::title);
    } else if (namespace_ == Namespace::MathML) {
        TODO();
    }

    return false;
}

void HTMLParser::handle_in_body(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            log_parse_error();
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        log_parse_error();
        if (m_stack_of_open_elements.contains(HTML::TagNames::template_))
            return;
        token.for_each_attribute([&](auto& attribute) {
            if (!current_node().has_attribute(attribute.local_name))
                current_node().set_attribute(attribute.local_name, attribute.value);
            return IterationDecision::Continue;
        });
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
        log_parse_error();
        if (m_stack_of_open_elements.elements().size() == 1
            || m_stack_of_open_elements.elements().at(1).local_name() != HTML::TagNames::body
            || m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            VERIFY(m_parsing_fragment);
            return;
        }
        m_frameset_ok = false;
        auto& body_element = m_stack_of_open_elements.elements().at(1);
        token.for_each_attribute([&](auto& attribute) {
            if (!body_element.has_attribute(attribute.local_name))
                body_element.set_attribute(attribute.local_name, attribute.value);
            return IterationDecision::Continue;
        });
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        log_parse_error();

        if (m_stack_of_open_elements.elements().size() == 1
            || m_stack_of_open_elements.elements().at(1).local_name() != HTML::TagNames::body) {
            VERIFY(m_parsing_fragment);
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
                log_parse_error();
                break;
            }
        }

        stop_parsing();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::body) {
        if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::body)) {
            log_parse_error();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::body, HTML::TagNames::html)) {
                log_parse_error();
                break;
            }
        }

        m_insertion_mode = InsertionMode::AfterBody;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::html) {
        if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::body)) {
            log_parse_error();
            return;
        }

        for (auto& node : m_stack_of_open_elements.elements()) {
            if (!node.local_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt, HTML::TagNames::li, HTML::TagNames::optgroup, HTML::TagNames::option, HTML::TagNames::p, HTML::TagNames::rb, HTML::TagNames::rp, HTML::TagNames::rt, HTML::TagNames::rtc, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::body, HTML::TagNames::html)) {
                log_parse_error();
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
        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6)) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        if (current_node().local_name().is_one_of(HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6)) {
            log_parse_error();
            (void)m_stack_of_open_elements.pop();
        }
        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::pre, HTML::TagNames::listing)) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();

        (void)insert_html_element(token);

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
            log_parse_error();
            return;
        }
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        auto element = insert_html_element(token);
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_))
            m_form_element = verify_cast<HTMLFormElement>(*element);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::li) {
        m_frameset_ok = false;

        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<DOM::Element> node = m_stack_of_open_elements.elements()[i];

            if (node->local_name() == HTML::TagNames::li) {
                generate_implied_end_tags(HTML::TagNames::li);
                if (current_node().local_name() != HTML::TagNames::li) {
                    log_parse_error();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::li);
                break;
            }

            if (is_special_tag(node->local_name(), node->namespace_()) && !node->local_name().is_one_of(HTML::TagNames::address, HTML::TagNames::div, HTML::TagNames::p))
                break;
        }

        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();

        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt)) {
        m_frameset_ok = false;
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            RefPtr<DOM::Element> node = m_stack_of_open_elements.elements()[i];
            if (node->local_name() == HTML::TagNames::dd) {
                generate_implied_end_tags(HTML::TagNames::dd);
                if (current_node().local_name() != HTML::TagNames::dd) {
                    log_parse_error();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::dd);
                break;
            }
            if (node->local_name() == HTML::TagNames::dt) {
                generate_implied_end_tags(HTML::TagNames::dt);
                if (current_node().local_name() != HTML::TagNames::dt) {
                    log_parse_error();
                }
                m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::dt);
                break;
            }
            if (is_special_tag(node->local_name(), node->namespace_()) && !node->local_name().is_one_of(HTML::TagNames::address, HTML::TagNames::div, HTML::TagNames::p))
                break;
        }
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::plaintext) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        (void)insert_html_element(token);
        m_tokenizer.switch_to({}, HTMLTokenizer::State::PLAINTEXT);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::button) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::button)) {
            log_parse_error();
            generate_implied_end_tags();
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::button);
        }
        reconstruct_the_active_formatting_elements();
        (void)insert_html_element(token);
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::address, HTML::TagNames::article, HTML::TagNames::aside, HTML::TagNames::blockquote, HTML::TagNames::button, HTML::TagNames::center, HTML::TagNames::details, HTML::TagNames::dialog, HTML::TagNames::dir, HTML::TagNames::div, HTML::TagNames::dl, HTML::TagNames::fieldset, HTML::TagNames::figcaption, HTML::TagNames::figure, HTML::TagNames::footer, HTML::TagNames::header, HTML::TagNames::hgroup, HTML::TagNames::listing, HTML::TagNames::main, HTML::TagNames::menu, HTML::TagNames::nav, HTML::TagNames::ol, HTML::TagNames::pre, HTML::TagNames::section, HTML::TagNames::summary, HTML::TagNames::ul)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            log_parse_error();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != token.tag_name()) {
            log_parse_error();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::form) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            auto node = m_form_element;
            m_form_element = nullptr;
            if (!node || !m_stack_of_open_elements.has_in_scope(*node)) {
                log_parse_error();
                return;
            }
            generate_implied_end_tags();
            if (&current_node() != node) {
                log_parse_error();
            }
            m_stack_of_open_elements.elements().remove_first_matching([&](auto& entry) { return entry.ptr() == node.ptr(); });
        } else {
            if (!m_stack_of_open_elements.has_in_scope(HTML::TagNames::form)) {
                log_parse_error();
                return;
            }
            generate_implied_end_tags();
            if (current_node().local_name() != HTML::TagNames::form) {
                log_parse_error();
            }
            m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::form);
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::p) {
        if (!m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p)) {
            log_parse_error();
            (void)insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::p));
        }
        close_a_p_element();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::li) {
        if (!m_stack_of_open_elements.has_in_list_item_scope(HTML::TagNames::li)) {
            log_parse_error();
            return;
        }
        generate_implied_end_tags(HTML::TagNames::li);
        if (current_node().local_name() != HTML::TagNames::li) {
            log_parse_error();
            dbgln("Expected <li> current node, but had <{}>", current_node().local_name());
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::li);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::dd, HTML::TagNames::dt)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            log_parse_error();
            return;
        }
        generate_implied_end_tags(token.tag_name());
        if (current_node().local_name() != token.tag_name()) {
            log_parse_error();
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
            log_parse_error();
            return;
        }

        generate_implied_end_tags();
        if (current_node().local_name() != token.tag_name()) {
            log_parse_error();
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
            log_parse_error();
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
            log_parse_error();
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
        (void)insert_html_element(token);
        m_list_of_active_formatting_elements.add_marker();
        m_frameset_ok = false;
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::applet, HTML::TagNames::marquee, HTML::TagNames::object)) {
        if (!m_stack_of_open_elements.has_in_scope(token.tag_name())) {
            log_parse_error();
            return;
        }

        generate_implied_end_tags();
        if (current_node().local_name() != token.tag_name()) {
            log_parse_error();
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
        (void)insert_html_element(token);
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
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::input) {
        reconstruct_the_active_formatting_elements();
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        auto type_attribute = token.attribute(HTML::AttributeNames::type);
        if (type_attribute.is_null() || !type_attribute.equals_ignoring_case("hidden")) {
            m_frameset_ok = false;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::param, HTML::TagNames::source, HTML::TagNames::track)) {
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::hr) {
        if (m_stack_of_open_elements.has_in_button_scope(HTML::TagNames::p))
            close_a_p_element();
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        m_frameset_ok = false;
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::image) {
        // Parse error. Change the token's tag name to HTML::TagNames::img and reprocess it. (Don't ask.)
        log_parse_error();
        token.set_tag_name("img");
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::textarea) {
        (void)insert_html_element(token);

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
        (void)insert_html_element(token);
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
            (void)m_stack_of_open_elements.pop();
        reconstruct_the_active_formatting_elements();
        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::rb, HTML::TagNames::rtc)) {
        if (m_stack_of_open_elements.has_in_scope(HTML::TagNames::ruby))
            generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::ruby)
            log_parse_error();

        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::rp, HTML::TagNames::rt)) {
        if (m_stack_of_open_elements.has_in_scope(HTML::TagNames::ruby))
            generate_implied_end_tags(HTML::TagNames::rtc);

        if (current_node().local_name() != HTML::TagNames::rtc || current_node().local_name() != HTML::TagNames::ruby)
            log_parse_error();

        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::math) {
        reconstruct_the_active_formatting_elements();
        adjust_mathml_attributes(token);
        adjust_foreign_attributes(token);

        (void)insert_foreign_element(token, Namespace::MathML);

        if (token.is_self_closing()) {
            (void)m_stack_of_open_elements.pop();
            token.acknowledge_self_closing_flag_if_set();
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::svg) {
        reconstruct_the_active_formatting_elements();
        adjust_svg_attributes(token);
        adjust_foreign_attributes(token);

        (void)insert_foreign_element(token, Namespace::SVG);

        if (token.is_self_closing()) {
            (void)m_stack_of_open_elements.pop();
            token.acknowledge_self_closing_flag_if_set();
        }
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::frame, HTML::TagNames::head, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))) {
        log_parse_error();
        return;
    }

    // Any other start tag
    if (token.is_start_tag()) {
        reconstruct_the_active_formatting_elements();
        (void)insert_html_element(token);
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
                    log_parse_error();
                }
                while (&current_node() != node) {
                    (void)m_stack_of_open_elements.pop();
                }
                (void)m_stack_of_open_elements.pop();
                break;
            }
            if (is_special_tag(node->local_name(), node->namespace_())) {
                log_parse_error();
                return;
            }
        }
        return;
    }
}

void HTMLParser::adjust_mathml_attributes(HTMLToken& token)
{
    token.adjust_attribute_name("definitionurl", "definitionURL");
}

void HTMLParser::adjust_svg_tag_names(HTMLToken& token)
{
    token.adjust_tag_name("altglyph", "altGlyph");
    token.adjust_tag_name("altglyphdef", "altGlyphDef");
    token.adjust_tag_name("altglyphitem", "altGlyphItem");
    token.adjust_tag_name("animatecolor", "animateColor");
    token.adjust_tag_name("animatemotion", "animateMotion");
    token.adjust_tag_name("animatetransform", "animateTransform");
    token.adjust_tag_name("clippath", "clipPath");
    token.adjust_tag_name("feblend", "feBlend");
    token.adjust_tag_name("fecolormatrix", "feColorMatrix");
    token.adjust_tag_name("fecomponenttransfer", "feComponentTransfer");
    token.adjust_tag_name("fecomposite", "feComposite");
    token.adjust_tag_name("feconvolvematrix", "feConvolveMatrix");
    token.adjust_tag_name("fediffuselighting", "feDiffuseLighting");
    token.adjust_tag_name("fedisplacementmap", "feDisplacementMap");
    token.adjust_tag_name("fedistantlight", "feDistantLight");
    token.adjust_tag_name("fedropshadow", "feDropShadow");
    token.adjust_tag_name("feflood", "feFlood");
    token.adjust_tag_name("fefunca", "feFuncA");
    token.adjust_tag_name("fefuncb", "feFuncB");
    token.adjust_tag_name("fefuncg", "feFuncG");
    token.adjust_tag_name("fefuncr", "feFuncR");
    token.adjust_tag_name("fegaussianblur", "feGaussianBlur");
    token.adjust_tag_name("feimage", "feImage");
    token.adjust_tag_name("femerge", "feMerge");
    token.adjust_tag_name("femergenode", "feMergeNode");
    token.adjust_tag_name("femorphology", "feMorphology");
    token.adjust_tag_name("feoffset", "feOffset");
    token.adjust_tag_name("fepointlight", "fePointLight");
    token.adjust_tag_name("fespecularlighting", "feSpecularLighting");
    token.adjust_tag_name("fespotlight", "feSpotlight");
    token.adjust_tag_name("glyphref", "glyphRef");
    token.adjust_tag_name("lineargradient", "linearGradient");
    token.adjust_tag_name("radialgradient", "radialGradient");
    token.adjust_tag_name("textpath", "textPath");
}

void HTMLParser::adjust_svg_attributes(HTMLToken& token)
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

void HTMLParser::adjust_foreign_attributes(HTMLToken& token)
{
    token.adjust_foreign_attribute("xlink:actuate", "xlink", "actuate", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:arcrole", "xlink", "arcrole", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:href", "xlink", "href", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:role", "xlink", "role", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:show", "xlink", "show", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:title", "xlink", "title", Namespace::XLink);
    token.adjust_foreign_attribute("xlink:type", "xlink", "type", Namespace::XLink);

    token.adjust_foreign_attribute("xml:lang", "xml", "lang", Namespace::XML);
    token.adjust_foreign_attribute("xml:space", "xml", "space", Namespace::XML);

    token.adjust_foreign_attribute("xmlns", "", "xmlns", Namespace::XMLNS);
    token.adjust_foreign_attribute("xmlns:xlink", "xmlns", "xlink", Namespace::XMLNS);
}

void HTMLParser::increment_script_nesting_level()
{
    ++m_script_nesting_level;
}

void HTMLParser::decrement_script_nesting_level()
{
    VERIFY(m_script_nesting_level);
    --m_script_nesting_level;
}

// https://html.spec.whatwg.org/multipage/parsing.html#parsing-main-incdata
void HTMLParser::handle_text(HTMLToken& token)
{
    if (token.is_character()) {
        insert_character(token.code_point());
        return;
    }
    if (token.is_end_of_file()) {
        log_parse_error();
        if (current_node().local_name() == HTML::TagNames::script)
            verify_cast<HTMLScriptElement>(current_node()).set_already_started({}, true);
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::script) {
        // Make sure the <script> element has up-to-date text content before preparing the script.
        flush_character_insertions();

        NonnullRefPtr<HTMLScriptElement> script = verify_cast<HTMLScriptElement>(current_node());
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        // Let the old insertion point have the same value as the current insertion point.
        m_tokenizer.store_insertion_point();
        // Let the insertion point be just before the next input character.
        m_tokenizer.update_insertion_point();
        increment_script_nesting_level();
        // FIXME: Check if active speculative HTML parser is null.
        script->prepare_script({});
        decrement_script_nesting_level();
        if (script_nesting_level() == 0)
            m_parser_pause_flag = false;
        // Let the insertion point have the value of the old insertion point.
        m_tokenizer.restore_insertion_point();

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

                // If the parser's Document has a style sheet that is blocking scripts
                // or the script's "ready to be parser-executed" flag is not set:
                // spin the event loop until the parser's Document has no style sheet
                // that is blocking scripts and the script's "ready to be parser-executed"
                // flag is set.
                if (m_document->has_a_style_sheet_that_is_blocking_scripts() || !script->is_ready_to_be_parser_executed()) {
                    main_thread_event_loop().spin_until([&] {
                        return !m_document->has_a_style_sheet_that_is_blocking_scripts() && script->is_ready_to_be_parser_executed();
                    });
                }

                if (the_script->failed_to_load())
                    return;

                VERIFY(the_script->is_ready_to_be_parser_executed());

                if (m_aborted)
                    return;

                m_tokenizer.set_blocked(false);

                // Let the insertion point be just before the next input character.
                m_tokenizer.update_insertion_point();

                VERIFY(script_nesting_level() == 0);
                increment_script_nesting_level();

                the_script->execute_script();

                decrement_script_nesting_level();
                VERIFY(script_nesting_level() == 0);
                m_parser_pause_flag = false;

                // Let the insertion point be undefined again.
                m_tokenizer.undefine_insertion_point();
            }
        }
        return;
    }

    if (token.is_end_tag()) {
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = m_original_insertion_mode;
        return;
    }
    TODO();
}

void HTMLParser::clear_the_stack_back_to_a_table_context()
{
    while (!current_node().local_name().is_one_of(HTML::TagNames::table, HTML::TagNames::template_, HTML::TagNames::html))
        (void)m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        VERIFY(m_parsing_fragment);
}

void HTMLParser::clear_the_stack_back_to_a_table_row_context()
{
    while (!current_node().local_name().is_one_of(HTML::TagNames::tr, HTML::TagNames::template_, HTML::TagNames::html))
        (void)m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        VERIFY(m_parsing_fragment);
}

void HTMLParser::clear_the_stack_back_to_a_table_body_context()
{
    while (!current_node().local_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::template_, HTML::TagNames::html))
        (void)m_stack_of_open_elements.pop();

    if (current_node().local_name() == HTML::TagNames::html)
        VERIFY(m_parsing_fragment);
}

void HTMLParser::handle_in_row(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::th, HTML::TagNames::td)) {
        clear_the_stack_back_to_a_table_row_context();
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InCell;
        m_list_of_active_formatting_elements.add_marker();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::tr) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            log_parse_error();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            log_parse_error();
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            log_parse_error();
            return;
        }
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tr)) {
            return;
        }
        clear_the_stack_back_to_a_table_row_context();
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::td, HTML::TagNames::th)) {
        log_parse_error();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLParser::close_the_cell()
{
    generate_implied_end_tags();
    if (!current_node().local_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
        log_parse_error();
    }
    while (!current_node().local_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th))
        (void)m_stack_of_open_elements.pop();
    (void)m_stack_of_open_elements.pop();
    m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
    m_insertion_mode = InsertionMode::InRow;
}

void HTMLParser::handle_in_cell(HTMLToken& token)
{
    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            log_parse_error();
            return;
        }
        generate_implied_end_tags();

        if (current_node().local_name() != token.tag_name()) {
            log_parse_error();
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(token.tag_name());

        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InRow;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::td) && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::th)) {
            VERIFY(m_parsing_fragment);
            log_parse_error();
            return;
        }
        close_the_cell();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html)) {
        log_parse_error();
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            log_parse_error();
            return;
        }
        close_the_cell();
        // Reprocess the token.
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLParser::handle_in_table_text(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            log_parse_error();
            return;
        }

        m_pending_table_character_tokens.append(move(token));
        return;
    }

    for (auto& pending_token : m_pending_table_character_tokens) {
        VERIFY(pending_token.is_character());
        if (!pending_token.is_parser_whitespace()) {
            // If any of the tokens in the pending table character tokens list
            // are character tokens that are not ASCII whitespace, then this is a parse error:
            // reprocess the character tokens in the pending table character tokens list using
            // the rules given in the "anything else" entry in the "in table" insertion mode.
            log_parse_error();
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

void HTMLParser::handle_in_table_body(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::tr) {
        clear_the_stack_back_to_a_table_body_context();
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InRow;
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::th, HTML::TagNames::td)) {
        log_parse_error();
        clear_the_stack_back_to_a_table_body_context();
        (void)insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::tr));
        m_insertion_mode = InsertionMode::InRow;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name())) {
            log_parse_error();
            return;
        }
        clear_the_stack_back_to_a_table_body_context();
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {

        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tbody)
            && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::thead)
            && !m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::tfoot)) {
            log_parse_error();
            return;
        }

        clear_the_stack_back_to_a_table_body_context();
        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(InsertionMode::InTable, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::td, HTML::TagNames::th, HTML::TagNames::tr)) {
        log_parse_error();
        return;
    }

    process_using_the_rules_for(InsertionMode::InTable, token);
}

void HTMLParser::handle_in_table(HTMLToken& token)
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
        log_parse_error();
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::caption) {
        clear_the_stack_back_to_a_table_context();
        m_list_of_active_formatting_elements.add_marker();
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InCaption;
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::colgroup) {
        clear_the_stack_back_to_a_table_context();
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InColumnGroup;
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::col) {
        clear_the_stack_back_to_a_table_context();
        (void)insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::colgroup));
        m_insertion_mode = InsertionMode::InColumnGroup;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead)) {
        clear_the_stack_back_to_a_table_context();
        (void)insert_html_element(token);
        m_insertion_mode = InsertionMode::InTableBody;
        return;
    }
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::td, HTML::TagNames::th, HTML::TagNames::tr)) {
        clear_the_stack_back_to_a_table_context();
        (void)insert_html_element(HTMLToken::make_start_tag(HTML::TagNames::tbody));
        m_insertion_mode = InsertionMode::InTableBody;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::table) {
        log_parse_error();
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::table))
            return;

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::table);

        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::table) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::table)) {
            log_parse_error();
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::table);

        reset_the_insertion_mode_appropriately();
        return;
    }
    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        log_parse_error();
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

        log_parse_error();
        (void)insert_html_element(token);

        // FIXME: Is this the correct interpretation of "Pop that input element off the stack of open elements."?
        //        Because this wording is the first time it's seen in the spec.
        //        Other times it's worded as: "Immediately pop the current node off the stack of open elements."
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }
    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::form) {
        log_parse_error();
        if (m_form_element || m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            return;
        }

        m_form_element = verify_cast<HTMLFormElement>(*insert_html_element(token));

        // FIXME: See previous FIXME, as this is the same situation but for form.
        (void)m_stack_of_open_elements.pop();
        return;
    }
    if (token.is_end_of_file()) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

AnythingElse:
    log_parse_error();
    m_foster_parenting = true;
    process_using_the_rules_for(InsertionMode::InBody, token);
    m_foster_parenting = false;
}

void HTMLParser::handle_in_select_in_table(HTMLToken& token)
{
    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::td, HTML::TagNames::th)) {
        log_parse_error();
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::table, HTML::TagNames::tbody, HTML::TagNames::tfoot, HTML::TagNames::thead, HTML::TagNames::tr, HTML::TagNames::td, HTML::TagNames::th)) {
        log_parse_error();

        if (!m_stack_of_open_elements.has_in_table_scope(token.tag_name()))
            return;

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    process_using_the_rules_for(InsertionMode::InSelect, token);
}

void HTMLParser::handle_in_select(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            log_parse_error();
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::option) {
        if (current_node().local_name() == HTML::TagNames::option) {
            (void)m_stack_of_open_elements.pop();
        }
        (void)insert_html_element(token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::optgroup) {
        if (current_node().local_name() == HTML::TagNames::option) {
            (void)m_stack_of_open_elements.pop();
        }
        if (current_node().local_name() == HTML::TagNames::optgroup) {
            (void)m_stack_of_open_elements.pop();
        }
        (void)insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::optgroup) {
        if (current_node().local_name() == HTML::TagNames::option && node_before_current_node().local_name() == HTML::TagNames::optgroup)
            (void)m_stack_of_open_elements.pop();

        if (current_node().local_name() == HTML::TagNames::optgroup) {
            (void)m_stack_of_open_elements.pop();
        } else {
            log_parse_error();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::option) {
        if (current_node().local_name() == HTML::TagNames::option) {
            (void)m_stack_of_open_elements.pop();
        } else {
            log_parse_error();
            return;
        }
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::select) {
        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            VERIFY(m_parsing_fragment);
            log_parse_error();
            return;
        }
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::select) {
        log_parse_error();

        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            VERIFY(m_parsing_fragment);
            return;
        }

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::select);
        reset_the_insertion_mode_appropriately();
        return;
    }

    if (token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::input, HTML::TagNames::keygen, HTML::TagNames::textarea)) {
        log_parse_error();

        if (!m_stack_of_open_elements.has_in_select_scope(HTML::TagNames::select)) {
            VERIFY(m_parsing_fragment);
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

    log_parse_error();
}

void HTMLParser::handle_in_caption(HTMLToken& token)
{
    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::caption) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::caption)) {
            VERIFY(m_parsing_fragment);
            log_parse_error();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::caption)
            log_parse_error();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::caption);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::caption, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr))
        || (token.is_end_tag() && token.tag_name() == HTML::TagNames::table)) {
        if (!m_stack_of_open_elements.has_in_table_scope(HTML::TagNames::caption)) {
            VERIFY(m_parsing_fragment);
            log_parse_error();
            return;
        }

        generate_implied_end_tags();

        if (current_node().local_name() != HTML::TagNames::caption)
            log_parse_error();

        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::caption);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();

        m_insertion_mode = InsertionMode::InTable;
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    if (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::body, HTML::TagNames::col, HTML::TagNames::colgroup, HTML::TagNames::html, HTML::TagNames::tbody, HTML::TagNames::td, HTML::TagNames::tfoot, HTML::TagNames::th, HTML::TagNames::thead, HTML::TagNames::tr)) {
        log_parse_error();
        return;
    }

    process_using_the_rules_for(InsertionMode::InBody, token);
}

void HTMLParser::handle_in_column_group(HTMLToken& token)
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::col) {
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::colgroup) {
        if (current_node().local_name() != HTML::TagNames::colgroup) {
            log_parse_error();
            return;
        }

        (void)m_stack_of_open_elements.pop();
        m_insertion_mode = InsertionMode::InTable;
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::col) {
        log_parse_error();
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
        log_parse_error();
        return;
    }

    (void)m_stack_of_open_elements.pop();
    m_insertion_mode = InsertionMode::InTable;
    process_using_the_rules_for(m_insertion_mode, token);
}

void HTMLParser::handle_in_template(HTMLToken& token)
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
        log_parse_error();
        return;
    }

    if (token.is_end_of_file()) {
        if (!m_stack_of_open_elements.contains(HTML::TagNames::template_)) {
            VERIFY(m_parsing_fragment);
            stop_parsing();
            return;
        }

        log_parse_error();
        m_stack_of_open_elements.pop_until_an_element_with_tag_name_has_been_popped(HTML::TagNames::template_);
        m_list_of_active_formatting_elements.clear_up_to_the_last_marker();
        m_stack_of_template_insertion_modes.take_last();
        reset_the_insertion_mode_appropriately();
        process_using_the_rules_for(m_insertion_mode, token);
    }
}

void HTMLParser::handle_in_frameset(HTMLToken& token)
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
        log_parse_error();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::html) {
        process_using_the_rules_for(InsertionMode::InBody, token);
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frameset) {
        (void)insert_html_element(token);
        return;
    }

    if (token.is_end_tag() && token.tag_name() == HTML::TagNames::frameset) {
        // FIXME: If the current node is the root html element, then this is a parse error; ignore the token. (fragment case)

        (void)m_stack_of_open_elements.pop();

        if (!m_parsing_fragment && current_node().local_name() != HTML::TagNames::frameset) {
            m_insertion_mode = InsertionMode::AfterFrameset;
        }
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::frame) {
        (void)insert_html_element(token);
        (void)m_stack_of_open_elements.pop();
        token.acknowledge_self_closing_flag_if_set();
        return;
    }

    if (token.is_start_tag() && token.tag_name() == HTML::TagNames::noframes) {
        process_using_the_rules_for(InsertionMode::InHead, token);
        return;
    }

    if (token.is_end_of_file()) {
        // FIXME: If the current node is not the root html element, then this is a parse error.

        stop_parsing();
        return;
    }

    log_parse_error();
}

void HTMLParser::handle_after_frameset(HTMLToken& token)
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
        log_parse_error();
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

    log_parse_error();
}

void HTMLParser::handle_after_after_frameset(HTMLToken& token)
{
    if (token.is_comment()) {
        auto comment = adopt_ref(*new DOM::Comment(document(), token.comment()));
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

    log_parse_error();
}

void HTMLParser::process_using_the_rules_for_foreign_content(HTMLToken& token)
{
    if (token.is_character()) {
        if (token.code_point() == 0) {
            log_parse_error();
            insert_character(0xFFFD);
            return;
        }
        if (token.is_parser_whitespace()) {
            insert_character(token.code_point());
            return;
        }
        insert_character(token.code_point());
        m_frameset_ok = false;
        return;
    }

    if (token.is_comment()) {
        insert_comment(token);
        return;
    }

    if (token.is_doctype()) {
        log_parse_error();
        return;
    }

    if ((token.is_start_tag() && token.tag_name().is_one_of(HTML::TagNames::b, HTML::TagNames::big, HTML::TagNames::blockquote, HTML::TagNames::body, HTML::TagNames::br, HTML::TagNames::center, HTML::TagNames::code, HTML::TagNames::dd, HTML::TagNames::div, HTML::TagNames::dl, HTML::TagNames::dt, HTML::TagNames::em, HTML::TagNames::embed, HTML::TagNames::h1, HTML::TagNames::h2, HTML::TagNames::h3, HTML::TagNames::h4, HTML::TagNames::h5, HTML::TagNames::h6, HTML::TagNames::head, HTML::TagNames::hr, HTML::TagNames::i, HTML::TagNames::img, HTML::TagNames::li, HTML::TagNames::listing, HTML::TagNames::menu, HTML::TagNames::meta, HTML::TagNames::nobr, HTML::TagNames::ol, HTML::TagNames::p, HTML::TagNames::pre, HTML::TagNames::ruby, HTML::TagNames::s, HTML::TagNames::small, HTML::TagNames::span, HTML::TagNames::strong, HTML::TagNames::strike, HTML::TagNames::sub, HTML::TagNames::sup, HTML::TagNames::table, HTML::TagNames::tt, HTML::TagNames::u, HTML::TagNames::ul, HTML::TagNames::var))
        || (token.is_start_tag() && token.tag_name() == HTML::TagNames::font && (token.has_attribute(HTML::AttributeNames::color) || token.has_attribute(HTML::AttributeNames::face) || token.has_attribute(HTML::AttributeNames::size)))
        || (token.is_end_tag() && token.tag_name().is_one_of(HTML::TagNames::br, HTML::TagNames::p))) {
        log_parse_error();

        // While the current node is not a MathML text integration point, an HTML integration point, or an element in the HTML namespace, pop elements from the stack of open elements.
        while (!is_mathml_text_integration_point(current_node())
            && !is_html_integration_point(current_node())
            && current_node().namespace_() != Namespace::HTML) {
            (void)m_stack_of_open_elements.pop();
        }

        // Reprocess the token according to the rules given in the section corresponding to the current insertion mode in HTML content.
        process_using_the_rules_for(m_insertion_mode, token);
        return;
    }

    // Any other start tag
    if (token.is_start_tag()) {
        if (adjusted_current_node().namespace_() == Namespace::MathML) {
            adjust_mathml_attributes(token);
        } else if (adjusted_current_node().namespace_() == Namespace::SVG) {
            adjust_svg_tag_names(token);
            adjust_svg_attributes(token);
        }

        adjust_foreign_attributes(token);
        (void)insert_foreign_element(token, adjusted_current_node().namespace_());

        if (token.is_self_closing()) {
            if (token.tag_name() == SVG::TagNames::script && current_node().namespace_() == Namespace::SVG) {
                token.acknowledge_self_closing_flag_if_set();
                goto ScriptEndTag;
            }

            (void)m_stack_of_open_elements.pop();
            token.acknowledge_self_closing_flag_if_set();
        }

        return;
    }

    if (token.is_end_tag() && current_node().namespace_() == Namespace::SVG && current_node().tag_name() == SVG::TagNames::script) {
    ScriptEndTag:
        // Pop the current node off the stack of open elements.
        (void)m_stack_of_open_elements.pop();
        // Let the old insertion point have the same value as the current insertion point.
        m_tokenizer.store_insertion_point();
        // Let the insertion point be just before the next input character.
        m_tokenizer.update_insertion_point();
        // Increment the parser's script nesting level by one.
        increment_script_nesting_level();
        // Set the parser pause flag to true.
        m_parser_pause_flag = true;
        // FIXME: Implement SVG script parsing.
        TODO();
        // Decrement the parser's script nesting level by one.
        decrement_script_nesting_level();
        // If the parser's script nesting level is zero, then set the parser pause flag to false.
        if (script_nesting_level() == 0)
            m_parser_pause_flag = false;

        // Let the insertion point have the value of the old insertion point.
        m_tokenizer.restore_insertion_point();
    }

    if (token.is_end_tag()) {
        RefPtr<DOM::Element> node = current_node();
        // FIXME: Not sure if this is the correct to_lowercase, as the specification says "to ASCII lowercase"
        if (node->tag_name().to_lowercase() != token.tag_name())
            log_parse_error();
        for (ssize_t i = m_stack_of_open_elements.elements().size() - 1; i >= 0; --i) {
            if (node == m_stack_of_open_elements.first()) {
                VERIFY(m_parsing_fragment);
                return;
            }
            // FIXME: See the above FIXME
            if (node->tag_name().to_lowercase() == token.tag_name()) {
                while (current_node() != node)
                    (void)m_stack_of_open_elements.pop();
                (void)m_stack_of_open_elements.pop();
                return;
            }

            node = m_stack_of_open_elements.elements().at(i - 1);

            if (node->namespace_() != Namespace::HTML)
                continue;

            process_using_the_rules_for(m_insertion_mode, token);
            return;
        }
    }

    VERIFY_NOT_REACHED();
}

// https://html.spec.whatwg.org/multipage/parsing.html#reset-the-insertion-mode-appropriately
void HTMLParser::reset_the_insertion_mode_appropriately()
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
            if (!last) {
                for (ssize_t j = i; j > 0; --j) {
                    auto& ancestor = m_stack_of_open_elements.elements().at(j - 1);

                    if (is<HTMLTemplateElement>(ancestor))
                        break;

                    if (is<HTMLTableElement>(ancestor)) {
                        m_insertion_mode = InsertionMode::InSelectInTable;
                        return;
                    }
                }
            }

            m_insertion_mode = InsertionMode::InSelect;
            return;
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
            VERIFY(m_parsing_fragment);
            m_insertion_mode = InsertionMode::InFrameset;
            return;
        }

        if (node->local_name() == HTML::TagNames::html) {
            if (!m_head_element) {
                VERIFY(m_parsing_fragment);
                m_insertion_mode = InsertionMode::BeforeHead;
                return;
            }

            m_insertion_mode = InsertionMode::AfterHead;
            return;
        }
    }

    VERIFY(m_parsing_fragment);
    m_insertion_mode = InsertionMode::InBody;
}

const char* HTMLParser::insertion_mode_name() const
{
    switch (m_insertion_mode) {
#define __ENUMERATE_INSERTION_MODE(mode) \
    case InsertionMode::mode:            \
        return #mode;
        ENUMERATE_INSERTION_MODES
#undef __ENUMERATE_INSERTION_MODE
    }
    VERIFY_NOT_REACHED();
}

DOM::Document& HTMLParser::document()
{
    return *m_document;
}

NonnullRefPtrVector<DOM::Node> HTMLParser::parse_html_fragment(DOM::Element& context_element, StringView markup)
{
    auto temp_document = DOM::Document::create();
    auto parser = HTMLParser::create(*temp_document, markup, "utf-8");
    parser->m_context_element = context_element;
    parser->m_parsing_fragment = true;
    parser->document().set_quirks_mode(context_element.document().mode());

    if (context_element.local_name().is_one_of(HTML::TagNames::title, HTML::TagNames::textarea)) {
        parser->m_tokenizer.switch_to({}, HTMLTokenizer::State::RCDATA);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::style, HTML::TagNames::xmp, HTML::TagNames::iframe, HTML::TagNames::noembed, HTML::TagNames::noframes)) {
        parser->m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::script)) {
        parser->m_tokenizer.switch_to({}, HTMLTokenizer::State::ScriptData);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::noscript)) {
        if (context_element.document().is_scripting_enabled())
            parser->m_tokenizer.switch_to({}, HTMLTokenizer::State::RAWTEXT);
    } else if (context_element.local_name().is_one_of(HTML::TagNames::plaintext)) {
        parser->m_tokenizer.switch_to({}, HTMLTokenizer::State::PLAINTEXT);
    }

    auto root = create_element(context_element.document(), HTML::TagNames::html, Namespace::HTML);
    parser->document().append_child(root);
    parser->m_stack_of_open_elements.push(root);

    if (context_element.local_name() == HTML::TagNames::template_) {
        parser->m_stack_of_template_insertion_modes.append(InsertionMode::InTemplate);
    }

    // FIXME: Create a start tag token whose name is the local name of context and whose attributes are the attributes of context.

    parser->reset_the_insertion_mode_appropriately();

    for (auto* form_candidate = &context_element; form_candidate; form_candidate = form_candidate->parent_element()) {
        if (is<HTMLFormElement>(*form_candidate)) {
            parser->m_form_element = verify_cast<HTMLFormElement>(*form_candidate);
            break;
        }
    }

    parser->run(context_element.document().url());

    NonnullRefPtrVector<DOM::Node> children;
    while (RefPtr<DOM::Node> child = root->first_child()) {
        root->remove_child(*child);
        context_element.document().adopt_node(*child);
        children.append(*child);
    }
    return children;
}

NonnullRefPtr<HTMLParser> HTMLParser::create_for_scripting(DOM::Document& document)
{
    return adopt_ref(*new HTMLParser(document));
}

NonnullRefPtr<HTMLParser> HTMLParser::create_with_uncertain_encoding(DOM::Document& document, const ByteBuffer& input)
{
    if (document.has_encoding())
        return adopt_ref(*new HTMLParser(document, input, document.encoding().value()));
    auto encoding = run_encoding_sniffing_algorithm(document, input);
    dbgln("The encoding sniffing algorithm returned encoding '{}'", encoding);
    return adopt_ref(*new HTMLParser(document, input, encoding));
}

NonnullRefPtr<HTMLParser> HTMLParser::create(DOM::Document& document, StringView input, String const& encoding)
{
    return adopt_ref(*new HTMLParser(document, input, encoding));
}

// https://html.spec.whatwg.org/multipage/parsing.html#html-fragment-serialisation-algorithm
String HTMLParser::serialize_html_fragment(DOM::Node const& node)
{
    // The algorithm takes as input a DOM Element, Document, or DocumentFragment referred to as the node.
    VERIFY(node.is_element() || node.is_document() || node.is_document_fragment());
    NonnullRefPtr<DOM::Node> actual_node = node;

    if (is<DOM::Element>(node)) {
        auto& element = verify_cast<DOM::Element>(node);

        // 1. If the node serializes as void, then return the empty string.
        //    (NOTE: serializes as void is defined only on elements in the spec)
        if (element.serializes_as_void())
            return String::empty();

        // 3. If the node is a template element, then let the node instead be the template element's template contents (a DocumentFragment node).
        //    (NOTE: This is out of order of the spec to avoid another dynamic cast. The second step just creates a string builder, so it shouldn't matter)
        if (is<HTML::HTMLTemplateElement>(element))
            actual_node = verify_cast<HTML::HTMLTemplateElement>(element).content();
    }

    enum class AttributeMode {
        No,
        Yes,
    };

    auto escape_string = [](StringView string, AttributeMode attribute_mode) -> String {
        // https://html.spec.whatwg.org/multipage/parsing.html#escapingString
        StringBuilder builder;
        for (auto& ch : string) {
            // 1. Replace any occurrence of the "&" character by the string "&amp;".
            if (ch == '&')
                builder.append("&amp;");
            // 2. Replace any occurrences of the U+00A0 NO-BREAK SPACE character by the string "&nbsp;".
            else if (ch == '\xA0')
                builder.append("&nbsp;");
            // 3. If the algorithm was invoked in the attribute mode, replace any occurrences of the """ character by the string "&quot;".
            else if (ch == '"' && attribute_mode == AttributeMode::Yes)
                builder.append("&quot;");
            // 4. If the algorithm was not invoked in the attribute mode, replace any occurrences of the "<" character by the string "&lt;", and any occurrences of the ">" character by the string "&gt;".
            else if (ch == '<' && attribute_mode == AttributeMode::No)
                builder.append("&lt;");
            else if (ch == '>' && attribute_mode == AttributeMode::No)
                builder.append("&gt;");
            else
                builder.append(ch);
        }
        return builder.to_string();
    };

    // 2. Let s be a string, and initialize it to the empty string.
    StringBuilder builder;

    // 4. For each child node of the node, in tree order, run the following steps:
    actual_node->for_each_child([&](DOM::Node& current_node) {
        // 1. Let current node be the child node being processed.

        // 2. Append the appropriate string from the following list to s:

        if (is<DOM::Element>(current_node)) {
            // -> If current node is an Element
            auto& element = verify_cast<DOM::Element>(current_node);

            // 1. If current node is an element in the HTML namespace, the MathML namespace, or the SVG namespace, then let tagname be current node's local name.
            //    Otherwise, let tagname be current node's qualified name.
            String tag_name;

            if (element.namespace_().is_one_of(Namespace::HTML, Namespace::MathML, Namespace::SVG))
                tag_name = element.local_name();
            else
                tag_name = element.qualified_name();

            // 2. Append a U+003C LESS-THAN SIGN character (<), followed by tagname.
            builder.append('<');
            builder.append(tag_name);

            // FIXME: 3. If current node's is value is not null, and the element does not have an is attribute in its attribute list,
            //           then append the string " is="", followed by current node's is value escaped as described below in attribute mode,
            //           followed by a U+0022 QUOTATION MARK character (").

            // 4. For each attribute that the element has, append a U+0020 SPACE character, the attribute's serialized name as described below, a U+003D EQUALS SIGN character (=),
            //    a U+0022 QUOTATION MARK character ("), the attribute's value, escaped as described below in attribute mode, and a second U+0022 QUOTATION MARK character (").
            //    NOTE: The order of attributes is implementation-defined. The only constraint is that the order must be stable.
            element.for_each_attribute([&](auto& name, auto& value) {
                builder.append(' ');

                // An attribute's serialized name for the purposes of the previous paragraph must be determined as follows:

                // FIXME: -> If the attribute has no namespace:
                //              The attribute's serialized name is the attribute's local name.
                //           (We currently always do this)
                builder.append(name);

                // FIXME: -> If the attribute is in the XML namespace:
                //             The attribute's serialized name is the string "xml:" followed by the attribute's local name.

                // FIXME: -> If the attribute is in the XMLNS namespace and the attribute's local name is xmlns:
                //             The attribute's serialized name is the string "xmlns".

                // FIXME: -> If the attribute is in the XMLNS namespace and the attribute's local name is not xmlns:
                //             The attribute's serialized name is the string "xmlns:" followed by the attribute's local name.

                // FIXME: -> If the attribute is in the XLink namespace:
                //             The attribute's serialized name is the string "xlink:" followed by the attribute's local name.

                // FIXME: -> If the attribute is in some other namespace:
                //             The attribute's serialized name is the attribute's qualified name.

                builder.append("=\"");
                builder.append(escape_string(value, AttributeMode::Yes));
                builder.append('"');
            });

            // 5. Append a U+003E GREATER-THAN SIGN character (>).
            builder.append('>');

            // 6. If current node serializes as void, then continue on to the next child node at this point.
            if (element.serializes_as_void())
                return IterationDecision::Continue;

            // 7. Append the value of running the HTML fragment serialization algorithm on the current node element (thus recursing into this algorithm for that element),
            //    followed by a U+003C LESS-THAN SIGN character (<), a U+002F SOLIDUS character (/), tagname again, and finally a U+003E GREATER-THAN SIGN character (>).
            builder.append(serialize_html_fragment(element));
            builder.append("</");
            builder.append(tag_name);
            builder.append('>');

            return IterationDecision::Continue;
        }

        if (is<DOM::Text>(current_node)) {
            // -> If current node is a Text node
            auto& text_node = verify_cast<DOM::Text>(current_node);
            auto* parent = current_node.parent();

            if (is<DOM::Element>(parent)) {
                auto& parent_element = verify_cast<DOM::Element>(*parent);

                // 1. If the parent of current node is a style, script, xmp, iframe, noembed, noframes, or plaintext element,
                //    or if the parent of current node is a noscript element and scripting is enabled for the node, then append the value of current node's data IDL attribute literally.
                if (parent_element.local_name().is_one_of(HTML::TagNames::style, HTML::TagNames::script, HTML::TagNames::xmp, HTML::TagNames::iframe, HTML::TagNames::noembed, HTML::TagNames::noframes, HTML::TagNames::plaintext)
                    || (parent_element.local_name() == HTML::TagNames::noscript && !parent_element.is_scripting_disabled())) {
                    builder.append(text_node.data());
                    return IterationDecision::Continue;
                }
            }

            // 2. Otherwise, append the value of current node's data IDL attribute, escaped as described below.
            builder.append(escape_string(text_node.data(), AttributeMode::No));
            return IterationDecision::Continue;
        }

        if (is<DOM::Comment>(current_node)) {
            // -> If current node is a Comment
            auto& comment_node = verify_cast<DOM::Comment>(current_node);

            // 1. Append the literal string "<!--" (U+003C LESS-THAN SIGN, U+0021 EXCLAMATION MARK, U+002D HYPHEN-MINUS, U+002D HYPHEN-MINUS),
            //    followed by the value of current node's data IDL attribute, followed by the literal string "-->" (U+002D HYPHEN-MINUS, U+002D HYPHEN-MINUS, U+003E GREATER-THAN SIGN).
            builder.append("<!--");
            builder.append(comment_node.data());
            builder.append("-->");
            return IterationDecision::Continue;
        }

        if (is<DOM::ProcessingInstruction>(current_node)) {
            // -> If current node is a ProcessingInstruction
            auto& processing_instruction_node = verify_cast<DOM::ProcessingInstruction>(current_node);

            // 1. Append the literal string "<?" (U+003C LESS-THAN SIGN, U+003F QUESTION MARK), followed by the value of current node's target IDL attribute,
            //    followed by a single U+0020 SPACE character, followed by the value of current node's data IDL attribute, followed by a single U+003E GREATER-THAN SIGN character (>).
            builder.append("<?");
            builder.append(processing_instruction_node.target());
            builder.append(' ');
            builder.append(processing_instruction_node.data());
            builder.append('>');
            return IterationDecision::Continue;
        }

        if (is<DOM::DocumentType>(current_node)) {
            // -> If current node is a DocumentType
            auto& document_type_node = verify_cast<DOM::DocumentType>(current_node);

            // 1. Append the literal string "<!DOCTYPE" (U+003C LESS-THAN SIGN, U+0021 EXCLAMATION MARK, U+0044 LATIN CAPITAL LETTER D, U+004F LATIN CAPITAL LETTER O,
            //    U+0043 LATIN CAPITAL LETTER C, U+0054 LATIN CAPITAL LETTER T, U+0059 LATIN CAPITAL LETTER Y, U+0050 LATIN CAPITAL LETTER P, U+0045 LATIN CAPITAL LETTER E),
            //    followed by a space (U+0020 SPACE), followed by the value of current node's name IDL attribute, followed by the literal string ">" (U+003E GREATER-THAN SIGN).
            builder.append("<!DOCTYPE ");
            builder.append(document_type_node.name());
            builder.append('>');
            return IterationDecision::Continue;
        }

        return IterationDecision::Continue;
    });

    // 5. Return s.
    return builder.to_string();
}

}
