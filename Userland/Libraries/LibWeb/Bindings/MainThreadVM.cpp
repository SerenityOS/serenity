/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Module.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/ModuleRequest.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Bindings/WindowExposedInterfaces.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Location.h>
#include <LibWeb/HTML/PromiseRejectionEvent.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/ExceptionReporter.h>
#include <LibWeb/HTML/Scripting/Fetching.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowProxy.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

namespace Web::Bindings {

// https://html.spec.whatwg.org/multipage/webappapis.html#active-script
HTML::Script* active_script()
{
    // 1. Let record be GetActiveScriptOrModule().
    auto record = main_thread_vm().get_active_script_or_module();

    // 2. If record is null, return null.
    // 3. Return record.[[HostDefined]].
    return record.visit(
        [](JS::NonnullGCPtr<JS::Script>& js_script) -> HTML::Script* {
            return verify_cast<HTML::ClassicScript>(js_script->host_defined());
        },
        [](JS::NonnullGCPtr<JS::Module>& js_module) -> HTML::Script* {
            return verify_cast<HTML::ModuleScript>(js_module->host_defined());
        },
        [](Empty) -> HTML::Script* {
            return nullptr;
        });
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
            if (is<HTML::WindowProxy>(object) || is<HTML::Location>(object))
                return vm->throw_completion<JS::TypeError>("Cannot add private elements to window or location object"sv);

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
                [&script](JS::NonnullGCPtr<JS::Script>& js_script) {
                    script = verify_cast<HTML::ClassicScript>(js_script->host_defined());
                },
                [&script](JS::NonnullGCPtr<JS::Module>& js_module) {
                    script = verify_cast<HTML::ModuleScript>(js_module->host_defined());
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
                settings_object.push_onto_about_to_be_notified_rejected_promises_list(promise);
                break;
            case JS::Promise::RejectionOperation::Handle: {
                // 5. If operation is "handle",
                //    1. If settings object's about-to-be-notified rejected promises list contains promise, then remove promise from that list and return.
                bool removed_about_to_be_notified_rejected_promise = settings_object.remove_from_about_to_be_notified_rejected_promises_list(promise);
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
                HTML::queue_global_task(HTML::Task::Source::DOMManipulation, global, [&global, &promise] {
                    // FIXME: This currently assumes that global is a WindowObject.
                    auto& window = verify_cast<HTML::Window>(global);

                    HTML::PromiseRejectionEventInit event_init {
                        {}, // Initialize the inherited DOM::EventInit
                        /* .promise = */ promise,
                        /* .reason = */ promise.result(),
                    };
                    auto promise_rejection_event = HTML::PromiseRejectionEvent::create(HTML::relevant_realm(global), HTML::EventNames::rejectionhandled, event_init).release_value_but_fixme_should_propagate_errors();
                    window.dispatch_event(promise_rejection_event);
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
        vm->host_enqueue_finalization_registry_cleanup_job = [](JS::FinalizationRegistry& finalization_registry) {
            // 1. Let global be finalizationRegistry.[[Realm]]'s global object.
            auto& global = finalization_registry.realm().global_object();

            // 2. Queue a global task on the JavaScript engine task source given global to perform the following steps:
            HTML::queue_global_task(HTML::Task::Source::JavaScriptEngine, global, [&finalization_registry] {
                // 1. Let entry be finalizationRegistry.[[CleanupCallback]].[[Callback]].[[Realm]]'s environment settings object.
                auto& entry = host_defined_environment_settings_object(*finalization_registry.cleanup_callback().callback.cell()->realm());

                // 2. Check if we can run script with entry. If this returns "do not run", then return.
                if (entry.can_run_script() == HTML::RunScriptDecision::DoNotRun)
                    return;

                // 3. Prepare to run script with entry.
                entry.prepare_to_run_script();

                // 4. Let result be the result of performing CleanupFinalizationRegistry(finalizationRegistry).
                auto result = finalization_registry.cleanup();

                // 5. Clean up after running script with entry.
                entry.clean_up_after_running_script();

                // 6. If result is an abrupt completion, then report the exception given by result.[[Value]].
                if (result.is_error())
                    HTML::report_exception(result, finalization_registry.realm());
            });
        };

        // 8.1.5.4.3 HostEnqueuePromiseJob(job, realm), https://html.spec.whatwg.org/multipage/webappapis.html#hostenqueuepromisejob
        vm->host_enqueue_promise_job = [](Function<JS::ThrowCompletionOr<JS::Value>()> job, JS::Realm* realm) {
            // 1. If realm is not null, then let job settings be the settings object for realm. Otherwise, let job settings be null.
            HTML::EnvironmentSettingsObject* job_settings { nullptr };
            if (realm)
                job_settings = &host_defined_environment_settings_object(*realm);

            // IMPLEMENTATION DEFINED: The JS spec says we must take implementation defined steps to make the currently active script or module at the time of HostEnqueuePromiseJob being invoked
            //                         also be the active script or module of the job at the time of its invocation.
            //                         This means taking it here now and passing it through to the lambda.
            auto script_or_module = vm->get_active_script_or_module();

            // 2. Queue a microtask on the surrounding agent's event loop to perform the following steps:
            // This instance of "queue a microtask" uses the "implied document". The best fit for "implied document" here is "If the task is being queued by or for a script, then return the script's settings object's responsible document."
            // Do note that "implied document" from the spec is handwavy and the spec authors are trying to get rid of it: https://github.com/whatwg/html/issues/4980
            auto* script = active_script();

            // NOTE: This keeps job_settings alive by keeping realm alive, which is holding onto job_settings.
            HTML::queue_a_microtask(script ? script->settings_object().responsible_document().ptr() : nullptr, [job_settings, job = move(job), script_or_module = move(script_or_module)] {
                // The dummy execution context has to be kept up here to keep it alive for the duration of the function.
                Optional<JS::ExecutionContext> dummy_execution_context;

                if (job_settings) {
                    // 1. If job settings is not null, then check if we can run script with job settings. If this returns "do not run" then return.
                    if (job_settings->can_run_script() == HTML::RunScriptDecision::DoNotRun)
                        return;

                    // 2. If job settings is not null, then prepare to run script with job settings.
                    job_settings->prepare_to_run_script();

                    // IMPLEMENTATION DEFINED: Additionally to preparing to run a script, we also prepare to run a callback here. This matches WebIDL's
                    //                         invoke_callback() / call_user_object_operation() functions, and prevents a crash in host_make_job_callback()
                    //                         when getting the incumbent settings object.
                    job_settings->prepare_to_run_callback();

                    // IMPLEMENTATION DEFINED: Per the previous "implementation defined" comment, we must now make the script or module the active script or module.
                    //                         Since the only active execution context currently is the realm execution context of job settings, lets attach it here.
                    job_settings->realm_execution_context().script_or_module = script_or_module;
                } else {
                    // FIXME: We need to setup a dummy execution context in case a JS::NativeFunction is called when processing the job.
                    //        This is because JS::NativeFunction::call excepts something to be on the execution context stack to be able to get the caller context to initialize the environment.
                    //        Do note that the JS spec gives _no_ guarantee that the execution context stack has something on it if HostEnqueuePromiseJob was called with a null realm: https://tc39.es/ecma262/#job-preparedtoevaluatecode
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

                    // IMPLEMENTATION DEFINED: See comment above, we need to clean up the non-standard prepare_to_run_callback() call.
                    job_settings->clean_up_after_running_callback();

                    job_settings->clean_up_after_running_script();
                } else {
                    // Pop off the dummy execution context. See the above FIXME block about why this is done.
                    vm->pop_execution_context();
                }

                // 5. If result is an abrupt completion, then report the exception given by result.[[Value]].
                if (result.is_error())
                    HTML::report_exception(result, job_settings->realm());
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
                if (is<HTML::ClassicScript>(script)) {
                    script_execution_context->script_or_module = JS::NonnullGCPtr<JS::Script>(*verify_cast<HTML::ClassicScript>(script)->script_record());
                } else if (is<HTML::ModuleScript>(script)) {
                    if (is<HTML::JavaScriptModuleScript>(script)) {
                        script_execution_context->script_or_module = JS::NonnullGCPtr<JS::Module>(*verify_cast<HTML::JavaScriptModuleScript>(script)->record());
                    } else {
                        // NOTE: Handle CSS and JSON module scripts once we have those.
                        VERIFY_NOT_REACHED();
                    }
                } else {
                    VERIFY_NOT_REACHED();
                }
            }

            // 5. Return the JobCallback Record { [[Callback]]: callable, [[HostDefined]]: { [[IncumbentSettings]]: incumbent settings, [[ActiveScriptContext]]: script execution context } }.
            auto host_defined = adopt_own(*new WebEngineCustomJobCallbackData(incumbent_settings, move(script_execution_context)));
            return { JS::make_handle(&callable), move(host_defined) };
        };

        // FIXME: Implement 8.1.5.5.1 HostGetImportMetaProperties(moduleRecord), https://html.spec.whatwg.org/multipage/webappapis.html#hostgetimportmetaproperties
        // FIXME: Implement 8.1.5.5.2 HostImportModuleDynamically(referencingScriptOrModule, moduleRequest, promiseCapability), https://html.spec.whatwg.org/multipage/webappapis.html#hostimportmoduledynamically(referencingscriptormodule,-modulerequest,-promisecapability)
        // FIXME: Implement 8.1.5.5.3 HostResolveImportedModule(referencingScriptOrModule, moduleRequest), https://html.spec.whatwg.org/multipage/webappapis.html#hostresolveimportedmodule(referencingscriptormodule,-modulerequest)

        // 8.1.5.5.4 HostGetSupportedImportAssertions(), https://html.spec.whatwg.org/multipage/webappapis.html#hostgetsupportedimportassertions
        vm->host_get_supported_import_assertions = []() -> Vector<DeprecatedString> {
            // 1. Return « "type" ».
            return { "type"sv };
        };

        // 8.1.6.5.3 HostResolveImportedModule(referencingScriptOrModule, moduleRequest), https://html.spec.whatwg.org/multipage/webappapis.html#hostresolveimportedmodule(referencingscriptormodule,-modulerequest)
        vm->host_resolve_imported_module = [](JS::ScriptOrModule const& referencing_string_or_module, JS::ModuleRequest const& module_request) -> JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Module>> {
            // 1. Let moduleMap and referencingScript be null.
            Optional<HTML::ModuleMap&> module_map;
            Optional<HTML::Script&> referencing_script;

            // 2. If referencingScriptOrModule is not null, then:
            if (!referencing_string_or_module.has<Empty>()) {
                // 1. Set referencingScript to referencingScriptOrModule.[[HostDefined]].
                referencing_script = verify_cast<HTML::Script>(referencing_string_or_module.has<JS::NonnullGCPtr<JS::Script>>() ? *referencing_string_or_module.get<JS::NonnullGCPtr<JS::Script>>()->host_defined() : *referencing_string_or_module.get<JS::NonnullGCPtr<JS::Module>>()->host_defined());

                // 2. Set moduleMap to referencingScript's settings object's module map.
                module_map = referencing_script->settings_object().module_map();
            }
            // 3. Otherwise:
            else {
                // 1. Assert: there is a current settings object.
                // NOTE: This is handled by the HTML::current_settings_object() accessor.

                // 2. Set moduleMap to the current settings object's module map.
                module_map = HTML::current_settings_object().module_map();
            }

            // 4. Let url be the result of resolving a module specifier given referencingScript and moduleRequest.[[Specifier]].
            auto url = MUST(HTML::resolve_module_specifier(referencing_script, module_request.module_specifier));

            // 5. Assert: the previous step never throws an exception, because resolving a module specifier must have been previously successful
            //    with these same two arguments (either while creating the corresponding module script, or in fetch an import() module script graph).
            // NOTE: Handled by MUST above.

            // 6. Let moduleType be the result of running the module type from module request steps given moduleRequest.
            auto module_type = HTML::module_type_from_module_request(module_request);

            // 7. Let resolvedModuleScript be moduleMap[(url, moduleType)]. (This entry must exist for us to have gotten to this point.)
            auto resolved_module_script = module_map->get(url, module_type).value();

            // 8. Assert: resolvedModuleScript is a module script (i.e., is not null or "fetching").
            VERIFY(resolved_module_script.type == HTML::ModuleMap::EntryType::ModuleScript);

            // 9. Assert: resolvedModuleScript's record is not null.
            VERIFY(resolved_module_script.module_script->record());

            // 10. Return resolvedModuleScript's record.
            return JS::NonnullGCPtr(*resolved_module_script.module_script->record());
        };

        // NOTE: We push a dummy execution context onto the JS execution context stack,
        //       just to make sure that it's never empty.
        auto& custom_data = *verify_cast<WebEngineCustomData>(vm->custom_data());
        custom_data.root_execution_context = MUST(JS::Realm::initialize_host_defined_realm(*vm, nullptr, nullptr));

        auto* root_realm = custom_data.root_execution_context->realm;
        auto intrinsics = root_realm->heap().allocate<Intrinsics>(*root_realm, *root_realm).release_allocated_value_but_fixme_should_propagate_errors();
        auto host_defined = make<HostDefined>(nullptr, intrinsics);
        root_realm->set_host_defined(move(host_defined));
        custom_data.internal_realm = root_realm;

        // NOTE: We make sure the internal realm has all the Window intrinsics initialized.
        //       The DeferGC is a hack to avoid nested GC allocations due to lazy ensure_web_prototype()
        //       and ensure_web_constructor() invocations.
        // FIXME: Find a nicer way to do this.
        JS::DeferGC defer_gc(root_realm->heap());
        auto object = JS::Object::create(*root_realm, nullptr);
        root_realm->set_global_object(object, object);
        add_window_exposed_interfaces(*object);

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
            auto records = mutation_observer->take_records();

            // 3. For each node of mo’s node list, remove all transient registered observers whose observer is mo from node’s registered observer list.
            for (auto& node : mutation_observer->node_list()) {
                // FIXME: Is this correct?
                if (node.is_null())
                    continue;

                node->registered_observers_list().remove_all_matching([&mutation_observer](DOM::RegisteredObserver& registered_observer) {
                    return is<DOM::TransientRegisteredObserver>(registered_observer) && static_cast<DOM::TransientRegisteredObserver&>(registered_observer).observer().ptr() == mutation_observer.ptr();
                });
            }

            // 4. If records is not empty, then invoke mo’s callback with « records, mo », and mo. If this throws an exception, catch it, and report the exception.
            if (!records.is_empty()) {
                auto& callback = mutation_observer->callback();
                auto& realm = callback.callback_context.realm();

                auto wrapped_records = MUST(JS::Array::create(realm, 0));
                for (size_t i = 0; i < records.size(); ++i) {
                    auto& record = records.at(i);
                    auto property_index = JS::PropertyKey { i };
                    MUST(wrapped_records->create_data_property(property_index, record.ptr()));
                }

                auto result = WebIDL::invoke_callback(callback, mutation_observer.ptr(), wrapped_records, mutation_observer.ptr());
                if (result.is_abrupt())
                    HTML::report_exception(result, realm);
            }
        }

        // FIXME: 6. For each slot of signalSet, fire an event named slotchange, with its bubbles attribute set to true, at slot.
    });
}

// https://html.spec.whatwg.org/multipage/webappapis.html#creating-a-new-javascript-realm
NonnullOwnPtr<JS::ExecutionContext> create_a_new_javascript_realm(JS::VM& vm, Function<JS::Object*(JS::Realm&)> create_global_object, Function<JS::Object*(JS::Realm&)> create_global_this_value)
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

void WebEngineCustomData::spin_event_loop_until(Function<bool()> goal_condition)
{
    Platform::EventLoopPlugin::the().spin_until(move(goal_condition));
}

}
