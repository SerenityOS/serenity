/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/ModuleEnvironment.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/SyntheticModule.h>

namespace JS {

// 1.2.1 CreateSyntheticModule ( exportNames, evaluationSteps, realm, hostDefined ), https://tc39.es/proposal-json-modules/#sec-synthetic-module-records
SyntheticModule::SyntheticModule(Vector<FlyString> export_names, SyntheticModule::EvaluationFunction evaluation_steps, Realm& realm, StringView filename)
    : Module(realm, filename)
    , m_export_names(move(export_names))
    , m_evaluation_steps(move(evaluation_steps))
{
    // 1. Return Synthetic Module Record { [[Realm]]: realm, [[Environment]]: undefined, [[Namespace]]: undefined, [[HostDefined]]: hostDefined, [[ExportNames]]: exportNames, [[EvaluationSteps]]: evaluationSteps }.
}

// 1.2.3.1 GetExportedNames( exportStarSet ), https://tc39.es/proposal-json-modules/#sec-smr-getexportednames
ThrowCompletionOr<Vector<FlyString>> JS::SyntheticModule::get_exported_names(VM&, Vector<Module*>)
{
    // 1. Return module.[[ExportNames]].
    return m_export_names;
}

// 1.2.3.2 ResolveExport( exportName, resolveSet ), https://tc39.es/proposal-json-modules/#sec-smr-resolveexport
ThrowCompletionOr<ResolvedBinding> JS::SyntheticModule::resolve_export(VM&, FlyString const& export_name, Vector<ResolvedBinding>)
{
    // 1. If module.[[ExportNames]] does not contain exportName, return null.
    if (!m_export_names.contains_slow(export_name))
        return ResolvedBinding::null();

    // 2. Return ResolvedBinding Record { [[Module]]: module, [[BindingName]]: exportName }.
    return ResolvedBinding { ResolvedBinding::BindingName, this, export_name };
}

// 1.2.3.3 Link ( ), https://tc39.es/proposal-json-modules/#sec-smr-instantiate
ThrowCompletionOr<void> JS::SyntheticModule::link(VM& vm)
{
    // Note: Has some changes from PR: https://github.com/tc39/proposal-json-modules/pull/13.
    //       Which includes renaming it from Instantiate ( ) to Link ( ).

    // 1. Let realm be module.[[Realm]].
    // 2. Assert: realm is not undefined.
    // Note: This must be true because we use a reference.

    auto& global_object = realm().global_object();

    // 3. Let env be NewModuleEnvironment(realm.[[GlobalEnv]]).
    auto* environment = vm.heap().allocate_without_global_object<ModuleEnvironment>(&realm().global_environment());

    // 4. Set module.[[Environment]] to env.
    set_environment(environment);

    // 5. For each exportName in module.[[ExportNames]],
    for (auto& export_name : m_export_names) {
        // a. Perform ! envRec.CreateMutableBinding(exportName, false).
        environment->create_mutable_binding(global_object, export_name, false);

        // b. Perform ! envRec.InitializeBinding(exportName, undefined).
        environment->initialize_binding(global_object, export_name, js_undefined());
    }

    // 6. Return undefined.
    // Note: This return value is never visible to the outside so we use void.
    return {};
}

// 1.2.3.4 Evaluate ( ), https://tc39.es/proposal-json-modules/#sec-smr-Evaluate
ThrowCompletionOr<Promise*> JS::SyntheticModule::evaluate(VM& vm)
{
    // Note: Has some changes from PR: https://github.com/tc39/proposal-json-modules/pull/13.
    // 1. Suspend the currently running execution context.
    // FIXME: We don't have suspend yet.

    // 2. Let moduleContext be a new ECMAScript code execution context.
    ExecutionContext module_context { vm.heap() };

    // 3. Set the Function of moduleContext to null.
    // Note: This is the default value.

    // 4. Set the Realm of moduleContext to module.[[Realm]].
    module_context.realm = &realm();

    // 5. Set the ScriptOrModule of moduleContext to module.
    module_context.script_or_module = this->make_weak_ptr();

    // 6. Set the VariableEnvironment of moduleContext to module.[[Environment]].
    module_context.variable_environment = environment();

    // 7. Set the LexicalEnvironment of moduleContext to module.[[Environment]].
    module_context.lexical_environment = environment();

    // 8. Push moduleContext on to the execution context stack; moduleContext is now the running execution context.
    vm.push_execution_context(module_context, realm().global_object());

    // 9. Let result be the result of performing module.[[EvaluationSteps]](module).
    auto result = m_evaluation_steps(*this);

    // 10. Suspend moduleContext and remove it from the execution context stack.
    vm.pop_execution_context();

    // 11. Resume the context that is now on the top of the execution context stack as the running execution context.

    // 12. Return Completion(result).
    // Note: Because we expect it to return a promise we convert this here.
    auto* promise = Promise::create(realm().global_object());
    if (result.is_error()) {
        VERIFY(result.throw_completion().value().has_value());
        promise->reject(*result.throw_completion().value());
    } else {
        // Note: This value probably isn't visible to JS code? But undefined is fine anyway.
        promise->fulfill(js_undefined());
    }
    return promise;
}

// 1.2.2 SetSyntheticModuleExport ( module, exportName, exportValue ), https://tc39.es/proposal-json-modules/#sec-setsyntheticmoduleexport
ThrowCompletionOr<void> SyntheticModule::set_synthetic_module_export(FlyString const& export_name, Value export_value)
{
    // Note: Has some changes from PR: https://github.com/tc39/proposal-json-modules/pull/13.
    // 1. Return ? module.[[Environment]].SetMutableBinding(name, value, true).
    return environment()->set_mutable_binding(realm().global_object(), export_name, export_value, true);
}

// 1.3 CreateDefaultExportSyntheticModule ( defaultExport ), https://tc39.es/proposal-json-modules/#sec-create-default-export-synthetic-module
NonnullRefPtr<SyntheticModule> SyntheticModule::create_default_export_synthetic_module(Value default_export, Realm& realm, StringView filename)
{
    // Note: Has some changes from PR: https://github.com/tc39/proposal-json-modules/pull/13.
    // 1. Let closure be the a Abstract Closure with parameters (module) that captures defaultExport and performs the following steps when called:
    auto closure = [default_export = make_handle(default_export)](SyntheticModule& module) -> ThrowCompletionOr<void> {
        // a. Return ? module.SetSyntheticExport("default", defaultExport).
        return module.set_synthetic_module_export("default", default_export.value());
    };

    // 2. Return CreateSyntheticModule("default", closure, realm)
    return adopt_ref(*new SyntheticModule({ "default" }, move(closure), realm, filename));
}

// 1.4 ParseJSONModule ( source ), https://tc39.es/proposal-json-modules/#sec-parse-json-module
ThrowCompletionOr<NonnullRefPtr<Module>> parse_json_module(StringView source_text, Realm& realm, StringView filename)
{
    // 1. Let jsonParse be realm's intrinsic object named "%JSON.parse%".
    auto* json_parse = realm.global_object().json_parse_function();

    // 2. Let json be ? Call(jsonParse, undefined, « sourceText »).
    auto json = TRY(call(realm.global_object(), *json_parse, js_undefined(), js_string(realm.vm(), source_text)));

    // 3. Return CreateDefaultExportSyntheticModule(json, realm, hostDefined).
    return SyntheticModule::create_default_export_synthetic_module(json, realm, filename);
}

}
