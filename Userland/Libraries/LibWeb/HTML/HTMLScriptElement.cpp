/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    if (m_already_started) {
        dbgln("HTMLScriptElement: Refusing to run script because it has already started.");
        return;
    }

    RefPtr<DOM::Document> parser_document = m_parser_document.ptr();
    m_parser_document = nullptr;

    if (parser_document && !has_attribute(HTML::AttributeNames::async)) {
        m_non_blocking = true;
    }

    auto source_text = child_text_content();
    if (!has_attribute(HTML::AttributeNames::src) && source_text.is_empty()) {
        dbgln("HTMLScriptElement: Refusing to run empty script.");
        return;
    }

    if (!is_connected()) {
        dbgln("HTMLScriptElement: Refusing to run script because the element is not connected.");
        return;
    }

    String script_block_type;
    bool has_type = has_attribute(HTML::AttributeNames::type);
    bool has_language = has_attribute(HTML::AttributeNames::language);
    if ((has_type && attribute(HTML::AttributeNames::type).is_empty())
        || (!has_type && has_language && attribute(HTML::AttributeNames::language).is_empty())
        || (!has_type && !has_language)) {
        script_block_type = "text/javascript";
    } else if (has_type) {
        script_block_type = attribute(HTML::AttributeNames::type).trim_whitespace();
    } else if (!attribute(HTML::AttributeNames::language).is_empty()) {
        script_block_type = String::formatted("text/{}", attribute(HTML::AttributeNames::language));
    }

    if (is_javascript_mime_type_essence_match(script_block_type)) {
        m_script_type = ScriptType::Classic;
    } else if (script_block_type.equals_ignoring_case("module")) {
        m_script_type = ScriptType::Module;
    } else {
        dbgln("HTMLScriptElement: Refusing to run script because the type '{}' is not recognized.", script_block_type);
        return;
    }

    if (parser_document) {
        m_parser_document = *parser_document;
        m_non_blocking = false;
    }

    m_already_started = true;
    m_preparation_time_document = document();

    if (parser_document && parser_document.ptr() != m_preparation_time_document.ptr()) {
        dbgln("HTMLScriptElement: Refusing to run script because the parser document is not the same as the preparation time document.");
        return;
    }

    // FIXME: Check if scripting is disabled, if so return

    if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::nomodule)) {
        dbgln("HTMLScriptElement: Refusing to run classic script because it has the nomodule attribute.");
        return;
    }

    // FIXME: Check CSP

    if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::event) && has_attribute(HTML::AttributeNames::for_)) {
        auto for_ = attribute(HTML::AttributeNames::for_).trim_whitespace();
        auto event = attribute(HTML::AttributeNames::event).trim_whitespace();

        if (!for_.equals_ignoring_case("window")) {
            dbgln("HTMLScriptElement: Refusing to run classic script because the provided 'for' attribute is not equal to 'window'");
            return;
        }

        if (!event.equals_ignoring_case("onload") && !event.equals_ignoring_case("onload()")) {
            dbgln("HTMLScriptElement: Refusing to run classic script because the provided 'event' attribute is not equal to 'onload' or 'onload()'");
            return;
        }
    }

    // FIXME: Check "charset" attribute
    // FIXME: Check CORS
    // FIXME: Module script credentials mode
    // FIXME: Cryptographic nonce
    // FIXME: Check "integrity" attribute
    // FIXME: Check "referrerpolicy" attribute

    m_parser_inserted = !!m_parser_document;

    // FIXME: Check fetch options

    if (has_attribute(HTML::AttributeNames::src)) {
        auto src = attribute(HTML::AttributeNames::src);
        if (src.is_empty()) {
            dbgln("HTMLScriptElement: Refusing to run script because the src attribute is empty.");
            // FIXME: Queue a task to do this.
            dispatch_event(DOM::Event::create(HTML::EventNames::error));
            return;
        }
        m_from_an_external_file = true;

        auto url = document().complete_url(src);
        if (!url.is_valid()) {
            dbgln("HTMLScriptElement: Refusing to run script because the src URL '{}' is invalid.", url);
            // FIXME: Queue a task to do this.
            dispatch_event(DOM::Event::create(HTML::EventNames::error));
            return;
        }

        if (m_script_type == ScriptType::Classic) {
            // FIXME: This load should be made asynchronous and the parser should spin an event loop etc.
            m_script_filename = url.basename();
            ResourceLoader::the().load_sync(
                url,
                [this, url](auto data, auto&) {
                    if (data.is_null()) {
                        dbgln("HTMLScriptElement: Failed to load {}", url);
                        return;
                    }
                    m_script_source = String::copy(data);
                    script_became_ready();
                },
                [this](auto&) {
                    m_failed_to_load = true;
                });
        } else {
            TODO();
        }
    } else {
        if (m_script_type == ScriptType::Classic) {
            m_script_source = source_text;
            script_became_ready();
        } else {
            TODO();
        }
    }

    if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && has_attribute(HTML::AttributeNames::defer) && m_parser_inserted && !has_attribute(HTML::AttributeNames::async))
        || (m_script_type == ScriptType::Module && m_parser_inserted && !has_attribute(HTML::AttributeNames::async))) {
        document().add_script_to_execute_when_parsing_has_finished({}, *this);
        when_the_script_is_ready([this] {
            m_ready_to_be_parser_executed = true;
        });
    }

    else if (m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && m_parser_inserted && !has_attribute(HTML::AttributeNames::async)) {
        document().set_pending_parsing_blocking_script({}, this);
        when_the_script_is_ready([this] {
            m_ready_to_be_parser_executed = true;
        });
    }

    else if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src) && !has_attribute(HTML::AttributeNames::async) && !m_non_blocking)
        || (m_script_type == ScriptType::Module && !has_attribute(HTML::AttributeNames::async) && !m_non_blocking)) {
        m_preparation_time_document->add_script_to_execute_as_soon_as_possible({}, *this);

        // FIXME: When the script is ready, run the following steps:
        //
        // If the element is not now the first element in the list of scripts
        // that will execute in order as soon as possible to which it was added above,
        // then mark the element as ready but return without executing the script yet.
        //
        // Execution: Execute the script block corresponding to the first script element
        // in this list of scripts that will execute in order as soon as possible.
        //
        // Remove the first element from this list of scripts that will execute in order
        // as soon as possible.
        //
        // If this list of scripts that will execute in order as soon as possible is still
        // not empty and the first entry has already been marked as ready, then jump back
        // to the step labeled execution.
    }

    else if ((m_script_type == ScriptType::Classic && has_attribute(HTML::AttributeNames::src)) || m_script_type == ScriptType::Module) {
        // FIXME: This should add to a set, not a list.
        m_preparation_time_document->add_script_to_execute_as_soon_as_possible({}, *this);
        // FIXME: When the script is ready, execute the script block and then remove the element
        //        from the set of scripts that will execute as soon as possible.
    }

    // FIXME: If the element does not have a src attribute, and the element is "parser-inserted",
    //        and either the parser that created the script is an XML parser or it's an HTML parser
    //        whose script nesting level is not greater than one, and the element's parser document
    //        has a style sheet that is blocking scripts:
    //        The element is the pending parsing-blocking script of its parser document.
    //        (There can only be one such script per Document at a time.)
    //        Set the element's "ready to be parser-executed" flag. The parser will handle executing the script.

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

void HTMLScriptElement::inserted_into(Node& parent)
{
    // FIXME: It would be nice to have a notification for "node became connected"
    if (is_connected()) {
        prepare_script();
    }
    HTMLElement::inserted_into(parent);
}

}
