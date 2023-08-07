/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/URLParser.h>
#include <LibJS/Interpreter.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/MessagePort.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Scripting/WorkerEnvironmentSettingsObject.h>
#include <LibWeb/HTML/WorkerDebugConsoleClient.h>
#include <LibWeb/Loader/ResourceLoader.h>

#define ENUMERATE_WORKER_EVENT_HANDLERS(E)  \
    E(onmessage, HTML::EventNames::message) \
    E(onmessageerror, HTML::EventNames::messageerror)

namespace Web::HTML {

struct WorkerOptions {
    String type { "classic"_string };
    String credentials { "same-origin"_string };
    String name { String {} };
};

// https://html.spec.whatwg.org/multipage/workers.html#dedicated-workers-and-the-worker-interface
class Worker : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Worker, DOM::EventTarget);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Worker>> create(String const& script_url, WorkerOptions const options, DOM::Document& document);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Worker>> construct_impl(JS::Realm& realm, String const& script_url, WorkerOptions const options)
    {
        auto& window = verify_cast<HTML::Window>(realm.global_object());
        return Worker::create(script_url, options, window.associated_document());
    }

    WebIDL::ExceptionOr<void> terminate();

    void post_message(JS::Value message, JS::Value transfer);

    virtual ~Worker() = default;

    MessagePort* implicit_message_port() { return m_implicit_port.ptr(); }
    JS::GCPtr<MessagePort> outside_message_port() { return m_outside_port; }

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)       \
    void set_##attribute_name(WebIDL::CallbackType*); \
    WebIDL::CallbackType* attribute_name();
    ENUMERATE_WORKER_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

protected:
    Worker(String const&, const WorkerOptions, DOM::Document&);

private:
    static HTML::EventLoop& get_vm_event_loop(JS::VM& target_vm)
    {
        return static_cast<Bindings::WebEngineCustomData*>(target_vm.custom_data())->event_loop;
    }

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    String m_script_url;
    WorkerOptions m_options;

    JS::GCPtr<DOM::Document> m_document;

    Bindings::WebEngineCustomData m_custom_data;

    NonnullRefPtr<JS::VM> m_worker_vm;
    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    JS::GCPtr<WorkerEnvironmentSettingsObject> m_inner_settings;
    JS::VM::InterpreterExecutionScope m_interpreter_scope;
    RefPtr<WorkerDebugConsoleClient> m_console;

    JS::NonnullGCPtr<MessagePort> m_implicit_port;
    JS::GCPtr<MessagePort> m_outside_port;

    // NOTE: These are inside the worker VM.
    JS::GCPtr<JS::Realm> m_worker_realm;
    JS::GCPtr<JS::Object> m_worker_scope;

    void run_a_worker(AK::URL& url, EnvironmentSettingsObject& outside_settings, MessagePort& outside_port, WorkerOptions const& options);
};

}
