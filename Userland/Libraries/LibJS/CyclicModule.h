/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Module.h>

namespace JS {

enum class ModuleStatus {
    Unlinked,
    Linking,
    Linked,
    Evaluating,
    EvaluatingAsync,
    Evaluated
};

// 16.2.1.5 Cyclic Module Records, https://tc39.es/ecma262/#sec-cyclic-module-records
class CyclicModule : public Module {
    JS_CELL(CyclicModule, Module);

public:
    // Note: Do not call these methods directly unless you are HostResolveImportedModule.
    //       Badges cannot be used because other hosts must be able to call this (and it is called recursively)
    virtual ThrowCompletionOr<void> link(VM& vm) override final;
    virtual ThrowCompletionOr<Promise*> evaluate(VM& vm) override final;

    Vector<ModuleRequest> const& requested_modules() const { return m_requested_modules; }

protected:
    CyclicModule(Realm& realm, StringView filename, bool has_top_level_await, Vector<ModuleRequest> requested_modules, Script::HostDefined* host_defined);

    virtual void visit_edges(Cell::Visitor&) override;

    virtual ThrowCompletionOr<u32> inner_module_linking(VM& vm, Vector<Module*>& stack, u32 index) override final;
    virtual ThrowCompletionOr<u32> inner_module_evaluation(VM& vm, Vector<Module*>& stack, u32 index) override final;

    virtual ThrowCompletionOr<void> initialize_environment(VM& vm);
    virtual ThrowCompletionOr<void> execute_module(VM& vm, GCPtr<PromiseCapability> capability = {});

    void execute_async_module(VM& vm);
    void gather_available_ancestors(Vector<CyclicModule*>& exec_list);
    void async_module_execution_fulfilled(VM& vm);
    void async_module_execution_rejected(VM& vm, Value error);

    ModuleStatus m_status { ModuleStatus::Unlinked };   // [[Status]]
    ThrowCompletionOr<void> m_evaluation_error;         // [[EvaluationError]]
    Optional<u32> m_dfs_index;                          // [[DFSIndex]]
    Optional<u32> m_dfs_ancestor_index;                 // [[DFSAncestorIndex]]
    Vector<ModuleRequest> m_requested_modules;          // [[RequestedModules]]
    GCPtr<CyclicModule> m_cycle_root;                   // [[CycleRoot]]
    bool m_has_top_level_await { false };               // [[HasTLA]]
    bool m_async_evaluation { false };                  // [[AsyncEvaluation]]
    GCPtr<PromiseCapability> m_top_level_capability;    // [[TopLevelCapability]]
    Vector<GCPtr<CyclicModule>> m_async_parent_modules; // [[AsyncParentModules]]
    Optional<u32> m_pending_async_dependencies;         // [[PendingAsyncDependencies]]
};

}
