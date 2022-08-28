/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

class HTMLScriptElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLScriptElement, HTMLElement);

public:
    virtual ~HTMLScriptElement() override;

    bool is_non_blocking() const { return m_non_blocking; }
    bool is_ready_to_be_parser_executed() const { return m_ready_to_be_parser_executed; }
    bool failed_to_load() const { return m_failed_to_load; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_parser_document(Badge<T>, DOM::Document& document) { m_parser_document = &document; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_non_blocking(Badge<T>, bool b) { m_non_blocking = b; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_already_started(Badge<T>, bool b) { m_already_started = b; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void prepare_script(Badge<T>) { prepare_script(); }

    void execute_script();

    bool is_parser_inserted() const { return !!m_parser_document; }

    virtual void inserted() override;

    // https://html.spec.whatwg.org/multipage/scripting.html#dom-script-supports
    static bool supports(String const& type)
    {
        return type.is_one_of("classic", "module");
    }

    void set_source_line_number(Badge<HTMLParser>, size_t source_line_number) { m_source_line_number = source_line_number; }

private:
    HTMLScriptElement(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Cell::Visitor&) override;

    void prepare_script();
    void script_became_ready();
    void when_the_script_is_ready(Function<void()>);
    void begin_delaying_document_load_event(DOM::Document&);

    JS::GCPtr<DOM::Document> m_parser_document;
    JS::GCPtr<DOM::Document> m_preparation_time_document;
    bool m_non_blocking { false };
    bool m_already_started { false };
    bool m_from_an_external_file { false };
    bool m_script_ready { false };
    bool m_ready_to_be_parser_executed { false };
    bool m_failed_to_load { false };

    enum class ScriptType {
        Classic,
        Module
    };

    ScriptType m_script_type { ScriptType::Classic };

    Function<void()> m_script_ready_callback;

    RefPtr<Script> m_script;

    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;

    size_t m_source_line_number { 1 };
};

}

WRAPPER_HACK(HTMLScriptElement, Web::HTML)
