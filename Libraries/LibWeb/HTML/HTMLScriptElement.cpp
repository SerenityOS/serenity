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

#include <AK/StringBuilder.h>
#include <LibJS/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLScriptElement::HTMLScriptElement(DOM::Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLScriptElement::~HTMLScriptElement()
{
}

void HTMLScriptElement::set_parser_document(Badge<HTMLDocumentParser>, DOM::Document& document)
{
    m_parser_document = document.make_weak_ptr();
}

void HTMLScriptElement::set_non_blocking(Badge<HTMLDocumentParser>, bool non_blocking)
{
    m_non_blocking = non_blocking;
}

void HTMLScriptElement::execute_script()
{
    document().run_javascript(m_script_source);
}

void HTMLScriptElement::prepare_script(Badge<HTMLDocumentParser>)
{
    if (m_already_started)
        return;
    RefPtr<DOM::Document> parser_document = m_parser_document.ptr();
    m_parser_document = nullptr;

    if (parser_document && !has_attribute(HTML::AttributeNames::async)) {
        m_non_blocking = true;
    }

    auto source_text = child_text_content();
    if (!has_attribute(HTML::AttributeNames::src) && source_text.is_empty())
        return;

    if (!is_connected())
        return;

    // FIXME: Check the "type" and "language" attributes

    if (parser_document) {
        m_parser_document = parser_document->make_weak_ptr();
        m_non_blocking = false;
    }

    m_already_started = true;
    m_preparation_time_document = document().make_weak_ptr();

    if (parser_document && parser_document.ptr() != m_preparation_time_document.ptr()) {
        return;
    }

    // FIXME: Check if scripting is disabled, if so return
    // FIXME: Check the "nomodule" content attribute
    // FIXME: Check CSP
    // FIXME: Check "event" and "for" attributes
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
            // FIXME: Fire an "error" event at the element and return
            ASSERT_NOT_REACHED();
        }
        m_from_an_external_file = true;

        auto url = document().complete_url(src);
        if (!url.is_valid()) {
            // FIXME: Fire an "error" event at the element and return
            ASSERT_NOT_REACHED();
        }

        // FIXME: Check classic vs. module script type

        // FIXME: This load should be made asynchronous and the parser should spin an event loop etc.
        ResourceLoader::the().load_sync(
            url,
            [this, url](auto& data, auto&) {
                if (data.is_null()) {
                    dbg() << "HTMLScriptElement: Failed to load " << url;
                    return;
                }
                m_script_source = String::copy(data);
                script_became_ready();
            },
            [this](auto&) {
                m_failed_to_load = true;
            });
    } else {
        // FIXME: Check classic vs. module script type
        m_script_source = source_text;
        script_became_ready();
    }

    // FIXME: Check classic vs. module
    if (has_attribute(HTML::AttributeNames::src) && has_attribute(HTML::AttributeNames::defer) && m_parser_inserted && !has_attribute(HTML::AttributeNames::async)) {
        document().add_script_to_execute_when_parsing_has_finished({}, *this);
    }

    else if (has_attribute(HTML::AttributeNames::src) && m_parser_inserted && !has_attribute(HTML::AttributeNames::async)) {

        document().set_pending_parsing_blocking_script({}, this);
        when_the_script_is_ready([this] {
            m_ready_to_be_parser_executed = true;
        });
    }

    else if (has_attribute(HTML::AttributeNames::src) && !has_attribute(HTML::AttributeNames::async) && !m_non_blocking) {
        ASSERT_NOT_REACHED();
    }

    else if (has_attribute(HTML::AttributeNames::src)) {
        m_preparation_time_document->add_script_to_execute_as_soon_as_possible({}, *this);
    }

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

}
