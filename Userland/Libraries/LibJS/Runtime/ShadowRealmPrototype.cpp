/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/ShadowRealmPrototype.h>

namespace JS {

// 3.4 Properties of the ShadowRealm Prototype Object, https://tc39.es/proposal-shadowrealm/#sec-properties-of-the-shadowrealm-prototype-object
ShadowRealmPrototype::ShadowRealmPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ShadowRealmPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.evaluate, evaluate, 1, attr);
    define_native_function(vm.names.importValue, import_value, 2, attr);

    // 3.4.3 ShadowRealm.prototype [ @@toStringTag ], https://tc39.es/proposal-shadowrealm/#sec-shadowrealm.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.ShadowRealm.as_string()), Attribute::Configurable);
}

// 3.4.1 ShadowRealm.prototype.evaluate ( sourceText ), https://tc39.es/proposal-shadowrealm/#sec-shadowrealm.prototype.evaluate
JS_DEFINE_NATIVE_FUNCTION(ShadowRealmPrototype::evaluate)
{
    auto source_text = vm.argument(0);

    // 1. Let O be this value.
    // 2. Perform ? ValidateShadowRealmObject(O).
    auto* object = TRY(typed_this_object(global_object));

    // 3. If Type(sourceText) is not String, throw a TypeError exception.
    if (!source_text.is_string())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAString, source_text);

    // 4. Let callerRealm be the current Realm Record.
    auto* caller_realm = vm.current_realm();

    // 5. Let evalRealm be O.[[ShadowRealm]].
    auto& eval_realm = object->shadow_realm();

    // 6. Return ? PerformShadowRealmEval(sourceText, callerRealm, evalRealm).
    return perform_shadow_realm_eval(global_object, source_text.as_string().string(), *caller_realm, eval_realm);
}

// 3.4.2 ShadowRealm.prototype.importValue ( specifier, exportName ), https://tc39.es/proposal-shadowrealm/#sec-shadowrealm.prototype.importvalue
JS_DEFINE_NATIVE_FUNCTION(ShadowRealmPrototype::import_value)
{
    auto specifier = vm.argument(0);
    auto export_name = vm.argument(1);

    // 1. Let O be this value.
    // 2. Perform ? ValidateShadowRealmObject(O).
    auto* object = TRY(typed_this_object(global_object));

    // 3. Let specifierString be ? ToString(specifier).
    auto specifier_string = TRY(specifier.to_string(global_object));

    // 4. Let exportNameString be ? ToString(exportName).
    auto export_name_string = TRY(export_name.to_string(global_object));

    // 5. Let callerRealm be the current Realm Record.
    auto* caller_realm = vm.current_realm();

    // 6. Let evalRealm be O.[[ShadowRealm]].
    auto& eval_realm = object->shadow_realm();

    // 7. Let evalContext be O.[[ExecutionContext]].
    auto& eval_context = object->execution_context();

    // 8. Return ? ShadowRealmImportValue(specifierString, exportNameString, callerRealm, evalRealm, evalContext).
    return shadow_realm_import_value(global_object, move(specifier_string), move(export_name_string), *caller_realm, eval_realm, eval_context);
}

}
