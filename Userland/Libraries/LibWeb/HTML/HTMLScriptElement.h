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

class HTMLScriptElement final
    : public HTMLElement
    , public ResourceClient {
    WEB_PLATFORM_OBJECT(HTMLScriptElement, HTMLElement);

public:
    virtual ~HTMLScriptElement() override;

    bool is_force_async() const { return m_force_async; }
    bool is_ready_to_be_parser_executed() const { return m_ready_to_be_parser_executed; }
    bool failed_to_load() const { return m_failed_to_load; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_parser_document(Badge<T>, DOM::Document& document) { m_parser_document = &document; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_force_async(Badge<T>, bool b) { m_force_async = b; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void set_already_started(Badge<T>, bool b) { m_already_started = b; }

    template<OneOf<XMLDocumentBuilder, HTMLParser> T>
    void prepare_script(Badge<T>) { prepare_script(); }

    void execute_script();

    bool is_parser_inserted() const { return !!m_parser_document; }

    virtual void inserted() override;

    // https://html.spec.whatwg.org/multipage/scripting.html#dom-script-supports
    static bool supports(JS::VM&, String const& type)
    {
        return type.is_one_of("classic", "module");
    }

    void set_source_line_number(Badge<HTMLParser>, size_t source_line_number) { m_source_line_number = source_line_number; }

public:
    HTMLScriptElement(DOM::Document&, DOM::QualifiedName);

    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;

    virtual void visit_edges(Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/scripting.html#prepare-the-script-element
    void prepare_script();

    void begin_delaying_document_load_event(DOM::Document&);

    struct ResultState {
        struct Uninitialized { };
        struct Null { };
    };

    using Result = Variant<ResultState::Uninitialized, ResultState::Null, JS::NonnullGCPtr<HTML::Script>>;

    // https://html.spec.whatwg.org/multipage/scripting.html#mark-as-ready
    void mark_as_ready(Result);

    // https://html.spec.whatwg.org/multipage/scripting.html#parser-document
    JS::GCPtr<DOM::Document> m_parser_document;

    // https://html.spec.whatwg.org/multipage/scripting.html#preparation-time-document
    JS::GCPtr<DOM::Document> m_preparation_time_document;

    // https://html.spec.whatwg.org/multipage/scripting.html#script-force-async
    bool m_force_async { false };

    // https://html.spec.whatwg.org/multipage/scripting.html#already-started
    bool m_already_started { false };

    // https://html.spec.whatwg.org/multipage/scripting.html#concept-script-external
    bool m_from_an_external_file { false };

    bool m_script_ready { false };

    // https://html.spec.whatwg.org/multipage/scripting.html#ready-to-be-parser-executed
    bool m_ready_to_be_parser_executed { false };

    bool m_failed_to_load { false };

    enum class ScriptType {
        Null,
        Classic,
        Module,
        ImportMap,
    };

    // https://html.spec.whatwg.org/multipage/scripting.html#concept-script-type
    ScriptType m_script_type { ScriptType::Null };

    // https://html.spec.whatwg.org/multipage/scripting.html#steps-to-run-when-the-result-is-ready
    Function<void()> m_steps_to_run_when_the_result_is_ready;

    // https://html.spec.whatwg.org/multipage/scripting.html#concept-script-result
    Result m_result { ResultState::Uninitialized {} };

    // https://html.spec.whatwg.org/multipage/scripting.html#concept-script-delay-load
    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;

    size_t m_source_line_number { 1 };
};

}
