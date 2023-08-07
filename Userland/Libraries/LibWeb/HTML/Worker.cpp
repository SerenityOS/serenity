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
#include <LibWeb/HTML/Worker.h>
#include <LibWeb/HTML/WorkerDebugConsoleClient.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/workers.html#dedicated-workers-and-the-worker-interface
Worker::Worker(String const& script_url, WorkerOptions const options, DOM::Document& document)
    : DOM::EventTarget(document.realm())
    , m_script_url(script_url)
    , m_options(options)
    , m_document(&document)
    , m_custom_data()
    , m_worker_vm(JS::VM::create(adopt_own(m_custom_data)).release_value_but_fixme_should_propagate_errors())
    , m_interpreter(JS::Interpreter::create<JS::GlobalObject>(m_worker_vm))
    , m_interpreter_scope(*m_interpreter)
    , m_implicit_port(MessagePort::create(document.realm()).release_value_but_fixme_should_propagate_errors())
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
    visitor.visit(m_inner_settings);
    visitor.visit(m_implicit_port);
    visitor.visit(m_outside_port);

    // These are in a separate VM and shouldn't be visited
    visitor.ignore(m_worker_realm);
    visitor.ignore(m_worker_scope);
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker
WebIDL::ExceptionOr<JS::NonnullGCPtr<Worker>> Worker::create(String const& script_url, WorkerOptions const options, DOM::Document& document)
{
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
    auto url = document.parse_url(script_url.to_deprecated_string());

    // 4. If this fails, throw a "SyntaxError" DOMException.
    if (!url.is_valid()) {
        dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Invalid URL loaded '{}'.", script_url);
        return WebIDL::SyntaxError::create(document.realm(), "url is not valid");
    }

    // 5. Let worker URL be the resulting URL record.

    // 6. Let worker be a new Worker object.
    auto worker = MUST_OR_THROW_OOM(document.heap().allocate<Worker>(document.realm(), script_url, options, document));

    // 7. Let outside port be a new MessagePort in outside settings's Realm.
    auto outside_port = TRY(MessagePort::create(outside_settings.realm()));

    // 8. Associate the outside port with worker
    worker->m_outside_port = outside_port;

    // 9. Run this step in parallel:
    //    1. Run a worker given worker, worker URL, outside settings, outside port, and options.
    worker->run_a_worker(url, outside_settings, *outside_port, options);

    // 10. Return worker
    return worker;
}

// https://html.spec.whatwg.org/multipage/workers.html#run-a-worker
void Worker::run_a_worker(AK::URL& url, EnvironmentSettingsObject& outside_settings, MessagePort& outside_port, WorkerOptions const& options)
{
    // 1. Let is shared be true if worker is a SharedWorker object, and false otherwise.
    // FIXME: SharedWorker support
    auto is_shared = false;
    auto is_top_level = false;

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
    // NOTE: This is effectively the worker's vm

    // 7. Let realm execution context be the result of creating a new JavaScript realm given agent and the following customizations:
    auto realm_execution_context = Bindings::create_a_new_javascript_realm(
        *m_worker_vm,
        [&](JS::Realm& realm) -> JS::Object* {
            //      7a. For the global object, if is shared is true, create a new SharedWorkerGlobalScope object.
            //      7b. Otherwise, create a new DedicatedWorkerGlobalScope object.
            // FIXME: Proper support for both SharedWorkerGlobalScope and DedicatedWorkerGlobalScope
            if (is_shared)
                TODO();
            // FIXME: Make and use subclasses of WorkerGlobalScope, however this requires JS::GlobalObject to
            //        play nicely with the IDL interpreter, to make spec-compliant extensions, which it currently does not.
            m_worker_scope = m_worker_vm->heap().allocate_without_realm<JS::GlobalObject>(realm);
            return m_worker_scope.ptr();
        },
        nullptr);

    auto& console_object = *realm_execution_context->realm->intrinsics().console_object();
    m_worker_realm = realm_execution_context->realm;

    m_console = adopt_ref(*new WorkerDebugConsoleClient(console_object.console()));
    console_object.console().set_client(*m_console);

    // FIXME: This should be done with IDL
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable;
    m_worker_scope->define_native_function(
        m_worker_scope->shape().realm(),
        "postMessage",
        [this](auto& vm) {
            // This is the implementation of the function that the spawned worked calls

            // https://html.spec.whatwg.org/multipage/workers.html#dom-dedicatedworkerglobalscope-postmessage
            // The postMessage(message, transfer) and postMessage(message, options) methods on DedicatedWorkerGlobalScope
            // objects act as if, when invoked, it immediately invoked the respective postMessage(message, transfer) and
            // postMessage(message, options) on the port, with the same arguments, and returned the same return value

            auto message = vm.argument(0);
            // FIXME: `transfer` not support by post_message yet

            dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Inner post_message");

            // FIXME: This is a bit of a hack, in reality, we should m_outside_port->post_message and the onmessage event
            //        should bubble up to the Worker itself from there.

            auto& event_loop = get_vm_event_loop(m_document->realm().vm());

            event_loop.task_queue().add(HTML::Task::create(HTML::Task::Source::PostedMessage, nullptr, [this, message] {
                MessageEventInit event_init {};
                event_init.data = message;
                event_init.origin = "<origin>"_string;
                dispatch_event(MessageEvent::create(*m_worker_realm, HTML::EventNames::message, event_init).release_value_but_fixme_should_propagate_errors());
            }));

            return JS::js_undefined();
        },
        2, attr);

    // 8. Let worker global scope be the global object of realm execution context's Realm component.
    // NOTE: This is the DedicatedWorkerGlobalScope or SharedWorkerGlobalScope object created in the previous step.

    // 9. Set up a worker environment settings object with realm execution context,
    //    outside settings, and unsafeWorkerCreationTime, and let inside settings be the result.
    m_inner_settings = WorkerEnvironmentSettingsObject::setup(move(realm_execution_context));

    // 10. Set worker global scope's name to the value of options's name member.
    // FIXME: name property requires the SharedWorkerGlobalScope or DedicatedWorkerGlobalScope child class to be used

    // 11. Append owner to worker global scope's owner set.
    // FIXME: support for 'owner' set on WorkerGlobalScope

    // 12. If is shared is true, then:
    if (is_shared) {
        // FIXME: Shared worker support
        // 1. Set worker global scope's constructor origin to outside settings's origin.
        // 2. Set worker global scope's constructor url to url.
        // 3. Set worker global scope's type to the value of options's type member.
        // 4. Set worker global scope's credentials to the value of options's credentials member.
    }

    // 13. Let destination be "sharedworker" if is shared is true, and "worker" otherwise.

    // 14. Obtain script by switching on the value of options's type member:
    // classic: Fetch a classic worker script given url, outside settings, destination, and inside settings.
    // module:  Fetch a module worker script graph given url, outside settings, destination, the value of the
    //          credentials member of options, and inside settings.
    if (options.type != "classic") {
        dbgln_if(WEB_WORKER_DEBUG, "Unsupported script type {} for LibWeb/Worker", options.type);
        TODO();
    }

    ResourceLoader::the().load(
        url,
        [this, is_shared, is_top_level, url, &outside_port](auto data, auto&, auto) {
            // In both cases, to perform the fetch given request, perform the following steps if the is top-level flag is set:
            if (is_top_level) {
                // 1. Set request's reserved client to inside settings.

                // 2. Fetch request, and asynchronously wait to run the remaining steps
                //    as part of fetch's process response for the response response.
                // Implied

                // 3. Set worker global scope's url to response's url.

                // 4. Initialize worker global scope's policy container given worker global scope, response, and inside settings.
                // FIXME: implement policy container

                // 5. If the Run CSP initialization for a global object algorithm returns "Blocked" when executed upon worker
                //    global scope, set response to a network error. [CSP]
                // FIXME: CSP support

                // 6. If worker global scope's embedder policy's value is compatible with cross-origin isolation and is shared is true,
                //    then set agent's agent cluster's cross-origin isolation mode to "logical" or "concrete".
                //    The one chosen is implementation-defined.
                // FIXME: CORS policy support

                // 7. If the result of checking a global object's embedder policy with worker global scope, outside settings,
                //    and response is false, then set response to a network error.
                // FIXME: Embed policy support

                // 8. Set worker global scope's cross-origin isolated capability to true if agent's agent cluster's cross-origin
                //    isolation mode is "concrete".
                // FIXME: CORS policy support

                if (!is_shared) {
                    // 9.  If is shared is false and owner's cross-origin isolated capability is false, then set worker
                    //     global scope's cross-origin isolated capability to false.
                    // 10. If is shared is false and response's url's scheme is "data", then set worker global scope's
                    //     cross-origin isolated capability to false.
                }
            }

            if (data.is_null()) {
                dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Failed to load {}", url);
                return;
            }

            // Asynchronously complete the perform the fetch steps with response.
            dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Script ready!");
            auto script = ClassicScript::create(url.to_deprecated_string(), data, *m_inner_settings, AK::URL());

            // NOTE: Steps 15-31 below pick up after step 14 in the main context, steps 1-10 above
            //       are only for validation when used in a top-level case (ie: from a Window)

            // 15. Associate worker with worker global scope.
            // FIXME: Global scope association

            // 16. Let inside port be a new MessagePort object in inside settings's Realm.
            auto inside_port = MessagePort::create(m_inner_settings->realm()).release_value_but_fixme_should_propagate_errors();

            // 17. Associate inside port with worker global scope.
            // FIXME: Global scope association

            // 18. Entangle outside port and inside port.
            outside_port.entangle_with(*inside_port);

            // 19. Create a new WorkerLocation object and associate it with worker global scope.

            // 20. Closing orphan workers: Start monitoring the worker such that no sooner than it
            //     stops being a protected worker, and no later than it stops being a permissible worker,
            //     worker global scope's closing flag is set to true.
            // FIXME: Worker monitoring and cleanup

            // 21. Suspending workers: Start monitoring the worker, such that whenever worker global scope's
            //     closing flag is false and the worker is a suspendable worker, the user agent suspends
            //     execution of script in that worker until such time as either the closing flag switches to
            //     true or the worker stops being a suspendable worker
            // FIXME: Worker suspending

            // 22. Set inside settings's execution ready flag.
            // FIXME: Implement worker settings object

            // 23. If script is a classic script, then run the classic script script.
            //     Otherwise, it is a module script; run the module script script.
            auto result = script->run();

            // 24. Enable outside port's port message queue.
            outside_port.start();

            // 25. If is shared is false, enable the port message queue of the worker's implicit port.
            if (!is_shared)
                m_implicit_port->start();

            // 26. If is shared is true, then queue a global task on DOM manipulation task source given worker
            //     global scope to fire an event named connect at worker global scope, using MessageEvent,
            //     with the data attribute initialized to the empty string, the ports attribute initialized
            //     to a new frozen array containing inside port, and the source attribute initialized to inside port.
            // FIXME: Shared worker support

            // 27. Enable the client message queue of the ServiceWorkerContainer object whose associated service
            //     worker client is worker global scope's relevant settings object.
            // FIXME: Understand....and support worker global settings

            // 28. Event loop: Run the responsible event loop specified by inside settings until it is destroyed.
        },
        [](auto&, auto) {
            dbgln_if(WEB_WORKER_DEBUG, "WebWorker: HONK! Failed to load script.");
        });
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-terminate
WebIDL::ExceptionOr<void> Worker::terminate()
{
    dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Terminate");

    return {};
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-postmessage
void Worker::post_message(JS::Value message, JS::Value)
{
    dbgln_if(WEB_WORKER_DEBUG, "WebWorker: Post Message: {}", MUST(message.to_string_without_side_effects()));

    // 1. Let targetPort be the port with which this is entangled, if any; otherwise let it be null.
    auto& target_port = m_outside_port;

    // 2. Let options be «[ "transfer" → transfer ]».
    // 3. Run the message port post message steps providing this, targetPort, message and options.
    target_port->post_message(message);
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
