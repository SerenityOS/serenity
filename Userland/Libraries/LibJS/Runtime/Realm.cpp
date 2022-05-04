/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 9.3.1 CreateRealm ( ), https://tc39.es/ecma262/#sec-createrealm
Realm* Realm::create(VM& vm)
{
    return vm.heap().allocate_without_global_object<Realm>();
}

// 9.3.3 SetRealmGlobalObject ( realmRec, globalObj, thisValue ), https://tc39.es/ecma262/#sec-setrealmglobalobject
void Realm::set_global_object(GlobalObject& global_object, Object* this_value)
{
    // NOTE: Step 1 is not supported, the global object must be allocated elsewhere.
    // 2. Assert: Type(globalObj) is Object.

    // Non-standard
    global_object.set_associated_realm({}, *this);

    // 3. If thisValue is undefined, set thisValue to globalObj.
    if (!this_value)
        this_value = &global_object;

    // 4. Set realmRec.[[GlobalObject]] to globalObj.
    m_global_object = &global_object;

    // 5. Let newGlobalEnv be NewGlobalEnvironment(globalObj, thisValue).
    // 6. Set realmRec.[[GlobalEnv]] to newGlobalEnv.
    m_global_environment = global_object.heap().allocate_without_global_object<GlobalEnvironment>(global_object, *this_value);

    // 7. Return unused.
}

void Realm::visit_edges(Visitor& visitor)
{
    visitor.visit(m_global_object);
    visitor.visit(m_global_environment);
}

}
