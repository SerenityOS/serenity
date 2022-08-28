/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 27.2.1.1 PromiseCapability Records, https://tc39.es/ecma262/#sec-promisecapability-records
struct PromiseCapability {
    Object* promise { nullptr };
    FunctionObject* resolve { nullptr };
    FunctionObject* reject { nullptr };
};

// 27.2.1.1.1 IfAbruptRejectPromise ( value, capability ), https://tc39.es/ecma262/#sec-ifabruptrejectpromise
#define __TRY_OR_REJECT(vm, capability, expression, CALL_CHECK)                                                                     \
    ({                                                                                                                              \
        auto _temporary_try_or_reject_result = (expression);                                                                        \
        /* 1. If value is an abrupt completion, then */                                                                             \
        if (_temporary_try_or_reject_result.is_error()) {                                                                           \
            /* a. Perform ? Call(capability.[[Reject]], undefined, « value.[[Value]] »). */                                       \
            CALL_CHECK(JS::call(vm, *capability.reject, js_undefined(), *_temporary_try_or_reject_result.release_error().value())); \
                                                                                                                                    \
            /* b. Return capability.[[Promise]]. */                                                                                 \
            return capability.promise;                                                                                              \
        }                                                                                                                           \
                                                                                                                                    \
        /* 2. Else if value is a Completion Record, set value to value.[[Value]]. */                                                \
        _temporary_try_or_reject_result.release_value();                                                                            \
    })

#define TRY_OR_REJECT(vm, capability, expression) \
    __TRY_OR_REJECT(vm, capability, expression, TRY)

#define TRY_OR_MUST_REJECT(vm, capability, expression) \
    __TRY_OR_REJECT(vm, capability, expression, MUST)

// 27.2.1.1.1 IfAbruptRejectPromise ( value, capability ), https://tc39.es/ecma262/#sec-ifabruptrejectpromise
#define TRY_OR_REJECT_WITH_VALUE(vm, capability, expression)                                                                 \
    ({                                                                                                                       \
        auto _temporary_try_or_reject_result = (expression);                                                                 \
        /* 1. If value is an abrupt completion, then */                                                                      \
        if (_temporary_try_or_reject_result.is_error()) {                                                                    \
            /* a. Perform ? Call(capability.[[Reject]], undefined, « value.[[Value]] »). */                                \
            TRY(JS::call(vm, *capability.reject, js_undefined(), *_temporary_try_or_reject_result.release_error().value())); \
                                                                                                                             \
            /* b. Return capability.[[Promise]]. */                                                                          \
            return Value { capability.promise };                                                                             \
        }                                                                                                                    \
                                                                                                                             \
        /* 2. Else if value is a Completion Record, set value to value.[[Value]]. */                                         \
        _temporary_try_or_reject_result.release_value();                                                                     \
    })

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
ThrowCompletionOr<PromiseCapability> new_promise_capability(VM& vm, Value constructor);

// 27.2.1.2 PromiseReaction Records, https://tc39.es/ecma262/#sec-promisereaction-records
class PromiseReaction final : public Cell {
    JS_CELL(PromiseReaction, Cell);

public:
    enum class Type {
        Fulfill,
        Reject,
    };

    static PromiseReaction* create(VM& vm, Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler)
    {
        return vm.heap().allocate_without_realm<PromiseReaction>(type, capability, move(handler));
    }

    virtual ~PromiseReaction() = default;

    Type type() const { return m_type; }
    Optional<PromiseCapability> const& capability() const { return m_capability; }

    Optional<JobCallback>& handler() { return m_handler; }
    Optional<JobCallback> const& handler() const { return m_handler; }

private:
    PromiseReaction(Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler);

    virtual void visit_edges(Visitor&) override;

    Type m_type;
    Optional<PromiseCapability> m_capability;
    Optional<JobCallback> m_handler;
};

}
