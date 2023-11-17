/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/URLParser.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/MessagePort.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/WindowEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Scripting/WorkerEnvironmentSettingsObject.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WorkerAgent.h>
#include <LibWeb/HTML/WorkerDebugConsoleClient.h>
#include <LibWeb/Loader/ResourceLoader.h>

#define ENUMERATE_WORKER_EVENT_HANDLERS(E)  \
    E(onmessage, HTML::EventNames::message) \
    E(onmessageerror, HTML::EventNames::messageerror)

namespace Web::HTML {

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
    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    String m_script_url;
    WorkerOptions m_options;

    JS::GCPtr<DOM::Document> m_document;
    JS::GCPtr<MessagePort> m_outside_port;

    JS::GCPtr<WorkerAgent> m_agent;

    void run_a_worker(AK::URL& url, EnvironmentSettingsObject& outside_settings, MessagePort& outside_port, WorkerOptions const& options);
};

}
