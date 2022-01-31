/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/CyclicModule.h>
#include <LibJS/Module.h>
#include <LibJS/Runtime/ModuleNamespaceObject.h>

namespace JS {

Module::Module(Realm& realm, String filename)
    : m_realm(make_handle(&realm))
    , m_filename(move(filename))
{
}

Module::~Module()
{
}

// 16.2.1.5.1.1 InnerModuleLinking ( module, stack, index ), https://tc39.es/ecma262/#sec-InnerModuleLinking
ThrowCompletionOr<u32> Module::inner_module_linking(VM& vm, Vector<Module*>&, u32 index)
{
    // 1. If module is not a Cyclic Module Record, then
    // a. Perform ? module.Link().
    TRY(link(vm));
    // b. Return index.
    return index;
}

// 16.2.1.5.2.1 InnerModuleEvaluation ( module, stack, index ), https://tc39.es/ecma262/#sec-innermoduleevaluation
ThrowCompletionOr<u32> Module::inner_module_evaluation(VM& vm, Vector<Module*>&, u32 index)
{
    // 1. If module is not a Cyclic Module Record, then
    // a. Let promise be ! module.Evaluate().
    auto* promise = TRY(evaluate(vm));

    // b. Assert: promise.[[PromiseState]] is not pending.
    VERIFY(promise->state() != Promise::State::Pending);

    // c. If promise.[[PromiseState]] is rejected, then
    if (promise->state() == Promise::State::Rejected) {
        // i. Return ThrowCompletion(promise.[[PromiseResult]]).
        return throw_completion(promise->result());
    }

    // d. Return index.
    return index;
}

// 16.2.1.10 GetModuleNamespace ( module ), https://tc39.es/ecma262/#sec-getmodulenamespace
ThrowCompletionOr<Object*> Module::get_module_namespace(VM& vm)
{
    // 1. Assert: If module is a Cyclic Module Record, then module.[[Status]] is not unlinked.
    // FIXME: How do we check this without breaking encapsulation?

    // 2. Let namespace be module.[[Namespace]].
    auto* namespace_ = m_namespace.is_null() ? nullptr : m_namespace.cell();

    // 3. If namespace is empty, then
    if (!namespace_) {
        // a. Let exportedNames be ? module.GetExportedNames().
        auto exported_names = TRY(get_exported_names(vm));

        // b. Let unambiguousNames be a new empty List.
        Vector<FlyString> unambiguous_names;

        // c. For each element name of exportedNames, do
        for (auto& name : exported_names) {
            // i. Let resolution be ? module.ResolveExport(name).
            auto resolution = TRY(resolve_export(vm, name));

            // ii. If resolution is a ResolvedBinding Record, append name to unambiguousNames.
            if (resolution.is_valid())
                unambiguous_names.append(name);
        }

        // d. Set namespace to ModuleNamespaceCreate(module, unambiguousNames).
        namespace_ = module_namespace_create(vm, unambiguous_names);
        VERIFY(!m_namespace.is_null());
        // Note: This set the local variable 'namespace' and not the member variable which is done by ModuleNamespaceCreate
    }

    // 4. Return namespace.
    return namespace_;
}

// 10.4.6.12 ModuleNamespaceCreate ( module, exports ), https://tc39.es/ecma262/#sec-modulenamespacecreate
Object* Module::module_namespace_create(VM& vm, Vector<FlyString> unambiguous_names)
{
    // 1. Assert: module.[[Namespace]] is empty.
    VERIFY(m_namespace.is_null());

    // 2. Let internalSlotsList be the internal slots listed in Table 34.
    // 3. Let M be ! MakeBasicObject(internalSlotsList).
    // 4. Set M's essential internal methods to the definitions specified in 10.4.6.
    // 5. Set M.[[Module]] to module.
    // 6. Let sortedExports be a List whose elements are the elements of exports ordered as if an Array of the same values had been sorted using %Array.prototype.sort% using undefined as comparefn.
    // 7. Set M.[[Exports]] to sortedExports.
    // 8. Create own properties of M corresponding to the definitions in 28.3.
    Object* module_namespace = vm.heap().allocate<ModuleNamespaceObject>(realm().global_object(), realm().global_object(), this, move(unambiguous_names));

    // 9. Set module.[[Namespace]] to M.
    m_namespace = make_handle(module_namespace);

    // 10. Return M.
    return module_namespace;
}

}
