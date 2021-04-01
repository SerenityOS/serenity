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

#pragma once

#include <AK/Vector.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

Object* promise_resolve(GlobalObject&, Object& constructor, Value);

class Promise final : public Object {
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
        Function& resolve;
        Function& reject;
    };
    ResolvingFunctions create_resolving_functions();

    Value fulfill(Value value);
    Value reject(Value reason);
    Value perform_then(Value on_fulfilled, Value on_rejected, Optional<PromiseCapability> result_capability);

private:
    virtual void visit_edges(Visitor&) override;

    bool is_settled() const { return m_state == State::Fulfilled || m_state == State::Rejected; }

    void trigger_reactions() const;

    State m_state { State::Pending };
    Value m_result;
    Vector<PromiseReaction*> m_fulfill_reactions;
    Vector<PromiseReaction*> m_reject_reactions;
    bool m_is_handled { false };
};

}
