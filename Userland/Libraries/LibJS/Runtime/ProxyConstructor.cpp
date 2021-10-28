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
    auto* proxy = TRY(proxy_create(global_object, vm.argument(0), vm.argument(1)));

    // 28.2.2.1.1 Proxy Revocation Functions, https://tc39.es/ecma262/#sec-proxy-revocation-functions
    auto* revoker = NativeFunction::create(global_object, "", [proxy_handle = make_handle(proxy)](auto&, auto&) -> ThrowCompletionOr<Value> {
        auto& proxy = const_cast<ProxyObject&>(*proxy_handle.cell());
        if (proxy.is_revoked())
            return js_undefined();
        // NOTE: The spec wants us to unset [[ProxyTarget]] and [[ProxyHandler]],
        // which is their way of revoking the Proxy - this might affect GC-ability,
        // but AFAICT not doing that should be ok compatibility-wise.
        proxy.revoke();
        return js_undefined();
    });
    revoker->define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
    revoker->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

    auto* result = Object::create(global_object, global_object.object_prototype());
    MUST(result->create_data_property_or_throw(vm.names.proxy, proxy));
    MUST(result->create_data_property_or_throw(vm.names.revoke, revoker));
    return result;
}

}
