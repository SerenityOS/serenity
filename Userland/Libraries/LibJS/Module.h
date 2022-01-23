/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Realm.h>

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
    Module* module { nullptr };
    FlyString export_name;

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

// 16.2.1.4 Abstract Module Records, https://tc39.es/ecma262/#sec-abstract-module-records
class Module : public RefCounted<Module> {
public:
    virtual ~Module();

    Realm& realm() { return *m_realm.cell(); }
    Realm const& realm() const { return *m_realm.cell(); }

    StringView filename() const { return m_filename; }

    Environment* environment() { return m_environment.cell(); }

    ThrowCompletionOr<Object*> get_module_namespace(VM& vm);

    virtual ThrowCompletionOr<void> link(VM& vm) = 0;
    virtual ThrowCompletionOr<Promise*> evaluate(VM& vm) = 0;

    virtual ThrowCompletionOr<Vector<FlyString>> get_exported_names(VM& vm, Vector<Module*> export_star_set = {}) = 0;
    virtual ThrowCompletionOr<ResolvedBinding> resolve_export(VM& vm, FlyString const& export_name, Vector<ResolvedBinding> resolve_set = {}) = 0;

    virtual ThrowCompletionOr<u32> inner_module_linking(VM& vm, Vector<Module*>& stack, u32 index);
    virtual ThrowCompletionOr<u32> inner_module_evaluation(VM& vm, Vector<Module*>& stack, u32 index);

protected:
    Module(Realm&, String filename);

    void set_environment(Environment* environment)
    {
        m_environment = make_handle(environment);
    }

private:
    Object* module_namespace_create(VM& vm, Vector<FlyString> unambiguous_names);

    // These handles are only safe as long as the VM they live in is valid.
    // But evaluated modules SHOULD be stored in the VM so unless you intentionally
    // destroy the VM but keep the modules this should not happen. Because VM
    // stores modules with a RefPtr we cannot just store the VM as that leads to
    // cycles.
    Handle<Realm> m_realm;             // [[Realm]]
    Handle<Environment> m_environment; // [[Environment]]
    Handle<Object> m_namespace;        // [[Namespace]]

    // Needed for potential lookups of modules.
    String m_filename;
};

}
