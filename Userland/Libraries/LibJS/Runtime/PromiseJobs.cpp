/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseJobs.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

PromiseReactionJob* PromiseReactionJob::create(GlobalObject& global_object, PromiseReaction& reaction, Value argument)
{
    return global_object.heap().allocate<PromiseReactionJob>(global_object, reaction, argument, *global_object.function_prototype());
}

PromiseReactionJob::PromiseReactionJob(PromiseReaction& reaction, Value argument, Object& prototype)
    : NativeFunction(prototype)
    , m_reaction(reaction)
    , m_argument(argument)
{
}

// 27.2.2.1 NewPromiseReactionJob ( reaction, argument ), https://tc39.es/ecma262/#sec-newpromisereactionjob
ThrowCompletionOr<Value> PromiseReactionJob::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // a. Let promiseCapability be reaction.[[Capability]].
    auto& promise_capability = m_reaction.capability();

    // b. Let type be reaction.[[Type]].
    auto type = m_reaction.type();

    // c. Let handler be reaction.[[Handler]].
    auto handler = m_reaction.handler();

    Completion handler_result;

    // d. If handler is empty, then
    if (!handler.has_value()) {
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Handler is empty", this);

        // i. If type is Fulfill, let handlerResult be NormalCompletion(argument).
        if (type == PromiseReaction::Type::Fulfill) {
            dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction type is Type::Fulfill, setting handler result to {}", this, m_argument);
            handler_result = normal_completion(m_argument);
        }
        // ii. Else,
        else {
            // 1. Assert: type is Reject.
            VERIFY(type == PromiseReaction::Type::Reject);

            // 2. Let handlerResult be ThrowCompletion(argument).
            dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction type is Type::Reject, throwing exception with argument {}", this, m_argument);
            handler_result = throw_completion(m_argument);
        }
    }
    // e. Else, let handlerResult be HostCallJobCallback(handler, undefined, « argument »).
    else {
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling handler callback {} @ {} with argument {}", this, handler.value().callback->class_name(), handler.value().callback, m_argument);
        handler_result = call_job_callback(global_object, handler.value(), js_undefined(), m_argument);
    }

    // f. If promiseCapability is undefined, then
    if (!promise_capability.has_value()) {
        // i. Assert: handlerResult is not an abrupt completion.
        VERIFY(!vm.exception());

        // ii. Return NormalCompletion(empty).
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction has no PromiseCapability, returning empty value", this);
        // TODO: This can't return an empty value at the moment, because the implicit conversion to Completion would fail.
        //       Change it back when this is using completions (`return normal_completion({})`)
        return js_undefined();
    }

    // g. Assert: promiseCapability is a PromiseCapability Record.

    // h. If handlerResult is an abrupt completion, then
    if (handler_result.is_abrupt()) {
        vm.clear_exception();

        // i. Let status be Call(promiseCapability.[[Reject]], undefined, « handlerResult.[[Value]] »).
        auto* reject_function = promise_capability.value().reject;
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling PromiseCapability's reject function @ {}", this, reject_function);
        return JS::call(global_object, *reject_function, js_undefined(), *handler_result.value());
    }
    // i. Else,
    else {
        // i. Let status be Call(promiseCapability.[[Resolve]], undefined, « handlerResult.[[Value]] »).
        auto* resolve_function = promise_capability.value().resolve;
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling PromiseCapability's resolve function @ {}", this, resolve_function);
        return JS::call(global_object, *resolve_function, js_undefined(), *handler_result.value());
    }

    // j. Return Completion(status).
}

void PromiseReactionJob::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_reaction);
    visitor.visit(m_argument);
}

PromiseResolveThenableJob* PromiseResolveThenableJob::create(GlobalObject& global_object, Promise& promise_to_resolve, Value thenable, JobCallback then)
{
    // FIXME: A bunch of stuff regarding realms, see step 2-5 in the spec linked below
    return global_object.heap().allocate<PromiseResolveThenableJob>(global_object, promise_to_resolve, thenable, then, *global_object.function_prototype());
}

PromiseResolveThenableJob::PromiseResolveThenableJob(Promise& promise_to_resolve, Value thenable, JobCallback then, Object& prototype)
    : NativeFunction(prototype)
    , m_promise_to_resolve(promise_to_resolve)
    , m_thenable(thenable)
    , m_then(then)
{
}

// 27.2.2.2 NewPromiseResolveThenableJob ( promiseToResolve, thenable, then ), https://tc39.es/ecma262/#sec-newpromiseresolvethenablejob
ThrowCompletionOr<Value> PromiseResolveThenableJob::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // a. Let resolvingFunctions be CreateResolvingFunctions(promiseToResolve).
    auto [resolve_function, reject_function] = m_promise_to_resolve.create_resolving_functions();

    // b. Let thenCallResult be HostCallJobCallback(then, thenable, « resolvingFunctions.[[Resolve]], resolvingFunctions.[[Reject]] »).
    dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: Calling then job callback for thenable {}", this, &m_thenable);
    auto then_call_result = call_job_callback(global_object, m_then, m_thenable, &resolve_function, &reject_function);

    // c. If thenCallResult is an abrupt completion, then
    if (then_call_result.is_error()) {
        vm.clear_exception();

        // i. Let status be Call(resolvingFunctions.[[Reject]], undefined, « thenCallResult.[[Value]] »).
        dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: then_call_result is an abrupt completion, calling reject function with value {}", this, *then_call_result.throw_completion().value());
        auto status = JS::call(global_object, &reject_function, js_undefined(), *then_call_result.throw_completion().value());

        // ii. Return Completion(status).
        return status;
    }

    // d. Return Completion(thenCallResult).
    dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: Returning then call result {}", this, then_call_result.value());
    return then_call_result;
}

void PromiseResolveThenableJob::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_promise_to_resolve);
    visitor.visit(m_thenable);
    visitor.visit(m_then.callback);
}

}
