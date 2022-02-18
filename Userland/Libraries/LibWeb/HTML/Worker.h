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
    String type { "classic" };
    String credentials { "same-origin" };
    String name { "" };
};

// https://html.spec.whatwg.org/multipage/workers.html#dedicated-workers-and-the-worker-interface
class Worker
    : public RefCounted<Worker>
    , public Weakable<Worker>
    , public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::WorkerWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static DOM::ExceptionOr<NonnullRefPtr<Worker>> create(FlyString const& script_url, WorkerOptions const options, DOM::Document& document);
    static DOM::ExceptionOr<NonnullRefPtr<Worker>> create_with_global_object(Bindings::WindowObject& window, FlyString const& script_url, WorkerOptions const options)
    {
        return Worker::create(script_url, options, window.impl().associated_document());
    }

    DOM::ExceptionOr<void> terminate();

    void post_message(JS::Value message, JS::Value transfer);

    virtual ~Worker() = default;

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    MessagePort* implicit_message_port() { return m_implicit_port; }
    RefPtr<MessagePort> outside_message_port() { return m_outside_port; }

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                  \
    void set_##attribute_name(Optional<Bindings::CallbackType>); \
    Bindings::CallbackType* attribute_name();
    ENUMERATE_WORKER_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

protected:
    Worker(FlyString const&, const WorkerOptions, DOM::Document&);

private:
    static HTML::EventLoop& get_vm_event_loop(JS::VM& target_vm)
    {
        return static_cast<Bindings::WebEngineCustomData*>(target_vm.custom_data())->event_loop;
    }

    FlyString m_script_url;
    WorkerOptions m_options;
    WeakPtr<DOM::Document> m_document;
    Bindings::WebEngineCustomData m_custom_data;

    NonnullRefPtr<JS::VM> m_worker_vm;
    NonnullOwnPtr<JS::Interpreter> m_interpreter;
    WeakPtr<WorkerEnvironmentSettingsObject> m_inner_settings;
    JS::VM::InterpreterExecutionScope m_interpreter_scope;
    JS::ExecutionContext m_execution_context;
    WeakPtr<JS::Realm> m_worker_realm;
    RefPtr<WorkerDebugConsoleClient> m_console;
    JS::GlobalObject* m_worker_scope;

    NonnullRefPtr<MessagePort> m_implicit_port;
    RefPtr<MessagePort> m_outside_port;

    void run_a_worker(AK::URL& url, EnvironmentSettingsObject& outside_settings, MessagePort& outside_port, WorkerOptions const options);
};

} // namespace Web::HTML

namespace Web::Bindings {

WorkerWrapper* wrap(JS::GlobalObject&, HTML::Worker&);

}
