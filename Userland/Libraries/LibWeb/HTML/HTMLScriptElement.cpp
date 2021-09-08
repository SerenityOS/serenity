/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <LibJS/Parser.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLScriptElement::HTMLScriptElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_script_filename("(document)")
{
}

HTMLScriptElement::~HTMLScriptElement()
{
}

void HTMLScriptElement::set_parser_document(Badge<HTMLDocumentParser>, DOM::Document& document)
{
    m_parser_document = document;
}

void HTMLScriptElement::set_non_blocking(Badge<HTMLDocumentParser>, bool non_blocking)
{
    m_non_blocking = non_blocking;
}

void HTMLScriptElement::execute_script()
{
    if (m_preparation_time_document.ptr() != &document()) {
        dbgln("HTMLScriptElement: Refusing to run script because the preparation time document is not the same as the node document.");
        return;
    }

    if (m_script_source.is_null()) {
        dbgln("HTMLScriptElement: Refusing to run script because the script source is null.");
        dispatch_event(DOM::Event::create(HTML::EventNames::error));
        return;
    }

    bool incremented_destructive_writes_counter = false;

    if (m_from_an_external_file || m_script_type == ScriptType::Module) {
        document().increment_ignore_destructive_writes_counter();
        incremented_destructive_writes_counter = true;
    }

    if (m_script_type == ScriptType::Classic) {
        auto old_current_script = document().current_script();
        if (!is<DOM::ShadowRoot>(root()))
            document().set_current_script({}, this);
        else
            document().set_current_script({}, nullptr);

        if (m_from_an_external_file)
            dbgln_if(HTML_SCRIPT_DEBUG, "HTMLScriptElement: Running script {}", attribute(HTML::AttributeNames::src));
        else
            dbgln_if(HTML_SCRIPT_DEBUG, "HTMLScriptElement: Running inline script");

        document().run_javascript(m_script_source, m_script_filename);

        document().set_current_script({}, old_current_script);
    } else {
        VERIFY(!document().current_script());
        TODO();
    }

    if (incremented_destructive_writes_counter)
        document().decrement_ignore_destructive_writes_counter();

    if (m_from_an_external_file)
        dispatch_event(DOM::Event::create(HTML::EventNames::load));
}

// https://mimesniff.spec.whatwg.org/#javascript-mime-type-essence-match
static bool is_javascript_mime_type_essence_match(const String& string)
{
    auto lowercase_string = string.to_lowercase();
    return lowercase_string.is_one_of("application/ecmascript", "application/javascript", "application/x-ecmascript", "application/x-javascript", "text/ecmascript", "text/javascript", "text/javascript1.0", "text/javascript1.1", "text/javascript1.2", "text/javascript1.3", "text/javascript1.4", "text/javascript1.5", "text/jscript", "text/livescript", "text/x-ecmascript", "text/x-javascript");
}

// https://html.spec.whatwg.org/multipage/scripting.html#prepare-a-script
void HTMLScriptElement::prepare_script()
{
    // 1. If the script element is marked as having "already started", then return. The script is not executed.
    if (m_already_started) {
        dbgln("HTMLScriptElement: Refusing to run script because it has already started.");
        return;
    }

    // 2. Let parser document be the element's parser document.
    RefPtr<DOM::Document> parser_document = m_parser_document.ptr();

    // 3. Set the element's parser document to null.
    m_parser_document = nullptr;

    // 4. If parser document is non-null and the element does not have an async attribute, then set the element's "non-blocking" flag to true.
    if (parser_document && !has_attribute(HTML::AttributeNames::async)) {
        m_non_blocking = true;
    }

    // 5. Let source text be the element's child text content.
    auto source_text = child_text_content();

    // 6. If the element has no src attribute, and source text is the empty string, then return. The script is not executed.
    if (!has_attribute(HTML::AttributeNames::src) && source_text.is_empty()) {
        dbgln("HTMLScriptElement: Refusing to run empty script.");
        return;
    }

    // 7. If the element is not connected, then return. The script is not executed.
    if (!is_connected()) {
        dbgln("HTMLScriptElement: Refusing to run script because the element is not connected.");
        return;
    }

    // 8. If either:
    //    - the script element has a type attribute and its value is the empty string, or
    //    - the script element has no type attribute but it has a language attribute and that attribute's value is the empty string, or
    //    - the script element has neither a type attribute nor a language attribute, then
    //        ...let the script block's type string for this script element be "text/javascript".
    String script_block_type;
    bool has_type = has_attribute(HTML::AttributeNames::type);
    bool has_language = has_attribute(HTML::AttributeNames::language);
    if ((has_type && attribute(HTML::AttributeNames::type).is_empty())
        || (!has_type && has_language && attribute(HTML::AttributeNames::language).is_empty())
        || (!has_type && !has_language)) {
        script_block_type = "text/javascript";
    } else if (has_type) {
        // Otherwise, if the script element has a type attribute, let the script block's type string for this script element be the value of that attribute.
        script_block_type = attribute(HTML::AttributeNames::type);
    } else if (!attribute(HTML::AttributeNames::language).is_empty()) {
        // Otherwise, the element has a non-empty language attribute; let the script block's type string for this script element be the concatenation of the string "text/" followed by the value of the language attribute.
        script_block_type = String::formatted("text/{}", attribute(HTML::AttributeNames::language));
    }

    // Determine the script's type as follows:
    if (is_javascript_mime_type_essence_match(script_block_type.trim_whitespace())) {
        // - If the script block's type string with leading and trailing ASCII whitespace stripped is a JavaScript MIME type essence match, the script's type is "classic".
        m_script_type = ScriptType::Classic;
    } else if (script_block_type.equals_ignoring_case("module")) {
        // - If the script block's type string is an ASCII case-insensitive match for the string "module", the script's type is "module".
        m_script_type = ScriptType::Module;
    } else {
        // - If neither of the above conditions are true, then return. No script is executed.
        dbgln("HTMLScriptElement: Refusing to run script because the type '{}' is not recognized.", script_block_type);
        return;
    }

    // 9. If parser document is non-null, then set the element's parser document back to parser document and set the element's "non-blocking" flag to false.
    if (parser_document) {
        m_parser_document = *parser_document;
        m_non_blocking = false;
    }

    // 10. Set the element's "already started" flag.
    m_already_started = true;

    // 11. Set the element's preparation-time document to its node document.
    m_preparation_time_document = document();

    // 12. If parser document is non-null, and parser document is not equal to the element's preparation-time document, then return.
    if (parser_document && parser_document.ptr() != m_preparation_time_document.ptr()) {
        dbgln("HTMLScriptElement: Refusing to run script because the parser document is not the same as the preparation time document.");
        return;
    }

    // 13. If scripting is disabled for the script element, then return. The script is not executed.
    if (is_scripting_disabled()) {
        dbgln("HTMLScriptElement: Refusing to run script because scripting is disabled.");
        return;
    }

    // 14. If the script element has a nomodule content attribute and the script's type is "classic", then return. The script is not executed.
    if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::nomodule)) {
        dbgln("HTMLScriptElement: Refusing to run classic script because it has the nomodule attribute.");
        return;
    }

    // FIXME: 15. If the script element does not have a src content attribute, and the `Should element's inline behavior be blocked by Content Security Policy?`
    //            algorithm returns "Blocked" when executed upon the script element, "script", and source text, then return. The script is not executed. [CSP]

    // 16. If the script element has an event attribute and a for attribute, and the script's type is "classic", then:
    if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::event) && has_attribute(HTML::AttributeNames::for_)) {
        // 1. Let for be the value of the for attribute.
        auto for_ = attribute(HTML::AttributeNames::for_);

        // 2. Let event be the value of the event attribute.
        auto event = attribute(HTML::AttributeNames::event);

        // 3. Strip leading and trailing ASCII whitespace from event and for.
        for_ = for_.trim_whitespace();
        event = event.trim_whitespace();

        // 4. If for is not an ASCII case-insensitive match for the string "window", then return. The script is not executed.
        if (!for_.equals_ignoring_case("window")) {
            dbgln("HTMLScriptElement: Refusing to run classic script because the provided 'for' attribute is not equal to 'window'");
            return;
        }

        // 5. If event is not an ASCII case-insensitive match for either the string "onload" or the string "onload()", then return. The script is not executed.
        if (!event.equals_ignoring_case("onload") && !event.equals_ignoring_case("onload()")) {
            dbgln("HTMLScriptElement: Refusing to run classic script because the provided 'event' attribute is not equal to 'onload' or 'onload()'");
            return;
        }
    }

    // FIXME: 17. If the script element has a charset attribute, then let encoding be the result of getting an encoding from the value of the charset attribute.
    //            If the script element does not have a charset attribute, or if getting an encoding failed, let encoding be the same as the encoding of the script element's node document.

    // FIXME: 18. Let classic script CORS setting be the current state of the element's crossorigin content attribute.

    // FIXME: 19. Let module script credentials mode be the CORS settings attribute credentials mode for the element's crossorigin content attribute.

    // FIXME: 20. Let cryptographic nonce be the element's [[CryptographicNonce]] internal slot's value.

    // FIXME: 21. If the script element has an integrity attribute, then let integrity metadata be that attribute's value.
    //            Otherwise, let integrity metadata be the empty string.

    // FIXME: 22. Let referrer policy be the current state of the element's referrerpolicy content attribute.

    // FIXME: 23. Let parser metadata be "parser-inserted" if the script element is "parser-inserted", and "not-parser-inserted" otherwise.

    // FIXME: 24. Let options be a script fetch options whose cryptographic nonce is cryptographic nonce, integrity metadata is integrity metadata, parser metadata is parser metadata,
    //            credentials mode is module script credentials mode, and referrer policy is referrer policy.

    // FIXME: 25. Let settings object be the element's node document's relevant settings object.

    // 26. If the element has a src content attribute, then:
    if (has_attribute(HTML::AttributeNames::src)) {
        // 1. Let src be the value of the element's src attribute.
        auto src = attribute(HTML::AttributeNames::src);
        // FIXME: 2. If src is the empty string, queue a task to fire an event named error at the element, and return.
        if (src.is_empty()) {
            dbgln("HTMLScriptElement: Refusing to run script because the src attribute is empty.");
            // FIXME: Queue a task to do this.
            dispatch_event(DOM::Event::create(HTML::EventNames::error));
            return;
        }
        // 3. Set the element's from an external file flag.
        m_from_an_external_file = true;

        // 4. Parse src relative to the element's node document.
        auto url = document().complete_url(src);
        // 5. If the previous step failed, queue a task to fire an event named error at the element, and return. Otherwise, let url be the resulting URL record.
        if (!url.is_valid()) {
            dbgln("HTMLScriptElement: Refusing to run script because the src URL '{}' is invalid.", url);
            // FIXME: Queue a task to do this.
            dispatch_event(DOM::Event::create(HTML::EventNames::error));
            return;
        }

        // 6. Switch on the script's type:
        if (m_script_type == ScriptType::Classic) {
            // -> "classic"
            //    Fetch a classic script given url, settings object, options, classic script CORS setting, and encoding.
            auto request = LoadRequest::create_for_url_on_page(url, document().page());

            // FIXME: This load should be made asynchronous and the parser should spin an event loop etc.
            m_script_filename = url.to_string();
            ResourceLoader::the().load_sync(
                request,
                [this, url](auto data, auto&, auto) {
                    if (data.is_null()) {
                        dbgln("HTMLScriptElement: Failed to load {}", url);
                        return;
                    }
                    m_script_source = String::copy(data);
                    script_became_ready();
                },
                [this](auto&, auto) {
                    m_failed_to_load = true;
                });
        } else if (m_script_type == ScriptType::Module) {
            // FIXME: -> "module"
            //        Fetch an external module script graph given url, settings object, and options.
        }
    } else {
        // 27. If the element does not have a src content attribute, run these substeps:

        // FIXME: 1. Let base URL be the script element's node document's document base URL.

        // 2. Switch on the script's type:
        if (m_script_type == ScriptType::Classic) {
            // -> "classic"
            // FIXME: 1. Let script be the result of creating a classic script using source text, settings object, base URL, and options.
            // FIXME: 2. Set the script's script to script.
            m_script_source = source_text;

            // 3. The script is ready.
            script_became_ready();
        } else if (m_script_type == ScriptType::Module) {
            // FIXME: -> "module"
            // 1. Fetch an inline module script graph, given source text, base URL, settings object, and options.
            //    When this asynchronously completes, set the script's script to the result. At that time, the script is ready.
            TODO();
        }
    }

    // 28. Then, follow the first of the following options that describes the situation:

    // -> If the script's type is "classic", and the element has a src attribute, and the element has a defer attribute, and the element is "parser-inserted", and the element does not have an async attribute
    // -> If the script's type is "module", and the element is "parser-inserted", and the element does not have an async attribute
    if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && has_attribute(HTML::AttributeNames::defer) && is_parser_inserted() && !has_attribute(HTML::AttributeNames::async))
        || (m_script_type == ScriptType::Module && is_parser_inserted() && !has_attribute(HTML::AttributeNames::async))) {
        // Add the element to the end of the list of scripts that will execute when the document has finished parsing associated with the Document of the parser that created the element.
        document().add_script_to_execute_when_parsing_has_finished({}, *this);

        // When the script is ready, set the element's "ready to be parser-executed" flag. The parser will handle executing the script.
        when_the_script_is_ready([this] {
            m_ready_to_be_parser_executed = true;
        });
    }

    // -> If the script's type is "classic", and the element has a src attribute, and the element is "parser-inserted", and the element does not have an async attribute
    else if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && is_parser_inserted() && !has_attribute(HTML::AttributeNames::async)) {
        // The element is the pending parsing-blocking script of the Document of the parser that created the element. (There can only be one such script per Document at a time.)
        document().set_pending_parsing_blocking_script({}, this);

        // When the script is ready, set the element's "ready to be parser-executed" flag. The parser will handle executing the script.
        when_the_script_is_ready([this] {
            m_ready_to_be_parser_executed = true;
        });
    }

    // -> If the script's type is "classic", and the element has a src attribute, and the element does not have an async attribute, and the element does not have the "non-blocking" flag set
    // -> If the script's type is "module", and the element does not have an async attribute, and the element does not have the "non-blocking" flag set
    else if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && !has_attribute(HTML::AttributeNames::async) && !m_non_blocking)
        || (m_script_type == ScriptType::Module && !has_attribute(HTML::AttributeNames::async) && !m_non_blocking)) {
        // Add the element to the end of the list of scripts that will execute in order as soon as possible associated with the element's preparation-time document.
        m_preparation_time_document->add_script_to_execute_as_soon_as_possible({}, *this);

        // FIXME: When the script is ready, run the following steps:

        // FIXME: 1. If the element is not now the first element in the list of scripts
        //           that will execute in order as soon as possible to which it was added above,
        //           then mark the element as ready but return without executing the script yet.

        // FIXME: 2. Execution: Execute the script block corresponding to the first script element
        //           in this list of scripts that will execute in order as soon as possible.
        //
        // FIXME: 3. Remove the first element from this list of scripts that will execute in order
        //           as soon as possible.
        //
        // FIXME: 4. If this list of scripts that will execute in order as soon as possible is still
        //           not empty and the first entry has already been marked as ready, then jump back
        //           to the step labeled execution.
    }

    // -> If the script's type is "classic", and the element has a src attribute
    // -> If the script's type is "module"
    else if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src)) || m_script_type == ScriptType::Module) {
        // The element must be added to the set of scripts that will execute as soon as possible of the element's preparation-time document.
        // FIXME: This should add to a set, not a list.
        m_preparation_time_document->add_script_to_execute_as_soon_as_possible({}, *this);
        // FIXME: When the script is ready, execute the script block and then remove the element
        //        from the set of scripts that will execute as soon as possible.
    }

    // FIXME: -> If the element does not have a src attribute, and the element is "parser-inserted",
    //           and either the parser that created the script is an XML parser or it's an HTML parser
    //           whose script nesting level is not greater than one, and the element's parser document
    //           has a style sheet that is blocking scripts:
    //
    // FIXME:      The element is the pending parsing-blocking script of its parser document.
    //             (There can only be one such script per Document at a time.)
    //             Set the element's "ready to be parser-executed" flag. The parser will handle executing the script.

    // -> Otherwise
    else {
        // Immediately execute the script block, even if other scripts are already executing.
        execute_script();
    }
}

void HTMLScriptElement::script_became_ready()
{
    m_script_ready = true;
    if (!m_script_ready_callback)
        return;
    m_script_ready_callback();
    m_script_ready_callback = nullptr;
}

void HTMLScriptElement::when_the_script_is_ready(Function<void()> callback)
{
    if (m_script_ready) {
        callback();
        return;
    }
    m_script_ready_callback = move(callback);
}

void HTMLScriptElement::inserted()
{
    if (!is_parser_inserted()) {
        // FIXME: Only do this if the element was previously not connected.
        if (is_connected()) {
            prepare_script();
        }
    }
    HTMLElement::inserted();
}

}
