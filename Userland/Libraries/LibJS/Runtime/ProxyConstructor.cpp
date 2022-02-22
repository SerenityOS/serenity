/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ProxyConstructor.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

// 10.5.14 ProxyCreate ( target, handler ), https://tc39.es/ecma262/#sec-proxycreate
static ThrowCompletionOr<ProxyObject*> proxy_create(GlobalObject& global_object, Value target, Value handler)
{
    auto& vm = global_object.vm();
    if (!target.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::ProxyConstructorBadType, "target", target.to_string_without_side_effects());
    if (!handler.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::ProxyConstructorBadType, "handler", handler.to_string_without_side_effects());
    return ProxyObject::create(global_object, target.as_object(), handler.as_object());
}

ProxyConstructor::ProxyConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Proxy.as_string(), *global_object.function_prototype())
{
}

void ProxyConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.revocable, revocable, 2, attr);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

ProxyConstructor::~ProxyConstructor()
{
}

// 28.2.1.1 Proxy ( target, handler ), https://tc39.es/ecma262/#sec-proxy-target-handler
ThrowCompletionOr<Value> ProxyConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Proxy);
}

// 28.2.1.1 Proxy ( target, handler ), https://tc39.es/ecma262/#sec-proxy-target-handler
ThrowCompletionOr<Object*> ProxyConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    return TRY(proxy_create(global_object(), vm.argument(0), vm.argument(1)));
}

// 28.2.2.1 Proxy.revocable ( target, handler ), https://tc39.es/ecma262/#sec-proxy.revocable
JS_DEFINE_NATIVE_FUNCTION(ProxyConstructor::revocable)
{
    // 1. Let p be ? ProxyCreate(target, handler).
    auto* proxy = TRY(proxy_create(global_object, vm.argument(0), vm.argument(1)));

    // 2. Let revokerClosure be a new Abstract Closure with no parameters that captures nothing and performs the following steps when called:
    auto revoker_closure = [proxy_handle = make_handle(proxy)](auto&, auto&) -> ThrowCompletionOr<Value> {
        // a. Let F be the active function object.

        // b. Let p be F.[[RevocableProxy]].
        auto& proxy = const_cast<ProxyObject&>(*proxy_handle.cell());

        // c. If p is null, return undefined.
        if (proxy.is_revoked())
            return js_undefined();

        // d. Set F.[[RevocableProxy]] to null.
        // e. Assert: p is a Proxy object.
        // f. Set p.[[ProxyTarget]] to null.
        // g. Set p.[[ProxyHandler]] to null.
        proxy.revoke();

        // h. Return undefined.
        return js_undefined();
    };

    // 3. Let revoker be ! CreateBuiltinFunction(revokerClosure, 0, "", « [[RevocableProxy]] »).
    // 4. Set revoker.[[RevocableProxy]] to p.
    auto* revoker = NativeFunction::create(global_object, move(revoker_closure), 0, "");

    // 5. Let result be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* result = Object::create(global_object, global_object.object_prototype());

    // 6. Perform ! CreateDataPropertyOrThrow(result, "proxy", p).
    MUST(result->create_data_property_or_throw(vm.names.proxy, proxy));

    // 7. Perform ! CreateDataPropertyOrThrow(result, "revoke", revoker).
    MUST(result->create_data_property_or_throw(vm.names.revoke, revoker));

    // 8. Return result.
    return result;
}

}
