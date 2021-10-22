/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
PromiseCapability new_promise_capability(GlobalObject& global_object, Value constructor)
{
    auto& vm = global_object.vm();
    if (!constructor.is_constructor()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return {};
    }

    struct {
        Value resolve { js_undefined() };
        Value reject { js_undefined() };
    } promise_capability_functions;

    // 27.2.1.5.1 GetCapabilitiesExecutor Functions, https://tc39.es/ecma262/#sec-getcapabilitiesexecutor-functions
    auto* executor = NativeFunction::create(global_object, "", [&promise_capability_functions](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
        auto resolve = vm.argument(0);
        auto reject = vm.argument(1);
        // No idea what other engines say here.
        if (!promise_capability_functions.resolve.is_undefined()) {
            return vm.template throw_completion<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);
        }
        if (!promise_capability_functions.reject.is_undefined()) {
            return vm.template throw_completion<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);
        }
        promise_capability_functions.resolve = resolve;
        promise_capability_functions.reject = reject;
        return js_undefined();
    });
    executor->define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
    executor->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

    MarkedValueList arguments(vm.heap());
    arguments.append(executor);
    auto* promise = TRY_OR_DISCARD(construct(global_object, constructor.as_function(), move(arguments)));

    if (!promise_capability_functions.resolve.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.resolve.to_string_without_side_effects());
        return {};
    }
    if (!promise_capability_functions.reject.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.reject.to_string_without_side_effects());
        return {};
    }

    return {
        promise,
        &promise_capability_functions.resolve.as_function(),
        &promise_capability_functions.reject.as_function(),
    };
}

PromiseReaction::PromiseReaction(Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler)
    : m_type(type)
    , m_capability(move(capability))
    , m_handler(move(handler))
{
}

void PromiseReaction::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    if (m_capability.has_value()) {
        auto& capability = m_capability.value();
        visitor.visit(capability.promise);
        visitor.visit(capability.resolve);
        visitor.visit(capability.reject);
    }
    if (m_handler.has_value()) {
        auto& handler = m_handler.value();
        visitor.visit(handler.callback);
    }
}

}
