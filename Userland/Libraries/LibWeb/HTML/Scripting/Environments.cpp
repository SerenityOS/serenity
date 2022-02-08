/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/PromiseRejectionEvent.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::HTML {

EnvironmentSettingsObject::EnvironmentSettingsObject(JS::ExecutionContext& realm_execution_context)
    : m_realm_execution_context(realm_execution_context)
{
    // Register with the responsible event loop so we can perform step 4 of "perform a microtask checkpoint".
    responsible_event_loop().register_environment_settings_object({}, *this);
}

EnvironmentSettingsObject::~EnvironmentSettingsObject()
{
    responsible_event_loop().unregister_environment_settings_object({}, *this);
}

JS::ExecutionContext& EnvironmentSettingsObject::realm_execution_context()
{
    // NOTE: All environment settings objects are created with a realm execution context, so it's stored and returned here in the base class.
    return m_realm_execution_context;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#environment-settings-object%27s-realm
JS::Realm& EnvironmentSettingsObject::realm()
{
    // An environment settings object's realm execution context's Realm component is the environment settings object's Realm.
    return *realm_execution_context().realm;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-settings-object-global
JS::GlobalObject& EnvironmentSettingsObject::global_object()
{
    // An environment settings object's Realm then has a [[GlobalObject]] field, which contains the environment settings object's global object.
    return realm().global_object();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#responsible-event-loop
EventLoop& EnvironmentSettingsObject::responsible_event_loop()
{
    // An environment settings object's responsible event loop is its global object's relevant agent's event loop.
    // This is here in case the realm that is holding onto this ESO is destroyed before the ESO is. The responsible event loop pointer is needed in the ESO destructor to deregister from the event loop.
    // FIXME: Figure out why the realm can be destroyed before the ESO, as the realm is holding onto this with an OwnPtr, but the heap block deallocator calls the ESO destructor directly instead of through the realm destructor.
    if (m_responsible_event_loop)
        return *m_responsible_event_loop;

    auto& vm = global_object().vm();
    auto& event_loop = verify_cast<Bindings::WebEngineCustomData>(vm.custom_data())->event_loop;
    m_responsible_event_loop = &event_loop;
    return event_loop;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#check-if-we-can-run-script
RunScriptDecision EnvironmentSettingsObject::can_run_script()
{
    // 1. If the global object specified by settings is a Window object whose Document object is not fully active, then return "do not run".
    if (is<Bindings::WindowObject>(global_object()) && !verify_cast<Bindings::WindowObject>(global_object()).impl().associated_document().is_fully_active())
        return RunScriptDecision::DoNotRun;

    // FIXME: 2. If scripting is disabled for settings, then return "do not run".

    // 3. Return "run".
    return RunScriptDecision::Run;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#prepare-to-run-script
void EnvironmentSettingsObject::prepare_to_run_script()
{
    // 1. Push settings's realm execution context onto the JavaScript execution context stack; it is now the running JavaScript execution context.
    global_object().vm().push_execution_context(realm_execution_context(), global_object());

    // FIXME: 2. Add settings to the currently running task's script evaluation environment settings object set.
}

// https://html.spec.whatwg.org/multipage/webappapis.html#clean-up-after-running-script
void EnvironmentSettingsObject::clean_up_after_running_script()
{
    auto& vm = global_object().vm();

    // 1. Assert: settings's realm execution context is the running JavaScript execution context.
    VERIFY(&realm_execution_context() == &vm.running_execution_context());

    // 2. Remove settings's realm execution context from the JavaScript execution context stack.
    vm.pop_execution_context();

    // 3. If the JavaScript execution context stack is now empty, perform a microtask checkpoint. (If this runs scripts, these algorithms will be invoked reentrantly.)
    if (vm.execution_context_stack().is_empty())
        responsible_event_loop().perform_a_microtask_checkpoint();
}

static JS::ExecutionContext* top_most_script_having_execution_context(JS::VM& vm)
{
    // Here, the topmost script-having execution context is the topmost entry of the JavaScript execution context stack that has a non-null ScriptOrModule component,
    // or null if there is no such entry in the JavaScript execution context stack.
    auto execution_context = vm.execution_context_stack().last_matching([&](JS::ExecutionContext* context) {
        return !context->script_or_module.has<Empty>();
    });

    if (!execution_context.has_value())
        return nullptr;

    return execution_context.value();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#prepare-to-run-a-callback
void EnvironmentSettingsObject::prepare_to_run_callback()
{
    auto& vm = global_object().vm();

    // 1. Push settings onto the backup incumbent settings object stack.
    // NOTE: The spec doesn't say which event loop's stack to put this on. However, all the examples of the incumbent settings object use iframes and cross browsing context communication to demonstrate the concept.
    //       This means that it must rely on some global state that can be accessed by all browsing contexts, which is the main thread event loop.
    HTML::main_thread_event_loop().push_onto_backup_incumbent_settings_object_stack({}, *this);

    // 2. Let context be the topmost script-having execution context.
    auto* context = top_most_script_having_execution_context(vm);

    // 3. If context is not null, increment context's skip-when-determining-incumbent counter.
    if (context)
        context->skip_when_determining_incumbent_counter++;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#clean-up-after-running-a-callback
void EnvironmentSettingsObject::clean_up_after_running_callback()
{
    auto& vm = global_object().vm();

    // 1. Let context be the topmost script-having execution context.
    auto* context = top_most_script_having_execution_context(vm);

    // 2. If context is not null, decrement context's skip-when-determining-incumbent counter.
    if (context)
        context->skip_when_determining_incumbent_counter--;

    // 3. Assert: the topmost entry of the backup incumbent settings object stack is settings.
    auto& event_loop = HTML::main_thread_event_loop();
    VERIFY(&event_loop.top_of_backup_incumbent_settings_object_stack() == this);

    // 4. Remove settings from the backup incumbent settings object stack.
    event_loop.pop_backup_incumbent_settings_object_stack({});
}

void EnvironmentSettingsObject::push_onto_outstanding_rejected_promises_weak_set(JS::Promise* promise)
{
    m_outstanding_rejected_promises_weak_set.append(promise);
}

bool EnvironmentSettingsObject::remove_from_outstanding_rejected_promises_weak_set(JS::Promise* promise)
{
    return m_outstanding_rejected_promises_weak_set.remove_first_matching([&](JS::Promise* promise_in_set) {
        return promise == promise_in_set;
    });
}

void EnvironmentSettingsObject::push_onto_about_to_be_notified_rejected_promises_list(JS::Handle<JS::Promise> promise)
{
    m_about_to_be_notified_rejected_promises_list.append(move(promise));
}

bool EnvironmentSettingsObject::remove_from_about_to_be_notified_rejected_promises_list(JS::Promise* promise)
{
    return m_about_to_be_notified_rejected_promises_list.remove_first_matching([&](JS::Handle<JS::Promise> promise_in_list) {
        return promise == promise_in_list.cell();
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#notify-about-rejected-promises
void EnvironmentSettingsObject::notify_about_rejected_promises(Badge<EventLoop>)
{
    // 1. Let list be a copy of settings object's about-to-be-notified rejected promises list.
    auto list = m_about_to_be_notified_rejected_promises_list;

    // 2. If list is empty, return.
    if (list.is_empty())
        return;

    // 3. Clear settings object's about-to-be-notified rejected promises list.
    m_about_to_be_notified_rejected_promises_list.clear();

    // 4. Let global be settings object's global object.
    auto& global = global_object();

    // 5. Queue a global task on the DOM manipulation task source given global to run the following substep:
    queue_global_task(Task::Source::DOMManipulation, global, [this, global = JS::make_handle(&global), list = move(list)]() mutable {
        // 1. For each promise p in list:
        for (auto promise_handle : list) {
            auto& promise = *promise_handle.cell();

            // 1. If p's [[PromiseIsHandled]] internal slot is true, continue to the next iteration of the loop.
            if (promise.is_handled())
                continue;

            // 2. Let notHandled be the result of firing an event named unhandledrejection at global, using PromiseRejectionEvent, with the cancelable attribute initialized to true,
            //    the promise attribute initialized to p, and the reason attribute initialized to the value of p's [[PromiseResult]] internal slot.
            PromiseRejectionEventInit event_init {
                {
                    .bubbles = false,
                    .cancelable = true,
                    .composed = false,
                },
                // Sadly we can't use .promise and .reason here, as we can't use the designator on the initialization of DOM::EventInit above.
                /* .promise = */ promise_handle,
                /* .reason = */ promise.result(),
            };
            auto promise_rejection_event = PromiseRejectionEvent::create(HTML::EventNames::unhandledrejection, event_init);

            // FIXME: This currently assumes that global is a WindowObject.
            auto& window = verify_cast<Bindings::WindowObject>(*global.cell());

            bool not_handled = window.impl().dispatch_event(move(promise_rejection_event));

            // 3. If notHandled is false, then the promise rejection is handled. Otherwise, the promise rejection is not handled.

            // 4. If p's [[PromiseIsHandled]] internal slot is false, add p to settings object's outstanding rejected promises weak set.
            if (!promise.is_handled())
                m_outstanding_rejected_promises_weak_set.append(&promise);

            // This algorithm results in promise rejections being marked as handled or not handled. These concepts parallel handled and not handled script errors.
            // If a rejection is still not handled after this, then the rejection may be reported to a developer console.
            if (not_handled)
                dbgln("WARNING: A promise was rejected without any handlers. promise={:p}, result={}", &promise, promise.result().to_string_without_side_effects());
        }
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#incumbent-settings-object
EnvironmentSettingsObject& incumbent_settings_object()
{
    auto& event_loop = HTML::main_thread_event_loop();
    auto& vm = event_loop.vm();

    // 1. Let context be the topmost script-having execution context.
    auto* context = top_most_script_having_execution_context(vm);

    // 2. If context is null, or if context's skip-when-determining-incumbent counter is greater than zero, then:
    if (!context || context->skip_when_determining_incumbent_counter > 0) {
        // 1. Assert: the backup incumbent settings object stack is not empty.
        // NOTE: If this assertion fails, it's because the incumbent settings object was used with no involvement of JavaScript.
        VERIFY(!event_loop.is_backup_incumbent_settings_object_stack_empty());

        // 2. Return the topmost entry of the backup incumbent settings object stack.
        return event_loop.top_of_backup_incumbent_settings_object_stack();
    }

    // 3. Return context's Realm component's settings object.
    return verify_cast<EnvironmentSettingsObject>(*context->realm->host_defined());
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-incumbent-realm
JS::Realm& incumbent_realm()
{
    // Then, the incumbent Realm is the Realm of the incumbent settings object.
    return incumbent_settings_object().realm();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-incumbent-global
JS::GlobalObject& incumbent_global_object()
{
    // Similarly, the incumbent global object is the global object of the incumbent settings object.
    return incumbent_settings_object().global_object();
}

}
