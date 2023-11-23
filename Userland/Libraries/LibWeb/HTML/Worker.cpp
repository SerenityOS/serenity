/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HTML/Worker.h>
#include <LibWeb/HTML/WorkerDebugConsoleClient.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(Worker);

// https://html.spec.whatwg.org/multipage/workers.html#dedicated-workers-and-the-worker-interface
Worker::Worker(String const& script_url, WorkerOptions const options, DOM::Document& document)
    : DOM::EventTarget(document.realm())
    , m_script_url(script_url)
    , m_options(options)
    , m_document(&document)
{
}

void Worker::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WorkerPrototype>(realm, "Worker"));
}

void Worker::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
    visitor.visit(m_outside_port);
    visitor.visit(m_agent);
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker
WebIDL::ExceptionOr<JS::NonnullGCPtr<Worker>> Worker::create(String const& script_url, WorkerOptions const options, DOM::Document& document)
{
    // FIXME: Modernize these spec steps.

    dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Creating worker with script_url = {}", script_url);

    // Returns a new Worker object. scriptURL will be fetched and executed in the background,
    // creating a new global environment for which worker represents the communication channel.
    // options can be used to define the name of that global environment via the name option,
    // primarily for debugging purposes. It can also ensure this new global environment supports
    // JavaScript modules (specify type: "module"), and if that is specified, can also be used
    // to specify how scriptURL is fetched through the credentials option.

    // FIXME: 1. The user agent may throw a "SecurityError" DOMException if the request violates
    // a policy decision (e.g. if the user agent is configured to not allow the page to start dedicated workers).
    // Technically not a fixme if our policy is not to throw errors :^)

    // 2. Let outside settings be the current settings object.
    auto& outside_settings = document.relevant_settings_object();

    // 3. Parse the scriptURL argument relative to outside settings.
    auto url = document.encoding_parse_url(script_url.to_deprecated_string());

    // 4. If this fails, throw a "SyntaxError" DOMException.
    if (!url.is_valid()) {
        dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Invalid URL loaded '{}'.", script_url);
        return WebIDL::SyntaxError::create(document.realm(), "url is not valid"_fly_string);
    }

    // 5. Let worker URL be the resulting URL record.

    // 6. Let worker be a new Worker object.
    auto worker = document.heap().allocate<Worker>(document.realm(), script_url, options, document);

    // 7. Let outside port be a new MessagePort in outside settings's Realm.
    auto outside_port = MessagePort::create(outside_settings.realm());

    // 8. Associate the outside port with worker
    worker->m_outside_port = outside_port;

    // 9. Run this step in parallel:
    //    1. Run a worker given worker, worker URL, outside settings, outside port, and options.
    worker->run_a_worker(url, outside_settings, *outside_port, options);

    // 10. Return worker
    return worker;
}

// https://html.spec.whatwg.org/multipage/workers.html#run-a-worker
void Worker::run_a_worker(AK::URL& url, EnvironmentSettingsObject& outside_settings, MessagePort&, WorkerOptions const& options)
{
    // 1. Let is shared be true if worker is a SharedWorker object, and false otherwise.
    // FIXME: SharedWorker support

    // 2. Let owner be the relevant owner to add given outside settings.
    // FIXME: Support WorkerGlobalScope options
    if (!is<HTML::WindowEnvironmentSettingsObject>(outside_settings))
        TODO();

    // 3. Let parent worker global scope be null.
    // 4. If owner is a WorkerGlobalScope object (i.e., we are creating a nested dedicated worker),
    //    then set parent worker global scope to owner.
    // FIXME: Support for nested workers.

    // 5. Let unsafeWorkerCreationTime be the unsafe shared current time.

    // 6. Let agent be the result of obtaining a dedicated/shared worker agent given outside settings
    // and is shared. Run the rest of these steps in that agent.

    // Note: This spawns a new process to act as the 'agent' for the worker.
    m_agent = heap().allocate_without_realm<WorkerAgent>(url, options);

    auto& socket = m_agent->socket();
    // FIXME: Hide this logic in MessagePort
    socket.set_notifications_enabled(true);
    socket.on_ready_to_read = [this] {
        auto& socket = this->m_agent->socket();
        auto& vm = this->vm();
        auto& realm = this->realm();

        auto num_bytes_ready = MUST(socket.pending_bytes());
        switch (m_outside_port_state) {
        case PortState::Header: {
            if (num_bytes_ready < 8)
                break;
            auto const magic = MUST(socket.read_value<u32>());
            if (magic != 0xDEADBEEF) {
                m_outside_port_state = PortState::Error;
                break;
            }
            m_outside_port_incoming_message_size = MUST(socket.read_value<u32>());
            num_bytes_ready -= 8;
            m_outside_port_state = PortState::Data;
        }
            [[fallthrough]];
        case PortState::Data: {
            if (num_bytes_ready < m_outside_port_incoming_message_size)
                break;
            SerializationRecord rec; // FIXME: Keep in class scope
            rec.resize(m_outside_port_incoming_message_size / sizeof(u32));

            MUST(socket.read_until_filled(to_bytes(rec.span())));

            TemporaryExecutionContext cxt(relevant_settings_object(*this));
            VERIFY(&realm == vm.current_realm());
            MessageEventInit event_init {};
            event_init.data = MUST(structured_deserialize(vm, rec, realm, {}));
            // FIXME: Fill in the rest of the info from MessagePort

            this->dispatch_event(MessageEvent::create(realm, EventNames::message, event_init));

            m_outside_port_state = PortState::Header;
            break;
        }
        case PortState::Error:
            VERIFY_NOT_REACHED();
            break;
        }
    };
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-terminate
WebIDL::ExceptionOr<void> Worker::terminate()
{
    dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Terminate");

    return {};
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-postmessage
WebIDL::ExceptionOr<void> Worker::post_message(JS::Value message, JS::Value)
{
    dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Post Message: {}", message.to_string_without_side_effects());

    // FIXME: 1. Let targetPort be the port with which this is entangled, if any; otherwise let it be null.
    // FIXME: 2. Let options be «[ "transfer" → transfer ]».
    // FIXME: 3. Run the message port post message steps providing this, targetPort, message and options.

    auto& realm = this->realm();
    auto& vm = this->vm();

    // FIXME: Use the with-transfer variant, which should(?) prepend the magic + size at the front
    auto data = TRY(structured_serialize(vm, message));

    Array<u32, 2> header = { 0xDEADBEEF, static_cast<u32>(data.size() * sizeof(u32)) };

    if (auto const err = m_agent->socket().write_until_depleted(to_readonly_bytes(header.span())); err.is_error())
        return WebIDL::DataCloneError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("{}", err.error())));

    if (auto const err = m_agent->socket().write_until_depleted(to_readonly_bytes(data.span())); err.is_error())
        return WebIDL::DataCloneError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("{}", err.error())));

    return {};
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                    \
    void Worker::set_##attribute_name(WebIDL::CallbackType* value) \
    {                                                              \
        set_event_handler_attribute(event_name, move(value));      \
    }                                                              \
    WebIDL::CallbackType* Worker::attribute_name()                 \
    {                                                              \
        return event_handler_attribute(event_name);                \
    }
ENUMERATE_WORKER_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

} // namespace Web::HTML
