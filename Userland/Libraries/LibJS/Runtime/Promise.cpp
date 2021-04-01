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

#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseJobs.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/PromiseResolvingFunction.h>

namespace JS {

// 27.2.4.7.1 PromiseResolve, https://tc39.es/ecma262/#sec-promise-resolve
Object* promise_resolve(GlobalObject& global_object, Object& constructor, Value value)
{
    auto& vm = global_object.vm();
    if (value.is_object() && is<Promise>(value.as_object())) {
        auto value_constructor = value.as_object().get(vm.names.constructor).value_or(js_undefined());
        if (vm.exception())
            return nullptr;
        if (same_value(value_constructor, &constructor))
            return &static_cast<Promise&>(value.as_object());
    }
    auto promise_capability = new_promise_capability(global_object, &constructor);
    if (vm.exception())
        return nullptr;
    [[maybe_unused]] auto result = vm.call(*promise_capability.resolve, js_undefined(), value);
    if (vm.exception())
        return nullptr;
    return promise_capability.promise;
}

Promise* Promise::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Promise>(global_object, *global_object.promise_prototype());
}

Promise::Promise(Object& prototype)
    : Object(prototype)
{
}

// 27.2.1.3 CreateResolvingFunctions, https://tc39.es/ecma262/#sec-createresolvingfunctions
Promise::ResolvingFunctions Promise::create_resolving_functions()
{
    dbgln_if(PROMISE_DEBUG, "[Promise @ {} / create_resolving_functions()]", this);
    auto& vm = this->vm();

    auto* already_resolved = vm.heap().allocate_without_global_object<AlreadyResolved>();

    // 27.2.1.3.2 Promise Resolve Functions, https://tc39.es/ecma262/#sec-promise-resolve-functions
    auto* resolve_function = PromiseResolvingFunction::create(global_object(), *this, *already_resolved, [](auto& vm, auto& global_object, auto& promise, auto& already_resolved) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Resolve function was called", &promise);
        if (already_resolved.value) {
            dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Promise is already resolved, returning undefined", &promise);
            return js_undefined();
        }
        already_resolved.value = true;
        auto resolution = vm.argument(0).value_or(js_undefined());
        if (resolution.is_object() && &resolution.as_object() == &promise) {
            dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Promise can't be resolved with itself, rejecting with error", &promise);
            auto* self_resolution_error = TypeError::create(global_object, "Cannot resolve promise with itself");
            return promise.reject(self_resolution_error);
        }
        if (!resolution.is_object()) {
            dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Resolution is not an object, fulfilling with {}", &promise, resolution);
            return promise.fulfill(resolution);
        }
        auto then_action = resolution.as_object().get(vm.names.then);
        if (vm.exception()) {
            dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Exception while getting 'then' property, rejecting with error", &promise);
            auto error = vm.exception()->value();
            vm.clear_exception();
            vm.stop_unwind();
            return promise.reject(error);
        }
        if (!then_action.is_function()) {
            dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Then action is not a function, fulfilling with {}", &promise, resolution);
            return promise.fulfill(resolution);
        }
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Creating JobCallback for then action @ {}", &promise, &then_action.as_function());
        auto then_job_callback = make_job_callback(then_action.as_function());
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Creating PromiseResolveThenableJob for thenable {}", &promise, resolution);
        auto* job = PromiseResolveThenableJob::create(global_object, promise, resolution, then_job_callback);
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Enqueuing job @ {}", &promise, job);
        vm.enqueue_promise_job(*job);
        return js_undefined();
    });

    // 27.2.1.3.1 Promise Reject Functions, https://tc39.es/ecma262/#sec-promise-reject-functions
    auto* reject_function = PromiseResolvingFunction::create(global_object(), *this, *already_resolved, [](auto& vm, auto&, auto& promise, auto& already_resolved) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / PromiseResolvingFunction]: Reject function was called", &promise);
        if (already_resolved.value)
            return js_undefined();
        already_resolved.value = true;
        auto reason = vm.argument(0).value_or(js_undefined());
        return promise.reject(reason);
    });

    return { *resolve_function, *reject_function };
}

// 27.2.1.4 FulfillPromise, https://tc39.es/ecma262/#sec-fulfillpromise
Value Promise::fulfill(Value value)
{
    dbgln_if(PROMISE_DEBUG, "[Promise @ {} / fulfill()]: Fulfilling promise with value {}", this, value);
    VERIFY(m_state == State::Pending);
    VERIFY(!value.is_empty());
    m_state = State::Fulfilled;
    m_result = value;
    trigger_reactions();
    m_fulfill_reactions.clear();
    m_reject_reactions.clear();
    return js_undefined();
}

// 27.2.1.7 RejectPromise, https://tc39.es/ecma262/#sec-rejectpromise
Value Promise::reject(Value reason)
{
    dbgln_if(PROMISE_DEBUG, "[Promise @ {} / reject()]: Rejecting promise with reason {}", this, reason);
    VERIFY(m_state == State::Pending);
    VERIFY(!reason.is_empty());
    auto& vm = this->vm();
    m_state = State::Rejected;
    m_result = reason;
    if (!m_is_handled)
        vm.promise_rejection_tracker(*this, RejectionOperation::Reject);
    trigger_reactions();
    m_fulfill_reactions.clear();
    m_reject_reactions.clear();
    return js_undefined();
}

// 27.2.1.8 TriggerPromiseReactions, https://tc39.es/ecma262/#sec-triggerpromisereactions
void Promise::trigger_reactions() const
{
    VERIFY(is_settled());
    auto& vm = this->vm();
    auto& reactions = m_state == State::Fulfilled
        ? m_fulfill_reactions
        : m_reject_reactions;
    for (auto& reaction : reactions) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / trigger_reactions()]: Creating PromiseReactionJob for PromiseReaction @ {} with argument {}", this, &reaction, m_result);
        auto* job = PromiseReactionJob::create(global_object(), *reaction, m_result);
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / trigger_reactions()]: Enqueuing job @ {}", this, job);
        vm.enqueue_promise_job(*job);
    }
    if constexpr (PROMISE_DEBUG) {
        if (reactions.is_empty())
            dbgln("[Promise @ {} / trigger_reactions()]: No reactions!", this);
    }
}

// 27.2.5.4.1 PerformPromiseThen, https://tc39.es/ecma262/#sec-performpromisethen
Value Promise::perform_then(Value on_fulfilled, Value on_rejected, Optional<PromiseCapability> result_capability)
{
    auto& vm = this->vm();

    Optional<JobCallback> on_fulfilled_job_callback;
    if (on_fulfilled.is_function()) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: Creating JobCallback for on_fulfilled function @ {}", this, &on_fulfilled.as_function());
        on_fulfilled_job_callback = make_job_callback(on_fulfilled.as_function());
    }

    Optional<JobCallback> on_rejected_job_callback;
    if (on_rejected.is_function()) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: Creating JobCallback for on_rejected function @ {}", this, &on_rejected.as_function());
        on_rejected_job_callback = make_job_callback(on_rejected.as_function());
    }

    auto* fulfill_reaction = PromiseReaction::create(vm, PromiseReaction::Type::Fulfill, result_capability, on_fulfilled_job_callback);
    auto* reject_reaction = PromiseReaction::create(vm, PromiseReaction::Type::Reject, result_capability, on_rejected_job_callback);

    switch (m_state) {
    case Promise::State::Pending:
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: state is State::Pending, adding fulfill/reject reactions", this);
        m_fulfill_reactions.append(fulfill_reaction);
        m_reject_reactions.append(reject_reaction);
        break;
    case Promise::State::Fulfilled: {
        auto value = m_result;
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: State is State::Fulfilled, creating PromiseReactionJob for PromiseReaction @ {} with argument {}", this, fulfill_reaction, value);
        auto* fulfill_job = PromiseReactionJob::create(global_object(), *fulfill_reaction, value);
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: Enqueuing job @ {}", this, fulfill_job);
        vm.enqueue_promise_job(*fulfill_job);
        break;
    }
    case Promise::State::Rejected: {
        auto reason = m_result;
        if (!m_is_handled)
            vm.promise_rejection_tracker(*this, RejectionOperation::Handle);
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: State is State::Rejected, creating PromiseReactionJob for PromiseReaction @ {} with argument {}", this, reject_reaction, reason);
        auto* reject_job = PromiseReactionJob::create(global_object(), *reject_reaction, reason);
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: Enqueuing job @ {}", this, reject_job);
        vm.enqueue_promise_job(*reject_job);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    m_is_handled = true;

    if (!result_capability.has_value()) {
        dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: No ResultCapability, returning undefined", this);
        return js_undefined();
    }
    auto* promise = result_capability.value().promise;
    dbgln_if(PROMISE_DEBUG, "[Promise @ {} / perform_then()]: Returning Promise @ {} from ResultCapability @ {}", this, promise, &result_capability.value());
    return promise;
}

void Promise::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_result);
    for (auto& reaction : m_fulfill_reactions)
        visitor.visit(reaction);
    for (auto& reaction : m_reject_reactions)
        visitor.visit(reaction);
}

}
