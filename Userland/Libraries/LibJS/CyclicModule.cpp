/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/CyclicModule.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

CyclicModule::CyclicModule(Realm& realm, StringView filename, bool has_top_level_await, Vector<ModuleRequest> requested_modules)
    : Module(realm, filename)
    , m_requested_modules(move(requested_modules))
    , m_has_top_level_await(has_top_level_await)
{
}

// 16.2.1.5.1 Link ( ), https://tc39.es/ecma262/#sec-moduledeclarationlinking
ThrowCompletionOr<void> CyclicModule::link(VM& vm)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] link[{}]()", this);
    // 1. Assert: module.[[Status]] is not linking or evaluating.
    VERIFY(m_status != ModuleStatus::Linking && m_status != ModuleStatus::Evaluating);
    // 2. Let stack be a new empty List.
    Vector<Module*> stack;

    // 3. Let result be InnerModuleLinking(module, stack, 0).
    auto inner_module_linked_or_error = inner_module_linking(vm, stack, 0);

    // 4. If result is an abrupt completion, then
    if (inner_module_linked_or_error.is_error()) {
        // a. For each Cyclic Module Record m of stack, do
        for (auto* module : stack) {
            if (is<CyclicModule>(module)) {
                auto& cyclic_module = static_cast<CyclicModule&>(*module);
                // i. Assert: m.[[Status]] is linking.
                VERIFY(cyclic_module.m_status == ModuleStatus::Linking);

                // ii. Set m.[[Status]] to unlinked.
                cyclic_module.m_status = ModuleStatus::Unlinked;
            }
        }
        // b. Assert: module.[[Status]] is unlinked.
        VERIFY(m_status == ModuleStatus::Unlinked);

        // c. Return result.
        return inner_module_linked_or_error.release_error();
    }

    // 5. Assert: module.[[Status]] is linked, evaluating-async, or evaluated.
    VERIFY(m_status == ModuleStatus::Linked || m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated);
    // 6. Assert: stack is empty.
    VERIFY(stack.is_empty());

    // 7. Return undefined.
    // Note: We return void since the result of this is never used.
    return {};
}

// 16.2.1.5.1.1 InnerModuleLinking ( module, stack, index ), https://tc39.es/ecma262/#sec-InnerModuleLinking
ThrowCompletionOr<u32> CyclicModule::inner_module_linking(VM& vm, Vector<Module*>& stack, u32 index)
{
    // 1. If module is not a Cyclic Module Record, then
    //    a. Perform ? module.Link().
    //    b. Return index.
    // Note: Step 1, 1.a and 1.b are handled in Module.cpp

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] inner_module_linking[{}](vm, {}, {})", this, String::join(",", stack), index);

    // 2. If module.[[Status]] is linking, linked, evaluating-async, or evaluated, then
    if (m_status == ModuleStatus::Linking || m_status == ModuleStatus::Linked || m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated) {
        // a. Return index.
        return index;
    }

    // 3. Assert: module.[[Status]] is unlinked.
    VERIFY(m_status == ModuleStatus::Unlinked);

    // 4. Set module.[[Status]] to linking.
    m_status = ModuleStatus::Linking;

    // 5. Set module.[[DFSIndex]] to index.
    m_dfs_index = index;

    // 6. Set module.[[DFSAncestorIndex]] to index.
    m_dfs_ancestor_index = index;

    // 7. Set index to index + 1.
    ++index;

    // 8. Append module to stack.
    stack.append(this);

#if JS_MODULE_DEBUG
    StringBuilder request_module_names;
    for (auto& module_request : m_requested_modules) {
        request_module_names.append(module_request.module_specifier);
        request_module_names.append(", ");
    }
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] module: {} has requested modules: [{}]", filename(), request_module_names.string_view());
#endif

    // 9. For each String required of module.[[RequestedModules]], do
    for (auto& required_string : m_requested_modules) {
        ModuleRequest required { required_string };

        // a. Let requiredModule be ? HostResolveImportedModule(module, required).
        auto required_module = TRY(vm.host_resolve_imported_module(this->make_weak_ptr(), required));

        // b. Set index to ? InnerModuleLinking(requiredModule, stack, index).
        index = TRY(required_module->inner_module_linking(vm, stack, index));

        // c. If requiredModule is a Cyclic Module Record, then
        if (is<CyclicModule>(*required_module)) {
            auto& cyclic_module = static_cast<CyclicModule&>(*required_module);
            // i. Assert: requiredModule.[[Status]] is either linking, linked, evaluating-async, or evaluated.
            VERIFY(cyclic_module.m_status == ModuleStatus::Linking || cyclic_module.m_status == ModuleStatus::Linked || cyclic_module.m_status == ModuleStatus::EvaluatingAsync || cyclic_module.m_status == ModuleStatus::Evaluated);

            // ii. Assert: requiredModule.[[Status]] is linking if and only if requiredModule is in stack.
            VERIFY((cyclic_module.m_status == ModuleStatus::Linking) == (stack.contains_slow(&cyclic_module)));

            // iii. If requiredModule.[[Status]] is linking, then
            if (cyclic_module.m_status == ModuleStatus::Linking) {
                // 1. Set module.[[DFSAncestorIndex]] to min(module.[[DFSAncestorIndex]], requiredModule.[[DFSAncestorIndex]]).
                m_dfs_ancestor_index = min(m_dfs_ancestor_index.value(), cyclic_module.m_dfs_ancestor_index.value());
            }
        }
    }

    // 10. Perform ? module.InitializeEnvironment().
    (void)TRY(initialize_environment(vm));

    // 11. Assert: module occurs exactly once in stack.
    size_t count = 0;
    for (auto* module : stack) {
        if (module == this)
            count++;
    }
    VERIFY(count == 1);

    // 12. Assert: module.[[DFSAncestorIndex]] ≤ module.[[DFSIndex]].
    VERIFY(m_dfs_ancestor_index.value() <= m_dfs_index.value());

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] module {} after inner_linking has dfs {} and ancestor dfs {}", filename(), m_dfs_index.value(), m_dfs_ancestor_index.value());

    // 13. If module.[[DFSAncestorIndex]] = module.[[DFSIndex]], then
    if (m_dfs_ancestor_index == m_dfs_index) {
        // a. Let done be false.
        // b. Repeat, while done is false,
        while (true) {
            // i. Let requiredModule be the last element in stack.
            // ii. Remove the last element of stack.
            auto* required_module = stack.take_last();

            // iii. Assert: requiredModule is a Cyclic Module Record.
            VERIFY(is<CyclicModule>(*required_module));

            // iv. Set requiredModule.[[Status]] to linked.
            static_cast<CyclicModule&>(*required_module).m_status = ModuleStatus::Linked;

            // v. If requiredModule and module are the same Module Record, set done to true.
            if (required_module == this)
                break;
        }
    }

    // 14. Return index.
    return index;
}

// 16.2.1.5.2 Evaluate ( ), https://tc39.es/ecma262/#sec-moduleevaluation
ThrowCompletionOr<Promise*> CyclicModule::evaluate(VM& vm)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] evaluate[{}](vm)", this);
    // 1. Assert: This call to Evaluate is not happening at the same time as another call to Evaluate within the surrounding agent.
    // FIXME: Verify this somehow

    // 2. Assert: module.[[Status]] is linked, evaluating-async, or evaluated.
    VERIFY(m_status == ModuleStatus::Linked || m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated);

    // 3. If module.[[Status]] is evaluating-async or evaluated, set module to module.[[CycleRoot]].
    if (m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated) {
        // Note: This will continue this function with module.[[CycleRoot]]
        VERIFY(m_cycle_root && m_cycle_root->m_status == ModuleStatus::Linked && this != m_cycle_root);
        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] evaluate[{}](vm) deferring to cycle root at {}", this, m_cycle_root);
        return m_cycle_root->evaluate(vm);
    }

    // 4. If module.[[TopLevelCapability]] is not empty, then
    if (m_top_level_capability.has_value()) {
        // a. Return module.[[TopLevelCapability]].[[Promise]].
        VERIFY(is<Promise>(*m_top_level_capability->promise));
        return static_cast<Promise*>(m_top_level_capability->promise);
    }

    // 5. Let stack be a new empty List.
    Vector<Module*> stack;

    auto& global_object = realm().global_object();

    // 6. Let capability be ! NewPromiseCapability(%Promise%).
    // 7. Set module.[[TopLevelCapability]] to capability.
    m_top_level_capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 8. Let result be InnerModuleEvaluation(module, stack, 0).
    auto result = inner_module_evaluation(vm, stack, 0);

    // 9. If result is an abrupt completion, then
    if (result.is_throw_completion()) {
        VERIFY(!m_evaluation_error.is_error());

        // a. For each Cyclic Module Record m of stack, do
        for (auto* mod : stack) {
            if (!is<CyclicModule>(*mod))
                continue;

            auto& cyclic_module = static_cast<CyclicModule&>(*mod);

            // i. Assert: m.[[Status]] is evaluating.
            VERIFY(cyclic_module.m_status == ModuleStatus::Evaluating);

            // ii. Set m.[[Status]] to evaluated.
            cyclic_module.m_status = ModuleStatus::Evaluated;

            // iii. Set m.[[EvaluationError]] to result.
            cyclic_module.m_evaluation_error = result.throw_completion();
        }

        // b. Assert: module.[[Status]] is evaluated.
        VERIFY(m_status == ModuleStatus::Evaluated);

        // c. Assert: module.[[EvaluationError]] is result.
        VERIFY(m_evaluation_error.is_error() && same_value(*m_evaluation_error.throw_completion().value(), *result.throw_completion().value()));

        // d. Perform ! Call(capability.[[Reject]], undefined, « result.[[Value]] »).
        MUST(call(global_object, m_top_level_capability->reject, js_undefined(), *result.throw_completion().value()));
    }
    // 10. Else,
    else {
        // a. Assert: module.[[Status]] is evaluating-async or evaluated.
        VERIFY(m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated);
        // b. Assert: module.[[EvaluationError]] is empty.
        VERIFY(!m_evaluation_error.is_error());

        // c. If module.[[AsyncEvaluation]] is false, then
        if (!m_async_evaluation) {
            // i. Assert: module.[[Status]] is evaluated.
            VERIFY(m_status == ModuleStatus::Evaluated);
            // ii. Perform ! Call(capability.[[Resolve]], undefined, « undefined »).
            MUST(call(global_object, m_top_level_capability->resolve, js_undefined(), js_undefined()));
        }

        // d. Assert: stack is empty.
        VERIFY(stack.is_empty());
    }

    // 11. Return capability.[[Promise]].
    VERIFY(is<Promise>(*m_top_level_capability->promise));
    return static_cast<Promise*>(m_top_level_capability->promise);
}

// 16.2.1.5.2.1 InnerModuleEvaluation ( module, stack, index ), https://tc39.es/ecma262/#sec-innermoduleevaluation
ThrowCompletionOr<u32> CyclicModule::inner_module_evaluation(VM& vm, Vector<Module*>& stack, u32 index)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] inner_module_evaluation[{}](vm, {}, {})", this, String::join(", ", stack), index);
    // Note: Step 1 is performed in Module.cpp

    // 2. If module.[[Status]] is evaluating-async or evaluated, then
    if (m_status == ModuleStatus::EvaluatingAsync || m_status == ModuleStatus::Evaluated) {
        // a. If module.[[EvaluationError]] is empty, return index.
        if (!m_evaluation_error.is_error())
            return index;

        // b. Otherwise, return module.[[EvaluationError]].
        return m_evaluation_error.throw_completion();
    }

    // 3. If module.[[Status]] is evaluating, return index.
    if (m_status == ModuleStatus::Evaluating)
        return index;

    // 4. Assert: module.[[Status]] is linked.
    VERIFY(m_status == ModuleStatus::Linked);

    // 5. Set module.[[Status]] to evaluating.
    m_status = ModuleStatus::Evaluating;

    // 6. Set module.[[DFSIndex]] to index.
    m_dfs_index = index;

    // 7. Set module.[[DFSAncestorIndex]] to index.
    m_dfs_ancestor_index = index;

    // 8. Set module.[[PendingAsyncDependencies]] to 0.
    m_pending_async_dependencies = 0;

    // 9. Set index to index + 1.
    ++index;

    // 10. Append module to stack.
    stack.append(this);

    // 11. For each String required of module.[[RequestedModules]], do
    for (auto& required : m_requested_modules) {

        // a. Let requiredModule be ! HostResolveImportedModule(module, required).
        auto* required_module = MUST(vm.host_resolve_imported_module(this->make_weak_ptr(), required)).ptr();
        // b. NOTE: Link must be completed successfully prior to invoking this method, so every requested module is guaranteed to resolve successfully.

        // c. Set index to ? InnerModuleEvaluation(requiredModule, stack, index).
        index = TRY(required_module->inner_module_evaluation(vm, stack, index));

        // d. If requiredModule is a Cyclic Module Record, then
        if (!is<CyclicModule>(*required_module))
            continue;

        auto* cyclic_module = static_cast<CyclicModule*>(required_module);
        // i. Assert: requiredModule.[[Status]] is either evaluating, evaluating-async, or evaluated.
        VERIFY(cyclic_module->m_status == ModuleStatus::Evaluating || cyclic_module->m_status == ModuleStatus::EvaluatingAsync || cyclic_module->m_status == ModuleStatus::Evaluated);

        // ii. Assert: requiredModule.[[Status]] is evaluating if and only if requiredModule is in stack.
        VERIFY(cyclic_module->m_status != ModuleStatus::Evaluating || stack.contains_slow(cyclic_module));

        // iii. If requiredModule.[[Status]] is evaluating, then
        if (cyclic_module->m_status == ModuleStatus::Evaluating) {
            // 1. Set module.[[DFSAncestorIndex]] to min(module.[[DFSAncestorIndex]], requiredModule.[[DFSAncestorIndex]]).
            m_dfs_ancestor_index = min(m_dfs_ancestor_index.value(), cyclic_module->m_dfs_ancestor_index.value());
        }
        // iv. Else,
        else {
            // 1. Set requiredModule to requiredModule.[[CycleRoot]].
            cyclic_module = cyclic_module->m_cycle_root;

            // 2. Assert: requiredModule.[[Status]] is evaluating-async or evaluated.
            VERIFY(cyclic_module->m_status == ModuleStatus::EvaluatingAsync || cyclic_module->m_status == ModuleStatus::Evaluated);

            // 3. If requiredModule.[[EvaluationError]] is not empty, return requiredModule.[[EvaluationError]].
            if (cyclic_module->m_evaluation_error.is_error())
                return cyclic_module->m_evaluation_error.throw_completion();
        }

        // v. If requiredModule.[[AsyncEvaluation]] is true, then
        if (cyclic_module->m_async_evaluation) {
            // 1. Set module.[[PendingAsyncDependencies]] to module.[[PendingAsyncDependencies]] + 1.
            ++m_pending_async_dependencies.value();

            // 2. Append module to requiredModule.[[AsyncParentModules]].
            cyclic_module->m_async_parent_modules.append(this);
        }
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] inner_module_evaluation on {} has tla: {} and pending async dep: {} dfs: {} ancestor dfs: {}", filename(), m_has_top_level_await, m_pending_async_dependencies.value(), m_dfs_index.value(), m_dfs_ancestor_index.value());
    // 12. If module.[[PendingAsyncDependencies]] > 0 or module.[[HasTLA]] is true, then
    if (m_pending_async_dependencies.value() > 0 || m_has_top_level_await) {
        // a. Assert: module.[[AsyncEvaluation]] is false and was never previously set to true.
        VERIFY(!m_async_evaluation); // FIXME: I don't think we can check previously?

        // b. Set module.[[AsyncEvaluation]] to true.
        m_async_evaluation = true;
        // c. NOTE: The order in which module records have their [[AsyncEvaluation]] fields transition to true is significant. (See 16.2.1.5.2.4.)

        // d. If module.[[PendingAsyncDependencies]] is 0, perform ! ExecuteAsyncModule(module).
        if (m_pending_async_dependencies.value() == 0)
            MUST(execute_async_module(vm));
    }
    // 13. Otherwise, perform ? module.ExecuteModule().
    else {
        (void)TRY(execute_module(vm));
    }

    // 14. Assert: module occurs exactly once in stack.
    auto count = 0;
    for (auto* module : stack) {
        if (module == this)
            count++;
    }
    VERIFY(count == 1);

    // 15. Assert: module.[[DFSAncestorIndex]] ≤ module.[[DFSIndex]].
    VERIFY(m_dfs_ancestor_index.value() <= m_dfs_index.value());

    // 16. If module.[[DFSAncestorIndex]] = module.[[DFSIndex]], then
    if (m_dfs_ancestor_index == m_dfs_index) {
        // a. Let done be false.
        bool done = false;
        // b. Repeat, while done is false,
        while (!done) {

            // i. Let requiredModule be the last element in stack.
            // ii. Remove the last element of stack.
            auto* required_module = stack.take_last();

            // iii. Assert: requiredModule is a Cyclic Module Record.
            VERIFY(is<CyclicModule>(*required_module));

            auto& cyclic_module = static_cast<CyclicModule&>(*required_module);

            // iv. If requiredModule.[[AsyncEvaluation]] is false, set requiredModule.[[Status]] to evaluated.
            if (!cyclic_module.m_async_evaluation)
                cyclic_module.m_status = ModuleStatus::Evaluated;
            // v. Otherwise, set requiredModule.[[Status]] to evaluating-async.
            else
                cyclic_module.m_status = ModuleStatus::EvaluatingAsync;

            // vi. If requiredModule and module are the same Module Record, set done to true.
            if (required_module == this)
                done = true;

            // vii. Set requiredModule.[[CycleRoot]] to module.
            cyclic_module.m_cycle_root = this;
        }
    }

    // 17. Return index.
    return index;
}

Completion CyclicModule::initialize_environment(VM&)
{
    // Note: In ecma262 this is never called on a cyclic module only on SourceTextModules.
    //       So this check is to make sure we don't accidentally call this.
    VERIFY_NOT_REACHED();
    return normal_completion({});
}

Completion CyclicModule::execute_module(VM&, Optional<PromiseCapability>)
{
    // Note: In ecma262 this is never called on a cyclic module only on SourceTextModules.
    //       So this check is to make sure we don't accidentally call this.
    VERIFY_NOT_REACHED();
    return js_undefined();
}

// 16.2.1.5.2.2 ExecuteAsyncModule ( module ), https://tc39.es/ecma262/#sec-execute-async-module
ThrowCompletionOr<void> CyclicModule::execute_async_module(VM& vm)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] executing async module {}", filename());
    // 1. Assert: module.[[Status]] is evaluating or evaluating-async.
    VERIFY(m_status == ModuleStatus::Evaluating || m_status == ModuleStatus::EvaluatingAsync);
    // 2. Assert: module.[[HasTLA]] is true.
    VERIFY(m_has_top_level_await);

    auto& global_object = realm().global_object();

    // 3. Let capability be ! NewPromiseCapability(%Promise%).
    auto capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 4. Let fulfilledClosure be a new Abstract Closure with no parameters that captures module and performs the following steps when called:
    auto fulfilled_closure = [&](VM& vm, GlobalObject&) -> ThrowCompletionOr<Value> {
        // a. Perform ! AsyncModuleExecutionFulfilled(module).
        MUST(async_module_execution_fulfilled(vm));

        // b. Return undefined.
        return js_undefined();
    };

    // 5. Let onFulfilled be ! CreateBuiltinFunction(fulfilledClosure, 0, "", « »).
    auto* on_fulfilled = NativeFunction::create(global_object, "", move(fulfilled_closure));

    // 6. Let rejectedClosure be a new Abstract Closure with parameters (error) that captures module and performs the following steps when called:
    auto rejected_closure = [&](VM& vm, GlobalObject&) -> ThrowCompletionOr<Value> {
        auto error = vm.argument(0);

        // a. Perform ! AsyncModuleExecutionRejected(module, error).
        MUST(async_module_execution_rejected(vm, error));

        // b. Return undefined.
        return js_undefined();
    };

    auto* on_rejected = NativeFunction::create(global_object, "", move(rejected_closure));
    // 7. Let onRejected be ! CreateBuiltinFunction(rejectedClosure, 0, "", « »).

    VERIFY(is<Promise>(*capability.promise));

    // 8. Perform ! PerformPromiseThen(capability.[[Promise]], onFulfilled, onRejected).
    static_cast<Promise*>(capability.promise)->perform_then(on_fulfilled, on_rejected, {});

    // 9. Perform ! module.ExecuteModule(capability).
    (void)MUST(execute_module(vm, capability));

    return {};
}

// 16.2.1.5.2.3 GatherAvailableAncestors ( module, execList ), https://tc39.es/ecma262/#sec-gather-available-ancestors
ThrowCompletionOr<void> CyclicModule::gather_available_ancestors(Vector<CyclicModule*>& exec_list)
{
    // 1. For each Cyclic Module Record m of module.[[AsyncParentModules]], do
    for (auto* module : m_async_parent_modules) {
        // a. If execList does not contain m and m.[[CycleRoot]].[[EvaluationError]] is empty, then
        if (!exec_list.contains_slow(module) && !module->m_cycle_root->m_evaluation_error.is_error()) {
            // i. Assert: m.[[Status]] is evaluating-async.
            VERIFY(module->m_status == ModuleStatus::EvaluatingAsync);

            // ii. Assert: m.[[EvaluationError]] is empty.
            VERIFY(!module->m_evaluation_error.is_error());

            // iii. Assert: m.[[AsyncEvaluation]] is true.
            VERIFY(module->m_async_evaluation);

            // iv. Assert: m.[[PendingAsyncDependencies]] > 0.
            VERIFY(module->m_pending_async_dependencies.value() > 0);

            // v. Set m.[[PendingAsyncDependencies]] to m.[[PendingAsyncDependencies]] - 1.
            module->m_pending_async_dependencies.value()--;

            // vi. If m.[[PendingAsyncDependencies]] = 0, then
            if (module->m_pending_async_dependencies.value() == 0) {
                // 1. Append m to execList.
                exec_list.append(module);

                // 2. If m.[[HasTLA]] is false, perform ! GatherAvailableAncestors(m, execList).
                if (!module->m_has_top_level_await)
                    MUST(module->gather_available_ancestors(exec_list));
            }
        }
    }

    return {};
}

// 16.2.1.5.2.4 AsyncModuleExecutionFulfilled ( module ), https://tc39.es/ecma262/#sec-async-module-execution-fulfilled
ThrowCompletionOr<void> CyclicModule::async_module_execution_fulfilled(VM& vm)
{
    // 1. If module.[[Status]] is evaluated, then
    if (m_status == ModuleStatus::Evaluated) {
        // a. Assert: module.[[EvaluationError]] is not empty.
        VERIFY(m_evaluation_error.is_error());
        // b. Return.
        return {};
    }

    // 2. Assert: module.[[Status]] is evaluating-async.
    VERIFY(m_status == ModuleStatus::EvaluatingAsync);

    // 3. Assert: module.[[AsyncEvaluation]] is true.
    VERIFY(m_async_evaluation);

    // 4. Assert: module.[[EvaluationError]] is empty.
    VERIFY(!m_evaluation_error.is_error());

    // 5. Set module.[[AsyncEvaluation]] to false.
    m_async_evaluation = false;

    // 6. Set module.[[Status]] to evaluated.
    m_status = ModuleStatus::Evaluated;

    // 7. If module.[[TopLevelCapability]] is not empty, then
    if (m_top_level_capability.has_value()) {
        // a. Assert: module.[[CycleRoot]] is module.
        VERIFY(m_cycle_root == this);

        VERIFY(vm.current_realm());
        // b. Perform ! Call(module.[[TopLevelCapability]].[[Resolve]], undefined, « undefined »).
        MUST(call(vm.current_realm()->global_object(), m_top_level_capability->resolve, js_undefined(), js_undefined()));
    }

    // 8. Let execList be a new empty List.
    Vector<CyclicModule*> exec_list;

    // 9. Perform ! GatherAvailableAncestors(module, execList).
    MUST(gather_available_ancestors(exec_list));

    // 10. Let sortedExecList be a List whose elements are the elements of execList, in the order in which they had their [[AsyncEvaluation]] fields set to true in InnerModuleEvaluation.
    // FIXME: Sort the list. To do this we need to use more than an Optional<bool> to track [[AsyncEvaluation]].

    // 11. Assert: All elements of sortedExecList have their [[AsyncEvaluation]] field set to true, [[PendingAsyncDependencies]] field set to 0, and [[EvaluationError]] field set to empty.
    VERIFY(all_of(exec_list, [&](CyclicModule* module) { return module->m_async_evaluation && module->m_pending_async_dependencies.value() == 0 && !module->m_evaluation_error.is_error(); }));

    // 12. For each Cyclic Module Record m of sortedExecList, do
    for (auto* module : exec_list) {
        // a. If m.[[Status]] is evaluated, then
        if (module->m_status == ModuleStatus::Evaluated) {
            // i. Assert: m.[[EvaluationError]] is not empty.
            VERIFY(module->m_evaluation_error.is_error());
        }
        // b. Else if m.[[HasTLA]] is true, then
        else if (module->m_has_top_level_await) {
            // i. Perform ! ExecuteAsyncModule(m).
            MUST(module->execute_async_module(vm));
        }
        // c. Else,
        else {
            // i. Let result be m.ExecuteModule().
            auto result = module->execute_module(vm);

            // ii. If result is an abrupt completion, then
            if (result.is_abrupt()) {
                // 1. Perform ! AsyncModuleExecutionRejected(m, result.[[Value]]).
                module->async_module_execution_rejected(vm, *result.value());
            }
            // iii. Else,
            else {
                // 1. Set m.[[Status]] to evaluated.
                module->m_status = ModuleStatus::Evaluated;

                // 2. If m.[[TopLevelCapability]] is not empty, then
                if (module->m_top_level_capability.has_value()) {
                    // a. Assert: m.[[CycleRoot]] is m.
                    VERIFY(module->m_cycle_root == module);

                    VERIFY(vm.current_realm());
                    // b. Perform ! Call(m.[[TopLevelCapability]].[[Resolve]], undefined, « undefined »).
                    MUST(call(vm.current_realm()->global_object(), module->m_top_level_capability->resolve, js_undefined(), js_undefined()));
                }
            }
        }
    }
    return {};
}

// 16.2.1.5.2.5 AsyncModuleExecutionRejected ( module, error ), https://tc39.es/ecma262/#sec-async-module-execution-rejected
ThrowCompletionOr<void> CyclicModule::async_module_execution_rejected(VM& vm, Value error)
{
    // 1. If module.[[Status]] is evaluated, then
    if (m_status == ModuleStatus::Evaluated) {
        // a. Assert: module.[[EvaluationError]] is not empty.
        VERIFY(m_evaluation_error.is_error());
        // b. Return.
        return {};
    }

    // 2. Assert: module.[[Status]] is evaluating-async.
    VERIFY(m_status == ModuleStatus::EvaluatingAsync);

    // 3. Assert: module.[[AsyncEvaluation]] is true.
    VERIFY(m_async_evaluation);

    // 4. Assert: module.[[EvaluationError]] is empty.
    VERIFY(!m_evaluation_error.is_error());

    // 5. Set module.[[EvaluationError]] to ThrowCompletion(error)
    m_evaluation_error = throw_completion(error);

    // 6. Set module.[[Status]] to evaluated.
    m_status = ModuleStatus::Evaluated;

    // 7. For each Cyclic Module Record m of module.[[AsyncParentModules]], do
    for (auto* module : m_async_parent_modules) {

        // a. Perform ! AsyncModuleExecutionRejected(m, error).
        MUST(module->async_module_execution_rejected(vm, error));
    }

    // 8. If module.[[TopLevelCapability]] is not empty, then
    if (m_top_level_capability.has_value()) {
        // a. Assert: module.[[CycleRoot]] is module.
        VERIFY(m_cycle_root == this);

        VERIFY(vm.current_realm());
        // b. Perform ! Call(module.[[TopLevelCapability]].[[Reject]], undefined, « error »).
        MUST(call(vm.current_realm()->global_object(), m_top_level_capability->reject, js_undefined(), error));
    }

    return {};
}

}
