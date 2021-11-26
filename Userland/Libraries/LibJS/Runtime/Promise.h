/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

ThrowCompletionOr<Object*> promise_resolve(GlobalObject&, Object& constructor, Value);

class Promise : public Object {
    JS_OBJECT(Promise, Object);

public:
    enum class State {
        Pending,
        Fulfilled,
        Rejected,
    };
    enum class RejectionOperation {
        Reject,
        Handle,
    };

    static Promise* create(GlobalObject&);

    explicit Promise(Object& prototype);
    virtual ~Promise() = default;

    State state() const { return m_state; }
    Value result() const { return m_result; }

    struct ResolvingFunctions {
        FunctionObject& resolve;
        FunctionObject& reject;
    };
    ResolvingFunctions create_resolving_functions();

    Value fulfill(Value value);
    Value reject(Value reason);
    Value perform_then(Value on_fulfilled, Value on_rejected, Optional<PromiseCapability> result_capability);

protected:
    virtual void visit_edges(Visitor&) override;

private:
    bool is_settled() const { return m_state == State::Fulfilled || m_state == State::Rejected; }

    void trigger_reactions() const;

    // 27.2.6 Properties of Promise Instances, https://tc39.es/ecma262/#sec-properties-of-promise-instances
    State m_state { State::Pending };             // [[PromiseState]]
    Value m_result;                               // [[PromiseResult]]
    Vector<PromiseReaction*> m_fulfill_reactions; // [[PromiseFulfillReactions]]
    Vector<PromiseReaction*> m_reject_reactions;  // [[PromiseRejectReactions]]
    bool m_is_handled { false };                  // [[PromiseIsHandled]]
};

}
