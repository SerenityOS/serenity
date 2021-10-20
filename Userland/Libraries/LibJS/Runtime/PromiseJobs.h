/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Promise.h>

namespace JS {

class PromiseReactionJob final : public NativeFunction {
    JS_OBJECT(PromiseReactionJob, NativeFunction);

public:
    static PromiseReactionJob* create(GlobalObject&, PromiseReaction&, Value argument);

    explicit PromiseReactionJob(PromiseReaction&, Value argument, Object& prototype);
    virtual ~PromiseReactionJob() override = default;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    PromiseReaction& m_reaction;
    Value m_argument;
};

class PromiseResolveThenableJob final : public NativeFunction {
    JS_OBJECT(PromiseReactionJob, NativeFunction);

public:
    static PromiseResolveThenableJob* create(GlobalObject&, Promise&, Value thenable, JobCallback then);

    explicit PromiseResolveThenableJob(Promise&, Value thenable, JobCallback then, Object& prototype);
    virtual ~PromiseResolveThenableJob() override = default;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    Promise& m_promise_to_resolve;
    Value m_thenable;
    JobCallback m_then;
};

}
