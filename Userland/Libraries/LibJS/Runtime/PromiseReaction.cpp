/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

// 27.2.1.5 NewPromiseCapability, https://tc39.es/ecma262/#sec-newpromisecapability
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

    auto* executor = NativeFunction::create(global_object, "", [&promise_capability_functions](auto& vm, auto& global_object) -> Value {
        auto resolve = vm.argument(0);
        auto reject = vm.argument(1);
        // No idea what other engines say here.
        if (!promise_capability_functions.resolve.is_undefined()) {
            vm.template throw_exception<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);
            return {};
        }
        if (!promise_capability_functions.resolve.is_undefined()) {
            vm.template throw_exception<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);
            return {};
        }
        promise_capability_functions.resolve = resolve;
        promise_capability_functions.reject = reject;
        return js_undefined();
    });
    executor->define_property(vm.names.length, Value(2));

    MarkedValueList arguments(vm.heap());
    arguments.append(executor);
    auto promise = vm.construct(constructor.as_function(), constructor.as_function(), move(arguments), global_object);
    if (vm.exception())
        return {};

    // I'm not sure if we could VERIFY(promise.is_object()) instead - the spec doesn't have this check...
    if (!promise.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, promise.to_string_without_side_effects());
        return {};
    }

    if (!promise_capability_functions.resolve.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.resolve.to_string_without_side_effects());
        return {};
    }
    if (!promise_capability_functions.reject.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.reject.to_string_without_side_effects());
        return {};
    }

    return {
        &promise.as_object(),
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
