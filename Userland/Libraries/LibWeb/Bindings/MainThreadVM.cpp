/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Module.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/MutationObserverWrapper.h>
#include <LibWeb/Bindings/MutationRecordWrapper.h>
#include <LibWeb/Bindings/WindowProxy.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/PromiseRejectionEvent.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

// https://html.spec.whatwg.org/multipage/webappapis.html#active-script
HTML::ClassicScript* active_script()
{
    // 1. Let record be GetActiveScriptOrModule().
    auto record = main_thread_vm().get_active_script_or_module();

    // 2. If record is null, return null.
    if (record.has<Empty>())
        return nullptr;

    // 3. Return record.[[HostDefined]].
    if (record.has<WeakPtr<JS::Module>>()) {
        // FIXME: We don't currently have a module script.
        TODO();
    }

    auto js_script = record.get<WeakPtr<JS::Script>>();
    VERIFY(js_script);
    VERIFY(js_script->host_defined());
    return verify_cast<HTML::ClassicScript>(js_script->host_defined());
}

JS::VM& main_thread_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm) {
        vm = JS::VM::create(make<WebEngineCustomData>());

        // NOTE: We intentionally leak the main thread JavaScript VM.
        //       This avoids doing an exhaustive garbage collection on process exit.
        vm->ref();

        static_cast<WebEngineCustomData*>(vm->custom_data())->event_loop.set_vm(*vm);

        // 8.1.5.1 HostEnsureCanAddPrivateElement(O), https://html.spec.whatwg.org/multipage/webappapis.html#the-hostensurecanaddprivateelement-implementation
        vm->host_ensure_can_add_private_element = [](JS::Object const& object) -> JS::ThrowCompletionOr<void> {
            // 1. If O is a WindowProxy object, or implements Location, then return Completion { [[Type]]: throw, [[Value]]: a new TypeError }.
            if (is<WindowProxy>(object) || is<LocationObject>(object))
                return vm->throw_completion<JS::TypeError>("Cannot add private elements to window or location object");

            // 2. Return NormalCompletion(unused).
            return {};
        };

        // FIXME: Implement 8.1.5.2 HostEnsureCanCompileStrings(callerRealm, calleeRealm), https://html.spec.whatwg.org/multipage/webappapis.html#hostensurecancompilestrings(callerrealm,-calleerealm)

        // 8.1.5.3 HostPromiseRejectionTracker(promise, operation), https://html.spec.whatwg.org/multipage/webappapis.html#the-hostpromiserejectiontracker-implementation
        vm->host_promise_rejection_tracker = [](JS::Promise& promise, JS::Promise::RejectionOperation operation) {
            // 1. Let script be the running script.
            //    The running script is the script in the [[HostDefined]] field in the ScriptOrModule component of the running JavaScript execution context.
            HTML::Script* script { nullptr };
            vm->running_execution_context().script_or_module.visit(
                [&script](WeakPtr<JS::Script>& js_script) {
                    script = verify_cast<HTML::ClassicScript>(js_script->host_defined());
                },
                [](WeakPtr<JS::Module>&) {
                    TODO();
                },
                [](Empty) {
                });

            // 2. If script is a classic script and script's muted errors is true, then return.
            // NOTE: is<T>() returns false if nullptr is passed.
            if (is<HTML::ClassicScript>(script)) {
                auto const& classic_script = static_cast<HTML::ClassicScript const&>(*script);
                if (classic_script.muted_errors() == HTML::ClassicScript::MutedErrors::Yes)
                    return;
            }

            // 3. Let settings object be the current settings object.
            // 4. If script is not null, then set settings object to script's settings object.
            auto& settings_object = script ? script->settings_object() : HTML::current_settings_object();

            switch (operation) {
            case JS::Promise::RejectionOperation::Reject:
                // 4. If operation is "reject",
                //    1. Add promise to settings object's about-to-be-notified rejected promises list.
                settings_object.push_onto_about_to_be_notified_rejected_promises_list(JS::make_handle(&promise));
                break;
            case JS::Promise::RejectionOperation::Handle: {
                // 5. If operation is "handle",
                //    1. If settings object's about-to-be-notified rejected promises list contains promise, then remove promise from that list and return.
                bool removed_about_to_be_notified_rejected_promise = settings_object.remove_from_about_to_be_notified_rejected_promises_list(&promise);
                if (removed_about_to_be_notified_rejected_promise)
                    return;

                // 3. Remove promise from settings object's outstanding rejected promises weak set.
                bool removed_outstanding_rejected_promise = settings_object.remove_from_outstanding_rejected_promises_weak_set(&promise);

                // 2. If settings object's outstanding rejected promises weak set does not contain promise, then return.
                // NOTE: This is done out of order because removed_outstanding_rejected_promise will be false if the promise wasn't in the set or true if it was and got removed.
                if (!removed_outstanding_rejected_promise)
                    return;

                // 4. Let global be settings object's global object.
                auto& global = settings_object.global_object();

                // 5. Queue a global task on the DOM manipulation task source given global to fire an event named rejectionhandled at global, using PromiseRejectionEvent,
                //    with the promise attribute initialized to promise, and the reason attribute initialized to the value of promise's [[PromiseResult]] internal slot.
                HTML::queue_global_task(HTML::Task::Source::DOMManipulation, global, [global = JS::make_handle(&global), promise = JS::make_handle(&promise)]() mutable {
                    // FIXME: This currently assumes that global is a WindowObject.
                    auto& window = verify_cast<Bindings::WindowObject>(*global.cell());

                    HTML::PromiseRejectionEventInit event_init {
                        {}, // Initialize the inherited DOM::EventInit
                        /* .promise = */ promise,
                        /* .reason = */ promise.cell()->result(),
                    };
                    auto promise_rejection_event = HTML::PromiseRejectionEvent::create(HTML::EventNames::rejectionhandled, event_init);
                    window.impl().dispatch_event(move(promise_rejection_event));
                });
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }
        };

        // 8.1.5.4.1 HostCallJobCallback(callback, V, argumentsList), https://html.spec.whatwg.org/multipage/webappapis.html#hostcalljobcallback
        vm->host_call_job_callback = [](JS::JobCallback& callback, JS::Value this_value, JS::MarkedVector<JS::Value> arguments_list) {
            auto& callback_host_defined = verify_cast<WebEngineCustomJobCallbackData>(*callback.custom_data);

            // 1. Let incumbent settings be callback.[[HostDefined]].[[IncumbentSettings]]. (NOTE: Not necessary)
            // 2. Let script execution context be callback.[[HostDefined]].[[ActiveScriptContext]]. (NOTE: Not necessary)

            // 3. Prepare to run a callback with incumbent settings.
            callback_host_defined.incumbent_settings.prepare_to_run_callback();

            // 4. If script execution context is not null, then push script execution context onto the JavaScript execution context stack.
            if (callback_host_defined.active_script_context)
                vm->push_execution_context(*callback_host_defined.active_script_context);

            // 5. Let result be Call(callback.[[Callback]], V, argumentsList).
            auto result = JS::call(*vm, *callback.callback.cell(), this_value, move(arguments_list));

            // 6. If script execution context is not null, then pop script execution context from the JavaScript execution context stack.
            if (callback_host_defined.active_script_context) {
                VERIFY(&vm->running_execution_context() == callback_host_defined.active_script_context.ptr());
                vm->pop_execution_context();
            }

            // 7. Clean up after running a callback with incumbent settings.
            callback_host_defined.incumbent_settings.clean_up_after_running_callback();

            // 8. Return result.
            return result;
        };

        // 8.1.5.4.2 HostEnqueueFinalizationRegistryCleanupJob(finalizationRegistry), https://html.spec.whatwg.org/multipage/webappapis.html#hostenqueuefinalizationregistrycleanupjob
        vm->host_enqueue_finalization_registry_cleanup_job = [](JS::FinalizationRegistry& finalization_registry) mutable {
            // 1. Let global be finalizationRegistry.[[Realm]]'s global object.
            auto& global = finalization_registry.realm().global_object();

            // 2. Queue a global task on the JavaScript engine task source given global to perform the following steps:
            HTML::queue_global_task(HTML::Task::Source::JavaScriptEngine, global, [finalization_registry = JS::make_handle(&finalization_registry)]() mutable {
                // 1. Let entry be finalizationRegistry.[[CleanupCallback]].[[Callback]].[[Realm]]'s environment settings object.
                auto& entry = verify_cast<HTML::EnvironmentSettingsObject>(*finalization_registry.cell()->cleanup_callback().callback.cell()->realm()->host_defined());

                // 2. Check if we can run script with entry. If this returns "do not run", then return.
                if (entry.can_run_script() == HTML::RunScriptDecision::DoNotRun)
                    return;

                // 3. Prepare to run script with entry.
                entry.prepare_to_run_script();

                // 4. Let result be the result of performing CleanupFinalizationRegistry(finalizationRegistry).
                auto result = finalization_registry.cell()->cleanup();

                // 5. Clean up after running script with entry.
                entry.clean_up_after_running_script();

                // 6. If result is an abrupt completion, then report the exception given by result.[[Value]].
                if (result.is_error())
                    HTML::report_exception(result);
            });
        };

        // 8.1.5.4.3 HostEnqueuePromiseJob(job, realm), https://html.spec.whatwg.org/multipage/webappapis.html#hostenqueuepromisejob
        vm->host_enqueue_promise_job = [](Function<JS::ThrowCompletionOr<JS::Value>()> job, JS::Realm* realm) {
            // 1. If realm is not null, then let job settings be the settings object for realm. Otherwise, let job settings be null.
            HTML::EnvironmentSettingsObject* job_settings { nullptr };
            if (realm)
                job_settings = verify_cast<HTML::EnvironmentSettingsObject>(realm->host_defined());

            // IMPLEMENTATION DEFINED: The JS spec says we must take implementation defined steps to make the currently active script or module at the time of HostEnqueuePromiseJob being invoked
            //                         also be the active script or module of the job at the time of its invocation.
            //                         This means taking it here now and passing it through to the lambda.
            auto script_or_module = vm->get_active_script_or_module();

            // 2. Queue a microtask on the surrounding agent's event loop to perform the following steps:
            // This instance of "queue a microtask" uses the "implied document". The best fit for "implied document" here is "If the task is being queued by or for a script, then return the script's settings object's responsible document."
            // Do note that "implied document" from the spec is handwavy and the spec authors are trying to get rid of it: https://github.com/whatwg/html/issues/4980
            auto* script = active_script();

            // NOTE: This keeps job_settings alive by keeping realm alive, which is holding onto job_settings.
            HTML::queue_a_microtask(script ? script->settings_object().responsible_document().ptr() : nullptr, [job_settings, job = move(job), realm = realm ? JS::make_handle(realm) : JS::Handle<JS::Realm> {}, script_or_module = move(script_or_module)]() mutable {
                // The dummy execution context has to be kept up here to keep it alive for the duration of the function.
                Optional<JS::ExecutionContext> dummy_execution_context;

                if (job_settings) {
                    // 1. If job settings is not null, then check if we can run script with job settings. If this returns "do not run" then return.
                    if (job_settings->can_run_script() == HTML::RunScriptDecision::DoNotRun)
                        return;

                    // 2. If job settings is not null, then prepare to run script with job settings.
                    job_settings->prepare_to_run_script();

                    // IMPLEMENTATION DEFINED: Per the previous "implementation defined" comment, we must now make the script or module the active script or module.
                    //                         Since the only active execution context currently is the realm execution context of job settings, lets attach it here.
                    job_settings->realm_execution_context().script_or_module = script_or_module;
                } else {
                    // FIXME: We need to setup a dummy execution context in case a JS::NativeFunction is called when processing the job.
                    //        This is because JS::NativeFunction::call excepts something to be on the execution context stack to be able to get the caller context to initialize the environment.
                    //        Since this requires pushing an execution context onto the stack, it also requires a global object. The only thing we can get a global object from in this case is the script or module.
                    //        To do this, we must assume script or module is not Empty. We must also assume that it is a Script Record for now as we don't currently run modules.
                    //        Do note that the JS spec gives _no_ guarantee that the execution context stack has something on it if HostEnqueuePromiseJob was called with a null realm: https://tc39.es/ecma262/#job-preparedtoevaluatecode
                    VERIFY(script_or_module.has<WeakPtr<JS::Script>>());
                    auto script_record = script_or_module.get<WeakPtr<JS::Script>>();
                    dummy_execution_context = JS::ExecutionContext { vm->heap() };
                    dummy_execution_context->script_or_module = script_or_module;
                    vm->push_execution_context(dummy_execution_context.value());
                }

                // 3. Let result be job().
                [[maybe_unused]] auto result = job();

                // 4. If job settings is not null, then clean up after running script with job settings.
                if (job_settings) {
                    // IMPLEMENTATION DEFINED: Disassociate the realm execution context from the script or module.
                    job_settings->realm_execution_context().script_or_module = Empty {};

                    job_settings->clean_up_after_running_script();
                } else {
                    // Pop off the dummy execution context. See the above FIXME block about why this is done.
                    vm->pop_execution_context();
                }

                // 5. If result is an abrupt completion, then report the exception given by result.[[Value]].
                if (result.is_error())
                    HTML::report_exception(result);
            });
        };

        // 8.1.5.4.4 HostMakeJobCallback(callable), https://html.spec.whatwg.org/multipage/webappapis.html#hostmakejobcallback
        vm->host_make_job_callback = [](JS::FunctionObject& callable) -> JS::JobCallback {
            // 1. Let incumbent settings be the incumbent settings object.
            auto& incumbent_settings = HTML::incumbent_settings_object();

            // 2. Let active script be the active script.
            auto* script = active_script();

            // 3. Let script execution context be null.
            OwnPtr<JS::ExecutionContext> script_execution_context;

            // 4. If active script is not null, set script execution context to a new JavaScript execution context, with its Function field set to null,
            //    its Realm field set to active script's settings object's Realm, and its ScriptOrModule set to active script's record.
            if (script) {
                script_execution_context = adopt_own(*new JS::ExecutionContext(vm->heap()));
                script_execution_context->function = nullptr;
                script_execution_context->realm = &script->settings_object().realm();
                VERIFY(script->script_record());
                script_execution_context->script_or_module = script->script_record()->make_weak_ptr();
            }

            // 5. Return the JobCallback Record { [[Callback]]: callable, [[HostDefined]]: { [[IncumbentSettings]]: incumbent settings, [[ActiveScriptContext]]: script execution context } }.
            auto host_defined = adopt_own(*new WebEngineCustomJobCallbackData(incumbent_settings, move(script_execution_context)));
            return { JS::make_handle(&callable), move(host_defined) };
        };

        // FIXME: Implement 8.1.5.5.1 HostGetImportMetaProperties(moduleRecord), https://html.spec.whatwg.org/multipage/webappapis.html#hostgetimportmetaproperties
        // FIXME: Implement 8.1.5.5.2 HostImportModuleDynamically(referencingScriptOrModule, moduleRequest, promiseCapability), https://html.spec.whatwg.org/multipage/webappapis.html#hostimportmoduledynamically(referencingscriptormodule,-modulerequest,-promisecapability)
        // FIXME: Implement 8.1.5.5.3 HostResolveImportedModule(referencingScriptOrModule, moduleRequest), https://html.spec.whatwg.org/multipage/webappapis.html#hostresolveimportedmodule(referencingscriptormodule,-modulerequest)
        // FIXME: Implement 8.1.5.5.4 HostGetSupportedImportAssertions(), https://html.spec.whatwg.org/multipage/webappapis.html#hostgetsupportedimportassertions

        vm->host_resolve_imported_module = [](JS::ScriptOrModule, JS::ModuleRequest const&) -> JS::ThrowCompletionOr<NonnullRefPtr<JS::Module>> {
            return vm->throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Modules in the browser");
        };

        // NOTE: We push a dummy execution context onto the JS execution context stack,
        //       just to make sure that it's never empty.
        auto& custom_data = *verify_cast<WebEngineCustomData>(vm->custom_data());
        custom_data.root_execution_context = make<JS::ExecutionContext>(vm->heap());
        vm->push_execution_context(*custom_data.root_execution_context);
    }
    return *vm;
}

// https://dom.spec.whatwg.org/#queue-a-mutation-observer-compound-microtask
void queue_mutation_observer_microtask(DOM::Document& document)
{
    auto& vm = main_thread_vm();
    auto& custom_data = verify_cast<WebEngineCustomData>(*vm.custom_data());

    // 1. If the surrounding agent’s mutation observer microtask queued is true, then return.
    if (custom_data.mutation_observer_microtask_queued)
        return;

    // 2. Set the surrounding agent’s mutation observer microtask queued to true.
    custom_data.mutation_observer_microtask_queued = true;

    // 3. Queue a microtask to notify mutation observers.
    // NOTE: This uses the implied document concept. In the case of mutation observers, it is always done in a node context, so document should be that node's document.
    // FIXME: Is it safe to pass custom_data through?
    HTML::queue_a_microtask(&document, [&custom_data]() {
        // 1. Set the surrounding agent’s mutation observer microtask queued to false.
        custom_data.mutation_observer_microtask_queued = false;

        // 2. Let notifySet be a clone of the surrounding agent’s mutation observers.
        auto notify_set = custom_data.mutation_observers;

        // FIXME: 3. Let signalSet be a clone of the surrounding agent’s signal slots.

        // FIXME: 4. Empty the surrounding agent’s signal slots.

        // 5. For each mo of notifySet:
        for (auto& mutation_observer : notify_set) {
            // 1. Let records be a clone of mo’s record queue.
            // 2. Empty mo’s record queue.
            auto records = mutation_observer.take_records();

            // 3. For each node of mo’s node list, remove all transient registered observers whose observer is mo from node’s registered observer list.
            for (auto& node : mutation_observer.node_list()) {
                // FIXME: Is this correct?
                if (node.is_null())
                    continue;

                node->registered_observers_list().remove_all_matching([&mutation_observer](DOM::RegisteredObserver& registered_observer) {
                    return is<DOM::TransientRegisteredObserver>(registered_observer) && static_cast<DOM::TransientRegisteredObserver&>(registered_observer).observer.ptr() == &mutation_observer;
                });
            }

            // 4. If records is not empty, then invoke mo’s callback with « records, mo », and mo. If this throws an exception, catch it, and report the exception.
            if (!records.is_empty()) {
                auto& callback = mutation_observer.callback();
                auto& realm = callback.callback_context.realm();

                auto* wrapped_records = MUST(JS::Array::create(realm, 0));
                for (size_t i = 0; i < records.size(); ++i) {
                    auto& record = records.at(i);
                    auto* wrapped_record = Bindings::wrap(realm, record);
                    auto property_index = JS::PropertyKey { i };
                    MUST(wrapped_records->create_data_property(property_index, wrapped_record));
                }

                auto* wrapped_mutation_observer = Bindings::wrap(realm, mutation_observer);

                auto result = IDL::invoke_callback(callback, wrapped_mutation_observer, wrapped_records, wrapped_mutation_observer);
                if (result.is_abrupt())
                    HTML::report_exception(result);
            }
        }

        // FIXME: 6. For each slot of signalSet, fire an event named slotchange, with its bubbles attribute set to true, at slot.
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-new-javascript-realm
NonnullOwnPtr<JS::ExecutionContext> create_a_new_javascript_realm(JS::VM& vm, Function<JS::GlobalObject*(JS::Realm&)> create_global_object, Function<JS::GlobalObject*(JS::Realm&)> create_global_this_value)
{
    // 1. Perform InitializeHostDefinedRealm() with the provided customizations for creating the global object and the global this binding.
    // 2. Let realm execution context be the running JavaScript execution context.
    auto realm_execution_context = MUST(JS::Realm::initialize_host_defined_realm(vm, move(create_global_object), move(create_global_this_value)));

    // 3. Remove realm execution context from the JavaScript execution context stack.
    vm.execution_context_stack().remove_first_matching([&realm_execution_context](auto* execution_context) {
        return execution_context == realm_execution_context.ptr();
    });

    // NO-OP: 4. Let realm be realm execution context's Realm component.
    // NO-OP: 5. Set realm's agent to agent.

    // FIXME: 6. If agent's agent cluster's cross-origin isolation mode is "none", then:
    //          1. Let global be realm's global object.
    //          2. Let status be ! global.[[Delete]]("SharedArrayBuffer").
    //          3. Assert: status is true.

    // 7. Return realm execution context.
    return realm_execution_context;
}

}
