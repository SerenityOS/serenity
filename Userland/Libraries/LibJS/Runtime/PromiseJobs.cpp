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

// 27.2.2.1 NewPromiseReactionJob, https://tc39.es/ecma262/#sec-newpromisereactionjob
Value PromiseReactionJob::call()
{
    auto& vm = this->vm();
    auto& promise_capability = m_reaction.capability();
    auto type = m_reaction.type();
    auto handler = m_reaction.handler();
    Value handler_result;
    if (!handler.has_value()) {
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Handler is empty", this);
        switch (type) {
        case PromiseReaction::Type::Fulfill:
            dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction type is Type::Fulfill, setting handler result to {}", this, m_argument);
            handler_result = m_argument;
            break;
        case PromiseReaction::Type::Reject:
            dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction type is Type::Reject, throwing exception with argument {}", this, m_argument);
            vm.throw_exception(global_object(), m_argument);
            // handler_result is set to exception value further below
            break;
        }
    } else {
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling handler callback {} @ {} with argument {}", this, handler.value().callback->class_name(), handler.value().callback, m_argument);
        handler_result = call_job_callback(vm, handler.value(), js_undefined(), m_argument);
    }

    if (!promise_capability.has_value()) {
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Reaction has no PromiseCapability, returning empty value", this);
        return {};
    }

    if (vm.exception()) {
        handler_result = vm.exception()->value();
        vm.clear_exception();
        vm.stop_unwind();
        auto* reject_function = promise_capability.value().reject;
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling PromiseCapability's reject function @ {}", this, reject_function);
        return vm.call(*reject_function, js_undefined(), handler_result);
    } else {
        auto* resolve_function = promise_capability.value().resolve;
        dbgln_if(PROMISE_DEBUG, "[PromiseReactionJob @ {}]: Calling PromiseCapability's resolve function @ {}", this, resolve_function);
        return vm.call(*resolve_function, js_undefined(), handler_result);
    }
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

// 27.2.2.2 NewPromiseResolveThenableJob, https://tc39.es/ecma262/#sec-newpromiseresolvethenablejob
Value PromiseResolveThenableJob::call()
{
    auto& vm = this->vm();
    auto [resolve_function, reject_function] = m_promise_to_resolve.create_resolving_functions();
    dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: Calling then job callback for thenable {}", this, &m_thenable);
    auto then_call_result = call_job_callback(vm, m_then, m_thenable, &resolve_function, &reject_function);
    if (vm.exception()) {
        auto error = vm.exception()->value();
        vm.clear_exception();
        vm.stop_unwind();
        dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: An exception was thrown, returning error {}", this, error);
        return error;
    }
    dbgln_if(PROMISE_DEBUG, "[PromiseResolveThenableJob @ {}]: Returning then call result {}", this, then_call_result);
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
