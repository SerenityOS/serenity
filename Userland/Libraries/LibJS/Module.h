/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/ModuleLoading.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Script.h>

namespace JS {

struct ResolvedBinding {
    enum Type {
        BindingName,
        Namespace,
        Ambiguous,
        Null,
    };

    static ResolvedBinding null()
    {
        return {};
    }

    static ResolvedBinding ambiguous()
    {
        ResolvedBinding binding;
        binding.type = Ambiguous;
        return binding;
    }

    Type type { Null };
    GCPtr<Module> module;
    DeprecatedFlyString export_name;

    bool is_valid() const
    {
        return type == BindingName || type == Namespace;
    }

    bool is_namespace() const
    {
        return type == Namespace;
    }

    bool is_ambiguous() const
    {
        return type == Ambiguous;
    }
};

// https://tc39.es/ecma262/#graphloadingstate-record
struct GraphLoadingState : public Cell {
    JS_CELL(GraphLoadingState, Cell);
    JS_DECLARE_ALLOCATOR(GraphLoadingState);

public:
    struct HostDefined : Cell {
        JS_CELL(HostDefined, Cell);

    public:
        virtual ~HostDefined() = default;
    };

    GCPtr<PromiseCapability> promise_capability; // [[PromiseCapability]]
    bool is_loading { false };                   // [[IsLoading]]
    size_t pending_module_count { 0 };           // [[PendingModulesCount]]
    HashTable<JS::GCPtr<CyclicModule>> visited;  // [[Visited]]
    GCPtr<HostDefined> host_defined;             // [[HostDefined]]

private:
    GraphLoadingState(GCPtr<PromiseCapability> promise_capability, bool is_loading, size_t pending_module_count, HashTable<JS::GCPtr<CyclicModule>> visited, GCPtr<HostDefined> host_defined)
        : promise_capability(move(promise_capability))
        , is_loading(is_loading)
        , pending_module_count(pending_module_count)
        , visited(move(visited))
        , host_defined(move(host_defined))
    {
    }
    virtual void visit_edges(Cell::Visitor&) override;
};

// 16.2.1.4 Abstract Module Records, https://tc39.es/ecma262/#sec-abstract-module-records
class Module : public Cell {
    JS_CELL(Module, Cell);
    JS_DECLARE_ALLOCATOR(Module);

public:
    virtual ~Module() override;

    Realm& realm() { return *m_realm; }
    Realm const& realm() const { return *m_realm; }

    StringView filename() const { return m_filename; }

    Environment* environment() { return m_environment; }

    Script::HostDefined* host_defined() const { return m_host_defined; }

    ThrowCompletionOr<Object*> get_module_namespace(VM& vm);

    virtual ThrowCompletionOr<void> link(VM& vm) = 0;
    virtual ThrowCompletionOr<Promise*> evaluate(VM& vm) = 0;

    virtual ThrowCompletionOr<Vector<DeprecatedFlyString>> get_exported_names(VM& vm, Vector<Module*> export_star_set = {}) = 0;
    virtual ThrowCompletionOr<ResolvedBinding> resolve_export(VM& vm, DeprecatedFlyString const& export_name, Vector<ResolvedBinding> resolve_set = {}) = 0;

    virtual ThrowCompletionOr<u32> inner_module_linking(VM& vm, Vector<Module*>& stack, u32 index);
    virtual ThrowCompletionOr<u32> inner_module_evaluation(VM& vm, Vector<Module*>& stack, u32 index);

    virtual PromiseCapability& load_requested_modules(GCPtr<GraphLoadingState::HostDefined>) = 0;

protected:
    Module(Realm&, ByteString filename, Script::HostDefined* host_defined = nullptr);

    virtual void visit_edges(Cell::Visitor&) override;

    void set_environment(Environment* environment)
    {
        m_environment = environment;
    }

private:
    Object* module_namespace_create(VM& vm, Vector<DeprecatedFlyString> unambiguous_names);

    // These handles are only safe as long as the VM they live in is valid.
    // But evaluated modules SHOULD be stored in the VM so unless you intentionally
    // destroy the VM but keep the modules this should not happen. Because VM
    // stores modules with a RefPtr we cannot just store the VM as that leads to
    // cycles.
    GCPtr<Realm> m_realm;                            // [[Realm]]
    GCPtr<Environment> m_environment;                // [[Environment]]
    GCPtr<Object> m_namespace;                       // [[Namespace]]
    Script::HostDefined* m_host_defined { nullptr }; // [[HostDefined]]

    // Needed for potential lookups of modules.
    ByteString m_filename;
};

class CyclicModule;
struct GraphLoadingState;

void finish_loading_imported_module(ImportedModuleReferrer, ModuleRequest const&, ImportedModulePayload, ThrowCompletionOr<NonnullGCPtr<Module>> const&);

}
